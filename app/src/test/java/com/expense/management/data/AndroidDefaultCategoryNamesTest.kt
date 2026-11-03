package com.expense.management.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expense.management.domain.model.CATEGORIES
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Ogni categoria predefinita ha un nome tradotto: una nuova categoria senza risorsa fa fallire il test. */
@RunWith(AndroidJUnit4::class)
class AndroidDefaultCategoryNamesTest {

    private fun names() = AndroidDefaultCategoryNames(ApplicationProvider.getApplicationContext<Context>())

    @Test
    @Config(qualifiers = "it")
    fun italianNamesMatchTheHistoricalLabels() {
        assertEquals(CATEGORIES.map { it.label }, CATEGORIES.map { names().nameOf(it.id) })
    }

    @Test
    @Config(qualifiers = "en")
    fun everyDefaultHasAnEnglishName() {
        assertEquals(
            listOf(
                "Food", "Transport", "Housing", "Entertainment", "Bills", "Health", "Shopping", "Other", "Card charge",
                "Salary", "Bank Transfer", "Gift", "Refund", "Investments", "Card credit",
            ),
            CATEGORIES.map { names().nameOf(it.id) },
        )
    }
}
