package dev.inmo.wishlist.features.ui.users.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.CalmButton
import dev.inmo.wishlist.features.common.client.ui.components.CalmButtonVariant
import dev.inmo.wishlist.features.common.client.ui.components.CalmForm
import dev.inmo.wishlist.features.common.client.ui.components.CalmTextField
import dev.inmo.wishlist.features.common.client.ui.components.ContentColumn
import dev.inmo.wishlist.features.common.client.ui.components.FormHint
import dev.inmo.wishlist.features.common.client.ui.components.PageHead
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.users.UsersListStrings
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.onSubmit
import org.jetbrains.compose.web.dom.Form
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Calm Studio screen for a pending or completed email-authorized password change. */
class PasswordChangeView(
    chain: NavigationChain<ViewConfig>,
    config: PasswordChangeViewConfig,
) : ComposeView<PasswordChangeViewConfig, ViewConfig, PasswordChangeViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: PasswordChangeViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@PasswordChangeView)
    }

    override val title: String
        @Composable get() = UsersListStrings.passwordChangeTitle.translation()

    @Composable
    override fun onDraw() {
        super.onDraw()
        val password by viewModel.passwordState.collectAsState()
        val confirmation by viewModel.confirmationState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val canSubmit by viewModel.canSubmitState.collectAsState()
        val result by viewModel.resultState.collectAsState()
        val mismatch by viewModel.passwordsMismatchState.collectAsState()
        val invalidPassword by viewModel.passwordInvalidState.collectAsState()

        ContentColumn {
            if (viewModel.completedState) {
                PageHead(UsersListStrings.passwordChangeTitle.translation())
                FormHint(UsersListStrings.passwordChanged.translation())
                CalmButton(
                    text = UsersListStrings.continueButton.translation(),
                    onClick = viewModel::onContinue,
                    variant = CalmButtonVariant.Primary,
                )
                return@ContentColumn
            }

            // CalmForm is a layout primitive; native Form is the narrow exception required so Enter
            // follows the same guarded submission path as the visible submit button.
            Form(attrs = {
                onSubmit {
                    it.preventDefault()
                    viewModel.onSubmitPasswordChange()
                }
            }) {
                CalmForm {
                    PageHead(UsersListStrings.passwordChangeTitle.translation())
                    CalmTextField(
                        value = password,
                        onValueChange = viewModel::onPasswordChanged,
                        label = UsersListStrings.newPasswordLabel.translation(),
                        type = InputType.Password,
                        disabled = loading,
                        hint = UsersListStrings.passwordChangePolicy.translation(),
                        id = "password-change-password",
                    )
                    CalmTextField(
                        value = confirmation,
                        onValueChange = viewModel::onConfirmationChanged,
                        label = UsersListStrings.confirmPasswordLabel.translation(),
                        type = InputType.Password,
                        disabled = loading,
                        id = "password-change-confirmation",
                    )
                    passwordChangeHint(
                        result ?: when {
                            mismatch -> PasswordChangeSubmissionState.Mismatch
                            invalidPassword -> PasswordChangeSubmissionState.InvalidPassword
                            else -> null
                        },
                    )?.let { hint -> FormHint(hint.first, hint.second) }
                    CalmButton(
                        text = UsersListStrings.changePasswordButton.translation(),
                        onClick = {},
                        variant = CalmButtonVariant.Primary,
                        disabled = !canSubmit,
                        type = ButtonType.Submit,
                    )
                }
            }
        }
    }

    /** Maps presentation state to translated inline feedback and its error treatment. */
    @Composable
    private fun passwordChangeHint(result: PasswordChangeSubmissionState?): Pair<String, Boolean>? = when (result) {
        PasswordChangeSubmissionState.Mismatch -> UsersListStrings.passwordMismatch.translation() to true
        PasswordChangeSubmissionState.InvalidPassword -> UsersListStrings.passwordChangeInvalidPassword.translation() to true
        PasswordChangeSubmissionState.InvalidApproval -> UsersListStrings.passwordChangeInvalidApproval.translation() to true
        PasswordChangeSubmissionState.Unconfirmed -> UsersListStrings.passwordChangeUnconfirmed.translation() to true
        null -> null
    }
}
