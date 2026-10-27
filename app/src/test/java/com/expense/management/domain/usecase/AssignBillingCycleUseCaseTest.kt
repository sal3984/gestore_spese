package com.expense.management.domain.usecase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expense.management.data.AppDatabase
import com.expense.management.data.BackupData
import com.expense.management.data.CreditCardDetailEntity
import com.expense.management.data.CycleTotals
import com.expense.management.data.ExpenseRepository
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.PaymentProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Assegnazione del ciclo con repository vero su Room in memoria (foreign key attive). */
@RunWith(AndroidJUnit4::class)
class AssignBillingCycleUseCaseTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ExpenseRepository
    private lateinit var assign: AssignBillingCycleUseCase
    private lateinit var save: SaveTransactionUseCase

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ExpenseRepository(db.transactionDao(), db.categoryDao(), db.currencyDao(), db.paymentMethodDao(), db.amexDao(), db)
        assign = AssignBillingCycleUseCase(repository, today = { LocalDate.of(2025, 6, 25) })
        save = SaveTransactionUseCase(repository, assign)
        card("saldo1", "SALDO", closingDay = 20, paymentDay = 15)
        card("revolving1", "REVOLVING", closingDay = 20, paymentDay = 15)
        card("noDays", "SALDO", closingDay = 0, paymentDay = 0)
        repository.insertPaymentMethod(PaymentMethodEntity(id = "bank1", name = "Conto", provider = PaymentProvider.CASH))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun savingASaldoPurchaseCreatesItsCycleAndLinksIt() = runBlocking {
        save(purchase("a", "2025-05-20", 100.0))

        val cycle = requireNotNull(repository.getBillingCycleById("saldo1:2025-05"))
        assertEquals(listOf("2025-05", "2025-05-20", "2025-06-15", "PAID"), listOf(cycle.cycleMonth, cycle.closingDate, cycle.dueDate, cycle.status))
        assertEquals("saldo1:2025-05", repository.getTransactionById("a")?.billingCycleId)
        assertEquals("2025-06-15", repository.getTransactionById("a")?.effectiveDate)
    }

    @Test
    fun anInstallmentSavedWithoutCardDelayIsChargedWithItsCycle() = runBlocking {
        // Rata salvata con applyCcDelayToInstallments spento: effectiveDate = data della rata
        save(purchase("rata", "2025-06-10", 40.0).copy(effectiveDate = "2025-06-10", installmentNumber = 1, totalInstallments = 3))

        assertEquals("2025-07-15", repository.getTransactionById("rata")?.effectiveDate)
    }

    @Test
    fun aPurchaseLeftWithoutCycleKeepsItsEffectiveDate() = runBlocking {
        save(purchase("revolving", "2025-06-10", 100.0, card = "revolving1").copy(effectiveDate = "2025-07-01"))

        assertEquals("2025-07-01", repository.getTransactionById("revolving")?.effectiveDate)
    }

    @Test
    fun purchasesOfTheSameCycleShareOneCycle() = runBlocking {
        save(purchase("a", "2025-06-01", 100.0))
        save(purchase("b", "2025-06-20", 50.0))

        assertEquals(CycleTotals(150.0, 0.0, 2), repository.getBillingCycleTotals("saldo1:2025-06"))
        assertEquals("CLOSED", repository.getBillingCycleById("saldo1:2025-06")?.status)
        assertEquals(listOf("saldo1:2025-06"), repository.getUnsettledBillingCycles("saldo1").map { it.cycle.id })
    }

    @Test
    fun editingAPurchaseRecomputesItsCycle() = runBlocking {
        save(purchase("a", "2025-06-10", 100.0))

        // La UI salva una entity nuova, senza billingCycleId: il ciclo va ricalcolato dalla data
        save(purchase("a", "2025-06-21", 80.0))

        assertEquals("saldo1:2025-07", repository.getTransactionById("a")?.billingCycleId)
        assertEquals("OPEN", repository.getBillingCycleById("saldo1:2025-07")?.status)
        assertEquals(CycleTotals(0.0, 0.0, 0), repository.getBillingCycleTotals("saldo1:2025-06"))
        assertEquals(CycleTotals(80.0, 0.0, 1), repository.getBillingCycleTotals("saldo1:2025-07"))
    }

    @Test
    fun movingAPurchaseOffTheCardDropsItsCycle() = runBlocking {
        save(purchase("a", "2025-06-10", 100.0))

        // Copia della riga salvata: arriva con il ciclo vecchio, che non deve sopravvivere
        save(requireNotNull(repository.getTransactionById("a")).copy(paymentMethodId = "bank1", isCreditCard = false))

        assertNull(repository.getTransactionById("a")?.billingCycleId)
    }

    @Test
    fun onlySaldoCardPurchasesGetACycle() = runBlocking {
        save(purchase("refund", "2025-06-10", 20.0, type = TransactionType.INCOME))
        save(purchase("revolving", "2025-06-10", 100.0, card = "revolving1"))
        save(purchase("noDays", "2025-06-10", 100.0, card = "noDays"))
        save(purchase("cash", "2025-06-10", 100.0, card = "bank1", isCreditCard = false))

        listOf("refund", "revolving", "noDays", "cash").forEach { assertNull(it, repository.getTransactionById(it)?.billingCycleId) }
        assertEquals(emptyList<Any>(), repository.getUnsettledBillingCycles("revolving1"))
        assertEquals(0, repository.getCardExpensesWithoutCycle().count { it.paymentMethodId == "saldo1" })
    }

    @Test
    fun theCardIsFoundInTheLegacyColumnToo() = runBlocking {
        save(purchase("legacy", "2025-06-10", 100.0, card = "bank1").copy(creditCardId = "saldo1"))

        assertEquals("saldo1:2025-06", repository.getTransactionById("legacy")?.billingCycleId)
    }

    @Test
    fun restoringABackupRelinksPurchasesToFreshCycles() = runBlocking {
        save(purchase("a", "2025-06-10", 100.0))
        val backup = BackupData(
            // Il backup porta un billingCycleId che dopo la cancellazione non esiste piu'
            transactions = listOf(requireNotNull(repository.getTransactionById("a")), purchase("b", "2025-06-21", 30.0)),
            categories = emptyList(),
            creditCard = null,
            paymentMethods = listOf(PaymentMethodEntity(id = "saldo1", name = "saldo1", provider = PaymentProvider.CREDIT_CARD_SALDO)),
            creditCardDetails = listOf(CreditCardDetailEntity(paymentMethodId = "saldo1", cardType = "SALDO", limit = 3000.0, closingDay = 20, paymentDay = 15)),
        )

        RestoreDataUseCase(repository, assign)(backup)

        assertEquals("saldo1:2025-06", repository.getTransactionById("a")?.billingCycleId)
        assertEquals("saldo1:2025-07", repository.getTransactionById("b")?.billingCycleId)
        assertEquals("2025-08-15", repository.getTransactionById("b")?.effectiveDate)
        assertEquals(0, assign.assignMissing())
    }

    private suspend fun card(id: String, cardType: String, closingDay: Int, paymentDay: Int) {
        repository.insertPaymentMethod(PaymentMethodEntity(id = id, name = id, provider = PaymentProvider.CREDIT_CARD_SALDO))
        repository.insertCreditCardDetail(
            CreditCardDetailEntity(paymentMethodId = id, cardType = cardType, limit = 3000.0, closingDay = closingDay, paymentDay = paymentDay),
        )
    }

    private fun purchase(
        id: String,
        date: String,
        amount: Double,
        card: String = "saldo1",
        isCreditCard: Boolean = true,
        type: TransactionType = TransactionType.EXPENSE,
    ) = TransactionEntity(
        id = id,
        date = date,
        description = id,
        amount = amount,
        categoryId = "shopping",
        type = type,
        isCreditCard = isCreditCard,
        effectiveDate = date,
        originalAmount = amount,
        originalCurrency = "EUR",
        paymentMethodId = card,
    )
}
