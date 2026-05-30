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
 * JS Compose-HTML Bootstrap inline auth widget embedded in the top navbar.
 *
 * - Authenticated: "Log out" button.
 * - Collapsed: "Log in" button (and "Register" when registration is enabled).
 * - Expanded login mode: username/password inputs + "Submit" + "Cancel".
 * - Expanded register mode: username/password inputs + "Create account" + "Cancel".
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

        when {
            loggedIn -> {
                Button(attrs = {
                    classes("btn", "btn-outline-light", "btn-sm")
                    onClick { viewModel.onLogout() }
                    if (loading) disabled()
                }) { Text(AuthStrings.logoutButton.translation()) }
            }
            !expanded -> {
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
            }
            else -> {
                Div({ classes("d-flex", "gap-2", "align-items-center") }) {
                    Input(type = InputType.Text) {
                        classes("form-control", "form-control-sm")
                        value(username)
                        placeholder(AuthStrings.usernamePlaceholder.translation())
                        onInput { viewModel.onUsernameChanged(it.value) }
                        if (loading) disabled()
                    }
                    Input(type = InputType.Password) {
                        classes("form-control", "form-control-sm")
                        value(password)
                        placeholder(AuthStrings.passwordPlaceholder.translation())
                        onInput { viewModel.onPasswordChanged(it.value) }
                        if (loading) disabled()
                    }
                    if (registerMode) {
                        Button(attrs = {
                            classes("btn", "btn-light", "btn-sm")
                            onClick { viewModel.onRegister() }
                            if (!loginEnabled) disabled()
                        }) { Text(AuthStrings.submitRegisterButton.translation()) }
                    } else {
                        Button(attrs = {
                            classes("btn", "btn-light", "btn-sm")
                            onClick { viewModel.onAuthorize() }
                            if (!loginEnabled) disabled()
                        }) { Text(AuthStrings.submitButton.translation()) }
                    }
                    Button(attrs = {
                        classes("btn", "btn-outline-light", "btn-sm")
                        onClick { viewModel.onCancelForm() }
                        if (loading) disabled()
                    }) { Text(AuthStrings.cancelButton.translation()) }
                    if (error) {
                        Span({ classes("text-warning", "small") }) {
                            val msg = if (registerMode) {
                                AuthStrings.errorRegisterFailed.translation()
                            } else {
                                AuthStrings.errorLoginFailed.translation()
                            }
                            Text(msg)
                        }
                    }
                }
            }
        }
    }
}
