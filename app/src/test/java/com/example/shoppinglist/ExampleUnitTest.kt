package com.example.shoppinglist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private val sampleItems = listOf(
        ShoppingItem(1, "Помидоры", "Овощи", 1, "кг", 180, false),
        ShoppingItem(2, "Молоко 2,5%", "Молочное", 1, "л", 110, true),
        ShoppingItem(3, "Сыр гауда", "Молочное", 1, "шт.", 310, true),
        ShoppingItem(4, "Куриное филе", "Мясо", 1, "кг", 400, false)
    )

    @Test
    fun calculatesTotalAndInCartAmountsCorrectly() {
        assertEquals(1000, ShoppingCalculator.totalAmount(sampleItems))
        assertEquals(420, ShoppingCalculator.inCartAmount(sampleItems))
    }

    @Test
    fun formatsRublesWithSpaces() {
        assertEquals("1 250 ₽", ShoppingCalculator.formatRubles(1250))
        assertEquals("420 ₽", ShoppingCalculator.formatRubles(420))
    }

    @Test
    fun formatsRussianItemCountPlurals() {
        assertEquals("1 товар", ShoppingCalculator.formatItemCount(1))
        assertEquals("2 товара", ShoppingCalculator.formatItemCount(2))
        assertEquals("5 товаров", ShoppingCalculator.formatItemCount(5))
    }

    @Test
    fun buildsShareTextWithTotal() {
        val text = ShoppingCalculator.buildShareText(sampleItems)
        assertTrue(text.contains("СПИСОК ПОКУПОК"))
        assertTrue(text.contains("Итого: 1 000 ₽"))
    }
}