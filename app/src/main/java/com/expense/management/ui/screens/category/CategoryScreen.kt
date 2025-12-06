package com.expense.management.ui.screens.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expense.management.R
import com.expense.management.data.CategoryEntity
import com.expense.management.data.TransactionType
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    categories: List<CategoryEntity>,
    onAddCategory: (CategoryEntity) -> Unit,
    onUpdateCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    // CAMBIATO: selectedTab ora è un TransactionType
    var selectedTab by remember { mutableStateOf(TransactionType.EXPENSE) }
    val selectedTabIndex = if (selectedTab == TransactionType.EXPENSE) 0 else 1
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // STATO PER IL DIALOG
    var showDialog by remember { mutableStateOf(false) } // Rinominato da showDialog per chiarezza
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) } // Tiene traccia di chi stiamo modificando


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { categoryToEdit = null
                            showDialog = true
                          },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                text = { Text(stringResource(R.string.new_category)) },
                icon = { Icon(Icons.Default.Add, stringResource(R.string.add_icon_desc)) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Modern Tab Switcher
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                        width = Dp.Unspecified,
                        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                    )
                },
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == TransactionType.EXPENSE,
                    onClick = { selectedTab = TransactionType.EXPENSE },
                    text = { Text(stringResource(R.string.expenses_tab), fontWeight = if(selectedTab == TransactionType.EXPENSE) FontWeight.Bold else FontWeight.Normal) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Tab(
                    selected = selectedTab == TransactionType.INCOME,
                    onClick = { selectedTab = TransactionType.INCOME },
                    text = { Text(stringResource(R.string.income_tab), fontWeight = if(selectedTab == TransactionType.INCOME) FontWeight.Bold else FontWeight.Normal) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Lista Categorie
            val filteredCategories = categories.filter { it.isCustom && it.type == selectedTab }

            if (filteredCategories.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_custom_categories),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp, top = 16.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredCategories, key = { it.id }) { category ->
                        CategoryCard(
                            category = category,
                            onEdit = {
                                categoryToEdit = category
                                showDialog= true
                            },
                            onDelete = { onDeleteCategory(category.id) }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        CategoryDialog(
            type = selectedTab, // Passiamo direttamente l'enum
            existingCategories = categories,
            categoryToEdit= categoryToEdit,
            onDismiss = { showDialog = false },
            onConfirm = { label, icon ->
                // Controllo duplicati per nome (case insensitive)
                if(categoryToEdit == null) {
                    val exists = categories.any {
                        it.label.equals(
                            label.trim(),
                            ignoreCase = true
                        ) && it.type == selectedTab
                    }

                    if (!exists) {
                        val newCategory = CategoryEntity(
                            id = UUID.randomUUID().toString(),
                            label = label.trim(),
                            icon = icon,
                            type = selectedTab,
                            isCustom = true
                        )
                        onAddCategory(newCategory)

                    }
                }else{
                    // MODIFICA
                    val updatedCategory = categoryToEdit!!.copy(
                        label = label.trim(),
                        icon = icon
                        // Manteniamo ID e Type originali
                    )
                    onUpdateCategory(updatedCategory)
                }
                showDialog = false
            }
        )
    }
}

@Composable
fun CategoryCard(
    category: CategoryEntity,
    onEdit: () -> Unit, // <--- NUOVO PARAMETRO
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(category.icon, fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    category.label,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // <--- NUOVA ROW PER I PULSANTI
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_category), // Assicurati di avere questa stringa o usa "Modifica"
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryDialog(
    type: TransactionType,
    existingCategories: List<CategoryEntity>,
    categoryToEdit: CategoryEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var label by remember { mutableStateOf(categoryToEdit?.label ?: "") }
    var selectedIcon by remember { mutableStateOf(categoryToEdit?.icon ?: "🏷️") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val availableIcons = listOf(
        "🏠", "🍔", "🚗", "🛒", "💊", "🎬", "✈️", "👔", "🎓", "🎁", "💡", "📱",
        "💰", "💸", "🏦", "📈", "💼", "🔧", "🐶", "👶", "🎉", "🏋️", "📚", "🎮",
        "💻", "☕", "🍻", "🍕", "🥦", "🚕", "⛽", "🏥", "👠", "⚽", "🎤", "🎨"
    )

    val isEditing = categoryToEdit != null
    val dialogTitle = if (isEditing) stringResource(R.string.edit_category) else if(type == TransactionType.EXPENSE) stringResource(R.string.new_expense_dialog) else stringResource(R.string.new_income_dialog)

    val msg = stringResource(R.string.error_msg_name)
    val msgIcon = stringResource(R.string.error_msg_icon)
    val msgDuplicate = stringResource(R.string.error_category_already_exists)


    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = dialogTitle,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.insert_emoji),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Campo Nome
                    OutlinedTextField(
                        value = label,
                        onValueChange = {
                            label = it
                            errorMessage = null
                        },
                        label = { Text(stringResource(R.string.category_name_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        isError = errorMessage != null,
                        supportingText = {
                            if (errorMessage != null) {
                                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )

                    // Campo Icona
                    OutlinedTextField(
                        value = selectedIcon,
                        onValueChange = { input ->
                            if (input.length <= 2) {
                                selectedIcon = input
                            }
                        },
                        label = { Text("Icona") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.width(80.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 20.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sezione Icone suggerite
                Text(
                    text = stringResource(R.string.suggest),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 6
                ) {
                    availableIcons.forEach { icon ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedIcon == icon) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selectedIcon == icon) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedIcon = icon }
                        ) {
                            Text(text = icon, fontSize = 20.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (label.isBlank()) {
                        errorMessage = msg
                    } else if (selectedIcon.isBlank()) {
                        errorMessage = msgIcon
                    } else {
                        // LOGICA DUPLICATI AGGIORNATA
                        // Se stiamo modificando, ignoriamo la categoria stessa nel controllo duplicati
                        val isDuplicate = existingCategories.any {
                            it.label.equals(label.trim(), ignoreCase = true) &&
                                it.type == type &&
                                it.id != categoryToEdit?.id // Ignora se stesso se in edit
                        }

                        if (isDuplicate) {
                            errorMessage = msgDuplicate
                        } else {
                            onConfirm(label, selectedIcon)
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
