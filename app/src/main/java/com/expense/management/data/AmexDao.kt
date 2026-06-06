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

    @Query("DELETE FROM amex_pagoflex_plans WHERE statementId = :statementId")
    suspend fun deletePagoFlexPlansForStatement(statementId: String)

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
