package com.expense.management.viewmodel

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expense.management.data.AmexPagoFlexPlanEntity
import com.expense.management.data.AmexPagoFlexScheduledPaymentEntity
import com.expense.management.data.AmexRevolvingStateEntity
import com.expense.management.data.AmexStatementEntity
import com.expense.management.data.CreditCardDetailEntity
import com.expense.management.data.ExpenseRepository
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.AmexDashboardProjection
import com.expense.management.domain.model.AmexForecastMonth
import com.expense.management.domain.model.AmexHubData
import com.expense.management.domain.model.AmexInstallmentStrategy
import com.expense.management.domain.model.AmexPaymentMode
import com.expense.management.domain.model.AmexStatementSummary
import com.expense.management.domain.model.PaymentProvider
import com.expense.management.domain.usecase.AutoPayAmexStatementsUseCase
import com.expense.management.domain.usecase.CalculateAmexDashboardProjectionsUseCase
import com.expense.management.domain.usecase.CalculateAmexStatementUseCase
import com.expense.management.domain.usecase.CreateAmexInstallmentPlanUseCase
import com.expense.management.domain.usecase.GetAmexHubDataUseCase
import com.expense.management.domain.usecase.PayAmexStatementUseCase
import com.expense.management.domain.usecase.RecalculateAmexInstallmentPlanUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

sealed interface AmexUiAction {
    data class PayStatement(val statement: AmexStatementEntity, val amount: Double) : AmexUiAction
    data class SetPaymentMode(val statementId: String, val mode: AmexPaymentMode, val amount: Double = 0.0) : AmexUiAction
    data class ToggleAutoPay(val enabled: Boolean) : AmexUiAction
    data class CreateStatement(val paymentMethodId: String, val month: String, val closingDate: String, val paymentDueDate: String) : AmexUiAction
    data class AddExpense(val statementId: String, val amount: Double) : AmexUiAction
    data class AddPagoFlex(
        val paymentMethodId: String,
        val statementMonth: String,
        val transactionId: String,
        val amount: Double,
        val installmentCount: Int,
        val startDate: String,
    ) : AmexUiAction
    data class CreateInstallmentPlan(
        val statementId: String,
        val transactionId: String,
        val totalAmount: Double,
        val startDate: String,
        val strategy: AmexInstallmentStrategy,
    ) : AmexUiAction
    data class RecalculateInstallmentPlan(
        val planId: String,
        val strategy: AmexInstallmentStrategy,
        val currentStatementPaymentDueDate: String,
    ) : AmexUiAction
    data class MarkScheduledPaymentPaid(val paymentId: String, val amount: Double) : AmexUiAction
    data class UpdateScheduledAmount(val paymentId: String, val amount: Double) : AmexUiAction
    data object SyncOutflows : AmexUiAction
    data object TriggerAutoPay : AmexUiAction
    data class CloseStatement(val statementId: String) : AmexUiAction
}

sealed interface AmexUiEvent {
    data class PaymentCompleted(val statementId: String, val paid: Double) : AmexUiEvent
    data class AutoPayTriggered(val paidCount: Int) : AmexUiEvent
    data class StatementCreated(val statementId: String) : AmexUiEvent
    data class InstallmentPlanSaved(val planId: String) : AmexUiEvent
    data class RecalculateFailed(val reason: String) : AmexUiEvent
    data object SyncCompleted : AmexUiEvent
}

