package dev.inmo.wishlist.features.deeplinks.common.repo

import dev.inmo.micro_utils.repos.set
import dev.inmo.micro_utils.repos.unset
import dev.inmo.wishlist.features.deeplinks.common.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.jetbrains.exposed.v1.jdbc.Database
import java.sql.DriverManager
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*

/** Actual JDBC regression proof, including required independent PostgreSQL connections. */
class ExposedDeepLinksRepoTest {
    /** Small polymorphic value used for semantic JSON equivalence and controlled read barriers.
     * @param purpose Purpose discriminator checked by semantic comparison.
     * @param number Payload value checked independently from the handler.
     */
    @Serializable
    data class Payload(val purpose: String, val number: Int)

    /** Aggregated serializer with a read interception confined to the test decoder. */
    private fun json(afterDecode: () -> Unit = {}): Json = Json {
        useArrayPolymorphism = true
        serializersModule = SerializersModule {
            polymorphic(Any::class) {
                subclass(Payload::class, object : KSerializer<Payload> {
                    /** Preserves the production-generated payload descriptor. */
                    override val descriptor = Payload.serializer().descriptor
                    /** Keeps encoding unchanged while interception remains read-only. */
                    override fun serialize(encoder: Encoder, value: Payload) = Payload.serializer().serialize(encoder, value)
                    /** Releases the test barrier only after complete semantic decoding. */
                    override fun deserialize(decoder: Decoder): Payload = Payload.serializer().deserialize(decoder).also { afterDecode() }
                })
            }
        }
    }

    /** Expected record whose purpose and payload must both match persistent storage. */
    private val info = DeepLinkHandlerInfo(DeepLinkHandlerId("test.handler"), Payload("password", 1))

    /** SQLite verifies mapping, malformed/ambiguous preservation, raw equivalence, and bounded keyset pages. */
    @Test
    fun sqliteMappingConditionalConsumptionAndRawPaging() = runBlocking {
        // A file database retains rows across Exposed's independent transaction connections.
        val file = kotlin.io.path.createTempFile("wishlist-links-", ".db").toFile()
        try {
            val url = "jdbc:sqlite:${file.absolutePath}"
            val repo = ExposedDeepLinksRepo(Database.connect(url, driver = "org.sqlite.JDBC"), json())
            val id = DeepLinkId("one")
            val removals = mutableListOf<DeepLinkId>()
            val collector = launch(Dispatchers.Unconfined) { repo.onValueRemoved.collect { removals += it } }
            try {
                repo.set(id, info)
                assertEquals(info, repo.get(id))
                assertFalse(repo.consumeIfEquals(id, info.copy(handlerId = DeepLinkHandlerId("other"))))
                assertFalse(repo.consumeIfEquals(id, info.copy(value = Payload("different", 1))))
                assertTrue(repo.consumeIfEquals(id, info))
                assertFalse(repo.consumeIfEquals(id, info))
                assertEquals(listOf(id), removals)
                repo.set(id, info)
                repo.unset(id)
                assertNull(repo.get(id))
                insert(url, "malformed", "{broken")
                assertFalse(repo.consumeIfEquals(DeepLinkId("malformed"), info))
                val encoded = json().encodeToString(DeepLinkHandlerInfo.serializer(), info)
                insert(url, "duplicate", encoded)
                insert(url, "duplicate", encoded + " ")
                assertFalse(repo.consumeIfEquals(DeepLinkId("duplicate"), info))
                insert(url, "equivalent", encoded.replace("{", "{ ").replace(",", ", "))
                assertTrue(repo.consumeIfEquals(DeepLinkId("equivalent"), info))
                repeat(205) { insert(url, "page-%03d".format(it), "opaque-$it") }
                val bound = assertNotNull(repo.scanUpperBound())
                insert(url, "zz-after-bound", "opaque")
                assertFailsWith<IllegalArgumentException> { repo.scanPage(null, bound, 0) }
                assertFailsWith<IllegalArgumentException> { repo.scanPage(null, bound, 101) }
                val seen = mutableListOf<DeepLinkStorageCursor>()
                var cursor: DeepLinkStorageCursor? = null
                while (true) {
                    val page = repo.scanPage(cursor, bound, 100)
                    if (page.records.isEmpty()) break
                    assertTrue(page.records.size <= 100)
                    seen += page.records.map { it.cursor }
                    cursor = page.nextCursor
                    DriverManager.getConnection(url).use { c -> c.prepareStatement("DELETE FROM deeplinks WHERE deeplink_id=? AND handler_info_json=?").use {
                        it.setString(1, cursor!!.id.string); it.setString(2, cursor!!.rawInfo); it.executeUpdate()
                    } }
                }
                assertEquals(208, seen.size)
                assertEquals(2, seen.count { it.id.string == "duplicate" })
                assertTrue(seen.none { it.id.string == "zz-after-bound" })
                assertEquals(seen.size, seen.distinct().size)
            } finally { collector.cancelAndJoin() }
        } finally { file.delete() }
    }

