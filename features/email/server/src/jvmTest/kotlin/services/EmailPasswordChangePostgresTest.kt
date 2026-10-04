package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.auth.common.models.*
import dev.inmo.wishlist.features.auth.server.repo.ExposedPasswordsRepo
import dev.inmo.wishlist.features.auth.server.repo.PasswordsRepo
import dev.inmo.wishlist.features.auth.server.services.AuthFeatureService
import dev.inmo.wishlist.features.deeplinks.common.models.*
import dev.inmo.wishlist.features.deeplinks.common.repo.*
import dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService
import dev.inmo.wishlist.features.email.server.models.*
import kotlinx.coroutines.*
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

/** Shared-storage proof with two independent Email coordinators, Auth locks, and JDBC repositories. */
class EmailPasswordChangePostgresTest {
    /** Actual persisted password storage with a shared write counter; no cross-service authorization lock.
     * @param backing Actual independent JDBC password repository.
     * @param writes Shared count of committed password writes.
     */
    private class CountedPasswords(private val backing: PasswordsRepo, private val writes: AtomicInteger) : PasswordsRepo by backing {
        /** Counts successful writes after persistence returns. */
        override suspend fun set(toSet: Map<dev.inmo.wishlist.features.users.common.models.UserId, Password>) {
            backing.set(toSet)
            writes.incrementAndGet()
        }
    }

    /** Holds both final approval reads while retaining each repository's real conditional DELETE.
     * @param backing Actual independent JDBC approval store.
     * @param read Shared final-read arrival barrier.
     * @param release Shared bounded release barrier.
     */
    private class HeldLinks(private val backing: MaintainableDeepLinksRepo, private val read: CountDownLatch, private val release: CountDownLatch) : MaintainableDeepLinksRepo by backing {
        /** Read ordinal local to one completion service. */
        private var reads = 0
        /** Holds the second read after Auth has checked the original credential fingerprint. */
        override suspend fun get(k: DeepLinkId): DeepLinkHandlerInfo? {
            val value = backing.get(k)
            reads++
            if (reads == 2) {
                read.countDown()
                check(release.await(10, TimeUnit.SECONDS)) { "Final approval reads exceeded bounded deadline" }
            }
            return value
        }
    }

    /** Both final authorization callbacks observe the approval, but at most one can write; old sessions remain usable. */
    @Test
    fun independentServicesAllowOnePasswordWriteAndPreserveBothSessions() = runBlocking {
        val base = requireNotNull(System.getenv("WISHLIST_TEST_POSTGRES_URL")) { "Required isolated PostgreSQL integration database is unavailable" }
        val schema = "email_test_" + UUID.randomUUID().toString().replace("-", "")
        DriverManager.getConnection(base).use { it.createStatement().use { s -> s.execute("CREATE SCHEMA $schema") } }
        val url = base + (if ('?' in base) "&" else "?") + "currentSchema=$schema"
        val release = CountDownLatch(1)
        try {
            val json = Json {
                useArrayPolymorphism = true
                serializersModule = SerializersModule {
                    polymorphic(Any::class, EmailPasswordChangePayload::class, EmailPasswordChangePayload.serializer())
                }
            }
            val user = PasswordChangeTestFixtures.user
            val users = FakeUsersRepo(mapOf(user.id to user))
            val writes = AtomicInteger()
            val databases = List(2) { Database.connect(url, driver = "org.postgresql.Driver") }
            val passwords = databases.map { CountedPasswords(ExposedPasswordsRepo(it), writes) }
            val auth = passwords.map { AuthFeatureService(users, users, it, userRoleAuthorization = PasswordChangeRoleAuthorization()) }
            auth.first().setPassword(user.id, PasswordChangeTestFixtures.oldPassword)
            writes.set(0)
            val sessions = auth.map { assertNotNull(it.login(user.username, PasswordChangeTestFixtures.oldPassword)) }
            val initialState = assertNotNull(auth.first().passwordChangeState(user.id))
            val id = DeepLinkId("123e4567-e89b-42d3-a456-426614174000")
            val payload = EmailPasswordChangePayload(user.id, user.email!!, System.currentTimeMillis() + 60_000, initialState)
            val rawRepos = databases.map { ExposedDeepLinksRepo(it, json) }
            rawRepos.first().set(mapOf(id to DeepLinkHandlerInfo(EmailPasswordChange.handlerId, payload)))
            val reads = CountDownLatch(2)
            val services = rawRepos.mapIndexed { index, repo -> EmailPasswordChangeService(
                emailsService = null,
                deepLinksService = DeepLinksService(HeldLinks(repo, reads, release), emptyList()),
                accountCoordinator = EmailVerificationAccountCoordinator(users, FakeRolesRepo()),
                authFeatureService = auth[index],
                publicHttpOrigin = "https://wishlist.example",
            ) }
            val request = CompletePasswordChangeRequest(user.id, id, Password("new-password"))
            val attempts = services.map { service -> async(Dispatchers.IO) { service.completePasswordChange(request) } }
            check(withContext(Dispatchers.IO) { reads.await(10, TimeUnit.SECONDS) })
            release.countDown()
            val results = withTimeout(15_000) { attempts.awaitAll() }
            assertEquals(1, results.count { it == PasswordChangeResult.Changed })
            assertEquals(1, results.count { it == PasswordChangeResult.InvalidApproval })
            assertEquals(1, writes.get())
            assertNull(rawRepos.first().get(id))
            auth.forEachIndexed { index, service ->
                assertNotNull(service.getUser(sessions[index].token))
                assertNull(service.login(user.username, PasswordChangeTestFixtures.oldPassword))
                assertNotNull(service.login(user.username, Password("new-password")))
            }
        } finally {
            release.countDown()
            DriverManager.getConnection(base).use { it.createStatement().use { s -> s.execute("DROP SCHEMA $schema CASCADE") } }
        }
    }
}
