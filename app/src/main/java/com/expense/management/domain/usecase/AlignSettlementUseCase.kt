package com.expense.management.domain.usecase

import com.expense.management.data.ExpenseRepository
import com.expense.management.domain.model.CycleDueBreakdown
import com.expense.management.domain.model.CycleStatus

/**
 * Porta l'addebito di un ciclo gia' pagato al totale attuale delle sue spese, dopo che una spesa
 * del ciclo e' stata aggiunta, modificata o cancellata. Lo chiede l'utente dall'estratto: il conto
 * non cambia mai da solo. Se il ciclo resta senza spese l'addebito viene cancellato.
 */
class AlignSettlementUseCase(private val repository: ExpenseRepository) {

    sealed interface Result {
        data class Aligned(val previousAmount: Double, val newAmount: Double) : Result
        data object Removed : Result
        data object NothingToAlign : Result
    }

    suspend operator fun invoke(cycleId: String): Result = repository.withTransaction {
        val cycle = repository.getBillingCycleById(cycleId)
        val settlementId = cycle?.settlementTransactionId
        val settlement = settlementId?.let { repository.getTransactionById(it) }
        if (cycle == null || cycle.status != CycleStatus.PAID.name || settlement == null) {
            return@withTransaction Result.NothingToAlign
        }
        val totals = repository.getBillingCycleTotals(cycleId)
        val due = CycleDueBreakdown(
            purchases = totals.purchases.coerceAtLeast(0.0),
            credits = totals.credits.coerceAtLeast(0.0),
        ).totalDue
        when {
            due == settlement.amount -> Result.NothingToAlign
            due == 0.0 -> {
                repository.markBillingCycleSettled(cycleId, null)
                repository.deleteTransaction(settlement.id)
                Result.Removed
            }
            else -> {
                repository.insertTransaction(settlement.copy(amount = due, originalAmount = due))
                Result.Aligned(previousAmount = settlement.amount, newAmount = due)
            }
        }
    }
}
