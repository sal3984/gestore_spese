package com.expense.management.domain.usecase

import com.expense.management.data.AmexStatementEntity
import com.expense.management.data.CategoryEntity
import com.expense.management.data.RecurrenceType
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.ActiveCreditCard
import com.expense.management.domain.model.AmexInstallmentStrategy
import com.expense.management.domain.model.CreditCardType
import com.expense.management.domain.model.PaymentProvider
import com.expense.management.ui.screens.AddTransactionUiState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

class AmexPagoFlexCashFlowTest {

    private val amexCard = ActiveCreditCard(
        id = "amex1",
        name = "Amex",
        provider = PaymentProvider.CREDIT_CARD_AMEX,
        cardType = CreditCardType.AMEX_HYBRID,
        limit = 5000.0,
        closingDay = 30,
        paymentDay = 15,
    )

    private val categories = listOf(
        CategoryEntity(id = "shopping", label = "Shopping", type = TransactionType.EXPENSE, icon = "shopping"),
        CategoryEntity(id = "credit_card_payment", label = "Pagamento carta", type = TransactionType.EXPENSE, icon = "credit_card"),
        CategoryEntity(id = "credit_card_adjustment", label = "Rimborso carta", type = TransactionType.INCOME, icon = "credit_card"),
    )

    @Test
    fun `amex pagoflex purchase creates only the credit card expense and future real installments`() {
        val saveUseCase = AddTransactionSaveUseCase()
        val uiState = AddTransactionUiState(
            amountText = "2000",
            description = "TV",
            selectedCategory = "shopping",
            type = TransactionType.EXPENSE,
            isCreditCard = true,
            creditCardId = amexCard.id,
            isPagoFlex = true,
            pagoFlexInstallments = 6,
            recurrenceType = RecurrenceType.NONE,
            dateStr = "15/06/2026",
            installmentStartDateStr = "15/07/2026",
            ignoreDateWarning = true,
        )

        val result = saveUseCase(
            uiState = uiState,
            transactionToEdit = null,
            availableCategories = categories,
            activeCreditCards = listOf(amexCard),
            dateFormat = "dd/MM/yyyy",
            locale = Locale.ITALY,
        )

        require(result is AddTransactionSaveResult.Ready)
        val transactions = result.transactions

        println("Transactions: ${transactions.map { "${it.id} ${it.type} ${it.amount} isCC=${it.isCreditCard} desc=${it.description}" }}")

        // La spesa iniziale deve essere una sola transazione sulla carta, non deve uscire dal conto corrente
        assertEquals(1, transactions.size)
        val mainTx = transactions.first()
        assertEquals(TransactionType.EXPENSE, mainTx.type)
        assertTrue(mainTx.isCreditCard)
        assertEquals(2000.0, mainTx.amount)
        assertEquals("TV", mainTx.description)
        assertNull(mainTx.installmentNumber)

        // Nessuna transazione mirror finta deve essere creata
        assertTrue(transactions.none { it.id.endsWith("_mirror") })

        // Il piano PagoFlex deve generare 6 rate reali
        val planResult = CreateAmexInstallmentPlanUseCase().execute(
            planId = "plan1",
            statementId = "stmt1",
            transactionId = mainTx.id,
            totalAmount = 2000.0,
            startDate = "2024-07-15",
            strategy = AmexInstallmentStrategy.FixedDuration(6),
        )
        assertEquals(6, planResult.second.size)

        // Le rate devono essere uscite dal conto corrente
        val installments = planResult.second
        assertTrue(installments.all { it.amount > 0 })
        assertEquals(2000.0, installments.sumOf { it.amount }, 0.01)

        // Quando pago lo statement con PagoFlex non deve uscire altro dal conto
        // e non deve comparire un'entrata finta nel flusso del conto corrente
        val statement = AmexStatementEntity(
            id = "stmt1",
            paymentMethodId = amexCard.id,
            statementMonth = "2024-06",
            totalExpenses = 2000.0,
            totalPagoflex = 2000.0,
            revolvingBalance = 0.0,
            paymentMode = "SALDO",
            paymentAmount = 2000.0,
            closingDate = "2024-06-30",
            paymentDueDate = "2024-07-15",
        )
        val payResult = PayAmexStatementUseCase().execute(
            statement = statement,
            amount = 2000.0,
            paymentDate = "2024-07-15",
            plans = listOf(planResult.first),
            scheduledPayments = installments,
        )

        assertNull(payResult.incomeTransaction)
        assertEquals(1, payResult.paidInstallments.size)
        assertEquals(1, payResult.paymentTransactions.size)
        val paidTx = payResult.paymentTransactions.first()
        assertEquals(TransactionType.EXPENSE, paidTx.type)
        assertTrue(!paidTx.isCreditCard)
    }
}
