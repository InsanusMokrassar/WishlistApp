package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.serialization.Serializable

/**
 * Navigation config for the alternative grid presentation of a user's wishlists.
 *
 * Shows the same data as [WishlistsListViewConfig] for a concrete owner
 * (via `model.getUserWishlists(userId)`) but rendered as a card grid.
 *
 * @property userId Owner whose wishlists are displayed.
 */
@Serializable
data class UserWishlistsViewConfig(
    val userId: UserId
) : ViewConfig
