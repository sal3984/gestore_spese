package com.expense.management.viewmodel

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expense.management.data.BackupData
import com.expense.management.data.CardType
import com.expense.management.data.CategoryEntity
import com.expense.management.data.CreditCardDetailEntity
import com.expense.management.data.CreditCardEntity
import com.expense.management.data.CurrencyRate
import com.expense.management.data.DebitCardDetailEntity
import com.expense.management.data.ExpenseRepository
import com.expense.management.data.KlarnaDetailEntity
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.data.PaypalDetailEntity
import com.expense.management.data.RevolutDetailEntity
import com.expense.management.data.SatispayDetailEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.ActiveCreditCard
import com.expense.management.domain.model.BnplProjection
import com.expense.management.domain.model.CreditCardType
import com.expense.management.domain.model.PaymentMethodDetails
import com.expense.management.domain.model.PaymentProvider
import com.expense.management.domain.usecase.CalculateBnplProjectionsUseCase
import com.expense.management.domain.usecase.DeleteTransactionUseCase
import com.expense.management.domain.usecase.GetBackupDataUseCase
import com.expense.management.domain.usecase.GetCategoriesUseCase
import com.expense.management.domain.usecase.GetCreditCardsUseCase
import com.expense.management.domain.usecase.GetFrequentCategoriesUseCase
import com.expense.management.domain.usecase.GetPaymentMethodsUseCase
import com.expense.management.domain.usecase.GetTransactionsUseCase
import com.expense.management.domain.usecase.InitializeCategoriesUseCase
import com.expense.management.domain.usecase.ManageCreditCardUseCase
import com.expense.management.domain.usecase.ManagePaymentMethodUseCase
import com.expense.management.domain.usecase.RestoreDataUseCase
import com.expense.management.domain.usecase.SaveTransactionUseCase
import com.expense.management.ui.model.DeleteType
import com.expense.management.ui.screens.category.CATEGORIES
import com.expense.management.utils.CurrencyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
) : ViewModel() {

    companion object {
        private const val KEY_CURRENCY = "currency"
        private const val KEY_CC_LIMIT = "cc_limit"
        private const val KEY_CC_DELAY = "cc_delay"
        private const val KEY_DATE_FORMAT = "date_format"
        private const val KEY_HIDE_AMOUNT = "hide_amount"
        private const val KEY_BIOMETRIC_ENABLED = "is_biometric_enabled"
        private const val KEY_CC_PAYMENT_MODE = "cc_payment_mode"
        private const val KEY_CSV_EXPORT_COLUMNS = "csv_export_columns"
        private const val KEY_THEME_MODE = "theme_mode"
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

    val reportTransactions: StateFlow<List<TransactionEntity>> = reportRangeState
        .flatMapLatest { (start, end) ->
            repository.getTransactionsBetween(
                start.atDay(1).toString(),
                end.atEndOfMonth().toString(),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setReportRange(start: YearMonth, end: YearMonth) {
        reportRangeState.value = start to end
    }

    // DATI CATEGORIE
    val allCategories: StateFlow<List<CategoryEntity>> =
        getCategoriesUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // DATI CARTE DI CREDITO
    val allCreditCards: StateFlow<List<CreditCardEntity>> =
        getCreditCardsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // DATI METODI DI PAGAMENTO
    val allPaymentMethods: StateFlow<List<PaymentMethodEntity>> =
        getPaymentMethodsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // CARTE DI CREDITO ATTIVE: unisce nuovo sistema (payment_methods + credit_card_details)
    // e legacy (allCreditCards) per garantire visibilità anche dopo restore da vecchio backup
    val activeCreditCards: StateFlow<List<ActiveCreditCard>> =
        combine(allPaymentMethods, repository.allCreditCardDetails, allCreditCards) { methods, details, legacyCards ->
            val newCards = methods
                .filter {
                    it.provider == PaymentProvider.CREDIT_CARD_SALDO.name ||
                        it.provider == PaymentProvider.CREDIT_CARD_REVOLVING.name
                }
                .mapNotNull { method ->
                    val detail = details.find { it.paymentMethodId == method.id }
                    detail?.let {
                        ActiveCreditCard(
                            id = method.id,
                            name = method.name,
                            provider = PaymentProvider.valueOf(method.provider),
                            cardType = CreditCardType.valueOf(it.cardType),
                            limit = it.limit,
                            closingDay = it.closingDay,
                            paymentDay = it.paymentDay,
                        )
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

    // --- STATO IMPOSTAZIONI ---
    private val _currency = MutableStateFlow(prefs?.getString(KEY_CURRENCY, "€") ?: "€")
    val currency = _currency.asStateFlow()

    private val _ccLimit = MutableStateFlow(prefs?.getFloat(KEY_CC_LIMIT, 1500f) ?: 1500f)
    val ccLimit = _ccLimit.asStateFlow()

    private val _ccDelay = MutableStateFlow(prefs?.getInt(KEY_CC_DELAY, 1) ?: 1)
    val ccDelay = _ccDelay.asStateFlow()

    private val _dateFormat = MutableStateFlow(prefs?.getString(KEY_DATE_FORMAT, "dd/MM/yyyy") ?: "dd/MM/yyyy")
    val dateFormat = _dateFormat.asStateFlow()

    private val _isAmountHidden = MutableStateFlow(prefs?.getBoolean(KEY_HIDE_AMOUNT, false) ?: false)
    val isAmountHidden = _isAmountHidden.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs?.getBoolean(KEY_BIOMETRIC_ENABLED, false) ?: false)
    val isBiometricEnabled = _isBiometricEnabled.asStateFlow()

    private val _ccPaymentMode = MutableStateFlow(prefs?.getString(KEY_CC_PAYMENT_MODE, "single") ?: "single")
    val ccPaymentMode = _ccPaymentMode.asStateFlow()

    val earliestMonth: StateFlow<YearMonth> = repository.allTransactions
        .map {
            val minDateString = repository.getMinEffectiveDate()
            if (minDateString != null) {
                try {
                    YearMonth.from(LocalDate.parse(minDateString))
                } catch (_: Exception) {
                    YearMonth.now()
                }
            } else {
                YearMonth.now()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), YearMonth.now())

    private val _currentDashboardMonth = MutableStateFlow(YearMonth.now())
    val currentDashboardMonth = _currentDashboardMonth.asStateFlow()

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

    init {
        viewModelScope.launch {
            initializeCategoriesUseCase()
            _currencyRatesUpdate.value = currencyUtils.getLastUpdate()
            refreshCurrencyRatesData()
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

    // PROIEZIONI BNPL
    private val _bnplProjections = MutableStateFlow<List<BnplProjection>>(emptyList())
    val bnplProjections: StateFlow<List<BnplProjection>> = _bnplProjections.asStateFlow()

    fun refreshBnplProjections(targetMonth: YearMonth) {
        val useCase = CalculateBnplProjectionsUseCase()
        _bnplProjections.value = useCase.execute(
            allTransactions = allTransactions.value,
            allPaymentMethods = allPaymentMethods.value,
            paypalDetails = allPaypalDetails.value,
            klarnaDetails = allKlarnaDetails.value,
            targetMonth = targetMonth,
        )
    }

    // GESTIONE METODI DI PAGAMENTO
    fun addPaymentMethod(
        paymentMethod: PaymentMethodEntity,
        closingDay: Int = 0,
        paymentDay: Int = 0,
        debitIssuer: String? = null,
        debitCardNumber: String? = null,
        debitNotes: String? = null,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            managePaymentMethodUseCase.add(paymentMethod)
            if (
                paymentMethod.provider == PaymentProvider.CREDIT_CARD_SALDO.name ||
                paymentMethod.provider == PaymentProvider.CREDIT_CARD_REVOLVING.name
            ) {
                repository.insertCreditCardDetail(
                    CreditCardDetailEntity(
                        paymentMethodId = paymentMethod.id,
                        cardType = if (paymentMethod.provider == PaymentProvider.CREDIT_CARD_SALDO.name) {
                            CreditCardType.SALDO.name
                        } else {
                            CreditCardType.REVOLVING.name
                        },
                        limit = 0.0,
                        closingDay = closingDay,
                        paymentDay = paymentDay,
                    ),
                )
            } else if (paymentMethod.provider == PaymentProvider.DEBIT_CARD.name) {
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
        return when (PaymentProvider.valueOf(method.provider)) {
            PaymentProvider.CREDIT_CARD_SALDO,
            PaymentProvider.CREDIT_CARD_REVOLVING,
            -> {
                repository.getCreditCardDetail(method.id)?.let {
                    PaymentMethodDetails.CreditCard(
                        name = method.name,
                        cardType = CreditCardType.valueOf(it.cardType),
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

    fun updateCcLimit(limit: Float) {
        _ccLimit.value = limit
        prefs?.edit { putFloat(KEY_CC_LIMIT, limit) }
    }

    fun updateCcDelay(delay: Int) {
        _ccDelay.value = delay
        prefs?.edit { putInt(KEY_CC_DELAY, delay) }
    }

    fun updateCcPaymentMode(mode: String) {
        _ccPaymentMode.value = mode
        prefs?.edit { putString(KEY_CC_PAYMENT_MODE, mode) }
    }

    fun updateIsAmountHidden(isHidden: Boolean) {
        _isAmountHidden.value = isHidden
        prefs?.edit { putBoolean(KEY_HIDE_AMOUNT, isHidden) }
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
        viewModelScope.launch(Dispatchers.IO) { repository.insertAllTransactions(list) }
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
