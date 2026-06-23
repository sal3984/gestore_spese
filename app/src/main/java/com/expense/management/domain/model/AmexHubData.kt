package com.expense.management.domain.model

import com.expense.management.data.AmexPagoFlexPlanEntity
import com.expense.management.data.AmexPagoFlexScheduledPaymentEntity
import com.expense.management.data.AmexRevolvingStateEntity
import com.expense.management.data.AmexStatementEntity

data class AmexHubData(
    val autoPayEnabled: Boolean,
    val cards: List<AmexCardHubData>,
)

data class AmexCardHubData(
    val cardName: String,
    val paymentMethodId: String,
    val statements: List<AmexStatementWithDetails>,
    val projection: AmexDashboardProjection?,
    val scheduledPayments: List<AmexPagoFlexScheduledPaymentEntity>,
    val currentAccountOutflow: Double,
)

data class AmexStatementWithDetails(
    val statement: AmexStatementEntity,
    val summary: AmexStatementSummary,
    val pagoFlexPlans: List<AmexPagoFlexPlanEntity>,
    val revolvingState: AmexRevolvingStateEntity?,
)
