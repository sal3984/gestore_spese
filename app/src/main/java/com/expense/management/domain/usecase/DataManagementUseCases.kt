package com.expense.management.domain.usecase

import com.expense.management.data.BackupData
import com.expense.management.data.ExpenseRepository

class GetBackupDataUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(): BackupData = BackupData(
        transactions = repository.getAllTransactionsList(),
        categories = repository.getAllCategories(),
        creditCard = repository.getAllCreditCard(),
    )
}

class RestoreDataUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(backupData: BackupData) {
        repository.deleteAllTransactions()
        repository.deleteAllCategories()
        repository.deleteAllCreditCards()
        repository.insertAllTransactions(backupData.transactions ?: emptyList())
        repository.insertAllCategories(backupData.categories ?: emptyList())
        repository.insertAllCreditCard(backupData.creditCard ?: emptyList())
    }
}
