package com.alessandrogregorio.gestorespese.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.alessandrogregorio.gestorespese.data.TransactionEntity
import com.alessandrogregorio.gestorespese.ui.screens.category.CATEGORIES
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ReportScreen(
    transactions: List<TransactionEntity>,
    currencySymbol: String,
    dateFormat: String,
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

    // Calcolo Bilancio Mensile (Ultimi 6 Mesi)
    val monthlyBalances = remember(transactions) {
        val currentYearMonth = YearMonth.now()
        (0..5).map { i ->
            val targetMonth = currentYearMonth.minusMonths(5 - i.toLong())
            val monthlyTransactions = transactions.filter {
                try {
                    YearMonth.from(LocalDate.parse(it.effectiveDate)) == targetMonth
                } catch (e: Exception) {
                    false
                }
            }
            val income = monthlyTransactions.filter { it.type == "income" }.sumOf { it.amount }
            val expense = monthlyTransactions.filter { it.type == "expense" }.sumOf { it.amount }
            targetMonth to (income - expense)
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
        // --- HEADER: Report Annuale ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                )
                .padding(24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                "Report $currentYear",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        "Risparmio Totale",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "$currencySymbol ${String.format(Locale.ITALIAN, "%.2f", savings)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (savings >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    tint = Color.White
                )
            }
        }

        // --- CONTENUTO ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = (-20).dp)
                .padding(horizontal = 16.dp)
        ) {
            // Grafico a Barre Mensili
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Bilancio Ultimi 6 Mesi",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    MonthlyBarChart(monthlyBalances, currencySymbol)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Dettaglio Categorie (Mese Corrente)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            // Lista Spese per Categoria
            LazyColumn(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(expenseByCategory) { (categoryId, amount) ->
                    val category = CATEGORIES.firstOrNull { it.id == categoryId }
                    val percentage = if (totalMonthlyExpense > 0) (amount / totalMonthlyExpense).toFloat() else 0f

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icona
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = category?.icon ?: "❓", style = MaterialTheme.typography.titleMedium)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Dati
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    category?.label ?: "Altro",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "$currencySymbol ${String.format(Locale.ITALIAN, "%.2f", amount)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            // Progress Bar
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LinearProgressIndicator(
                                    progress = { percentage },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    strokeCap = StrokeCap.Round,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = String.format(Locale.ITALIAN, "%.0f%%", percentage * 100),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyBarChart(data: List<Pair<YearMonth, Double>>, currencySymbol: String) {
    if (data.isEmpty()) return

    val maxAbs = data.maxOfOrNull { kotlin.math.abs(it.second) }?.toFloat()?.coerceAtLeast(1f) ?: 1f
    var selectedMonth by remember { mutableStateOf<YearMonth?>(null) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { (month, balance) ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null // Rimuovi il ripple effect
                        ) { selectedMonth = if (selectedMonth == month) null else month },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Area del grafico (Divisa in due: Positiva e Negativa)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        // PARTE POSITIVA (Sopra la linea)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (balance > 0) {
                                val heightFraction = (balance.toFloat() / maxAbs).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(heightFraction)
                                        .width(16.dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(
                                            if (selectedMonth == month) Color(0xFF2E7D32) else Color(0xFF43A047) // Verde scuro se selezionato
                                        )
                                )
                            }
                        }

                        // Linea dello Zero
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                        // PARTE NEGATIVA (Sotto la linea)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            if (balance < 0) {
                                val heightFraction = (kotlin.math.abs(balance).toFloat() / maxAbs).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(heightFraction)
                                        .width(16.dp)
                                        .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                                        .background(
                                            if (selectedMonth == month) Color(0xFFB71C1C) else MaterialTheme.colorScheme.error // Rosso scuro se selezionato
                                        )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Etichetta Mese
                    Text(
                        text = month.month.getDisplayName(TextStyle.SHORT, Locale.ITALIAN).uppercase().take(3),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selectedMonth == month) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = 10.sp,
                        color = if (selectedMonth == month) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }

        // Popup informativo
        selectedMonth?.let { month ->
            val balance = data.find { it.first == month }?.second ?: 0.0

            // Posizionamento semplice al centro (o in alto/basso a seconda di dove vuoi)
            // Per semplicità lo metto in alto al centro del grafico per ora
            Popup(
                alignment = Alignment.TopCenter,
                onDismissRequest = { selectedMonth = null }
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier.padding(top = 16.dp) // Un po' di offset
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                         Text(
                             text = month.month.getDisplayName(TextStyle.FULL, Locale.ITALIAN).replaceFirstChar { it.uppercase() },
                             style = MaterialTheme.typography.labelMedium,
                             color = MaterialTheme.colorScheme.inverseOnSurface
                         )
                         Text(
                             text = "$currencySymbol ${String.format(Locale.ITALIAN, "%.2f", balance)}",
                             style = MaterialTheme.typography.bodyLarge,
                             fontWeight = FontWeight.Bold,
                             color = if(balance >= 0) Color(0xFF66BB6A) else Color(0xFFEF5350)
                         )
                    }
                }
            }
        }
    }
}
