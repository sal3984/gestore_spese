package com.expense.management.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expense.management.R
import com.expense.management.data.CategoryEntity
import com.expense.management.data.TransactionEntity
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// --- COMPONENTI UI CONDIVISI ---

@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    dateFormat: String,
    isAmountHidden: Boolean,
    onDelete: (String) -> Unit,
    onEdit: (String) -> Unit
) {
    val category = getCategory(transaction.categoryId, categories)
    val categoryLabel = getLocalizedCategoryLabel(category)
    val isIncome = transaction.type == "income"
    val amountColor = if (isIncome) Color(0xFF43A047) else MaterialTheme.colorScheme.onSurface

    // Modifica: Formattazione data da ISO a formato utente (con fallback retrocompatibile)
    val formattedDate = remember(transaction.date, dateFormat) {
        try {
            // Prova a parsare come ISO (formato nuovo standard)
            LocalDate.parse(transaction.date, DateTimeFormatter.ISO_LOCAL_DATE)
                .format(DateTimeFormatter.ofPattern(dateFormat))
        } catch (e: Exception) {
            try {
                 // Fallback 1: Prova a parsare con il formato utente corrente (magari salvata così)
                 LocalDate.parse(transaction.date, DateTimeFormatter.ofPattern(dateFormat))
                     .format(DateTimeFormatter.ofPattern(dateFormat)) // Ridondante ma per verifica
            } catch (e2: Exception) {
                // Fallback 2: Restituisci la stringa grezza (vecchio formato custom)
                transaction.date
            }
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_transaction_title)) },
            text = { Text(stringResource(R.string.delete_transaction_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(transaction.id)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_uppercase))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel).uppercase())
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onEdit(transaction.id) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Flat look
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                             if(isIncome) MaterialTheme.colorScheme.secondaryContainer
                             else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.icon,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        transaction.description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )

                    // Riga con Categoria e Data
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Text(
                            categoryLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Separatore (bullet point)
                        Text(
                            " • ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        // Data visualizzata accanto alla categoria
                        Text(
                            formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if(transaction.isCreditCard && !isIncome) {
                        val ccLabel = if (transaction.installmentNumber != null && transaction.totalInstallments != null) {
                            "${stringResource(R.string.credit_card)} (${transaction.installmentNumber}/${transaction.totalInstallments})"
                        } else {
                            stringResource(R.string.credit_card)
                        }

                        Text(
                            ccLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // Sezione Importo e Delete
            Column(horizontalAlignment = Alignment.End) {
                val amountText = if (isAmountHidden) {
                    "*** ${currencySymbol}**"
                } else {
                    "${if (isIncome) "+" else "-"} $currencySymbol${String.format(Locale.getDefault(), "%.2f", transaction.amount)}"
                }

                Text(
                    text = amountText,
                    style = MaterialTheme.typography.titleMedium,
                    color = amountColor,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(24.dp).offset(x = 8.dp, y = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun getLocalizedCategoryLabel(category: CategoryEntity): String {
    return when(category.id) {
        "food" -> stringResource(R.string.cat_food)
        "transport" -> stringResource(R.string.cat_transport)
        "housing" -> stringResource(R.string.cat_housing)
        "entertainment" -> stringResource(R.string.cat_entertainment)
        "bills" -> stringResource(R.string.cat_bills)
        "health" -> stringResource(R.string.cat_health)
        "shopping" -> stringResource(R.string.cat_shopping)
        "other" -> stringResource(R.string.cat_other)
        "salary" -> stringResource(R.string.cat_salary)
        "bonifico" -> stringResource(R.string.cat_bonifico)
        "gift" -> stringResource(R.string.cat_gift)
        "refund" -> stringResource(R.string.cat_refund)
        "investment" -> stringResource(R.string.cat_investment)
        else -> category.label
    }
}

// --- Helpers ---

fun formatMoney(amount: Double): String = NumberFormat.getCurrencyInstance(Locale.getDefault()).format(amount)

fun getCategory(id: String, categories: List<CategoryEntity>): CategoryEntity {
    return categories.firstOrNull { it.id == id }
        ?: categories.firstOrNull { it.id == "other" }
        ?: CategoryEntity("other", "Altro", "❓", "expense", false)
}
