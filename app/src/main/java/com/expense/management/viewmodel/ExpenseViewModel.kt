package com.expense.management.viewmodel

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expense.management.data.BackupData
import com.expense.management.data.CategoryEntity
import com.expense.management.data.CreditCardEntity
import com.expense.management.data.CurrencyRate
import com.expense.management.data.ExpenseRepository
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.usecase.DeleteTransactionUseCase
import com.expense.management.domain.usecase.GetBackupDataUseCase
import com.expense.management.domain.usecase.GetCategoriesUseCase
import com.expense.management.domain.usecase.GetCreditCardsUseCase
import com.expense.management.domain.usecase.GetFrequentCategoriesUseCase
import com.expense.management.domain.usecase.GetTransactionsUseCase
import com.expense.management.domain.usecase.InitializeCategoriesUseCase
import com.expense.management.domain.usecase.ManageCreditCardUseCase
import com.expense.management.domain.usecase.RestoreDataUseCase
import com.expense.management.domain.usecase.SaveTransactionUseCase
import com.expense.management.ui.model.DeleteType
import com.expense.management.ui.screens.category.CATEGORIES
import com.expense.management.utils.CurrencyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

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

    // DATI CATEGORIE
    val allCategories: StateFlow<List<CategoryEntity>> =
        getCategoriesUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // DATI CARTE DI CREDITO
    val allCreditCards: StateFlow<List<CreditCardEntity>> =
        getCreditCardsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
