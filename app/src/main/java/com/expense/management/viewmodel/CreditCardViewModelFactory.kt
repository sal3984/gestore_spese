package com.expense.management.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.expense.management.data.AppDatabase
import com.expense.management.data.ExpenseRepository
import com.expense.management.domain.usecase.DeleteTransactionUseCase
import com.expense.management.domain.usecase.SaveTransactionUseCase
import com.expense.management.utils.CurrencyUtils

class CreditCardViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreditCardViewModel::class.java)) {
            val db = AppDatabase.getDatabase(context)
            val repository = ExpenseRepository(
                db.transactionDao(),
                db.categoryDao(),
                db.currencyDao(),
                db.creditCardDao(),
                db.paymentMethodDao(),
            )
            val currencyUtils = CurrencyUtils(db.currencyDao())

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
