package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.navigation.core.NavigationChainId
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.booking.ui.BookingViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.WishlistsFeatureItem

/**
 * [WishlistAdditionalConfigsProvider] adapter for the booking scenario.
 *
 * Draws the compact gift-booking view ([BookingViewConfig], scenario view A) inline on the wishlist
 * item screen, inside a fresh anonymous navigation chain. All booking UI and rules live in
 * `features/ui/booking` and `features/booking`; this adapter only routes the item into that scenario.
 */
class BookingConfigsProvider : WishlistAdditionalConfigsProvider {
    /**
     * Builds the booking view config for [item].
     *
     * @param item Item to draw the inline gift-booking view for.
     * @return [BookingViewConfig] carrying the item and its parent wishlist.
     */
    override fun createConfig(item: WishlistsFeatureItem): ViewConfig =
        BookingViewConfig(item.id, item.wishlistId)
}
