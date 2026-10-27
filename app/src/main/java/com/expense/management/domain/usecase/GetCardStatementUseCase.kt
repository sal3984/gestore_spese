package com.expense.management.domain.usecase

import com.expense.management.data.BillingCycleEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.data.toDomain
import com.expense.management.domain.model.ActiveCreditCard
import com.expense.management.domain.model.BillingCycle
import com.expense.management.domain.model.CardStatement
import com.expense.management.domain.model.CycleStatus
import com.expense.management.domain.model.StatementCycle
import com.expense.management.domain.model.StatementEntry
import java.time.LocalDate
import kotlin.math.roundToLong

/**
 * Costruisce l'estratto di una carta a saldo dai suoi cicli, dalle transazioni collegate e dagli
 * addebiti che li hanno saldati. L'esposizione arriva dalla stessa query della barra in dashboard,
 * cosi' schermata e dashboard mostrano la stessa cifra.
 */
class GetCardStatementUseCase(
    private val calculateCardExposure: CalculateCardExposureUseCase = CalculateCardExposureUseCase(),
) {

    fun execute(
        card: ActiveCreditCard,
        cycles: List<BillingCycleEntity>,
        cycleTransactions: List<TransactionEntity>,
        settlements: List<TransactionEntity>,
        unsettledExposure: Double,
        linkedAccountName: String?,
        today: LocalDate,
    ): CardStatement {
        val transactionsByCycle = cycleTransactions.groupBy { it.billingCycleId }
        val settlementsById = settlements.associateBy { it.id }
        val statementCycles = cycles
            .mapNotNull { it.toDomain() }
            .map { it.toStatementCycle(transactionsByCycle[it.id].orEmpty(), settlementsById, today) }
        return CardStatement(
            cardId = card.id,
            cardName = card.name,
            limit = card.limit,
            closingDay = card.closingDay,
            paymentDay = card.paymentDay,
            exposure = calculateCardExposure.execute(card.limit, unsettledExposure),
            linkedAccountName = linkedAccountName,
            upcomingCharges = statementCycles
                .filter { it.status == CycleStatus.CLOSED || it.status == CycleStatus.PARTIALLY_PAID }
                .sortedBy { it.dueDate },
            openCycles = statementCycles.filter { it.status == CycleStatus.OPEN }.sortedBy { it.cycleMonth },
            history = statementCycles.filter { it.status == CycleStatus.PAID }.sortedByDescending { it.cycleMonth },
        )
    }

    private fun BillingCycle.toStatementCycle(
        transactions: List<TransactionEntity>,
        settlementsById: Map<String, TransactionEntity>,
        today: LocalDate,
    ): StatementCycle {
        val entries = transactions.mapNotNull { it.toEntry() }.sortedByDescending { it.date }
        val (scheduled, current) = entries.partition { it.date.isAfter(today) }
        val settlement = settlementTransactionId?.let { settlementsById[it] }
        val total = current.sumOf { if (it.isCredit) -it.amount else it.amount }.toCents()
        return StatementCycle(
            cycleId = id,
            cycleMonth = cycleMonth,
            closingDate = closingDate,
            dueDate = dueDate,
            status = status,
            total = total,
            entries = current,
            scheduledEntries = scheduled,
            settledAmount = settlement?.amount,
            // Un ciclo vuoto si salda senza addebito: non e' un ciclo di prima dell'aggiornamento
            settledBeforeUpdate = status == CycleStatus.PAID && settlementTransactionId == null && total > 0.0,
        )
    }

    private fun TransactionEntity.toEntry(): StatementEntry? {
        val date = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
        return StatementEntry(
            transactionId = id,
            description = description,
            date = date,
            amount = amount,
            isCredit = type == TransactionType.INCOME,
        )
    }

    private fun Double.toCents(): Double = (this * 100).roundToLong() / 100.0
}
