package dev.inmo.wishlist.features.ui.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.CalmButton
import dev.inmo.wishlist.features.common.client.ui.components.CalmButtonVariant
import dev.inmo.wishlist.features.common.client.ui.components.CalmModal
import dev.inmo.wishlist.features.common.client.ui.components.CalmTextField
import dev.inmo.wishlist.features.common.client.ui.components.FormHint
import dev.inmo.wishlist.features.common.client.ui.components.ModalBody
import dev.inmo.wishlist.features.common.client.ui.components.ModalFooter
import dev.inmo.wishlist.features.common.client.ui.components.ModalHeader
import dev.inmo.wishlist.features.common.client.ui.components.ModalTabs
import dev.inmo.wishlist.features.ui.auth.AuthStrings
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.onSubmit
import org.jetbrains.compose.web.dom.Form
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * JS Compose-HTML auth widget for the Calm Studio top bar.
 *
 * - Authenticated: a ghost "Log out" button.
 * - Logged-out: ghost "Log in" (and "Register", when registration is enabled) triggers.
 * - Expanded: the credentials form is shown as a [CalmModal] with a [ModalTabs] switch between login and
 *   registration, composed from the shared Calm Studio modal/form components.
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
            CalmButton(
                text = AuthStrings.logoutButton.translation(),
                onClick = { viewModel.onLogout() },
                variant = CalmButtonVariant.Ghost,
                disabled = loading,
            )
            return
        }

        CalmButton(
            text = AuthStrings.loginButton.translation(),
            onClick = { viewModel.onToggleForm() },
            variant = CalmButtonVariant.Ghost,
        )
        if (registrationEnabled) {
            CalmButton(
                text = AuthStrings.registerButton.translation(),
                onClick = { viewModel.onToggleRegisterForm() },
                variant = CalmButtonVariant.Primary,
            )
        }

        if (!expanded) return

        val title = if (registerMode) {
            AuthStrings.registerButton.translation()
        } else {
            AuthStrings.loginButton.translation()
        }

        CalmModal(onDismiss = { if (!loading) viewModel.onCancelForm() }) {
            ModalHeader(title)
            Form(attrs = {
                onSubmit {
                    it.preventDefault()
                    if (registerMode) viewModel.onRegister() else viewModel.onAuthorize()
                }
            }) {
                ModalBody {
                    if (registrationEnabled) {
                        ModalTabs(
                            tabs = listOf(false, true),
                            selected = registerMode,
                            label = { isRegister ->
                                if (isRegister) AuthStrings.registerButton.translation()
                                else AuthStrings.loginButton.translation()
                            },
                            onSelect = { isRegister ->
                                if (isRegister) viewModel.onToggleRegisterForm() else viewModel.onShowLoginForm()
                            },
                        )
                    }
                    CalmTextField(
                        value = username,
                        onValueChange = { viewModel.onUsernameChanged(it) },
                        label = AuthStrings.usernamePlaceholder.translation(),
                        placeholder = AuthStrings.usernamePlaceholder.translation(),
                        disabled = loading,
                        id = "auth-username",
                    )
                    CalmTextField(
                        value = password,
                        onValueChange = { viewModel.onPasswordChanged(it) },
                        label = AuthStrings.passwordPlaceholder.translation(),
                        placeholder = AuthStrings.passwordPlaceholder.translation(),
                        type = InputType.Password,
                        disabled = loading,
                        id = "auth-password",
                    )
                    if (error) {
                        FormHint(
                            text = if (registerMode) AuthStrings.errorRegisterFailed.translation()
                                else AuthStrings.errorLoginFailed.translation(),
                            error = true,
                        )
                    }
                }
                ModalFooter {
                    CalmButton(
                        text = AuthStrings.cancelButton.translation(),
                        onClick = { viewModel.onCancelForm() },
                        variant = CalmButtonVariant.Ghost,
                        disabled = loading,
                    )
                    CalmButton(
                        text = if (registerMode) AuthStrings.submitRegisterButton.translation()
                            else AuthStrings.submitButton.translation(),
                        onClick = { },
                        variant = CalmButtonVariant.Primary,
                        disabled = !loginEnabled,
                        type = ButtonType.Submit,
                    )
                }
            }
        }
    }
}
