package com.expense.management.viewmodel

import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.expense.management.domain.usecase.GetTransactionsUseCase
import com.expense.management.domain.usecase.InitializeCategoriesUseCase
import com.expense.management.domain.usecase.ManageCreditCardUseCase
import com.expense.management.domain.usecase.RestoreDataUseCase
import com.expense.management.domain.usecase.SaveTransactionUseCase
import com.expense.management.ui.screens.DeleteType
import com.expense.management.ui.screens.category.CATEGORIES
import com.expense.management.utils.CurrencyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
) : ViewModel() {

    // MODIFICA: Inizializza lo stato di sblocco in base alla preferenza.
    var isAppUnlocked = mutableStateOf(!(prefs?.getBoolean("is_biometric_enabled", false) ?: false))

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
    private val _currency = MutableStateFlow(prefs?.getString("currency", "€") ?: "€")
    val currency = _currency.asStateFlow()

    private val _ccLimit = MutableStateFlow(prefs?.getFloat("cc_limit", 1500f) ?: 1500f)
    val ccLimit = _ccLimit.asStateFlow()

    private val _ccDelay = MutableStateFlow(prefs?.getInt("cc_delay", 1) ?: 1)
    val ccDelay = _ccDelay.asStateFlow()

    private val _dateFormat = MutableStateFlow(prefs?.getString("date_format", "dd/MM/yyyy") ?: "dd/MM/yyyy")
    val dateFormat = _dateFormat.asStateFlow()

    private val _isAmountHidden = MutableStateFlow(prefs?.getBoolean("hide_amount", false) ?: false)
    val isAmountHidden = _isAmountHidden.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs?.getBoolean("is_biometric_enabled", false) ?: false)
    val isBiometricEnabled = _isBiometricEnabled.asStateFlow()

    private val _ccPaymentMode = MutableStateFlow(prefs?.getString("cc_payment_mode", "single") ?: "single")
    val ccPaymentMode = _ccPaymentMode.asStateFlow()

    private val _earliestMonth = MutableStateFlow(YearMonth.now())
    val earliestMonth = _earliestMonth.asStateFlow()

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
        prefs?.getStringSet("csv_export_columns", defaultExportColumns) ?: defaultExportColumns,
    )
    val csvExportColumns = _csvExportColumns.asStateFlow()

    init {
        viewModelScope.launch {
            initializeCategoriesUseCase()
            loadEarliestMonth()
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

    private suspend fun loadEarliestMonth() {
        allTransactions.collectLatest { _ ->
            val minDateString = repository.getMinEffectiveDate()
            if (minDateString != null) {
                try {
                    val minDate = LocalDate.parse(minDateString)
                    _earliestMonth.value = YearMonth.from(minDate)
                } catch (_: Exception) {
                    _earliestMonth.value = YearMonth.now()
                }
            } else {
                _earliestMonth.value = YearMonth.now()
            }
        }
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

    suspend fun getTransactionById(id: String): TransactionEntity? = withContext(Dispatchers.IO) { repository.getTransactionById(id) }

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
        prefs?.edit { putString("currency", symbol) }
    }

    fun updateDateFormat(format: String) {
        _dateFormat.value = format
        prefs?.edit { putString("date_format", format) }
    }

    fun updateCcLimit(limit: Float) {
        _ccLimit.value = limit
        prefs?.edit { putFloat("cc_limit", limit) }
    }

    fun updateCcDelay(delay: Int) {
        _ccDelay.value = delay
        prefs?.edit { putInt("cc_delay", delay) }
    }

    fun updateCcPaymentMode(mode: String) {
        _ccPaymentMode.value = mode
        prefs?.edit { putString("cc_payment_mode", mode) }
    }

    fun updateIsAmountHidden(isHidden: Boolean) {
        _isAmountHidden.value = isHidden
        prefs?.edit { putBoolean("hide_amount", isHidden) }
    }

    fun updateBiometricEnabled(isEnabled: Boolean) {
        _isBiometricEnabled.value = isEnabled
        prefs?.edit { putBoolean("is_biometric_enabled", isEnabled) }
    }

    fun updateCsvExportColumns(columns: Set<String>) {
        _csvExportColumns.value = columns
        prefs?.edit { putStringSet("csv_export_columns", columns) }
    }

    fun refreshCurrencyRates() {
        viewModelScope.launch {
            _currencyRatesUpdate.value = currencyUtils.getLastUpdate()
            refreshCurrencyRatesData()
        }
    }

    fun forceCurrencyRatesUpdate(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = currencyUtils.forceUpdate()
            if (success) {
                _currencyRatesUpdate.value = currencyUtils.getLastUpdate()
                refreshCurrencyRatesData()
            }
            onResult(success)
        }
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

data class BackupData(
    val transactions: List<TransactionEntity>,
    val categories: List<CategoryEntity>,
    val creditCard: List<CreditCardEntity>?,
)
