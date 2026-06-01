package dev.inmo.wishlist.features.ui.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
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
 * Android Compose-Material3 view for the inline auth widget.
 *
 * Same four states as the JVM view; uses material3 widgets and resource-based
 * translations.
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
        val resources = LocalResources.current
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
            ) { Text(AuthStrings.logoutButton.translation(resources)) }
            return
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(onClick = { viewModel.onToggleForm() }) {
                Text(AuthStrings.loginButton.translation(resources))
            }
            if (registrationEnabled) {
                Button(onClick = { viewModel.onToggleRegisterForm() }) {
                    Text(AuthStrings.registerButton.translation(resources))
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
                        if (registerMode) AuthStrings.registerButton.translation(resources)
                        else AuthStrings.loginButton.translation(resources)
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { viewModel.onUsernameChanged(it) },
                        placeholder = { Text(AuthStrings.usernamePlaceholder.translation(resources)) },
                        singleLine = true,
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { viewModel.onPasswordChanged(it) },
                        placeholder = { Text(AuthStrings.passwordPlaceholder.translation(resources)) },
                        singleLine = true,
                        enabled = !loading,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error) {
                        Text(
                            if (registerMode) AuthStrings.errorRegisterFailed.translation(resources)
                            else AuthStrings.errorLoginFailed.translation(resources)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        Button(
                            onClick = { viewModel.onCancelForm() },
                            enabled = !loading
                        ) { Text(AuthStrings.cancelButton.translation(resources)) }
                        if (registerMode) {
                            Button(
                                onClick = { viewModel.onRegister() },
                                enabled = loginEnabled
                            ) { Text(AuthStrings.submitRegisterButton.translation(resources)) }
                        } else {
                            Button(
                                onClick = { viewModel.onAuthorize() },
                                enabled = loginEnabled
                            ) { Text(AuthStrings.submitButton.translation(resources)) }
                        }
                    }
                }
            }
        }
    }
}
