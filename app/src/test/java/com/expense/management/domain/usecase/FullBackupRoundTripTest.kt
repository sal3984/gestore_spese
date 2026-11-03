package com.expense.management.domain.usecase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expense.management.data.AmexPagoFlexPlanChangeEntity
import com.expense.management.data.AmexPagoFlexPlanEntity
import com.expense.management.data.AmexPagoFlexScheduledPaymentEntity
import com.expense.management.data.AmexRevolvingStateEntity
import com.expense.management.data.AmexStatementEntity
import com.expense.management.data.AppDatabase
import com.expense.management.data.BackupData
import com.expense.management.data.BillingCycleEntity
import com.expense.management.data.CategoryEntity
import com.expense.management.data.CreditCardDetailEntity
import com.expense.management.data.CreditCardInstallmentPlanEntity
import com.expense.management.data.CurrencyRate
import com.expense.management.data.ExpenseRepository
import com.expense.management.data.InstallmentScheduledPaymentEntity
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.PaymentProvider
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Backup completo: ogni tabella esce nel JSON e torna identica dopo il ripristino in un database
 * vuoto, passando da Gson come fa l'app.
 */
@RunWith(AndroidJUnit4::class)
class FullBackupRoundTripTest {

    private lateinit var source: AppDatabase
    private lateinit var target: AppDatabase

    @Before
    fun setUp() {
        source = database()
        target = database()
    }

    @After
    fun tearDown() {
        source.close()
        target.close()
    }

    @Test
    fun everyTableSurvivesBackupAndRestore() = runBlocking {
        val from = repository(source)
        populate(from)
        val json = Gson().toJson(GetBackupDataUseCase(from)())

        val to = repository(target)
        restore(to, Gson().fromJson(json, BackupData::class.java))

        assertEquals(BackupData.CURRENT_VERSION, Gson().fromJson(json, BackupData::class.java).version)
        assertEquals(snapshot(from), snapshot(to))
    }

    @Test
    fun restoringReplacesWhatWasThere() = runBlocking {
        val from = repository(source)
        populate(from)
        val backup = GetBackupDataUseCase(from)()
        val to = repository(target)
        to.insertPaymentMethod(PaymentMethodEntity(id = "old", name = "Vecchia", provider = PaymentProvider.CASH))

        restore(to, backup)

        assertEquals(snapshot(from), snapshot(to))
    }

    @Test
    fun rowsWhoseParentIsMissingAreSkippedInsteadOfFailing() = runBlocking {
        val from = repository(source)
        populate(from)
        val backup = GetBackupDataUseCase(from)()
        val to = repository(target)

        // Backup scritto a mano o troncato: l'estratto Amex manca, i suoi figli non hanno genitore
        restore(to, backup.copy(amexStatements = emptyList()))

        assertEquals(emptyList<AmexPagoFlexPlanEntity>(), to.getAllAmexPagoFlexPlans())
        assertEquals(emptyList<AmexRevolvingStateEntity>(), to.getAllAmexRevolvingStates())
        assertEquals(snapshot(from)["transactions"], snapshot(to)["transactions"])
    }

    private suspend fun restore(repository: ExpenseRepository, backup: BackupData) {
        RestoreDataUseCase(repository, AssignBillingCycleUseCase(repository, today = { TODAY }))(backup)
    }

    private fun database() = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    private fun repository(db: AppDatabase) =
        ExpenseRepository(db.transactionDao(), db.categoryDao(), db.currencyDao(), db.paymentMethodDao(), db.amexDao(), db)

    private suspend fun snapshot(repository: ExpenseRepository): Map<String, List<Any>> = mapOf(
        "transactions" to repository.getAllTransactionsList().sortedBy { it.id },
        "categories" to repository.getAllCategories().sortedBy { it.id },
        "paymentMethods" to repository.getAllPaymentMethods().sortedBy { it.id },
        "creditCardDetails" to repository.getAllCreditCardDetails().sortedBy { it.paymentMethodId },
        "billingCycles" to repository.getAllBillingCycles().sortedBy { it.id },
        "installmentPlans" to repository.getAllInstallmentPlans().sortedBy { it.id },
        "installmentScheduledPayments" to repository.getAllInstallmentScheduledPayments().sortedBy { it.id },
        "amexStatements" to repository.getAllAmexStatements().sortedBy { it.id },
        "amexPagoFlexPlans" to repository.getAllAmexPagoFlexPlans().sortedBy { it.id },
        "amexScheduledPayments" to repository.getAllAmexScheduledPaymentsList().sortedBy { it.id },
        "amexPlanChanges" to repository.getAllAmexPlanChanges().sortedBy { it.id },
        "amexRevolvingStates" to repository.getAllAmexRevolvingStates().sortedBy { it.id },
        "currencyRates" to repository.getAllCurrencyRates().sortedBy { it.currencyCode },
    )

