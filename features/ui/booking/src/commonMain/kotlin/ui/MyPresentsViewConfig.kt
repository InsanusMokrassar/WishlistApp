package dev.inmo.wishlist.features.ui.booking.ui

import dev.inmo.wishlist.features.common.client.models.ViewConfig
import kotlinx.serialization.Serializable

/**
 * Navigation config for scenario view B — the list of all presents the caller plans to make.
 *
 * Carries no parameters: the screen always shows the authenticated caller's own booked items.
 * Per issue #29 point #6, nothing in the app currently navigates to this config; it is registered
 * and renderable but unreachable by design.
 */
@Serializable
class MyPresentsViewConfig : ViewConfig
