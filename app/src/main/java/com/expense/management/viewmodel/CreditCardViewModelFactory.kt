package com.expense.management.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.expense.management.data.ExpenseRepository
import com.expense.management.domain.usecase.DeleteTransactionUseCase
import com.expense.management.domain.usecase.SaveTransactionUseCase
import com.expense.management.utils.CurrencyUtils

class CreditCardViewModelFactory(
    private val repository: ExpenseRepository,
    private val currencyUtils: CurrencyUtils,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreditCardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreditCardViewModel(
                repository = repository,
                currencyUtils = currencyUtils,
                saveTransactionUseCase = SaveTransactionUseCase(repository),
                deleteTransactionUseCase = DeleteTransactionUseCase(repository),
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
