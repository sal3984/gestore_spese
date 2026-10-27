package com.expense.management.domain.usecase

import com.expense.management.data.ExpenseRepository
import com.expense.management.domain.model.CycleStatus
import java.time.LocalDate

/** Chiude i cicli aperti il cui giorno di chiusura e' passato. */
class CloseBillingCycleUseCase(
    private val repository: ExpenseRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
) {
    suspend operator fun invoke(): Int {
        val toClose = repository.getBillingCyclesToClose(today().toString())
        toClose.forEach { repository.updateBillingCycleStatus(it.id, CycleStatus.CLOSED.name) }
        return toClose.size
    }
}
