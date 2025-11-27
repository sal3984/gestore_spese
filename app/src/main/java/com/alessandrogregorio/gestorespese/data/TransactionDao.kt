package com.alessandrogregorio.gestorespese.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow


// 2. Il DAO (Le query)
@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY effectiveDate ASC")
    fun getAllFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllList(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    // AGGIORNATO: L'ID per la cancellazione è String
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: String)

    // AGGIORNATO: L'ID per l'ottenimento è String
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    // NUOVO: Cerca descrizioni uniche per l'autocomplete
    @Query("SELECT DISTINCT description FROM transactions WHERE description LIKE :query || '%' LIMIT 5")
    fun getDescriptionSuggestions(query: String): List<String>

    // NUOVO: Ottieni la data di addebito più vecchia
    @Query("SELECT MIN(effectiveDate) FROM transactions")
    suspend fun getMinEffectiveDate(): String?
}
