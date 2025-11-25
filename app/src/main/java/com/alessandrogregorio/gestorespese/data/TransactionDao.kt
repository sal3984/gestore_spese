package com.alessandrogregorio.gestorespese.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow


// 2. Il DAO (Le query)
@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY effectiveDate DESC")
    fun getAllFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions")
    suspend fun getAllList(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)

    // NUOVO: Ottieni una transazione per ID
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    // NUOVO: Cerca descrizioni uniche per l'autocomplete
    @Query("SELECT DISTINCT description FROM transactions WHERE description LIKE :query || '%' LIMIT 5")
    fun getDescriptionSuggestions(query: String): List<String>
}