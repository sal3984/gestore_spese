package com.alessandrogregorio.gestorespese.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alessandrogregorio.gestorespese.data.TransactionEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    transactions: List<TransactionEntity>,
    currencySymbol: String,
    ccLimit: Float,
    onDelete: (Long) -> Unit,
    onEdit: (Long) -> Unit // Handler per la modifica
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val currentMonthStr = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))

    // Transazioni che hanno effetto in questo mese
    val currentTrans = transactions.filter { it.effectiveDate.startsWith(currentMonthStr) }

    val income = currentTrans.filter { it.type == "income" }.sumOf { it.amount }
    val expense = currentTrans.filter { it.type == "expense" }.sumOf { it.amount }
    val balance = income - expense

    // CALCOLO UTILIZZO CARTA DI CREDITO
    val creditCardUsed = transactions
        .filter { it.date.startsWith(currentMonthStr) && it.isCreditCard && it.type == "expense" }
        .sumOf { it.amount }

    val ccProgress = if (ccLimit > 0) (creditCardUsed.toFloat() / ccLimit).coerceIn(0f, 1f) else 0f

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        MonthSelector(selectedDate) { selectedDate = it }

        Spacer(modifier = Modifier.height(16.dp))

        // Saldo Mensile
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Saldo Mensile")
                Text("$currencySymbol ${String.format("%.2f", balance)}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = if(balance>=0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // MONITORAGGIO CARTA DI CREDITO
        if (ccLimit > 0) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Plafond Carta (Spese fatte ora)", style = MaterialTheme.typography.labelMedium)
                        Text("$currencySymbol ${String.format("%.0f", creditCardUsed)} / $currencySymbol ${String.format("%.0f", ccLimit)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { ccProgress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).background(Color.LightGray, RoundedCornerShape(4.dp)),
                        color = when {
                            ccProgress >= 1f -> MaterialTheme.colorScheme.error // Rosso
                            ccProgress > 0.8f -> Color(0xFFFBBC05) // Giallo
                            else -> MaterialTheme.colorScheme.primary // Blu
                        },
                        trackColor = Color.LightGray.copy(alpha = 0.5f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Lista Transazioni
        Text("Movimenti del Mese (Addebito):", fontWeight = FontWeight.Bold)
        LazyColumn(modifier = Modifier.fillMaxHeight()) {
            items(currentTrans, key = { it.id }) { t ->
                TransactionItem(t, currencySymbol, onDelete, onEdit) // Passa onEdit
            }
        }
    }
}