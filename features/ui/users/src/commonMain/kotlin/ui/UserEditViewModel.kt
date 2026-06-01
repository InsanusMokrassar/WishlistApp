package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.micro_utils.common.MPPFile
import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.onResumeFlow
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.takeWhile

/**
 * ViewModel for the user profile edit screen.
 *
 * Access rules mirror the requirement:
 * - The screen is reachable by the profile **owner** and **root**; both may upload an avatar.
 * - A non-root owner has **no editable text fields** ([isRootState] is `false`): only the avatar
 *   uploader is shown.
 * - **root** ([isRootState] is `true`) may edit the username and set a new password (with a
 *   confirmation field that must match) and delete the user. The user id is never editable.
 *
 * Username/password mutations go through the root-only admin endpoints; the avatar upload goes
 * through the files feature (allowed for the owner or root). Server-side authorization is the
 * source of truth — the field gating here is purely presentational.
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Users data source.
 * @param interactor Navigation delegate for this screen.
 */
class UserEditViewModel(
    private val node: NavigationNode<UserEditViewConfig, ViewConfig>,
    private val model: UsersModel,
    private val interactor: UserEditViewInteractor
) : ViewModel<ViewConfig>(node) {
    /** Identifier of the edited user; surfaced read-only to the view. */
    val userId: UserId = node.config.userId

    private val _isRootState = MutableRedeliverStateFlow(false)

    /** `true` when the caller is `root`; gates the editable username/password fields and delete. */
    val isRootState = _isRootState.asStateFlow()

    private val _usernameState = MutableRedeliverStateFlow("")

    /** Current value of the username input (editable by root only). */
    val usernameState = _usernameState.asStateFlow()

    private val _passwordState = MutableRedeliverStateFlow("")

    /** Current value of the new-password input (root only; blank = keep current password). */
    val passwordState = _passwordState.asStateFlow()

    private val _confirmPasswordState = MutableRedeliverStateFlow("")

    /** Current value of the password confirmation input (root only). */
    val confirmPasswordState = _confirmPasswordState.asStateFlow()

    private val _avatarIdState = MutableRedeliverStateFlow<FileId?>(null)

    /** Current avatar file id of the edited user, or `null` when none. */
    val avatarIdState = _avatarIdState.asStateFlow()

    private val _uploadingAvatarState = MutableRedeliverStateFlow(false)

    /** `true` while an avatar upload is in flight. */
    val uploadingAvatarState = _uploadingAvatarState.asStateFlow()

    private val _loadingState = MutableRedeliverStateFlow(false)

    /** `true` while a load/save/delete request is in flight. */
    val loadingState = _loadingState.asStateFlow()

    private val _isDirtyState = MutableRedeliverStateFlow(false)

    /** `true` when a username/password field was edited since opening (gates the discard dialog). */
    val isDirtyState = _isDirtyState.asStateFlow()

    private val _showConfirmDialogState = MutableRedeliverStateFlow(false)

    /** `true` when the discard-changes confirmation dialog should be visible. */
    val showConfirmDialogState = _showConfirmDialogState.asStateFlow()

    private val _showDeleteDialogState = MutableRedeliverStateFlow(false)

    /** `true` when the delete confirmation dialog should be visible. */
    val showDeleteDialogState = _showDeleteDialogState.asStateFlow()

    /**
     * `true` when the typed password and confirmation differ while at least one is non-blank.
     * Used by the view to show an inline mismatch error.
     */
    val passwordMismatchState: StateFlow<Boolean> =
        combine(_passwordState, _confirmPasswordState) { password, confirm ->
            password != confirm && (password.isNotBlank() || confirm.isNotBlank())
        }.stateIn(scope, SharingStarted.Eagerly, false)

    /**
     * `true` when the form may be saved: caller is root, username is non-blank, no request is in
     * flight, and the optional new password (when provided) matches its confirmation.
     */
    val canSaveState: StateFlow<Boolean> =
        combine(
            _isRootState,
            _usernameState,
            _passwordState,
            _confirmPasswordState,
            _loadingState
        ) { isRoot, username, password, confirm, loading ->
            val passwordOk = (password.isBlank() && confirm.isBlank()) || password == confirm
            isRoot && username.isNotBlank() && !loading && passwordOk
        }.stateIn(scope, SharingStarted.Eagerly, false)

    init {
        var inited = false
        merge(flowOf(Unit), node.onResumeFlow).takeWhile { !inited }.subscribeLoggingDropExceptions(scope) {
            _loadingState.value = true
            try {
                _isRootState.value = model.isCurrentUserRoot()
                model.getUser(userId)?.let { _usernameState.value = it.username.string }
                _avatarIdState.value = model.getAvatar(userId)
            } finally {
                _loadingState.value = false
            }
            inited = true
        }
    }

    /**
     * Updates the username field and marks the form dirty (root only).
     *
     * @param username New username value.
     */
    fun onUsernameChanged(username: String) {
        _usernameState.value = username
        _isDirtyState.value = true
    }

    /**
     * Updates the new-password field and marks the form dirty (root only).
     *
     * @param password New password value.
     */
    fun onPasswordChanged(password: String) {
        _passwordState.value = password
        _isDirtyState.value = true
    }

    /**
     * Updates the password confirmation field and marks the form dirty (root only).
     *
     * @param confirm Confirmation value; must match [passwordState] to save.
     */
    fun onConfirmPasswordChanged(confirm: String) {
        _confirmPasswordState.value = confirm
        _isDirtyState.value = true
    }

    /**
     * Uploads [file] and sets it as this user's avatar (owner or root). Refreshes [avatarIdState] on
     * success. Avatar changes persist immediately and do not affect [isDirtyState].
     *
     * @param file Image chosen by the user on the current platform.
     */
    fun onAvatarPicked(file: MPPFile) {
        scope.launchLoggingDropExceptions {
            _uploadingAvatarState.value = true
            try {
                val newId = model.uploadAvatar(userId, file)
                if (newId != null) _avatarIdState.value = newId
            } finally {
                _uploadingAvatarState.value = false
            }
        }
    }

    /**
     * Attempts to navigate back. Shows the discard dialog when the form is dirty, otherwise pops.
     */
    fun onBack() {
        if (_isDirtyState.value) {
            _showConfirmDialogState.value = true
        } else {
            scope.launchLoggingDropExceptions { interactor.onNavigateBack(node) }
        }
    }

    /** Confirms discarding changes and pops. */
    fun onConfirmBack() {
        _showConfirmDialogState.value = false
        scope.launchLoggingDropExceptions { interactor.onNavigateBack(node) }
    }

    /** Cancels the discard dialog, returning to the form. */
    fun onCancelBack() {
        _showConfirmDialogState.value = false
    }

    /**
     * Saves root-editable fields: updates the username and, when a new password was entered and
     * confirmed, sets it. No-op unless [canSaveState] is `true`.
     */
    fun onSave() {
        scope.launchLoggingDropExceptions {
            if (!canSaveState.value) return@launchLoggingDropExceptions
            _loadingState.value = true
            try {
                model.updateUsername(userId, Username(_usernameState.value.trim()))
                val password = _passwordState.value
                if (password.isNotBlank()) {
                    model.setPassword(userId, Password(password))
                }
                interactor.onSaved(node)
            } finally {
                _loadingState.value = false
            }
        }
    }

    /** Opens the delete confirmation dialog. No-op unless the caller is root. */
    fun onDeleteRequest() {
        if (!_isRootState.value) return
        _showDeleteDialogState.value = true
    }

    /** Confirms deletion: removes the user (server cascade) then delegates to [UserEditViewInteractor.onDeleted]. */
    fun onConfirmDelete() {
        scope.launchLoggingDropExceptions {
            _showDeleteDialogState.value = false
            _loadingState.value = true
            try {
                model.deleteUser(userId)
            } finally {
                _loadingState.value = false
            }
            interactor.onDeleted(node)
        }
    }

    /** Cancels the delete dialog. */
    fun onCancelDelete() {
        _showDeleteDialogState.value = false
    }

    /**
     * Builds the URL of the avatar image [id] for direct rendering (JS).
     *
     * @param id Avatar file id.
     * @return Relative URL resolved against the server base URL.
     */
    fun imageUrl(id: FileId): String = model.imageUrl(id)

    /**
     * Downloads avatar bytes for platforms that decode images themselves (JVM/Android).
     *
     * @param id Avatar file id.
     * @return Payload bytes, or `null` on failure.
     */
    suspend fun loadImageBytes(id: FileId): ByteArray? = model.loadImageBytes(id)
}
