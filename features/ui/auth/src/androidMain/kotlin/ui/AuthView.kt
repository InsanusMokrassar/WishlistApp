package project_group.project_name.features.ui.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import project_group.project_name.features.common.client.models.ViewConfig
import project_group.project_name.features.ui.auth.AuthStrings

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
        val username by viewModel.usernameState.collectAsState()
        val password by viewModel.passwordState.collectAsState()
        val address by viewModel.addressState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val error by viewModel.errorState.collectAsState()
        val loginEnabled by viewModel.loginEnabledState.collectAsState()

        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(AuthStrings.title.translation(resources))
            OutlinedTextField(
                value = address,
                onValueChange = { viewModel.onAddressChanged(it) },
                placeholder = { Text(AuthStrings.serverAddressPlaceholder.translation(resources)) },
                singleLine = true,
                enabled = !loading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
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
                Text(AuthStrings.errorLoginFailed.translation(resources))
            }
            Button(
                onClick = { viewModel.onAuthorize() },
                enabled = loginEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(AuthStrings.loginButton.translation(resources))
            }
        }
    }
}
