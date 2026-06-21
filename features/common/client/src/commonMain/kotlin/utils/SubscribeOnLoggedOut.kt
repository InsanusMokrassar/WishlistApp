package dev.inmo.wishlist.features.common.client.utils

import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * Subscribes [scope] to "the authenticated caller just logged out" transitions of this reactive
 * current-user-id flow and runs [action] on each one.
 *
 * The flow is expected to carry the current caller id (`null` == anonymous / not yet resolved). A
 * logout is the transition from a non-`null` id to `null`. The flow's initial value is dropped, so
 * neither a cold start that begins anonymous nor the asynchronous first resolution of the id fires
 * [action] — only a genuine non-`null` → `null` change while the subscriber is alive does.
 *
 * Used by edit screens so that, on logout, each one exits itself (replacing the edit view with its
 * non-edit/read view) instead of leaving an orphaned editor open for a now-anonymous user.
 *
 * @param scope Coroutine scope tied to the subscriber's lifecycle.
 * @param action Side effect to run on each logout transition (typically a navigation exit).
 */
fun <T : Any> Flow<T?>.subscribeOnLoggedOut(
    scope: CoroutineScope,
    action: suspend () -> Unit
) {
    map { it == null }
        .distinctUntilChanged()
        .drop(1)
        .filter { it }
        .subscribeLoggingDropExceptions(scope) { action() }
}
