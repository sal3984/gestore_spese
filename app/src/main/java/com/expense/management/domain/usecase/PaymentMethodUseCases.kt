package com.expense.management.domain.usecase

import com.expense.management.domain.model.PaymentMethod
import com.expense.management.domain.model.PaymentMethodDetails
import com.expense.management.domain.repository.PaymentMethodRepository
import kotlinx.coroutines.flow.Flow

class GetPaymentMethodsUseCase(private val repository: PaymentMethodRepository) {
    operator fun invoke(): Flow<List<PaymentMethod>> = repository.getAllPaymentMethodsFlow()
}

class ManagePaymentMethodUseCase(private val repository: PaymentMethodRepository) {
    suspend fun add(method: PaymentMethod, details: PaymentMethodDetails? = null) {
        repository.savePaymentMethod(method, details)
    }

    suspend fun update(method: PaymentMethod, details: PaymentMethodDetails? = null) {
        repository.updatePaymentMethod(method, details)
    }

    suspend fun delete(id: String) = repository.deletePaymentMethod(id)
    suspend fun deleteAll() = repository.deleteAllPaymentMethods()

    suspend fun getPaymentMethodById(id: String): PaymentMethod? =
        repository.getPaymentMethodById(id)

    suspend fun getAllPaymentMethods(): List<PaymentMethod> =
        repository.getAllPaymentMethods()
}
