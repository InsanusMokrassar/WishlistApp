package dev.inmo.wishlist.features.ui.auth.ui

import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.flow.StateFlow

/**
 * Model facade consumed by [AuthViewModel].
 *
 * Wraps the persistent credentials storage and the login/logout side of
 * `features/auth/client`. Server URL editing is owned by `features/ui/serverUrl`
 * — this model no longer exposes any URL-related operations.
 */
interface AuthModel {
    /**
     * Reactive flag that emits whether valid credentials are currently stored.
     *
     * Drives the View between "show Login button" and "show Logout button".
     */
    val userAuthorisedState: StateFlow<Boolean>

    /**
     * Verifies that the stored credentials are still valid against the server.
     *
     * @return `true` when the server confirms the caller, `false` otherwise.
     */
    suspend fun isAlreadyLoggedIn(): Boolean

    /**
     * Submits credentials to the server.
     *
     * @param username Login.
     * @param password Plaintext password.
     * @return `true` when the server returned success and credentials are persisted.
     */
    suspend fun login(username: Username, password: Password): Boolean

    /**
     * Invalidates the active session and clears local credentials.
     */
    suspend fun logout()
}