    /** Two overlapping PostgreSQL reads yield one committed winner; replacements and failed commits deny authorization. */
    @Test
    fun requiredPostgresIndependentConnectionsSelectOneWinnerAndPreserveReplacement() = runBlocking {
        val base = requireNotNull(System.getenv("WISHLIST_TEST_POSTGRES_URL")) { "Required isolated PostgreSQL integration database is unavailable" }
        val schema = "links_test_" + UUID.randomUUID().toString().replace("-", "")
        DriverManager.getConnection(base).use { it.createStatement().use { s -> s.execute("CREATE SCHEMA $schema") } }
        val url = base + (if ('?' in base) "&" else "?") + "currentSchema=$schema"
        try {
            val entered = CountDownLatch(2)
            val release = CountDownLatch(1)
            val barrierJson = json {
                entered.countDown()
                check(release.await(10, TimeUnit.SECONDS)) { "Bounded concurrent read barrier timed out" }
            }
            val first = ExposedDeepLinksRepo(Database.connect(url, driver = "org.postgresql.Driver"), barrierJson)
            val second = ExposedDeepLinksRepo(Database.connect(url, driver = "org.postgresql.Driver"), barrierJson)
            val id = DeepLinkId("same")
            first.set(id, info)
            val attempts = listOf(first, second).map { repo -> async(Dispatchers.IO) { repo.consumeIfEquals(id, info) } }
            check(withContext(Dispatchers.IO) { entered.await(10, TimeUnit.SECONDS) })
            release.countDown()
            assertEquals(1, withTimeout(15_000) { attempts.awaitAll() }.count { it })
            assertNull(first.get(id))

            val read = CountDownLatch(1)
            val deleteRelease = CountDownLatch(1)
            val replacementRepo = ExposedDeepLinksRepo(Database.connect(url, driver = "org.postgresql.Driver"), json {
                read.countDown(); check(deleteRelease.await(10, TimeUnit.SECONDS))
            })
            first.set(id, info)
            val replacing = async(Dispatchers.IO) { replacementRepo.consumeIfEquals(id, info) }
            check(withContext(Dispatchers.IO) { read.await(10, TimeUnit.SECONDS) })
            val replacement = info.copy(value = Payload("replacement", 2))
            second.set(id, replacement)
            deleteRelease.countDown()
            assertFalse(withTimeout(15_000) { replacing.await() })
            assertEquals(replacement, second.get(id))

            DriverManager.getConnection(url).use { c -> c.createStatement().use { s ->
                s.execute("CREATE FUNCTION fail_delete() RETURNS trigger LANGUAGE plpgsql AS 'BEGIN RAISE EXCEPTION ''test deletion failure''; END'")
                s.execute("CREATE TRIGGER deny_delete BEFORE DELETE ON deeplinks FOR EACH ROW EXECUTE FUNCTION fail_delete()")
            } }
            assertFails { second.consumeIfEquals(id, replacement) }
            assertEquals(replacement, second.get(id))
            DriverManager.getConnection(url).use { c -> c.createStatement().use { it.execute("DROP TRIGGER deny_delete ON deeplinks") } }
            // A deferred trigger fails at commit, after DELETE reported one affected row.
            DriverManager.getConnection(url).use { c -> c.createStatement().use { s ->
                s.execute("CREATE CONSTRAINT TRIGGER deny_commit AFTER DELETE ON deeplinks DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION fail_delete()")
            } }
            assertFails { second.consumeIfEquals(id, replacement) }
            assertEquals(replacement, second.get(id))
            DriverManager.getConnection(url).use { c -> c.createStatement().use { it.execute("DROP TRIGGER deny_commit ON deeplinks") } }
            assertTrue(second.consumeIfEquals(id, replacement))
        } finally {
            DriverManager.getConnection(base).use { it.createStatement().use { s -> s.execute("DROP SCHEMA $schema CASCADE") } }
        }
    }

    /** Inserts opaque raw rows directly to exercise malformed and alternate serialization representations. */
    private fun insert(url: String, id: String, raw: String) {
        DriverManager.getConnection(url).use { c -> c.prepareStatement("INSERT INTO deeplinks(deeplink_id,handler_info_json) VALUES (?,?)").use {
            it.setString(1, id); it.setString(2, raw); it.executeUpdate()
        } }
    }
}
