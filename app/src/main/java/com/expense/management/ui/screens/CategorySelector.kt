package com.expense.management.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.expense.management.R
import com.expense.management.data.CategoryEntity
import com.expense.management.data.TransactionType
import com.expense.management.utils.CategoryImage

@Composable
fun CategorySelector(
    type: TransactionType,
    selectedCategoryId: String?,
    onCategorySelected: (String) -> Unit,
    availableCategories: List<CategoryEntity>,
    frequentExpenseCategories: List<CategoryEntity> = emptyList(),
    frequentIncomeCategories: List<CategoryEntity> = emptyList(),
) {
    val frequentCategories = remember(type, frequentExpenseCategories, frequentIncomeCategories) {
        if (type == TransactionType.EXPENSE) frequentExpenseCategories else frequentIncomeCategories
    }

    var searchQuery by remember { mutableStateOf("") }
    var showAllDialog by remember { mutableStateOf(false) }
    val filteredCategories = remember(availableCategories, type, searchQuery) {
        availableCategories.filter { it.type == type && it.label.contains(searchQuery, ignoreCase = true) }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.search_categories)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_search))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
            )

            if (searchQuery.isEmpty()) {
                val selectedCategory = remember(selectedCategoryId, availableCategories) {
                    availableCategories.find { it.id == selectedCategoryId }
                }
                if (selectedCategory != null && selectedCategory.type == type) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp),
                    ) {
                        CategoryImage(category = selectedCategory, size = 20.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            selectedCategory.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (frequentCategories.isNotEmpty() && searchQuery.isEmpty()) {
                Text(
                    text = stringResource(R.string.frequent_categories),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    frequentCategories.forEach { category ->
                        key(category.id) {
                            CategoryChip(
                                category = category,
                                isSelected = selectedCategoryId == category.id,
                                onClick = { onCategorySelected(category.id) },
                            )
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
            }

            if (searchQuery.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.search_results),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
                )
                CategoryGrid(
                    categories = filteredCategories,
                    selectedCategoryId = selectedCategoryId,
                    onCategorySelected = onCategorySelected,
                )
            } else {
                TextButton(
                    onClick = { showAllDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.show_all_categories))
                }
            }
        }
    }

    if (showAllDialog) {
        CategoryPickerDialog(
            type = type,
            availableCategories = availableCategories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = { id ->
                onCategorySelected(id)
                showAllDialog = false
            },
            onDismiss = { showAllDialog = false },
        )
    }
}

@Composable
private fun CategoryGrid(
    categories: List<CategoryEntity>,
    selectedCategoryId: String?,
    onCategorySelected: (String) -> Unit,
) {
    if (categories.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_categories_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        val chunks = remember(categories) { categories.chunked(4) }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            chunks.forEach { rowCategories ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowCategories.forEach { category ->
                        key(category.id) {
                            CategoryGridItem(
                                modifier = Modifier.weight(1f),
                                category = category,
                                isSelected = selectedCategoryId == category.id,
                                onClick = { onCategorySelected(category.id) },
                            )
                        }
                    }
                    if (rowCategories.size < 4) {
                        repeat(4 - rowCategories.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryGridItem(
    category: CategoryEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                    shape = RoundedCornerShape(16.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            CategoryImage(category = category, size = 32.dp)
        }
        Text(
            text = category.label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
fun CategoryChip(
    category: CategoryEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.height(48.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            CategoryImage(category = category, size = 24.dp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = category.label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}
