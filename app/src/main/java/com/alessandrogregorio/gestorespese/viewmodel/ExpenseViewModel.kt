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

    // NUOVO: Formato Data (es. "dd/MM/yyyy" o "MM/dd/yyyy")
    private val _dateFormat = MutableStateFlow(prefs.getString("date_format", "dd/MM/yyyy") ?: "dd/MM/yyyy")
    val dateFormat = _dateFormat.asStateFlow()


    // --- AZIONI ---

    // Aggiungi/Aggiorna (OnConflictStrategy.REPLACE gestisce entrambi i casi)
    fun saveTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) { dao.insert(transaction) }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { dao.delete(id) }
    }

    // NUOVO: Ottiene una transazione specifica
    suspend fun getTransactionById(id: Long): TransactionEntity? {
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

    fun updateCcLimit(limit: Float) {
        _ccLimit.value = limit
        prefs.edit().putFloat("cc_limit", limit).apply()
    }

    fun updateCcDelay(delay: Int) {
        _ccDelay.value = delay
        prefs.edit().putInt("cc_delay", delay).apply()
    }

    // NUOVO: Aggiornamento Formato Data
    fun updateDateFormat(format: String) {
        _dateFormat.value = format
        prefs.edit().putString("date_format", format).apply()
    }


    // Metodi Backup
    suspend fun getAllForBackup() = dao.getAllList()

    fun restoreData(list: List<TransactionEntity>) {
        viewModelScope.launch(Dispatchers.IO) { dao.insertAll(list) }
    }

    // NUOVO: Metodo per ottenere solo le spese per l'esportazione CSV
    suspend fun getExpensesForExport(): List<TransactionEntity> {
        // Filtra in memoria per semplicità (la query con il WHERE sarebbe più efficiente)
        return withContext(Dispatchers.IO) {
            dao.getAllList().filter { it.type == "expense" }
        }
    }
}
