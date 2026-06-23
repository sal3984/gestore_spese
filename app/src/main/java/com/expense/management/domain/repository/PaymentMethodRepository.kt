package com.expense.management.domain.repository

import com.expense.management.domain.model.ActiveCreditCard
import com.expense.management.domain.model.PaymentMethod
import com.expense.management.domain.model.PaymentMethodDetails
import kotlinx.coroutines.flow.Flow

interface PaymentMethodRepository {
    fun getAllPaymentMethodsFlow(): Flow<List<PaymentMethod>>
    suspend fun getPaymentMethodById(id: String): PaymentMethod?
    suspend fun getAllPaymentMethods(): List<PaymentMethod>
    fun getAllActiveCreditCardsFlow(): Flow<List<ActiveCreditCard>>
    suspend fun savePaymentMethod(method: PaymentMethod, details: PaymentMethodDetails?)
    suspend fun updatePaymentMethod(method: PaymentMethod, details: PaymentMethodDetails? = null)
    suspend fun deletePaymentMethod(id: String)
    suspend fun deleteAllPaymentMethods()
    suspend fun getAllPaymentMethodDetails(): List<PaymentMethodDetails>
    suspend fun getPaymentMethodDetails(paymentMethodId: String): PaymentMethodDetails?
}
