package com.expense.management.domain.usecase

import com.expense.management.data.BillingCycleEntity
import com.expense.management.data.ExpenseRepository
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.CardCycleRules
import com.expense.management.domain.model.CreditCardType
import java.time.LocalDate

/**
 * Collega un acquisto al ciclo di fatturazione della sua carta, creando il ciclo se manca.
 * Stesse regole del backfill della migration 23->24: solo spese lato carta su carte a SALDO
 * con giorni configurati, ciclo ricalcolato dalla data d'acquisto. Ogni altra transazione
 * esce senza ciclo, anche se ne aveva uno (es. spesa spostata su un'altra carta).
 */
class AssignBillingCycleUseCase(
    private val repository: ExpenseRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
) {
    private val calculateCycle = CalculateBillingCycleUseCase()

    suspend operator fun invoke(transaction: TransactionEntity): TransactionEntity {
        val cycle = cycleFor(transaction)
        if (cycle != null) repository.insertBillingCycle(cycle)
        return transaction.copy(billingCycleId = cycle?.id)
    }

    /** Collega gli acquisti rimasti senza ciclo, es. dopo un ripristino da backup. */
    suspend fun assignMissing(): Int {
        var assigned = 0
        for (transaction in repository.getCardExpensesWithoutCycle()) {
            val cycle = cycleFor(transaction) ?: continue
            repository.insertBillingCycle(cycle)
            repository.setTransactionBillingCycle(transaction.id, cycle.id)
            assigned++
        }
        return assigned
    }

    private suspend fun cycleFor(transaction: TransactionEntity): BillingCycleEntity? {
        if (!transaction.isCreditCard || transaction.type != TransactionType.EXPENSE) return null
        // La carta puo' stare in paymentMethodId (sistema nuovo) o in creditCardId (legacy)
        val card = listOfNotNull(transaction.paymentMethodId, transaction.creditCardId)
            .firstNotNullOfOrNull { repository.getCreditCardDetail(it) }
            ?: return null
        if (card.cardType != CreditCardType.SALDO.name) return null
        val rules = CardCycleRules.fromCardDays(card.closingDay, card.paymentDay) ?: return null
        val purchaseDate = runCatching { LocalDate.parse(transaction.date) }.getOrNull() ?: return null
        return BillingCycleEntity.forPurchase(card.paymentMethodId, calculateCycle.execute(purchaseDate, rules), today())
    }
}
