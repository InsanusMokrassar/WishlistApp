package dev.inmo.wishlist.client.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.wishlist.client.PasswordChangeNavigationOwner
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import kotlinx.coroutines.CoroutineScope

/**
 * Binds password-change transitions to the composition scope that owns the live root hierarchy.
 *
 * @param owner Root-owned password navigation transition coordinator.
 * @param root Current root hierarchy exposed by the composition.
 * @param content Receives the composition-owned scope used by navigation initialization.
 */
@Composable
internal fun WithPasswordChangeNavigationBinding(
    owner: PasswordChangeNavigationOwner,
    root: NavigationChain<ViewConfig>,
    content: @Composable (CoroutineScope) -> Unit,
) {
    val rootScope = rememberCoroutineScope()
    DisposableEffect(owner, root, rootScope) {
        val unbind = owner.bind(root, rootScope)
        onDispose(unbind)
    }
    content(rootScope)
}
