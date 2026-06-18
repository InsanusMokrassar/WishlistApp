package dev.inmo.wishlist.features.ui.auth.ui

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.auth.AuthStrings
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.onSubmit
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Form
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * JS Compose-HTML auth widget for the Calm Studio top bar.
 *
 * - Authenticated: a ghost "Log out" button.
 * - Logged-out: ghost "Log in" (and "Register", when registration is enabled) triggers.
 * - Expanded: the credentials form is shown as a Calm `.scrim` modal with a `.tabs` switch between
 *   login and registration, mirroring the design skill's `LoginModal` reference.
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
                classes(CalmStudioStyleSheet.btn, CalmStudioStyleSheet.ghost)
                onClick { viewModel.onLogout() }
                if (loading) disabled()
            }) { Text(AuthStrings.logoutButton.translation()) }
            return
        }

        Button(attrs = {
            classes(CalmStudioStyleSheet.btn, CalmStudioStyleSheet.ghost)
            onClick { viewModel.onToggleForm() }
        }) { Text(AuthStrings.loginButton.translation()) }
        if (registrationEnabled) {
            Button(attrs = {
                classes(CalmStudioStyleSheet.btn, CalmStudioStyleSheet.primary)
                onClick { viewModel.onToggleRegisterForm() }
            }) { Text(AuthStrings.registerButton.translation()) }
        }

        if (!expanded) return

        val title = if (registerMode) {
            AuthStrings.registerButton.translation()
        } else {
            AuthStrings.loginButton.translation()
        }

        Div({
            classes(CalmStudioStyleSheet.scrim)
            onClick { if (!loading) viewModel.onCancelForm() }
        }) {
            Div({
                classes(CalmStudioStyleSheet.modal)
                onClick { it.stopPropagation() }
            }) {
                Div({ classes(CalmStudioStyleSheet.mhead) }) {
                    H2 { Text(title) }
                }
                Form(attrs = {
                    onSubmit {
                        it.preventDefault()
                        if (registerMode) viewModel.onRegister() else viewModel.onAuthorize()
                    }
                }) {
                    Div({ classes(CalmStudioStyleSheet.mbody) }) {
                        if (registrationEnabled) {
                            Div({ classes(CalmStudioStyleSheet.tabs) }) {
                                Button(attrs = {
                                    type(ButtonType.Button)
                                    if (!registerMode) classes(CalmStudioStyleSheet.on)
                                    onClick { viewModel.onShowLoginForm() }
                                }) { Text(AuthStrings.loginButton.translation()) }
                                Button(attrs = {
                                    type(ButtonType.Button)
                                    if (registerMode) classes(CalmStudioStyleSheet.on)
                                    onClick { viewModel.onToggleRegisterForm() }
                                }) { Text(AuthStrings.registerButton.translation()) }
                            }
                        }
                        Div({ classes(CalmStudioStyleSheet.fieldset) }) {
                            Label("auth-username") { Text(AuthStrings.usernamePlaceholder.translation()) }
                            Input(type = InputType.Text) {
                                id("auth-username")
                                classes(CalmStudioStyleSheet.input)
                                value(username)
                                placeholder(AuthStrings.usernamePlaceholder.translation())
                                onInput { viewModel.onUsernameChanged(it.value) }
                                if (loading) disabled()
                            }
                        }
                        Div({ classes(CalmStudioStyleSheet.fieldset) }) {
                            Label("auth-password") { Text(AuthStrings.passwordPlaceholder.translation()) }
                            Input(type = InputType.Password) {
                                id("auth-password")
                                classes(CalmStudioStyleSheet.input)
                                value(password)
                                placeholder(AuthStrings.passwordPlaceholder.translation())
                                onInput { viewModel.onPasswordChanged(it.value) }
                                if (loading) disabled()
                            }
                        }
                        if (error) {
                            P({
                                classes(CalmStudioStyleSheet.hint)
                                style { property("color", "var(--cs-danger)") }
                            }) {
                                val msg = if (registerMode) {
                                    AuthStrings.errorRegisterFailed.translation()
                                } else {
                                    AuthStrings.errorLoginFailed.translation()
                                }
                                Text(msg)
                            }
                        }
                    }
                    Div({ classes(CalmStudioStyleSheet.mfoot) }) {
                        Button(attrs = {
                            type(ButtonType.Button)
                            classes(CalmStudioStyleSheet.btn, CalmStudioStyleSheet.ghost)
                            onClick { viewModel.onCancelForm() }
                            if (loading) disabled()
                        }) { Text(AuthStrings.cancelButton.translation()) }
                        val submitLabel = if (registerMode) {
                            AuthStrings.submitRegisterButton.translation()
                        } else {
                            AuthStrings.submitButton.translation()
                        }
                        Button(attrs = {
                            type(ButtonType.Submit)
                            classes(CalmStudioStyleSheet.btn, CalmStudioStyleSheet.primary)
                            if (!loginEnabled) disabled()
                        }) { Text(submitLabel) }
                    }
                }
            }
        }
    }
}
