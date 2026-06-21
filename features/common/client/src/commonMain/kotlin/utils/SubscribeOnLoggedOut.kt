package dev.inmo.wishlist.features.common.client.utils

import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter

/**
 * Subscribes [scope] to "the authenticated caller just logged out" transitions of this login-state
 * flow and runs [action] on each genuine logout.
 *
 * The flow carries `true` while a user is logged in and `false` when logged out (the canonical
 * source is `AuthCredentialsStorage.userAuthorised`). A logout is the transition from `true` to
 * `false`.
 *
 * The flow's initial/replayed value is discarded via `drop(1)`: a [StateFlow] always re-emits its
 * current value to every new collector, so without `drop(1)` a ViewModel created while already
 * logged-out (`false`) would fire [action] immediately and close a freshly-opened editor. `drop(1)`
 * guarantees that only a value arriving **after** subscription can fire [action]. Neither a cold
 * start that begins logged-out nor the StateFlow's initial replay will trigger [action] — only a
 * genuine `true → false` transition while the subscriber is alive does. `distinctUntilChanged` is
 * intentionally omitted: [StateFlow] guarantees by contract that it only emits when the value
 * actually changes, so deduplication is already handled by the flow itself.
 *
 * Used by edit screens so that, on logout, each one exits itself (replacing the edit view with its
 * non-edit/read view) instead of leaving an orphaned editor open for a now-anonymous user.
 *
 * The subscription is tied to [scope] (the ViewModel scope) and therefore dies automatically when
 * the navigation node is removed from the chain.
 *
 * @param scope Coroutine scope tied to the subscriber's lifecycle (the ViewModel scope).
 * @param action Suspending side effect to run on each logout transition (a navigation exit call).
 */
fun StateFlow<Boolean>.subscribeOnLoggedOut(
    scope: CoroutineScope,
    action: suspend () -> Unit
) {
    drop(1)
        .filter { authorised -> !authorised }
        .subscribeLoggingDropExceptions(scope) { action() }
}
