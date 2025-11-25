package com.alessandrogregorio.gestorespese.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alessandrogregorio.gestorespese.data.TransactionEntity
import com.alessandrogregorio.gestorespese.ui.screens.category.CATEGORIES
import java.time.LocalDate
import java.util.Locale

@Composable
fun ReportScreen(
    transactions: List<TransactionEntity>,
    currencySymbol: String,
    dateFormat: String, // NUOVO PARAMETRO (Aggiunto per consistenza)
) {
    // Calcolo Risparmio Anno Corrente
    val currentYear = LocalDate.now().year
    val savings = transactions
        .filter {
            try {
                LocalDate.parse(it.effectiveDate).year == currentYear
            } catch (e: Exception) {
                false
            }
        }
        .sumOf { if(it.type == "income") it.amount else -it.amount }

    // Calcolo Spese per Categoria (Mese Corrente)
    val currentMonth = LocalDate.now().monthValue
    val expenseByCategory = remember(transactions) {
        transactions
            .filter {
                it.type == "expense" && try {
                    LocalDate.parse(it.effectiveDate).monthValue == currentMonth
                } catch (e: Exception) {
                    false
                }
            }
            .groupBy { it.categoryId }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val totalMonthlyExpense = expenseByCategory.sumOf { it.second }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Report $currentYear",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Card Risparmio Totale Anno
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Saldo Netto Annuale",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currencySymbol ${String.format(Locale.ITALIAN, "%.2f", savings)}",
                        style = MaterialTheme.typography.headlineLarge,
                        color = if (savings >= 0) Color.White else MaterialTheme.colorScheme.errorContainer,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Icon(
                    imageVector = if (savings >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "Spese per Categoria (Mese Corrente)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Totale: $currencySymbol ${String.format(Locale.ITALIAN, "%.2f", totalMonthlyExpense)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Lista Spese per Categoria
        LazyColumn(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(expenseByCategory) { (categoryId, amount) ->
                val category = CATEGORIES.firstOrNull { it.id == categoryId }
                val percentage = if (totalMonthlyExpense > 0) (amount / totalMonthlyExpense).toFloat() else 0f

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icona e Nome
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = category?.icon ?: "❓",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    category?.label ?: "Altro",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Importo
                            Text(
                                text = "$currencySymbol ${String.format(Locale.ITALIAN, "%.2f", amount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Barra percentuale
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { percentage },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surface,
                                strokeCap = StrokeCap.Round,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = String.format(Locale.ITALIAN, "%.1f%%", percentage * 100),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
