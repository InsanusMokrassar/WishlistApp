package dev.inmo.wishlist.features.ui.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.auth.AuthStrings
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * JS Compose-HTML Bootstrap auth widget embedded in the top navbar.
 *
 * - Authenticated: "Log out" button.
 * - Logged-out: "Log in" button (and "Register" when registration is enabled).
 * - Expanded: the credentials form is shown inside a modal dialog (Bootstrap
 *   `modal` overlay + backdrop) rather than inline.
 */
class AuthView(
    chain: NavigationChain<ViewConfig>,
    config: AuthViewConfig,
) : ComposeView<AuthViewConfig, ViewConfig, AuthViewModel>(config, chain) {
    override val viewModel: AuthViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@AuthView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val loggedIn by viewModel.loggedInState.collectAsState()
        val expanded by viewModel.formExpandedState.collectAsState()
        val registerMode by viewModel.registerModeState.collectAsState()
        val registrationEnabled by viewModel.registrationEnabledState.collectAsState()
        val username by viewModel.usernameState.collectAsState()
        val password by viewModel.passwordState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val error by viewModel.errorState.collectAsState()
        val loginEnabled by viewModel.loginEnabledState.collectAsState()

        if (loggedIn) {
            Button(attrs = {
                classes("btn", "btn-outline-light", "btn-sm")
                onClick { viewModel.onLogout() }
                if (loading) disabled()
            }) { Text(AuthStrings.logoutButton.translation()) }
            return
        }

        Div({ classes("d-flex", "gap-2") }) {
            Button(attrs = {
                classes("btn", "btn-outline-light", "btn-sm")
                onClick { viewModel.onToggleForm() }
            }) { Text(AuthStrings.loginButton.translation()) }
            if (registrationEnabled) {
                Button(attrs = {
                    classes("btn", "btn-outline-light", "btn-sm")
                    onClick { viewModel.onToggleRegisterForm() }
                }) { Text(AuthStrings.registerButton.translation()) }
            }
        }

        if (!expanded) return

        val title = if (registerMode) {
            AuthStrings.registerButton.translation()
        } else {
            AuthStrings.loginButton.translation()
        }

        // Modal backdrop
        Div({ classes("modal-backdrop", "fade", "show") })
        // Modal dialog
        Div({
            classes("modal", "fade", "show", "d-block")
            attr("tabindex", "-1")
        }) {
            Div({ classes("modal-dialog", "modal-dialog-centered") }) {
                Div({ classes("modal-content") }) {
                    Div({ classes("modal-header") }) {
                        Span({ classes("modal-title", "h5") }) { Text(title) }
                        Button(attrs = {
                            classes("btn-close")
                            attr("aria-label", "Close")
                            onClick { viewModel.onCancelForm() }
                            if (loading) disabled()
                        }) {}
                    }
                    Div({ classes("modal-body", "d-flex", "flex-column", "gap-2") }) {
                        Input(type = InputType.Text) {
                            classes("form-control")
                            value(username)
                            placeholder(AuthStrings.usernamePlaceholder.translation())
                            onInput { viewModel.onUsernameChanged(it.value) }
                            if (loading) disabled()
                        }
                        Input(type = InputType.Password) {
                            classes("form-control")
                            value(password)
                            placeholder(AuthStrings.passwordPlaceholder.translation())
                            onInput { viewModel.onPasswordChanged(it.value) }
                            if (loading) disabled()
                        }
                        if (error) {
                            Span({ classes("text-danger", "small") }) {
                                val msg = if (registerMode) {
                                    AuthStrings.errorRegisterFailed.translation()
                                } else {
                                    AuthStrings.errorLoginFailed.translation()
                                }
                                Text(msg)
                            }
                        }
                    }
                    Div({ classes("modal-footer") }) {
                        Button(attrs = {
                            classes("btn", "btn-outline-secondary")
                            onClick { viewModel.onCancelForm() }
                            if (loading) disabled()
                        }) { Text(AuthStrings.cancelButton.translation()) }
                        if (registerMode) {
                            Button(attrs = {
                                classes("btn", "btn-primary")
                                onClick { viewModel.onRegister() }
                                if (!loginEnabled) disabled()
                            }) { Text(AuthStrings.submitRegisterButton.translation()) }
                        } else {
                            Button(attrs = {
                                classes("btn", "btn-primary")
                                onClick { viewModel.onAuthorize() }
                                if (!loginEnabled) disabled()
                            }) { Text(AuthStrings.submitButton.translation()) }
                        }
                    }
                }
            }
        }
    }
}
