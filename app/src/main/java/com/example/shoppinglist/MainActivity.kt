package com.example.shoppinglist

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F5E9),
    onPrimaryContainer = Color(0xFF1B5E20),
    background = Color(0xFFF6F8F6),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1A),
    onSurfaceVariant = Color(0xFF6B7280),
    outlineVariant = Color(0xFFE5E7EB)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF0A2E0D),
    primaryContainer = Color(0xFF1B3B1E),
    onPrimaryContainer = Color(0xFFA5D6A7),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF9CA3AF),
    outlineVariant = Color(0xFF2E2E2E)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: ShoppingViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()

            MaterialTheme(
                colorScheme = if (state.isDarkTheme) DarkColors else LightColors
            ) {
                ShoppingApp(state = state, viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingApp(state: ShoppingUiState, viewModel: ShoppingViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showItemSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }
    var showShareSheet by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Покупки", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    if (state.hasCheckedItems) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Очистить купленное"
                            )
                        }
                    }
                    IconButton(
                        onClick = { showShareSheet = true },
                        enabled = state.items.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Поделиться"
                        )
                    }
                    IconButton(onClick = { viewModel.toggleTheme() }) {
                        Icon(
                            imageVector = if (state.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Сменить тему"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.items.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        editingItem = null
                        showItemSheet = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить товар")
                }
            }
        },
        bottomBar = {
            if (state.items.isNotEmpty()) {
                SummaryBottomBar(
                    inCartAmount = state.inCartAmount,
                    totalAmount = state.totalAmount
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.items.isEmpty()) {
                EmptyShoppingState(
                    onAddClick = {
                        editingItem = null
                        showItemSheet = true
                    }
                )
            } else {
                CategoryFilterRow(
                    categories = state.categories,
                    selectedCategory = state.selectedCategory,
                    onSelect = { viewModel.selectCategory(it) },
                    onAddCategoryClick = { showAddCategoryDialog = true }
                )
                ShoppingListContent(
                    categories = state.categories,
                    items = state.filteredItems,
                    onEdit = { item ->
                        editingItem = item
                        showItemSheet = true
                    },
                    onToggle = { viewModel.toggleChecked(it) },
                    onDelete = { item ->
                        viewModel.deleteItem(item)
                        scope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            val res = snackbarHostState.showSnackbar(
                                message = "Товар удалён",
                                actionLabel = "Отменить",
                                duration = SnackbarDuration.Short
                            )
                            if (res == SnackbarResult.ActionPerformed) {
                                viewModel.undoDelete()
                            }
                        }
                    }
                )
            }
        }
    }

    if (showItemSheet) {
        ItemBottomSheet(
            initialItem = editingItem,
            categories = state.categories,
            onDismiss = {
                showItemSheet = false
                editingItem = null
            },
            onAddCategory = { newCat -> viewModel.addCategory(newCat) },
            onSave = { name, category, qty, unit, price ->
                val current = editingItem
                if (current == null) {
                    viewModel.addItem(name, category, qty, unit, price)
                } else {
                    viewModel.updateItem(
                        current.copy(
                            name = name,
                            category = category,
                            quantity = qty,
                            unit = unit,
                            price = price
                        )
                    )
                }
                showItemSheet = false
                editingItem = null
            }
        )
    }

    if (showShareSheet) {
        ShareBottomSheet(
            items = state.items,
            onDismiss = { showShareSheet = false },
            onCopied = {
                showShareSheet = false
                scope.launch {
                    snackbarHostState.showSnackbar("Список скопирован в буфер обмена")
                }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Очистить купленное?") },
            text = {
                Text("Будет удалено: ${ShoppingCalculator.formatItemCount(state.checkedCount)}. Это действие нельзя отменить.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCheckedItems()
                        showClearDialog = false
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { newName ->
                viewModel.addCategory(newName)
                showAddCategoryDialog = false
            }
        )
    }
}

@Composable
fun CategoryFilterRow(
    categories: List<String>,
    selectedCategory: String?,
    onSelect: (String?) -> Unit,
    onAddCategoryClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedCategory == null,
            onClick = { onSelect(null) },
            label = { Text("Все") }
        )
        categories.forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onSelect(category) },
                label = { Text(category) }
            )
        }
        AssistChip(
            onClick = onAddCategoryClick,
            label = { Text("+ Новая") }
        )
    }
}

