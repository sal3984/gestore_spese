package com.expense.management.domain.usecase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expense.management.data.AppDatabase
import com.expense.management.data.BillingCycleEntity
import com.expense.management.data.CreditCardDetailEntity
import com.expense.management.data.ExpenseRepository
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.data.isCycleSettlement
import com.expense.management.domain.model.PaymentProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

/** Chiusura e liquidazione dei cicli con repository vero su Room in memoria. */
@RunWith(AndroidJUnit4::class)
class SettleBillingCycleUseCaseTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ExpenseRepository
    private var today = LocalDate.of(2025, 6, 15)
    private lateinit var close: CloseBillingCycleUseCase
    private lateinit var settle: SettleBillingCycleUseCase
    private lateinit var save: SaveTransactionUseCase

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ExpenseRepository(db.transactionDao(), db.categoryDao(), db.currencyDao(), db.paymentMethodDao(), db.amexDao(), db)
        close = CloseBillingCycleUseCase(repository) { today }
        settle = SettleBillingCycleUseCase(repository) { today }
        save = SaveTransactionUseCase(repository, AssignBillingCycleUseCase(repository) { today })
        repository.insertPaymentMethod(PaymentMethodEntity(id = "bank1", name = "Conto", provider = PaymentProvider.CASH))
        repository.insertPaymentMethod(PaymentMethodEntity(id = "saldo1", name = "Visa", provider = PaymentProvider.CREDIT_CARD_SALDO))
        repository.insertCreditCardDetail(
            CreditCardDetailEntity(paymentMethodId = "saldo1", cardType = "SALDO", limit = 3000.0, closingDay = 20, paymentDay = 15, linkedPaymentMethodId = "bank1"),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun closesOnlyCyclesWhoseClosingDayHasPassed() = runBlocking {
        cycle("2025-05", "OPEN")
        cycle("2025-06", "OPEN")
        cycle("2025-04", "PAID")
        today = LocalDate.of(2025, 6, 20)

        assertEquals(1, close())

        assertEquals(listOf("CLOSED", "OPEN", "PAID"), listOf("2025-05", "2025-06", "2025-04").map { status(it) })
    }

    @Test
    fun settlesADueCycleWithOneDebitOnTheLinkedAccount() = runBlocking {
        save(purchase("a", "2025-05-02", 100.0))
        save(purchase("b", "2025-05-19", 50.25))
        repository.updateBillingCycleStatus("saldo1:2025-05", "CLOSED")

        assertEquals(1, settle())

        val cycle = requireNotNull(repository.getBillingCycleById("saldo1:2025-05"))
        val settlement = requireNotNull(repository.getTransactionById(requireNotNull(cycle.settlementTransactionId)))
        assertEquals("PAID", cycle.status)
        assertEquals(150.25, settlement.amount, 0.001)
        assertEquals(listOf("2025-06-15", "2025-06-15"), listOf(settlement.date, settlement.effectiveDate))
        assertEquals("Addebito Visa", settlement.description)
        assertEquals(TransactionType.EXPENSE, settlement.type)
        assertFalse(settlement.isCreditCard)
        assertEquals("bank1", settlement.paymentMethodId)
        assertEquals("credit_card_payment", settlement.categoryId)
        assertNull(settlement.billingCycleId)
        assertTrue(settlement.isCycleSettlement)
        assertEquals(0.0, repository.getUnsettledCardExposure("saldo1"), 0.001)
    }

    @Test
    fun theExposureFlowDropsTheCycleOnceItIsSettled() = runBlocking {
        save(purchase("a", "2025-05-02", 100.0))
        save(purchase("b", "2025-05-19", 50.25))
        repository.updateBillingCycleStatus("saldo1:2025-05", "CLOSED")

        assertEquals(mapOf("saldo1" to 150.25), repository.unsettledExposureByCard(flowOf(LocalDate.of(2025, 6, 15))).first())

        settle()

        assertEquals(emptyMap<String, Double>(), repository.unsettledExposureByCard(flowOf(LocalDate.of(2025, 6, 15))).first())
    }

    @Test
    fun theExposureFlowPicksUpTodaysPurchaseWhenTheDateChanges() = runBlocking {
        today = LocalDate.of(2025, 6, 27)
        save(purchase("a", "2025-06-27", 100.0))
        save(purchase("b", "2025-06-28", 50.0))
        val dates = MutableStateFlow(LocalDate.of(2025, 6, 27))
        val firstValue = CompletableDeferred<Unit>()

        val values = async {
            repository.unsettledExposureByCard(dates)
                .map { it["saldo1"] }
                .onEach { firstValue.complete(Unit) }
                .take(2)
                .toList()
        }
        withTimeout(5_000) { firstValue.await() }
        // Mezzanotte con l'app ancora aperta
        dates.value = LocalDate.of(2025, 6, 28)

        assertEquals(listOf(100.0, 150.0), withTimeout(5_000) { values.await() })
    }

    @Test
    fun doesNotSettleBeforeTheDueDate() = runBlocking {
        save(purchase("a", "2025-05-02", 100.0))
        repository.updateBillingCycleStatus("saldo1:2025-05", "CLOSED")
        today = LocalDate.of(2025, 6, 14)

        assertEquals(0, settle())

        assertEquals("CLOSED", status("2025-05"))
        assertEquals(100.0, repository.getUnsettledCardExposure("saldo1"), 0.001)
    }

    @Test
    fun aDebitWrittenBeforeACrashIsRelinkedNotDuplicated() = runBlocking {
        save(purchase("a", "2025-05-02", 100.0))
        repository.updateBillingCycleStatus("saldo1:2025-05", "CLOSED")
        // Addebito gia' scritto, ciclo rimasto CLOSED: stato di un crash a meta' scrittura
        repository.insertTransaction(purchase("leftover", "2025-06-15", 100.0, card = "bank1", isCreditCard = false).copy(sourceRef = BillingCycleEntity.settlementRef("saldo1:2025-05")))

        settle()
        settle()

        assertEquals("leftover", repository.getBillingCycleById("saldo1:2025-05")?.settlementTransactionId)
        assertEquals(1, repository.getAllTransactionsList().count { it.isCycleSettlement })
    }

    @Test
    fun anEmptyCycleIsPaidWithoutADebit() = runBlocking {
        cycle("2025-05", "CLOSED")

        settle()

        assertEquals("PAID", status("2025-05"))
        assertNull(repository.getBillingCycleById("saldo1:2025-05")?.settlementTransactionId)
        assertEquals(0, repository.getAllTransactionsList().size)
    }

    @Test
    fun atStartupAnOverdueOpenCycleIsClosedThenSettled() = runBlocking {
        today = LocalDate.of(2025, 5, 10)
        save(purchase("a", "2025-05-10", 80.0))
        assertEquals("OPEN", status("2025-05"))

        // L'app viene riaperta a luglio: chiusura e addebito sono entrambi passati
        today = LocalDate.of(2025, 7, 1)
        close()
        settle()

        assertEquals("PAID", status("2025-05"))
        assertEquals(1, repository.getAllTransactionsList().count { it.isCycleSettlement })
    }

    @Test
    fun theCurrentAccountCountsTheDebitOnceAndTheReportCountsThePurchasesOnce() = runBlocking {
        save(purchase("a", "2025-05-02", 100.0))
        save(purchase("b", "2025-05-19", 50.0))
        repository.updateBillingCycleStatus("saldo1:2025-05", "CLOSED")
        settle()
        val all = repository.getAllTransactionsList()

        val account = CalculateCurrentAccountCashFlowUseCase().execute(all, emptyList(), emptyList(), YearMonth.of(2025, 6))
        val spending = all.filter { !it.isCycleSettlement && it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        assertEquals(150.0, account.outflows, 0.001)
        assertEquals(150.0, spending, 0.001)
    }

    private suspend fun cycle(month: String, status: String) {
        val cycleMonth = YearMonth.parse(month)
        repository.insertBillingCycle(
            BillingCycleEntity(
                id = BillingCycleEntity.idFor("saldo1", month),
                paymentMethodId = "saldo1",
                cycleMonth = month,
                closingDate = cycleMonth.atDay(20).toString(),
                dueDate = cycleMonth.plusMonths(1).atDay(15).toString(),
                status = status,
                paymentMode = "SALDO",
            ),
        )
    }

    private suspend fun status(month: String): String? = repository.getBillingCycleById(BillingCycleEntity.idFor("saldo1", month))?.status

    private fun purchase(id: String, date: String, amount: Double, card: String = "saldo1", isCreditCard: Boolean = true) = TransactionEntity(
        id = id,
        date = date,
        description = id,
        amount = amount,
        categoryId = "shopping",
        type = TransactionType.EXPENSE,
        isCreditCard = isCreditCard,
        effectiveDate = date,
        originalAmount = amount,
        originalCurrency = "EUR",
        paymentMethodId = card,
    )
}
