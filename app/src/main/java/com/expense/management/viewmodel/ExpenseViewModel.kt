package com.expense.management.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.expense.management.data.AppDatabase
import com.expense.management.data.CategoryEntity
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

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.transactionDao()
    private val categoryDao = db.categoryDao()
    private val prefs = application.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    // Aggiungi questa variabile nel ViewModel
    var isAppUnlocked = mutableStateOf(false)

    // Stato per i suggerimenti (inizialmente vuoto)
    private val _descriptionSuggestions = MutableStateFlow<List<String>>(emptyList())
    val descriptionSuggestions: StateFlow<List<String>> = _descriptionSuggestions.asStateFlow()


    // Dati Transazioni
    val allTransactions: StateFlow<List<TransactionEntity>> = dao.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // DATI CATEGORIE: Unione di quelle nel DB e quelle di Default (in memory)
    // Questo assicura che le categorie di default siano sempre visibili anche se il DB è vuoto o l'inizializzazione fallisce
    val allCategories: StateFlow<List<CategoryEntity>> = categoryDao.getAllCategoriesFlow()
        .map { dbCategories ->
            // Crea un set degli ID già presenti nel DB per evitare duplicati
            val dbIds = dbCategories.map { it.id }.toSet()

            // Identifica le categorie di default che mancano nel DB
            val missingDefaults = CATEGORIES.filter { it.id !in dbIds }.map {
                CategoryEntity(
                    id = it.id,
                    label = it.label,
                    icon = it.icon,
                    type = it.type,
                    isCustom = false
                )
            }
            // Restituisce la lista combinata (DB + Default mancanti)
            dbCategories + missingDefaults
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- STATO IMPOSTAZIONI ---
    private val _currency = MutableStateFlow(prefs.getString("currency", "€") ?: "€")
    val currency = _currency.asStateFlow()

    private val _ccLimit = MutableStateFlow(prefs.getFloat("cc_limit", 1500f)) // Plafond default 1500€
    val ccLimit = _ccLimit.asStateFlow()

    private val _ccDelay = MutableStateFlow(prefs.getInt("cc_delay", 1))
    val ccDelay = _ccDelay.asStateFlow()

    private val _dateFormat = MutableStateFlow(prefs.getString("date_format", "dd/MM/yyyy") ?: "dd/MM/yyyy")
    val dateFormat = _dateFormat.asStateFlow()

    // NUOVO: Stato per oscurare gli importi
    private val _isAmountHidden = MutableStateFlow(prefs.getBoolean("hide_amount", false))
    val isAmountHidden = _isAmountHidden.asStateFlow()

    // NUOVO: Stato per il blocco biometrico
    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean("is_biometric_enabled", false))
    val isBiometricEnabled = _isBiometricEnabled.asStateFlow()

    // NUOVO: Mese della transazione più vecchia per la navigazione
    private val _earliestMonth = MutableStateFlow(YearMonth.now())
    val earliestMonth = _earliestMonth.asStateFlow()

    // STATO NAVIGAZIONE DASHBOARD
    // Mantiene il mese visualizzato sulla dashboard anche dopo navigazioni
    private val _currentDashboardMonth = MutableStateFlow(YearMonth.now())
    val currentDashboardMonth = _currentDashboardMonth.asStateFlow()


    init {
        // Carica il mese più vecchio all'avvio
        viewModelScope.launch {
            loadEarliestMonth()
            // Inizializza le categorie di default nel DB (persistenza)
            ensureCategoriesInitialized()
        }
    }

    // Assicura che le categorie siano salvate anche nel DB per coerenza futura
    fun ensureCategoriesInitialized() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existingCategories = categoryDao.getAllCategories()
                val existingIds = existingCategories.map { it.id }.toSet()

                val categoriesToAdd = CATEGORIES.filter { it.id !in existingIds }.map {
                    CategoryEntity(
                        id = it.id,
                        label = it.label,
                        icon = it.icon,
                        type = it.type,
                        isCustom = false
                    )
                }

                if (categoriesToAdd.isNotEmpty()) {
                    categoryDao.insertAllCategories(categoriesToAdd)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // GESTIONE CATEGORIE
    fun addCategory(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            categoryDao.insertCategory(category)
        }
    }

    fun removeCategory(id: String) {
         viewModelScope.launch(Dispatchers.IO) {
            categoryDao.deleteCategoryById(id)
        }
    }

    fun updateDashboardMonth(month: YearMonth) {
        _currentDashboardMonth.value = month
    }

    private suspend fun loadEarliestMonth() {
        // Questo viene lanciato ogni volta che la lista delle transazioni cambia (dal DAO)
        allTransactions.collect { transactions ->
            val minDateString = dao.getMinEffectiveDate()
            if (minDateString != null) {
                try {
                    val minDate = LocalDate.parse(minDateString)
                    _earliestMonth.value = YearMonth.from(minDate)
                } catch (e: Exception) {
                    // Se la data è invalida, resta su YearMonth.now()
                    _earliestMonth.value = YearMonth.now()
                }
            } else {
                _earliestMonth.value = YearMonth.now()
            }
        }
    }

    // --- AZIONI ---

    // Aggiungi/Aggiorna (OnConflictStrategy.REPLACE gestisce entrambi i casi)
    fun saveTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insert(transaction)
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch(Dispatchers.IO) { dao.delete(id) }
    }

    // NUOVO: Ottiene una transazione specifica
    suspend fun getTransactionById(id: String): TransactionEntity? {
        return withContext(Dispatchers.IO) { dao.getById(id) }
    }

    // Funzione per l'Autocomplete
    suspend fun getSuggestions(query: String): List<String> {
        return if (query.length < 2) emptyList()
        else withContext(Dispatchers.IO) { dao.getDescriptionSuggestions(query) }
    }

    // Aggiornamento Impostazioni
    fun updateCurrency(symbol: String) {
        _currency.value = symbol
        prefs.edit().putString("currency", symbol).apply()
    }

    fun updateDateFormat(format: String) {
        _dateFormat.value = format
        prefs.edit().putString("date_format", format).apply()
    }

    fun updateCcLimit(limit: Float) {
        _ccLimit.value = limit
        prefs.edit().putFloat("cc_limit", limit).apply()
    }

    fun updateCcDelay(delay: Int) {
        _ccDelay.value = delay
        prefs.edit().putInt("cc_delay", delay).apply()
    }

    // --- NUOVO: Funzione di aggiornamento isAmountHidden ---
    fun updateIsAmountHidden(isHidden: Boolean) {
        _isAmountHidden.value = isHidden
        prefs.edit().putBoolean("hide_amount", isHidden).apply()
    }

    // --- NUOVO: Funzione di aggiornamento isBiometricEnabled ---
    fun updateBiometricEnabled(isEnabled: Boolean) {
        _isBiometricEnabled.value = isEnabled
        prefs.edit().putBoolean("is_biometric_enabled", isEnabled).apply()
    }

    // Metodi Backup
    suspend fun getAllForBackup(): BackupData {
        return BackupData(
            transactions = dao.getAllList(),
            categories = categoryDao.getAllCategories()
        )
    }

    fun restoreData(backupData: BackupData) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertAll(backupData.transactions)
            categoryDao.insertAllCategories(backupData.categories)
        }
    }

    fun restoreLegacyData(list: List<TransactionEntity>) {
        viewModelScope.launch(Dispatchers.IO) { dao.insertAll(list) }
    }

    // NUOVO: Metodo per ottenere solo le spese per l'esportazione CSV
    suspend fun getExpensesForExport(): List<TransactionEntity> {
        // Filtra in memoria per semplicità (la query SQL sarebbe complessa con JOIN e filtraggi)
        return dao.getAllList().filter { it.type == "expense" }
    }

    // Funzione per cercare le descrizioni nel Database
    fun searchDescriptionSuggestions(query: String) {
        viewModelScope.launch {
            if (query.length >= 2) { // Cerca solo se l'utente ha digitato almeno 2 caratteri
                // Assumendo che tu abbia un repository, chiamalo qui.
                // Se usi direttamente il DAO: dao.getDescriptionSuggestions(query)
                val results = dao.getDescriptionSuggestions(query)
                _descriptionSuggestions.value = results
            } else {
                _descriptionSuggestions.value = emptyList()
            }
        }
    }
}



data class BackupData(
    val transactions: List<TransactionEntity>,
    val categories: List<CategoryEntity>
)
