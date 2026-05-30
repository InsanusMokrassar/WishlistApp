package dev.inmo.wishlist.features.ui.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
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
 * - Not authenticated + collapsed: single "Log in" button.
 * - Not authenticated + expanded: username/password fields + "Submit" / "Cancel".
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

        when {
            loggedIn -> {
                Button(
                    onClick = { viewModel.onLogout() },
                    enabled = !loading
                ) { Text(AuthStrings.logoutButton.translation()) }
            }
            !expanded -> {
                Button(onClick = { viewModel.onToggleForm() }) {
                    Text(AuthStrings.loginButton.translation())
                }
            }
            else -> {
                Row(
                    modifier = Modifier.padding(start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { viewModel.onUsernameChanged(it) },
                        placeholder = { Text(AuthStrings.usernamePlaceholder.translation()) },
                        singleLine = true,
                        enabled = !loading,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { viewModel.onPasswordChanged(it) },
                        placeholder = { Text(AuthStrings.passwordPlaceholder.translation()) },
                        singleLine = true,
                        enabled = !loading,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Button(
                        onClick = { viewModel.onAuthorize() },
                        enabled = loginEnabled
                    ) { Text(AuthStrings.submitButton.translation()) }
                    Button(
                        onClick = { viewModel.onToggleForm() },
                        enabled = !loading
                    ) { Text(AuthStrings.cancelButton.translation()) }
                    if (error) {
                        Text(AuthStrings.errorLoginFailed.translation())
                    }
                }
            }
        }
    }
}
