package com.example.shoppinglist

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ShoppingDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "shopping_list.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                quantity INTEGER NOT NULL,
                unit TEXT NOT NULL,
                price INTEGER NOT NULL,
                is_checked INTEGER NOT NULL
            )
            """.trimIndent()
        )
        createCategoriesTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            createCategoriesTable(db)
        }
    }

    private fun createCategoriesTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL
            )
            """.trimIndent()
        )
        ShoppingDefaults.categories.forEach { category ->
            val values = ContentValues().apply { put("name", category) }
            db.insertWithOnConflict("categories", null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    fun getAllCategories(): List<String> {
        val result = mutableListOf<String>()
        readableDatabase.rawQuery("SELECT name FROM categories ORDER BY id ASC", null).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(cursor.getString(0))
            }
        }
        return result.ifEmpty { ShoppingDefaults.categories }
    }

    fun insertCategory(name: String) {
        val values = ContentValues().apply { put("name", name.trim()) }
        writableDatabase.insertWithOnConflict("categories", null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun getAllItems(): List<ShoppingItem> {
        val result = mutableListOf<ShoppingItem>()
        readableDatabase.rawQuery("SELECT * FROM items ORDER BY id ASC", null).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    ShoppingItem(
                        id = cursor.getLong(0),
                        name = cursor.getString(1),
                        category = cursor.getString(2),
                        quantity = cursor.getInt(3),
                        unit = cursor.getString(4),
                        price = cursor.getInt(5),
                        isChecked = cursor.getInt(6) == 1
                    )
                )
            }
        }
        return result
    }

    fun insertItem(item: ShoppingItem): Long {
        val values = ContentValues().apply {
            put("name", item.name)
            put("category", item.category)
            put("quantity", item.quantity)
            put("unit", item.unit)
            put("price", item.price)
            put("is_checked", if (item.isChecked) 1 else 0)
        }
        return writableDatabase.insert("items", null, values)
    }

    fun updateItem(item: ShoppingItem) {
        val values = ContentValues().apply {
            put("name", item.name)
            put("category", item.category)
            put("quantity", item.quantity)
            put("unit", item.unit)
            put("price", item.price)
            put("is_checked", if (item.isChecked) 1 else 0)
        }
        writableDatabase.update("items", values, "id = ?", arrayOf(item.id.toString()))
    }

    fun updateChecked(id: Long, isChecked: Boolean) {
        val values = ContentValues().apply {
            put("is_checked", if (isChecked) 1 else 0)
        }
        writableDatabase.update("items", values, "id = ?", arrayOf(id.toString()))
    }

    fun deleteItem(id: Long) {
        writableDatabase.delete("items", "id = ?", arrayOf(id.toString()))
    }

    fun deleteCheckedItems() {
        writableDatabase.delete("items", "is_checked = 1", null)
    }
}