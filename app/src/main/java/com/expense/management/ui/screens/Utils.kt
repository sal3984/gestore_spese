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
import com.expense.management.ui.theme.ExpenseRed
import com.expense.management.ui.theme.IncomeGreen
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

    // Use theme colors if possible, otherwise fallback
    val amountColor = if (isIncome) IncomeGreen else ExpenseRed

    val formattedDate = remember(transaction.date, dateFormat) {
        try {
            LocalDate.parse(transaction.date, DateTimeFormatter.ISO_LOCAL_DATE)
                .format(DateTimeFormatter.ofPattern(dateFormat))
        } catch (e: Exception) {
            try {
                 LocalDate.parse(transaction.date, DateTimeFormatter.ofPattern(dateFormat))
                     .format(DateTimeFormatter.ofPattern(dateFormat))
            } catch (e2: Exception) {
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
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                             if(isIncome) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                             else MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.icon,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        transaction.description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Text(
                            categoryLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            " • ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

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
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Sezione Importo
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
                    fontWeight = FontWeight.ExtraBold
                )
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
