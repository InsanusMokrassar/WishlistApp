package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.common.client.ui.components.CalmButton
import dev.inmo.wishlist.features.common.client.ui.components.CalmButtonVariant
import dev.inmo.wishlist.features.common.client.ui.components.CalmIcons
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings

/**
 * Owner-gated "New Wishlist" button shared by [WishlistsListView] and [UserWishlistsView].
 *
 * Renders nothing when [isOwner] is `false` — non-owners and anonymous viewers never see the button.
 * Rendered through the shared [CalmButton] primary action with a leading plus glyph; label is
 * [WishlistStrings.createWishlistButton].
 *
 * @param isOwner Whether the authenticated caller owns the displayed list; gates rendering.
 * @param onClick Invoked when the button is pressed; callers delegate to the screen's
 *   `onCreateWishlist()`.
 */
@Composable
fun CreateWishlistButton(isOwner: Boolean, onClick: () -> Unit) {
    if (!isOwner) return
    CalmButton(
        text = WishlistStrings.createWishlistButton.translation(),
        onClick = onClick,
        variant = CalmButtonVariant.Primary,
        leadingIcon = CalmIcons.plus,
    )
}
