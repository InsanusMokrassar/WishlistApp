package project_group.project_name.features.ui.auth.ui

import dev.inmo.navigation.core.NavigationNode
import project_group.project_name.features.common.client.models.ViewConfig

interface AuthViewInteractor {
    suspend fun onUserLoggedIn(node: NavigationNode<AuthViewConfig, ViewConfig>)
    suspend fun onUserLoggedOut()
}
