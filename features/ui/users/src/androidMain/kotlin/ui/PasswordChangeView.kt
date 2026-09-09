package dev.inmo.wishlist.features.ui.users.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.users.UsersListStrings
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** Android Material3 screen for a pending or completed email-authorized password change. */
class PasswordChangeView(
    chain: NavigationChain<ViewConfig>,
    config: PasswordChangeViewConfig,
) : ComposeView<PasswordChangeViewConfig, ViewConfig, PasswordChangeViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: PasswordChangeViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@PasswordChangeView)
    }

    override val title: String
        @Composable get() = UsersListStrings.passwordChangeTitle.translation(LocalResources.current)

    @Composable
    override fun onDraw() {
        super.onDraw()
        val resources = LocalResources.current
        val password by viewModel.passwordState.collectAsState()
        val confirmation by viewModel.confirmationState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val canSubmit by viewModel.canSubmitState.collectAsState()
        val result by viewModel.resultState.collectAsState()
        val mismatch by viewModel.passwordsMismatchState.collectAsState()
        val invalidPassword by viewModel.passwordInvalidState.collectAsState()
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BackButton(UsersListStrings.backButton.translation(resources)) { viewModel.onContinue() }
            Text(UsersListStrings.passwordChangeTitle.translation(resources), style = MaterialTheme.typography.headlineSmall)
            if (viewModel.completedState) {
                Text(UsersListStrings.passwordChanged.translation(resources))
                Button(onClick = viewModel::onContinue, modifier = Modifier.fillMaxWidth()) {
                    Text(UsersListStrings.continueButton.translation(resources))
                }
                return@Column
            }
            OutlinedTextField(
                value = password,
                onValueChange = viewModel::onPasswordChanged,
                label = { Text(UsersListStrings.newPasswordLabel.translation(resources)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                singleLine = true,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = confirmation,
                onValueChange = viewModel::onConfirmationChanged,
                label = { Text(UsersListStrings.confirmPasswordLabel.translation(resources)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.onSubmitPasswordChange() }),
                singleLine = true,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(UsersListStrings.passwordChangePolicy.translation(resources))
            passwordChangeMessage(result ?: when {
                mismatch -> PasswordChangeSubmissionState.Mismatch
                invalidPassword -> PasswordChangeSubmissionState.InvalidPassword
                else -> null
            })?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = viewModel::onSubmitPasswordChange,
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(UsersListStrings.changePasswordButton.translation(resources)) }
        }
    }

    /** Returns translated feedback for a non-null submission state. */
    @Composable
    private fun passwordChangeMessage(state: PasswordChangeSubmissionState?): String? = when (state) {
        PasswordChangeSubmissionState.Mismatch -> UsersListStrings.passwordMismatch.translation(LocalResources.current)
        PasswordChangeSubmissionState.InvalidPassword -> UsersListStrings.passwordChangeInvalidPassword.translation(LocalResources.current)
        PasswordChangeSubmissionState.InvalidApproval -> UsersListStrings.passwordChangeInvalidApproval.translation(LocalResources.current)
        PasswordChangeSubmissionState.Unconfirmed -> UsersListStrings.passwordChangeUnconfirmed.translation(LocalResources.current)
        null -> null
    }
}
