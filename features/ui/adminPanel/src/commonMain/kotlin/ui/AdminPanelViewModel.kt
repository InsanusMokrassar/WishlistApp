package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the admin panel dashboard screen.
 *
 * Provides section navigation (delegated to [interactor]) and a root-only "send test e-mail" action
 * backed by [model].
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Admin data source; also used to send the test e-mail.
 * @param interactor Navigation delegate for this screen.
 */
class AdminPanelViewModel(
    private val node: NavigationNode<AdminPanelViewConfig, ViewConfig>,
    private val model: AdminPanelModel,
    private val interactor: AdminPanelViewInteractor
) : ViewModel<ViewConfig>(node) {

    private val _testEmailAddressState = MutableRedeliverStateFlow("")

    /** Current text in the test-email address input. */
    val testEmailAddressState = _testEmailAddressState.asStateFlow()

    private val _sendingTestEmailState = MutableRedeliverStateFlow(false)

    /** `true` while a test-email send request is in flight. */
    val sendingTestEmailState = _sendingTestEmailState.asStateFlow()

    private val _testEmailResultState = MutableRedeliverStateFlow<Boolean?>(null)

    /** Last test-email result: `true` sent, `false` failed, `null` no attempt yet / reset. */
    val testEmailResultState = _testEmailResultState.asStateFlow()

    /** Called when the user taps the Users section button. */
    fun onOpenUsers() {
        scope.launchLoggingDropExceptions { interactor.onOpenUsers(node) }
    }

    /** Called when the user taps the Wishlists section button. */
    fun onOpenWishlists() {
        scope.launchLoggingDropExceptions { interactor.onOpenWishlists(node) }
    }

    /**
     * Called when the user edits the test-email address input.
     *
     * @param value New input text.
     */
    fun onTestEmailAddressChanged(value: String) {
        _testEmailAddressState.value = value
        _testEmailResultState.value = null
    }

    /** Called when the user taps the "send test e-mail" button. */
    fun onSendTestEmail() {
        scope.launchLoggingDropExceptions {
            _sendingTestEmailState.value = true
            try {
                _testEmailResultState.value = model.sendTestEmail(_testEmailAddressState.value)
            } finally {
                _sendingTestEmailState.value = false
            }
        }
    }
}
