package com.expense.management.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expense.management.data.ExpenseRepository
import com.expense.management.data.TransactionEntity
import com.expense.management.domain.usecase.DeleteTransactionUseCase
import com.expense.management.domain.usecase.SaveTransactionUseCase
import com.expense.management.ui.model.DeleteType
import com.expense.management.utils.CurrencyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CreditCardViewModel(
    private val repository: ExpenseRepository,
    private val currencyUtils: CurrencyUtils,
    private val saveTransactionUseCase: SaveTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
) : ViewModel() {

    fun saveTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            saveTransactionUseCase(transaction)
        }
    }

    fun deleteTransaction(
        transactionId: String,
        deleteType: DeleteType,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            deleteTransactionUseCase(transactionId, deleteType)
        }
    }

    suspend fun getTransactionById(id: String): TransactionEntity? = repository.getTransactionById(id)

    suspend fun updateCurrencyRate(amount: Double, from: String, to: String): Double? {
        return currencyUtils.convert(amount, from, to)
    }
}
