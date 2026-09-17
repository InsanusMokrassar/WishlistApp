package dev.inmo.wishlist.features.ui.sidebar.ui

import dev.inmo.wishlist.features.ui.booking.ui.BookingModel
import dev.inmo.wishlist.features.ui.users.ui.UsersModel
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsModel
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistsFeatureWishlist
import kotlinx.coroutines.flow.StateFlow

/**
 * Default sidebar model that composes existing UI feature models.

 * @param wishlistsModel Wishlist data and caller identity capability.
 * @param bookingModel Booking capability used for the reserved-item count.
 * @param usersModel User-role capability used for the admin navigation gate.
 */
class DefaultSidebarModel(
    private val wishlistsModel: WishlistsModel,
    private val bookingModel: BookingModel,
    private val usersModel: UsersModel,
) : SidebarModel {
    /** Exact caller-id flow exposed by the wishlist model. */
    override val currentUserIdFlow: StateFlow<UserId?> = wishlistsModel.currentUserIdFlow

    /** Exact root-access flow exposed by the users model. */
    override val isCurrentUserRootFlow: StateFlow<Boolean> = usersModel.isCurrentUserRootFlow

    /** @return Wishlists owned by the authenticated caller. */
    override suspend fun getMyWishlists(): List<WishlistsFeatureWishlist> = wishlistsModel.getMyWishlists()

    /** @return Count of items reserved by the authenticated caller. */
    override suspend fun getReservedCount(): Int = bookingModel.myPresentsBooks().size

    /** @return The display name of [userId], or `null` when unknown. */
    override suspend fun getUserName(userId: UserId): String? = wishlistsModel.getUserName(userId)
}