class AmexViewModel(
    private val repository: ExpenseRepository,
    private val prefs: SharedPreferences?,
    private val getHubDataUseCase: GetAmexHubDataUseCase = GetAmexHubDataUseCase(),
) : ViewModel() {

    companion object {
        private const val KEY_AMEX_AUTO_PAY = "amex_auto_pay_enabled"
    }

    // Base flows
    val allAmexStatements: StateFlow<List<AmexStatementEntity>> =
        repository.allAmexStatements
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAmexPagoFlexPlans: StateFlow<List<AmexPagoFlexPlanEntity>> =
        repository.allAmexPagoFlexPlans
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAmexRevolvingStates: StateFlow<List<AmexRevolvingStateEntity>> =
        repository.allAmexRevolvingStates
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAmexScheduledPayments: StateFlow<List<AmexPagoFlexScheduledPaymentEntity>> =
        repository.allAmexScheduledPayments
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPaymentMethods: StateFlow<List<PaymentMethodEntity>> =
        repository.allPaymentMethods
            .map { methods ->
                val cash = PaymentMethodEntity(
                    id = "__cash__",
                    name = "Contante",
                    provider = PaymentProvider.CASH,
                    isActive = true,
                )
                methods + cash
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCreditCardDetails: StateFlow<List<CreditCardDetailEntity>> =
        repository.allCreditCardDetails
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    fun updateSelectedMonth(month: YearMonth) {
        _selectedMonth.value = month
    }

    val amexHubData: StateFlow<AmexHubData> = combine(
        combine(allPaymentMethods, allAmexStatements, allAmexPagoFlexPlans, allAmexRevolvingStates, allAmexScheduledPayments) { args ->
            @Suppress("UNCHECKED_CAST")
            HubDataInput(
                methods = args[0] as List<PaymentMethodEntity>,
                statements = args[1] as List<AmexStatementEntity>,
                plans = args[2] as List<AmexPagoFlexPlanEntity>,
                revolving = args[3] as List<AmexRevolvingStateEntity>,
                scheduledPayments = args[4] as List<AmexPagoFlexScheduledPaymentEntity>,
            )
        },
        _selectedMonth,
    ) { input, month ->
        val amexMethods = input.methods.filter { it.provider == PaymentProvider.CREDIT_CARD_AMEX }
        val projections = CalculateAmexDashboardProjectionsUseCase().execute(
            targetMonth = month,
            allPaymentMethods = input.methods,
            allStatements = input.statements,
            allPagoFlexPlans = input.plans,
            allRevolvingStates = input.revolving,
        )
        getHubDataUseCase.execute(
            amexPaymentMethods = amexMethods,
            allStatements = input.statements,
            allPagoFlexPlans = input.plans,
            allRevolvingStates = input.revolving,
            allScheduledPayments = input.scheduledPayments,
            projections = projections,
            isAutoPayEnabled = _isAmexAutoPayEnabled.value,
            targetMonth = month,
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AmexHubData(autoPayEnabled = true, cards = emptyList()))

    val amexDashboardProjections: StateFlow<List<AmexDashboardProjection>> = combine(
        _selectedMonth,
        allPaymentMethods,
        allAmexStatements,
        allAmexPagoFlexPlans,
        allAmexRevolvingStates,
    ) { month, methods, statements, plans, revolving ->
        CalculateAmexDashboardProjectionsUseCase().execute(
            targetMonth = month,
            allPaymentMethods = methods,
            allStatements = statements,
            allPagoFlexPlans = plans,
            allRevolvingStates = revolving,
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAmexAutoPayEnabled = MutableStateFlow(
        prefs?.getBoolean(KEY_AMEX_AUTO_PAY, true) ?: true,
    )
    val isAmexAutoPayEnabled: StateFlow<Boolean> = _isAmexAutoPayEnabled.asStateFlow()

    private val _payInProgressFor = MutableStateFlow<String?>(null)
    val payInProgressFor: StateFlow<String?> = _payInProgressFor.asStateFlow()

    private val _events = Channel<AmexUiEvent>(Channel.BUFFERED)
    val events: Flow<AmexUiEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (prefs?.getBoolean(KEY_AMEX_AUTO_PAY, true) != false) {
                runAmexAutoPay()
            }
            syncAmexCurrentAccountOutflows()
        }
    }

    fun onAction(action: AmexUiAction) {
        when (action) {
            is AmexUiAction.PayStatement -> payAmexStatement(action.statement, action.amount)
            is AmexUiAction.SetPaymentMode -> setAmexPaymentMode(action.statementId, action.mode, action.amount)
            is AmexUiAction.ToggleAutoPay -> toggleAmexAutoPay(action.enabled)
            is AmexUiAction.CreateStatement -> createAmexStatement(action.paymentMethodId, action.month, action.closingDate, action.paymentDueDate)
            is AmexUiAction.AddExpense -> addExpenseToAmexStatement(action.statementId, action.amount)
            is AmexUiAction.AddPagoFlex -> addPagoFlexToAmexStatement(action.paymentMethodId, action.statementMonth, action.transactionId, action.amount, action.installmentCount, action.startDate)
            is AmexUiAction.CreateInstallmentPlan -> createAmexInstallmentPlan(action.statementId, action.transactionId, action.totalAmount, action.startDate, action.strategy)
            is AmexUiAction.RecalculateInstallmentPlan -> recalculateAmexInstallmentPlan(action.planId, action.strategy, action.currentStatementPaymentDueDate)
            is AmexUiAction.MarkScheduledPaymentPaid -> updateAmexScheduledAmountAndPay(action.paymentId, action.amount)
            is AmexUiAction.UpdateScheduledAmount -> updateAmexScheduledPaymentAmount(action.paymentId, action.amount)
            is AmexUiAction.SyncOutflows -> syncAmexCurrentAccountOutflows()
            is AmexUiAction.TriggerAutoPay -> triggerAmexAutoPay()
            is AmexUiAction.CloseStatement -> closeAmexStatement(action.statementId)
        }
    }

    fun toggleAmexAutoPay(enabled: Boolean) {
        _isAmexAutoPayEnabled.value = enabled
        prefs?.edit { putBoolean(KEY_AMEX_AUTO_PAY, enabled) }
    }

    fun getAmexStatementsForCard(paymentMethodId: String): Flow<List<AmexStatementEntity>> =
        repository.getAmexStatementsForCardFlow(paymentMethodId)

    fun getAmexPagoFlexPlansForStatement(statementId: String): Flow<List<AmexPagoFlexPlanEntity>> =
        repository.getAmexPagoFlexPlansForStatementFlow(statementId)

    fun getAmexStatementSummary(
        statement: AmexStatementEntity,
        pagoFlexPlans: List<AmexPagoFlexPlanEntity>,
        revolvingState: AmexRevolvingStateEntity?,
        interestRate: Double = 0.0,
    ): AmexStatementSummary {
        return CalculateAmexStatementUseCase().execute(statement, pagoFlexPlans, revolvingState, interestRate)
    }

    fun generateAmexForecast(
        statementMonth: String,
        pagoFlexPlans: List<AmexPagoFlexPlanEntity>,
        revolvingState: AmexRevolvingStateEntity?,
        paymentMode: AmexPaymentMode,
        paymentAmount: Double,
        interestRate: Double,
        monthsToForecast: Int = 3,
    ): List<AmexForecastMonth> {
        val useCase = com.expense.management.domain.usecase.GenerateAmexForecastUseCase()
        return useCase.execute(statementMonth, pagoFlexPlans, revolvingState, paymentMode, paymentAmount, interestRate, monthsToForecast)
    }

    fun createAmexStatement(paymentMethodId: String, statementMonth: String, closingDate: String, paymentDueDate: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = YearMonth.now()
            val month = statementMonth.ifEmpty { now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")) }
            val existing = repository.getAmexStatementByMonth(paymentMethodId, month)
            if (existing == null) {
                val today = java.time.LocalDate.now()
                val statement = AmexStatementEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    paymentMethodId = paymentMethodId,
                    statementMonth = month,
                    closingDate = closingDate.ifEmpty { today.withDayOfMonth(28).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) },
                    paymentDueDate = paymentDueDate.ifEmpty { today.plusDays(15).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) },
                    paymentMode = AmexPaymentMode.SALDO.name,
                )
                repository.insertAmexStatement(statement)
                _events.send(AmexUiEvent.StatementCreated(statement.id))
            }
        }
    }

    fun addExpenseToAmexStatement(statementId: String, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addExpenseToAmexStatement(statementId, amount)
        }
    }

    fun addPagoFlexToAmexStatement(
        paymentMethodId: String,
        statementMonth: String,
        transactionId: String,
        amount: Double,
        installmentCount: Int,
        startDate: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val today = java.time.LocalDate.now()
            val statement = repository.getAmexStatementByMonth(paymentMethodId, statementMonth)
                ?: AmexStatementEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    paymentMethodId = paymentMethodId,
                    statementMonth = statementMonth,
                    closingDate = today.withDayOfMonth(today.lengthOfMonth()).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE),
                    paymentDueDate = today.plusDays(15).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE),
                    paymentMode = AmexPaymentMode.SALDO.name,
                ).also { repository.insertAmexStatement(it) }
            repository.addExpenseToAmexStatement(statement.id, amount)
            repository.addPagoflexToAmexStatement(statement.id, amount)
            val strategy = AmexInstallmentStrategy.FixedDuration(installmentCount)
            val (plan, payments) = CreateAmexInstallmentPlanUseCase().execute(
                planId = java.util.UUID.randomUUID().toString(),
                statementId = statement.id,
                transactionId = transactionId,
                totalAmount = amount,
                startDate = startDate,
                strategy = strategy,
            )
            repository.insertAmexPagoFlexPlan(plan)
            repository.insertAmexScheduledPayments(payments)
        }
    }

    fun createAmexInstallmentPlan(
        statementId: String,
        transactionId: String,
        totalAmount: Double,
        startDate: String,
        strategy: AmexInstallmentStrategy,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val (plan, payments) = CreateAmexInstallmentPlanUseCase().execute(
                planId = java.util.UUID.randomUUID().toString(),
                statementId = statementId,
                transactionId = transactionId,
                totalAmount = totalAmount,
                startDate = startDate,
                strategy = strategy,
            )
            repository.insertAmexPagoFlexPlan(plan)
            repository.insertAmexScheduledPayments(payments)
            _events.send(AmexUiEvent.InstallmentPlanSaved(plan.id))
        }
    }

    fun recalculateAmexInstallmentPlan(
        planId: String,
        strategy: AmexInstallmentStrategy,
        currentStatementPaymentDueDate: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            recalculateAmexInstallmentPlanInternal(planId, strategy, currentStatementPaymentDueDate)
        }
    }

    private suspend fun recalculateAmexInstallmentPlanInternal(
        planId: String,
        strategy: AmexInstallmentStrategy,
        currentStatementPaymentDueDate: String,
    ) {
        val plan = repository.getAmexPagoFlexPlanById(planId) ?: return
        val existingPayments = repository.getAmexScheduledPaymentsForPlan(planId)
        val syncedPending = existingPayments
            .filter { it.status == "PENDING" && it.expenseTransactionId != null }
        val syncedByMonth = syncedPending.associate { it.dueDate.take(7) to it.expenseTransactionId!! }
        val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        val (updatedPlan, newPendingPayments) = RecalculateAmexInstallmentPlanUseCase().execute(
            existingPlan = plan,
            existingPayments = existingPayments,
            newStrategy = strategy,
            today = today,
            currentStatementPaymentDueDate = currentStatementPaymentDueDate,
        )
        repository.updateAmexPagoFlexPlanCalculation(
            planId = updatedPlan.id,
            installmentCount = updatedPlan.installmentCount,
            installmentAmount = updatedPlan.installmentAmount,
            planType = updatedPlan.planType,
            initialInstallmentAmount = updatedPlan.initialInstallmentAmount,
        )
        repository.deletePendingAmexScheduledPaymentsForPlan(planId)
        repository.insertAmexScheduledPayments(newPendingPayments)

        val linkedTxIds = mutableSetOf<String>()
        for (newPayment in newPendingPayments) {
            val monthKey = newPayment.dueDate.take(7)
            val oldTxId = syncedByMonth[monthKey] ?: continue
            repository.updateAmexScheduledPaymentExpenseTransactionId(newPayment.id, oldTxId)
            val tx = repository.getTransactionById(oldTxId) ?: continue
            repository.insertTransaction(tx.copy(amount = newPayment.amount, originalAmount = newPayment.amount, effectiveDate = newPayment.dueDate))
            linkedTxIds.add(oldTxId)
        }
        for (orphanTxId in syncedByMonth.values.filter { it !in linkedTxIds }) {
            repository.deleteTransaction(orphanTxId)
        }
    }

    fun syncAmexCurrentAccountOutflows() {
        viewModelScope.launch(Dispatchers.IO) {
            val today = java.time.LocalDate.now()
            val monthPrefix = YearMonth.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
            val plans = repository.getAllAmexPagoFlexPlans()
            val payments = repository.getPendingAmexScheduledPaymentsForMonth(monthPrefix)
            val ccDetails = repository.getAllCreditCardDetails()
            val touchedPlans = mutableSetOf<String>()
            for (payment in payments) {
                if (payment.expenseTransactionId != null) continue
                val dueDate = try {
                    java.time.LocalDate.parse(payment.dueDate, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                } catch (_: Exception) {
                    continue
                }
                if (dueDate.isAfter(today)) continue
                val plan = plans.find { it.id == payment.planId } ?: continue
                val statement = repository.getAmexStatementById(plan.statementId) ?: continue
                val detail = ccDetails.find { it.paymentMethodId == statement.paymentMethodId }
                val tx = TransactionEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    date = payment.dueDate,
                    description = "Rata Amex ${payment.sequenceNumber}/${plan.installmentCount}",
                    amount = payment.amount,
                    categoryId = "credit_card_payment",
                    type = TransactionType.EXPENSE,
                    isCreditCard = false,
                    originalAmount = payment.amount,
                    originalCurrency = "€",
                    effectiveDate = payment.dueDate,
                    installmentNumber = payment.sequenceNumber,
                    totalInstallments = plan.installmentCount,
                    groupId = plan.id,
                    paymentMethodId = detail?.linkedPaymentMethodId,
                )
                repository.insertTransaction(tx)
                repository.markAmexScheduledPaymentAsPaid(payment.id, tx.id)
                touchedPlans.add(plan.id)
            }
            for (planId in touchedPlans) {
                val paidCount = repository.getAmexScheduledPaymentsForPlan(planId).count { it.status == "PAID" }
                repository.updateAmexPagoFlexPaidCount(planId, paidCount)
            }
            _events.send(AmexUiEvent.SyncCompleted)
        }
    }

    fun setAmexPaymentMode(statementId: String, mode: AmexPaymentMode, amount: Double = 0.0) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateAmexStatementPayment(statementId, mode.name, amount)
        }
    }

    fun closeAmexStatement(statementId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.closeAmexStatement(statementId)
        }
    }

    fun payAmexStatement(statement: AmexStatementEntity, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            _payInProgressFor.value = statement.id
            try {
                val paymentDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                val statementPlans = repository.getAmexPagoFlexPlansForStatement(statement.id)
                val scheduledPayments = statementPlans.flatMap { repository.getAmexScheduledPaymentsForPlan(it.id) }
                val detail = repository.getCreditCardDetail(statement.paymentMethodId)
                val linkedId = detail?.linkedPaymentMethodId
                val result = PayAmexStatementUseCase().execute(statement, amount, paymentDate, statementPlans, scheduledPayments, linkedId)
                result.paymentTransactions.forEach { repository.insertTransaction(it) }
                result.incomeTransaction?.let { repository.insertTransaction(it) }
                result.paidInstallments.forEachIndexed { i, payment ->
                    val tx = result.paymentTransactions.getOrNull(i)
                    if (tx != null) {
                        if (tx.amount != payment.amount) {
                            repository.updateAmexScheduledPaymentAmount(payment.id, tx.amount)
                        }
                        repository.markAmexScheduledPaymentAsPaid(payment.id, tx.id)
                    }
                }
                updateAmexPlanPaidCounts(statementPlans)
                autoRecalculateAmexPlans(statementPlans)
                repository.closeAmexStatement(statement.id)
                _events.send(AmexUiEvent.PaymentCompleted(statement.id, amount))
            } finally {
                _payInProgressFor.value = null
            }
        }
    }

    private suspend fun updateAmexPlanPaidCounts(plans: List<AmexPagoFlexPlanEntity>) {
        for (plan in plans) {
            val paidCount = repository.getAmexScheduledPaymentsForPlan(plan.id).count { it.status == "PAID" }
            repository.updateAmexPagoFlexPaidCount(plan.id, paidCount)
        }
    }

    private suspend fun autoRecalculateAmexPlans(plans: List<AmexPagoFlexPlanEntity>) {
        for (plan in plans) {
            val updatedPlan = repository.getAmexPagoFlexPlanById(plan.id) ?: continue
            val newPaidCount = repository.getAmexScheduledPaymentsForPlan(plan.id).count { it.status == "PAID" }
            val remainingMonths = updatedPlan.installmentCount - newPaidCount
            if (remainingMonths <= 0) continue
            val statement = repository.getAmexStatementById(updatedPlan.statementId) ?: continue
            val strategy = if (updatedPlan.planType == "FIXED_AMOUNT") {
                AmexInstallmentStrategy.FixedAmount(updatedPlan.initialInstallmentAmount ?: updatedPlan.installmentAmount)
            } else {
                AmexInstallmentStrategy.FixedDuration(remainingMonths)
            }
            recalculateAmexInstallmentPlanInternal(updatedPlan.id, strategy, statement.paymentDueDate)
        }
    }

    fun triggerAmexAutoPay() {
        viewModelScope.launch(Dispatchers.IO) {
            runAmexAutoPay()
            syncAmexCurrentAccountOutflows()
        }
    }

    private suspend fun runAmexAutoPay() {
        val statements = repository.getNonClosedAmexStatements()
        if (statements.isEmpty()) return
        val plans = repository.getAllAmexPagoFlexPlans()
        val revolving = repository.getAllAmexRevolvingStates()
        val useCase = AutoPayAmexStatementsUseCase()
        val dueStatements = useCase.execute(statements, plans, revolving)
        val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        val allPayments = repository.getAllAmexScheduledPaymentsList()
        var paidCount = 0
        for (due in dueStatements) {
            val dueDate = try {
                java.time.LocalDate.parse(due.statement.paymentDueDate, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (_: Exception) {
                continue
            }
            if (dueDate.isAfter(java.time.LocalDate.now())) continue
            val statementPlans = repository.getAmexPagoFlexPlansForStatement(due.statement.id)
            val scheduledPayments = statementPlans.flatMap { plan ->
                allPayments.filter { it.planId == plan.id }
            }
            val detail = repository.getCreditCardDetail(due.statement.paymentMethodId)
            val linkedId = detail?.linkedPaymentMethodId
            val result = PayAmexStatementUseCase().execute(due.statement, due.paymentAmount, today, statementPlans, scheduledPayments, linkedId)
            result.paymentTransactions.forEach { repository.insertTransaction(it) }
            result.incomeTransaction?.let { repository.insertTransaction(it) }
            result.paidInstallments.forEachIndexed { i, payment ->
                val tx = result.paymentTransactions.getOrNull(i)
                if (tx != null) {
                    if (tx.amount != payment.amount) {
                        repository.updateAmexScheduledPaymentAmount(payment.id, tx.amount)
                    }
                    repository.markAmexScheduledPaymentAsPaid(payment.id, tx.id)
                }
            }
            updateAmexPlanPaidCounts(statementPlans)
            autoRecalculateAmexPlans(statementPlans)
            repository.closeAmexStatement(due.statement.id)
            paidCount++
        }
        if (paidCount > 0) {
            _events.send(AmexUiEvent.AutoPayTriggered(paidCount))
        }
    }

    private fun updateAmexScheduledAmountAndPay(paymentId: String, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateAmexScheduledPaymentAmount(paymentId, amount)
        }
    }

    private fun updateAmexScheduledPaymentAmount(paymentId: String, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateAmexScheduledPaymentAmount(paymentId, amount)
        }
    }
}

private data class HubDataInput(
    val methods: List<PaymentMethodEntity>,
    val statements: List<AmexStatementEntity>,
    val plans: List<AmexPagoFlexPlanEntity>,
    val revolving: List<AmexRevolvingStateEntity>,
    val scheduledPayments: List<AmexPagoFlexScheduledPaymentEntity>,
)
