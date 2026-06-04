package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.micro_utils.strings.StringResource
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem

/**
 * Extension point for additional, item-scoped screens reachable from the wishlist item view.
 *
 * Each provider contributes one button to the wishlist item screen (rendered in the same row that
 * formerly held the hard-coded booking button). Tapping the button pushes the [ViewConfig] returned
 * by [createConfig] onto the navigation chain, opening the provider's own screen for the item.
 *
 * Implementations are collected at the ViewModel factory via Koin `getAllDistinct` and injected into
 * [WishlistItemViewModel] through its constructor. The interface is `sealed`, so every implementation
 * lives in this module (`features/ui/wishlist`); a feature that wants to plug in (e.g. booking)
 * provides a thin adapter here that returns the foreign feature's own [ViewConfig].
 */
sealed interface WishlistAdditionalConfigsProvider {
    /** Localized label of the button rendered for this provider on the wishlist item screen. */
    val buttonLabel: StringResource

    /**
     * Builds the navigation config to open when the user taps this provider's button.
     *
     * @param item Item currently displayed on the wishlist item screen.
     * @return Config pushed onto the navigation chain to open the provider's screen for [item].
     */
    fun createConfig(item: RegisteredWishlistItem): ViewConfig
}
