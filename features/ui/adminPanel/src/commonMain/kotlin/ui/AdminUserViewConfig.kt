package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.serialization.Serializable

/**
 * Navigation config for the admin user detail screen.
 *
 * @property userId Identifier of the user to display.
 */
@Serializable
data class AdminUserViewConfig(val userId: UserId) : ViewConfig
