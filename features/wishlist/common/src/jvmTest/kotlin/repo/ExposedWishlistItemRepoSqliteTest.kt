package dev.inmo.wishlist.features.wishlist.common.repo

import dev.inmo.micro_utils.repos.create
import dev.inmo.wishlist.features.common.common.models.Amount
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.nio.file.Files
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExposedWishlistItemRepoSqliteTest {
    @Test
    fun newPricesKeepUnsignedPartsSignAndScaleAcrossWritesAndReopen() = runBlocking {
        val file = Files.createTempFile("wishlist-prices", ".sqlite")
        val url = "jdbc:sqlite:${file.toAbsolutePath()}"
        try {
            val database = Database.connect(url = url, driver = "org.sqlite.JDBC")
            val small = Amount(0uL, 1uL, true, 4)
            val large = Amount(ULong.MAX_VALUE, ULong.MAX_VALUE, false, 20)
            val repo = ExposedWishlistItemRepo(database)
            val smallItem = repo.create(NewWishlistItem(WishlistId(1), "small", approximatePrice = small)).single()
            val largeItem = repo.create(NewWishlistItem(WishlistId(1), "large", approximatePrice = large)).single()
            val emptyItem = repo.create(NewWishlistItem(WishlistId(1), "empty")).single()
            assertEquals(small, repo.getById(smallItem.id)?.approximatePrice)
            assertEquals(large, repo.getById(largeItem.id)?.approximatePrice)
            assertNull(repo.getById(emptyItem.id)?.approximatePrice)

            val updated = Amount(0uL, 5uL, false, 3)
            repo.update(smallItem.id, NewWishlistItem(WishlistId(1), "small", approximatePrice = updated))
            TransactionManager.closeAndUnregister(database)

            val reopened = Database.connect(url = url, driver = "org.sqlite.JDBC")
            try {
                val restored = ExposedWishlistItemRepo(reopened)
                assertEquals(updated, restored.getById(smallItem.id)?.approximatePrice)
                assertEquals(large, restored.getById(largeItem.id)?.approximatePrice)
                assertNull(restored.getById(emptyItem.id)?.approximatePrice)
                DriverManager.getConnection(url).use { connection ->
                    connection.prepareStatement("SELECT approx_price_int, approx_price_dec, approx_price_negative, approx_price_decimal_places FROM wishlist_items WHERE id = ?").use { statement ->
                        statement.setLong(1, largeItem.id.long)
                        statement.executeQuery().use { row ->
                            assertEquals(true, row.next())
                            assertEquals(-1L, row.getLong(1))
                            assertEquals(-1L, row.getLong(2))
                            assertEquals(false, row.getBoolean(3))
                            assertEquals(20, row.getInt(4))
                        }
                    }
                }
            } finally {
                TransactionManager.closeAndUnregister(reopened)
            }
        } finally {
            Files.deleteIfExists(file)
        }
    }

    @Test
    fun legacyRowsSurviveAdditiveSchemaMigration() = runBlocking {
        val file = Files.createTempFile("wishlist-prices-legacy", ".sqlite")
        val url = "jdbc:sqlite:${file.toAbsolutePath()}"
        try {
            DriverManager.getConnection(url).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("CREATE TABLE wishlist_items (id INTEGER PRIMARY KEY AUTOINCREMENT, wishlist_id BIGINT NOT NULL, title TEXT NOT NULL, amount INT NOT NULL DEFAULT 1, approx_price_int BIGINT, approx_price_dec BIGINT, price_units TEXT NOT NULL, description TEXT NOT NULL, priority_weight BIGINT NOT NULL DEFAULT 50)")
                    statement.execute("INSERT INTO wishlist_items (id, wishlist_id, title, approx_price_int, approx_price_dec, price_units, description) VALUES (1, 1, 'legacy-negative', -12, 5, '', '')")
                    statement.execute("INSERT INTO wishlist_items (id, wishlist_id, title, approx_price_int, approx_price_dec, price_units, description) VALUES (2, 1, 'legacy-min', -9223372036854775808, 0, '', '')")
                    statement.execute("INSERT INTO wishlist_items (id, wishlist_id, title, price_units, description) VALUES (3, 1, 'legacy-empty', '', '')")
                }
            }
            val database = Database.connect(url = url, driver = "org.sqlite.JDBC")
            try {
                val repo = ExposedWishlistItemRepo(database)
                assertEquals(Amount(12uL, 5uL, true), repo.getById(WishlistItemId(1))?.approximatePrice)
                assertEquals(Amount(1uL shl 63, 0uL, true), repo.getById(WishlistItemId(2))?.approximatePrice)
                assertNull(repo.getById(WishlistItemId(3))?.approximatePrice)
                DriverManager.getConnection(url).use { connection ->
                    connection.createStatement().use { statement ->
                        statement.executeQuery("SELECT approx_price_int, approx_price_dec, approx_price_negative, approx_price_decimal_places FROM wishlist_items WHERE id = 1").use { row ->
                            assertEquals(true, row.next())
                            assertEquals(-12L, row.getLong(1))
                            assertEquals(5L, row.getLong(2))
                            assertNull(row.getObject(3))
                            assertNull(row.getObject(4))
                        }
                    }
                }
            } finally {
                TransactionManager.closeAndUnregister(database)
            }
        } finally {
            Files.deleteIfExists(file)
        }
    }
}
