package com.expense.management.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM transactions WHERE groupId = :groupId")
    suspend fun deleteByGroupId(groupId: String)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE groupId = :groupId")
    suspend fun getByGroupId(groupId: String): List<TransactionEntity>

    @Query("SELECT DISTINCT description FROM transactions WHERE description LIKE :query || '%' LIMIT 5")
    suspend fun getDescriptionSuggestions(query: String): List<String>

    @Query("SELECT MIN(effectiveDate) FROM transactions")
    suspend fun getMinEffectiveDate(): String?

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("UPDATE transactions SET creditCardId = NULL WHERE creditCardId = :cardId")
    suspend fun nullifyCreditCardId(cardId: String)

    @Query("UPDATE transactions SET paymentMethodId = NULL WHERE paymentMethodId = :methodId")
    suspend fun nullifyPaymentMethodId(methodId: String)
}
