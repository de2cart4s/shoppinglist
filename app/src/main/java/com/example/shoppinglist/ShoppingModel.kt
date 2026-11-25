package com.example.shoppinglist

object ShoppingDefaults {
    val categories = listOf("Овощи", "Молочное", "Мясо", "Бытовое")
    val units = listOf("шт.", "кг", "г", "л", "уп.")
}

data class ShoppingItem(
    val id: Long = 0,
    val name: String,
    val category: String,
    val quantity: Int = 1,
    val unit: String = "шт.",
    val price: Int,
    val isChecked: Boolean = false
)

object ShoppingCalculator {
    fun totalAmount(items: List<ShoppingItem>): Int =
        items.sumOf { it.price }

    fun inCartAmount(items: List<ShoppingItem>): Int =
        items.filter { it.isChecked }.sumOf { it.price }

    fun formatRubles(amount: Int): String {
        val str = amount.toString()
        val formatted = str.reversed().chunked(3).joinToString(" ").reversed()
        return "$formatted ₽"
    }

    fun formatItemCount(count: Int): String {
        val mod100 = count % 100
        val mod10 = count % 10
        val word = when {
            mod100 in 11..19 -> "товаров"
            mod10 == 1 -> "товар"
            mod10 in 2..4 -> "товара"
            else -> "товаров"
        }
        return "$count $word"
    }

    fun buildShareText(items: List<ShoppingItem>): String {
        if (items.isEmpty()) return "Список покупок пуст"
        val sb = StringBuilder("СПИСОК ПОКУПОК (${formatItemCount(items.size)})\n\n")
        val grouped = items.groupBy { it.category }
        grouped.forEach { (category, catItems) ->
            sb.append("$category:\n")
            catItems.forEach { item ->
                val mark = if (item.isChecked) "[x]" else "[ ]"
                sb.append("$mark ${item.name} · ${item.quantity} ${item.unit} — ${formatRubles(item.price)}\n")
            }
            sb.append("\n")
        }
        sb.append("Итого: ${formatRubles(totalAmount(items))}")
        return sb.toString()
    }
}