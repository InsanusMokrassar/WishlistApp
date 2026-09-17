package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.wishlist.features.admin.client.AdminFeature
import dev.inmo.wishlist.features.admin.common.models.AdminUser
import dev.inmo.wishlist.features.admin.common.models.AdminWishlist
import dev.inmo.wishlist.features.admin.common.models.AdminWishlistItem
import dev.inmo.wishlist.features.admin.common.models.NewUserWithPassword
import dev.inmo.wishlist.features.auth.client.AuthCredentialsStorage
import dev.inmo.wishlist.features.email.client.EmailFeature
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlist
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistInFeature
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId

/**
 * Default outside-world implementation used by every admin-panel screen.
 *
 * @param admin Administrative user, wishlist, and item capabilities.
 * @param email Email capability used by the SMTP controls.
 * @param credentialsStorage Persistent authentication state exposed to editor ViewModels.
 */
class DefaultAdminPanelModel(
    private val admin: AdminFeature,
    private val email: EmailFeature,
    private val credentialsStorage: AuthCredentialsStorage,
) : AdminPanelModel {
    /** Authentication state shared with admin editor ViewModels. */
    override val userAuthorisedState = credentialsStorage.userAuthorised

    /** @return Every user visible through the administrative API. */
    override suspend fun getAllUsers(): List<AdminUser> =
        admin.usersManagement.getAll()

    /** @return The user identified by [id], or `null` when absent. */
    override suspend fun getUserById(id: UserId): AdminUser? =
        admin.usersManagement.getById(id)

    /** @return The created user, or `null` when creation fails. */
    override suspend fun createUser(newUser: NewUserWithPassword): AdminUser? =
        admin.usersManagement.create(newUser)

    /** @return `true` when replacing user [id] with [newUser] succeeds. */
    override suspend fun updateUser(id: UserId, newUser: NewUser): Boolean =
        admin.usersManagement.update(id, newUser)

    /** @return `true` when changing user [id] to [username] succeeds. */
    override suspend fun updateUsername(id: UserId, username: Username): Boolean =
        admin.usersManagement.updateUsername(id, username)

    /** @return `true` when user [id] is deleted. */
    override suspend fun deleteUser(id: UserId): Boolean =
        admin.usersManagement.delete(id)

    /** @return Every wishlist visible through the administrative API. */
    override suspend fun getAllWishlists(): List<AdminWishlist> =
        admin.wishlists.getAll()

    /** @return Wishlists owned by [userId]. */
    override suspend fun getWishlistsByUser(userId: UserId): List<AdminWishlist> =
        admin.wishlists.getByUserId(userId)

    /** @return The wishlist identified by [id], or `null` when absent. */
    override suspend fun getWishlistById(id: WishlistId): AdminWishlist? =
        admin.wishlists.getById(id)

    /** @return The created wishlist, or `null` when creation fails. */
    override suspend fun createWishlist(newWishlist: NewWishlist): AdminWishlist? =
        admin.wishlists.create(newWishlist)

    /**
     * Replaces the title of wishlist [id].
     *
     * @param userId Owner value retained by the UI contract; the administrative endpoint only accepts the title.
     * @return `true` when the update succeeds.
     */
    override suspend fun updateWishlist(id: WishlistId, userId: UserId, title: String): Boolean =
        admin.wishlists.update(id, NewWishlistInFeature(title))

    /** @return `true` when wishlist [id] is deleted. */
    override suspend fun deleteWishlist(id: WishlistId): Boolean =
        admin.wishlists.delete(id)

    /** @return Items belonging to [wishlistId]. */
    override suspend fun getItemsByWishlist(wishlistId: WishlistId): List<AdminWishlistItem> =
        admin.wishlistItems.getByWishlistId(wishlistId)

    /** @return The created item, or `null` when creation fails. */
    override suspend fun createWishlistItem(item: NewWishlistItem): AdminWishlistItem? =
        admin.wishlistItems.create(item)

    /** @return `true` when item [id] is replaced with [item]. */
    override suspend fun updateWishlistItem(id: WishlistItemId, item: NewWishlistItem): Boolean =
        admin.wishlistItems.update(id, item)

    /** @return `true` when item [id] is deleted. */
    override suspend fun deleteWishlistItem(id: WishlistItemId): Boolean =
        admin.wishlistItems.delete(id)

    /** @return `true` when SMTP-backed delivery is available. */
    override suspend fun isEmailFeatureEnabled(): Boolean =
        email.isFeatureEnabled()

    /** @return `true` when a test message is accepted for [recipient]. */
    override suspend fun sendTestEmail(recipient: Email): Boolean =
        email.sendTestEmail(recipient)
}
