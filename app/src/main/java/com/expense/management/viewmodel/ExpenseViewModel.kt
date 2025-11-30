package com.expense.management.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.expense.management.data.AppDatabase
import com.expense.management.data.CategoryEntity
import com.expense.management.data.ExpenseRepository
import com.expense.management.data.TransactionEntity
import com.expense.management.ui.screens.category.CATEGORIES
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

class ExpenseViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)

    // Use Repository instead of DAOs directly
    private val repository = ExpenseRepository(db.transactionDao(), db.categoryDao())
    private val prefs = application.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    // MODIFICA: Inizializza lo stato di sblocco in base alla preferenza.
    // Se la biometria NON è abilitata, l'app è sbloccata di default (true).
    // Se la biometria È abilitata, l'app è bloccata (false).
    var isAppUnlocked = mutableStateOf(!prefs.getBoolean("is_biometric_enabled", false))

    // Dati Transazioni
    val allTransactions: StateFlow<List<TransactionEntity>> =
        repository.allTransactions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // DATI CATEGORIE
    val allCategories: StateFlow<List<CategoryEntity>> =
        repository.allCategoriesFlow
            .map { dbCategories ->
                val dbIds = dbCategories.map { it.id }.toSet()
                val missingDefaults =
                    CATEGORIES.filter { it.id !in dbIds }.map {
                        CategoryEntity(
                            id = it.id,
                            label = it.label,
                            icon = it.icon,
                            type = it.type,
                            isCustom = false,
                        )
                    }
                dbCategories + missingDefaults
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- STATO IMPOSTAZIONI ---
    private val _currency = MutableStateFlow(prefs.getString("currency", "€") ?: "€")
    val currency = _currency.asStateFlow()

    private val _ccLimit = MutableStateFlow(prefs.getFloat("cc_limit", 1500f))
    val ccLimit = _ccLimit.asStateFlow()

    private val _ccDelay = MutableStateFlow(prefs.getInt("cc_delay", 1))
    val ccDelay = _ccDelay.asStateFlow()

    private val _dateFormat = MutableStateFlow(prefs.getString("date_format", "dd/MM/yyyy") ?: "dd/MM/yyyy")
    val dateFormat = _dateFormat.asStateFlow()

    private val _isAmountHidden = MutableStateFlow(prefs.getBoolean("hide_amount", false))
    val isAmountHidden = _isAmountHidden.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean("is_biometric_enabled", false))
    val isBiometricEnabled = _isBiometricEnabled.asStateFlow()

    private val _ccPaymentMode = MutableStateFlow(prefs.getString("cc_payment_mode", "single") ?: "single")
    val ccPaymentMode = _ccPaymentMode.asStateFlow()

    private val _earliestMonth = MutableStateFlow(YearMonth.now())
    val earliestMonth = _earliestMonth.asStateFlow()

    private val _currentDashboardMonth = MutableStateFlow(YearMonth.now())
    val currentDashboardMonth = _currentDashboardMonth.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    init {
        viewModelScope.launch {
            loadEarliestMonth()
            ensureCategoriesInitialized()
        }
    }

    fun ensureCategoriesInitialized() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existingCategories = repository.getAllCategories()
                val existingIds = existingCategories.map { it.id }.toSet()

                val categoriesToAdd =
                    CATEGORIES.filter { it.id !in existingIds }.map {
                        CategoryEntity(
                            id = it.id,
                            label = it.label,
                            icon = it.icon,
                            type = it.type,
                            isCustom = false,
                        )
                    }

                if (categoriesToAdd.isNotEmpty()) {
                    repository.insertAllCategories(categoriesToAdd)
                }
            } catch (e: Exception) {
                e.printStackTrace()
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

    private suspend fun loadEarliestMonth() {
        allTransactions.collect { _ ->
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
            repository.insertTransaction(transaction)
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val transaction = repository.getTransactionById(id)
            if (transaction != null) {
                if (transaction.groupId != null) {
                    repository.deleteTransactionGroup(transaction.groupId)
                } else {
                    repository.deleteTransaction(id)
                }
            }
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
        prefs.edit { putString("currency", symbol) }
    }

    fun updateDateFormat(format: String) {
        _dateFormat.value = format
        prefs.edit { putString("date_format", format) }
    }

    fun updateCcLimit(limit: Float) {
        _ccLimit.value = limit
        prefs.edit { putFloat("cc_limit", limit) }
    }

    fun updateCcDelay(delay: Int) {
        _ccDelay.value = delay
        prefs.edit { putInt("cc_delay", delay) }
    }

    fun updateCcPaymentMode(mode: String) {
        _ccPaymentMode.value = mode
        prefs.edit { putString("cc_payment_mode", mode) }
    }

    fun updateIsAmountHidden(isHidden: Boolean) {
        _isAmountHidden.value = isHidden
        prefs.edit { putBoolean("hide_amount", isHidden) }
    }

    fun updateBiometricEnabled(isEnabled: Boolean) {
        _isBiometricEnabled.value = isEnabled
        prefs.edit { putBoolean("is_biometric_enabled", isEnabled) }
    }

    // Metodi Backup
    suspend fun getAllForBackup(): BackupData =
        BackupData(
            transactions = repository.getAllTransactionsList(),
            categories = repository.getAllCategories(),
        )

    fun restoreData(backupData: BackupData) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertAllTransactions(backupData.transactions)
            repository.insertAllCategories(backupData.categories)
        }
    }

    fun restoreLegacyData(list: List<TransactionEntity>) {
        viewModelScope.launch(Dispatchers.IO) { repository.insertAllTransactions(list) }
    }

    suspend fun getExpensesForExport(): List<TransactionEntity> = repository.getAllTransactionsList().filter { it.type == "expense" }
}

data class BackupData(
    val transactions: List<TransactionEntity>,
    val categories: List<CategoryEntity>,
)
