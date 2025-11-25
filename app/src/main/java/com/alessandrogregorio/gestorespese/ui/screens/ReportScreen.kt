package com.alessandrogregorio.gestorespese.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        Text("Report $currentYear", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Card Risparmio Totale Anno
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Saldo Netto Annuale ($currentYear)", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "$currencySymbol ${String.format(Locale.ITALIAN, "%.2f", savings)}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (savings >= 0) Color.White else MaterialTheme.colorScheme.errorContainer,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Spese per Categoria (Mese Corrente)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text("Totale Spese: $currencySymbol ${String.format(Locale.ITALIAN, "%.2f", totalMonthlyExpense)}", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(12.dp))

        // Lista Spese per Categoria
        LazyColumn(modifier = Modifier.fillMaxHeight()) {
            items(expenseByCategory) { (categoryId, amount) ->
                val category = CATEGORIES.firstOrNull { it.id == categoryId }
                val percentage = if (totalMonthlyExpense > 0) (amount / totalMonthlyExpense) * 100 else 0.0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nome Categoria
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = category?.icon ?: "❓",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(category?.label ?: "Altro", style = MaterialTheme.typography.titleMedium)
                    }

                    // Importo e Percentuale
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$currencySymbol ${String.format(Locale.ITALIAN, "%.2f", amount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = String.format(Locale.ITALIAN, "%.1f%%", percentage),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
