package com.expense.management.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.HttpURLConnection
import java.net.URL

object CurrencyUtils {
    private const val ECB_URL = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml"
    private var cachedRates: Map<String, Double>? = null
    private var lastFetchTime: Long = 0
    private const val CACHE_DURATION = 3600 * 1000 * 24 // 24 hours

    suspend fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double? {
        val fromCurrencyNormalized = normalizeCurrencyCode(fromCurrency)
        val toCurrencyNormalized = normalizeCurrencyCode(toCurrency)

        if (fromCurrencyNormalized == toCurrencyNormalized) return amount

        val rates = getRates() ?: return null

        val fromRate = if (fromCurrencyNormalized == "EUR") 1.0 else rates[fromCurrencyNormalized]
        val toRate = if (toCurrencyNormalized == "EUR") 1.0 else rates[toCurrencyNormalized]

        if (fromRate == null || toRate == null) return null

        // Convert to EUR then to target
        val amountInEur = amount / fromRate
        return amountInEur * toRate
    }

    private suspend fun getRates(): Map<String, Double>? {
        if (cachedRates != null && System.currentTimeMillis() - lastFetchTime < CACHE_DURATION) {
            return cachedRates
        }

        return withContext(Dispatchers.IO) {
            try {
                val url = URL(ECB_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val inputStream = connection.inputStream
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(inputStream, null)

                val rates = mutableMapOf<String, Double>()
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.name == "Cube") {
                        val currency = parser.getAttributeValue(null, "currency")
                        val rate = parser.getAttributeValue(null, "rate")
                        if (currency != null && rate != null) {
                            rates[currency] = rate.toDouble()
                        }
                    }
                    eventType = parser.next()
                }
                inputStream.close()
                cachedRates = rates
                lastFetchTime = System.currentTimeMillis()
                rates
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun normalizeCurrencyCode(currencySymbol: String): String {
        return when (currencySymbol) {
            "€" -> "EUR"
            "$" -> "USD"
            "£" -> "GBP"
            "¥" -> "JPY"
            "Ft" -> "HUF"
            // Se la stringa è già un codice a 3 lettere (es. "USD"), la restituisce così com'è,
            // altrimenti la converte in maiuscolo per sicurezza.
            else -> currencySymbol.uppercase()
        }
    }
}
