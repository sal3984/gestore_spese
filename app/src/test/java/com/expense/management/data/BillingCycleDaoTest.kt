package com.expense.management.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expense.management.domain.model.BillingCycle
import com.expense.management.domain.model.CycleStatus
import com.expense.management.domain.model.PaymentProvider
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
import java.time.YearMonth

/** BillingCycleDao su un Room in memoria con foreign key attive, come nell'app. */
@RunWith(AndroidJUnit4::class)
class BillingCycleDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: BillingCycleDao

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.billingCycleDao()
        listOf("card1", "card2").forEach {
            db.paymentMethodDao().insertPaymentMethod(PaymentMethodEntity(id = it, name = it, provider = PaymentProvider.CREDIT_CARD_SALDO))
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private companion object {
        const val TODAY = "2025-05-20"
    }

    @Test
    fun cycleTotalsAreTheSumOfLinkedCardTransactions() = runBlocking {
        cycle("card1", "2025-05")
        cycle("card1", "2025-06")
        tx("a", 100.0, cycle = "card1:2025-05")
        tx("b", 50.25, cycle = "card1:2025-05")
        tx("refund", 20.0, cycle = "card1:2025-05", type = TransactionType.INCOME)
        tx("bankSide", 999.0, cycle = "card1:2025-05", isCreditCard = false)
        tx("otherCycle", 70.0, cycle = "card1:2025-06")
        tx("unlinked", 80.0, cycle = null)

        assertEquals(CycleTotals(purchases = 150.25, credits = 20.0, transactionCount = 3), dao.getCycleTotals("card1:2025-05"))
    }

    @Test
    fun cycleTotalsOfAnEmptyCycleAreZero() = runBlocking {
        cycle("card1", "2025-05")

        assertEquals(CycleTotals(0.0, 0.0, 0), dao.getCycleTotals("card1:2025-05"))
    }

    @Test
    fun cycleTotalsFollowEditsAndDeletesWithoutCounters() = runBlocking {
        cycle("card1", "2025-05")
        tx("a", 100.0, cycle = "card1:2025-05")
        tx("b", 50.0, cycle = "card1:2025-05")

        val edited = requireNotNull(db.transactionDao().getById("a")).copy(amount = 40.0)
        db.transactionDao().insert(edited)
        db.transactionDao().delete("b")

        assertEquals(CycleTotals(40.0, 0.0, 1), dao.getCycleTotals("card1:2025-05"))
    }

    @Test
    fun unsettledCyclesExcludePaidOnesAndOtherCardsAndIncludeEmptyCycles() = runBlocking {
        cycle("card1", "2025-04", CycleStatus.PAID)
        cycle("card1", "2025-06", CycleStatus.OPEN)
        cycle("card1", "2025-05", CycleStatus.CLOSED)
        cycle("card2", "2025-05", CycleStatus.OPEN)
        tx("paid", 500.0, cycle = "card1:2025-04")
        tx("closed", 200.0, cycle = "card1:2025-05")
        tx("refund", 20.0, cycle = "card1:2025-05", type = TransactionType.INCOME)
        tx("otherCard", 300.0, cycle = "card2:2025-05")

        val unsettled = dao.getUnsettledCyclesWithTotals("card1")

        assertEquals(listOf("card1:2025-05", "card1:2025-06"), unsettled.map { it.cycle.id })
        assertEquals(listOf(200.0 to 20.0, 0.0 to 0.0), unsettled.map { it.purchases to it.credits })
    }

    @Test
    fun exposureIsNetUnsettledSpendMinusWhatWasAlreadyPaidOnPartialCycles() = runBlocking {
        cycle("card1", "2025-03", CycleStatus.PAID)
        cycle("card1", "2025-04", CycleStatus.PARTIALLY_PAID, settlementTransactionId = "settlement")
        cycle("card1", "2025-05", CycleStatus.CLOSED)
        cycle("card1", "2025-06", CycleStatus.OPEN)
        cycle("card2", "2025-06", CycleStatus.OPEN)
        tx("paid", 500.0, cycle = "card1:2025-03")
        tx("partial", 300.0, cycle = "card1:2025-04")
        tx("settlement", 120.0, cycle = null, isCreditCard = false)
        tx("closed", 200.0, cycle = "card1:2025-05")
        tx("refund", 20.0, cycle = "card1:2025-05", type = TransactionType.INCOME)
        tx("open", 100.0, cycle = "card1:2025-06")
        tx("otherCard", 999.0, cycle = "card2:2025-06")

        // 300 - 120 + 200 - 20 + 100
        assertEquals(listOf(CardExposureRow("card1", 460.0), CardExposureRow("card2", 999.0)), dao.getUnsettledExposureByCard(TODAY).sortedBy { it.paymentMethodId })
        assertEquals(null, dao.getUnsettledExposureByCard(TODAY).find { it.paymentMethodId == "cardWithoutCycles" })
    }

    @Test
    fun exposureIgnoresPurchasesDatedAfterToday() = runBlocking {
        cycle("card1", "2025-05", CycleStatus.OPEN)
        tx("today", 100.0, cycle = "card1:2025-05", date = TODAY)
        // Ricorrenza gia' generata per un mese futuro
        tx("future", 50.0, cycle = "card1:2025-05", date = "2025-05-21")

        assertEquals(listOf(CardExposureRow("card1", 100.0)), dao.getUnsettledExposureByCard(TODAY))
    }

    @Test
    fun insertingAnExistingCycleKeepsTheRowAndItsTransactions() = runBlocking {
        cycle("card1", "2025-05", CycleStatus.CLOSED)
        tx("a", 100.0, cycle = "card1:2025-05")

        val inserted = dao.insertCycle(entity("card1", "2025-05", CycleStatus.OPEN)) != -1L

        assertFalse(inserted)
        assertEquals("CLOSED", dao.getCycleById("card1:2025-05")?.status)
        assertEquals("card1:2025-05", db.transactionDao().getById("a")?.billingCycleId)
    }

    @Test
    fun cycleEntityMapsToTheDomainAndBack() {
        val cycle = BillingCycle(
            id = "card1:2025-05",
            paymentMethodId = "card1",
            cycleMonth = YearMonth.of(2025, 5),
            closingDate = LocalDate.of(2025, 5, 20),
            dueDate = LocalDate.of(2025, 6, 15),
            status = CycleStatus.PARTIALLY_PAID,
            chosenAmount = 100.0,
            settlementTransactionId = "s1",
        )

        assertEquals(cycle, cycle.toEntity().toDomain())
    }

    @Test
    fun invalidCycleRowsMapToNull() {
        val valid = entity("card1", "2025-05", CycleStatus.OPEN)

        assertTrue(valid.toDomain() != null)
        assertNull(valid.copy(status = "SETTLED").toDomain())
        assertNull(valid.copy(closingDate = "20/05/2025").toDomain())
        assertNull(valid.copy(dueDate = "2025-05-01").toDomain())
    }

    private suspend fun cycle(card: String, month: String, status: CycleStatus = CycleStatus.OPEN, settlementTransactionId: String? = null) {
        assertTrue(dao.insertCycle(entity(card, month, status, settlementTransactionId)) != -1L)
    }

    private fun entity(card: String, month: String, status: CycleStatus, settlementTransactionId: String? = null): BillingCycleEntity {
        val cycleMonth = YearMonth.parse(month)
        return BillingCycleEntity(
            id = BillingCycleEntity.idFor(card, month),
            paymentMethodId = card,
            cycleMonth = month,
            closingDate = cycleMonth.atDay(20).toString(),
            dueDate = cycleMonth.plusMonths(1).atDay(15).toString(),
            status = status.name,
            paymentMode = "SALDO",
            settlementTransactionId = settlementTransactionId,
        )
    }

    private suspend fun tx(
        id: String,
        amount: Double,
        cycle: String?,
        type: TransactionType = TransactionType.EXPENSE,
        isCreditCard: Boolean = true,
        date: String = "2025-05-10",
    ) {
        db.transactionDao().insert(
            TransactionEntity(
                id = id,
                date = date,
                description = id,
                amount = amount,
                categoryId = "shopping",
                type = type,
                isCreditCard = isCreditCard,
                effectiveDate = "2025-06-15",
                originalAmount = amount,
                originalCurrency = "EUR",
                paymentMethodId = cycle?.substringBefore(':'),
                billingCycleId = cycle,
            ),
        )
    }
}
