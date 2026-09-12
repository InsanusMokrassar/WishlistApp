package dev.inmo.wishlist.features.ui.users.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
import dev.inmo.wishlist.features.ui.users.UsersListStrings
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlin.test.Test

/** Real desktop semantics coverage for the production [OwnerEmailEditor] boundary. */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)
class UserEditEmailRenderTest {
    /** Renders the production saved-address and editable replacement controls in the desktop host. */
    @Test
    fun ownerEmailPanelRendersOnDesktop() {
        val ownerId = UserId(7L)
        val owner = AuthFeatureUser(
            ownerId,
            Username("owner"),
            email = Email("saved@example.com"),
            emailApproved = false,
        )
        val compositionScheduler = TestCoroutineScheduler()
        val testBodyScheduler = TestCoroutineScheduler()
        val viewModelScheduler = TestCoroutineScheduler()
        val node = userEditTestNode(ownerId)
        val model = UserEditTestUsersModel(ownerId, owner)
        val viewModel = UserEditViewModel(
            node,
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(viewModelScheduler),
        )
        try {
            runDesktopComposeUiTest(
                width = 1024,
                height = 1200,
                effectContext = StandardTestDispatcher(compositionScheduler),
                runTestContext = StandardTestDispatcher(testBodyScheduler),
            ) {
                setContent {
                    MaterialTheme {
                        Column { OwnerEmailEditor(viewModel, node) }
                    }
                }
                runOnUiThread { viewModelScheduler.advanceUntilIdle() }
                awaitIdle()

                onNodeWithTag("settings-email-saved").assertExists().assertIsDisplayed()
                onNodeWithTag("settings-email").assertExists().assertIsDisplayed()
                onNodeWithText(UsersListStrings.emailPendingApproval.translation()).assertExists()
                onNodeWithText(UsersListStrings.refreshEmailButton.translation()).assertExists()
            }
        } finally {
            viewModel.scope.cancel()
        }
    }

    /** Shows confirmed Disabled delivery guidance exactly through the production owner panel. */
    @Test
    fun disabledDeliveryExplainsSaveOnlyStorage() {
        val ownerId = UserId(7L)
        val compositionScheduler = TestCoroutineScheduler()
        val testBodyScheduler = TestCoroutineScheduler()
        val viewModelScheduler = TestCoroutineScheduler()
        val node = userEditTestNode(ownerId)
        val model = UserEditTestUsersModel(
            ownerId,
            AuthFeatureUser(ownerId, Username("owner"), email = null),
        ).apply {
            emailFeatureEnabled = false
        }
        val viewModel = UserEditViewModel(
            node,
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(viewModelScheduler),
        )
        try {
            runDesktopComposeUiTest(
                width = 1024,
                height = 1200,
                effectContext = StandardTestDispatcher(compositionScheduler),
                runTestContext = StandardTestDispatcher(testBodyScheduler),
            ) {
                setContent {
                    MaterialTheme {
                        Column { OwnerEmailEditor(viewModel, node) }
                    }
                }
                runOnUiThread { viewModelScheduler.advanceUntilIdle() }
                awaitIdle()

                onNodeWithText(UsersListStrings.emailVerificationUnavailable.translation()).assertExists()
                onNodeWithText(UsersListStrings.saveEmailButton.translation()).assertExists()
                onNodeWithTag("settings-email").assertExists().assertIsDisplayed()
                onNodeWithText(UsersListStrings.resendEmailVerificationButton.translation()).assertDoesNotExist()
            }
        } finally {
            viewModel.scope.cancel()
        }
    }

    /** Removes all private widget semantics after the exact rendered node retargets before ViewModel work runs. */
    @Test
    fun liveRetargetHidesPrivateEmailBeforeOwnerCollectorRuns() {
        val ownerId = UserId(7L)
        val otherId = UserId(8L)
        val savedEmail = Email("saved@example.com")
        val compositionScheduler = TestCoroutineScheduler()
        val testBodyScheduler = TestCoroutineScheduler()
        val viewModelScheduler = TestCoroutineScheduler()
        val node = userEditTestNode(ownerId)
        val model = UserEditTestUsersModel(
            ownerId,
            AuthFeatureUser(ownerId, Username("owner"), email = savedEmail, emailApproved = false),
        ).apply {
            requestResult = EmailVerificationRequestResult.Sent
        }
        val viewModel = UserEditViewModel(
            node,
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(viewModelScheduler),
        )
        try {
            runDesktopComposeUiTest(
                width = 1024,
                height = 1200,
                effectContext = StandardTestDispatcher(compositionScheduler),
                runTestContext = StandardTestDispatcher(testBodyScheduler),
            ) {
                setContent {
                    MaterialTheme {
                        Column { OwnerEmailEditor(viewModel, node) }
                    }
                }
                runOnUiThread { viewModelScheduler.advanceUntilIdle() }
                awaitIdle()
                runOnUiThread {
                    viewModel.onEmailChanged("draft@example.com")
                    viewModel.onResendEmailVerification()
                    viewModelScheduler.advanceUntilIdle()
                }
                awaitIdle()
                onNodeWithTag("settings-email-saved").assertExists()
                onNodeWithTag("settings-email").assertExists()
                onNodeWithText(UsersListStrings.emailVerificationSent.translation()).assertExists()

                runOnUiThread { node.retarget(otherId) }
                awaitIdle()

                onNodeWithTag("settings-email-saved").assertDoesNotExist()
                onNodeWithTag("settings-email").assertDoesNotExist()
                onNodeWithText(savedEmail.string).assertDoesNotExist()
                onNodeWithText(UsersListStrings.refreshEmailButton.translation()).assertDoesNotExist()
                onNodeWithText(UsersListStrings.emailVerificationSent.translation()).assertDoesNotExist()
            }
        } finally {
            viewModel.scope.cancel()
        }
    }
}
