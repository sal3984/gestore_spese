package com.expense.management.domain.model

/**
 * Stato di un ciclo di fatturazione. Il ciclo si apre, chiude alla data di chiusura,
 * poi viene saldato in tutto o in parte. Annullare il pagamento lo riporta a CLOSED.
 */
enum class CycleStatus {
    OPEN,
    CLOSED,
    PARTIALLY_PAID,
    PAID,
    ;

    fun canMoveTo(next: CycleStatus): Boolean = next in allowedNext()

    private fun allowedNext(): Set<CycleStatus> = when (this) {
        OPEN -> setOf(CLOSED)
        CLOSED -> setOf(PARTIALLY_PAID, PAID)
        PARTIALLY_PAID -> setOf(PAID, CLOSED)
        PAID -> setOf(CLOSED)
    }

    companion object {
        fun safeValueOf(name: String): CycleStatus? =
            entries.find { it.name == name }
    }
}
