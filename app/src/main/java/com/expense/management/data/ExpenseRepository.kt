package com.expense.management.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) {

    // Transactions
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllFlow()

    suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insert(transaction)
    }

    suspend fun deleteTransaction(id: String) {
        transactionDao.delete(id)
    }

    suspend fun deleteTransactionGroup(groupId: String) {
        transactionDao.deleteByGroupId(groupId)
    }

    suspend fun getTransactionById(id: String): TransactionEntity? {
        return transactionDao.getById(id)
    }

    suspend fun insertAllTransactions(transactions: List<TransactionEntity>) {
        transactionDao.insertAll(transactions)
    }

    suspend fun getAllTransactionsList(): List<TransactionEntity> {
        return transactionDao.getAllList()
    }

    suspend fun getMinEffectiveDate(): String? {
        return transactionDao.getMinEffectiveDate()
    }

    suspend fun getDescriptionSuggestions(query: String): List<String> {
        return transactionDao.getDescriptionSuggestions(query)
    }

    // Categories
    val allCategoriesFlow: Flow<List<CategoryEntity>> = categoryDao.getAllCategoriesFlow()

    suspend fun getAllCategories(): List<CategoryEntity> {
        return categoryDao.getAllCategories()
    }

    suspend fun insertCategory(category: CategoryEntity) {
        categoryDao.insertCategory(category)
    }

    suspend fun deleteCategoryById(id: String) {
        categoryDao.deleteCategoryById(id)
    }

    suspend fun insertAllCategories(categories: List<CategoryEntity>) {
        categoryDao.insertAllCategories(categories)
    }

}
