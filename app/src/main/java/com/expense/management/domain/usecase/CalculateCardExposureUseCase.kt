package com.expense.management.domain.usecase

import com.expense.management.domain.model.CardExposure
import kotlin.math.roundToLong

/**
 * Plafond di una carta: disponibile = limite - esposizione non liquidata. Accrediti oltre le
 * spese non aumentano il plafond, e oltre il limite il disponibile resta a zero.
 */
class CalculateCardExposureUseCase {

    fun execute(limit: Double, unsettledExposure: Double): CardExposure {
        val exposure = unsettledExposure.coerceAtLeast(0.0).toCents()
        return CardExposure(
            exposure = exposure,
            available = (limit - exposure).coerceAtLeast(0.0).toCents(),
            utilization = if (limit > 0) (exposure / limit).toFloat() else 0f,
        )
    }

    private fun Double.toCents(): Double = (this * 100).roundToLong() / 100.0
}
