package com.expense.management.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AmexDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatement(statement: AmexStatementEntity)

    @Query("SELECT * FROM amex_statements WHERE paymentMethodId = :paymentMethodId AND statementMonth = :month LIMIT 1")
    suspend fun getStatementByMonth(paymentMethodId: String, month: String): AmexStatementEntity?

    @Query("SELECT * FROM amex_statements WHERE id = :statementId LIMIT 1")
    suspend fun getStatementById(statementId: String): AmexStatementEntity?

    @Query("SELECT * FROM amex_statements WHERE paymentMethodId = :paymentMethodId AND isClosed = 0 ORDER BY statementMonth DESC LIMIT 1")
    suspend fun getOpenStatementForCard(paymentMethodId: String): AmexStatementEntity?

    @Query("SELECT * FROM amex_statements WHERE paymentMethodId = :paymentMethodId ORDER BY statementMonth DESC")
    fun getStatementsForCardFlow(paymentMethodId: String): Flow<List<AmexStatementEntity>>

    @Query("SELECT * FROM amex_statements WHERE isClosed = 0 ORDER BY statementMonth DESC")
    suspend fun getNonClosedStatements(): List<AmexStatementEntity>

    @Query("SELECT * FROM amex_statements ORDER BY statementMonth DESC")
    fun getAllStatementsFlow(): Flow<List<AmexStatementEntity>>

    @Query("UPDATE amex_statements SET isClosed = 1 WHERE id = :statementId")
    suspend fun closeStatement(statementId: String)

    @Query("UPDATE amex_statements SET totalExpenses = totalExpenses + :amount WHERE id = :statementId")
    suspend fun addExpenseToStatement(statementId: String, amount: Double)

    @Query("UPDATE amex_statements SET totalPagoflex = totalPagoflex + :amount WHERE id = :statementId")
    suspend fun addPagoflexToStatement(statementId: String, amount: Double)

    @Query("UPDATE amex_statements SET paymentMode = :mode, paymentAmount = :amount WHERE id = :statementId")
    suspend fun updateStatementPayment(statementId: String, mode: String, amount: Double)

    @Query("DELETE FROM amex_statements WHERE id = :statementId")
    suspend fun deleteStatement(statementId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPagoFlexPlan(plan: AmexPagoFlexPlanEntity)

    @Query("SELECT * FROM amex_pagoflex_plans WHERE id = :planId LIMIT 1")
    suspend fun getPagoFlexPlanById(planId: String): AmexPagoFlexPlanEntity?

    @Query("SELECT * FROM amex_pagoflex_plans WHERE statementId = :statementId ORDER BY startDate ASC")
    suspend fun getPagoFlexPlansForStatement(statementId: String): List<AmexPagoFlexPlanEntity>

    @Query("SELECT * FROM amex_pagoflex_plans ORDER BY startDate ASC")
    suspend fun getAllPagoFlexPlans(): List<AmexPagoFlexPlanEntity>

    @Query("SELECT * FROM amex_pagoflex_plans ORDER BY startDate ASC")
    fun getAllPagoFlexPlansFlow(): Flow<List<AmexPagoFlexPlanEntity>>

    @Query("SELECT * FROM amex_pagoflex_plans WHERE statementId = :statementId ORDER BY startDate ASC")
    fun getPagoFlexPlansForStatementFlow(statementId: String): Flow<List<AmexPagoFlexPlanEntity>>

    @Query("SELECT * FROM amex_pagoflex_plans WHERE transactionId = :transactionId LIMIT 1")
    suspend fun getPagoFlexPlanByTransaction(transactionId: String): AmexPagoFlexPlanEntity?

    @Query("SELECT afp.* FROM amex_pagoflex_plans afp INNER JOIN amex_statements afs ON afp.statementId = afs.id WHERE afs.paymentMethodId = :paymentMethodId ORDER BY afp.startDate ASC")
    suspend fun getPagoFlexPlansForPaymentMethod(paymentMethodId: String): List<AmexPagoFlexPlanEntity>

    @Query("UPDATE amex_pagoflex_plans SET paidCount = :paidCount WHERE id = :planId")
    suspend fun updatePagoFlexPaidCount(planId: String, paidCount: Int)

    @Query("UPDATE amex_pagoflex_plans SET installmentCount = :installmentCount, installmentAmount = :installmentAmount, planType = :planType, initialInstallmentAmount = :initialInstallmentAmount WHERE id = :planId")
    suspend fun updatePagoFlexPlanCalculation(
        planId: String,
        installmentCount: Int,
        installmentAmount: Double,
        planType: String,
        initialInstallmentAmount: Double?,
    )

    @Query("DELETE FROM amex_pagoflex_plans WHERE statementId = :statementId")
    suspend fun deletePagoFlexPlansForStatement(statementId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAmexScheduledPayments(payments: List<AmexPagoFlexScheduledPaymentEntity>)

    @Query("SELECT * FROM amex_pagoflex_scheduled_payments WHERE planId = :planId ORDER BY sequenceNumber ASC")
    suspend fun getScheduledPaymentsForPlan(planId: String): List<AmexPagoFlexScheduledPaymentEntity>

    @Query("SELECT * FROM amex_pagoflex_scheduled_payments ORDER BY dueDate ASC")
    fun getAllScheduledPaymentsFlow(): Flow<List<AmexPagoFlexScheduledPaymentEntity>>

    @Query("SELECT * FROM amex_pagoflex_scheduled_payments ORDER BY dueDate ASC")
    suspend fun getAllScheduledPaymentsList(): List<AmexPagoFlexScheduledPaymentEntity>

    @Query("SELECT * FROM amex_pagoflex_scheduled_payments WHERE planId = :planId ORDER BY sequenceNumber ASC")
    fun getScheduledPaymentsForPlanFlow(planId: String): Flow<List<AmexPagoFlexScheduledPaymentEntity>>

    @Query("SELECT * FROM amex_pagoflex_scheduled_payments WHERE planId = :planId AND status = 'PENDING' ORDER BY sequenceNumber ASC")
    suspend fun getPendingScheduledPaymentsForPlan(planId: String): List<AmexPagoFlexScheduledPaymentEntity>

    @Query("SELECT * FROM amex_pagoflex_scheduled_payments WHERE status = 'PENDING' AND strftime('%Y-%m', dueDate) = :month ORDER BY dueDate ASC")
    suspend fun getPendingScheduledPaymentsForMonth(month: String): List<AmexPagoFlexScheduledPaymentEntity>

    @Query("SELECT * FROM amex_pagoflex_scheduled_payments WHERE status = 'PENDING' AND strftime('%Y-%m', dueDate) = :month ORDER BY dueDate ASC")
    fun getPendingScheduledPaymentsForMonthFlow(month: String): Flow<List<AmexPagoFlexScheduledPaymentEntity>>

    @Query("UPDATE amex_pagoflex_scheduled_payments SET status = 'PAID', expenseTransactionId = :transactionId WHERE id = :paymentId")
    suspend fun markScheduledPaymentAsPaid(paymentId: String, transactionId: String)

    @Query("SELECT * FROM amex_pagoflex_scheduled_payments WHERE expenseTransactionId = :transactionId LIMIT 1")
    suspend fun getScheduledPaymentByExpenseTxId(transactionId: String): AmexPagoFlexScheduledPaymentEntity?

    @Query("UPDATE amex_pagoflex_scheduled_payments SET status = 'PENDING', expenseTransactionId = NULL WHERE id = :paymentId")
    suspend fun revertScheduledPaymentToPending(paymentId: String)

    @Query("UPDATE amex_pagoflex_scheduled_payments SET amount = :amount WHERE id = :paymentId")
    suspend fun updateScheduledPaymentAmount(paymentId: String, amount: Double)

    @Query("UPDATE amex_pagoflex_scheduled_payments SET expenseTransactionId = :transactionId WHERE id = :paymentId")
    suspend fun updateScheduledPaymentExpenseTransactionId(paymentId: String, transactionId: String)

    @Query("DELETE FROM amex_pagoflex_scheduled_payments WHERE planId = :planId AND status = 'PENDING'")
    suspend fun deletePendingScheduledPaymentsForPlan(planId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAmexPlanChange(change: AmexPagoFlexPlanChangeEntity)

    @Query("SELECT * FROM amex_pagoflex_plan_changes WHERE planId = :planId ORDER BY changedAt DESC")
    suspend fun getPlanChanges(planId: String): List<AmexPagoFlexPlanChangeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevolvingState(state: AmexRevolvingStateEntity)

    @Query("SELECT * FROM amex_revolving_balances WHERE statementId = :statementId LIMIT 1")
    suspend fun getRevolvingStateForStatement(statementId: String): AmexRevolvingStateEntity?

    @Query("SELECT * FROM amex_revolving_balances ORDER BY statementId ASC")
    suspend fun getAllRevolvingStates(): List<AmexRevolvingStateEntity>

    @Query("SELECT * FROM amex_revolving_balances ORDER BY statementId ASC")
    fun getAllRevolvingStatesFlow(): Flow<List<AmexRevolvingStateEntity>>

    @Query("UPDATE amex_revolving_balances SET carriedForwardDebt = :debt, interestCharged = :interest WHERE statementId = :statementId")
    suspend fun updateRevolvingBalance(statementId: String, debt: Double, interest: Double)

    @Query("DELETE FROM amex_revolving_balances WHERE statementId = :statementId")
    suspend fun deleteRevolvingStateForStatement(statementId: String)
}
