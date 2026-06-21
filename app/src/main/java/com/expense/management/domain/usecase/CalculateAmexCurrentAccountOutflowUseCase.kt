package com.expense.management.domain.usecase

import com.expense.management.data.AmexPagoFlexScheduledPaymentEntity

class CalculateAmexCurrentAccountOutflowUseCase {

    fun execute(
        targetMonth: String,
        scheduledPayments: List<AmexPagoFlexScheduledPaymentEntity>,
    ): Double {
        return scheduledPayments
            .filter { it.dueDate.startsWith(targetMonth) }
            .sumOf { it.amount }
    }
}
