package com.expense.management.domain.usecase

import com.expense.management.data.AmexPagoFlexPlanEntity
import com.expense.management.data.AmexRevolvingStateEntity
import com.expense.management.data.AmexStatementEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class AmexAutoPayResult(
    val statement: AmexStatementEntity,
    val paymentAmount: Double,
)

class AutoPayAmexStatementsUseCase {

    private val calculateUseCase = CalculateAmexStatementUseCase()

    fun execute(
        statements: List<AmexStatementEntity>,
        pagoFlexPlans: List<AmexPagoFlexPlanEntity>,
        revolvingStates: List<AmexRevolvingStateEntity>,
        today: LocalDate = LocalDate.now(),
    ): List<AmexAutoPayResult> {
        val results = mutableListOf<AmexAutoPayResult>()
        val plansByStatement = pagoFlexPlans.groupBy { it.statementId }
        val revolvingByStatement = revolvingStates.associateBy { it.statementId }

        for (statement in statements) {
            if (statement.isClosed) continue
            val dueDate = try {
                LocalDate.parse(statement.paymentDueDate, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (_: Exception) {
                continue
            }
            if (dueDate.isAfter(today)) continue

            val plans = plansByStatement[statement.id].orEmpty()
            val revolving = revolvingByStatement[statement.id]
            val summary = calculateUseCase.execute(statement, plans, revolving)
            if (summary.paymentAmount > 0.0) {
                results.add(AmexAutoPayResult(statement, summary.paymentAmount))
            }
        }
        return results
    }
}
