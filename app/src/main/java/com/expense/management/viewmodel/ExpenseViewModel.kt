package com.expense.management.viewmodel

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expense.management.data.AmexPagoFlexPlanEntity
import com.expense.management.data.AmexPagoFlexScheduledPaymentEntity
import com.expense.management.data.AmexRevolvingStateEntity
import com.expense.management.data.AmexStatementEntity
import com.expense.management.data.BackupData
import com.expense.management.data.CategoryEntity
import com.expense.management.data.CurrencyRate
import com.expense.management.data.ExpenseRepository
import com.expense.management.data.InstallmentScheduledPaymentEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.data.toData
import com.expense.management.domain.model.ActiveCreditCard
import com.expense.management.domain.model.AmexStatementSummary
import com.expense.management.domain.model.CATEGORIES
import com.expense.management.domain.model.CreditCardType
import com.expense.management.domain.model.CurrentAccountCashFlow
import com.expense.management.domain.model.DashboardWidget
import com.expense.management.domain.model.DeleteType
import com.expense.management.domain.model.ReceiptScanResult
import com.expense.management.domain.model.ReportData
import com.expense.management.domain.usecase.CalculateAmexCurrentAccountOutflowUseCase
import com.expense.management.domain.usecase.CalculateAmexStatementUseCase
import com.expense.management.domain.usecase.CalculateCurrentAccountCashFlowUseCase
import com.expense.management.domain.usecase.CalculateReportUseCase
import com.expense.management.domain.usecase.DeleteTransactionUseCase
import com.expense.management.domain.usecase.GenerateCreditCardPaymentUseCase
import com.expense.management.domain.usecase.GetActiveCreditCardsUseCase
import com.expense.management.domain.usecase.GetBackupDataUseCase
import com.expense.management.domain.usecase.GetCategoriesUseCase
import com.expense.management.domain.usecase.GetFrequentCategoriesUseCase
import com.expense.management.domain.usecase.GetTransactionsUseCase
import com.expense.management.domain.usecase.InitializeCategoriesUseCase
import com.expense.management.domain.usecase.RestoreDataUseCase
import com.expense.management.domain.usecase.SaveTransactionUseCase
import com.expense.management.domain.usecase.ScanReceiptUseCase
import com.expense.management.ui.theme.AppStyle
import com.expense.management.utils.CurrencyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModel(
    private val repository: ExpenseRepository,
    private val currencyUtils: CurrencyUtils,
    private val prefs: SharedPreferences?,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val saveTransactionUseCase: SaveTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val initializeCategoriesUseCase: InitializeCategoriesUseCase,
    private val getActiveCreditCardsUseCase: GetActiveCreditCardsUseCase,
    private val getBackupDataUseCase: GetBackupDataUseCase,
    private val restoreDataUseCase: RestoreDataUseCase,
    private val getFrequentCategoriesUseCase: GetFrequentCategoriesUseCase,
    private val calculateReportUseCase: CalculateReportUseCase,
    private val scanReceiptUseCase: ScanReceiptUseCase,
    private val generateCreditCardPaymentUseCase: GenerateCreditCardPaymentUseCase,
    private val calculateAmexCurrentAccountOutflowUseCase: CalculateAmexCurrentAccountOutflowUseCase,
    private val calculateCurrentAccountCashFlowUseCase: CalculateCurrentAccountCashFlowUseCase,
    private val calculateAmexStatementUseCase: CalculateAmexStatementUseCase,
) : ViewModel() {

    companion object {
        private const val KEY_CURRENCY = "currency"
        private const val KEY_DATE_FORMAT = "date_format"
        private const val KEY_HIDE_AMOUNT = "hide_amount"
        private const val KEY_BIOMETRIC_ENABLED = "is_biometric_enabled"
        private const val KEY_CC_PAYMENT_MODE = "cc_payment_mode"
        private const val KEY_CSV_EXPORT_COLUMNS = "csv_export_columns"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_APP_STYLE = "app_style"
        private const val KEY_ENABLED_WIDGETS = "enabled_widgets"
    }

    // Frequent Categories Caching
    private val frequentExpenses = getFrequentCategoriesUseCase(com.expense.management.domain.model.TransactionType.EXPENSE)
        .map { it.map { category -> category.toData() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val frequentIncome = getFrequentCategoriesUseCase(com.expense.management.domain.model.TransactionType.INCOME)
        .map { it.map { category -> category.toData() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getFrequentCategories(type: TransactionType): StateFlow<List<CategoryEntity>> =
        if (type == TransactionType.EXPENSE) frequentExpenses else frequentIncome

    // MODIFICA: Inizializza lo stato di sblocco in base alla preferenza.
    private val _isAppUnlocked = MutableStateFlow(!(prefs?.getBoolean(KEY_BIOMETRIC_ENABLED, false) ?: false))
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    fun unlockApp() {
        _isAppUnlocked.value = true
    }

    // Dati Transazioni
    val allTransactions: StateFlow<List<TransactionEntity>> =
        getTransactionsUseCase()
            .map { it.map { transaction -> transaction.toData() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val reportRangeState = MutableStateFlow(
        Pair(YearMonth.now().minusMonths(2), YearMonth.now()),
    )

    val reportTransactions: StateFlow<List<TransactionEntity>> = combine(
        repository.allTransactions,
        reportRangeState,
    ) { allTx, (start, end) ->
        allTx.filter { tx ->
            try {
                val date = LocalDate.parse(tx.effectiveDate)
                !date.isBefore(start.atDay(1)) && !date.isAfter(end.atEndOfMonth())
            } catch (_: Exception) {
                false
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setReportRange(start: YearMonth, end: YearMonth) {
        reportRangeState.value = start to end
    }

    // DATI CATEGORIE
    val allCategories: StateFlow<List<CategoryEntity>> =
        getCategoriesUseCase()
            .map { it.map { category -> category.toData() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reportData: StateFlow<ReportData> = combine(
        reportTransactions,
        allCategories,
        reportRangeState,
    ) { tx, cats, range ->
        calculateReportUseCase.execute(tx, cats, range.first, range.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportData.EMPTY)

    val activeCreditCards: StateFlow<List<ActiveCreditCard>> =
        getActiveCreditCardsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentDashboardMonth = MutableStateFlow(YearMonth.now())
    val currentDashboardMonth = _currentDashboardMonth.asStateFlow()

    val dashboardFilteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        _currentDashboardMonth,
        activeCreditCards,
    ) { tx, month, cards ->
        val amexCardIds = cards
            .filter { it.cardType == CreditCardType.AMEX_HYBRID }
            .map { it.id }
            .toSet()
        tx.filter { transaction ->
            val isAmexCreditExpense = transaction.isCreditCard &&
                transaction.type == TransactionType.EXPENSE &&
                (transaction.creditCardId in amexCardIds || transaction.paymentMethodId in amexCardIds)
            !isAmexCreditExpense && try {
                YearMonth.from(LocalDate.parse(transaction.effectiveDate)) == month
            } catch (_: Exception) {
                false
            }
        }.sortedByDescending { it.effectiveDate }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- STATO IMPOSTAZIONI ---
    private val _currency = MutableStateFlow(prefs?.getString(KEY_CURRENCY, "€") ?: "€")
    val currency = _currency.asStateFlow()

    private val _dateFormat = MutableStateFlow(prefs?.getString(KEY_DATE_FORMAT, "dd/MM/yyyy") ?: "dd/MM/yyyy")
    val dateFormat = _dateFormat.asStateFlow()

    private val _isAmountHidden = MutableStateFlow(prefs?.getBoolean(KEY_HIDE_AMOUNT, false) ?: false)
    val isAmountHidden = _isAmountHidden.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs?.getBoolean(KEY_BIOMETRIC_ENABLED, false) ?: false)
    val isBiometricEnabled = _isBiometricEnabled.asStateFlow()

    private val _ccPaymentMode = MutableStateFlow(prefs?.getString(KEY_CC_PAYMENT_MODE, "single") ?: "single")
    val ccPaymentMode = _ccPaymentMode.asStateFlow()

    val earliestMonth: StateFlow<YearMonth> = repository.allTransactions
        .map { transactions ->
            transactions.minOfOrNull { tx ->
                try {
                    LocalDate.parse(tx.effectiveDate)
                } catch (_: Exception) {
                    LocalDate.now()
                }
            }?.let { YearMonth.from(it) } ?: YearMonth.now()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), YearMonth.now())

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    private val _currencyRatesUpdate = MutableStateFlow<Long?>(null)
    val currencyRatesUpdate = _currencyRatesUpdate.asStateFlow()

    // Stato Tassi di Cambio per UI
    private val _currencyRates = MutableStateFlow<List<CurrencyRate>>(emptyList())
    val currencyRates = _currencyRates.asStateFlow()

    val defaultExportColumns = setOf(
        "ID", "Data", "Descrizione", "ImportoConvertito", "ImportoOriginale",
        "ValutaOriginale", "Categoria", "Tipo", "CartaDiCredito", "DataAddebito",
    )

    private val _csvExportColumns = MutableStateFlow(
        prefs?.getStringSet(KEY_CSV_EXPORT_COLUMNS, defaultExportColumns) ?: defaultExportColumns,
    )
    val csvExportColumns = _csvExportColumns.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs?.getString(KEY_THEME_MODE, "system") ?: "system")
    val themeMode = _themeMode.asStateFlow()

    private val _appStyle = MutableStateFlow(
        prefs?.getString(KEY_APP_STYLE, null)?.let {
            try {
                AppStyle.valueOf(it)
            } catch (_: IllegalArgumentException) {
                AppStyle.MATERIAL_YOU
            }
        } ?: AppStyle.MATERIAL_YOU,
    )
    val appStyle = _appStyle.asStateFlow()

    private val _enabledWidgets = MutableStateFlow(
        prefs?.getStringSet(KEY_ENABLED_WIDGETS, null)?.let { saved ->
            saved.mapNotNull {
                try {
                    DashboardWidget.valueOf(it)
                } catch (_: IllegalArgumentException) {
                    null
                }
            }.toSet()
        } ?: DashboardWidget.entries.toSet(),
    )
    val enabledWidgets = _enabledWidgets.asStateFlow()

    init {
        viewModelScope.launch {
            initializeCategoriesUseCase()
        }
        viewModelScope.launch {
            _currencyRatesUpdate.value = currencyUtils.getLastUpdate()
            refreshCurrencyRatesData()
        }
    }

    val allScheduledPayments: StateFlow<List<InstallmentScheduledPaymentEntity>> =
        repository.allScheduledPayments
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AMEX STATEMENTS
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

    val amexCurrentAccountOutflow: StateFlow<Double> = combine(
        _currentDashboardMonth,
        allAmexScheduledPayments,
    ) { month, payments ->
        val monthPrefix = month.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
        calculateAmexCurrentAccountOutflowUseCase.execute(monthPrefix, payments)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentAccountCashFlow: StateFlow<CurrentAccountCashFlow> = combine(
        allTransactions,
        allAmexScheduledPayments,
        allScheduledPayments,
        _currentDashboardMonth,
    ) { tx, amexPayments, genericPayments, month ->
        calculateCurrentAccountCashFlowUseCase.execute(tx, amexPayments, genericPayments, month)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CurrentAccountCashFlow(0.0, 0.0))

    val currentAccountIncomeForMonth: StateFlow<Double> = currentAccountCashFlow.map { it.income }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentAccountOutflowsForMonth: StateFlow<Double> = currentAccountCashFlow.map { it.outflows }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val amexStatementSummaries: StateFlow<Map<String, AmexStatementSummary>> = combine(
        allAmexStatements,
        allAmexPagoFlexPlans,
        allAmexRevolvingStates,
    ) { statements, plans, revolving ->
        val plansByStatement = plans.groupBy { it.statementId }
        val revolvingByStatement = revolving.associateBy { it.statementId }
        statements.associateWith { stmt ->
            calculateAmexStatementUseCase.execute(
                statement = stmt,
                pagoFlexPlans = plansByStatement[stmt.id].orEmpty(),
                revolvingState = revolvingByStatement[stmt.id],
            )
        }.mapKeys { it.key.id }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val amexPendingByCard: StateFlow<Map<String, Double>> = combine(
        allAmexStatements,
        allAmexPagoFlexPlans,
        allAmexScheduledPayments,
    ) { statements, plans, payments ->
        val statementIdsByCard = statements
            .groupBy { it.paymentMethodId }
            .mapValues { it.value.map { s -> s.id }.toSet() }
        statementIdsByCard.mapValues { (_, statementIds) ->
            plans
                .filter { it.statementId in statementIds }
                .flatMap { plan -> payments.filter { it.planId == plan.id && it.status == "PENDING" } }
                .sumOf { it.amount }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val amexPaidByCard: StateFlow<Map<String, Double>> = combine(
        allAmexStatements,
        allAmexPagoFlexPlans,
        allAmexScheduledPayments,
    ) { statements, plans, payments ->
        val statementIdsByCard = statements
            .groupBy { it.paymentMethodId }
            .mapValues { it.value.map { s -> s.id }.toSet() }
        statementIdsByCard.mapValues { (_, statementIds) ->
            plans
                .filter { it.statementId in statementIds }
                .flatMap { plan -> payments.filter { it.planId == plan.id && it.status == "PAID" } }
                .sumOf { it.amount }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun syncInstallmentPlanOutflows() {
        viewModelScope.launch(Dispatchers.IO) {
            val today = java.time.LocalDate.now()
            val plans = repository.getAllInstallmentPlans()
            if (plans.isEmpty()) return@launch
            val methods = repository.getAllPaymentMethods()
            val details = repository.getAllCreditCardDetails()
            val categories = repository.getAllCategories()
            for (plan in plans) {
                val method = methods.find { it.id == plan.paymentMethodId } ?: continue
                val detail = details.find { it.paymentMethodId == plan.paymentMethodId }
                val card = ActiveCreditCard(
                    id = method.id,
                    name = method.name,
                    provider = method.provider,
                    cardType = detail?.cardType?.let { CreditCardType.safeValueOf(it) } ?: CreditCardType.INSTALLMENT,
                    limit = detail?.limit ?: 0.0,
                    closingDay = detail?.closingDay ?: 0,
                    paymentDay = detail?.paymentDay ?: 0,
                    linkedPaymentMethodId = detail?.linkedPaymentMethodId,
                )
                val payments = repository.getScheduledPaymentsByPlan(plan.id)
                    .filter { it.status == "PENDING" && it.expenseTransactionId == null }
                for (payment in payments) {
                    val dueDate = try {
                        java.time.LocalDate.parse(payment.dueDate, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                    } catch (_: Exception) {
                        null
                    } ?: continue
                    if (dueDate.isAfter(today)) continue
                    val transactions = generateCreditCardPaymentUseCase.execute(card, payment.amount, dueDate, categories)
                    transactions.forEach { repository.insertTransaction(it) }
                    val expenseTx = transactions.firstOrNull()
                    repository.updateScheduledPaymentStatus(payment.id, "PAID", expenseTx?.id)
                }
                val paidCount = repository.getScheduledPaymentsByPlan(plan.id).count { it.status == "PAID" }
                repository.updateInstallmentPlanPaidCount(plan.id, paidCount)
            }
        }
    }

    // SCANSIONE RICEVUTE (ML Kit OCR)
    private val _receiptScanResult = MutableStateFlow<ReceiptScanResult?>(null)
    val receiptScanResult: StateFlow<ReceiptScanResult?> = _receiptScanResult.asStateFlow()

    fun scanReceipt(recognizedText: String) {
        val result = scanReceiptUseCase(recognizedText)
        _receiptScanResult.value = result
    }

    fun clearReceiptScanResult() {
        _receiptScanResult.value = null
    }

    fun addCategory(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCategory(category)
        }
    }

    fun removeCategory(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCategoryById(id)
        }
    }

    fun updateDashboardMonth(month: YearMonth) {
        _currentDashboardMonth.value = month
    }

    // --- AZIONI ---

    fun saveTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            saveTransactionUseCase(transaction)
        }
    }

    fun deleteTransaction(
        transactionId: String,
        deleteType: DeleteType,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val amexPayment = repository.getAmexScheduledPaymentByExpenseTxId(transactionId)
            val planIdToRecalculate = amexPayment?.planId
            val planBeforeDelete = planIdToRecalculate?.let { repository.getAmexPagoFlexPlanById(it) }
            val statementBeforeDelete = planBeforeDelete?.let { repository.getAmexStatementById(it.statementId) }

            deleteTransactionUseCase(transactionId, deleteType)

            if (planIdToRecalculate != null && planBeforeDelete != null && statementBeforeDelete != null) {
                val updatedPlan = repository.getAmexPagoFlexPlanById(planIdToRecalculate) ?: return@launch
                val existingPayments = repository.getAmexScheduledPaymentsForPlan(planIdToRecalculate)
                val strategy = if (updatedPlan.planType == "FIXED_AMOUNT") {
                    com.expense.management.domain.model.AmexInstallmentStrategy.FixedAmount(updatedPlan.initialInstallmentAmount ?: updatedPlan.installmentAmount)
                } else {
                    com.expense.management.domain.model.AmexInstallmentStrategy.FixedDuration(updatedPlan.installmentCount)
                }
                val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                val useCase = com.expense.management.domain.usecase.RecalculateAmexInstallmentPlanUseCase()
                val (newPlan, newPendingPayments) = useCase.execute(
                    existingPlan = updatedPlan,
                    existingPayments = existingPayments,
                    newStrategy = strategy,
                    today = today,
                    currentStatementPaymentDueDate = statementBeforeDelete.paymentDueDate,
                )
                repository.updateAmexPagoFlexPlanCalculation(
                    planId = newPlan.id,
                    installmentCount = newPlan.installmentCount,
                    installmentAmount = newPlan.installmentAmount,
                    planType = newPlan.planType,
                    initialInstallmentAmount = newPlan.initialInstallmentAmount,
                )
                repository.deletePendingAmexScheduledPaymentsForPlan(planIdToRecalculate)
                repository.insertAmexScheduledPayments(newPendingPayments)
                val paidCount = repository.getAmexScheduledPaymentsForPlan(planIdToRecalculate).count { it.status == "PAID" }
                repository.updateAmexPagoFlexPaidCount(planIdToRecalculate, paidCount)
            }
        }
    }

    suspend fun getTransactionById(id: String): TransactionEntity? = repository.getTransactionById(id)

    fun searchDescriptionSuggestions(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (query.length < 2) {
                _suggestions.value = emptyList()
            } else {
                _suggestions.value = repository.getDescriptionSuggestions(query)
            }
        }
    }

    // Aggiornamento Impostazioni
    fun updateCurrency(symbol: String) {
        _currency.value = symbol
        prefs?.edit { putString(KEY_CURRENCY, symbol) }
    }

    fun updateDateFormat(format: String) {
        _dateFormat.value = format
        prefs?.edit { putString(KEY_DATE_FORMAT, format) }
    }

    fun updateCcPaymentMode(mode: String) {
        _ccPaymentMode.value = mode
        prefs?.edit { putString(KEY_CC_PAYMENT_MODE, mode) }
    }

    fun updateIsAmountHidden(isHidden: Boolean) {
        _isAmountHidden.value = isHidden
        prefs?.edit { putBoolean(KEY_HIDE_AMOUNT, isHidden) }
    }

    fun toggleAmountHidden() {
        val newValue = !_isAmountHidden.value
        _isAmountHidden.value = newValue
        prefs?.edit { putBoolean(KEY_HIDE_AMOUNT, newValue) }
    }

    fun updateBiometricEnabled(isEnabled: Boolean) {
        _isBiometricEnabled.value = isEnabled
        prefs?.edit { putBoolean(KEY_BIOMETRIC_ENABLED, isEnabled) }
    }

    fun updateCsvExportColumns(columns: Set<String>) {
        _csvExportColumns.value = columns
        prefs?.edit { putStringSet(KEY_CSV_EXPORT_COLUMNS, columns) }
    }

    fun updateThemeMode(mode: String) {
        _themeMode.value = mode
        prefs?.edit { putString(KEY_THEME_MODE, mode) }
    }

    fun updateAppStyle(style: AppStyle) {
        _appStyle.value = style
        prefs?.edit { putString(KEY_APP_STYLE, style.name) }
    }

    fun updateEnabledWidgets(widgets: Set<DashboardWidget>) {
        _enabledWidgets.value = widgets
        prefs?.edit { putStringSet(KEY_ENABLED_WIDGETS, widgets.map { it.name }.toSet()) }
    }

    fun refreshCurrencyRates() {
        viewModelScope.launch {
            _currencyRatesUpdate.value = currencyUtils.getLastUpdate()
            refreshCurrencyRatesData()
        }
    }

    suspend fun forceCurrencyRatesUpdateSuspend(): Boolean {
        val success = currencyUtils.forceUpdate()
        if (success) {
            _currencyRatesUpdate.value = currencyUtils.getLastUpdate()
            refreshCurrencyRatesData()
        }
        return success
    }

    private suspend fun refreshCurrencyRatesData() {
        val rates = repository.getAllCurrencyRates()
        _currencyRates.value = rates
    }

    // Metodi Backup
    suspend fun getAllForBackup(): BackupData = getBackupDataUseCase()

    fun restoreData(backupData: BackupData) {
        viewModelScope.launch(Dispatchers.IO) {
            restoreDataUseCase(backupData)
        }
    }

    fun restoreLegacyData(list: List<TransactionEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllTransactions()
            repository.insertAllTransactions(list)
        }
    }

    suspend fun getExpensesForExport(): List<TransactionEntity> = repository.getAllTransactionsList().filter { it.type == TransactionType.EXPENSE }

    suspend fun getAllCategoryForExport(): List<CategoryEntity> = repository.getAllCategories() + CATEGORIES.map { CategoryEntity(it.id, it.label, it.icon, it.type.toData()) }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCategory(category)
        }
    }

    suspend fun updateCurrencyRate(amount: Double, from: String, to: String): Double? {
        val result = currencyUtils.convert(amount, from, to)
        refreshCurrencyRates()
        return result
    }
}
