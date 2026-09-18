package dev.inmo.wishlist.features.admin.server.configurators

import dev.inmo.wishlist.features.admin.server.AdminFeature
import dev.inmo.wishlist.features.admin.server.FakePasswordsRepo
import dev.inmo.wishlist.features.admin.server.FakeUsersRepo
import dev.inmo.wishlist.features.admin.server.FakeWishlistItemRepo
import dev.inmo.wishlist.features.admin.server.FakeWishlistRepo
import dev.inmo.wishlist.features.admin.server.NoopRolesRepo
import dev.inmo.wishlist.features.admin.server.UsersManagementFeature
import dev.inmo.wishlist.features.auth.server.services.AuthFeatureService
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.services.EmailVerificationAccountCoordinator
import dev.inmo.wishlist.features.roles.common.models.FunctionalityId
import dev.inmo.wishlist.features.roles.server.RolesFeature
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.UsersRepo
import dev.inmo.wishlist.features.users.common.repo.ExposedUsersRepo
import dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
import dev.inmo.wishlist.features.users.common.repo.exceptions.EmailChangeCooldownException
import dev.inmo.wishlist.features.wishlist.server.services.WishlistService
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.bearer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Verifies authorization and username-only mutation semantics through the installed admin routes. */
class AdminRoutingsConfiguratorTest {
    private val rootId = UserId(1L)
    private val otherId = UserId(2L)
    private val user = RegisteredUser(UserId(17L), Username("alice"), Email("alice@example.com"), emailApproved = true)

