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
import dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
import dev.inmo.wishlist.features.wishlist.server.services.WishlistService
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
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
import kotlin.test.Test
import kotlin.test.assertEquals

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

    private fun ApplicationTestBuilder.installAdminRoutes(users: UsersRepo) {
        val wishlists = FakeWishlistRepo()
        val wishlistItems = FakeWishlistItemRepo()
        val management = UsersManagementFeature(
            usersRepo = users,
            authService = AuthFeatureService(users, users, FakePasswordsRepo()),
            wishlistRepo = wishlists,
            wishlistItemRepo = wishlistItems,
            accountCoordinator = EmailVerificationAccountCoordinator(users, NoopRolesRepo),
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
        override suspend fun update(id: UserId, value: NewUser): RegisteredUser? {
            throw DuplicateUserFieldException()
        }
    }
}
