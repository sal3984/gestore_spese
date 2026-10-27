package com.expense.management.domain.model

import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

/**
 * Estratto di una carta a saldo: plafond, cicli in attesa di addebito, ciclo in corso e storico.
 */
data class CardStatement(
    val cardId: String,
    val cardName: String,
    val limit: Double,
    val closingDay: Int,
    val paymentDay: Int,
    val exposure: CardExposure,
    val linkedAccountName: String?,
    // CLOSED e PARTIALLY_PAID, dal primo addebito in scadenza
    val upcomingCharges: List<StatementCycle>,
    // OPEN, dal piu' vecchio
    val openCycles: List<StatementCycle>,
    // PAID, dal piu' recente
    val history: List<StatementCycle>,
)

data class StatementCycle(
    val cycleId: String,
    val cycleMonth: YearMonth,
    val closingDate: LocalDate,
    val dueDate: LocalDate,
    val status: CycleStatus,
    // Spese meno accrediti con data fino a oggi
    val total: Double,
    val entries: List<StatementEntry>,
    // Ricorrenze gia' generate con data futura: elencate, non conteggiate
    val scheduledEntries: List<StatementEntry>,
    // Importo dell'addebito sul conto; null se il ciclo non ne ha
    val settledAmount: Double?,
    // Ciclo segnato pagato dalla migration, quando ogni acquisto usciva dal conto da solo
    val settledBeforeUpdate: Boolean,
) {
    /** Le spese del ciclo sono cambiate dopo l'addebito. */
    val settlementMismatch: Boolean
        get() = settledAmount != null && abs(settledAmount - total) >= CENT / 2

    private companion object {
        const val CENT = 0.01
    }
}

data class StatementEntry(
    val transactionId: String,
    val description: String,
    val date: LocalDate,
    val amount: Double,
    val isCredit: Boolean,
)
