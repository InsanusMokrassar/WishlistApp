package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import kotlinx.serialization.Serializable

/**
 * Navigation config for the admin wishlist create/edit screen.
 *
 * @property wishlistId `null` in create mode; non-null to edit the existing wishlist.
 * @property preselectedUserId When non-null, the owner dropdown is pre-selected to this user.
 */
@Serializable
data class AdminWishlistEditViewConfig(
    val wishlistId: WishlistId?,
    val preselectedUserId: UserId? = null
) : ViewConfig
