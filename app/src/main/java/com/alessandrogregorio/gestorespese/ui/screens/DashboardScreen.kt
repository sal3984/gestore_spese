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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alessandrogregorio.gestorespese.data.TransactionEntity
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    transactions: List<TransactionEntity>,
    currencySymbol: String,
    ccLimit: Float,
    dateFormat: String, // NUOVO PARAMETRO
    onDelete: (Long) -> Unit,
    onEdit: (Long) -> Unit
) {
    val currentMonth = YearMonth.now()
    val formatter = DateTimeFormatter.ofPattern(dateFormat) // Usiamo il formato data

    // Filtra transazioni per il mese corrente (basato sulla data di addebito effettiva)
    val currentTrans = transactions
        .filter {
            try {
                YearMonth.from(LocalDate.parse(it.effectiveDate)) == currentMonth
            } catch (e: Exception) {
                false // Ignora transazioni con data non valida
            }
        }
        .sortedByDescending { it.effectiveDate } // Ordina per data addebito

    // Calcolo Totali
    val totalIncome = currentTrans.filter { it.type == "income" }.sumOf { it.amount }
    val totalExpense = currentTrans.filter { it.type == "expense" }.sumOf { it.amount }
    val netBalance = totalIncome - totalExpense

    // Calcolo Carta di Credito (spese non ancora addebitate, ovvero quelle con data di addebito successiva al mese corrente)
    val creditCardUsed = transactions
        .filter { it.isCreditCard && it.type == "expense" }
        .filter {
            try {
                YearMonth.from(LocalDate.parse(it.effectiveDate)) > currentMonth
            } catch (e: Exception) {
                false
            }
        }
        .sumOf { it.amount }

    val ccProgress = if (ccLimit > 0) (creditCardUsed / ccLimit).toFloat() else 0f

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Text("Riepilogo del Mese", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Card Riepilogo Totale
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Saldo Netto (${currentMonth.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.ITALIAN)})", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "$currencySymbol ${String.format(Locale.ITALIAN, "%.2f", netBalance)}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (netBalance >= 0) Color(0xFF0F9D58) else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Entrate:", style = MaterialTheme.typography.bodyMedium)
                    Text("$currencySymbol ${String.format(Locale.ITALIAN, "%.2f", totalIncome)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Uscite:", style = MaterialTheme.typography.bodyMedium)
                    Text("$currencySymbol ${String.format(Locale.ITALIAN, "%.2f", totalExpense)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Card Plafond Carta di Credito (Solo se c'è plafond)
        if (ccLimit > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Utilizzo Carta (Spese fatte ora)", style = MaterialTheme.typography.labelMedium)
                        Text("$currencySymbol ${String.format(Locale.ITALIAN, "%.0f", creditCardUsed)} / $currencySymbol ${String.format(Locale.ITALIAN, "%.0f", ccLimit)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { ccProgress.coerceAtMost(1f) }, // Limita a 1 (100%)
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
                // NOTA: Qui TransactionItem deve usare 'dateFormat' per formattare le date!
                TransactionItem(t, currencySymbol, dateFormat, onDelete, onEdit) // Passa dateFormat
            }
        }
    }
}
