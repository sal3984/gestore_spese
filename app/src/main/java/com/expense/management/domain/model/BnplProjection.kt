package com.expense.management.domain.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate

@Immutable
data class BnplProjection(
    val methodName: String,
    val provider: PaymentProvider,
    val installments: List<BnplInstallment>,
) {
    val totalExpected: Double get() = installments.sumOf { it.amount }
}

@Immutable
data class BnplInstallment(
    val expectedDate: LocalDate,
    val amount: Double,
    val description: String,
    val transactionId: String,
)
