package com.expense.management.domain.usecase

import com.expense.management.data.CreditCardDetailEntity
import com.expense.management.data.ExpenseRepository
import com.expense.management.data.KlarnaDetailEntity
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.data.PaypalDetailEntity
import com.expense.management.data.RevolutDetailEntity
import com.expense.management.data.SatispayDetailEntity
import kotlinx.coroutines.flow.Flow

class GetPaymentMethodsUseCase(private val repository: ExpenseRepository) {
    operator fun invoke(): Flow<List<PaymentMethodEntity>> = repository.allPaymentMethods
}

class InsertPaymentMethodUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(paymentMethod: PaymentMethodEntity) {
        repository.insertPaymentMethod(paymentMethod)
    }
}

class DeletePaymentMethodUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(id: String) {
        repository.deletePaymentMethod(id)
    }
}

class GetCreditCardDetailUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(paymentMethodId: String): CreditCardDetailEntity? =
        repository.getCreditCardDetail(paymentMethodId)
}

class InsertCreditCardDetailUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(detail: CreditCardDetailEntity) {
        repository.insertCreditCardDetail(detail)
    }
}

class GetRevolutDetailUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(paymentMethodId: String): RevolutDetailEntity? =
        repository.getRevolutDetail(paymentMethodId)
}

class InsertRevolutDetailUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(detail: RevolutDetailEntity) {
        repository.insertRevolutDetail(detail)
    }
}

class GetSatispayDetailUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(paymentMethodId: String): SatispayDetailEntity? =
        repository.getSatispayDetail(paymentMethodId)
}

class InsertSatispayDetailUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(detail: SatispayDetailEntity) {
        repository.insertSatispayDetail(detail)
    }
}

class GetPaypalDetailUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(paymentMethodId: String): PaypalDetailEntity? =
        repository.getPaypalDetail(paymentMethodId)
}

class InsertPaypalDetailUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(detail: PaypalDetailEntity) {
        repository.insertPaypalDetail(detail)
    }
}

class GetKlarnaDetailUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(paymentMethodId: String): KlarnaDetailEntity? =
        repository.getKlarnaDetail(paymentMethodId)
}

class InsertKlarnaDetailUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(detail: KlarnaDetailEntity) {
        repository.insertKlarnaDetail(detail)
    }
}

class ManagePaymentMethodUseCase(private val repository: ExpenseRepository) {
    suspend fun add(paymentMethod: PaymentMethodEntity) = repository.insertPaymentMethod(paymentMethod)
    suspend fun update(paymentMethod: PaymentMethodEntity) {
        repository.insertPaymentMethod(paymentMethod)
    }
    suspend fun delete(id: String) = repository.deletePaymentMethod(id)
    suspend fun deleteAll() = repository.deleteAllPaymentMethods()
    suspend fun getPaymentMethodById(id: String): PaymentMethodEntity? = repository.getPaymentMethodById(id)
    suspend fun getAllPaymentMethods(): List<PaymentMethodEntity> = repository.getAllPaymentMethods()
}
