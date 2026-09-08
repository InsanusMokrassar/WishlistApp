package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.onResumeFlow
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.wishlist.features.admin.common.models.AdminUser
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge

/**
 * ViewModel for the admin panel dashboard screen.
 *
 * Handles navigation to sub-screens and the root-only "send test email" action.
 * Delegates navigation to [interactor]; delegates data to [model].
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Admin data source, also provides email feature capability.
 * @param interactor Navigation delegate for this screen.
 */
class AdminPanelViewModel(
    private val node: NavigationNode<AdminPanelViewConfig, ViewConfig>,
    private val model: AdminPanelModel,
    private val interactor: AdminPanelViewInteractor
) : ViewModel<ViewConfig>(node) {
    private val _usersState = MutableRedeliverStateFlow<List<AdminUser>>(emptyList())

    /** Current registered users shown on the dashboard. */
    val usersState: StateFlow<List<AdminUser>> = _usersState.asStateFlow()

    private val _usersLoadingState = MutableRedeliverStateFlow(false)

    /** `true` while the dashboard is loading registered users. */
    val usersLoadingState: StateFlow<Boolean> = _usersLoadingState.asStateFlow()

    private val _usersLoadFailedState = MutableRedeliverStateFlow(false)

    /** `true` when the most recent users refresh failed. */
    val usersLoadFailedState: StateFlow<Boolean> = _usersLoadFailedState.asStateFlow()

    private val _emailFeatureEnabledState = MutableRedeliverStateFlow(false)

    /** Whether SMTP delivery is available for the root-only test-email form. */
    val emailFeatureEnabledState: StateFlow<Boolean> = _emailFeatureEnabledState.asStateFlow()

    private val _sendTestEmailInProgressState = MutableRedeliverStateFlow(false)

    /** `true` while a test-email request is in flight. */
    val sendTestEmailInProgressState: StateFlow<Boolean> = _sendTestEmailInProgressState.asStateFlow()


    /**
     * Holds the result of the most recent send-test-email attempt.
     *
     * `null` = not yet attempted; `true` = last send succeeded; `false` = last send failed.
     */
    private val _sendTestEmailState = MutableRedeliverStateFlow<Boolean?>(null)

    /** Observable result of the last [onSendTestEmail] call. */
    val sendTestEmailState: StateFlow<Boolean?> = _sendTestEmailState.asStateFlow()

    private var usersRequestVersion = 0L

    init {
        merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(scope) {
            refreshUsers()
        }
        model.userAuthorisedState.subscribeLoggingDropExceptions(scope) { authorised ->
            if (authorised) {
                refreshUsers()
            } else {
                clearUsers()
            }
        }
        scope.launchLoggingDropExceptions {
            _emailFeatureEnabledState.value = try {
                model.isEmailFeatureEnabled()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                false
            }
        }
    }

    private fun clearUsers() {
        usersRequestVersion += 1
        _usersState.value = emptyList()
        _usersLoadingState.value = false
        _usersLoadFailedState.value = false
    }

    private suspend fun refreshUsers() {
        if (!model.userAuthorisedState.value) {
            clearUsers()
            return
        }

        val requestVersion = ++usersRequestVersion
        _usersLoadingState.value = true
        _usersLoadFailedState.value = false
        try {
            val users = model.getAllUsers()
            if (requestVersion == usersRequestVersion && model.userAuthorisedState.value) {
                _usersState.value = users
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            if (requestVersion == usersRequestVersion) {
                _usersState.value = emptyList()
                _usersLoadFailedState.value = true
            }
        } finally {
            if (requestVersion == usersRequestVersion) {
                _usersLoadingState.value = false
            }
        }
    }

    /** Called when the user taps the Users section button. */
    fun onOpenUsers() {
        scope.launchLoggingDropExceptions { interactor.onOpenUsers(node) }
    }

    /** Called when the user taps the Wishlists section button. */
    fun onOpenWishlists() {
        scope.launchLoggingDropExceptions { interactor.onOpenWishlists(node) }
    }

    /** Retries loading users after a transient dashboard failure. */
    fun onRetryUsers() {
        scope.launchLoggingDropExceptions { refreshUsers() }
    }

    /** Opens the selected registered user's admin detail screen. */
    fun onUserSelected(userId: UserId) {
        scope.launchLoggingDropExceptions { interactor.onUserSelected(node, userId) }
    }

    /**
     * Sends a test email to [recipient] via the server's SMTP configuration.
     *
     * Updates [sendTestEmailState] with the server's response. Only root callers succeed
     * server-side — authorization is enforced by the server's `requireRoot` guard.
     *
     * @param recipient Validated target address.
     */
    fun onSendTestEmail(recipient: Email) {
        if (!_emailFeatureEnabledState.value || _sendTestEmailInProgressState.value) {
            return
        }
        _sendTestEmailInProgressState.value = true
        scope.launchLoggingDropExceptions {
            try {
                _sendTestEmailState.value = model.sendTestEmail(recipient)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                _sendTestEmailState.value = false
            } finally {
                _sendTestEmailInProgressState.value = false
            }
        }
    }
}
