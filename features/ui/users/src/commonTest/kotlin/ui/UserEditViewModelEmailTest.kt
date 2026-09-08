package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UserEditViewModelEmailTest {
    private val ownerId = UserId(7L)
    private val owner = AuthFeatureUser(ownerId, Username("owner"), email = null)

    @Test
    fun missingOwnerEmailIsSavedThenVerified() = runTest {
        val model = UserEditTestUsersModel(ownerId, owner)
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            assertTrue(viewModel.canManageOwnEmailState.value)
            assertFalse(viewModel.emailLoadingState.value)

            viewModel.onEmailChanged("owner@example.com")
            advanceUntilIdle()
            assertTrue(viewModel.isDirtyState.value)
            viewModel.onSaveEmailAndRequestVerification()

            advanceUntilIdle()
            assertEquals(EmailVerificationRequestResult.Sent, viewModel.emailVerificationResultState.value)
            assertEquals(listOf<Email?>(Email("owner@example.com")), model.savedEmails)
            assertEquals(listOf(Email("owner@example.com")), model.requestedEmails)
            assertFalse(viewModel.isDirtyState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun failedEmailSaveDoesNotRequestVerification() = runTest {
        val model = UserEditTestUsersModel(ownerId, owner).apply { saveEmailResult = false }
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            assertTrue(viewModel.canManageOwnEmailState.value)
            assertFalse(viewModel.emailLoadingState.value)

            viewModel.onEmailChanged("owner@example.com")
            viewModel.onSaveEmailAndRequestVerification()

            advanceUntilIdle()
            assertEquals(EmailEditorError.SaveFailed, viewModel.emailErrorState.value)
            assertEquals(listOf<Email?>(Email("owner@example.com")), model.savedEmails)
            assertTrue(model.requestedEmails.isEmpty())
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun pendingOwnerEmailCanRetryVerificationWithoutSavingAgain() = runTest {
        val pendingEmail = Email("owner@example.com")
        val model = UserEditTestUsersModel(ownerId, owner.copy(email = pendingEmail, emailApproved = false))
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            assertTrue(viewModel.canManageOwnEmailState.value)
            assertFalse(viewModel.emailLoadingState.value)
            assertEquals(pendingEmail, viewModel.ownEmailProfileState.value?.email)

            viewModel.onResendEmailVerification()

            advanceUntilIdle()
            assertEquals(EmailVerificationRequestResult.Sent, viewModel.emailVerificationResultState.value)
            assertTrue(model.savedEmails.isEmpty())
            assertEquals(listOf(pendingEmail), model.requestedEmails)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun queuedSaveRejectsChangedOwner() = runTest {
        val model = UserEditTestUsersModel(ownerId, owner)
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            viewModel.onEmailChanged("owner@example.com")
            viewModel.onSaveEmailAndRequestVerification()
            model.currentUserIdState.value = UserId(8L)

            advanceUntilIdle()

            assertTrue(model.savedEmails.isEmpty())
            assertTrue(model.requestedEmails.isEmpty())
            assertNull(viewModel.ownEmailProfileState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun identityLossDuringPutPreventsPostAndRefresh() = runTest {
        val putEntered = CompletableDeferred<Unit>()
        val releasePut = CompletableDeferred<Unit>()
        val model = UserEditTestUsersModel(ownerId, owner).apply {
            saveEmailHandler = { email ->
                profileState.value = profileState.value?.copy(email = email, emailApproved = false)
                putEntered.complete(Unit)
                withContext(NonCancellable) { releasePut.await() }
                true
            }
        }
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            val readsBeforeSave = model.profileReads
            viewModel.onEmailChanged("owner@example.com")
            viewModel.onSaveEmailAndRequestVerification()
            runCurrent()
            assertTrue(putEntered.isCompleted)

            model.currentUserIdState.value = UserId(8L)
            runCurrent()
            releasePut.complete(Unit)
            advanceUntilIdle()

            assertTrue(model.requestedEmails.isEmpty())
            assertEquals(readsBeforeSave, model.profileReads)
            assertNull(viewModel.ownEmailProfileState.value)
            assertFalse(viewModel.emailBusyState.value)
        } finally {
            releasePut.complete(Unit)
            viewModel.scope.cancel()
        }
    }

    @Test
    fun logoutDuringPostSuppressesOldResult() = runTest {
        val pendingEmail = Email("owner@example.com")
        val postEntered = CompletableDeferred<Unit>()
        val releasePost = CompletableDeferred<Unit>()
        val model = UserEditTestUsersModel(ownerId, owner.copy(email = pendingEmail, emailApproved = false)).apply {
            requestHandler = {
                postEntered.complete(Unit)
                withContext(NonCancellable) { releasePost.await() }
                EmailVerificationRequestResult.Sent
            }
        }
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            viewModel.onResendEmailVerification()
            runCurrent()
            assertTrue(postEntered.isCompleted)

            model.authorisedState.value = false
            model.currentUserIdState.value = null
            runCurrent()
            releasePost.complete(Unit)
            advanceUntilIdle()

            assertNull(viewModel.emailVerificationResultState.value)
            assertNull(viewModel.ownEmailProfileState.value)
            assertFalse(viewModel.emailBusyState.value)
        } finally {
            releasePost.complete(Unit)
            viewModel.scope.cancel()
        }
    }

    @Test
    fun ownerLeaveAndReturnDoesNotReviveOperation() = runTest {
        val putEntered = CompletableDeferred<Unit>()
        val releasePut = CompletableDeferred<Unit>()
        val model = UserEditTestUsersModel(ownerId, owner).apply {
            saveEmailHandler = { email ->
                profileState.value = profileState.value?.copy(email = email, emailApproved = false)
                putEntered.complete(Unit)
                withContext(NonCancellable) { releasePut.await() }
                true
            }
        }
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            viewModel.onEmailChanged("owner@example.com")
            viewModel.onSaveEmailAndRequestVerification()
            runCurrent()
            assertTrue(putEntered.isCompleted)

            model.currentUserIdState.value = UserId(8L)
            runCurrent()
            model.currentUserIdState.value = ownerId
            advanceUntilIdle()
            releasePut.complete(Unit)
            advanceUntilIdle()

            assertTrue(model.requestedEmails.isEmpty())
            assertNull(viewModel.emailVerificationResultState.value)
            assertEquals(ownerId, viewModel.ownEmailProfileState.value?.id)
        } finally {
            releasePut.complete(Unit)
            viewModel.scope.cancel()
        }
    }

    @Test
    fun lateCancelledRefreshCannotPublish() = runTest {
        val probeEntered = CompletableDeferred<Unit>()
        val releaseProbe = CompletableDeferred<Unit>()
        val model = UserEditTestUsersModel(ownerId, owner).apply {
            probeHandler = {
                probeEntered.complete(Unit)
                withContext(NonCancellable) { releaseProbe.await() }
                true
            }
        }
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            runCurrent()
            assertTrue(probeEntered.isCompleted)

            model.currentUserIdState.value = UserId(8L)
            runCurrent()
            releaseProbe.complete(Unit)
            advanceUntilIdle()

            assertEquals(EmailCapabilityState.Unknown, viewModel.emailCapabilityState.value)
            assertNull(viewModel.ownEmailProfileState.value)
            assertFalse(viewModel.emailLoadingState.value)
        } finally {
            releaseProbe.complete(Unit)
            viewModel.scope.cancel()
        }
    }

    @Test
    fun oldFinallyCannotClearNewBusy() = runTest {
        val pendingEmail = Email("owner@example.com")
        val firstRequestEntered = CompletableDeferred<Unit>()
        val releaseFirstRequest = CompletableDeferred<Unit>()
        val secondRequestEntered = CompletableDeferred<Unit>()
        val releaseSecondRequest = CompletableDeferred<Unit>()
        var requestCount = 0
        val model = UserEditTestUsersModel(ownerId, owner.copy(email = pendingEmail, emailApproved = false)).apply {
            requestHandler = {
                requestCount += 1
                when (requestCount) {
                    1 -> {
                        firstRequestEntered.complete(Unit)
                        withContext(NonCancellable) { releaseFirstRequest.await() }
                    }
                    else -> {
                        secondRequestEntered.complete(Unit)
                        withContext(NonCancellable) { releaseSecondRequest.await() }
                    }
                }
                EmailVerificationRequestResult.Sent
            }
        }
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            viewModel.onResendEmailVerification()
            runCurrent()
            assertTrue(firstRequestEntered.isCompleted)

            model.currentUserIdState.value = UserId(8L)
            runCurrent()
            model.currentUserIdState.value = ownerId
            advanceUntilIdle()
            assertTrue(viewModel.canMutateOwnEmailState.value)

            viewModel.onResendEmailVerification()
            runCurrent()
            assertTrue(secondRequestEntered.isCompleted)
            assertTrue(viewModel.emailBusyState.value)

            releaseFirstRequest.complete(Unit)
            runCurrent()
            assertTrue(viewModel.emailBusyState.value)

            releaseSecondRequest.complete(Unit)
            advanceUntilIdle()
            assertFalse(viewModel.emailBusyState.value)
        } finally {
            releaseFirstRequest.complete(Unit)
            releaseSecondRequest.complete(Unit)
            viewModel.scope.cancel()
        }
    }

    @Test
    fun putThrowBeforeCommitKeepsDraftAndNeverPosts() = runTest {
        val model = UserEditTestUsersModel(ownerId, owner).apply {
            saveEmailHandler = { throw IllegalStateException("transport uncertain") }
        }
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            viewModel.onEmailChanged("owner@example.com")
            viewModel.onSaveEmailAndRequestVerification()
            advanceUntilIdle()

            assertEquals(EmailEditorError.SaveFailed, viewModel.emailErrorState.value)
            assertNull(viewModel.emailVerificationResultState.value)
            assertEquals("owner@example.com", viewModel.emailInputState.value)
            assertTrue(viewModel.isDirtyState.value)
            assertTrue(model.requestedEmails.isEmpty())
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun putThrowAfterCommitReconcilesPendingWithoutPosting() = runTest {
        val email = Email("owner@example.com")
        val model = UserEditTestUsersModel(ownerId, owner).apply {
            saveEmailHandler = { savedEmail ->
                profileState.value = profileState.value?.copy(email = savedEmail, emailApproved = false)
                throw IllegalStateException("response lost after commit")
            }
        }
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            viewModel.onEmailChanged(email.string)
            viewModel.onSaveEmailAndRequestVerification()
            advanceUntilIdle()

            assertEquals(EmailEditorError.SaveFailed, viewModel.emailErrorState.value)
            assertEquals(email, viewModel.ownEmailProfileState.value?.email)
            assertTrue(model.requestedEmails.isEmpty())

            viewModel.onResendEmailVerification()
            advanceUntilIdle()
            assertEquals(listOf(email), model.requestedEmails)
            assertEquals(listOf<Email?>(email), model.savedEmails)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun falsePutReportsSaveFailure() = runTest {
        val model = UserEditTestUsersModel(ownerId, owner).apply { saveEmailResult = false }
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            viewModel.onEmailChanged("owner@example.com")
            viewModel.onSaveEmailAndRequestVerification()
            advanceUntilIdle()

            assertEquals(EmailEditorError.SaveFailed, viewModel.emailErrorState.value)
            assertNull(viewModel.emailVerificationResultState.value)
            assertTrue(model.requestedEmails.isEmpty())
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun postThrowAfterConfirmedSaveReportsDeliveryFailure() = runTest {
        val model = UserEditTestUsersModel(ownerId, owner).apply {
            requestHandler = { throw IllegalStateException("SMTP unavailable") }
        }
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            viewModel.onEmailChanged("owner@example.com")
            viewModel.onSaveEmailAndRequestVerification()
            advanceUntilIdle()

            assertNull(viewModel.emailErrorState.value)
            assertEquals(EmailVerificationRequestResult.DeliveryFailed, viewModel.emailVerificationResultState.value)
            assertEquals(listOf(Email("owner@example.com")), model.requestedEmails)
            assertEquals(Email("owner@example.com"), viewModel.ownEmailProfileState.value?.email)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun secondaryRefreshFailurePreservesPrimaryError() = runTest {
        var failProfileRead = false
        val model = UserEditTestUsersModel(ownerId, owner).apply {
            saveEmailResult = false
            profileHandler = {
                if (failProfileRead) throw IllegalStateException("private read unavailable")
                profileState.value
            }
        }
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            failProfileRead = true
            viewModel.onEmailChanged("owner@example.com")
            viewModel.onSaveEmailAndRequestVerification()
            advanceUntilIdle()

            assertEquals(EmailEditorError.SaveFailed, viewModel.emailErrorState.value)
            assertTrue(viewModel.emailLoadFailedState.value)
            assertNull(viewModel.emailVerificationResultState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun initialProbeFailureExposesRetryAndRecovers() = runTest {
        var probeFails = true
        val model = UserEditTestUsersModel(ownerId, owner).apply {
            probeHandler = {
                if (probeFails) throw IllegalStateException("probe unavailable")
                true
            }
        }
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            assertEquals(EmailCapabilityState.Failed, viewModel.emailCapabilityState.value)
            assertTrue(viewModel.canManageOwnEmailState.value)
            assertFalse(viewModel.canMutateOwnEmailState.value)
            assertTrue(viewModel.emailLoadFailedState.value)

            viewModel.onEmailChanged("owner@example.com")
            assertEquals("", viewModel.emailInputState.value)
            probeFails = false
            viewModel.onRefreshEmail()
            advanceUntilIdle()

            assertEquals(EmailCapabilityState.Enabled, viewModel.emailCapabilityState.value)
            assertEquals(ownerId, viewModel.ownEmailProfileState.value?.id)
            assertTrue(viewModel.canMutateOwnEmailState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun privateReadFailureExposesRetryAndPreservesDraft() = runTest {
        var profileFails = false
        val model = UserEditTestUsersModel(ownerId, owner).apply {
            profileHandler = {
                if (profileFails) throw IllegalStateException("private profile unavailable")
                profileState.value
            }
        }
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            viewModel.onEmailChanged("owner@example.com")
            profileFails = true
            viewModel.onRefreshEmail()
            advanceUntilIdle()

            assertEquals(EmailCapabilityState.Failed, viewModel.emailCapabilityState.value)
            assertTrue(viewModel.emailLoadFailedState.value)
            assertFalse(viewModel.canMutateOwnEmailState.value)
            assertEquals("owner@example.com", viewModel.emailInputState.value)

            profileFails = false
            viewModel.onRefreshEmail()
            advanceUntilIdle()
            assertEquals(EmailCapabilityState.Enabled, viewModel.emailCapabilityState.value)
            assertTrue(viewModel.canMutateOwnEmailState.value)
            assertEquals("owner@example.com", viewModel.emailInputState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun disabledProbeHidesSectionAndRejectsWrites() = runTest {
        val model = UserEditTestUsersModel(ownerId, owner).apply { emailFeatureEnabled = false }
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            assertEquals(EmailCapabilityState.Disabled, viewModel.emailCapabilityState.value)
            assertFalse(viewModel.canManageOwnEmailState.value)
            assertFalse(viewModel.canMutateOwnEmailState.value)

            viewModel.onEmailChanged("owner@example.com")
            viewModel.onSaveEmailAndRequestVerification()
            advanceUntilIdle()
            assertTrue(model.savedEmails.isEmpty())
            assertTrue(model.requestedEmails.isEmpty())
        } finally {
            viewModel.scope.cancel()
        }
    }
}
