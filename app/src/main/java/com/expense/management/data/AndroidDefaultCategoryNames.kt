package com.expense.management.data

import android.content.Context
import com.expense.management.R
import com.expense.management.domain.model.DefaultCategoryNames

/** Nomi delle categorie predefinite letti dalle risorse, nella lingua corrente dell'app. */
class AndroidDefaultCategoryNames(context: Context) : DefaultCategoryNames {
    private val context = context.applicationContext

    override fun nameOf(categoryId: String): String? = NAMES[categoryId]?.let(context::getString)

    companion object {
        val NAMES: Map<String, Int> = mapOf(
            "food" to R.string.cat_food,
            "transport" to R.string.cat_transport,
            "housing" to R.string.cat_housing,
            "entertainment" to R.string.cat_entertainment,
            "bills" to R.string.cat_bills,
            "health" to R.string.cat_health,
            "shopping" to R.string.cat_shopping,
            "other" to R.string.cat_other,
            "credit_card_payment" to R.string.cat_credit_card_payment,
            "salary" to R.string.cat_salary,
            "bonifico" to R.string.cat_bonifico,
            "gift" to R.string.cat_gift,
            "refund" to R.string.cat_refund,
            "investment" to R.string.cat_investment,
            "credit_card_adjustment" to R.string.cat_credit_card_adjustment,
        )
    }
}
