package dev.inmo.wishlist.features.admin.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.repos.deleteById
import dev.inmo.wishlist.features.admin.common.Constants
import dev.inmo.wishlist.features.admin.common.models.NewUserWithPassword
import dev.inmo.wishlist.features.admin.common.models.asAdminUser
import dev.inmo.wishlist.features.admin.common.models.asAdminWishlist
import dev.inmo.wishlist.features.admin.common.models.asAdminWishlistItem
import dev.inmo.wishlist.features.admin.server.AdminFeature
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.server.utils.getCallerUserIdOrAnswerUnauthorized
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.repo.ReadUsersRepo
import dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlist
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistInFeature
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import dev.inmo.wishlist.features.wishlist.common.repo.WishlistItemRepo
import dev.inmo.wishlist.features.wishlist.common.repo.WishlistRepo
import dev.inmo.wishlist.features.wishlist.server.services.WishlistService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

/**
 * Ktor routing configurator that registers all admin CRUD endpoints under `/admin`.
 *
 * All routes require a valid bearer token AND the authenticated caller must be the `root` user.
 * Non-root callers receive `403 Forbidden`.
 *
 * **Users management routes** (`/admin/users/...`):
 * - `GET    /admin/users/getAll`         — list all registered users
 * - `POST   /admin/users/create`         — create user with password; body: [NewUserWithPassword]; `409` on duplicate username
 * - `PUT    /admin/users/update/{id}`    — update user info; body: [NewUser]; `409` on duplicate username/email
 * - `DELETE /admin/users/delete/{id}`    — remove user by id
 *
 * **Wishlists management routes** (`/admin/wishlists/...`):
 * - `GET    /admin/wishlists/getByUserId/{userId}` — get wishlists owned by a specific user
 * - `GET    /admin/wishlists/getById/{id}`         — get a single wishlist by id
 * - `POST   /admin/wishlists/create`               — create wishlist for any user; body: [NewWishlist]
 * - `PUT    /admin/wishlists/update/{id}`           — update any wishlist (no ownership check); body: [NewWishlistInFeature]
 * - `DELETE /admin/wishlists/delete/{id}`           — delete any wishlist (no ownership check)
 *
 * Wishlist read operations delegate to [WishlistService] (existing functionality).
 * Wishlist write operations that bypass ownership delegate directly to [WishlistRepo] (existing functionality).
 *
 * A username/email colliding with an existing user surfaces as
 * [dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException] from
 * [adminFeature]'s `usersManagement.create`/`update` — caught here and translated to `409 Conflict`.
 */
