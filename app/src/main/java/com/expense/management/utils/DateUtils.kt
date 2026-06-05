package com.expense.management.utils

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

object DateUtils {

    data class CardDateInfo(
        val closingDay: Int,
        val paymentDay: Int,
    )

    fun calculateEffectiveDate(transactionDate: LocalDate, card: CardDateInfo): String {
        if (card.closingDay <= 0 || card.paymentDay <= 0) {
            return transactionDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        }

        val closingDay = card.closingDay
        val paymentDay = card.paymentDay
        val transactionYearMonth = YearMonth.from(transactionDate)
        val effectiveClosing = closingDay.coerceAtMost(transactionYearMonth.lengthOfMonth())

        val closingMonth = if (transactionDate.dayOfMonth >= effectiveClosing) {
            transactionYearMonth.plusMonths(1)
        } else {
            transactionYearMonth
        }

        val paymentMonth = if (paymentDay >= closingDay) {
            closingMonth
        } else {
            closingMonth.plusMonths(1)
        }

        var finalPaymentDate = getValidDateForDay(paymentMonth, paymentDay)

        if (!finalPaymentDate.isAfter(transactionDate)) {
            val adjustedMonth = paymentMonth.plusMonths(1)
            finalPaymentDate = getValidDateForDay(adjustedMonth, paymentDay)
        }

        return finalPaymentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    private fun getValidDateForDay(yearMonth: YearMonth, day: Int): LocalDate {
        val lastDayOfMonth = yearMonth.lengthOfMonth()
        val validDay = if (day > lastDayOfMonth) lastDayOfMonth else day
        return yearMonth.atDay(validDay)
    }
}
