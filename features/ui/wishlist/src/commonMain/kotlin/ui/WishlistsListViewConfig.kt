package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.serialization.Serializable

/**
 * Navigation config for the wishlists list screen.
 *
 * When [userId] is `null` the view shows the authenticated caller's own
 * wishlists (via `model.getMyWishlists()`); when non-null it shows the
 * specified user's wishlists (via `model.getUserWishlists(userId)`).
 *
 * @param userId Optional owner whose wishlists should be displayed.
 */
@Serializable
class WishlistsListViewConfig(
    val userId: UserId? = null
) : ViewConfig
