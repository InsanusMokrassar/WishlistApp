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
 * JS Compose-HTML view for the inline auth widget.
 *
 * - Authenticated: renders a "Log out" button.
 * - Not authenticated, form collapsed: renders a "Log in" button.
 * - Not authenticated, form expanded: renders username + password inputs and a "Submit" / "Cancel" pair.
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
        val username by viewModel.usernameState.collectAsState()
        val password by viewModel.passwordState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val error by viewModel.errorState.collectAsState()
        val loginEnabled by viewModel.loginEnabledState.collectAsState()

        Div {
            when {
                loggedIn -> {
                    Button(
                        attrs = {
                            onClick { viewModel.onLogout() }
                            if (loading) disabled()
                        }
                    ) { Text(AuthStrings.logoutButton.translation()) }
                }
                !expanded -> {
                    Button(
                        attrs = { onClick { viewModel.onToggleForm() } }
                    ) { Text(AuthStrings.loginButton.translation()) }
                }
                else -> {
                    Div {
                        Input(type = InputType.Text) {
                            value(username)
                            placeholder(AuthStrings.usernamePlaceholder.translation())
                            onInput { viewModel.onUsernameChanged(it.value) }
                            if (loading) disabled()
                        }
                        Input(type = InputType.Password) {
                            value(password)
                            placeholder(AuthStrings.passwordPlaceholder.translation())
                            onInput { viewModel.onPasswordChanged(it.value) }
                            if (loading) disabled()
                        }
                        Button(
                            attrs = {
                                onClick { viewModel.onAuthorize() }
                                if (!loginEnabled) disabled()
                            }
                        ) { Text(AuthStrings.submitButton.translation()) }
                        Button(
                            attrs = {
                                onClick { viewModel.onToggleForm() }
                                if (loading) disabled()
                            }
                        ) { Text(AuthStrings.cancelButton.translation()) }
                        if (error) {
                            Span { Text(AuthStrings.errorLoginFailed.translation()) }
                        }
                    }
                }
            }
        }
    }
}
