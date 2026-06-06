package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.navigation.core.NavigationChainId
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.booking.ui.BookingViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem

/**
 * [WishlistAdditionalConfigsProvider] adapter for the booking scenario.
 *
 * Draws the compact gift-booking view ([BookingViewConfig], scenario view A) inline on the wishlist
 * item screen, inside its own navigation chain ([chainId]). All booking UI and rules live in
 * `features/ui/booking` and `features/booking`; this adapter only routes the item into that scenario.
 */
class BookingConfigsProvider : WishlistAdditionalConfigsProvider {
    /**
     * Stable non-null chain id for the inline booking view. An external host may pre-create a chain
     * with this id to relocate the compact booking view into a dedicated area; otherwise the item
     * screen injects it inline in an isolated chain under this id.
     */
    override val chainId: NavigationChainId? = NavigationChainId("wishlistItemAdditionalConfig_booking")

    /**
     * Builds the booking view config for [item].
     *
     * @param item Item to draw the inline gift-booking view for.
     * @return [BookingViewConfig] carrying the item and its parent wishlist.
     */
    override fun createConfig(item: RegisteredWishlistItem): ViewConfig =
        BookingViewConfig(item.id, item.wishlistId)
}
