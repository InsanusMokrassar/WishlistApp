package dev.inmo.wishlist.features.ui.sidebar.ui

import dev.inmo.wishlist.features.common.client.models.ViewConfig
import kotlinx.serialization.Serializable

/**
 * Navigation config for the Calm Studio left sidebar.
 *
 * Placed in the scaffold's `leftConfig` slot on the web client; carries no parameters because the
 * sidebar always reflects the signed-in caller's own context (lists, reserved count, profile).
 */
@Serializable
class SidebarViewConfig : ViewConfig
