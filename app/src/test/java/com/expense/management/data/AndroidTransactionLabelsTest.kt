package com.expense.management.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Le descrizioni generate escono nella lingua dell'app, con gli argomenti al posto giusto. */
@RunWith(AndroidJUnit4::class)
class AndroidTransactionLabelsTest {

    private fun labels() = AndroidTransactionLabels(ApplicationProvider.getApplicationContext<Context>())

    private fun all() = labels().let {
        listOf(
            it.cardCharge("Visa"),
            it.cardInstallment("Visa"),
            it.limitRestore("Visa"),
            it.amexInstallment(2, 6),
            it.amexInstallment(1, null),
            it.amexStatementPayment("2026-10"),
            it.amexStatementRefund("2026-10"),
            it.unnamedCard,
            it.futurePayment,
        )
    }

    @Test
    @Config(qualifiers = "it")
    fun italian() {
        assertEquals(
            listOf(
                "Addebito Visa",
                "Rata Visa",
                "[Visa] Ripristino plafond",
                "Rata Amex 2/6",
                "Rata Amex 1/-",
                "Pagamento Amex 2026-10",
                "Rimborso Amex 2026-10",
                "Carta di credito",
                "(Pagamento futuro)",
            ),
            all(),
        )
    }

    @Test
    @Config(qualifiers = "en")
    fun english() {
        assertEquals(
            listOf(
                "Charge Visa",
                "Installment Visa",
                "[Visa] Limit restored",
                "Amex installment 2/6",
                "Amex installment 1/-",
                "Amex payment 2026-10",
                "Amex refund 2026-10",
                "Credit card",
                "(Future payment)",
            ),
            all(),
        )
    }

    @Test
    @Config(qualifiers = "de")
    fun anyOtherLanguageFallsBackToEnglish() {
        assertEquals("Charge Visa", labels().cardCharge("Visa"))
    }
}
