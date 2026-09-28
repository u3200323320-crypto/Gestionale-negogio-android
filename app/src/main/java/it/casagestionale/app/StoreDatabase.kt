package it.casagestionale.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Product(
    val id: Long,
    val name: String,
    val category: String,
    val barcode: String,
    val stock: Int,
    val minStock: Int,
    val cost: Double,
    val price: Double,
)

data class Sale(
    val id: Long,
    val total: Double,
    val payment: String,
    val date: Long,
    val items: Int,
)

data class DashboardStats(
    val todayTotal: Double,
    val todayCount: Int,
    val productCount: Int,
    val lowStockCount: Int,
    val stockValue: Double,
    val weekTotal: Double,
    val previousWeekTotal: Double,
)

class StoreDatabase(context: Context) : SQLiteOpenHelper(context, "casa_gestionale.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE products (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            category TEXT NOT NULL DEFAULT '',
            barcode TEXT NOT NULL DEFAULT '',
            stock INTEGER NOT NULL DEFAULT 0,
            min_stock INTEGER NOT NULL DEFAULT 3,
            cost REAL NOT NULL DEFAULT 0,
            price REAL NOT NULL DEFAULT 0
        )""")
        db.execSQL("CREATE UNIQUE INDEX product_barcode ON products(barcode) WHERE barcode != ''")
        db.execSQL("""CREATE TABLE sales (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            total REAL NOT NULL,
            payment TEXT NOT NULL,
            date INTEGER NOT NULL
        )""")
        db.execSQL("""CREATE TABLE sale_items (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            sale_id INTEGER NOT NULL REFERENCES sales(id),
            product_id INTEGER NOT NULL REFERENCES products(id),
            name TEXT NOT NULL,
            quantity INTEGER NOT NULL,
            unit_price REAL NOT NULL
        )""")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun products(query: String = ""): List<Product> {
        val db = readableDatabase
        val normalized = query.trim()
        val cursor = if (normalized.isEmpty()) {
            db.rawQuery("SELECT * FROM products ORDER BY name COLLATE NOCASE", null)
        } else {
            db.rawQuery(
                "SELECT * FROM products WHERE name LIKE ? OR category LIKE ? OR barcode LIKE ? ORDER BY name COLLATE NOCASE",
                arrayOf("%$normalized%", "%$normalized%", "%$normalized%"),
            )
        }
        return cursor.use { result ->
            buildList {
                while (result.moveToNext()) {
                    add(
                        Product(
                            result.getLong(0), result.getString(1), result.getString(2), result.getString(3),
                            result.getInt(4), result.getInt(5), result.getDouble(6), result.getDouble(7),
                        ),
                    )
                }
            }
        }
    }

    fun saveProduct(product: Product) {
        val values = ContentValues().apply {
            put("name", product.name.trim())
            put("category", product.category.trim())
            put("barcode", product.barcode.trim())
            put("stock", product.stock)
            put("min_stock", product.minStock)
            put("cost", product.cost)
            put("price", product.price)
        }
        if (product.id == 0L) writableDatabase.insertOrThrow("products", null, values)
        else writableDatabase.update("products", values, "id = ?", arrayOf(product.id.toString()))
    }

    fun productByBarcode(barcode: String): Product? = products(barcode).firstOrNull { it.barcode == barcode }

    fun completeSale(cart: Map<Product, Int>, payment: String) {
        require(cart.isNotEmpty())
        val db = writableDatabase
        db.beginTransaction()
        try {
            val total = cart.entries.sumOf { (product, quantity) -> product.price * quantity }
            val saleValues = ContentValues().apply {
                put("total", total)
                put("payment", payment)
                put("date", System.currentTimeMillis())
            }
            val saleId = db.insertOrThrow("sales", null, saleValues)
            cart.forEach { (product, quantity) ->
                val itemValues = ContentValues().apply {
                    put("sale_id", saleId)
                    put("product_id", product.id)
                    put("name", product.name)
                    put("quantity", quantity)
                    put("unit_price", product.price)
                }
                db.insertOrThrow("sale_items", null, itemValues)
                val changed = db.compileStatement(
                    "UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?",
                ).use { statement ->
                    statement.bindLong(1, quantity.toLong())
                    statement.bindLong(2, product.id)
                    statement.bindLong(3, quantity.toLong())
                    statement.executeUpdateDelete()
                }
                check(changed == 1) { "Scorte insufficienti per ${product.name}" }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun sales(): List<Sale> {
        val cursor = readableDatabase.rawQuery(
            """SELECT s.id, s.total, s.payment, s.date,
                COALESCE(SUM(si.quantity), 0) FROM sales s
                LEFT JOIN sale_items si ON si.sale_id = s.id
                GROUP BY s.id ORDER BY s.date DESC""",
            null,
        )
        return cursor.use { result ->
            buildList {
                while (result.moveToNext()) {
                    add(Sale(result.getLong(0), result.getDouble(1), result.getString(2), result.getLong(3), result.getInt(4)))
                }
            }
        }
    }

    fun stats(): DashboardStats {
        val now = System.currentTimeMillis()
        val today = java.util.Calendar.getInstance().apply {
            timeInMillis = now
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val week = today - 6L * 24 * 60 * 60 * 1000
        val previousWeek = week - 7L * 24 * 60 * 60 * 1000
        val db = readableDatabase
        fun scalar(sql: String, args: Array<String>? = null): Double =
            db.rawQuery(sql, args).use { if (it.moveToFirst()) it.getDouble(0) else 0.0 }

        return DashboardStats(
            scalar("SELECT COALESCE(SUM(total), 0) FROM sales WHERE date >= ?", arrayOf(today.toString())),
            scalar("SELECT COUNT(*) FROM sales WHERE date >= ?", arrayOf(today.toString())).toInt(),
            scalar("SELECT COUNT(*) FROM products").toInt(),
            scalar("SELECT COUNT(*) FROM products WHERE stock <= min_stock").toInt(),
            scalar("SELECT COALESCE(SUM(stock * cost), 0) FROM products"),
            scalar("SELECT COALESCE(SUM(total), 0) FROM sales WHERE date >= ?", arrayOf(week.toString())),
            scalar(
                "SELECT COALESCE(SUM(total), 0) FROM sales WHERE date >= ? AND date < ?",
                arrayOf(previousWeek.toString(), week.toString()),
            ),
        )
    }
}