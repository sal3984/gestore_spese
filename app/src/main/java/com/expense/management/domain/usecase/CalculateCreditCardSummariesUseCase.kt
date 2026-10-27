package com.expense.management.domain.usecase

import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.ActiveCreditCard
import com.expense.management.domain.model.CardCycleRules
import com.expense.management.domain.model.CreditCardSummary
import com.expense.management.domain.model.CreditCardType
import java.time.LocalDate
import java.time.YearMonth

class CalculateCreditCardSummariesUseCase(
    private val calculateCardExposure: CalculateCardExposureUseCase = CalculateCardExposureUseCase(),
) {

    fun execute(
        transactions: List<TransactionEntity>,
        cards: List<ActiveCreditCard>,
        targetMonth: YearMonth,
        amexPaidByCard: Map<String, Double>,
        unsettledExposureByCard: Map<String, Double> = emptyMap(),
    ): Map<String, CreditCardSummary> = cards.associate { card ->
        card.id to when (card.cardType) {
            CreditCardType.REVOLVING,
            CreditCardType.INSTALLMENT,
            -> revolvingSummary(card, transactions)
            CreditCardType.AMEX_HYBRID -> amexSummary(card, transactions, amexPaidByCard[card.id] ?: 0.0)
            CreditCardType.SALDO -> if (CardCycleRules.fromCardDays(card.closingDay, card.paymentDay) != null) {
                saldoCycleSummary(card, unsettledExposureByCard[card.id] ?: 0.0)
            } else {
                saldoSummary(card, transactions, targetMonth)
            }
        }
    }

    private fun revolvingSummary(card: ActiveCreditCard, transactions: List<TransactionEntity>): CreditCardSummary {
        val cardTransactions = transactions.filter { it.belongsTo(card) }
        val totalUtilized = cardTransactions
            .filter { it.type == TransactionType.EXPENSE && it.isCreditCard }
            .sumOf { it.amount }
        val totalRepaid = cardTransactions
            .filter { it.type == TransactionType.INCOME && it.isCreditCard }
            .sumOf { it.amount }
        val displayed = totalUtilized - totalRepaid
        return CreditCardSummary(
            cardId = card.id,
            name = card.name,
            limit = card.limit,
            cardType = card.cardType,
            displayedSpent = displayed,
            totalUtilized = totalUtilized,
            totalPaid = 0.0,
            totalRepaid = totalRepaid,
            progress = card.progressFor(displayed),
        )
    }

    private fun amexSummary(
        card: ActiveCreditCard,
        transactions: List<TransactionEntity>,
        pagoflexPaid: Double,
    ): CreditCardSummary {
        val cardTransactions = transactions.filter { it.belongsTo(card) }
        val totalUtilized = cardTransactions
            .filter { it.type == TransactionType.EXPENSE && it.isCreditCard }
            .sumOf { it.amount }
        val incomeRepaid = cardTransactions
            .filter { it.type == TransactionType.INCOME && it.isCreditCard }
            .sumOf { it.amount }
        val totalRepaid = incomeRepaid + pagoflexPaid
        val displayed = (totalUtilized - totalRepaid).coerceAtLeast(0.0)
        return CreditCardSummary(
            cardId = card.id,
            name = card.name,
            limit = card.limit,
            cardType = card.cardType,
            displayedSpent = displayed,
            totalUtilized = totalUtilized,
            totalPaid = totalRepaid,
            totalRepaid = totalRepaid,
            progress = card.progressFor(displayed),
        )
    }

    /**
     * Carta a saldo con ciclo: barra e importo sono l'esposizione non liquidata (spese dei cicli
     * non ancora addebitati), non la spesa del mese visualizzato. Non dipende dal mese.
     */
    private fun saldoCycleSummary(card: ActiveCreditCard, unsettledExposure: Double): CreditCardSummary {
        val exposure = calculateCardExposure.execute(card.limit, unsettledExposure)
        return CreditCardSummary(
            cardId = card.id,
            name = card.name,
            limit = card.limit,
            cardType = card.cardType,
            displayedSpent = exposure.exposure,
            totalUtilized = exposure.exposure,
            totalPaid = 0.0,
            progress = exposure.utilization,
        )
    }

    /** Carta a saldo senza giorni di chiusura/addebito: nessun ciclo, resta la spesa del mese. */
    private fun saldoSummary(
        card: ActiveCreditCard,
        transactions: List<TransactionEntity>,
        targetMonth: YearMonth,
    ): CreditCardSummary {
        val spent = transactions
            .filter { it.belongsTo(card) && it.type == TransactionType.EXPENSE }
            .filter { it.effectiveDate.inMonth(targetMonth) }
            .sumOf { it.amount }
        return CreditCardSummary(
            cardId = card.id,
            name = card.name,
            limit = card.limit,
            cardType = card.cardType,
            displayedSpent = spent,
            totalUtilized = spent,
            totalPaid = 0.0,
            progress = card.progressFor(spent),
        )
    }

    private fun TransactionEntity.belongsTo(card: ActiveCreditCard): Boolean =
        creditCardId == card.id || paymentMethodId == card.id

    private fun ActiveCreditCard.progressFor(displayed: Double): Float =
        if (limit > 0) (displayed / limit).toFloat() else 0f

    private fun String.inMonth(month: YearMonth): Boolean = try {
        YearMonth.from(LocalDate.parse(this)) == month
    } catch (_: Exception) {
        false
    }
}
