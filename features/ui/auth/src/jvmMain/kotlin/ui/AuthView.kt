package dev.inmo.wishlist.features.ui.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.auth.AuthStrings
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * JVM Compose-Desktop view for the inline auth widget.
 *
 * Compact horizontal layout intended for the top bar:
 * - Authenticated: single "Log out" button.
 * - Collapsed: "Log in" button (and "Register" when registration is enabled).
 * - Expanded login mode: username/password fields + "Submit" / "Cancel".
 * - Expanded register mode: username/password fields + "Create account" / "Cancel".
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
            Button(
                onClick = { viewModel.onLogout() },
                enabled = !loading
            ) { Text(AuthStrings.logoutButton.translation()) }
            return
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(onClick = { viewModel.onToggleForm() }) {
                Text(AuthStrings.loginButton.translation())
            }
            if (registrationEnabled) {
                Button(onClick = { viewModel.onToggleRegisterForm() }) {
                    Text(AuthStrings.registerButton.translation())
                }
            }
        }

        if (!expanded) return

        Dialog(onDismissRequest = { if (!loading) viewModel.onCancelForm() }) {
            Surface {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (registerMode) AuthStrings.registerButton.translation()
                        else AuthStrings.loginButton.translation()
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { viewModel.onUsernameChanged(it) },
                        placeholder = { Text(AuthStrings.usernamePlaceholder.translation()) },
                        singleLine = true,
                        enabled = !loading,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { viewModel.onSubmit() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { viewModel.onPasswordChanged(it) },
                        placeholder = { Text(AuthStrings.passwordPlaceholder.translation()) },
                        singleLine = true,
                        enabled = !loading,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { viewModel.onSubmit() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error) {
                        Text(
                            if (registerMode) AuthStrings.errorRegisterFailed.translation()
                            else AuthStrings.errorLoginFailed.translation()
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        Button(
                            onClick = { viewModel.onCancelForm() },
                            enabled = !loading
                        ) { Text(AuthStrings.cancelButton.translation()) }
                        if (registerMode) {
                            Button(
                                onClick = { viewModel.onRegister() },
                                enabled = loginEnabled
                            ) { Text(AuthStrings.submitRegisterButton.translation()) }
                        } else {
                            Button(
                                onClick = { viewModel.onAuthorize() },
                                enabled = loginEnabled
                            ) { Text(AuthStrings.submitButton.translation()) }
                        }
                    }
                }
            }
        }
    }
}
