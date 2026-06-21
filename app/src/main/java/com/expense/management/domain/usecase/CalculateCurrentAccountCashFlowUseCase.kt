package com.expense.management.domain.usecase

import com.expense.management.data.AmexPagoFlexScheduledPaymentEntity
import com.expense.management.data.InstallmentScheduledPaymentEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.CurrentAccountCashFlow
import java.time.LocalDate
import java.time.YearMonth

class CalculateCurrentAccountCashFlowUseCase {

    fun execute(
        transactions: List<TransactionEntity>,
        amexScheduledPayments: List<AmexPagoFlexScheduledPaymentEntity>,
        genericScheduledPayments: List<InstallmentScheduledPaymentEntity>,
        targetMonth: YearMonth,
    ): CurrentAccountCashFlow {
        val income = transactions
            .filter { it.type == TransactionType.INCOME && !it.isCreditCard }
            .filter { it.effectiveDate.inMonth(targetMonth) }
            .sumOf { it.amount }
        val transactionOutflow = transactions
            .filter { it.type == TransactionType.EXPENSE && !it.isCreditCard }
            .filter { it.effectiveDate.inMonth(targetMonth) }
            .sumOf { it.amount }
        val pendingAmexOutflow = amexScheduledPayments
            .filter { it.status == "PENDING" && it.expenseTransactionId == null && it.dueDate.inMonth(targetMonth) }
            .sumOf { it.amount }
        val pendingGenericOutflow = genericScheduledPayments
            .filter { it.status == "PENDING" && it.expenseTransactionId == null && it.dueDate.inMonth(targetMonth) }
            .sumOf { it.amount }
        return CurrentAccountCashFlow(
            income = income,
            outflows = transactionOutflow + pendingAmexOutflow + pendingGenericOutflow,
        )
    }

    private fun String.inMonth(month: YearMonth): Boolean = try {
        YearMonth.from(LocalDate.parse(this)) == month
    } catch (_: Exception) {
        false
    }
}
