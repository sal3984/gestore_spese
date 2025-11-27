package com.alessandrogregorio.gestorespese.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alessandrogregorio.gestorespese.data.AppDatabase
import com.alessandrogregorio.gestorespese.data.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).transactionDao()
    private val prefs = application.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    // Dati Transazioni
    val allTransactions: StateFlow<List<TransactionEntity>> = dao.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
        prefs.edit().putBoolean("is_amount_hidden", isHidden).apply()
    }

    // Metodi Backup
    suspend fun getAllForBackup() = dao.getAllList()

    fun restoreData(list: List<TransactionEntity>) {
        viewModelScope.launch(Dispatchers.IO) { dao.insertAll(list) }
    }

    // NUOVO: Metodo per ottenere solo le spese per l'esportazione CSV
    suspend fun getExpensesForExport(): List<TransactionEntity> {
        // Filtra in memoria per semplicità (la query SQL sarebbe complessa con JOIN e filtraggi)
        return dao.getAllList().filter { it.type == "expense" }
    }
}
