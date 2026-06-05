package com.expense.management.domain.usecase

import com.expense.management.data.ExpenseRepository
import com.expense.management.data.PaymentMethodEntity
import kotlinx.coroutines.flow.Flow

class GetPaymentMethodsUseCase(private val repository: ExpenseRepository) {
    operator fun invoke(): Flow<List<PaymentMethodEntity>> = repository.allPaymentMethods
}

class ManagePaymentMethodUseCase(private val repository: ExpenseRepository) {
    suspend fun add(paymentMethod: PaymentMethodEntity) = repository.insertPaymentMethod(paymentMethod)
    suspend fun update(paymentMethod: PaymentMethodEntity) {
        repository.updatePaymentMethod(paymentMethod)
    }
    suspend fun delete(id: String) = repository.deletePaymentMethod(id)
    suspend fun deleteAll() = repository.deleteAllPaymentMethods()
    suspend fun getPaymentMethodById(id: String): PaymentMethodEntity? = repository.getPaymentMethodById(id)
    suspend fun getAllPaymentMethods(): List<PaymentMethodEntity> = repository.getAllPaymentMethods()
}
