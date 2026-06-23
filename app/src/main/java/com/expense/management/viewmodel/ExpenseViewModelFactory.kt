package com.expense.management.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.expense.management.data.AppDatabase
import com.expense.management.data.ExpenseRepository
import com.expense.management.data.PaymentMethodRepositoryAdapter
import com.expense.management.domain.usecase.CalculateAmexCurrentAccountOutflowUseCase
import com.expense.management.domain.usecase.CalculateAmexStatementUseCase
import com.expense.management.domain.usecase.CalculateBnplProjectionsUseCase
import com.expense.management.domain.usecase.CalculateCurrentAccountCashFlowUseCase
import com.expense.management.domain.usecase.CalculateReportUseCase
import com.expense.management.domain.usecase.CalculateSatispayStatusUseCase
import com.expense.management.domain.usecase.DeleteTransactionUseCase
import com.expense.management.domain.usecase.GenerateCreditCardPaymentUseCase
import com.expense.management.domain.usecase.GetActiveCreditCardsUseCase
import com.expense.management.domain.usecase.GetBackupDataUseCase
import com.expense.management.domain.usecase.GetCategoriesUseCase
import com.expense.management.domain.usecase.GetFrequentCategoriesUseCase
import com.expense.management.domain.usecase.GetPaymentMethodsUseCase
import com.expense.management.domain.usecase.GetTransactionsUseCase
import com.expense.management.domain.usecase.InitializeCategoriesUseCase
import com.expense.management.domain.usecase.ManagePaymentMethodUseCase
import com.expense.management.domain.usecase.RestoreDataUseCase
import com.expense.management.domain.usecase.SaveTransactionUseCase
import com.expense.management.domain.usecase.ScanReceiptUseCase
import com.expense.management.utils.CurrencyUtils

class ExpenseViewModelFactory(
    private val context: Context,
    private val sharedRepository: ExpenseRepository? = null,
    private val sharedCurrencyUtils: CurrencyUtils? = null,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            val repository = sharedRepository ?: run {
                val db = AppDatabase.getDatabase(context)
                ExpenseRepository(
                    db.transactionDao(),
                    db.categoryDao(),
                    db.currencyDao(),
                    db.paymentMethodDao(),
                    db.amexDao(),
                )
            }
            val currencyUtils = sharedCurrencyUtils ?: CurrencyUtils(
                AppDatabase.getDatabase(context).currencyDao(),
            )
            val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)

            val expensePaymentMethodRepo = PaymentMethodRepositoryAdapter(repository)
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
                getActiveCreditCardsUseCase = GetActiveCreditCardsUseCase(expensePaymentMethodRepo),
                getBackupDataUseCase = GetBackupDataUseCase(repository),
                restoreDataUseCase = RestoreDataUseCase(repository),
                getFrequentCategoriesUseCase = GetFrequentCategoriesUseCase(repository),
                calculateReportUseCase = CalculateReportUseCase(),
                scanReceiptUseCase = ScanReceiptUseCase(),
                generateCreditCardPaymentUseCase = GenerateCreditCardPaymentUseCase(),
                calculateAmexCurrentAccountOutflowUseCase = CalculateAmexCurrentAccountOutflowUseCase(),
                calculateCurrentAccountCashFlowUseCase = CalculateCurrentAccountCashFlowUseCase(),
                calculateAmexStatementUseCase = CalculateAmexStatementUseCase(),
            ) as T
        }
        if (modelClass.isAssignableFrom(PaymentMethodsViewModel::class.java)) {
            val repository = sharedRepository ?: run {
                val db = AppDatabase.getDatabase(context)
                ExpenseRepository(
                    db.transactionDao(),
                    db.categoryDao(),
                    db.currencyDao(),
                    db.paymentMethodDao(),
                    db.amexDao(),
                )
            }
            val paymentMethodRepo = PaymentMethodRepositoryAdapter(repository)
            val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
            @Suppress("UNCHECKED_CAST")
            return PaymentMethodsViewModel(
                repository = repository,
                prefs = prefs,
                paymentMethodRepo = paymentMethodRepo,
                getPaymentMethodsUseCase = GetPaymentMethodsUseCase(paymentMethodRepo),
                managePaymentMethodUseCase = ManagePaymentMethodUseCase(paymentMethodRepo),
                getActiveCreditCardsUseCase = GetActiveCreditCardsUseCase(paymentMethodRepo),
                getCategoriesUseCase = GetCategoriesUseCase(repository),
                getTransactionsUseCase = GetTransactionsUseCase(repository),
                saveTransactionUseCase = SaveTransactionUseCase(repository),
                deleteTransactionUseCase = DeleteTransactionUseCase(repository),
                calculateBnplProjectionsUseCase = CalculateBnplProjectionsUseCase(),
                calculateSatispayStatusUseCase = CalculateSatispayStatusUseCase(repository),
                generateCreditCardPaymentUseCase = GenerateCreditCardPaymentUseCase(),
            ) as T
        }
        if (modelClass.isAssignableFrom(AmexViewModel::class.java)) {
            val repository = sharedRepository ?: run {
                val db = AppDatabase.getDatabase(context)
                ExpenseRepository(
                    db.transactionDao(),
                    db.categoryDao(),
                    db.currencyDao(),
                    db.paymentMethodDao(),
                    db.amexDao(),
                )
            }
            val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
            @Suppress("UNCHECKED_CAST")
            return AmexViewModel(
                repository = repository,
                prefs = prefs,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
