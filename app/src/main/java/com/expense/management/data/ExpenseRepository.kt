package com.expense.management.data

import androidx.room.Transaction
import androidx.room.withTransaction
import com.expense.management.domain.model.CycleCharge
import com.expense.management.utils.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val currencyDao: CurrencyDao,
    private val paymentMethodDao: PaymentMethodDao,
    private val amexDao: AmexDao,
    private val database: AppDatabase,
    private val billingCycleDao: BillingCycleDao = database.billingCycleDao(),
) {
    suspend fun <T> withTransaction(block: suspend () -> T): T = database.withTransaction(block)

    // Transactions
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllFlow()

    fun getTransactionsBetween(startDate: String, endDate: String): Flow<List<TransactionEntity>> =
        transactionDao.getBetweenFlow(startDate, endDate)

    suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insert(transaction)
    }

    suspend fun deleteTransaction(id: String) {
        transactionDao.delete(id)
    }

    suspend fun deleteTransactionGroup(groupId: String) {
        transactionDao.deleteByGroupId(groupId)
    }

    @Transaction
    suspend fun deleteTransactionsByIds(ids: List<String>) {
        ids.forEach { id -> transactionDao.delete(id) }
    }

    @Transaction
    suspend fun updateTransactionsCategory(transactions: List<TransactionEntity>) {
        transactions.forEach { tx -> transactionDao.insert(tx) }
    }

    suspend fun getTransactionById(id: String): TransactionEntity? = transactionDao.getById(id)

    suspend fun getCardExpensesWithoutCycle(): List<TransactionEntity> = transactionDao.getCardExpensesWithoutCycle()

    suspend fun getTransactionBySourceRef(sourceRef: String): TransactionEntity? = transactionDao.getBySourceRef(sourceRef)

    /** Collega la transazione al ciclo e porta la sua data di addebito alla scadenza del ciclo. */
    suspend fun setTransactionBillingCycle(transactionId: String, cycleId: String, dueDate: String) {
        transactionDao.setBillingCycle(transactionId, cycleId, dueDate)
    }

    suspend fun insertAllTransactions(transactions: List<TransactionEntity>) {
        transactionDao.insertAll(transactions)
    }

    suspend fun getAllTransactionsList(): List<TransactionEntity> = transactionDao.getAllList()

    suspend fun getDescriptionSuggestions(query: String): List<String> = transactionDao.getDescriptionSuggestions(query)

    fun getTopCategoryIds(type: TransactionType, limit: Int): Flow<List<String>> =
        transactionDao.getTopCategoryIds(type, limit)

    // Categories
    val allCategoriesFlow: Flow<List<CategoryEntity>> = categoryDao.getAllCategoriesFlow()

    suspend fun getAllCategories(): List<CategoryEntity> = categoryDao.getAllCategories()

    suspend fun insertCategory(category: CategoryEntity) {
        categoryDao.insertCategory(category)
    }

    suspend fun deleteCategoryById(id: String) {
        categoryDao.deleteCategoryById(id)
    }

    suspend fun insertAllCategories(categories: List<CategoryEntity>) {
        categoryDao.insertAllCategories(categories)
    }

    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(category)
    }

    // Currencies
    suspend fun getAllCurrencyRates(): List<CurrencyRate> = currencyDao.getAllRates()

    suspend fun getTransactionsByGroupId(groupId: String): List<TransactionEntity> =
        transactionDao.getByGroupId(groupId)

    suspend fun deleteAllTransactions() = transactionDao.deleteAll()
    suspend fun deleteAllCategories() = categoryDao.deleteAll()

    // Payment Methods
    val allPaymentMethods: Flow<List<PaymentMethodEntity>> = paymentMethodDao.getAllPaymentMethodsFlow()

    suspend fun getAllPaymentMethods(): List<PaymentMethodEntity> = paymentMethodDao.getAllPaymentMethods()

    suspend fun getAllCreditCardDetails(): List<CreditCardDetailEntity> = paymentMethodDao.getAllCreditCardDetails()

    suspend fun getAllRevolutDetails(): List<RevolutDetailEntity> = paymentMethodDao.getAllRevolutDetails()

    suspend fun getAllSatispayDetails(): List<SatispayDetailEntity> = paymentMethodDao.getAllSatispayDetails()

    suspend fun getAllPaypalDetails(): List<PaypalDetailEntity> = paymentMethodDao.getAllPaypalDetails()

    suspend fun getAllKlarnaDetails(): List<KlarnaDetailEntity> = paymentMethodDao.getAllKlarnaDetails()

    suspend fun getAllDebitCardDetails(): List<DebitCardDetailEntity> = paymentMethodDao.getAllDebitCardDetails()

    suspend fun getPaymentMethodById(id: String): PaymentMethodEntity? = paymentMethodDao.getPaymentMethodById(id)

    suspend fun insertPaymentMethod(paymentMethod: PaymentMethodEntity) {
        paymentMethodDao.insertPaymentMethod(paymentMethod)
    }

    suspend fun updatePaymentMethod(paymentMethod: PaymentMethodEntity) {
        paymentMethodDao.updatePaymentMethod(paymentMethod)
    }

    @Transaction
    suspend fun deletePaymentMethod(id: String) {
        transactionDao.nullifyPaymentMethodId(id)
        paymentMethodDao.deletePaymentMethod(id)
    }

    suspend fun insertAllPaymentMethods(paymentMethods: List<PaymentMethodEntity>) {
        paymentMethodDao.insertAllPaymentMethods(paymentMethods)
    }

    suspend fun deleteAllPaymentMethods() = paymentMethodDao.deleteAll()

    // Payment Method Details
    suspend fun insertCreditCardDetail(detail: CreditCardDetailEntity) {
        paymentMethodDao.insertCreditCardDetail(detail)
    }

    suspend fun getCreditCardDetail(paymentMethodId: String): CreditCardDetailEntity? =
        paymentMethodDao.getCreditCardDetail(paymentMethodId)

    val allCreditCardDetails: Flow<List<CreditCardDetailEntity>> =
        paymentMethodDao.getAllCreditCardDetailsFlow()

    val allPaypalDetails: Flow<List<PaypalDetailEntity>> =
        paymentMethodDao.getAllPaypalDetailsFlow()

    val allKlarnaDetails: Flow<List<KlarnaDetailEntity>> =
        paymentMethodDao.getAllKlarnaDetailsFlow()

    suspend fun insertRevolutDetail(detail: RevolutDetailEntity) {
        paymentMethodDao.insertRevolutDetail(detail)
    }

    suspend fun getRevolutDetail(paymentMethodId: String): RevolutDetailEntity? =
        paymentMethodDao.getRevolutDetail(paymentMethodId)

    suspend fun insertSatispayDetail(detail: SatispayDetailEntity) {
        paymentMethodDao.insertSatispayDetail(detail)
    }

    suspend fun getSatispayDetail(paymentMethodId: String): SatispayDetailEntity? =
        paymentMethodDao.getSatispayDetail(paymentMethodId)

    suspend fun insertPaypalDetail(detail: PaypalDetailEntity) {
        paymentMethodDao.insertPaypalDetail(detail)
    }

    suspend fun getPaypalDetail(paymentMethodId: String): PaypalDetailEntity? =
        paymentMethodDao.getPaypalDetail(paymentMethodId)

    suspend fun insertKlarnaDetail(detail: KlarnaDetailEntity) {
        paymentMethodDao.insertKlarnaDetail(detail)
    }

    suspend fun getKlarnaDetail(paymentMethodId: String): KlarnaDetailEntity? =
        paymentMethodDao.getKlarnaDetail(paymentMethodId)

    suspend fun insertDebitCardDetail(detail: DebitCardDetailEntity) {
        paymentMethodDao.insertDebitCardDetail(detail)
    }

    suspend fun getDebitCardDetail(paymentMethodId: String): DebitCardDetailEntity? =
        paymentMethodDao.getDebitCardDetail(paymentMethodId)

    suspend fun insertInstallmentPlan(plan: CreditCardInstallmentPlanEntity) {
        paymentMethodDao.insertInstallmentPlan(plan)
    }

    suspend fun getInstallmentPlanByCard(paymentMethodId: String): CreditCardInstallmentPlanEntity? =
        paymentMethodDao.getInstallmentPlanByCard(paymentMethodId)

    val allInstallmentPlans: Flow<List<CreditCardInstallmentPlanEntity>> =
        paymentMethodDao.getAllInstallmentPlansFlow()

    suspend fun getAllInstallmentPlans(): List<CreditCardInstallmentPlanEntity> =
        paymentMethodDao.getAllInstallmentPlans()

    suspend fun updateInstallmentPlanPaidCount(planId: String, paidCount: Int) {
        paymentMethodDao.updateInstallmentPlanPaidCount(planId, paidCount)
    }

    suspend fun deleteInstallmentPlan(planId: String) {
        paymentMethodDao.deleteInstallmentPlan(planId)
    }

    suspend fun insertScheduledPayment(payment: InstallmentScheduledPaymentEntity) {
        paymentMethodDao.insertScheduledPayment(payment)
    }

    suspend fun insertScheduledPayments(payments: List<InstallmentScheduledPaymentEntity>) {
        paymentMethodDao.insertScheduledPayments(payments)
    }

    suspend fun getScheduledPaymentsByPlan(planId: String): List<InstallmentScheduledPaymentEntity> =
        paymentMethodDao.getScheduledPaymentsByPlan(planId)

    fun getScheduledPaymentsByPlanFlow(planId: String): Flow<List<InstallmentScheduledPaymentEntity>> =
        paymentMethodDao.getScheduledPaymentsByPlanFlow(planId)

    val allScheduledPayments: Flow<List<InstallmentScheduledPaymentEntity>> =
        paymentMethodDao.getAllScheduledPaymentsFlow()

    suspend fun getNextPendingScheduledPayment(planId: String): InstallmentScheduledPaymentEntity? =
        paymentMethodDao.getNextPendingScheduledPayment(planId)

    fun getPendingScheduledPaymentsByCardFlow(paymentMethodId: String): Flow<List<InstallmentScheduledPaymentEntity>> =
        paymentMethodDao.getPendingScheduledPaymentsByCardFlow(paymentMethodId)

    suspend fun updateScheduledPaymentStatus(paymentId: String, status: String, expenseTransactionId: String?) {
        paymentMethodDao.updateScheduledPaymentStatus(paymentId, status, expenseTransactionId)
    }

    suspend fun deleteScheduledPaymentsByPlan(planId: String) {
        paymentMethodDao.deleteScheduledPaymentsByPlan(planId)
    }

    suspend fun deletePendingScheduledPaymentsByPlan(planId: String) {
        paymentMethodDao.deletePendingScheduledPaymentsByPlan(planId)
    }

    // AMEX Statements
    suspend fun insertAmexStatement(statement: AmexStatementEntity) {
        amexDao.insertStatement(statement)
    }

    suspend fun getAmexStatementByMonth(paymentMethodId: String, month: String): AmexStatementEntity? =
        amexDao.getStatementByMonth(paymentMethodId, month)

    suspend fun getAmexStatementById(statementId: String): AmexStatementEntity? =
        amexDao.getStatementById(statementId)

    suspend fun getOpenAmexStatementForCard(paymentMethodId: String): AmexStatementEntity? =
        amexDao.getOpenStatementForCard(paymentMethodId)

    fun getAmexStatementsForCardFlow(paymentMethodId: String): Flow<List<AmexStatementEntity>> =
        amexDao.getStatementsForCardFlow(paymentMethodId)

    val allAmexStatements: Flow<List<AmexStatementEntity>> = amexDao.getAllStatementsFlow()

    suspend fun getNonClosedAmexStatements(): List<AmexStatementEntity> =
        amexDao.getNonClosedStatements()

    suspend fun getAllAmexPagoFlexPlans(): List<AmexPagoFlexPlanEntity> =
        amexDao.getAllPagoFlexPlans()

    suspend fun getAllAmexRevolvingStates(): List<AmexRevolvingStateEntity> =
        amexDao.getAllRevolvingStates()

    suspend fun getAmexPagoFlexPlansForPaymentMethod(paymentMethodId: String): List<AmexPagoFlexPlanEntity> =
        amexDao.getPagoFlexPlansForPaymentMethod(paymentMethodId)

    suspend fun closeAmexStatement(statementId: String) {
        amexDao.closeStatement(statementId)
    }

    suspend fun addExpenseToAmexStatement(statementId: String, amount: Double) {
        amexDao.addExpenseToStatement(statementId, amount)
    }

    suspend fun addPagoflexToAmexStatement(statementId: String, amount: Double) {
        amexDao.addPagoflexToStatement(statementId, amount)
    }

    suspend fun updateAmexStatementPayment(statementId: String, mode: String, amount: Double) {
        amexDao.updateStatementPayment(statementId, mode, amount)
    }

    suspend fun deleteAmexStatement(statementId: String) {
        amexDao.deleteStatement(statementId)
    }

    // AMEX PagoFlex
    suspend fun insertAmexPagoFlexPlan(plan: AmexPagoFlexPlanEntity) {
        amexDao.insertPagoFlexPlan(plan)
    }

    suspend fun getAmexPagoFlexPlansForStatement(statementId: String): List<AmexPagoFlexPlanEntity> =
        amexDao.getPagoFlexPlansForStatement(statementId)

    suspend fun getAmexPagoFlexPlanByTransaction(transactionId: String): AmexPagoFlexPlanEntity? =
        amexDao.getPagoFlexPlanByTransaction(transactionId)

    suspend fun getAmexPagoFlexPlanById(planId: String): AmexPagoFlexPlanEntity? =
        amexDao.getPagoFlexPlanById(planId)

    suspend fun updateAmexPagoFlexPaidCount(planId: String, paidCount: Int) {
        amexDao.updatePagoFlexPaidCount(planId, paidCount)
    }

    suspend fun updateAmexPagoFlexPlanCalculation(
        planId: String,
        installmentCount: Int,
        installmentAmount: Double,
        planType: String,
        initialInstallmentAmount: Double?,
    ) {
        amexDao.updatePagoFlexPlanCalculation(
            planId,
            installmentCount,
            installmentAmount,
            planType,
            initialInstallmentAmount,
        )
    }

    suspend fun deleteAmexPagoFlexPlansForStatement(statementId: String) {
        amexDao.deletePagoFlexPlansForStatement(statementId)
    }

    // AMEX Scheduled Payments
    val allAmexScheduledPayments: Flow<List<AmexPagoFlexScheduledPaymentEntity>> = amexDao.getAllScheduledPaymentsFlow()

    suspend fun getAllAmexScheduledPaymentsList(): List<AmexPagoFlexScheduledPaymentEntity> = amexDao.getAllScheduledPaymentsList()

    suspend fun insertAmexScheduledPayments(payments: List<AmexPagoFlexScheduledPaymentEntity>) {
        amexDao.insertAmexScheduledPayments(payments)
    }

    suspend fun updateAmexScheduledPayment(payment: AmexPagoFlexScheduledPaymentEntity) {
        amexDao.updateScheduledPaymentAmount(payment.id, payment.amount)
        payment.expenseTransactionId?.let {
            amexDao.updateScheduledPaymentExpenseTransactionId(payment.id, it)
        }
    }

    suspend fun updateAmexScheduledPaymentAmount(paymentId: String, amount: Double) {
        amexDao.updateScheduledPaymentAmount(paymentId, amount)
    }

    suspend fun updateAmexScheduledPaymentExpenseTransactionId(paymentId: String, transactionId: String) {
        amexDao.updateScheduledPaymentExpenseTransactionId(paymentId, transactionId)
    }

    suspend fun getAmexScheduledPaymentsForPlan(planId: String): List<AmexPagoFlexScheduledPaymentEntity> =
        amexDao.getScheduledPaymentsForPlan(planId)

    suspend fun getAmexScheduledPaymentById(paymentId: String): AmexPagoFlexScheduledPaymentEntity? =
        amexDao.getScheduledPaymentById(paymentId)

    fun getAmexScheduledPaymentsForPlanFlow(planId: String): Flow<List<AmexPagoFlexScheduledPaymentEntity>> =
        amexDao.getScheduledPaymentsForPlanFlow(planId)

    suspend fun getPendingAmexScheduledPaymentsForPlan(planId: String): List<AmexPagoFlexScheduledPaymentEntity> =
        amexDao.getPendingScheduledPaymentsForPlan(planId)

    suspend fun getPendingAmexScheduledPaymentsDueBy(date: String): List<AmexPagoFlexScheduledPaymentEntity> =
        amexDao.getPendingScheduledPaymentsDueBy(date)

    suspend fun getPendingAmexScheduledPaymentsForMonth(month: String): List<AmexPagoFlexScheduledPaymentEntity> =
        amexDao.getPendingScheduledPaymentsForMonth(month)

    fun getPendingAmexScheduledPaymentsForMonthFlow(month: String): Flow<List<AmexPagoFlexScheduledPaymentEntity>> =
        amexDao.getPendingScheduledPaymentsForMonthFlow(month)

    suspend fun markAmexScheduledPaymentAsPaid(paymentId: String, transactionId: String) {
        amexDao.markScheduledPaymentAsPaid(paymentId, transactionId)
    }

    suspend fun deletePendingAmexScheduledPaymentsForPlan(planId: String) {
        amexDao.deletePendingScheduledPaymentsForPlan(planId)
    }

    suspend fun getAmexScheduledPaymentByExpenseTxId(transactionId: String): AmexPagoFlexScheduledPaymentEntity? =
        amexDao.getScheduledPaymentByExpenseTxId(transactionId)

    suspend fun revertAmexScheduledPaymentToPending(paymentId: String) {
        amexDao.revertScheduledPaymentToPending(paymentId)
    }

    suspend fun getGenericScheduledPaymentByExpenseTxId(transactionId: String): InstallmentScheduledPaymentEntity? =
        paymentMethodDao.getScheduledPaymentByExpenseTxId(transactionId)

    suspend fun revertGenericScheduledPaymentToPending(paymentId: String) {
        paymentMethodDao.revertScheduledPaymentToPending(paymentId)
    }

    // AMEX Plan Changes
    suspend fun insertAmexPlanChange(change: AmexPagoFlexPlanChangeEntity) {
        amexDao.insertAmexPlanChange(change)
    }

    suspend fun getAmexPlanChanges(planId: String): List<AmexPagoFlexPlanChangeEntity> =
        amexDao.getPlanChanges(planId)

    // AMEX Revolving
    suspend fun insertAmexRevolvingState(state: AmexRevolvingStateEntity) {
        amexDao.insertRevolvingState(state)
    }

    suspend fun getAmexRevolvingStateForStatement(statementId: String): AmexRevolvingStateEntity? =
        amexDao.getRevolvingStateForStatement(statementId)

    suspend fun updateAmexRevolvingBalance(statementId: String, debt: Double, interest: Double) {
        amexDao.updateRevolvingBalance(statementId, debt, interest)
    }

    suspend fun deleteAmexRevolvingStateForStatement(statementId: String) {
        amexDao.deleteRevolvingStateForStatement(statementId)
    }

    // AMEX Flow queries
    val allAmexPagoFlexPlans: Flow<List<AmexPagoFlexPlanEntity>> = amexDao.getAllPagoFlexPlansFlow()

    fun getAmexPagoFlexPlansForStatementFlow(statementId: String): Flow<List<AmexPagoFlexPlanEntity>> =
        amexDao.getPagoFlexPlansForStatementFlow(statementId)

    val allAmexRevolvingStates: Flow<List<AmexRevolvingStateEntity>> = amexDao.getAllRevolvingStatesFlow()

    // Billing cycles
    /** False se il ciclo esisteva gia': la riga esistente non viene toccata. */
    suspend fun insertBillingCycle(cycle: BillingCycleEntity): Boolean = billingCycleDao.insertCycle(cycle) != -1L

    suspend fun getBillingCycleById(cycleId: String): BillingCycleEntity? = billingCycleDao.getCycleById(cycleId)

    suspend fun getBillingCycleTotals(cycleId: String): CycleTotals = billingCycleDao.getCycleTotals(cycleId)

    suspend fun getUnsettledBillingCycles(paymentMethodId: String): List<BillingCycleWithTotals> =
        billingCycleDao.getUnsettledCyclesWithTotals(paymentMethodId)

    suspend fun getUnsettledCardExposure(paymentMethodId: String, today: String = LocalDate.now().toString()): Double =
        billingCycleDao.getUnsettledExposureByCard(today).firstOrNull { it.paymentMethodId == paymentMethodId }?.exposure ?: 0.0

    /**
     * Esposizione non liquidata per carta; le carte senza cicli aperti non compaiono.
     * Si ricalcola anche quando cambia la data: gli acquisti di oggi entrano senza riavviare l'app.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun unsettledExposureByCard(dates: Flow<LocalDate> = DateUtils.currentDateFlow()): Flow<Map<String, Double>> =
        dates.flatMapLatest { today -> billingCycleDao.getUnsettledExposureByCardFlow(today.toString()) }
            .map { rows -> rows.associate { it.paymentMethodId to it.exposure } }

    /** Cicli non saldati di tutte le carte con il loro totale ad oggi, per il prossimo addebito. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun unsettledCycleCharges(dates: Flow<LocalDate> = DateUtils.currentDateFlow()): Flow<List<CycleCharge>> =
        dates.flatMapLatest { today -> billingCycleDao.getUnsettledCycleTotalsFlow(today.toString()) }
            .map { rows ->
                rows.mapNotNull { row ->
                    val dueDate = runCatching { LocalDate.parse(row.dueDate) }.getOrNull() ?: return@mapNotNull null
                    CycleCharge(row.paymentMethodId, dueDate, row.amount)
                }
            }

    fun billingCyclesForCard(paymentMethodId: String): Flow<List<BillingCycleEntity>> =
        billingCycleDao.getCyclesForCardFlow(paymentMethodId)

    fun cycleTransactionsForCard(paymentMethodId: String): Flow<List<TransactionEntity>> =
        billingCycleDao.getCycleTransactionsForCardFlow(paymentMethodId)

    fun settlementsForCard(paymentMethodId: String): Flow<List<TransactionEntity>> =
        billingCycleDao.getSettlementsForCardFlow(paymentMethodId)

    suspend fun getBillingCyclesToClose(today: String): List<BillingCycleEntity> = billingCycleDao.getCyclesToClose(today)

    suspend fun getBillingCyclesToSettle(today: String): List<BillingCycleEntity> = billingCycleDao.getCyclesToSettle(today)

    suspend fun updateBillingCycleStatus(cycleId: String, status: String) {
        billingCycleDao.updateStatus(cycleId, status)
    }

    suspend fun markBillingCycleSettled(cycleId: String, settlementTransactionId: String?) {
        billingCycleDao.markSettled(cycleId, settlementTransactionId)
    }
}
