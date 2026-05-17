package com.expense.management.domain.usecase

import com.expense.management.data.CreditCardEntity
import com.expense.management.data.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class GetCreditCardsUseCase(private val repository: ExpenseRepository) {
    operator fun invoke(): Flow<List<CreditCardEntity>> = repository.allCreditCards ?: flowOf(emptyList())
}

class ManageCreditCardUseCase(private val repository: ExpenseRepository) {
    suspend fun add(card: CreditCardEntity) = repository.insertCreditCard(card)
    suspend fun update(card: CreditCardEntity) = repository.updateCreditCard(card)
    suspend fun delete(card: CreditCardEntity) = repository.deleteCreditCard(card)
}
