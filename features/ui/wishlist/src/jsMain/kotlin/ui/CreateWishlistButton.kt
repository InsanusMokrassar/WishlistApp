package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Text

/**
 * Owner-gated "New Wishlist" button shared by [WishlistsListView] and [UserWishlistsView].
 *
 * Renders nothing when [isOwner] is `false` — non-owners and anonymous viewers never see the
 * button. Uses Bootstrap `btn btn-primary` markup; label is [WishlistStrings.createWishlistButton].
 *
 * @param isOwner Whether the authenticated caller owns the displayed list; gates rendering.
 * @param onClick Invoked when the button is pressed; callers delegate to the screen's
 *   `onCreateWishlist()`.
 */
@Composable
fun CreateWishlistButton(isOwner: Boolean, onClick: () -> Unit) {
    if (!isOwner) return
    Button({
        classes("btn", "btn-primary")
        onClick { onClick() }
    }) {
        Text(WishlistStrings.createWishlistButton.translation())
    }
}
