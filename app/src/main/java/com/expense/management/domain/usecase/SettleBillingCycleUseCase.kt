package com.expense.management.domain.usecase

import com.expense.management.data.BillingCycleEntity
import com.expense.management.data.ExpenseRepository
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.CycleDueBreakdown
import java.time.LocalDate
import java.util.UUID

/**
 * Salda i cicli chiusi arrivati alla data di addebito con un solo addebito sul conto collegato,
 * pari alle spese meno gli accrediti del ciclo. Idempotente: l'addebito ha sourceRef univoco
 * e, se esiste gia', viene solo ricollegato. Un ciclo a zero si chiude senza addebito.
 */
class SettleBillingCycleUseCase(
    private val repository: ExpenseRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
) {
    suspend operator fun invoke(): Int {
        val toSettle = repository.getBillingCyclesToSettle(today().toString())
        toSettle.forEach { cycle ->
            repository.withTransaction {
                val totals = repository.getBillingCycleTotals(cycle.id)
                val due = CycleDueBreakdown(
                    purchases = totals.purchases.coerceAtLeast(0.0),
                    credits = totals.credits.coerceAtLeast(0.0),
                ).totalDue
                val settlement = if (due > 0.0) settlementFor(cycle, due) else null
                repository.markBillingCycleSettled(cycle.id, settlement?.id)
            }
        }
        return toSettle.size
    }

    private suspend fun settlementFor(cycle: BillingCycleEntity, amount: Double): TransactionEntity {
        val sourceRef = BillingCycleEntity.settlementRef(cycle.id)
        repository.getTransactionBySourceRef(sourceRef)?.let { return it }
        val cardName = repository.getPaymentMethodById(cycle.paymentMethodId)?.name.orEmpty()
        val settlement = TransactionEntity(
            id = UUID.randomUUID().toString(),
            date = cycle.dueDate,
            description = "Addebito $cardName".trim(),
            amount = amount,
            categoryId = SETTLEMENT_CATEGORY,
            type = TransactionType.EXPENSE,
            isCreditCard = false,
            effectiveDate = cycle.dueDate,
            originalAmount = amount,
            originalCurrency = "",
            paymentMethodId = repository.getCreditCardDetail(cycle.paymentMethodId)?.linkedPaymentMethodId,
            sourceRef = sourceRef,
        )
        repository.insertTransaction(settlement)
        return settlement
    }

    private companion object {
        const val SETTLEMENT_CATEGORY = "credit_card_payment"
    }
}
