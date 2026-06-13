package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.serialization.Serializable

/**
 * Navigation config for the user profile edit screen.
 *
 * Reachable only for the profile owner and `root`. A non-root owner has no editable text fields but
 * may upload an avatar; `root` may edit the username and password and delete the user. The user id
 * itself is never editable. Creation of users is not done here (it is an admin-panel concern).
 *
 * @property userId Identifier of the user being edited.
 */
@Serializable
data class UserEditViewConfig(val userId: UserId) : ViewConfig
