package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.micro_utils.common.MPPFile
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.models.UsersFeatureUser
import kotlinx.coroutines.flow.StateFlow

/**
 * Model facade consumed by every users UI screen (list, profile view, profile edit).
 *
 * Hides the underlying `features/users/client` (public read), `features/auth/client`
 * (current caller), `features/roles/client` (functionality-availability checks),
 * `features/admin/client` (admin-panel mutations) and `features/files/client` (avatar storage)
 * feature surfaces behind one interface.
 */
interface UsersModel {
    /**
     * Reactive login-state flow; emits `true` while a user is authenticated and `false` after logout.
     * Mirrors `AuthCredentialsStorage.userAuthorised`, exposed here so edit ViewModels route their
     * logout-exit through the model instead of touching auth storage directly (MVVM boundary).
     */
    val userAuthorisedState: StateFlow<Boolean>

    /**
     * Returns the full list of registered users (public read).
     *
     * @return All [UsersFeatureUser]s; empty when none registered.
     */
    suspend fun getAllUsers(): List<UsersFeatureUser>

    /**
     * Resolves a single user by [id] from the public users list.
     *
     * @param id User identifier.
     * @return Matching [UsersFeatureUser], or `null` when not found.
     */
    suspend fun getUser(id: UserId): UsersFeatureUser?

    /**
     * Reactive id of the authenticated caller ("me"), or `null` when anonymous / not yet resolved.
     *
     * Backed by the auth "me" [StateFlow], so it self-corrects as the first `getMe()` round-trip
     * completes and on every later login/logout. Used to decide profile-edit access (owner) and the
     * "My profile" target; consumers MUST derive from this flow rather than a one-shot snapshot so
     * the gated UI does not stay stale after a cold start (PR #31 F2).
     */
    val currentUserIdFlow: StateFlow<UserId?>

    /**
     * Reactive flag: `true` while the authenticated caller may access the admin panel
     * (`admin.panel` functionality) — the identity permitted to edit arbitrary user fields or delete
     * users. Backed by `features/roles/client`'s functionality check over the auth "me" [StateFlow]
     * (same self-correcting guarantee as [currentUserIdFlow]); `false` while anonymous or not yet
     * resolved. Name kept for continuity (SuperAdmin is architecturally fixed to `root`).
     */
    val isCurrentUserRootFlow: StateFlow<Boolean>

    /**
     * Reactive flag: `true` while the authenticated caller may change another user's avatar
     * (`files.avatarChangeForOthers` functionality). Gates the avatar uploader when editing a profile
     * that is not the caller's own. Backed by `features/roles/client`; `false` while anonymous or not
     * yet resolved.
     */
    val canChangeAvatarForOthersFlow: StateFlow<Boolean>

    /**
     * Updates the username of user [id] (root-only on the server).
     *
     * @param id User to update.
     * @param username New username.
     * @return `true` when the server confirmed the update; `false` otherwise.
     */
    suspend fun updateUsername(id: UserId, username: Username): Boolean

    /**
     * Replaces the password of user [id] (root-only on the server).
     *
     * @param id User whose password to change.
     * @param password New plaintext password; hashed server-side.
     * @return `true` when the server confirmed the change; `false` otherwise.
     */
    suspend fun setPassword(id: UserId, password: Password): Boolean

    /**
     * Deletes the user [id] together with all data owned by the user (root-only on the server).
     *
     * @param id Identifier of the user to remove.
     * @return `true` when the server confirmed deletion; `false` otherwise.
     */
    suspend fun deleteUser(id: UserId): Boolean

    /**
     * Returns the avatar [FileId] currently set for [userId], or `null` when the user has none.
     *
     * @param userId Identity whose avatar to resolve.
     */
    suspend fun getAvatar(userId: UserId): FileId?

    /**
     * Uploads [file] and sets it as the avatar of [userId] (allowed for the user themselves or root).
     *
     * @param userId Identity whose avatar to set.
     * @param file Image chosen by the user on the current platform.
     * @return The stored avatar [FileId], or `null` when the upload/association failed.
     */
    suspend fun uploadAvatar(userId: UserId, file: MPPFile): FileId?

    /**
     * Builds the download URL of the image stored under [id], suitable as an `<img>` source.
     *
     * @param id Image file identifier.
     * @return Relative URL resolved against the configured server base URL.
     */
    fun imageUrl(id: FileId): String

    /**
     * Downloads the raw bytes of image [id]. Used by JVM/Android which decode images themselves.
     *
     * @param id Image file identifier.
     * @return Payload bytes, or `null` on failure.
     */
    suspend fun loadImageBytes(id: FileId): ByteArray?
}
