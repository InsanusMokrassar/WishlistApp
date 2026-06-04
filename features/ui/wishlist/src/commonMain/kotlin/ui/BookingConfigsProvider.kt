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
    /** Dedicated chain id so the inline booking view lives in its own isolated navigation chain. */
    override val chainId: NavigationChainId = NavigationChainId("wishlistItemAdditionalConfig_booking")

    /**
     * Builds the booking view config for [item].
     *
     * @param item Item to draw the inline gift-booking view for.
     * @return [BookingViewConfig] carrying the item and its parent wishlist.
     */
    override fun createConfig(item: RegisteredWishlistItem): ViewConfig =
        BookingViewConfig(item.id, item.wishlistId)
}
