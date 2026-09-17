package dev.inmo.wishlist.features.ui.users.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.semantics.SemanticsActions
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
import dev.inmo.wishlist.features.ui.users.UsersListStrings
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
            email = Email("approved@example.com"),
            emailApproved = true,
            pendingEmail = Email("pending@example.com"),
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
                onNodeWithTag("settings-email-pending").assertExists().assertIsDisplayed()
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

                assertTrue(viewModel.canManageOwnEmailState.value)
                assertEquals(savedEmail, viewModel.ownEmailProfileState.value?.email)
                assertEquals("draft@example.com", viewModel.emailInputState.value)
                assertEquals(EmailVerificationRequestResult.Sent, viewModel.emailVerificationResultState.value)

                runOnUiThread { node.retarget(otherId) }
                awaitIdle()

                assertTrue(viewModel.canManageOwnEmailState.value)
                assertEquals(savedEmail, viewModel.ownEmailProfileState.value?.email)
                assertEquals("draft@example.com", viewModel.emailInputState.value)
                assertEquals(EmailVerificationRequestResult.Sent, viewModel.emailVerificationResultState.value)
                onNodeWithTag("settings-email-saved").assertDoesNotExist()
                onNodeWithTag("settings-email").assertDoesNotExist()
                onNodeWithText(savedEmail.string).assertDoesNotExist()
                onNodeWithText(UsersListStrings.emailPendingApproval.translation()).assertDoesNotExist()
                onNodeWithText(UsersListStrings.emailReplacementNeedsVerification.translation()).assertDoesNotExist()
                onNodeWithText(UsersListStrings.saveEmailAndVerifyButton.translation()).assertDoesNotExist()
                onNodeWithText(UsersListStrings.resendEmailVerificationButton.translation()).assertDoesNotExist()
                onNodeWithText(UsersListStrings.refreshEmailButton.translation()).assertDoesNotExist()
                onNodeWithText(UsersListStrings.emailVerificationSent.translation()).assertDoesNotExist()
            }
        } finally {
            viewModel.scope.cancel()
        }
    }

    /** Keeps an approved saved address distinct from a replacement draft through the real save widget. */
    @Test
    fun savedApprovedAddressRemainsSeparateFromReplacementDraft() {
        val ownerId = UserId(7L)
        val savedEmail = Email("saved@example.com")
        val replacementEmail = Email("replacement@example.com")
        val compositionScheduler = TestCoroutineScheduler()
        val testBodyScheduler = TestCoroutineScheduler()
        val viewModelScheduler = TestCoroutineScheduler()
        val node = userEditTestNode(ownerId)
        val model = UserEditTestUsersModel(
            ownerId,
            AuthFeatureUser(ownerId, Username("owner"), email = savedEmail, emailApproved = true),
        )
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
                setContent { MaterialTheme { Column { OwnerEmailEditor(viewModel, node) } } }
                runOnUiThread { viewModelScheduler.advanceUntilIdle() }
                awaitIdle()
                onNodeWithTag("settings-email-saved").assertExists()
                onNodeWithText(UsersListStrings.emailApproved.translation()).assertExists()

                model.emailEvents.clear()
                onNodeWithTag("settings-email").performTextReplacement(replacementEmail.string)
                runOnUiThread { viewModelScheduler.runCurrent() }
                awaitIdle()
                onNodeWithText(savedEmail.string).assertExists()
                onNodeWithText(UsersListStrings.saveEmailAndVerifyButton.translation()).performClick()
                runOnUiThread { viewModelScheduler.advanceUntilIdle() }
                awaitIdle()

                assertEquals(
                    listOf("PUT:${replacementEmail.string}", "GET", "POST:${replacementEmail.string}", "GET"),
                    model.emailEvents,
                )
                assertEquals(replacementEmail, viewModel.ownEmailProfileState.value?.email)
                assertFalse(viewModel.ownEmailProfileState.value?.emailApproved ?: true)
            }
        } finally {
            viewModel.scope.cancel()
        }
    }

    /** Keeps a pending saved address distinct from a replacement draft through the real save widget. */
    @Test
    fun savedPendingAddressRemainsSeparateFromReplacementDraft() {
        val ownerId = UserId(7L)
        val savedEmail = Email("saved@example.com")
        val replacementEmail = Email("replacement@example.com")
        val compositionScheduler = TestCoroutineScheduler()
        val testBodyScheduler = TestCoroutineScheduler()
        val viewModelScheduler = TestCoroutineScheduler()
        val node = userEditTestNode(ownerId)
        val model = UserEditTestUsersModel(
            ownerId,
            AuthFeatureUser(ownerId, Username("owner"), email = savedEmail, emailApproved = false),
        )
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
                setContent { MaterialTheme { Column { OwnerEmailEditor(viewModel, node) } } }
                runOnUiThread { viewModelScheduler.advanceUntilIdle() }
                awaitIdle()
                onNodeWithText(UsersListStrings.emailPendingApproval.translation()).assertExists()
                onNodeWithTag("settings-email").performTextReplacement(replacementEmail.string)
                runOnUiThread { viewModelScheduler.runCurrent() }
                awaitIdle()
                onNodeWithText(savedEmail.string).assertExists()
                onNodeWithText(UsersListStrings.saveEmailAndVerifyButton.translation()).performClick()
                runOnUiThread { viewModelScheduler.advanceUntilIdle() }
                awaitIdle()

                assertEquals(listOf<Email?>(replacementEmail), model.savedEmails)
                assertEquals(listOf(replacementEmail), model.requestedEmails)
                assertEquals(replacementEmail, viewModel.ownEmailProfileState.value?.email)
                assertFalse(viewModel.ownEmailProfileState.value?.emailApproved ?: true)
            }
        } finally {
            viewModel.scope.cancel()
        }
    }

    /** Shows immediate invalid-input feedback through native desktop text semantics without submission. */
    @Test
    fun typingInvalidEmailShowsFeedbackWithoutSubmitting() {
        val ownerId = UserId(7L)
        val compositionScheduler = TestCoroutineScheduler()
        val testBodyScheduler = TestCoroutineScheduler()
        val viewModelScheduler = TestCoroutineScheduler()
        val node = userEditTestNode(ownerId)
        val model = UserEditTestUsersModel(ownerId, AuthFeatureUser(ownerId, Username("owner"), email = null))
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
                setContent { MaterialTheme { Column { OwnerEmailEditor(viewModel, node) } } }
                runOnUiThread { viewModelScheduler.advanceUntilIdle() }
                awaitIdle()
                onNodeWithTag("settings-email").performTextReplacement("partial-address")
                awaitIdle()

                onNodeWithText(UsersListStrings.emailInvalid.translation()).assertExists()
                onNodeWithText(UsersListStrings.saveEmailAndVerifyButton.translation()).assertIsNotEnabled()
                assertTrue(model.savedEmails.isEmpty())
                assertTrue(model.requestedEmails.isEmpty())

                onNodeWithTag("settings-email").performTextReplacement("replacement@example.com")
                awaitIdle()
                onNodeWithText(UsersListStrings.emailInvalid.translation()).assertDoesNotExist()
            }
        } finally {
            viewModel.scope.cancel()
        }
    }

    /** Proves a root editor for another account receives no private owner-email semantics. */
    @Test
    fun rootEditingAnotherUserHasNoPrivateEmailPanel() {
        val ownerId = UserId(7L)
        val rootId = UserId(1L)
        val compositionScheduler = TestCoroutineScheduler()
        val testBodyScheduler = TestCoroutineScheduler()
        val viewModelScheduler = TestCoroutineScheduler()
        val node = userEditTestNode(ownerId)
        val model = UserEditTestUsersModel(
            rootId,
            AuthFeatureUser(ownerId, Username("owner"), email = Email("private@example.com"), emailApproved = true),
        ).apply {
            rootState.value = true
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
                setContent { MaterialTheme { Column { OwnerEmailEditor(viewModel, node) } } }
                runOnUiThread { viewModelScheduler.advanceUntilIdle() }
                awaitIdle()

                onNodeWithTag("settings-email-saved").assertDoesNotExist()
                onNodeWithTag("settings-email").assertDoesNotExist()
                onNodeWithText("private@example.com").assertDoesNotExist()
                onNodeWithText(UsersListStrings.refreshEmailButton.translation()).assertDoesNotExist()
                onNodeWithText(UsersListStrings.emailOperationInterrupted.translation()).assertDoesNotExist()
                assertTrue(model.emailEvents.isEmpty())
                assertTrue(model.savedEmails.isEmpty())
                assertTrue(model.requestedEmails.isEmpty())
            }
        } finally {
            viewModel.scope.cancel()
        }
    }

    /** Covers missing, pending, and approved profiles under confirmed save-only delivery capability. */
    @Test
    fun disabledDeliveryGuidanceCoversEverySavedProfileState() {
        listOf<AuthFeatureUser?>(
            AuthFeatureUser(UserId(7L), Username("owner"), email = null),
            AuthFeatureUser(UserId(7L), Username("owner"), email = Email("pending@example.com"), emailApproved = false),
            AuthFeatureUser(UserId(7L), Username("owner"), email = Email("approved@example.com"), emailApproved = true),
        ).forEach { profile ->
            val ownerId = UserId(7L)
            val compositionScheduler = TestCoroutineScheduler()
            val testBodyScheduler = TestCoroutineScheduler()
            val viewModelScheduler = TestCoroutineScheduler()
            val node = userEditTestNode(ownerId)
            val model = UserEditTestUsersModel(ownerId, profile).apply { emailFeatureEnabled = false }
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
                    setContent { MaterialTheme { Column { OwnerEmailEditor(viewModel, node) } } }
                    runOnUiThread { viewModelScheduler.advanceUntilIdle() }
                    awaitIdle()

                    onNodeWithText(UsersListStrings.emailVerificationUnavailable.translation()).assertExists()
                    onNodeWithText(UsersListStrings.saveEmailButton.translation()).assertExists()
                    onNodeWithTag("settings-email").assertExists()
                    onNodeWithText(UsersListStrings.resendEmailVerificationButton.translation()).assertDoesNotExist()
                    assertTrue(model.requestedEmails.isEmpty())
                }
            } finally {
                viewModel.scope.cancel()
            }
        }
    }

    /** Keeps a failed storage attempt visible in widgets through a later real refresh. */
    @Test
    fun failedSaveKeepsDraftAndVisibleFailure() {
        val ownerId = UserId(7L)
        val savedEmail = Email("saved@example.com")
        val replacementEmail = Email("replacement@example.com")
        val compositionScheduler = TestCoroutineScheduler()
        val testBodyScheduler = TestCoroutineScheduler()
        val viewModelScheduler = TestCoroutineScheduler()
        val node = userEditTestNode(ownerId)
        val model = UserEditTestUsersModel(
            ownerId,
            AuthFeatureUser(ownerId, Username("owner"), email = savedEmail, emailApproved = true),
        ).apply { saveEmailResult = false }
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
                setContent { MaterialTheme { Column { OwnerEmailEditor(viewModel, node) } } }
                runOnUiThread { viewModelScheduler.advanceUntilIdle() }
                awaitIdle()
                onNodeWithTag("settings-email").performTextReplacement(replacementEmail.string)
                runOnUiThread { viewModelScheduler.runCurrent() }
                awaitIdle()
                onNodeWithText(UsersListStrings.saveEmailAndVerifyButton.translation()).performClick()
                runOnUiThread { viewModelScheduler.advanceUntilIdle() }
                awaitIdle()

                onNodeWithText(UsersListStrings.emailSaveFailed.translation()).assertExists()
                onNodeWithText(savedEmail.string).assertExists()
                assertEquals(replacementEmail.string, viewModel.emailInputState.value)
                assertNull(viewModel.emailSavedState.value)
                assertTrue(model.requestedEmails.isEmpty())

                onNodeWithText(UsersListStrings.refreshEmailButton.translation()).performClick()
                runOnUiThread { viewModelScheduler.advanceUntilIdle() }
                awaitIdle()
                onNodeWithText(UsersListStrings.emailSaveFailed.translation()).assertExists()
                assertEquals(replacementEmail.string, viewModel.emailInputState.value)
            }
        } finally {
            viewModel.scope.cancel()
        }
    }

    /** Verifies resend targets the saved pending address while retaining a separate replacement draft. */
    @Test
    fun resendUsesSavedAddressBesideReplacementDraft() {
        val ownerId = UserId(7L)
        val savedEmail = Email("saved@example.com")
        val replacementEmail = Email("replacement@example.com")
        val compositionScheduler = TestCoroutineScheduler()
        val testBodyScheduler = TestCoroutineScheduler()
        val viewModelScheduler = TestCoroutineScheduler()
        val node = userEditTestNode(ownerId)
        val model = UserEditTestUsersModel(
            ownerId,
            AuthFeatureUser(ownerId, Username("owner"), email = savedEmail, emailApproved = false),
        )
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
                setContent { MaterialTheme { Column { OwnerEmailEditor(viewModel, node) } } }
                runOnUiThread { viewModelScheduler.advanceUntilIdle() }
                awaitIdle()
                model.emailEvents.clear()
                onNodeWithTag("settings-email").performTextReplacement(replacementEmail.string)
                awaitIdle()
                onNodeWithText(UsersListStrings.resendEmailVerificationButton.translation()).performClick()
                runOnUiThread { viewModelScheduler.advanceUntilIdle() }
                awaitIdle()

                assertEquals(listOf("POST:${savedEmail.string}", "GET"), model.emailEvents)
                assertTrue(model.savedEmails.isEmpty())
                assertEquals(replacementEmail.string, viewModel.emailInputState.value)
                onNodeWithText(UsersListStrings.emailVerificationSent.translation()).assertExists()
            }
        } finally {
            viewModel.scope.cancel()
        }
    }

    /** Removes a legitimately rendered already-verified result after the real Refresh action sees pending state. */
    @Test
    fun laterPendingRefreshRemovesAlreadyVerifiedCopy() {
        val ownerId = UserId(7L)
        val savedEmail = Email("saved@example.com")
        val compositionScheduler = TestCoroutineScheduler()
        val testBodyScheduler = TestCoroutineScheduler()
        val viewModelScheduler = TestCoroutineScheduler()
        val node = userEditTestNode(ownerId)
        val model = UserEditTestUsersModel(
            ownerId,
            AuthFeatureUser(ownerId, Username("owner"), email = savedEmail, emailApproved = false),
        ).apply {
            requestHandler = {
                profileState.value = profileState.value?.copy(emailApproved = true)
                EmailVerificationRequestResult.AlreadyApproved
            }
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
                setContent { MaterialTheme { Column { OwnerEmailEditor(viewModel, node) } } }
                runOnUiThread { viewModelScheduler.advanceUntilIdle() }
                awaitIdle()
                onNodeWithText(UsersListStrings.resendEmailVerificationButton.translation()).performClick()
                runOnUiThread { viewModelScheduler.advanceUntilIdle() }
                awaitIdle()
                onNodeWithText(UsersListStrings.emailVerificationAlreadyApproved.translation()).assertExists()

                model.profileState.value = model.profileState.value?.copy(emailApproved = false)
                onNodeWithText(UsersListStrings.refreshEmailButton.translation()).performClick()
                runOnUiThread { viewModelScheduler.advanceUntilIdle() }
                awaitIdle()
                onNodeWithText(UsersListStrings.emailPendingApproval.translation()).assertExists()
                onNodeWithText(UsersListStrings.emailVerificationAlreadyApproved.translation()).assertDoesNotExist()
                assertNull(viewModel.emailVerificationResultState.value)
            }
        } finally {
            viewModel.scope.cancel()
        }
    }

    /** Exercises the production Done action for valid, invalid, blank, unchanged, and busy email drafts. */
    @Test
    fun imeDoneUsesGuardedEmailSave() {
        val ownerId = UserId(7L)
        val savedEmail = Email("saved@example.com")
        val firstReplacement = Email("replacement@example.com")
        val secondReplacement = Email("second@example.com")
        val putEntered = CompletableDeferred<Unit>()
        val releasePut = CompletableDeferred<Unit>()
        var suspendPut = false
        val compositionScheduler = TestCoroutineScheduler()
        val testBodyScheduler = TestCoroutineScheduler()
        val viewModelScheduler = TestCoroutineScheduler()
        val node = userEditTestNode(ownerId)
        val model = UserEditTestUsersModel(
            ownerId,
            AuthFeatureUser(ownerId, Username("owner"), email = savedEmail, emailApproved = true),
        ).apply {
            saveEmailHandler = { email ->
                profileState.value = profileState.value?.copy(email = email, emailApproved = false)
                if (suspendPut) {
                    putEntered.complete(Unit)
                    releasePut.await()
                }
                true
            }
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
                setContent { MaterialTheme { Column { OwnerEmailEditor(viewModel, node) } } }
                runOnUiThread { viewModelScheduler.advanceUntilIdle() }
                awaitIdle()
                model.emailEvents.clear()
                onNodeWithTag("settings-email").performTextReplacement(firstReplacement.string)
                awaitIdle()
                onNodeWithTag("settings-email").performImeAction()
                runOnUiThread { viewModelScheduler.advanceUntilIdle() }
                awaitIdle()
                assertEquals(1, model.savedEmails.size)
                assertEquals(firstReplacement, model.savedEmails.single())

                onNodeWithTag("settings-email").performTextReplacement("partial-address")
                awaitIdle()
                onNodeWithTag("settings-email").performImeAction()
                onNodeWithTag("settings-email").performTextReplacement("")
                awaitIdle()
                onNodeWithTag("settings-email").performImeAction()
                onNodeWithTag("settings-email").performTextReplacement(firstReplacement.string)
                awaitIdle()
                onNodeWithTag("settings-email").performImeAction()
                runOnUiThread { viewModelScheduler.advanceUntilIdle() }
                awaitIdle()
                assertEquals(1, model.savedEmails.size)
                assertTrue(model.requestedEmails.size == 1)

                val capturedImeAction = onNodeWithTag("settings-email")
                    .fetchSemanticsNode()
                    .config[SemanticsActions.OnImeAction]
                    .action
                suspendPut = true
                onNodeWithTag("settings-email").performTextReplacement(secondReplacement.string)
                awaitIdle()
                onNodeWithTag("settings-email").performImeAction()
                runOnUiThread { viewModelScheduler.runCurrent() }
                assertTrue(putEntered.isCompleted)
                onNodeWithTag("settings-email").assertIsNotEnabled()
                runOnUiThread { capturedImeAction?.invoke() }
                runOnUiThread { viewModelScheduler.runCurrent() }
                assertEquals(2, model.savedEmails.size)
                releasePut.complete(Unit)
                runOnUiThread { viewModelScheduler.advanceUntilIdle() }
                awaitIdle()
            }
        } finally {
            releasePut.complete(Unit)
            viewModel.scope.cancel()
        }
    }
}
