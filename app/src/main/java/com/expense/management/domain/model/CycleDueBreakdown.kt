package com.expense.management.domain.model

import kotlin.math.roundToLong

/**
 * Composizione del dovuto di un ciclo. Ogni voce è un importo positivo; il totale è
 * spese − rimborsi − quota spostata a rate + rate in scadenza + revolving + interessi,
 * mai negativo e arrotondato al centesimo.
 */
data class CycleDueBreakdown(
    val purchases: Double,
    val credits: Double = 0.0,
    val movedToInstallments: Double = 0.0,
    val installmentsDue: Double = 0.0,
    val revolvingDue: Double = 0.0,
    val interest: Double = 0.0,
) {
    init {
        listOf(purchases, credits, movedToInstallments, installmentsDue, revolvingDue, interest).forEach {
            require(it >= 0.0 && it.isFinite()) { "voce del dovuto non valida: $it" }
        }
        require(movedToInstallments <= purchases + CENT_TOLERANCE) {
            "quota a rate ($movedToInstallments) oltre le spese del ciclo ($purchases)"
        }
    }

    val totalDue: Double
        get() {
            val raw = purchases - credits - movedToInstallments + installmentsDue + revolvingDue + interest
            return (raw.coerceAtLeast(0.0) * 100).roundToLong() / 100.0
        }

    private companion object {
        const val CENT_TOLERANCE = 0.005
    }
}
