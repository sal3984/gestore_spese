package com.expense.management.viewmodel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expense.management.data.AppDatabase
import com.expense.management.data.BillingCycleEntity
import com.expense.management.data.ExpenseRepository
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.utils.CurrencyUtils
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * ExpenseViewModel costruito dal factory di produzione su Room in memoria: copre i filtri che
 * vivono nel ViewModel (dashboard, report, conto corrente) e il passaggio dal modello di dominio.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ExpenseViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ExpenseRepository
    private lateinit var viewModel: ExpenseViewModel
    private val today = LocalDate.now().toString()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = ExpenseRepository(db.transactionDao(), db.categoryDao(), db.currencyDao(), db.paymentMethodDao(), db.amexDao(), db)
        viewModel = ExpenseViewModelFactory(context, repository, mockk<CurrencyUtils>(relaxed = true))
            .create(ExpenseViewModel::class.java)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun theCycleChargeStaysOutOfTheMonthListAndTheReportButCountsOnTheAccount() = runBlocking {
        repository.insertTransaction(transaction("purchase", 100.0, isCreditCard = true))
        repository.insertTransaction(
            transaction("charge", 150.0, isCreditCard = false).copy(sourceRef = BillingCycleEntity.settlementRef("visa1:2026-10")),
        )
        repository.insertTransaction(transaction("rent", 700.0, isCreditCard = false))

        val monthList = withTimeout(5_000) { viewModel.dashboardFilteredTransactions.first { it.size >= 2 } }
        val report = withTimeout(5_000) { viewModel.reportTransactions.first { it.size >= 2 } }
        val account = withTimeout(5_000) { viewModel.currentAccountCashFlow.first { it.outflows > 0.0 } }

        assertEquals(setOf("purchase", "rent"), monthList.map { it.id }.toSet())
        assertEquals(setOf("purchase", "rent"), report.map { it.id }.toSet())
        assertEquals(850.0, account.outflows, 0.001)
    }

    private fun transaction(id: String, amount: Double, isCreditCard: Boolean) = TransactionEntity(
        id = id,
        date = today,
        description = id,
        amount = amount,
        categoryId = "shopping",
        type = TransactionType.EXPENSE,
        isCreditCard = isCreditCard,
        effectiveDate = today,
        originalAmount = amount,
        originalCurrency = "EUR",
        paymentMethodId = if (isCreditCard) "visa1" else "bank1",
    )
}
