package com.expense.management.domain.usecase

import com.expense.management.data.ExpenseRepository
import com.expense.management.domain.model.ActiveCreditCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class GetCreditCardsUseCase(private val repository: ExpenseRepository) {
    operator fun invoke(): Flow<List<ActiveCreditCard>> = flowOf(emptyList())
}

class ManageCreditCardUseCase(private val repository: ExpenseRepository) {
    suspend fun add(card: ActiveCreditCard) {}
    suspend fun update(card: ActiveCreditCard) {}
    suspend fun delete(card: ActiveCreditCard) {}
}
