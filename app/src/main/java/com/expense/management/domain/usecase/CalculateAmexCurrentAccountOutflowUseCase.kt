package com.expense.management.domain.usecase

import com.expense.management.data.AmexPagoFlexPlanEntity
import com.expense.management.data.AmexPagoFlexScheduledPaymentEntity

class CalculateAmexCurrentAccountOutflowUseCase {

    fun execute(
        targetMonth: String,
        scheduledPayments: List<AmexPagoFlexScheduledPaymentEntity>,
        planEntities: List<AmexPagoFlexPlanEntity>,
    ): Double {
        return scheduledPayments
            .filter { it.status == "PENDING" && it.dueDate.startsWith(targetMonth) }
            .sumOf { it.amount }
    }
}
