package com.example.shoppinglist

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ShoppingUiState(
    val items: List<ShoppingItem> = emptyList(),
    val categories: List<String> = ShoppingDefaults.categories,
    val selectedCategory: String? = null, // null = "Все"
    val isDarkTheme: Boolean = false
) {
    val filteredItems: List<ShoppingItem>
        get() = if (selectedCategory == null) items else items.filter { it.category == selectedCategory }

    val totalAmount: Int get() = ShoppingCalculator.totalAmount(items)
    val inCartAmount: Int get() = ShoppingCalculator.inCartAmount(items)
    val checkedCount: Int get() = items.count { it.isChecked }
    val hasCheckedItems: Boolean get() = checkedCount > 0
}

class ShoppingViewModel(application: Application) : AndroidViewModel(application) {
    private val db = ShoppingDatabaseHelper(application)
    private val prefs = application.getSharedPreferences("shopping_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        ShoppingUiState(isDarkTheme = prefs.getBoolean("dark_theme", false))
    )
    val uiState: StateFlow<ShoppingUiState> = _uiState.asStateFlow()

    private var lastDeletedItem: ShoppingItem? = null

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val loadedItems = withContext(Dispatchers.IO) { db.getAllItems() }
            val loadedCategories = withContext(Dispatchers.IO) { db.getAllCategories() }
            _uiState.update {
                it.copy(items = loadedItems, categories = loadedCategories)
            }
        }
    }

    fun selectCategory(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun addCategory(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { db.insertCategory(clean) }
            loadData()
        }
    }

    fun toggleTheme() {
        val newDark = !_uiState.value.isDarkTheme
        prefs.edit().putBoolean("dark_theme", newDark).apply()
        _uiState.update { it.copy(isDarkTheme = newDark) }
    }

    fun addItem(name: String, category: String, quantity: Int, unit: String, price: Int) {
        viewModelScope.launch {
            val item = ShoppingItem(
                name = name.trim(),
                category = category,
                quantity = quantity,
                unit = unit,
                price = price
            )
            withContext(Dispatchers.IO) { db.insertItem(item) }
            loadData()
        }
    }

    fun updateItem(item: ShoppingItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { db.updateItem(item.copy(name = item.name.trim())) }
            loadData()
        }
    }

    fun toggleChecked(item: ShoppingItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { db.updateChecked(item.id, !item.isChecked) }
            loadData()
        }
    }

    fun deleteItem(item: ShoppingItem) {
        lastDeletedItem = item
        viewModelScope.launch {
            withContext(Dispatchers.IO) { db.deleteItem(item.id) }
            loadData()
        }
    }

    fun undoDelete() {
        val itemToRestore = lastDeletedItem ?: return
        lastDeletedItem = null
        viewModelScope.launch {
            withContext(Dispatchers.IO) { db.insertItem(itemToRestore) }
            loadData()
        }
    }

    fun clearCheckedItems() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { db.deleteCheckedItems() }
            loadData()
        }
    }
}