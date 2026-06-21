package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.email.common.models.Email
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    /**
     * Holds the result of the most recent send-test-email attempt.
     *
     * `null` = not yet attempted; `true` = last send succeeded; `false` = last send failed.
     */
    private val _sendTestEmailState = MutableRedeliverStateFlow<Boolean?>(null)

    /** Observable result of the last [onSendTestEmail] call. */
    val sendTestEmailState: StateFlow<Boolean?> = _sendTestEmailState.asStateFlow()

    /** Called when the user taps the Users section button. */
    fun onOpenUsers() {
        scope.launchLoggingDropExceptions { interactor.onOpenUsers(node) }
    }

    /** Called when the user taps the Wishlists section button. */
    fun onOpenWishlists() {
        scope.launchLoggingDropExceptions { interactor.onOpenWishlists(node) }
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
        scope.launchLoggingDropExceptions {
            _sendTestEmailState.value = model.sendTestEmail(recipient)
        }
    }
}