    @Test
    fun usernameRouteRequiresRootAndPreservesEmailApproval() = testApplication {
        val users = FakeUsersRepo(mapOf(user.id to user))
        installAdminRoutes(users)

        listOf(null, "Bearer other").forEach { authorization ->
            val response = client.put("/api/admin/users/setUsername/${user.id.long}") {
                authorization?.let { header(HttpHeaders.Authorization, it) }
                contentType(ContentType.Application.Json)
                setBody("\"renamed\"")
            }
            assertEquals(
                if (authorization == null) HttpStatusCode.Unauthorized else HttpStatusCode.Forbidden,
                response.status,
            )
        }

        val response = client.put("/api/admin/users/setUsername/${user.id.long}") {
            header(HttpHeaders.Authorization, "Bearer root")
            contentType(ContentType.Application.Json)
            setBody("\"renamed\"")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(user.copy(username = Username("renamed")), users.getById(user.id))
    }

    @Test
    fun usernameRouteMapsMalformedMissingAndDuplicateWritesToExistingStatuses() = testApplication {
        val users = FakeUsersRepo(mapOf(user.id to user))
        installAdminRoutes(users)

        val malformedId = client.put("/api/admin/users/setUsername/not-a-number") {
            header(HttpHeaders.Authorization, "Bearer root")
            contentType(ContentType.Application.Json)
            setBody("\"renamed\"")
        }
        assertEquals(HttpStatusCode.BadRequest, malformedId.status)

        val malformedBody = client.put("/api/admin/users/setUsername/${user.id.long}") {
            header(HttpHeaders.Authorization, "Bearer root")
            contentType(ContentType.Application.Json)
            setBody("{")
        }
        assertEquals(HttpStatusCode.BadRequest, malformedBody.status)

        val absent = client.put("/api/admin/users/setUsername/999") {
            header(HttpHeaders.Authorization, "Bearer root")
            contentType(ContentType.Application.Json)
            setBody("\"renamed\"")
        }
        assertEquals(HttpStatusCode.NotFound, absent.status)
    }

    @Test
    fun usernameRouteMapsRepositoryDuplicateToConflictWithoutMutatingStoredUser() = testApplication {
        val backing = FakeUsersRepo(mapOf(user.id to user))
        installAdminRoutes(DuplicateOnUpdateUsersRepo(backing))

        val response = client.put("/api/admin/users/setUsername/${user.id.long}") {
            header(HttpHeaders.Authorization, "Bearer root")
            contentType(ContentType.Application.Json)
            setBody("\"duplicate\"")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals(user, backing.getById(user.id))
    }

    @Test
    fun fullUpdateMapsCooldownWithoutPartiallyRenamingUser() = testApplication {
        val backing = FakeUsersRepo(mapOf(user.id to user))
        installAdminRoutes(CooldownOnUpdateUsersRepo(backing))

        val response = client.put("/api/admin/users/update/${user.id.long}") {
            header(HttpHeaders.Authorization, "Bearer root")
            contentType(ContentType.Application.Json)
            setBody("{\"username\":\"renamed\",\"email\":\"replacement@example.com\"}")
        }

        assertEquals(HttpStatusCode.TooManyRequests, response.status)
        assertEquals("{\"emailChangeAllowedAt\":123456789}", response.bodyAsText())
        assertEquals(user, backing.getById(user.id))
    }

    /** The actual route preserves root-only deadline disclosure and real repository atomicity. */
    @Test
    fun realCooldownFullUpdateReturnsTyped429AndKeepsDeniedCallersPrivate() {
        val file = Files.createTempFile("wishlist-admin-routes", ".sqlite")
        val database = Database.connect(url = "jdbc:sqlite:${file.toAbsolutePath()}", driver = "org.sqlite.JDBC")
        try {
            var now = 1_000L
            val users = ExposedUsersRepo(database, nowMillis = { now })
            val addressA = Email("route-real-a@example.com")
            val addressB = Email("route-real-b@example.com")
            val duplicate = Email("route-real-duplicate@example.com")
            val target = runBlocking {
                users.create(listOf(NewUser(Username("route-real"), addressA))).single().also {
                    checkNotNull(users.approveEmail(it.id, addressA, cooldownMillis = 10L))
                }
            }
            runBlocking { users.create(listOf(NewUser(Username("route-duplicate"), duplicate))) }
            val approved = runBlocking { checkNotNull(users.getById(target.id)) }

            testApplication {
                installAdminRoutes(users, cooldownMillis = 10L)
                val updateBody = "{\"username\":\"must-not-rename\",\"email\":\"${addressB.string}\"}"
                val rootRejected = client.put("/api/admin/users/update/${target.id.long}") {
                    header(HttpHeaders.Authorization, "Bearer root")
                    contentType(ContentType.Application.Json)
                    setBody(updateBody)
                }
                assertEquals(HttpStatusCode.TooManyRequests, rootRejected.status)
                assertEquals("{\"emailChangeAllowedAt\":1010}", rootRejected.bodyAsText())
                assertEquals(approved, users.getById(target.id))

                listOf(null, "Bearer other").forEach { authorization ->
                    val denied = client.put("/api/admin/users/update/${target.id.long}") {
                        authorization?.let { header(HttpHeaders.Authorization, it) }
                        contentType(ContentType.Application.Json)
                        setBody(updateBody)
                    }
                    assertEquals(
                        if (authorization == null) HttpStatusCode.Unauthorized else HttpStatusCode.Forbidden,
                        denied.status,
                    )
                    assertFalse(denied.bodyAsText().contains("emailChangeAllowedAt"))
                }

                val rename = client.put("/api/admin/users/setUsername/${target.id.long}") {
                    header(HttpHeaders.Authorization, "Bearer root")
                    contentType(ContentType.Application.Json)
                    setBody("\"route-renamed\"")
                }
                assertEquals(HttpStatusCode.OK, rename.status)
                assertEquals(approved.copy(username = Username("route-renamed")), users.getById(target.id))

                now = 1_010L
                val replacement = client.put("/api/admin/users/update/${target.id.long}") {
                    header(HttpHeaders.Authorization, "Bearer root")
                    contentType(ContentType.Application.Json)
                    setBody("{\"username\":\"route-expired\",\"email\":\"${addressB.string}\"}")
                }
                assertEquals(HttpStatusCode.OK, replacement.status)
                assertEquals(
                    approved.copy(username = Username("route-expired")),
                    users.getById(target.id),
                )
                assertEquals(addressB, users.getEmailProfileFresh(target.id)?.pendingEmail)

                val duplicateResponse = client.put("/api/admin/users/update/${target.id.long}") {
                    header(HttpHeaders.Authorization, "Bearer root")
                    contentType(ContentType.Application.Json)
                    setBody("{\"username\":\"route-expired\",\"email\":\"${duplicate.string}\"}")
                }
                assertEquals(HttpStatusCode.Conflict, duplicateResponse.status)
            }
        } finally {
            try {
                TransactionManager.closeAndUnregister(database)
            } finally {
                Files.deleteIfExists(file)
            }
        }
    }

    private fun ApplicationTestBuilder.installAdminRoutes(users: UsersRepo, cooldownMillis: Long = 0L) {
        val wishlists = FakeWishlistRepo()
        val wishlistItems = FakeWishlistItemRepo()
        val management = UsersManagementFeature(
            usersRepo = users,
            authService = AuthFeatureService(users, users, FakePasswordsRepo()),
            wishlistRepo = wishlists,
            wishlistItemRepo = wishlistItems,
            accountCoordinator = EmailVerificationAccountCoordinator(users, NoopRolesRepo, cooldownMillis),
        )
        application {
            install(Authentication) {
                bearer {
                    authenticate { credential ->
                        when (credential.token) {
                            "root" -> rootId
                            "other" -> otherId
                            else -> null
                        }
                    }
                }
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            routing {
                route("/api") {
                    with(
                        AdminRoutingsConfigurator(
                            adminFeature = AdminFeature(management),
                            usersRepo = users,
                            wishlistService = WishlistService(wishlists),
                            wishlistRepo = wishlists,
                            wishlistItemRepo = wishlistItems,
                            rolesFeature = RootOnlyRolesFeature(rootId),
                        )
                    ) { invoke() }
                }
            }
        }
    }

    private class RootOnlyRolesFeature(private val rootId: UserId) : RolesFeature {
        override suspend fun isFunctionalityAvailable(userId: UserId, functionalityId: FunctionalityId): Boolean =
            userId == rootId
    }

    private class DuplicateOnUpdateUsersRepo(private val delegate: UsersRepo) : UsersRepo by delegate {
        override suspend fun updateUsername(id: UserId, username: Username): RegisteredUser? {
            throw DuplicateUserFieldException()
        }
    }

    /** Simulates the repository-owned durable deadline rejection at the full-update boundary. */
    private class CooldownOnUpdateUsersRepo(private val delegate: UsersRepo) : UsersRepo by delegate {
        override suspend fun update(id: UserId, value: NewUser): RegisteredUser? {
            throw EmailChangeCooldownException(123456789L)
        }
    }
}
