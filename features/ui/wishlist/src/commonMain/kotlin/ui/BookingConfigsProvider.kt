package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.micro_utils.strings.StringResource
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.booking.BookingStrings
import dev.inmo.wishlist.features.ui.booking.ui.BookingViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem

/**
 * [WishlistAdditionalConfigsProvider] adapter for the booking scenario.
 *
 * Contributes the "Book for gifting" button to the wishlist item screen and, on tap, opens the
 * booking scenario's gift-booking view ([BookingViewConfig], scenario view A). All booking UI and
 * rules live in `features/ui/booking` and `features/booking`; this adapter only routes the item
 * into that scenario.
 */
class BookingConfigsProvider : WishlistAdditionalConfigsProvider {
    override val buttonLabel: StringResource = BookingStrings.bookButton

    /**
     * Builds the booking view config for [item].
     *
     * @param item Item to open the gift-booking screen for.
     * @return [BookingViewConfig] carrying the item and its parent wishlist.
     */
    override fun createConfig(item: RegisteredWishlistItem): ViewConfig =
        BookingViewConfig(item.id, item.wishlistId)
}
