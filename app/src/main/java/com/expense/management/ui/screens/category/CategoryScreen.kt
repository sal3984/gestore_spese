package com.expense.management.ui.screens.category

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
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

@Composable
fun CategoryScreen(
    categories: List<CategoryEntity>,
    onAddCategory: (CategoryEntity) -> Unit,
    onUpdateCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(TransactionType.EXPENSE) }
    var showDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    val context = LocalContext.current

    CategoryScreenStateless(
        state = CategoryUiState(
            categories = categories,
            selectedTab = selectedTab,
            showDialog = showDialog,
            categoryToEdit = categoryToEdit,
        ),
        onEvent = { event ->
            when (event) {
                is CategoryEvent.OnTabChanged -> selectedTab = event.tab
                is CategoryEvent.OnAddCategoryClick -> {
                    categoryToEdit = null
                    showDialog = true
                }
                is CategoryEvent.OnEditCategoryClick -> {
                    categoryToEdit = event.category
                    showDialog = true
                }
                is CategoryEvent.OnDeleteCategoryClick -> onDeleteCategory(event.categoryId)
                is CategoryEvent.OnDialogDismiss -> {
                    showDialog = false
                    categoryToEdit = null
                }
                is CategoryEvent.OnDialogConfirm -> {
                    val oldImageUri = categoryToEdit?.imageUri
                    if (oldImageUri != null && oldImageUri != event.imageUri) {
                        deleteImageFile(context, oldImageUri)
                    }
                    if (categoryToEdit == null) {
                        val exists = categories.any {
                            it.label.equals(event.label.trim(), ignoreCase = true) && it.type == selectedTab
                        }
                        if (!exists) {
                            onAddCategory(
                                CategoryEntity(
                                    id = UUID.randomUUID().toString(),
                                    label = event.label.trim(),
                                    icon = event.icon,
                                    type = selectedTab,
                                    isCustom = true,
                                    imageUri = event.imageUri,
                                ),
                            )
                        }
                    } else {
                        onUpdateCategory(
                            categoryToEdit!!.copy(
                                label = event.label.trim(),
                                icon = event.icon,
                                imageUri = event.imageUri,
                            ),
                        )
                    }
                    showDialog = false
                    categoryToEdit = null
                }
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryScreenStateless(
    state: CategoryUiState,
    onEvent: (CategoryEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val selectedTabIndex = if (state.selectedTab == TransactionType.EXPENSE) 0 else 1

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onEvent(CategoryEvent.OnAddCategoryClick) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                text = { Text(stringResource(R.string.new_category)) },
                icon = { Icon(Icons.Default.Add, stringResource(R.string.add_icon_desc)) },
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            CategoryTabRow(
                selectedTab = state.selectedTab,
                onTabChange = { onEvent(CategoryEvent.OnTabChanged(it)) },
                selectedTabIndex = selectedTabIndex,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Crossfade(
                targetState = state.selectedTab,
                modifier = Modifier.weight(1f),
            ) { tab ->
                val filteredCategories = state.categories.filter { it.isCustom && it.type == tab }

                if (filteredCategories.isEmpty()) {
                    CategoryEmptyState()
                } else {
                    CategoryList(
                        categories = filteredCategories,
                        onEdit = { onEvent(CategoryEvent.OnEditCategoryClick(it)) },
                        onDelete = { onEvent(CategoryEvent.OnDeleteCategoryClick(it.id)) },
                    )
                }
            }
        }
    }

    if (state.showDialog) {
        CategoryDialog(
            type = state.selectedTab,
            existingCategories = state.categories,
            categoryToEdit = state.categoryToEdit,
            onDismiss = { onEvent(CategoryEvent.OnDialogDismiss) },
            onConfirm = { label, icon, imageUri ->
                onEvent(CategoryEvent.OnDialogConfirm(label, icon, imageUri))
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryTabRow(
    selectedTab: TransactionType,
    onTabChange: (TransactionType) -> Unit,
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
) {
    PrimaryTabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                width = Dp.Unspecified,
                shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
            )
        },
        divider = {},
        modifier = modifier,
    ) {
        Tab(
            selected = selectedTab == TransactionType.EXPENSE,
            onClick = { onTabChange(TransactionType.EXPENSE) },
            text = {
                Text(
                    stringResource(R.string.expenses_tab),
                    style = MaterialTheme.typography.titleSmall,
                )
            },
            selectedContentColor = MaterialTheme.colorScheme.primary,
            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Tab(
            selected = selectedTab == TransactionType.INCOME,
            onClick = { onTabChange(TransactionType.INCOME) },
            text = {
                Text(
                    stringResource(R.string.income_tab),
                    style = MaterialTheme.typography.titleSmall,
                )
            },
            selectedContentColor = MaterialTheme.colorScheme.primary,
            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CategoryEmptyState(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Category,
                contentDescription = stringResource(R.string.category_icon),
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
}

@Composable
private fun CategoryList(
    categories: List<CategoryEntity>,
    onEdit: (CategoryEntity) -> Unit,
    onDelete: (CategoryEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 88.dp, top = 16.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier,
    ) {
        items(categories, key = { it.id }) { category ->
            CategoryCard(
                category = category,
                onEdit = { onEdit(category) },
                onDelete = { onDelete(category) },
            )
        }
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
        shape = MaterialTheme.shapes.large,
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
                    style = MaterialTheme.typography.titleMedium,
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
    var isSaving by remember { mutableStateOf(false) }

    val isEditing = categoryToEdit != null
    val dialogTitle = if (isEditing) stringResource(R.string.edit_category) else stringResource(R.string.new_expense_dialog)
    val msg = stringResource(R.string.error_msg_name)
    val msgDuplicate = stringResource(R.string.error_category_already_exists)
    val context = LocalContext.current
    val oldImageUri = categoryToEdit?.imageUri
    var lastPickedUri by remember { mutableStateOf<String?>(null) }

    val cropLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            imageUri = UCrop.getOutput(result.data!!)?.toString() ?: lastPickedUri
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            lastPickedUri?.let { saveImageToInternalStorage(context, it)?.let { uri -> imageUri = uri } }
        }
        lastPickedUri = null
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            lastPickedUri = uri.toString()
            val dir = File(context.filesDir, "category_images").also { if (!it.exists()) it.mkdirs() }
            val destUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(dir, "cat_${UUID.randomUUID()}.jpg"))
            val intent = UCrop.of(uri, destUri).withAspectRatio(1f, 1f).withMaxResultSize(512, 512).getIntent(context)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            cropLauncher.launch(intent)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = dialogTitle, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                CategoryDialogIconPicker(
                    selectedIcon = selectedIcon,
                    imageUri = imageUri,
                    onPickImage = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onRemoveImage = { imageUri = null },
                    onIconSelect = { icon ->
                        selectedIcon = icon
                        imageUri = null
                    },
                )

                Spacer(modifier = Modifier.height(16.dp))

                CategoryDialogNameField(
                    value = label,
                    isError = errorMessage != null,
                    errorMessage = errorMessage,
                    onValueChange = {
                        label = it
                        errorMessage = null
                    },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (label.isBlank()) {
                        errorMessage = msg
                    } else {
                        val isDuplicate = existingCategories.any { it.label.equals(label.trim(), ignoreCase = true) && it.type == type && it.id != categoryToEdit?.id }
                        if (isDuplicate) {
                            errorMessage = msgDuplicate
                        } else {
                            isSaving = true
                            if (isEditing && oldImageUri != null && oldImageUri != imageUri) deleteImageFile(context, oldImageUri)
                            onConfirm(label, selectedIcon, imageUri)
                        }
                    }
                },
                enabled = !isSaving,
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun CategoryDialogIconPicker(
    selectedIcon: String,
    imageUri: String?,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
    onIconSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showGrid by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
                    .clickable { if (imageUri != null && !showGrid) onPickImage() },
                contentAlignment = Alignment.Center,
            ) {
                if (imageUri != null && !showGrid) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = stringResource(R.string.change_image),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = stringResource(R.string.change_image), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                } else {
                    Text(text = selectedIcon, style = MaterialTheme.typography.displayMedium)
                }
            }

            AnimatedVisibility(
                visible = imageUri != null && !showGrid,
                enter = expandVertically(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(200)),
            ) {
                IconButton(onClick = onRemoveImage) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_image), tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { showGrid = !showGrid },
                modifier = Modifier.weight(1f),
            ) {
                Text(text = "😊", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.emoji_button), style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
                onClick = {
                    showGrid = false
                    onPickImage()
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.photo_button), style = MaterialTheme.typography.labelSmall)
            }
        }

        if (showGrid) {
            Spacer(modifier = Modifier.height(12.dp))
            CategoryIconGrid(
                availableIcons = availableIcons,
                selectedIcon = selectedIcon,
                selectedImageUri = imageUri,
                onIconSelect = { icon ->
                    onIconSelect(icon)
                    showGrid = false
                },
            )
        }
    }
}

@Composable
private fun CategoryDialogNameField(
    value: String,
    isError: Boolean,
    errorMessage: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.category_name_label)) },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
        isError = isError,
        supportingText = {
            AnimatedVisibility(visible = isError, enter = expandVertically(animationSpec = tween(200)), exit = shrinkVertically(animationSpec = tween(200))) {
                Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error)
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryIconGrid(
    availableIcons: List<String>,
    selectedIcon: String,
    selectedImageUri: String?,
    onIconSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 6,
    ) {
        availableIcons.forEach { icon ->
            val isSelected = selectedIcon == icon && selectedImageUri == null
            val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .clickable { onIconSelect(icon) },
            ) {
                Text(text = icon, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
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

@Preview(showBackground = true, name = "Category Card Light")
@Composable
private fun CategoryCardPreview() {
    gestoreSpeseTheme(darkTheme = false, dynamicColor = false) {
        CategoryCard(
            category = CategoryEntity(id = "test", label = "Cibo", icon = "🍔", type = TransactionType.EXPENSE, isCustom = true),
            onEdit = {},
            onDelete = {},
        )
    }
}

@Preview(showBackground = true, name = "Category Card Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoryCardPreviewDark() {
    gestoreSpeseTheme(darkTheme = true, dynamicColor = false) {
        CategoryCard(
            category = CategoryEntity(id = "test", label = "Cibo", icon = "🍔", type = TransactionType.EXPENSE, isCustom = true),
            onEdit = {},
            onDelete = {},
        )
    }
}

@Preview(showBackground = true, name = "Dialog Icon Picker Light")
@Composable
private fun CategoryDialogIconPickerPreview() {
    gestoreSpeseTheme(darkTheme = false, dynamicColor = false) {
        CategoryDialogIconPicker(
            selectedIcon = "🍔",
            imageUri = null,
            onPickImage = {},
            onRemoveImage = {},
            onIconSelect = {},
        )
    }
}

@Preview(showBackground = true, name = "Dialog Name Field Light")
@Composable
private fun CategoryDialogNameFieldPreview() {
    gestoreSpeseTheme(darkTheme = false, dynamicColor = false) {
        CategoryDialogNameField(value = "", isError = true, errorMessage = "Nome obbligatorio", onValueChange = {})
    }
}

@Preview(showBackground = true, name = "Dialog Icon Grid Light")
@Composable
private fun CategoryIconGridPreview() {
    gestoreSpeseTheme(darkTheme = false, dynamicColor = false) {
        CategoryIconGrid(
            availableIcons = availableIcons.take(12),
            selectedIcon = "🍔",
            selectedImageUri = null,
            onIconSelect = {},
        )
    }
}

@Preview(showBackground = true, name = "Category Dialog Light")
@Composable
private fun CategoryDialogPreview() {
    gestoreSpeseTheme(darkTheme = false, dynamicColor = false) {
        CategoryDialog(
            type = TransactionType.EXPENSE,
            existingCategories = emptyList(),
            onDismiss = {},
            onConfirm = { _, _, _ -> },
        )
    }
}

@Preview(showBackground = true, name = "Category Dialog Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoryDialogPreviewDark() {
    gestoreSpeseTheme(darkTheme = true, dynamicColor = false) {
        CategoryDialog(
            type = TransactionType.EXPENSE,
            existingCategories = emptyList(),
            onDismiss = {},
            onConfirm = { _, _, _ -> },
        )
    }
}
