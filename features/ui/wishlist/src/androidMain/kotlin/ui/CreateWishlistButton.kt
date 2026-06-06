package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings

/**
 * Owner-gated "New Wishlist" button shared by [WishlistsListView] and [UserWishlistsView].
 *
 * Renders nothing when [isOwner] is `false` — non-owners and anonymous viewers never see the
 * button. Uses a Material3 [Button]; label is [WishlistStrings.createWishlistButton] resolved
 * via [LocalResources].
 *
 * @param isOwner Whether the authenticated caller owns the displayed list; gates rendering.
 * @param onClick Invoked when the button is pressed; callers delegate to the screen's
 *   `onCreateWishlist()`.
 */
@Composable
fun CreateWishlistButton(isOwner: Boolean, onClick: () -> Unit) {
    if (!isOwner) return
    Button(onClick = onClick) {
        Text(WishlistStrings.createWishlistButton.translation(LocalResources.current))
    }
}