class AdminRoutingsConfigurator(
    private val adminFeature: AdminFeature,
    private val usersRepo: ReadUsersRepo,
    private val wishlistService: WishlistService,
    private val wishlistRepo: WishlistRepo,
    private val wishlistItemRepo: WishlistItemRepo
) : ApplicationRoutingConfigurator.Element {

    private val rootUsername = "root"

    private suspend fun RoutingContext.requireAdmin(): UserId? {
        val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return null
        val user = usersRepo.getById(callerId)
        if (user == null || user.username.string != rootUsername) {
            call.respond(HttpStatusCode.Forbidden)
            return null
        }
        return callerId
    }

    override fun Route.invoke() {
        authenticate {
            route(Constants.adminPrefixPathPart) {
                route(Constants.usersPathPart) {
                    get(Constants.usersGetAllPathPart) {
                        requireAdmin() ?: return@get
                        call.respond(adminFeature.usersManagement.getAll())
                    }
                    get("${Constants.usersGetByIdPathPart}/{id}") {
                        requireAdmin() ?: return@get
                        val id = call.parameters["id"]?.toLongOrNull()?.let(::UserId) ?: run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@get
                        }
                        val user = usersRepo.getById(id) ?: run {
                            call.respond(HttpStatusCode.NotFound)
                            return@get
                        }
                        call.respond(user.asAdminUser())
                    }
                    post(Constants.usersCreatePathPart) {
                        requireAdmin() ?: return@post
                        val newUser = call.receive<NewUserWithPassword>()
                        val result = try {
                            adminFeature.usersManagement.create(newUser)
                        } catch (e: DuplicateUserFieldException) {
                            call.respond(HttpStatusCode.Conflict)
                            return@post
                        }
                        if (result == null) {
                            call.respond(HttpStatusCode.InternalServerError)
                        } else {
                            call.respond(result)
                        }
                    }
                    put("${Constants.usersUpdatePathPart}/{id}") {
                        requireAdmin() ?: return@put
                        val id = call.parameters["id"]?.toLongOrNull()?.let(::UserId) ?: run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@put
                        }
                        val newUser = call.receive<NewUser>()
                        val result = try {
                            adminFeature.usersManagement.update(id, newUser)
                        } catch (e: DuplicateUserFieldException) {
                            call.respond(HttpStatusCode.Conflict)
                            return@put
                        }
                        when (result) {
                            true -> call.respond(HttpStatusCode.OK)
                            false -> call.respond(HttpStatusCode.InternalServerError)
                            null -> call.respond(HttpStatusCode.NotFound)
                        }
                    }
                    put("${Constants.usersSetPasswordPathPart}/{id}") {
                        requireAdmin() ?: return@put
                        val id = call.parameters["id"]?.toLongOrNull()?.let(::UserId) ?: run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@put
                        }
                        val password = call.receive<Password>()
                        when (adminFeature.usersManagement.setPassword(id, password)) {
                            true -> call.respond(HttpStatusCode.OK)
                            false -> call.respond(HttpStatusCode.InternalServerError)
                            null -> call.respond(HttpStatusCode.NotFound)
                        }
                    }
                    delete("${Constants.usersDeletePathPart}/{id}") {
                        requireAdmin() ?: return@delete
                        val id = call.parameters["id"]?.toLongOrNull()?.let(::UserId) ?: run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@delete
                        }
                        when (adminFeature.usersManagement.delete(id)) {
                            true -> call.respond(HttpStatusCode.OK)
                            false -> call.respond(HttpStatusCode.InternalServerError)
                            null -> call.respond(HttpStatusCode.NotFound)
                        }
                    }
                }
                route(Constants.wishlistsPathPart) {
                    get(Constants.wishlistsGetAllPathPart) {
                        requireAdmin() ?: return@get
                        call.respond(wishlistRepo.getAll().values.map { it.asAdminWishlist() })
                    }
                    get("${Constants.wishlistsGetByUserIdPathPart}/{userId}") {
                        requireAdmin() ?: return@get
                        val userId = call.parameters["userId"]?.toLongOrNull()?.let(::UserId) ?: run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@get
                        }
                        call.respond(wishlistService.getByUserId(userId).map { it.asAdminWishlist() })
                    }
                    get("${Constants.wishlistsGetByIdPathPart}/{id}") {
                        requireAdmin() ?: return@get
                        val id = call.parameters["id"]?.toLongOrNull()?.let(::WishlistId) ?: run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@get
                        }
                        val wishlist = wishlistService.getById(id) ?: run {
                            call.respond(HttpStatusCode.NotFound)
                            return@get
                        }
                        call.respond(wishlist.asAdminWishlist())
                    }
                    post(Constants.wishlistsCreatePathPart) {
                        requireAdmin() ?: return@post
                        val newWishlist = call.receive<NewWishlist>()
                        val result = wishlistService.create(
                            NewWishlistInFeature(newWishlist.title),
                            newWishlist.userId
                        )
                        if (result == null) {
                            call.respond(HttpStatusCode.InternalServerError)
                        } else {
                            call.respond(result.asAdminWishlist())
                        }
                    }
                    put("${Constants.wishlistsUpdatePathPart}/{id}") {
                        requireAdmin() ?: return@put
                        val id = call.parameters["id"]?.toLongOrNull()?.let(::WishlistId) ?: run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@put
                        }
                        val newWishlist = call.receive<NewWishlistInFeature>()
                        val existing = wishlistRepo.getById(id) ?: run {
                            call.respond(HttpStatusCode.NotFound)
                            return@put
                        }
                        val updated = wishlistRepo.update(
                            id,
                            NewWishlist(existing.userId, newWishlist.title, existing.defaultPriceUnits)
                        )
                        if (updated != null) {
                            call.respond(updated.asAdminWishlist())
                        } else {
                            call.respond(HttpStatusCode.InternalServerError)
                        }
                    }
                    delete("${Constants.wishlistsDeletePathPart}/{id}") {
                        requireAdmin() ?: return@delete
                        val id = call.parameters["id"]?.toLongOrNull()?.let(::WishlistId) ?: run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@delete
                        }
                        if (!wishlistRepo.contains(id)) {
                            call.respond(HttpStatusCode.NotFound)
                            return@delete
                        }
                        wishlistRepo.deleteById(id)
                        call.respond(HttpStatusCode.OK)
                    }
                }
                route(Constants.wishlistItemsPathPart) {
                    get("${Constants.wishlistItemsGetByWishlistIdPathPart}/{wishlistId}") {
                        requireAdmin() ?: return@get
                        val wishlistId = call.parameters["wishlistId"]?.toLongOrNull()?.let(::WishlistId) ?: run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@get
                        }
                        call.respond(wishlistItemRepo.getByWishlistId(wishlistId).map { it.asAdminWishlistItem() })
                    }
                    post(Constants.wishlistItemsCreatePathPart) {
                        requireAdmin() ?: return@post
                        val newItem = call.receive<NewWishlistItem>()
                        val result = wishlistItemRepo.create(listOf(newItem)).firstOrNull() ?: run {
                            call.respond(HttpStatusCode.InternalServerError)
                            return@post
                        }
                        call.respond(result.asAdminWishlistItem())
                    }
                    put("${Constants.wishlistItemsUpdatePathPart}/{id}") {
                        requireAdmin() ?: return@put
                        val id = call.parameters["id"]?.toLongOrNull()?.let(::WishlistItemId) ?: run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@put
                        }
                        val newItem = call.receive<NewWishlistItem>()
                        val updated = wishlistItemRepo.update(id, newItem) ?: run {
                            call.respond(HttpStatusCode.NotFound)
                            return@put
                        }
                        call.respond(updated.asAdminWishlistItem())
                    }
                    delete("${Constants.wishlistItemsDeletePathPart}/{id}") {
                        requireAdmin() ?: return@delete
                        val id = call.parameters["id"]?.toLongOrNull()?.let(::WishlistItemId) ?: run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@delete
                        }
                        if (!wishlistItemRepo.contains(id)) {
                            call.respond(HttpStatusCode.NotFound)
                            return@delete
                        }
                        wishlistItemRepo.deleteById(id)
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }
    }
}
