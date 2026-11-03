package com.expense.management.domain.model

/**
 * Quando chiedere di nuovo lo sblocco biometrico: se l'app e' rimasta fuori dallo schermo per almeno
 * [timeoutMillis]. Un rientro rapido (notifica, cambio app veloce) non blocca.
 */
class AppLockTimer(
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private var leftAt: Long? = null

    fun onLeave() {
        leftAt = now()
    }

    /** true se al rientro va chiesto lo sblocco. Ogni uscita conta per un solo rientro. */
    fun shouldLockOnReturn(): Boolean {
        val left = leftAt ?: return false
        leftAt = null
        return now() - left >= timeoutMillis
    }

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 60_000L
    }
}
