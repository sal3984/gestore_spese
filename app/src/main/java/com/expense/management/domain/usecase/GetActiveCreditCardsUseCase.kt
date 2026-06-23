package com.expense.management.domain.usecase

import com.expense.management.domain.model.ActiveCreditCard
import com.expense.management.domain.repository.PaymentMethodRepository
import kotlinx.coroutines.flow.Flow

class GetActiveCreditCardsUseCase(private val repository: PaymentMethodRepository) {
    operator fun invoke(): Flow<List<ActiveCreditCard>> = repository.getAllActiveCreditCardsFlow()
}
