package com.alessandrogregorio.gestorespese.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alessandrogregorio.gestorespese.data.TransactionEntity
import com.alessandrogregorio.gestorespese.ui.screens.category.CATEGORIES
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// --- COMPONENTI UI CONDIVISI ---

@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    currencySymbol: String,
    dateFormat: String,
    onDelete: (Long) -> Unit,
    onEdit: (Long) -> Unit
) {
    val category = getCategory(transaction.categoryId)
    val isIncome = transaction.type == "income"
    val amountColor = if (isIncome) Color(0xFF0F9D58) else MaterialTheme.colorScheme.error

    val effectiveDate = try {
        LocalDate.parse(transaction.effectiveDate)
    } catch (e: Exception) {
        LocalDate.now()
    }

    val dateFormatter = try {
        DateTimeFormatter.ofPattern(dateFormat)
    } catch (e: Exception) {
        DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }
    val formattedDate = effectiveDate.format(dateFormatter)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onEdit(transaction.id) },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Icona Categoria
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.icon,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        transaction.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Text(
                            category.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "• $formattedDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    if(transaction.isCreditCard && !isIncome) {
                        Text(
                            "Carta di Credito",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // Sezione Importo e Pulsante Elimina
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isIncome) "+" else "-"} $currencySymbol ${String.format(Locale.ITALIAN, "%.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = amountColor,
                    fontWeight = FontWeight.ExtraBold
                )

                IconButton(
                    onClick = { onDelete(transaction.id) },
                    modifier = Modifier.size(32.dp).padding(top = 8.dp)
                ) {
                     Icon(
                         Icons.Default.Delete,
                         contentDescription = "Elimina",
                         tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                         modifier = Modifier.size(20.dp)
                     )
                }
            }
        }
    }
}

// --- Helpers ---

fun formatMoney(amount: Double): String = NumberFormat.getCurrencyInstance(Locale.ITALY).format(amount)

fun getCategory(id: String) = CATEGORIES.firstOrNull { it.id == id } ?: CATEGORIES.first { it.id == "other" }
