package com.expense.management.domain.usecase

import com.expense.management.data.CategoryEntity
import com.expense.management.data.RecurrenceType
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.ActiveCreditCard
import com.expense.management.domain.model.CreditCardType
import com.expense.management.domain.model.PaymentProvider
import com.expense.management.ui.screens.AddTransactionUiState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@DisplayName("Entrata su carta al salvataggio")
class CardRefundSaveTest {

    private val categories = listOf(
        CategoryEntity(id = "shopping", label = "Shopping", type = TransactionType.INCOME, icon = "shopping"),
        CategoryEntity(id = "credit_card_payment", label = "Pagamento carta", type = TransactionType.EXPENSE, icon = "credit_card"),
    )

    private fun card(closingDay: Int, paymentDay: Int, type: CreditCardType = CreditCardType.SALDO) = ActiveCreditCard(
        id = "visa1",
        name = "Visa",
        provider = PaymentProvider.CREDIT_CARD_SALDO,
        cardType = type,
        limit = 2000.0,
        closingDay = closingDay,
        paymentDay = paymentDay,
    )

    private fun saveRefund(card: ActiveCreditCard): List<String> {
        val result = AddTransactionSaveUseCase()(
            uiState = AddTransactionUiState(
                amountText = "30",
                description = "Reso",
                selectedCategory = "shopping",
                type = TransactionType.INCOME,
                isCreditCard = true,
                creditCardId = card.id,
                recurrenceType = RecurrenceType.NONE,
                dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                ignoreDateWarning = true,
            ),
            transactionToEdit = null,
            availableCategories = categories,
            activeCreditCards = listOf(card),
            dateFormat = "dd/MM/yyyy",
            locale = Locale.ITALY,
        )
        require(result is AddTransactionSaveResult.Ready)
        return result.transactions.map { if (it.id.endsWith("_mirror")) "mirror" else "refund" }
    }

    @Test
    fun `a refund on a saldo card with a cycle creates no account mirror`() {
        assertEquals(listOf("refund"), saveRefund(card(closingDay = 20, paymentDay = 15)))
    }

    @Test
    fun `cards without a cycle keep the account mirror`() {
        assertEquals(listOf("mirror", "refund"), saveRefund(card(closingDay = 0, paymentDay = 0)))
        assertEquals(listOf("mirror", "refund"), saveRefund(card(closingDay = 20, paymentDay = 15, type = CreditCardType.REVOLVING)))
    }

    @Test
    fun `the account mirror is described with the labels it receives`() {
        val result = AddTransactionSaveUseCase(FakeTransactionLabels)(
            uiState = AddTransactionUiState(
                amountText = "30",
                description = "Reso",
                selectedCategory = "shopping",
                type = TransactionType.INCOME,
                isCreditCard = true,
                creditCardId = "visa1",
                recurrenceType = RecurrenceType.NONE,
                dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                ignoreDateWarning = true,
            ),
            transactionToEdit = null,
            availableCategories = categories,
            activeCreditCards = listOf(card(closingDay = 0, paymentDay = 0)),
            dateFormat = "dd/MM/yyyy",
            locale = Locale.ITALY,
        )
        require(result is AddTransactionSaveResult.Ready)

        assertEquals("[Visa] Reso (future)", result.transactions.single { it.id.endsWith("_mirror") }.description)
    }
}
