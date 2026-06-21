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
import com.expense.management.data.CardType
import com.expense.management.data.CategoryEntity
import com.expense.management.data.CreditCardDetailEntity
import com.expense.management.data.CreditCardEntity
import com.expense.management.data.CreditCardInstallmentPlanEntity
import com.expense.management.data.CurrencyRate
import com.expense.management.data.DebitCardDetailEntity
import com.expense.management.data.ExpenseRepository
import com.expense.management.data.InstallmentScheduledPaymentEntity
import com.expense.management.data.KlarnaDetailEntity
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.data.PaypalDetailEntity
import com.expense.management.data.RevolutDetailEntity
import com.expense.management.data.SatispayDetailEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.ActiveCreditCard
import com.expense.management.domain.model.AmexDashboardProjection
import com.expense.management.domain.model.AmexForecastMonth
import com.expense.management.domain.model.AmexInstallmentStrategy
import com.expense.management.domain.model.AmexPaymentMode
import com.expense.management.domain.model.AmexStatementSummary
import com.expense.management.domain.model.BnplProjection
import com.expense.management.domain.model.CATEGORIES
import com.expense.management.domain.model.CreditCardSummary
import com.expense.management.domain.model.CreditCardType
import com.expense.management.domain.model.DashboardWidget
import com.expense.management.domain.model.DeleteType
import com.expense.management.domain.model.PaymentMethodDetails
import com.expense.management.domain.model.PaymentProvider
import com.expense.management.domain.model.ReceiptScanResult
import com.expense.management.domain.model.ReportData
import com.expense.management.domain.model.SatispayStatus
import com.expense.management.domain.usecase.AutoPayAmexStatementsUseCase
import com.expense.management.domain.usecase.CalculateAmexCurrentAccountOutflowUseCase
import com.expense.management.domain.usecase.CalculateAmexDashboardProjectionsUseCase
import com.expense.management.domain.usecase.CalculateAmexStatementUseCase
import com.expense.management.domain.usecase.CalculateBnplProjectionsUseCase
import com.expense.management.domain.usecase.CalculateReportUseCase
import com.expense.management.domain.usecase.CalculateSatispayStatusUseCase
import com.expense.management.domain.usecase.CreateAmexInstallmentPlanUseCase
import com.expense.management.domain.usecase.DeleteTransactionUseCase
import com.expense.management.domain.usecase.GenerateAmexForecastUseCase
import com.expense.management.domain.usecase.GenerateCreditCardPaymentUseCase
import com.expense.management.domain.usecase.GetBackupDataUseCase
import com.expense.management.domain.usecase.GetCategoriesUseCase
import com.expense.management.domain.usecase.GetCreditCardsUseCase
import com.expense.management.domain.usecase.GetFrequentCategoriesUseCase
import com.expense.management.domain.usecase.GetPaymentMethodsUseCase
import com.expense.management.domain.usecase.GetTransactionsUseCase
import com.expense.management.domain.usecase.InitializeCategoriesUseCase
import com.expense.management.domain.usecase.ManageCreditCardUseCase
import com.expense.management.domain.usecase.ManagePaymentMethodUseCase
import com.expense.management.domain.usecase.PayAmexStatementUseCase
import com.expense.management.domain.usecase.RecalculateAmexInstallmentPlanUseCase
import com.expense.management.domain.usecase.RestoreDataUseCase
import com.expense.management.domain.usecase.SaveTransactionUseCase
import com.expense.management.domain.usecase.ScanReceiptUseCase
import com.expense.management.ui.theme.AppStyle
import com.expense.management.utils.CurrencyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
    private val getCreditCardsUseCase: GetCreditCardsUseCase,
    private val manageCreditCardUseCase: ManageCreditCardUseCase,
    private val getPaymentMethodsUseCase: GetPaymentMethodsUseCase,
    private val managePaymentMethodUseCase: ManagePaymentMethodUseCase,
    private val getBackupDataUseCase: GetBackupDataUseCase,
    private val restoreDataUseCase: RestoreDataUseCase,
    private val getFrequentCategoriesUseCase: GetFrequentCategoriesUseCase,
    private val calculateReportUseCase: CalculateReportUseCase,
    private val scanReceiptUseCase: ScanReceiptUseCase,
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
        private const val KEY_DEFAULT_PAYMENT_METHOD = "default_payment_method_id"
        private const val KEY_AMEX_AUTO_PAY = "amex_auto_pay_enabled"
    }

    // Frequent Categories Caching
    private val frequentExpenses = getFrequentCategoriesUseCase(TransactionType.EXPENSE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val frequentIncome = getFrequentCategoriesUseCase(TransactionType.INCOME)
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
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reportData: StateFlow<ReportData> = combine(
        reportTransactions,
        allCategories,
        reportRangeState,
    ) { tx, cats, range ->
        calculateReportUseCase.execute(tx, cats, range.first, range.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportData.EMPTY)

    // DATI CARTE DI CREDITO
    val allCreditCards: StateFlow<List<CreditCardEntity>> =
        getCreditCardsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // DATI METODI DI PAGAMENTO (inietta Contante come metodo built-in)
    val allPaymentMethods: StateFlow<List<PaymentMethodEntity>> =
        getPaymentMethodsUseCase()
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

    // CARTE DI CREDITO ATTIVE: unisce nuovo sistema (payment_methods + credit_card_details)
    // e legacy (allCreditCards) per garantire visibilità anche dopo restore da vecchio backup
    val activeCreditCards: StateFlow<List<ActiveCreditCard>> =
        combine(allPaymentMethods, repository.allCreditCardDetails, allCreditCards) { methods, details, legacyCards ->
            val newCards = methods
                .filter {
                    it.provider == PaymentProvider.CREDIT_CARD_SALDO ||
                        it.provider == PaymentProvider.CREDIT_CARD_REVOLVING ||
                        it.provider == PaymentProvider.CREDIT_CARD_INSTALLMENT ||
                        it.provider == PaymentProvider.CREDIT_CARD_AMEX
                }
                .mapNotNull { method ->
                    val detail = details.find { it.paymentMethodId == method.id }
                    val provider = method.provider
                    if (provider == PaymentProvider.CREDIT_CARD_AMEX) {
                        ActiveCreditCard(
                            id = method.id,
                            name = method.name,
                            provider = provider,
                            cardType = detail?.let { CreditCardType.safeValueOf(it.cardType) } ?: CreditCardType.AMEX_HYBRID,
                            limit = detail?.limit ?: 0.0,
                            closingDay = detail?.closingDay ?: 0,
                            paymentDay = detail?.paymentDay ?: 0,
                        )
                    } else {
                        detail?.let {
                            ActiveCreditCard(
                                id = method.id,
                                name = method.name,
                                provider = provider,
                                cardType = CreditCardType.safeValueOf(it.cardType) ?: return@mapNotNull null,
                                limit = it.limit,
                                closingDay = it.closingDay,
                                paymentDay = it.paymentDay,
                            )
                        }
                    }
                }
            val existingIds = newCards.map { it.id }.toSet()
            val fallbackCards = legacyCards
                .filter { it.id !in existingIds }
                .map { card ->
                    ActiveCreditCard(
                        id = card.id,
                        name = card.name,
                        provider = if (card.type == CardType.SALDO) PaymentProvider.CREDIT_CARD_SALDO else PaymentProvider.CREDIT_CARD_REVOLVING,
                        cardType = if (card.type == CardType.SALDO) CreditCardType.SALDO else CreditCardType.REVOLVING,
                        limit = card.limit,
                        closingDay = card.closingDay,
                        paymentDay = card.paymentDay,
                    )
                }
            newCards + fallbackCards
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private val _defaultPaymentMethodId = MutableStateFlow(
        prefs?.getString(KEY_DEFAULT_PAYMENT_METHOD, "__cash__") ?: "__cash__",
    )
    val defaultPaymentMethodId = _defaultPaymentMethodId.asStateFlow()

    init {
        viewModelScope.launch {
            initializeCategoriesUseCase()
        }
        viewModelScope.launch {
            _currencyRatesUpdate.value = currencyUtils.getLastUpdate()
            refreshCurrencyRatesData()
        }
        viewModelScope.launch(Dispatchers.IO) {
            if (prefs?.getBoolean(KEY_AMEX_AUTO_PAY, true) != false) {
                triggerAmexAutoPay()
            }
            syncAmexCurrentAccountOutflows()
        }
    }

    // GESTIONE CARTE DI CREDITO
    fun addCreditCard(creditCard: CreditCardEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            manageCreditCardUseCase.add(creditCard)
        }
    }

    fun updateCreditCard(creditCard: CreditCardEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            manageCreditCardUseCase.update(creditCard)
        }
    }

    fun deleteCreditCard(creditCard: CreditCardEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            manageCreditCardUseCase.delete(creditCard)
        }
    }

    // DATI DETTAGLIO PAYPAL
    val allPaypalDetails: StateFlow<List<PaypalDetailEntity>> =
        repository.allPaypalDetails
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // DATI DETTAGLIO KLARNA
    val allKlarnaDetails: StateFlow<List<KlarnaDetailEntity>> =
        repository.allKlarnaDetails
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // PIANI RATEALI CARTE INSTALLMENT
    val allInstallmentPlans: StateFlow<List<CreditCardInstallmentPlanEntity>> =
        repository.allInstallmentPlans
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        CalculateAmexCurrentAccountOutflowUseCase().execute(monthPrefix, payments, emptyList())
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val amexDashboardProjections: StateFlow<List<AmexDashboardProjection>> = combine(
        _currentDashboardMonth,
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

    val creditCardSummaries: StateFlow<Map<String, CreditCardSummary>> = combine(
        allTransactions,
        activeCreditCards,
        _currentDashboardMonth,
        amexPendingByCard,
    ) { tx, cards, month, pendingByCard ->
        cards.associate { card ->
            card.id to when (card.cardType) {
                CreditCardType.REVOLVING,
                CreditCardType.INSTALLMENT,
                -> {
                    val cardTx = tx.filter {
                        (it.creditCardId == card.id || it.paymentMethodId == card.id)
                    }
                    val totalUtilized = cardTx
                        .filter { it.type == TransactionType.EXPENSE && it.isCreditCard }
                        .sumOf { it.amount }
                    val totalRepaid = cardTx
                        .filter { it.type == TransactionType.INCOME && it.isCreditCard }
                        .sumOf { it.amount }
                    val displayed = totalUtilized - totalRepaid
                    CreditCardSummary(
                        cardId = card.id,
                        name = card.name,
                        limit = card.limit,
                        cardType = card.cardType,
                        displayedSpent = displayed,
                        totalUtilized = totalUtilized,
                        totalPaid = 0.0,
                        totalRepaid = totalRepaid,
                        progress = if (card.limit > 0) (displayed / card.limit).toFloat() else 0f,
                    )
                }
                CreditCardType.AMEX_HYBRID -> {
                    val cardTx = tx.filter {
                        (it.creditCardId == card.id || it.paymentMethodId == card.id)
                    }
                    val totalUtilized = cardTx
                        .filter { it.type == TransactionType.EXPENSE && it.isCreditCard }
                        .sumOf { it.amount }
                    val pendingAmount = pendingByCard[card.id] ?: 0.0
                    val totalRepaid = (totalUtilized - pendingAmount).coerceAtLeast(0.0)
                    CreditCardSummary(
                        cardId = card.id,
                        name = card.name,
                        limit = card.limit,
                        cardType = card.cardType,
                        displayedSpent = pendingAmount,
                        totalUtilized = totalUtilized,
                        totalPaid = totalRepaid,
                        totalRepaid = totalRepaid,
                        progress = if (card.limit > 0) (pendingAmount / card.limit).toFloat() else 0f,
                    )
                }
                CreditCardType.SALDO -> {
                    val spent = tx
                        .filter { (it.creditCardId == card.id || it.paymentMethodId == card.id) && it.type == TransactionType.EXPENSE }
                        .filter { t ->
                            try {
                                YearMonth.from(LocalDate.parse(t.effectiveDate)) == month
                            } catch (_: Exception) {
                                false
                            }
                        }
                        .sumOf { it.amount }
                    CreditCardSummary(
                        cardId = card.id,
                        name = card.name,
                        limit = card.limit,
                        cardType = card.cardType,
                        displayedSpent = spent,
                        totalUtilized = spent,
                        totalPaid = 0.0,
                        progress = if (card.limit > 0) (spent / card.limit).toFloat() else 0f,
                    )
                }
            }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _isAmexAutoPayEnabled = MutableStateFlow(
        prefs?.getBoolean(KEY_AMEX_AUTO_PAY, true) ?: true,
    )
    val isAmexAutoPayEnabled: StateFlow<Boolean> = _isAmexAutoPayEnabled.asStateFlow()

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
        val useCase = CalculateAmexStatementUseCase()
        return useCase.execute(statement, pagoFlexPlans, revolvingState, interestRate)
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
        val useCase = GenerateAmexForecastUseCase()
        return useCase.execute(
            statementMonth,
            pagoFlexPlans,
            revolvingState,
            paymentMode,
            paymentAmount,
            interestRate,
            monthsToForecast,
        )
    }

    fun createAmexStatement(
        paymentMethodId: String,
        statementMonth: String,
        closingDate: String,
        paymentDueDate: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = java.time.YearMonth.now()
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
                    paymentMode = com.expense.management.domain.model.AmexPaymentMode.SALDO.name,
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
        }
    }

    fun recalculateAmexInstallmentPlan(
        planId: String,
        strategy: AmexInstallmentStrategy,
        currentStatementPaymentDueDate: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val plan = repository.getAmexPagoFlexPlanById(planId) ?: return@launch
            val existingPayments = repository.getAmexScheduledPaymentsForPlan(planId)
            val oldCurrentPayment = existingPayments
                .filter { it.status == "PENDING" }
                .minByOrNull { it.sequenceNumber }
            val oldCurrentTxId = oldCurrentPayment?.expenseTransactionId
            val oldCurrentMonth = oldCurrentPayment?.dueDate?.take(7)
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

            if (oldCurrentTxId != null && oldCurrentMonth != null) {
                val newCurrentPayment = newPendingPayments.firstOrNull { it.dueDate.startsWith(oldCurrentMonth) }
                newCurrentPayment?.let { payment ->
                    repository.updateAmexScheduledPaymentExpenseTransactionId(payment.id, oldCurrentTxId)
                    val tx = repository.getTransactionById(oldCurrentTxId) ?: return@let
                    repository.insertTransaction(tx.copy(amount = payment.amount, originalAmount = payment.amount))
                }
            }
        }
    }

    fun syncAmexCurrentAccountOutflows() {
        viewModelScope.launch(Dispatchers.IO) {
            val today = java.time.LocalDate.now()
            val monthPrefix = java.time.YearMonth.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
            val plans = repository.getAllAmexPagoFlexPlans()
            val payments = repository.getPendingAmexScheduledPaymentsForMonth(monthPrefix)
            for (payment in payments) {
                if (payment.expenseTransactionId != null) continue
                val dueDate = try {
                    java.time.LocalDate.parse(payment.dueDate, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                } catch (_: Exception) { continue }
                if (dueDate.isAfter(today)) continue
                val plan = plans.find { it.id == payment.planId } ?: continue
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
                )
                repository.insertTransaction(tx)
                repository.updateAmexScheduledPaymentExpenseTransactionId(payment.id, tx.id)
            }
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
            val paymentDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
            val statementPlans = repository.getAmexPagoFlexPlansForStatement(statement.id)
            val scheduledPayments = statementPlans.flatMap { repository.getAmexScheduledPaymentsForPlan(it.id) }
            val result = PayAmexStatementUseCase().execute(statement, amount, paymentDate, statementPlans, scheduledPayments)
            result.paymentTransaction?.let { repository.insertTransaction(it) }
            result.incomeTransaction?.let { incomeTx ->
                repository.insertTransaction(incomeTx)
                result.paymentsToMarkPaid.forEach { payment ->
                    repository.markAmexScheduledPaymentAsPaid(payment.id, incomeTx.id)
                }
            }
            repository.closeAmexStatement(statement.id)
        }
    }

    fun triggerAmexAutoPay() {
        viewModelScope.launch(Dispatchers.IO) {
            val statements = repository.getNonClosedAmexStatements()
            if (statements.isEmpty()) return@launch
            val plans = repository.getAllAmexPagoFlexPlans()
            val revolving = repository.getAllAmexRevolvingStates()
            val useCase = AutoPayAmexStatementsUseCase()
            val dueStatements = useCase.execute(statements, plans, revolving)
            val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
            val allPayments = repository.getAllAmexScheduledPaymentsList()
            for (due in dueStatements) {
                val dueDate = try {
                    java.time.LocalDate.parse(due.statement.paymentDueDate, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                } catch (_: Exception) { continue }
                if (dueDate.isAfter(java.time.LocalDate.now())) continue
                val statementPlans = repository.getAmexPagoFlexPlansForStatement(due.statement.id)
                val scheduledPayments = statementPlans.flatMap { plan ->
                    allPayments.filter { it.planId == plan.id }
                }
                val result = PayAmexStatementUseCase().execute(due.statement, due.paymentAmount, today, statementPlans, scheduledPayments)
                result.paymentTransaction?.let { repository.insertTransaction(it) }
                result.incomeTransaction?.let { incomeTx ->
                    repository.insertTransaction(incomeTx)
                    result.paymentsToMarkPaid.forEach { payment ->
                        repository.markAmexScheduledPaymentAsPaid(payment.id, incomeTx.id)
                    }
                }
                repository.closeAmexStatement(due.statement.id)
            }
            syncAmexCurrentAccountOutflows()
        }
    }

    // PROIEZIONI BNPL
    val bnplProjections: StateFlow<List<BnplProjection>> = combine(
        allTransactions,
        allPaymentMethods,
        allPaypalDetails,
        allKlarnaDetails,
        _currentDashboardMonth,
    ) { tx, methods, paypal, klarna, month ->
        CalculateBnplProjectionsUseCase().execute(
            allTransactions = tx,
            allPaymentMethods = methods,
            paypalDetails = paypal,
            klarnaDetails = klarna,
            targetMonth = month,
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // GESTIONE METODI DI PAGAMENTO
    fun addPaymentMethod(
        paymentMethod: PaymentMethodEntity,
        closingDay: Int = 0,
        paymentDay: Int = 0,
        creditLimit: Double = 0.0,
        debitIssuer: String? = null,
        debitCardNumber: String? = null,
        debitNotes: String? = null,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            managePaymentMethodUseCase.add(paymentMethod)
            if (
                paymentMethod.provider == PaymentProvider.CREDIT_CARD_SALDO ||
                paymentMethod.provider == PaymentProvider.CREDIT_CARD_REVOLVING ||
                paymentMethod.provider == PaymentProvider.CREDIT_CARD_INSTALLMENT ||
                paymentMethod.provider == PaymentProvider.CREDIT_CARD_AMEX
            ) {
                repository.insertCreditCardDetail(
                    CreditCardDetailEntity(
                        paymentMethodId = paymentMethod.id,
                        cardType = when (paymentMethod.provider) {
                            PaymentProvider.CREDIT_CARD_SALDO -> CreditCardType.SALDO.name
                            PaymentProvider.CREDIT_CARD_REVOLVING -> CreditCardType.REVOLVING.name
                            PaymentProvider.CREDIT_CARD_INSTALLMENT -> CreditCardType.INSTALLMENT.name
                            PaymentProvider.CREDIT_CARD_AMEX -> CreditCardType.AMEX_HYBRID.name
                            else -> CreditCardType.SALDO.name
                        },
                        limit = creditLimit,
                        closingDay = closingDay,
                        paymentDay = paymentDay,
                    ),
                )
            } else if (paymentMethod.provider == PaymentProvider.DEBIT_CARD) {
                repository.insertDebitCardDetail(
                    DebitCardDetailEntity(
                        paymentMethodId = paymentMethod.id,
                        issuer = debitIssuer,
                        cardNumber = debitCardNumber,
                        notes = debitNotes,
                    ),
                )
            }
        }
    }

    fun updatePaymentMethod(paymentMethod: PaymentMethodEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            managePaymentMethodUseCase.update(paymentMethod)
        }
    }

    fun deletePaymentMethod(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            managePaymentMethodUseCase.delete(id)
        }
    }

    suspend fun getPaymentMethodById(id: String): PaymentMethodEntity? =
        managePaymentMethodUseCase.getPaymentMethodById(id)

    suspend fun getAllPaymentMethods(): List<PaymentMethodEntity> =
        managePaymentMethodUseCase.getAllPaymentMethods()

    // DETTAGLI METODI DI PAGAMENTO
    suspend fun getCreditCardDetail(paymentMethodId: String): CreditCardDetailEntity? =
        repository.getCreditCardDetail(paymentMethodId)

    suspend fun insertCreditCardDetail(detail: CreditCardDetailEntity) {
        repository.insertCreditCardDetail(detail)
    }

    suspend fun getRevolutDetail(paymentMethodId: String): RevolutDetailEntity? =
        repository.getRevolutDetail(paymentMethodId)

    suspend fun insertRevolutDetail(detail: RevolutDetailEntity) {
        repository.insertRevolutDetail(detail)
    }

    suspend fun getSatispayDetail(paymentMethodId: String): SatispayDetailEntity? =
        repository.getSatispayDetail(paymentMethodId)

    suspend fun insertSatispayDetail(detail: SatispayDetailEntity) {
        repository.insertSatispayDetail(detail)
    }

    suspend fun getPaypalDetail(paymentMethodId: String): PaypalDetailEntity? =
        repository.getPaypalDetail(paymentMethodId)

    suspend fun insertPaypalDetail(detail: PaypalDetailEntity) {
        repository.insertPaypalDetail(detail)
    }

    suspend fun getKlarnaDetail(paymentMethodId: String): KlarnaDetailEntity? =
        repository.getKlarnaDetail(paymentMethodId)

    suspend fun insertKlarnaDetail(detail: KlarnaDetailEntity) {
        repository.insertKlarnaDetail(detail)
    }

    suspend fun getPaymentMethodDetails(method: PaymentMethodEntity): PaymentMethodDetails? {
        val provider = method.provider
        return when (provider) {
            PaymentProvider.CREDIT_CARD_SALDO,
            PaymentProvider.CREDIT_CARD_REVOLVING,
            PaymentProvider.CREDIT_CARD_INSTALLMENT,
            PaymentProvider.CREDIT_CARD_AMEX,
            -> {
                repository.getCreditCardDetail(method.id)?.let {
                    PaymentMethodDetails.CreditCard(
                        name = method.name,
                        cardType = CreditCardType.safeValueOf(it.cardType) ?: return null,
                        limit = it.limit,
                        closingDay = it.closingDay,
                        paymentDay = it.paymentDay,
                    )
                }
            }
            PaymentProvider.DEBIT_CARD -> {
                repository.getDebitCardDetail(method.id)?.let {
                    PaymentMethodDetails.DebitCard(
                        name = method.name,
                        issuer = it.issuer,
                        cardNumber = it.cardNumber,
                        notes = it.notes,
                    )
                }
            }
            PaymentProvider.REVOLUT -> {
                repository.getRevolutDetail(method.id)?.let {
                    PaymentMethodDetails.Revolut(
                        name = method.name,
                        currency = it.currency,
                        iban = it.iban,
                        accountNumber = it.accountNumber,
                    )
                }
            }
            PaymentProvider.SATISPAY -> {
                repository.getSatispayDetail(method.id)?.let {
                    PaymentMethodDetails.Satispay(
                        name = method.name,
                        weeklyBudget = it.weeklyBudget,
                        sddDay = it.sddDay,
                        iban = it.iban,
                    )
                }
            }
            PaymentProvider.PAYPAL -> {
                repository.getPaypalDetail(method.id)?.let {
                    PaymentMethodDetails.Paypal(
                        name = method.name,
                        email = it.email,
                        bnplInstallmentCount = it.bnplInstallmentCount,
                        bnplCycleDays = it.bnplCycleDays,
                    )
                }
            }
            PaymentProvider.KLARNA -> {
                repository.getKlarnaDetail(method.id)?.let {
                    PaymentMethodDetails.Klarna(
                        name = method.name,
                        bnplInstallmentCount = it.bnplInstallmentCount,
                        bnplCycleDays = it.bnplCycleDays,
                    )
                }
            }
            PaymentProvider.CASH -> PaymentMethodDetails.Cash(name = method.name)
        }
    }

    suspend fun calculateSatispayStatus(method: PaymentMethodEntity, detail: SatispayDetailEntity): SatispayStatus {
        val useCase = CalculateSatispayStatusUseCase(repository)
        return useCase.execute(method, detail)
    }

    fun payCreditCardInstallment(
        card: ActiveCreditCard,
        paymentAmount: Double,
        paymentDate: LocalDate,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val categories = repository.getAllCategories()
            val useCase = GenerateCreditCardPaymentUseCase()
            val transactions = useCase.execute(card, paymentAmount, paymentDate, categories)
            transactions.forEach { repository.insertTransaction(it) }
        }
    }

    fun payInstallmentPlan(
        plan: CreditCardInstallmentPlanEntity,
        card: ActiveCreditCard,
        paymentDate: LocalDate,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val nextPayment = repository.getNextPendingScheduledPayment(plan.id) ?: return@launch
            val categories = repository.getAllCategories()
            val useCase = GenerateCreditCardPaymentUseCase()
            val transactions = useCase.execute(card, nextPayment.amount, paymentDate, categories)
            transactions.forEach { repository.insertTransaction(it) }
            val expenseTx = transactions.firstOrNull()
            repository.updateScheduledPaymentStatus(nextPayment.id, "PAID", expenseTx?.id)
            repository.updateInstallmentPlanPaidCount(plan.id, plan.paidCount + 1)
        }
    }

    fun saveInstallmentPlan(
        paymentMethodId: String,
        totalAmount: Double,
        installmentCount: Int,
        installmentAmount: Double,
        startDate: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingPlan = repository.getInstallmentPlanByCard(paymentMethodId)
            if (existingPlan != null) {
                repository.deleteScheduledPaymentsByPlan(existingPlan.id)
                repository.deleteInstallmentPlan(existingPlan.id)
            }
            val planId = java.util.UUID.randomUUID().toString()
            val plan = CreditCardInstallmentPlanEntity(
                id = planId,
                paymentMethodId = paymentMethodId,
                totalAmount = totalAmount,
                installmentCount = installmentCount,
                installmentAmount = installmentAmount,
                paidCount = 0,
                startDate = startDate,
            )
            repository.insertInstallmentPlan(plan)
            val payments = generateScheduledPayments(planId, totalAmount, installmentCount, installmentAmount, startDate)
            repository.insertScheduledPayments(payments)
        }
    }

    private fun generateScheduledPayments(
        planId: String,
        totalAmount: Double,
        installmentCount: Int,
        installmentAmount: Double,
        startDate: String,
    ): List<InstallmentScheduledPaymentEntity> {
        val parts = startDate.split("-")
        val baseYear = parts[0].toIntOrNull() ?: 2024
        val baseMonth = parts[1].toIntOrNull() ?: 1
        val baseDay = (parts[2].toIntOrNull() ?: 1).coerceIn(1, 28)
        return List(installmentCount) { i ->
            val monthOffset = i
            val year = baseYear + (baseMonth - 1 + monthOffset) / 12
            val month = ((baseMonth - 1 + monthOffset) % 12) + 1
            val dueDate = "%04d-%02d-%02d".format(year, month, baseDay)
            val amount = if (i == installmentCount - 1) {
                totalAmount - (installmentAmount * (installmentCount - 1))
            } else {
                installmentAmount
            }
            InstallmentScheduledPaymentEntity(
                id = java.util.UUID.randomUUID().toString(),
                planId = planId,
                dueDate = dueDate,
                amount = amount,
                status = "PENDING",
            )
        }
    }

    fun updatePaymentMethodWithDetails(method: PaymentMethodEntity, details: PaymentMethodDetails) {
        viewModelScope.launch(Dispatchers.IO) {
            managePaymentMethodUseCase.update(method)
            when (details) {
                is PaymentMethodDetails.CreditCard -> repository.insertCreditCardDetail(
                    CreditCardDetailEntity(
                        paymentMethodId = method.id,
                        cardType = details.cardType.name,
                        limit = details.limit,
                        closingDay = details.closingDay,
                        paymentDay = details.paymentDay,
                    ),
                )
                is PaymentMethodDetails.Revolut -> repository.insertRevolutDetail(
                    RevolutDetailEntity(
                        paymentMethodId = method.id,
                        currency = details.currency,
                        iban = details.iban,
                        accountNumber = details.accountNumber,
                    ),
                )
                is PaymentMethodDetails.Satispay -> repository.insertSatispayDetail(
                    SatispayDetailEntity(
                        paymentMethodId = method.id,
                        weeklyBudget = details.weeklyBudget,
                        sddDay = details.sddDay,
                        iban = details.iban,
                    ),
                )
                is PaymentMethodDetails.Paypal -> repository.insertPaypalDetail(
                    PaypalDetailEntity(
                        paymentMethodId = method.id,
                        email = details.email,
                        bnplInstallmentCount = details.bnplInstallmentCount,
                        bnplCycleDays = details.bnplCycleDays,
                    ),
                )
                is PaymentMethodDetails.Klarna -> repository.insertKlarnaDetail(
                    KlarnaDetailEntity(
                        paymentMethodId = method.id,
                        bnplInstallmentCount = details.bnplInstallmentCount,
                        bnplCycleDays = details.bnplCycleDays,
                    ),
                )
                is PaymentMethodDetails.DebitCard -> repository.insertDebitCardDetail(
                    DebitCardDetailEntity(
                        paymentMethodId = method.id,
                        issuer = details.issuer,
                        cardNumber = details.cardNumber,
                        notes = details.notes,
                    ),
                )
                is PaymentMethodDetails.Cash -> {}
            }
        }
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
            deleteTransactionUseCase(transactionId, deleteType)
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

    fun updateDefaultPaymentMethod(id: String?) {
        val value = id ?: "__cash__"
        _defaultPaymentMethodId.value = value
        prefs?.edit { putString(KEY_DEFAULT_PAYMENT_METHOD, value) }
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

    suspend fun getAllCategoryForExport(): List<CategoryEntity> = repository.getAllCategories() + CATEGORIES.map { CategoryEntity(it.id, it.label, it.icon, it.type) }

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
