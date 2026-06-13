package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.serialization.Serializable

/**
 * Navigation config for the admin user create/edit screen.
 *
 * @property userId `null` in create mode; non-null to edit the existing user.
 */
@Serializable
data class AdminUserEditViewConfig(val userId: UserId?) : ViewConfig
