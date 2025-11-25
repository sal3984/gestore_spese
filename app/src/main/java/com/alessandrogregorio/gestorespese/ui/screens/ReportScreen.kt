package com.alessandrogregorio.gestorespese.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alessandrogregorio.gestorespese.data.TransactionEntity
import java.time.LocalDate

@Composable
fun ReportScreen(
    transactions: List<TransactionEntity>,
    ccDelay: Int,
    onDelayChange: (Int) -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit
) {
    // Calcolo Risparmio Anno Corrente
    val currentYear = LocalDate.now().year
    val savings = transactions
        .filter { LocalDate.parse(it.effectiveDate).year == currentYear }
        .sumOf { if(it.type == "income") it.amount else -it.amount }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Report $currentYear", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Card Risparmio
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Risparmio Totale Anno", style = MaterialTheme.typography.titleMedium)
                Text(formatMoney(savings), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                Text("Entrate - Uscite (date effettive)", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top=8.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Impostazioni", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Slider Ritardo Carta
        Text("Ritardo Carta di Credito: $ccDelay mesi", fontWeight = FontWeight.Bold)
        Slider(
            value = ccDelay.toFloat(),
            onValueChange = { onDelayChange(it.toInt()) },
            valueRange = 0f..6f,
            steps = 5
        )
        Text("0 = Addebito immediato. 1 = Mese successivo.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // Sezione Backup
        Text("Backup & Ripristino", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Salva o ripristina i dati (JSON)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onBackup,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9D58)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Share, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Backup")
            }

            Button(
                onClick = onRestore,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ripristina")
            }
        }
    }
}