package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Exercises token-bound form validation and completion outcomes without platform UI dependencies. */
@OptIn(ExperimentalCoroutinesApi::class)
class PasswordChangeViewModelTest {
    /** Fixed canonical v4 approval used to prove the ViewModel never rewrites route identity. */
    private val approvalId = DeepLinkId("123e4567-e89b-42d3-a456-426614174000")

    /** Pending configuration used by tests that submit a real immutable subject and approval. */
    private val pendingConfig = PasswordChangeViewConfig.Pending(UserId(7L), approvalId)

    /** A matching acceptable submission sends the exact route values and retires entered passwords. */
    @Test
    fun matchingSubmissionUsesExactApprovalAndClearsSensitiveFields() = runTest {
        val model = UserEditTestUsersModel(
            initialUserId = null,
            initialProfile = AuthFeatureUser(UserId(7L), Username("owner"), email = null),
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

    /** A rejected approval becomes terminal and suppresses accidental replay from the same page. */
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

    /** A completed route carries no approval and cannot issue a completion transport request. */
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
}
