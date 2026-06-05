package com.expense.management.domain.usecase

import com.expense.management.domain.model.ReceiptScanResult

class ScanReceiptUseCase {

    private val amountLabelRegex = Regex(
        """(?i)(?:TOTALE|IMPORTO|SUBTOTALE|TOTAL|DOVUTO|PAGAMENTO|ACCETTATO|ADDEBITATO)\s*[:\s]*([0-9]+[.,][0-9]{1,3}(?:\s*[.,]\s*[0-9]{2})?)""",
    )
    private val unitBeforeRegex = Regex("""(?i)(?:€|EUR|EURO)\s*([0-9]+[.,][0-9]{1,3}(?:\s*[.,]\s*[0-9]{2})?)""")
    private val unitAfterRegex = Regex("""([0-9]+[.,][0-9]{1,3}(?:\s*[.,]\s*[0-9]{2})?)\s*(?:€|EUR|EURO)""")
    private val plainDecimalRegex = Regex("""^([0-9]+[.,][0-9]{2})$""")
    private val dateRegex = Regex("""(\d{1,2})[/\-](\d{1,2})[/\-](\d{2,4})""")
    private val yearFirstDateRegex = Regex("""(\d{4})[/\-](\d{1,2})[/\-](\d{1,2})""")

    operator fun invoke(rawText: String): ReceiptScanResult {
        if (rawText.isBlank()) return ReceiptScanResult(rawText = rawText)

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val totalAmount = extractAmount(lines)
        val description = extractMerchant(lines)
        val date = extractDate(lines)

        return ReceiptScanResult(
            amount = totalAmount,
            description = description,
            date = date,
            rawText = rawText,
        )
    }

    private fun extractAmount(lines: List<String>): Double? {
        val tail = lines.asReversed().take(20)
        for (line in tail) {
            val match = amountLabelRegex.find(line)
            if (match != null) {
                val num = parseAmount(match.groupValues[1])
                if (num != null) return num
            }
        }
        for (line in tail) {
            val match = unitBeforeRegex.find(line)
            if (match != null) {
                val num = parseAmount(match.groupValues[1])
                if (num != null) return num
            }
        }
        for (line in tail) {
            val match = unitAfterRegex.find(line)
            if (match != null) {
                val num = parseAmount(match.groupValues[1])
                if (num != null) return num
            }
        }
        for (line in tail) {
            val sanitized = line.replace(".", "").replace(",", ".")
            val match = plainDecimalRegex.find(sanitized)
            if (match != null) {
                return match.groupValues[1].toDoubleOrNull()
            }
        }
        return null
    }

    private fun parseAmount(raw: String): Double? {
        val cleaned = raw.replace(" ", "").replace(",", ".")
        val parts = cleaned.split(".")
        if (parts.size <= 2) return cleaned.toDoubleOrNull()
        val last = parts.last()
        if (last.length == 2) {
            return parts.dropLast(1).joinToString("").toDoubleOrNull()?.let { it + last.toDouble() / 100 }
        }
        return cleaned.toDoubleOrNull()
    }

    private fun extractMerchant(lines: List<String>): String? {
        val skipKeywords = listOf(
            "VIA ", "VIALE ", "PIAZZA ", "CORSO ", "TEL", "PARTITA IVA", "P.IVA", "CF",
            "CODICE FISCALE", "DATA", "ORA ", "FATTURA", "RICEVUTA", "SCONTRINO",
        )
        for (line in lines.take(5)) {
            val upper = line.uppercase()
            if (line.length < 2 || line.length > 80) continue
            if (upper.any { it.isLetter() } && skipKeywords.none { upper.startsWith(it) }) {
                return line
            }
        }
        return null
    }

    private fun extractDate(lines: List<String>): String? {
        for (line in lines.asReversed().take(15)) {
            val yearFirstMatch = yearFirstDateRegex.find(line)
            if (yearFirstMatch != null) {
                val (y, m, d) = yearFirstMatch.destructured
                val year = if (y.length == 4) y else "20$y"
                return "$year-${m.padStart(2, '0')}-${d.padStart(2, '0')}"
            }
            val match = dateRegex.find(line)
            if (match != null) {
                val (d, m, y) = match.destructured
                val year = if (y.length == 4) y else "20$y"
                return "$year-${m.padStart(2, '0')}-${d.padStart(2, '0')}"
            }
        }
        return null
    }
}
