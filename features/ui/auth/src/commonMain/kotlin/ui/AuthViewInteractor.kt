package dev.inmo.wishlist.features.ui.auth.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.common.client.models.ViewConfig

interface AuthViewInteractor {
    suspend fun onUserLoggedIn(node: NavigationNode<AuthViewConfig, ViewConfig>)
    suspend fun onUserLoggedOut()
}
