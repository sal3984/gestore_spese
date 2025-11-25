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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.StrokeCap
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
    dateFormat: String,
    earliestMonth: YearMonth, // NUOVO PARAMETRO: Il mese della prima transazione
    onDelete: (Long) -> Unit,
    onEdit: (Long) -> Unit
) {
    val today = YearMonth.now()
    // Stato per il mese attualmente visualizzato (inizializzato al mese corrente)
    var currentDisplayedMonth by remember { mutableStateOf(today) }

    // Funzione per formattare il mese in Italiano (es: "Novembre 2025")
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALIAN)

    // Filtra transazioni per il MESE CORRENTE VISUALIZZATO (basato sulla data di addebito effettiva)
    val currentTrans = transactions
        .filter {
            try {
                YearMonth.from(LocalDate.parse(it.effectiveDate)) == currentDisplayedMonth
            } catch (e: Exception) {
                false // Ignora transazioni con data non valida
            }
        }
        .sortedByDescending { it.effectiveDate } // Ordina per data addebito

    // Calcolo Totali
    val totalIncome = currentTrans.filter { it.type == "income" }.sumOf { it.amount }
    val totalExpense = currentTrans.filter { it.type == "expense" }.sumOf { it.amount }
    val netBalance = totalIncome - totalExpense

    // Calcolo Carta di Credito (spese non ancora addebitate, ovvero quelle con data di addebito successiva al MESE CORRENTE VISUALIZZATO)
    // Mostriamo l'utilizzo CC solo per il mese corrente (today) per coerenza
    val isViewingCurrentMonth = currentDisplayedMonth == today

    val creditCardUsed = transactions
        .filter { it.isCreditCard && it.type == "expense" }
        .filter {
            try {
                // Filtra solo le spese CC la cui data di addebito è maggiore di OGGI
                YearMonth.from(LocalDate.parse(it.effectiveDate)) > today
            } catch (e: Exception) {
                false
            }
        }
        .sumOf { it.amount }

    val ccProgress = if (ccLimit > 0) (creditCardUsed / ccLimit).toFloat() else 0f

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        // --- CONTROLLI DI NAVIGAZIONE MESE ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulsante Mese Precedente
            IconButton(
                onClick = { currentDisplayedMonth = currentDisplayedMonth.minusMonths(1) },
                enabled = currentDisplayedMonth > earliestMonth // Disabilita se è il mese più vecchio
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mese Precedente", modifier = Modifier.size(32.dp))
            }

            // Mese Attuale Visualizzato
            Text(
                currentDisplayedMonth.format(monthFormatter).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ITALIAN) else it.toString() }, // Maiuscola la prima lettera
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Pulsante Mese Successivo
            IconButton(
                onClick = { currentDisplayedMonth = currentDisplayedMonth.plusMonths(1) },
                enabled = currentDisplayedMonth < today // Disabilita se è il mese corrente
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mese Successivo", modifier = Modifier.size(32.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Card Riepilogo Totale - MIGLIORATA
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Saldo Netto",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$currencySymbol ${String.format(Locale.ITALIAN, "%.2f", netBalance)}",
                    style = MaterialTheme.typography.displaySmall,
                    color = if (netBalance >= 0) Color(0xFF0F9D58) else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    // Entrate
                    Column(horizontalAlignment = Alignment.Start) {
                         Row(verticalAlignment = Alignment.CenterVertically) {
                             Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFF0F9D58), modifier = Modifier.size(18.dp))
                             Spacer(modifier = Modifier.width(4.dp))
                             Text("Entrate", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                         }
                         Text(
                             "$currencySymbol ${String.format(Locale.ITALIAN, "%.2f", totalIncome)}",
                             style = MaterialTheme.typography.titleMedium,
                             fontWeight = FontWeight.Bold,
                             color = MaterialTheme.colorScheme.onPrimaryContainer
                         )
                    }

                    // Uscite
                    Column(horizontalAlignment = Alignment.End) {
                         Row(verticalAlignment = Alignment.CenterVertically) {
                             Text("Uscite", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                             Spacer(modifier = Modifier.width(4.dp))
                             Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                         }
                         Text(
                             "$currencySymbol ${String.format(Locale.ITALIAN, "%.2f", totalExpense)}",
                             style = MaterialTheme.typography.titleMedium,
                             fontWeight = FontWeight.Bold,
                             color = MaterialTheme.colorScheme.onPrimaryContainer
                         )
                    }
                }
            }
        }

        // Card Plafond Carta di Credito (Mostrata solo se si è sul mese corrente)
        if (ccLimit > 0 && isViewingCurrentMonth) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Plafond Carta di Credito", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "$currencySymbol ${String.format(Locale.ITALIAN, "%.0f", creditCardUsed)} / $currencySymbol ${String.format(Locale.ITALIAN, "%.0f", ccLimit)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { ccProgress.coerceAtMost(1f) }, // Limita a 1 (100%)
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(Color.LightGray.copy(alpha=0.3f), RoundedCornerShape(5.dp)),
                        color = when {
                            ccProgress >= 1f -> MaterialTheme.colorScheme.error // Rosso
                            ccProgress > 0.8f -> Color(0xFFFBBC05) // Giallo
                            else -> MaterialTheme.colorScheme.primary // Blu
                        },
                        trackColor = Color.Transparent,
                        strokeCap = StrokeCap.Round
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Lista Transazioni
        Text(
            "Movimenti",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxHeight()) {
            items(currentTrans, key = { it.id }) { t ->
                TransactionItem(t, currencySymbol, dateFormat, onDelete, onEdit)
            }
        }
    }
}
