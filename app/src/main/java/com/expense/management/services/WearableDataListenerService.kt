package com.expense.management.services

import com.expense.management.data.AppDatabase
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.UUID

class WearableDataListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/add_transaction") {
            val payload = String(messageEvent.data, StandardCharsets.UTF_8)
            val parts = payload.split("|")
            if (parts.size >= 2) {
                val amountStr = parts[0]
                val description = parts[1]
                val amount = amountStr.toDoubleOrNull()

                if (amount != null) {
                    val db = AppDatabase.getDatabase(applicationContext)
                    val dao = db.transactionDao()

                    // Recuperiamo la valuta preferita dalle SharedPreferences, default Euro
                    val prefs = applicationContext.getSharedPreferences("prefs", MODE_PRIVATE)
                    val currency = prefs.getString("currency", "€") ?: "€"

                    scope.launch {
                        val now = LocalDate.now().toString()
                        val transaction = TransactionEntity(
                            id = UUID.randomUUID().toString(),
                            date = now,
                            description = description,
                            amount = amount,
                            categoryId = "other",
                            type = TransactionType.EXPENSE,
                            isCreditCard = false,
                            effectiveDate = now,
                            originalAmount = amount,
                            originalCurrency = currency,
                        )
                        dao.insert(transaction)
                    }
                }
            }
        }
    }
}
