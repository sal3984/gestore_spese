package com.expense.management.utils

import com.expense.management.data.CurrencyDao
import com.expense.management.data.CurrencyRate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.HttpURLConnection
import java.net.URL

class CurrencyUtils(private val currencyDao: CurrencyDao) {
    private val ecbUrl = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml"
    private val cacheDuration = 3600 * 1000 * 24 // 24 hours

    suspend fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double? {
        val fromNormalized = normalizeCurrencyCode(fromCurrency)
        val toNormalized = normalizeCurrencyCode(toCurrency)

        if (fromNormalized == toNormalized) return amount

        // Recuperiamo i tassi (prima prova rete, poi DB)
        val ratesMap = getRatesMap()

        val fromRate = if (fromNormalized == "EUR") 1.0 else ratesMap[fromNormalized]
        val toRate = if (toNormalized == "EUR") 1.0 else ratesMap[toNormalized]

        if (fromRate == null || toRate == null) return null

        val amountInEur = amount / fromRate
        return amountInEur * toRate
    }

    private suspend fun getRatesMap(): Map<String, Double> {
        val lastUpdate = currencyDao.getLastUpdateTimestamp() ?: 0L
        val isCacheExpired = (System.currentTimeMillis() - lastUpdate) > cacheDuration

        // 1. Se la cache è scaduta, proviamo a scaricare da internet
        if (isCacheExpired) {
            val freshRates = fetchFromNetwork()
            if (freshRates != null) {
                // Salviamo nel DB
                currencyDao.insertRates(freshRates)
                // Ritorniamo la mappa fresca
                return freshRates.associate { it.currencyCode to it.rateAgainstEuro }
            }
        }

        // 2. Se la cache è valida O il download è fallito (offline), usiamo il DB
        val dbRates = currencyDao.getAllRates()
        return dbRates.associate { it.currencyCode to it.rateAgainstEuro }
    }

    suspend fun forceUpdate(): Boolean {
        val freshRates = fetchFromNetwork()
        if (freshRates != null) {
            currencyDao.insertRates(freshRates)
            return true
        }
        return false
    }

    private suspend fun fetchFromNetwork(): List<CurrencyRate>? = withContext(Dispatchers.IO) {
        try {
            val url = URL(ecbUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val inputStream = connection.inputStream
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(inputStream, null)

            val ratesList = mutableListOf<CurrencyRate>()
            val now = System.currentTimeMillis()

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "Cube") {
                    val currency = parser.getAttributeValue(null, "currency")
                    val rate = parser.getAttributeValue(null, "rate")
                    if (currency != null && rate != null) {
                        ratesList.add(CurrencyRate(currency, rate.toDouble(), now))
                    }
                }
                eventType = parser.next()
            }
            inputStream.close()
            ratesList
        } catch (e: Exception) {
            e.printStackTrace()
            // Ritorna null se offline o errore
            null
        }
    }

    suspend fun getLastUpdate(): Long? {
        return currencyDao.getLastUpdateTimestamp()
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