    /** Una riga per tabella, con i collegamenti tra tabelle e un ciclo gia' addebitato. */
    private suspend fun populate(repository: ExpenseRepository) {
        repository.insertAllCategories(listOf(CategoryEntity("food", "Cibo", "x", TransactionType.EXPENSE, isCustom = false)))
        repository.insertAllPaymentMethods(
            listOf(
                PaymentMethodEntity(id = "bank1", name = "Conto", provider = PaymentProvider.CASH),
                PaymentMethodEntity(id = "visa1", name = "Visa", provider = PaymentProvider.CREDIT_CARD_SALDO),
                PaymentMethodEntity(id = "amex1", name = "Amex", provider = PaymentProvider.CREDIT_CARD_AMEX),
                PaymentMethodEntity(id = "rate1", name = "Rateale", provider = PaymentProvider.CREDIT_CARD_INSTALLMENT),
            ),
        )
        repository.insertCreditCardDetail(CreditCardDetailEntity(paymentMethodId = "visa1", cardType = "SALDO", limit = 2000.0, closingDay = 20, paymentDay = 15, linkedPaymentMethodId = "bank1"))
        repository.insertCreditCardDetail(CreditCardDetailEntity(paymentMethodId = "amex1", cardType = "AMEX_HYBRID", limit = 5000.0, closingDay = 25, paymentDay = 10))
        repository.insertCreditCardDetail(CreditCardDetailEntity(paymentMethodId = "rate1", cardType = "INSTALLMENT", limit = 3000.0, closingDay = 0, paymentDay = 0))

        // Ciclo di maggio gia' addebitato: stato, addebito e importo scelto devono tornare uguali
        repository.insertBillingCycle(
            BillingCycleEntity(
                id = "visa1:2025-05", paymentMethodId = "visa1", cycleMonth = "2025-05", closingDate = "2025-05-20",
                dueDate = "2025-06-15", status = "PAID", paymentMode = "FULL", chosenAmount = 90.0,
                settlementTransactionId = "settle", carriedForward = 10.0,
            ),
        )
        repository.insertAllTransactions(
            listOf(
                tx("buy", "2025-05-10", 100.0, card = "visa1").copy(billingCycleId = "visa1:2025-05", effectiveDate = "2025-06-15"),
                tx("settle", "2025-06-15", 100.0, card = "bank1", isCreditCard = false).copy(categoryId = "credit_card_payment", sourceRef = "cycle:visa1:2025-05:settlement"),
                tx("amexBuy", "2025-05-12", 600.0, card = "amex1"),
            ),
        )

        repository.insertAmexStatement(
            AmexStatementEntity(
                id = "st1", paymentMethodId = "amex1", statementMonth = "2025-05", totalExpenses = 600.0, totalPagoflex = 600.0,
                paymentMode = "PAGOFLEX", paymentAmount = 0.0, closingDate = "2025-05-25", paymentDueDate = "2025-06-10", isClosed = true,
            ),
        )
        repository.insertAmexPagoFlexPlan(
            AmexPagoFlexPlanEntity(
                id = "pf1", statementId = "st1", transactionId = "amexBuy", totalAmount = 600.0, installmentCount = 6,
                installmentAmount = 100.0, paidCount = 1, startDate = "2025-06-10", planType = "FIXED_AMOUNT", initialInstallmentAmount = 100.0,
            ),
        )
        repository.insertAmexScheduledPayments(
            listOf(AmexPagoFlexScheduledPaymentEntity(id = "pf1-1", planId = "pf1", sequenceNumber = 1, dueDate = "2025-06-10", amount = 100.0, status = "PAID", expenseTransactionId = "settle")),
        )
        repository.insertAmexPlanChange(
            AmexPagoFlexPlanChangeEntity(
                id = "ch1", planId = "pf1", changedAt = "2025-06-01", previousInstallmentCount = 5, newInstallmentCount = 6,
                previousInstallmentAmount = 120.0, newInstallmentAmount = 100.0, remainingDebtAtChange = 600.0, reason = "test",
            ),
        )
        repository.insertAmexRevolvingState(
            AmexRevolvingStateEntity(id = "rv1", statementId = "st1", carriedForwardDebt = 50.0, interestCharged = 2.5, interestRate = 0.15, userPaymentChoice = "MINIMUM", paymentAmount = 20.0),
        )

        repository.insertInstallmentPlan(
            CreditCardInstallmentPlanEntity(id = "ip1", paymentMethodId = "rate1", totalAmount = 300.0, installmentCount = 3, installmentAmount = 100.0, paidCount = 1, startDate = "2025-06-01"),
        )
        repository.insertScheduledPayments(
            listOf(InstallmentScheduledPaymentEntity(id = "ip1-1", planId = "ip1", dueDate = "2025-06-01", amount = 100.0, status = "PAID")),
        )
        repository.insertCurrencyRates(listOf(CurrencyRate("USD", 1.08, 1_700_000_000_000L)))
    }

    private fun tx(id: String, date: String, amount: Double, card: String, isCreditCard: Boolean = true) = TransactionEntity(
        id = id,
        date = date,
        description = id,
        amount = amount,
        categoryId = "food",
        type = TransactionType.EXPENSE,
        isCreditCard = isCreditCard,
        effectiveDate = date,
        originalAmount = amount,
        originalCurrency = "EUR",
        paymentMethodId = card,
    )

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2025, 6, 25)
    }
}
