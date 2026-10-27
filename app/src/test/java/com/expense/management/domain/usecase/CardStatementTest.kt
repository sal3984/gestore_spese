package com.expense.management.domain.usecase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expense.management.data.AppDatabase
import com.expense.management.data.CreditCardDetailEntity
import com.expense.management.data.ExpenseRepository
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.data.isCycleSettlement
import com.expense.management.domain.model.ActiveCreditCard
import com.expense.management.domain.model.CardStatement
import com.expense.management.domain.model.CreditCardType
import com.expense.management.domain.model.PaymentProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Estratto carta e allineamento dell'addebito con repository vero su Room in memoria. */
@RunWith(AndroidJUnit4::class)
class CardStatementTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ExpenseRepository
    private var today = LocalDate.of(2026, 10, 28)
    private lateinit var save: SaveTransactionUseCase
    private val card = ActiveCreditCard(
        id = "visa1",
        name = "Visa Saldo",
        provider = PaymentProvider.CREDIT_CARD_SALDO,
        cardType = CreditCardType.SALDO,
        limit = 2000.0,
        closingDay = 20,
        paymentDay = 15,
        linkedPaymentMethodId = "bank1",
    )

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ExpenseRepository(db.transactionDao(), db.categoryDao(), db.currencyDao(), db.paymentMethodDao(), db.amexDao(), db)
        save = SaveTransactionUseCase(repository, AssignBillingCycleUseCase(repository) { today })
        repository.insertPaymentMethod(PaymentMethodEntity(id = "bank1", name = "Conto Fineco", provider = PaymentProvider.CASH))
        repository.insertPaymentMethod(PaymentMethodEntity(id = "visa1", name = "Visa Saldo", provider = PaymentProvider.CREDIT_CARD_SALDO))
        repository.insertCreditCardDetail(
            CreditCardDetailEntity(paymentMethodId = "visa1", cardType = "SALDO", limit = 2000.0, closingDay = 20, paymentDay = 15, linkedPaymentMethodId = "bank1"),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun statementSplitsCyclesIntoUpcomingOpenAndHistory() = runBlocking {
        save(purchase("agosto", "2026-08-10", 40.0))
        save(purchase("ottobre", "2026-10-05", 100.0))
        save(purchase("benzina", "2026-10-20", 50.0))
        save(purchase("scarpe", "2026-10-25", 80.0))
        save(purchase("pranzo", "2026-10-28", 45.0))
        save(purchase("abbonamento", "2026-11-19", 30.0))

        val statement = statement()

        assertEquals(listOf("visa1:2026-10"), statement.upcomingCharges.map { it.cycleId })
        assertEquals(listOf("visa1:2026-11"), statement.openCycles.map { it.cycleId })
        assertEquals(listOf("visa1:2026-08"), statement.history.map { it.cycleId })
        assertEquals(150.0, statement.upcomingCharges.single().total, 0.001)
        assertEquals(LocalDate.of(2026, 11, 15), statement.upcomingCharges.single().dueDate)
        val open = statement.openCycles.single()
        assertEquals(125.0, open.total, 0.001)
        assertEquals(listOf("pranzo", "scarpe"), open.entries.map { it.transactionId })
        assertEquals(listOf("abbonamento"), open.scheduledEntries.map { it.transactionId })
        // La stessa cifra della barra in dashboard
        assertEquals(275.0, statement.exposure.exposure, 0.001)
        assertEquals(1725.0, statement.exposure.available, 0.001)
        assertEquals("Conto Fineco", statement.linkedAccountName)
        assertTrue(statement.history.single().settledBeforeUpdate)
        assertNull(statement.history.single().settledAmount)
    }

    @Test
    fun aPurchaseAddedAfterTheChargeIsFlaggedAndCanBeAligned() = runBlocking {
        save(purchase("ottobre", "2026-10-05", 100.0))
        save(purchase("benzina", "2026-10-20", 50.0))
        settleOn(LocalDate.of(2026, 11, 15))
        save(purchase("dimenticata", "2026-10-10", 30.0))

        val flagged = statement().history.single()
        assertEquals(150.0, flagged.settledAmount!!, 0.001)
        assertEquals(180.0, flagged.total, 0.001)
        assertTrue(flagged.settlementMismatch)
        assertFalse(flagged.settledBeforeUpdate)

        assertEquals(AlignSettlementUseCase.Result.Aligned(150.0, 180.0), AlignSettlementUseCase(repository)("visa1:2026-10"))

        val aligned = statement().history.single()
        assertEquals(180.0, aligned.settledAmount!!, 0.001)
        assertFalse(aligned.settlementMismatch)
        val settlements = repository.getAllTransactionsList().filter { it.isCycleSettlement }
        assertEquals(listOf(180.0), settlements.map { it.amount })
        assertEquals(listOf(180.0), settlements.map { it.originalAmount })
    }

    @Test
    fun aChargedCycleLeftEmptyLosesItsCharge() = runBlocking {
        save(purchase("ottobre", "2026-10-05", 100.0))
        settleOn(LocalDate.of(2026, 11, 15))
        repository.deleteTransaction("ottobre")

        assertEquals(AlignSettlementUseCase.Result.Removed, AlignSettlementUseCase(repository)("visa1:2026-10"))

        assertNull(repository.getBillingCycleById("visa1:2026-10")?.settlementTransactionId)
        assertEquals(0, repository.getAllTransactionsList().count { it.isCycleSettlement })
        val cycle = statement().history.single()
        assertEquals(0.0, cycle.total, 0.001)
        assertFalse(cycle.settledBeforeUpdate)
        assertFalse(cycle.settlementMismatch)
    }

    @Test
    fun alignmentLeavesUnchangedOpenAndPreUpdateCyclesAlone() = runBlocking {
        save(purchase("agosto", "2026-08-10", 40.0))
        save(purchase("ottobre", "2026-10-05", 100.0))
        save(purchase("scarpe", "2026-10-25", 80.0))
        settleOn(LocalDate.of(2026, 11, 15))
        val align = AlignSettlementUseCase(repository)

        assertEquals(AlignSettlementUseCase.Result.NothingToAlign, align("visa1:2026-10"))
        assertEquals(AlignSettlementUseCase.Result.NothingToAlign, align("visa1:2026-11"))
        assertEquals(AlignSettlementUseCase.Result.NothingToAlign, align("visa1:2026-08"))
        assertEquals(AlignSettlementUseCase.Result.NothingToAlign, align("missing"))
        assertEquals(1, repository.getAllTransactionsList().count { it.isCycleSettlement })
    }

    @Test
    fun creditsLinkedToACycleReduceItsTotal() {
        val cycle = com.expense.management.data.BillingCycleEntity(
            id = "visa1:2026-10",
            paymentMethodId = "visa1",
            cycleMonth = "2026-10",
            closingDate = "2026-10-20",
            dueDate = "2026-11-15",
            status = "CLOSED",
            paymentMode = "SALDO",
        )
        val transactions = listOf(
            purchase("a", "2026-10-05", 100.0).copy(billingCycleId = cycle.id),
            purchase("rimborso", "2026-10-06", 30.0).copy(billingCycleId = cycle.id, type = TransactionType.INCOME),
            purchase("illeggibile", "05/10/2026", 999.0).copy(billingCycleId = cycle.id),
        )

        val statement = GetCardStatementUseCase().execute(card, listOf(cycle), transactions, emptyList(), 70.0, null, today)

        assertEquals(70.0, statement.upcomingCharges.single().total, 0.001)
        assertEquals(listOf("rimborso", "a"), statement.upcomingCharges.single().entries.map { it.transactionId })
        assertTrue(statement.upcomingCharges.single().entries.first().isCredit)
    }

    @Test
    fun aOneCentDifferenceIsAMismatchButRoundingNoiseIsNot() {
        fun cycle(settled: Double?, total: Double) = com.expense.management.domain.model.StatementCycle(
            cycleId = "c",
            cycleMonth = java.time.YearMonth.of(2026, 10),
            closingDate = LocalDate.of(2026, 10, 20),
            dueDate = LocalDate.of(2026, 11, 15),
            status = com.expense.management.domain.model.CycleStatus.PAID,
            total = total,
            entries = emptyList(),
            scheduledEntries = emptyList(),
            settledAmount = settled,
            settledBeforeUpdate = false,
        )

        assertTrue(cycle(settled = 150.0, total = 150.01).settlementMismatch)
        assertFalse(cycle(settled = 150.0, total = 150.0 + 1e-9).settlementMismatch)
        assertFalse(cycle(settled = null, total = 150.0).settlementMismatch)
    }

    private suspend fun settleOn(date: LocalDate) {
        today = date
        CloseBillingCycleUseCase(repository) { today }()
        SettleBillingCycleUseCase(repository) { today }()
    }

    private suspend fun statement(): CardStatement = GetCardStatementUseCase().execute(
        card = card,
        cycles = repository.billingCyclesForCard("visa1").first(),
        cycleTransactions = repository.cycleTransactionsForCard("visa1").first(),
        settlements = repository.settlementsForCard("visa1").first(),
        unsettledExposure = repository.unsettledExposureByCard(flowOf(today)).first()["visa1"] ?: 0.0,
        linkedAccountName = repository.getPaymentMethodById("bank1")?.name,
        today = today,
    )

    private fun purchase(id: String, date: String, amount: Double) = TransactionEntity(
        id = id,
        date = date,
        description = id,
        amount = amount,
        categoryId = "shopping",
        type = TransactionType.EXPENSE,
        isCreditCard = true,
        effectiveDate = date,
        originalAmount = amount,
        originalCurrency = "EUR",
        paymentMethodId = "visa1",
    )
}
