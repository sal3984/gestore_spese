package com.expense.management.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CurrencyDao {
    // Ottieni tutti i tassi salvati
    @Query("SELECT * FROM currency_rates")
    suspend fun getAllRates(): List<CurrencyRate>

    // Ottieni un tasso specifico
    @Query("SELECT * FROM currency_rates WHERE currencyCode = :code")
    suspend fun getRate(code: String): CurrencyRate?

    // Salva o aggiorna una lista di tassi
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<CurrencyRate>)

    // Controlla quando è stato fatto l'ultimo aggiornamento globale (prendendo il più recente)
    @Query("SELECT MAX(lastUpdatedTimestamp) FROM currency_rates")
    suspend fun getLastUpdateTimestamp(): Long?
}
