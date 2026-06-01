package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.serialization.Serializable

/**
 * Navigation config for the public user profile detail screen.
 *
 * Readable by anyone (anonymous included). The Edit action is gated to the profile owner and `root`.
 *
 * @property userId Identifier of the user to display.
 */
@Serializable
data class UserViewConfig(val userId: UserId) : ViewConfig
