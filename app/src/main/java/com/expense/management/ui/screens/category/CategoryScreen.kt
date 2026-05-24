package com.expense.management.ui.screens.category

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.expense.management.R
import com.expense.management.data.CategoryEntity
import com.expense.management.data.TransactionType
import com.expense.management.ui.theme.gestoreSpeseTheme
import com.expense.management.utils.CategoryImage
import com.expense.management.utils.deleteImageFile
import com.expense.management.utils.saveImageToInternalStorage
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.UUID

private val availableIcons = listOf(
    "🏠", "🍔", "🚗", "🛒", "💊", "🎬", "✈️", "👔", "🎓", "🎁", "💡", "📱",
    "💰", "💸", "🏦", "📈", "💼", "🔧", "🐶", "👶", "🎉", "🏋️", "📚", "🎮",
    "💻", "☕", "🍻", "🍕", "🥦", "🚕", "⛽", "🏥", "👠", "⚽", "🎤", "🎨",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    modifier: Modifier = Modifier,
    categories: List<CategoryEntity>,
    onAddCategory: (CategoryEntity) -> Unit,
    onUpdateCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (String) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(TransactionType.EXPENSE) }
    val selectedTabIndex = if (selectedTab == TransactionType.EXPENSE) 0 else 1
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    categoryToEdit = null
                    showDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                text = { Text(stringResource(R.string.new_category)) },
                icon = { Icon(Icons.Default.Add, stringResource(R.string.add_icon_desc)) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                        width = Dp.Unspecified,
                        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                    )
                },
                divider = {},
            ) {
                Tab(
                    selected = selectedTab == TransactionType.EXPENSE,
                    onClick = { selectedTab = TransactionType.EXPENSE },
                    text = { Text(stringResource(R.string.expenses_tab), fontWeight = if (selectedTab == TransactionType.EXPENSE) FontWeight.Bold else FontWeight.Normal) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Tab(
                    selected = selectedTab == TransactionType.INCOME,
                    onClick = { selectedTab = TransactionType.INCOME },
                    text = { Text(stringResource(R.string.income_tab), fontWeight = if (selectedTab == TransactionType.INCOME) FontWeight.Bold else FontWeight.Normal) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            val filteredCategories = categories.filter { it.isCustom && it.type == selectedTab }

            if (filteredCategories.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_custom_categories),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp, top = 16.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filteredCategories, key = { it.id }) { category ->
                        CategoryCard(
                            category = category,
                            onEdit = {
                                categoryToEdit = category
                                showDialog = true
                            },
                            onDelete = {
                                category.imageUri?.let { deleteImageFile(context, it) }
                                onDeleteCategory(category.id)
                            },
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        CategoryDialog(
            type = selectedTab,
            existingCategories = categories,
            categoryToEdit = categoryToEdit,
            onDismiss = { showDialog = false },
            onConfirm = { label, icon, imageUri ->
                if (categoryToEdit == null) {
                    val exists = categories.any {
                        it.label.equals(label.trim(), ignoreCase = true) && it.type == selectedTab
                    }

                    if (!exists) {
                        val newCategory = CategoryEntity(
                            id = UUID.randomUUID().toString(),
                            label = label.trim(),
                            icon = icon,
                            type = selectedTab,
                            isCustom = true,
                            imageUri = imageUri,
                        )
                        onAddCategory(newCategory)
                    }
                } else {
                    val updatedCategory = categoryToEdit!!.copy(
                        label = label.trim(),
                        icon = icon,
                        imageUri = imageUri,
                    )
                    onUpdateCategory(updatedCategory)
                }
                showDialog = false
            },
        )
    }
}

@Composable
fun CategoryCard(
    modifier: Modifier = Modifier,
    category: CategoryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    CategoryImage(category = category, size = 32.dp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    category.label,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_category),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryDialog(
    modifier: Modifier = Modifier,
    type: TransactionType,
    existingCategories: List<CategoryEntity>,
    categoryToEdit: CategoryEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?) -> Unit,
) {
    var label by remember { mutableStateOf(categoryToEdit?.label ?: "") }
    var selectedIcon by remember { mutableStateOf(categoryToEdit?.icon ?: "🏷️") }
    var imageUri by remember { mutableStateOf<String?>(categoryToEdit?.imageUri) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isEditing = categoryToEdit != null
    val dialogTitle = if (isEditing) stringResource(R.string.edit_category) else if (type == TransactionType.EXPENSE) stringResource(R.string.new_expense_dialog) else stringResource(R.string.new_income_dialog)

    val msg = stringResource(R.string.error_msg_name)
    val msgDuplicate = stringResource(R.string.error_category_already_exists)

    val context = LocalContext.current
    val oldImageUri = categoryToEdit?.imageUri

    var lastPickedUri by remember { mutableStateOf<String?>(null) }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val croppedUri = UCrop.getOutput(result.data!!)
            imageUri = croppedUri?.toString() ?: lastPickedUri
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            lastPickedUri?.let { original ->
                saveImageToInternalStorage(context, original)?.let { imageUri = it }
            }
        }
        lastPickedUri = null
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            lastPickedUri = uri.toString()
            val dir = File(context.filesDir, "category_images")
            if (!dir.exists()) dir.mkdirs()
            val destFile = File(dir, "cat_${UUID.randomUUID()}.jpg")
            val uCrop = UCrop.of(uri, Uri.fromFile(destFile))
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(512, 512)
            val intent = uCrop.getIntent(context)
            cropLauncher.launch(intent)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = dialogTitle, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                            .clickable { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (imageUri != null) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.PhotoCamera, null, tint = Color.White)
                            }
                        } else {
                            Text(text = selectedIcon, style = MaterialTheme.typography.displayMedium)
                        }
                    }

                    if (imageUri != null) {
                        IconButton(onClick = { imageUri = null }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_image), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = {
                        label = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.category_name_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.icon_or_emoji),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    OutlinedTextField(
                        value = selectedIcon,
                        onValueChange = { selectedIcon = it },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.width(70.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                    )

                    Button(
                        onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.image), style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.suggest),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 6,
                ) {
                    availableIcons.forEach { icon ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (selectedIcon == icon && imageUri == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable {
                                    selectedIcon = icon
                                    imageUri = null
                                },
                        ) {
                            Text(text = icon, style = MaterialTheme.typography.titleLarge)
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
                    } else {
                        val isDuplicate = existingCategories.any {
                            it.label.equals(label.trim(), ignoreCase = true) &&
                                it.type == type &&
                                it.id != categoryToEdit?.id
                        }
                        if (isDuplicate) {
                            errorMessage = msgDuplicate
                        } else {
                            if (isEditing && oldImageUri != null && oldImageUri != imageUri) {
                                deleteImageFile(context, oldImageUri)
                            }
                            onConfirm(label, selectedIcon, imageUri)
                        }
                    }
                },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Preview(showBackground = true, name = "Category Light")
@Composable
private fun CategoryPreview() {
    gestoreSpeseTheme(darkTheme = false, dynamicColor = false) {
        CategoryScreen(categories = emptyList(), onAddCategory = {}, onUpdateCategory = {}, onDeleteCategory = {})
    }
}

@Preview(showBackground = true, name = "Category Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoryPreviewDark() {
    gestoreSpeseTheme(darkTheme = true, dynamicColor = false) {
        CategoryScreen(categories = emptyList(), onAddCategory = {}, onUpdateCategory = {}, onDeleteCategory = {})
    }
}
