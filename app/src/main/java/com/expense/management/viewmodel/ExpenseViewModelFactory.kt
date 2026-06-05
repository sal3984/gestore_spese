package com.expense.management.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.expense.management.data.AppDatabase
import com.expense.management.data.ExpenseRepository
import com.expense.management.domain.usecase.CalculateReportUseCase
import com.expense.management.domain.usecase.DeleteTransactionUseCase
import com.expense.management.domain.usecase.GetBackupDataUseCase
import com.expense.management.domain.usecase.GetCategoriesUseCase
import com.expense.management.domain.usecase.GetCreditCardsUseCase
import com.expense.management.domain.usecase.GetFrequentCategoriesUseCase
import com.expense.management.domain.usecase.GetPaymentMethodsUseCase
import com.expense.management.domain.usecase.GetTransactionsUseCase
import com.expense.management.domain.usecase.InitializeCategoriesUseCase
import com.expense.management.domain.usecase.ManageCreditCardUseCase
import com.expense.management.domain.usecase.ManagePaymentMethodUseCase
import com.expense.management.domain.usecase.RestoreDataUseCase
import com.expense.management.domain.usecase.SaveTransactionUseCase
import com.expense.management.domain.usecase.ScanReceiptUseCase
import com.expense.management.utils.CurrencyUtils

class ExpenseViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            val db = AppDatabase.getDatabase(context)
            val repository = ExpenseRepository(
                db.transactionDao(),
                db.categoryDao(),
                db.currencyDao(),
                db.creditCardDao(),
                db.paymentMethodDao(),
            )
            val currencyUtils = CurrencyUtils(db.currencyDao())
            val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)

            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(
                repository = repository,
                currencyUtils = currencyUtils,
                prefs = prefs,
                getTransactionsUseCase = GetTransactionsUseCase(repository),
                getCategoriesUseCase = GetCategoriesUseCase(repository),
                saveTransactionUseCase = SaveTransactionUseCase(repository),
                deleteTransactionUseCase = DeleteTransactionUseCase(repository),
                initializeCategoriesUseCase = InitializeCategoriesUseCase(repository),
                getCreditCardsUseCase = GetCreditCardsUseCase(repository),
                manageCreditCardUseCase = ManageCreditCardUseCase(repository),
                getPaymentMethodsUseCase = GetPaymentMethodsUseCase(repository),
                managePaymentMethodUseCase = ManagePaymentMethodUseCase(repository),
                getBackupDataUseCase = GetBackupDataUseCase(repository),
                restoreDataUseCase = RestoreDataUseCase(repository),
                getFrequentCategoriesUseCase = GetFrequentCategoriesUseCase(repository),
                calculateReportUseCase = CalculateReportUseCase(),
                scanReceiptUseCase = ScanReceiptUseCase(),
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