@Composable
fun ShoppingListContent(
    categories: List<String>,
    items: List<ShoppingItem>,
    onEdit: (ShoppingItem) -> Unit,
    onToggle: (ShoppingItem) -> Unit,
    onDelete: (ShoppingItem) -> Unit
) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "В этой категории пока нет товаров",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val grouped = items.groupBy { it.category }
    val orderedCategories = (categories + grouped.keys).distinct()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        orderedCategories.forEach { category ->
            val catItems = grouped[category] ?: return@forEach
            item(key = "header_$category") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = ShoppingCalculator.formatItemCount(catItems.size),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
            items(catItems, key = { it.id }) { item ->
                ShoppingItemCard(
                    item = item,
                    onEdit = { onEdit(item) },
                    onToggle = { onToggle(item) },
                    onDelete = { onDelete(item) }
                )
            }
        }
    }
}

@Composable
fun ShoppingItemCard(
    item: ShoppingItem,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val textColor = if (item.isChecked) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = { onToggle() }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = "${item.quantity} ${item.unit}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = ShoppingCalculator.formatRubles(item.price),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                modifier = Modifier.padding(end = 4.dp)
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SummaryBottomBar(inCartAmount: Int, totalAmount: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "В корзине: ${ShoppingCalculator.formatRubles(inCartAmount)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            Text(
                text = "Всего: ${ShoppingCalculator.formatRubles(totalAmount)}",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun EmptyShoppingState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.ShoppingBasket,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Список пока пуст",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Добавьте первый товар, чтобы контролировать расходы",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddClick,
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Добавить товар")
        }
    }
}

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая категория") },
        text = {
            OutlinedTextField(
                value = categoryName,
                onValueChange = {
                    categoryName = it
                    isError = false
                },
                label = { Text("Название") },
                placeholder = { Text("Например, Выпечка") },
                isError = isError,
                supportingText = if (isError) ({ Text("Введите название") }) else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (categoryName.isBlank()) {
                        isError = true
                    } else {
                        onConfirm(categoryName.trim())
                    }
                }
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemBottomSheet(
    initialItem: ShoppingItem?,
    categories: List<String>,
    onDismiss: () -> Unit,
    onAddCategory: (String) -> Unit,
    onSave: (String, String, Int, String, Int) -> Unit
) {
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var selectedCategory by remember {
        mutableStateOf(initialItem?.category ?: categories.firstOrNull() ?: "Овощи")
    }
    var quantity by remember { mutableIntStateOf(initialItem?.quantity ?: 1) }
    var selectedUnit by remember { mutableStateOf(initialItem?.unit ?: "шт.") }
    var priceText by remember { mutableStateOf(initialItem?.price?.toString() ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (initialItem == null) "Новый товар" else "Редактировать товар",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                label = { Text("Название товара") },
                placeholder = { Text("Например, Молоко 2,5%") },
                isError = nameError,
                supportingText = if (nameError) ({ Text("Введите название товара") }) else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Категория", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) }
                        )
                    }
                    AssistChip(
                        onClick = { showCategoryDialog = true },
                        label = { Text("+ Новая") }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Единица измерения", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShoppingDefaults.units.forEach { unit ->
                        FilterChip(
                            selected = selectedUnit == unit,
                            onClick = { selectedUnit = unit },
                            label = { Text(unit) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Количество", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { if (quantity > 1) quantity-- }) {
                                Icon(Icons.Default.Remove, contentDescription = "Уменьшить")
                            }
                            Text(text = quantity.toString(), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = { quantity++ }) {
                                Icon(Icons.Default.Add, contentDescription = "Увеличить")
                            }
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Цена, ₽", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = {
                            if (it.all { ch -> ch.isDigit() }) {
                                priceText = it
                                priceError = false
                            }
                        },
                        placeholder = { Text("110") },
                        isError = priceError,
                        supportingText = if (priceError) ({ Text("Укажите цену") }) else null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Button(
                onClick = {
                    val parsedPrice = priceText.toIntOrNull()
                    nameError = name.isBlank()
                    priceError = parsedPrice == null || parsedPrice < 0
                    if (!nameError && !priceError && parsedPrice != null) {
                        onSave(name, selectedCategory, quantity, selectedUnit, parsedPrice)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text(
                    text = if (initialItem == null) "Добавить в список" else "Сохранить изменения",
                    fontSize = 16.sp
                )
            }
        }
    }

    if (showCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showCategoryDialog = false },
            onConfirm = { newCat ->
                onAddCategory(newCat)
                selectedCategory = newCat
                showCategoryDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    items: List<ShoppingItem>,
    onDismiss: () -> Unit,
    onCopied: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val shareText = remember(items) { ShoppingCalculator.buildShareText(items) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Поделиться списком", fontSize = 20.sp, fontWeight = FontWeight.Bold)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = shareText,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString(shareText))
                    onCopied()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Скопировать текст")
            }

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Отправить список покупок"))
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Отправить в мессенджер")
            }
        }
    }
}