package com.expense.management.domain.model

data class AmexInstallmentPlan(
    val planId: String,
    val statementId: String,
    val transactionId: String,
    val totalAmount: Double,
    val installmentCount: Int,
    val installmentAmount: Double,
    val paidCount: Int,
    val startDate: String,
    val planType: String,
    val initialInstallmentAmount: Double?,
    val installments: List<Installment>,
) {
    data class Installment(
        val id: String,
        val sequenceNumber: Int,
        val dueDate: String,
        val amount: Double,
        val isPaid: Boolean,
    )
}
