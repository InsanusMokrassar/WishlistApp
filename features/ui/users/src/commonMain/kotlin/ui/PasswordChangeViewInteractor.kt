package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig

/** Side-effecting navigation capability for the token-authorized password-change screen. */
interface PasswordChangeViewInteractor {
    /** Replaces a successfully redeemed pending page with its credential-free completed state. */
    suspend fun onChanged(node: NavigationNode<PasswordChangeViewConfig, ViewConfig>)

    /** Leaves the completed or invalid page at a safe users-list destination. */
    suspend fun onContinue(node: NavigationNode<PasswordChangeViewConfig, ViewConfig>)
}
