package com.expense.management.domain.usecase

import com.expense.management.data.ExpenseRepository
import com.expense.management.viewmodel.BackupData

class GetBackupDataUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(): BackupData = BackupData(
        transactions = repository.getAllTransactionsList(),
        categories = repository.getAllCategories(),
        creditCard = repository.getAllCreditCard(),
    )
}

class RestoreDataUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(backupData: BackupData) {
        repository.insertAllTransactions(backupData.transactions)
        repository.insertAllCategories(backupData.categories)
        repository.insertAllCreditCard(backupData.creditCard ?: emptyList())
    }
}
