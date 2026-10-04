package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.users.common.models.EmailProfile
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Shared ViewModel tests for approval-bound password submission and secret clearing. */
@OptIn(ExperimentalCoroutinesApi::class)
class PasswordChangeViewModelTest {
    /** Fixed canonical v4 approval used to prove the ViewModel never rewrites route identity. */
    private val approvalId = DeepLinkId("123e4567-e89b-42d3-a456-426614174000")

    /** Pending configuration used by tests that submit a real immutable subject and approval. */
    private val pendingConfig = PasswordChangeViewConfig.Pending(UserId(7L), approvalId)

    /** Verifies matching input submits the exact approval and clears plaintext. */
    @Test
    fun matchingSubmissionUsesExactApprovalAndClearsSensitiveFields() = runTest {
        val model = UserEditTestUsersModel(
            initialUserId = null,
            initialProfile = EmailProfile(userId = 7L),
            initiallyAuthorised = false,
        )
        val interactor = RecordingPasswordChangeInteractor()
        val viewModel = PasswordChangeViewModel(
            passwordChangeTestNode(pendingConfig),
            model,
            interactor,
            StandardTestDispatcher(testScheduler),
        )
        try {
            runCurrent()
            viewModel.onPasswordChanged("new-password")
            viewModel.onConfirmationChanged("new-password")
            runCurrent()
            assertTrue(viewModel.canSubmitState.value)

            viewModel.onSubmitPasswordChange()
            advanceUntilIdle()

            assertEquals(1, model.passwordChangeRequests.size)
            assertEquals(pendingConfig.userId, model.passwordChangeRequests.single().userId)
            assertEquals(approvalId, model.passwordChangeRequests.single().approvalId)
            assertEquals(1, interactor.changedCalls)
            assertEquals("", viewModel.passwordState.value)
            assertEquals("", viewModel.confirmationState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }

    /** Mismatch and UTF-8-overlong input fail locally without spending the persisted approval. */
    /** Verifies invalid local input never submits an approval. */
    @Test
    fun invalidLocalInputNeverSubmitsApproval() = runTest {
        val model = UserEditTestUsersModel(null, null, initiallyAuthorised = false)
        val viewModel = PasswordChangeViewModel(
            passwordChangeTestNode(pendingConfig),
            model,
            RecordingPasswordChangeInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            viewModel.onPasswordChanged("different")
            viewModel.onConfirmationChanged("new-password")
            viewModel.onSubmitPasswordChange()
            assertEquals(PasswordChangeSubmissionState.Mismatch, viewModel.resultState.value)
            assertTrue(model.passwordChangeRequests.isEmpty())

            val oversizedUtf8 = "a".repeat(71) + "€"
            viewModel.onPasswordChanged(oversizedUtf8)
            viewModel.onConfirmationChanged(oversizedUtf8)
            viewModel.onSubmitPasswordChange()
            assertEquals(PasswordChangeSubmissionState.InvalidPassword, viewModel.resultState.value)
            assertTrue(model.passwordChangeRequests.isEmpty())
        } finally {
            viewModel.scope.cancel()
        }
    }

    /** Raw input state, rather than asynchronously derived presentation state, controls admission. */
    /** Verifies synchronous guards reject stale mismatch and duplicate clicks. */
    @Test
    fun synchronousSubmissionGuardRejectsStaleMismatchAndDuplicateClick() = runTest {
        val model = UserEditTestUsersModel(null, null, initiallyAuthorised = false)
        val viewModel = PasswordChangeViewModel(
            passwordChangeTestNode(pendingConfig),
            model,
            RecordingPasswordChangeInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            viewModel.onPasswordChanged("new-password")
            viewModel.onConfirmationChanged("different")
            viewModel.onSubmitPasswordChange()
            assertEquals(PasswordChangeSubmissionState.Mismatch, viewModel.resultState.value)
            assertTrue(model.passwordChangeRequests.isEmpty())

            viewModel.onConfirmationChanged("new-password")
            viewModel.onSubmitPasswordChange()
            viewModel.onSubmitPasswordChange()
            assertTrue(viewModel.loadingState.value)
            runCurrent()
            assertEquals(1, model.passwordChangeRequests.size)
        } finally {
            viewModel.scope.cancel()
        }
    }

    /** A pending server response holds the raw busy slot before dispatched work starts. */
    /** Verifies immediate double submission admits exactly one request. */
    @Test
    fun immediateDoubleSubmitAdmitsOneRequest() = runTest {
        val response = CompletableDeferred<PasswordChangeResult?>()
        val model = UserEditTestUsersModel(null, null, initiallyAuthorised = false).apply {
            passwordChangeHandler = { response.await() }
        }
        val interactor = RecordingPasswordChangeInteractor()
        val viewModel = PasswordChangeViewModel(
            passwordChangeTestNode(pendingConfig), model, interactor, StandardTestDispatcher(testScheduler),
        )
        try {
            viewModel.onPasswordChanged("new-password")
            viewModel.onConfirmationChanged("new-password")
            viewModel.onSubmitPasswordChange()
            viewModel.onSubmitPasswordChange()
            assertTrue(viewModel.loadingState.value)
            runCurrent()
            assertEquals(1, model.passwordChangeRequests.size)
            response.complete(PasswordChangeResult.Changed)
            advanceUntilIdle()
            assertEquals(1, interactor.changedCalls)
            viewModel.onSubmitPasswordChange()
            advanceUntilIdle()
            assertEquals(1, model.passwordChangeRequests.size)
            assertEquals(1, interactor.changedCalls)
        } finally {
            viewModel.scope.cancel()
        }
    }

    /** Raw admission accepts valid input before derived presentation state catches up and rejects stale policy state. */
    /** Verifies admission uses raw fields instead of lagging derived state. */
    @Test
    fun immediateAdmissionAndLocalValidationDoNotTrustDerivedSubmitState() = runTest {
        val model = UserEditTestUsersModel(null, null, initiallyAuthorised = false)
        val viewModel = PasswordChangeViewModel(
            passwordChangeTestNode(pendingConfig), model, RecordingPasswordChangeInteractor(), StandardTestDispatcher(testScheduler),
        )
        try {
            viewModel.onPasswordChanged("new-password")
            viewModel.onConfirmationChanged("new-password")
            viewModel.onSubmitPasswordChange()
            assertTrue(viewModel.loadingState.value)
            runCurrent()
            assertEquals(1, model.passwordChangeRequests.size)

            val staleModel = UserEditTestUsersModel(null, null, initiallyAuthorised = false)
            val staleViewModel = PasswordChangeViewModel(
                passwordChangeTestNode(pendingConfig), staleModel, RecordingPasswordChangeInteractor(), StandardTestDispatcher(testScheduler),
            )
            try {
                staleViewModel.onPasswordChanged("new-password")
                staleViewModel.onConfirmationChanged("new-password")
                runCurrent()
                assertTrue(staleViewModel.canSubmitState.value)
                val oversizedUtf8 = "a".repeat(71) + "€"
                staleViewModel.onPasswordChanged(oversizedUtf8)
                staleViewModel.onConfirmationChanged(oversizedUtf8)
                staleViewModel.onSubmitPasswordChange()
                assertEquals(PasswordChangeSubmissionState.InvalidPassword, staleViewModel.resultState.value)
                assertTrue(staleModel.passwordChangeRequests.isEmpty())
            } finally {
                staleViewModel.scope.cancel()
            }
        } finally {
            viewModel.scope.cancel()
        }
    }

    /** A rejected approval becomes terminal and suppresses accidental replay from the same page. */
    /** Verifies invalid approval becomes terminal and prevents duplicate submission. */
    @Test
    fun invalidApprovalPreventsDuplicateSubmission() = runTest {
        val model = UserEditTestUsersModel(null, null, initiallyAuthorised = false).apply {
            passwordChangeResult = PasswordChangeResult.InvalidApproval
        }
        val viewModel = PasswordChangeViewModel(
            passwordChangeTestNode(pendingConfig),
            model,
            RecordingPasswordChangeInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            runCurrent()
            viewModel.onPasswordChanged("new-password")
            viewModel.onConfirmationChanged("new-password")
            runCurrent()
            viewModel.onSubmitPasswordChange()
            advanceUntilIdle()

            assertTrue(viewModel.terminalInvalidApprovalState.value)
            assertEquals(PasswordChangeSubmissionState.InvalidApproval, viewModel.resultState.value)
            viewModel.onSubmitPasswordChange()
            advanceUntilIdle()
            assertEquals(1, model.passwordChangeRequests.size)
            assertFalse(viewModel.canSubmitState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }

    /** Unknown and denied responses clear secrets and permanently reject queued callbacks before safe exit. */
    @Test
    fun terminalResponsesStopRawReplayClearFieldsAndContinueWithoutSuccess() = runTest {
        listOf<PasswordChangeResult?>(null, PasswordChangeResult.InvalidApproval).forEach { outcome ->
            val response = CompletableDeferred<PasswordChangeResult?>()
            val model = UserEditTestUsersModel(null, null, initiallyAuthorised = false).apply {
                passwordChangeHandler = { response.await() }
            }
            val interactor = RecordingPasswordChangeInteractor()
            val viewModel = PasswordChangeViewModel(passwordChangeTestNode(pendingConfig), model, interactor, StandardTestDispatcher(testScheduler))
            try {
                viewModel.onPasswordChanged("new-password")
                viewModel.onConfirmationChanged("new-password")
                runCurrent()
                assertTrue(viewModel.canSubmitState.value)
                viewModel.onSubmitPasswordChange()
                runCurrent()
                response.complete(outcome)
                runCurrent()
                val expected = if (outcome == null) PasswordChangeSubmissionState.Unconfirmed else PasswordChangeSubmissionState.InvalidApproval
                assertEquals(expected, viewModel.resultState.value)
                assertEquals("", viewModel.passwordState.value)
                assertEquals("", viewModel.confirmationState.value)
                viewModel.onPasswordChanged("second-password")
                viewModel.onConfirmationChanged("second-password")
                viewModel.onSubmitPasswordChange()
                runCurrent()
                assertTrue(viewModel.submissionStoppedState.value)
                assertFalse(viewModel.canSubmitState.value)
                assertEquals(expected, viewModel.resultState.value)
                assertEquals("", viewModel.passwordState.value)
                viewModel.onSubmitPasswordChange()
                viewModel.onContinue()
                advanceUntilIdle()
                assertEquals(1, model.passwordChangeRequests.size)
                assertEquals(1, interactor.continueCalls)
                assertEquals(0, interactor.changedCalls)
            } finally { viewModel.scope.cancel() }
        }
    }

    /** A transport exception is unknown, while explicit policy rejection permits correction and retry. */
    @Test
    fun ordinaryTransportExceptionStopsButInvalidPasswordRemainsRetryable() = runTest {
        listOf(true, false).forEach { throws ->
            val model = UserEditTestUsersModel(null, null, initiallyAuthorised = false).apply {
                passwordChangeHandler = {
                    if (throws) throw IllegalStateException("transport")
                    PasswordChangeResult.InvalidPassword
                }
            }
            val viewModel = PasswordChangeViewModel(passwordChangeTestNode(pendingConfig), model, RecordingPasswordChangeInteractor(), StandardTestDispatcher(testScheduler))
            try {
                viewModel.onPasswordChanged("new-password")
                viewModel.onConfirmationChanged("new-password")
                viewModel.onSubmitPasswordChange()
                advanceUntilIdle()
                assertEquals(throws, viewModel.submissionStoppedState.value)
                assertFalse(viewModel.loadingState.value)
                viewModel.onPasswordChanged("second-password")
                viewModel.onConfirmationChanged("second-password")
                viewModel.onSubmitPasswordChange()
                advanceUntilIdle()
                assertEquals(if (throws) 1 else 2, model.passwordChangeRequests.size)
            } finally { viewModel.scope.cancel() }
        }
    }

    /** A completed route carries no approval and cannot issue a completion transport request. */
    /** Verifies completed configuration has no actionable credential form. */
    @Test
    fun completedConfigIsCredentialFreeAndNonActionable() = runTest {
        val model = UserEditTestUsersModel(null, null, initiallyAuthorised = false)
        val viewModel = PasswordChangeViewModel(
            passwordChangeTestNode(PasswordChangeViewConfig.Completed),
            model,
            RecordingPasswordChangeInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            viewModel.onPasswordChanged("new-password")
            viewModel.onConfirmationChanged("new-password")
            viewModel.onSubmitPasswordChange()

            assertTrue(viewModel.completedState)
            assertFalse(viewModel.canSubmitState.value)
            assertTrue(model.passwordChangeRequests.isEmpty())
            assertFalse(PasswordChangeViewConfig.Completed.toString().contains(approvalId.string))
        } finally {
            viewModel.scope.cancel()
        }
    }

    /** Destroying the pending node clears plaintext inputs even when no server response arrives. */
    /** Verifies ViewModel destruction clears in-memory password fields. */
    @Test
    fun destructionClearsInMemoryPasswordInputs() = runTest {
        val viewModel = PasswordChangeViewModel(
            passwordChangeTestNode(pendingConfig),
            UserEditTestUsersModel(null, null, initiallyAuthorised = false),
            RecordingPasswordChangeInteractor(),
            StandardTestDispatcher(testScheduler),
        )

        viewModel.onPasswordChanged("new-password")
        viewModel.onConfirmationChanged("new-password")
        val lifecycleJob = checkNotNull(viewModel.scope.coroutineContext[Job])
        viewModel.scope.cancel()
        lifecycleJob.join()

        assertEquals("", viewModel.passwordState.value)
        assertEquals("", viewModel.confirmationState.value)
    }
    /** English and Russian resources state uncertainty, a safe exit, and inbox checking without claiming failure. */
    @Test
    fun terminalAndEmailUncertaintyResourcesContainSelectedEnglishAndRussianCopy() {
        val strings = dev.inmo.wishlist.features.ui.users.UsersListStrings
        assertEquals("The password may have changed, but the result could not be confirmed. Do not submit this link again. Continue, then try signing in with the new password. If needed, request a new link from your profile.", strings.passwordChangeUnconfirmed.default)
        assertEquals("Пароль мог измениться, но результат не удалось подтвердить. Не отправляйте эту форму повторно. Нажмите «Продолжить» и попробуйте войти с новым паролем. При необходимости запросите новую ссылку в своём профиле.", strings.passwordChangeUnconfirmed.translation(dev.inmo.micro_utils.language_codes.IetfLang.Russian))
        assertEquals("The password-change email request could not be confirmed. Check your inbox before requesting another link.", strings.passwordChangeEmailUnconfirmed.default)
        assertEquals("Не удалось подтвердить запрос письма для смены пароля. Проверьте входящие, прежде чем запрашивать новую ссылку.", strings.passwordChangeEmailUnconfirmed.translation(dev.inmo.micro_utils.language_codes.IetfLang.Russian))
    }

    /** Cancellation propagates without publishing an outcome or automatically retrying the transport. */
    @Test
    fun completionCancellationClearsOnDestructionWithoutFalseOutcome() = runTest {
        val response = CompletableDeferred<PasswordChangeResult?>()
        val model = UserEditTestUsersModel(null, null, initiallyAuthorised = false).apply { passwordChangeHandler = { response.await() } }
        val interactor = RecordingPasswordChangeInteractor()
        val viewModel = PasswordChangeViewModel(passwordChangeTestNode(pendingConfig), model, interactor, StandardTestDispatcher(testScheduler))
        viewModel.onPasswordChanged("new-password")
        viewModel.onConfirmationChanged("new-password")
        viewModel.onSubmitPasswordChange()
        runCurrent()
        viewModel.scope.cancel()
        runCurrent()
        viewModel.scope.coroutineContext[Job]?.join()
        assertEquals("", viewModel.passwordState.value)
        assertEquals("", viewModel.confirmationState.value)
        kotlin.test.assertNull(viewModel.resultState.value)
        assertEquals(1, model.passwordChangeRequests.size)
        assertEquals(0, interactor.changedCalls)
    }

}
