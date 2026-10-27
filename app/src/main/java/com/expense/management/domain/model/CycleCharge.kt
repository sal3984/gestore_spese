package com.expense.management.domain.model

import java.time.LocalDate

/** Addebito atteso di un ciclo non ancora saldato: data e importo ad oggi. */
data class CycleCharge(
    val cardId: String,
    val dueDate: LocalDate,
    val amount: Double,
)
