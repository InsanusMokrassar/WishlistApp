package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
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
            viewModel.onSaveEmail()

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
    fun approvedOwnerEmailCanBeReplacedAndVerified() = runTest {
        val approvedEmail = Email("approved@example.com")
        val replacementEmail = Email("replacement@example.com")
        val model = UserEditTestUsersModel(
            ownerId,
            owner.copy(email = approvedEmail, emailApproved = true),
        )
        val interactor = RecordingUserEditInteractor()
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            interactor,
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            assertTrue(viewModel.canManageOwnEmailState.value)
            assertTrue(viewModel.canMutateOwnEmailState.value)

            model.emailEvents.clear()
            viewModel.onEmailChanged(replacementEmail.string)
            viewModel.onSaveEmail()
            advanceUntilIdle()

            assertEquals(
                listOf("PUT:${replacementEmail.string}", "GET", "POST:${replacementEmail.string}", "GET"),
                model.emailEvents,
            )
            assertEquals(listOf<Email?>(replacementEmail), model.savedEmails)
            assertEquals(listOf(replacementEmail), model.requestedEmails)
            assertEquals(replacementEmail, viewModel.ownEmailProfileState.value?.email)
            assertFalse(viewModel.ownEmailProfileState.value?.emailApproved ?: true)
            assertEquals(replacementEmail, viewModel.emailSavedState.value)
            assertFalse(viewModel.isDirtyState.value)
            assertEquals(0, interactor.savedCalls)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun pendingOwnerEmailCanBeReplacedAndVerified() = runTest {
        val pendingEmail = Email("pending@example.com")
        val replacementEmail = Email("replacement@example.com")
        val model = UserEditTestUsersModel(
            ownerId,
            owner.copy(email = pendingEmail, emailApproved = false),
        )
        val interactor = RecordingUserEditInteractor()
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            interactor,
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            assertTrue(viewModel.canManageOwnEmailState.value)
            assertTrue(viewModel.canMutateOwnEmailState.value)

            model.emailEvents.clear()
            viewModel.onEmailChanged(replacementEmail.string)
            viewModel.onSaveEmail()
            advanceUntilIdle()

            assertEquals(
                listOf("PUT:${replacementEmail.string}", "GET", "POST:${replacementEmail.string}", "GET"),
                model.emailEvents,
            )
            assertEquals(listOf<Email?>(replacementEmail), model.savedEmails)
            assertEquals(listOf(replacementEmail), model.requestedEmails)
            assertEquals(replacementEmail, viewModel.ownEmailProfileState.value?.email)
            assertFalse(viewModel.ownEmailProfileState.value?.emailApproved ?: true)
            assertEquals(replacementEmail, viewModel.emailSavedState.value)
            assertFalse(viewModel.isDirtyState.value)
            assertEquals(0, interactor.savedCalls)
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
            viewModel.onSaveEmail()

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
        val replacementEmail = Email("replacement@example.com")
        val model = UserEditTestUsersModel(
            ownerId,
            owner.copy(email = Email("saved@example.com"), emailApproved = true),
        )
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            viewModel.onEmailChanged(replacementEmail.string)
            viewModel.onSaveEmail()
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
        val model = UserEditTestUsersModel(
            ownerId,
            owner.copy(email = Email("saved@example.com"), emailApproved = true),
        ).apply {
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
            viewModel.onEmailChanged("replacement@example.com")
            viewModel.onSaveEmail()
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
        val replacementEmail = Email("replacement@example.com")
        val model = UserEditTestUsersModel(
            ownerId,
            owner.copy(email = Email("saved@example.com"), emailApproved = true),
        ).apply {
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
            viewModel.onEmailChanged(replacementEmail.string)
            viewModel.onSaveEmail()
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
            assertNull(viewModel.emailSavedState.value)
            assertEquals(ownerId, viewModel.ownEmailProfileState.value?.id)
            assertEquals(replacementEmail, viewModel.ownEmailProfileState.value?.email)
            assertTrue(viewModel.emailOperationInterruptedState.value)
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

    /**
     * The production default is the UI dispatcher, so invalidation and a late non-cooperative
     * refresh continuation share one serial context and the stale continuation cannot publish.
     */
    @Test
    fun defaultUiDispatcherSuppressesLateRefreshAfterIdentityInvalidation() = runTest {
        val uiDispatcher = StandardTestDispatcher(testScheduler)
        val probeEntered = CompletableDeferred<Unit>()
        val releaseProbe = CompletableDeferred<Unit>()
        val model = UserEditTestUsersModel(ownerId, owner).apply {
            probeHandler = {
                probeEntered.complete(Unit)
                withContext(NonCancellable) { releaseProbe.await() }
                true
            }
        }
        Dispatchers.setMain(uiDispatcher)
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
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
            advanceUntilIdle()
            Dispatchers.resetMain()
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
    fun replacementThrowBeforeCommitPreservesDraft() = runTest {
        val approvedEmail = Email("approved@example.com")
        val replacementEmail = Email("replacement@example.com")
        val model = UserEditTestUsersModel(ownerId, owner.copy(email = approvedEmail, emailApproved = true)).apply {
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
            viewModel.onEmailChanged(replacementEmail.string)
            viewModel.onSaveEmail()
            advanceUntilIdle()

            assertEquals(EmailEditorError.SaveFailed, viewModel.emailErrorState.value)
            assertNull(viewModel.emailVerificationResultState.value)
            assertEquals(replacementEmail.string, viewModel.emailInputState.value)
            assertEquals(approvedEmail, viewModel.ownEmailProfileState.value?.email)
            assertTrue(viewModel.ownEmailProfileState.value?.emailApproved == true)
            assertTrue(viewModel.isDirtyState.value)
            assertTrue(model.requestedEmails.isEmpty())
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun replacementThrowAfterCommitDoesNotReportSuccess() = runTest {
        val approvedEmail = Email("approved@example.com")
        val email = Email("replacement@example.com")
        val model = UserEditTestUsersModel(ownerId, owner.copy(email = approvedEmail, emailApproved = true)).apply {
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
            viewModel.onSaveEmail()
            advanceUntilIdle()

            assertEquals(EmailEditorError.SaveFailed, viewModel.emailErrorState.value)
            assertEquals(email, viewModel.ownEmailProfileState.value?.email)
            assertFalse(viewModel.ownEmailProfileState.value?.emailApproved ?: true)
            assertEquals(email.string, viewModel.emailInputState.value)
            assertFalse(viewModel.isDirtyState.value)
            assertNull(viewModel.emailSavedState.value)
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
    fun falseReplacementPreservesDraftAndSaveFailure() = runTest {
        val approvedEmail = Email("approved@example.com")
        val replacementEmail = Email("replacement@example.com")
        val model = UserEditTestUsersModel(ownerId, owner.copy(email = approvedEmail, emailApproved = true)).apply {
            saveEmailResult = false
        }
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            viewModel.onEmailChanged(replacementEmail.string)
            viewModel.onSaveEmail()
            advanceUntilIdle()

            assertEquals(EmailEditorError.SaveFailed, viewModel.emailErrorState.value)
            assertNull(viewModel.emailVerificationResultState.value)
            assertEquals(replacementEmail.string, viewModel.emailInputState.value)
            assertEquals(approvedEmail, viewModel.ownEmailProfileState.value?.email)
            assertTrue(viewModel.ownEmailProfileState.value?.emailApproved == true)
            assertTrue(viewModel.isDirtyState.value)
            assertNull(viewModel.emailSavedState.value)
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
            viewModel.onSaveEmail()
            advanceUntilIdle()

            assertNull(viewModel.emailErrorState.value)
            assertEquals(EmailVerificationRequestResult.DeliveryFailed, viewModel.emailVerificationResultState.value)
            assertEquals(listOf(Email("owner@example.com")), model.requestedEmails)
            assertEquals(Email("owner@example.com"), viewModel.ownEmailProfileState.value?.email)
            assertEquals(Email("owner@example.com"), viewModel.emailSavedState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun failedReconciliationPreservesPrimaryErrorAndBlocksMutation() = runTest {
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
            viewModel.onSaveEmail()
            advanceUntilIdle()

            assertEquals(EmailEditorError.SaveFailed, viewModel.emailErrorState.value)
            assertTrue(viewModel.emailLoadFailedState.value)
            assertNull(viewModel.emailVerificationResultState.value)
            assertFalse(viewModel.canMutateOwnEmailState.value)
            val savesAfterFailure = model.savedEmails.size
            viewModel.onSaveEmail()
            viewModel.onResendEmailVerification()
            advanceUntilIdle()
            assertEquals(savesAfterFailure, model.savedEmails.size)

            failProfileRead = false
            viewModel.onRefreshEmail()
            advanceUntilIdle()
            assertTrue(viewModel.canMutateOwnEmailState.value)
            assertEquals("owner@example.com", viewModel.emailInputState.value)
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
    fun disabledSmtpOwnerCanReplaceApprovedEmailWithoutSending() = runTest {
        val approvedEmail = Email("approved@example.com")
        val replacementEmail = Email("replacement@example.com")
        val model = UserEditTestUsersModel(
            ownerId,
            owner.copy(email = approvedEmail, emailApproved = true),
        ).apply { emailFeatureEnabled = false }
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            assertEquals(EmailCapabilityState.Disabled, viewModel.emailCapabilityState.value)
            assertTrue(viewModel.canManageOwnEmailState.value)
            assertTrue(viewModel.canMutateOwnEmailState.value)

            model.emailEvents.clear()
            viewModel.onEmailChanged(replacementEmail.string)
            viewModel.onSaveEmail()
            advanceUntilIdle()

            assertEquals(listOf("PUT:${replacementEmail.string}", "GET"), model.emailEvents)
            assertEquals(listOf<Email?>(replacementEmail), model.savedEmails)
            assertTrue(model.requestedEmails.isEmpty())
            assertTrue(viewModel.canManageOwnEmailState.value)
            assertEquals(replacementEmail, viewModel.ownEmailProfileState.value?.email)
            assertFalse(viewModel.ownEmailProfileState.value?.emailApproved ?: true)
            assertEquals(replacementEmail, viewModel.emailSavedState.value)
            assertFalse(viewModel.isDirtyState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun disabledSmtpPendingAndMissingEmailCanBeSavedWithoutSending() = runTest {
        val replacementEmail = Email("replacement@example.com")
        val startingProfiles = listOf(
            owner.copy(email = Email("pending@example.com"), emailApproved = false),
            owner,
        )
        startingProfiles.forEach { startingProfile ->
            val model = UserEditTestUsersModel(ownerId, startingProfile).apply {
                emailFeatureEnabled = false
            }
            val viewModel = UserEditViewModel(
                userEditTestNode(ownerId),
                model,
                RecordingUserEditInteractor(),
                StandardTestDispatcher(testScheduler),
            )
            try {
                advanceUntilIdle()
                model.emailEvents.clear()
                viewModel.onEmailChanged(replacementEmail.string)
                viewModel.onSaveEmail()
                advanceUntilIdle()

                assertEquals(listOf("PUT:${replacementEmail.string}", "GET"), model.emailEvents)
                assertEquals(replacementEmail, viewModel.emailSavedState.value)
                assertEquals(replacementEmail, viewModel.ownEmailProfileState.value?.email)
                assertFalse(viewModel.ownEmailProfileState.value?.emailApproved ?: true)
            } finally {
                viewModel.scope.cancel()
            }
        }
    }

    @Test
    fun emailSavePreservesDirtyAdminFields() = runTest {
        val replacementEmail = Email("replacement@example.com")
        val model = UserEditTestUsersModel(ownerId, owner.copy(email = Email("old@example.com"), emailApproved = true))
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            viewModel.onUsernameChanged("changed owner")
            viewModel.onEmailChanged(replacementEmail.string)
            viewModel.onSaveEmail()
            advanceUntilIdle()

            assertEquals(replacementEmail, viewModel.emailSavedState.value)
            assertTrue(viewModel.isDirtyState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun unchangedNormalizedEmailNeverDispatches() = runTest {
        val savedEmail = Email("Owner@example.com")
        val model = UserEditTestUsersModel(ownerId, owner.copy(email = savedEmail, emailApproved = true))
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            listOf(savedEmail.string, "  ${savedEmail.string}  ").forEach { unchangedInput ->
                viewModel.onEmailChanged(unchangedInput)
                advanceUntilIdle()
                assertFalse(viewModel.canSaveEmailState.value)
                viewModel.onSaveEmail()
                advanceUntilIdle()
                assertFalse(viewModel.isDirtyState.value)
            }
            viewModel.onEmailChanged("other@example.com")
            advanceUntilIdle()
            assertTrue(viewModel.canSaveEmailState.value)
            viewModel.onEmailChanged(savedEmail.string)
            viewModel.onSaveEmail()
            advanceUntilIdle()

            assertFalse(viewModel.canSaveEmailState.value)
            assertFalse(viewModel.isDirtyState.value)
            assertTrue(model.savedEmails.isEmpty())
            assertTrue(model.requestedEmails.isEmpty())
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun blankAndInvalidReplacementNeverDispatch() = runTest {
        val savedEmail = Email("saved@example.com")
        val model = UserEditTestUsersModel(ownerId, owner.copy(email = savedEmail, emailApproved = true))
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        val invalidDrafts = listOf("", "   ", "not-an-email", "a".repeat(245) + "@example.com")
        try {
            advanceUntilIdle()
            invalidDrafts.forEach { draft ->
                viewModel.onEmailChanged(draft)
                advanceUntilIdle()
                assertFalse(viewModel.canSaveEmailState.value, draft)
                viewModel.onSaveEmail()
                advanceUntilIdle()
                assertEquals(draft, viewModel.emailInputState.value)
                assertEquals(EmailEditorError.InvalidEmail, viewModel.emailErrorState.value)
            }

            assertTrue(model.savedEmails.isEmpty())
            assertTrue(model.requestedEmails.isEmpty())
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun replacementUsesExactEmailEquality() = runTest {
        val savedEmail = Email("Owner@example.com")
        val replacementEmail = Email("owner@example.com")
        val model = UserEditTestUsersModel(ownerId, owner.copy(email = savedEmail, emailApproved = true))
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            viewModel.onEmailChanged("  ${replacementEmail.string}  ")
            advanceUntilIdle()
            assertTrue(viewModel.canSaveEmailState.value)
            viewModel.onSaveEmail()
            advanceUntilIdle()

            assertEquals(listOf<Email?>(replacementEmail), model.savedEmails)
            assertEquals(replacementEmail.string, viewModel.emailInputState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun sameOwnerRootHasTheSameEmailControls() = runTest {
        listOf(true, false).forEach { smtpEnabled ->
            val oldEmail = Email("old@example.com")
            val replacementEmail = Email("replacement@example.com")
            val model = UserEditTestUsersModel(ownerId, owner.copy(email = oldEmail, emailApproved = true)).apply {
                rootState.value = true
                emailFeatureEnabled = smtpEnabled
            }
            val viewModel = UserEditViewModel(
                userEditTestNode(ownerId),
                model,
                RecordingUserEditInteractor(),
                StandardTestDispatcher(testScheduler),
            )
            try {
                advanceUntilIdle()
                assertTrue(viewModel.canManageOwnEmailState.value)
                assertTrue(viewModel.canMutateOwnEmailState.value)
                viewModel.onEmailChanged(replacementEmail.string)
                viewModel.onSaveEmail()
                advanceUntilIdle()
                assertEquals(listOf<Email?>(replacementEmail), model.savedEmails)
                assertEquals(if (smtpEnabled) listOf(replacementEmail) else emptyList(), model.requestedEmails)
            } finally {
                viewModel.scope.cancel()
            }
        }
    }

    @Test
    fun verificationWaitsForMatchingPrivateRefresh() = runTest {
        val firstGetEntered = CompletableDeferred<Unit>()
        val releaseFirstGet = CompletableDeferred<Unit>()
        val oldEmail = Email("old@example.com")
        val replacementEmail = Email("replacement@example.com")
        val model = UserEditTestUsersModel(ownerId, owner.copy(email = oldEmail, emailApproved = true))
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            var mutationReads = 0
            model.profileHandler = {
                mutationReads += 1
                if (mutationReads == 1) {
                    firstGetEntered.complete(Unit)
                    releaseFirstGet.await()
                }
                model.profileState.value
            }
            model.emailEvents.clear()
            viewModel.onEmailChanged(replacementEmail.string)
            viewModel.onSaveEmail()
            runCurrent()

            assertTrue(firstGetEntered.isCompleted)
            assertEquals(listOf("PUT:${replacementEmail.string}", "GET"), model.emailEvents)
            assertTrue(model.requestedEmails.isEmpty())
            assertNull(viewModel.emailSavedState.value)

            releaseFirstGet.complete(Unit)
            advanceUntilIdle()
            assertEquals(listOf(replacementEmail), model.requestedEmails)
            assertEquals(replacementEmail, viewModel.emailSavedState.value)
        } finally {
            releaseFirstGet.complete(Unit)
            viewModel.scope.cancel()
        }
    }

    @Test
    fun successfulPutWithMissingMismatchedOrFailedProfileNeverSends() = runTest {
        val oldEmail = Email("old@example.com")
        val replacementEmail = Email("replacement@example.com")
        val variants = listOf("missing", "wrong-owner", "old-address", "other-address", "no-address", "failure")
        variants.forEach { variant ->
            val model = UserEditTestUsersModel(ownerId, owner.copy(email = oldEmail, emailApproved = true))
            val viewModel = UserEditViewModel(
                userEditTestNode(ownerId),
                model,
                RecordingUserEditInteractor(),
                StandardTestDispatcher(testScheduler),
            )
            try {
                advanceUntilIdle()
                model.profileHandler = {
                    when (variant) {
                        "missing" -> null
                        "wrong-owner" -> owner.copy(id = UserId(99L), email = replacementEmail)
                        "old-address" -> owner.copy(email = oldEmail, emailApproved = true)
                        "other-address" -> owner.copy(email = Email("other@example.com"), emailApproved = false)
                        "no-address" -> owner
                        else -> throw IllegalStateException("private read failed")
                    }
                }
                viewModel.onEmailChanged(replacementEmail.string)
                viewModel.onSaveEmail()
                advanceUntilIdle()

                assertTrue(model.requestedEmails.isEmpty(), variant)
                assertNull(viewModel.emailSavedState.value, variant)
                assertNull(viewModel.emailVerificationResultState.value, variant)
                when (variant) {
                    "wrong-owner", "missing" -> {
                        assertNull(viewModel.ownEmailProfileState.value, variant)
                        assertTrue(viewModel.emailLoadFailedState.value, variant)
                    }
                    "failure" -> {
                        assertEquals(oldEmail, viewModel.ownEmailProfileState.value?.email, variant)
                        assertTrue(viewModel.emailLoadFailedState.value, variant)
                    }
                    else -> {
                        assertEquals(ownerId, viewModel.ownEmailProfileState.value?.id, variant)
                        assertEquals(EmailEditorError.EmailChanged, viewModel.emailErrorState.value, variant)
                    }
                }
            } finally {
                viewModel.scope.cancel()
            }
        }
    }

    @Test
    fun alreadyApprovedRefreshedReplacementDoesNotSend() = runTest {
        val replacementEmail = Email("replacement@example.com")
        val model = UserEditTestUsersModel(
            ownerId,
            owner.copy(email = Email("old@example.com"), emailApproved = true),
        ).apply {
            saveEmailHandler = { email ->
                profileState.value = profileState.value?.copy(email = email, emailApproved = true)
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
            model.emailEvents.clear()
            viewModel.onEmailChanged(replacementEmail.string)
            viewModel.onSaveEmail()
            advanceUntilIdle()

            assertEquals(listOf("PUT:${replacementEmail.string}", "GET", "GET"), model.emailEvents)
            assertTrue(model.requestedEmails.isEmpty())
            assertTrue(viewModel.ownEmailProfileState.value?.emailApproved == true)
            assertEquals(replacementEmail, viewModel.emailSavedState.value)
            assertFalse(viewModel.canResendEmailVerificationState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun verificationResultsPublishOnlyAfterFinalMatchingRefresh() = runTest {
        EmailVerificationRequestResult.entries.forEach { result ->
            val finalGetEntered = CompletableDeferred<Unit>()
            val releaseFinalGet = CompletableDeferred<Unit>()
            val replacementEmail = Email("replacement@example.com")
            val model = UserEditTestUsersModel(
                ownerId,
                owner.copy(email = Email("old@example.com"), emailApproved = false),
            ).apply {
                requestHandler = {
                    if (result == EmailVerificationRequestResult.AlreadyApproved) {
                        profileState.value = profileState.value?.copy(emailApproved = true)
                    }
                    result
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
                var mutationReads = 0
                model.profileHandler = {
                    mutationReads += 1
                    if (mutationReads == 2) {
                        finalGetEntered.complete(Unit)
                        releaseFinalGet.await()
                    }
                    model.profileState.value
                }
                viewModel.onEmailChanged(replacementEmail.string)
                viewModel.onSaveEmail()
                runCurrent()

                assertTrue(finalGetEntered.isCompleted, result.name)
                assertNull(viewModel.emailVerificationResultState.value, result.name)
                assertNull(viewModel.emailSavedState.value, result.name)

                releaseFinalGet.complete(Unit)
                advanceUntilIdle()
                assertEquals(result, viewModel.emailVerificationResultState.value, result.name)
                assertEquals(replacementEmail, viewModel.emailSavedState.value, result.name)
            } finally {
                releaseFinalGet.complete(Unit)
                viewModel.scope.cancel()
            }
        }
    }

    @Test
    fun changedMissingWrongOwnerOrFailedFinalReadSuppressesSuccess() = runTest {
        val variants = listOf("changed", "missing", "wrong-owner", "failed")
        variants.forEach { variant ->
            val replacementEmail = Email("replacement@example.com")
            val model = UserEditTestUsersModel(
                ownerId,
                owner.copy(email = Email("old@example.com"), emailApproved = true),
            )
            val viewModel = UserEditViewModel(
                userEditTestNode(ownerId),
                model,
                RecordingUserEditInteractor(),
                StandardTestDispatcher(testScheduler),
            )
            try {
                advanceUntilIdle()
                var mutationReads = 0
                model.profileHandler = {
                    mutationReads += 1
                    if (mutationReads == 1) {
                        model.profileState.value
                    } else {
                        when (variant) {
                            "changed" -> owner.copy(email = Email("changed@example.com"), emailApproved = false)
                            "missing" -> null
                            "wrong-owner" -> owner.copy(id = UserId(99L), email = replacementEmail)
                            else -> throw IllegalStateException("final read failed")
                        }
                    }
                }
                viewModel.onEmailChanged(replacementEmail.string)
                viewModel.onSaveEmail()
                advanceUntilIdle()

                assertNull(viewModel.emailSavedState.value, variant)
                assertNull(viewModel.emailVerificationResultState.value, variant)
                if (variant == "changed") {
                    assertEquals(EmailEditorError.EmailChanged, viewModel.emailErrorState.value)
                } else {
                    assertTrue(viewModel.emailLoadFailedState.value, variant)
                }
            } finally {
                viewModel.scope.cancel()
            }
        }
    }

    @Test
    fun alreadyApprovedResultWithFinalPendingProfileCannotClaimApproval() = runTest {
        val replacementEmail = Email("replacement@example.com")
        val model = UserEditTestUsersModel(
            ownerId,
            owner.copy(email = Email("old@example.com"), emailApproved = true),
        ).apply {
            requestResult = EmailVerificationRequestResult.AlreadyApproved
        }
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            viewModel.onEmailChanged(replacementEmail.string)
            viewModel.onSaveEmail()
            advanceUntilIdle()

            assertEquals(listOf(replacementEmail), model.requestedEmails)
            assertFalse(viewModel.ownEmailProfileState.value?.emailApproved ?: true)
            assertNull(viewModel.emailVerificationResultState.value)
            assertNull(viewModel.emailSavedState.value)
            assertEquals(EmailEditorError.EmailChanged, viewModel.emailErrorState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun failedPostReconciliationPreservesDeliveryFailureAndBlocksMutation() = runTest {
        var failFinalRead = false
        val replacementEmail = Email("replacement@example.com")
        val model = UserEditTestUsersModel(
            ownerId,
            owner.copy(email = Email("old@example.com"), emailApproved = true),
        ).apply {
            requestHandler = {
                failFinalRead = true
                throw IllegalStateException("delivery failed")
            }
            profileHandler = {
                if (failFinalRead) throw IllegalStateException("final read failed")
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
            viewModel.onEmailChanged(replacementEmail.string)
            viewModel.onSaveEmail()
            advanceUntilIdle()

            assertEquals(EmailVerificationRequestResult.DeliveryFailed, viewModel.emailVerificationResultState.value)
            assertTrue(viewModel.emailLoadFailedState.value)
            assertFalse(viewModel.canMutateOwnEmailState.value)
            assertEquals(replacementEmail.string, viewModel.emailInputState.value)
            assertNull(viewModel.emailSavedState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun resendUsesSavedEmailAndPreservesReplacementDraft() = runTest {
        val savedEmail = Email("saved@example.com")
        val replacementDraft = "replacement@example.com"
        val outcomes = EmailVerificationRequestResult.entries.map { it as EmailVerificationRequestResult? } + null
        outcomes.forEach { configuredOutcome ->
            val model = UserEditTestUsersModel(
                ownerId,
                owner.copy(email = savedEmail, emailApproved = false),
            ).apply {
                requestHandler = {
                    when (configuredOutcome) {
                        null -> throw IllegalStateException("delivery transport failed")
                        EmailVerificationRequestResult.AlreadyApproved -> {
                            profileState.value = profileState.value?.copy(emailApproved = true)
                            configuredOutcome
                        }
                        else -> configuredOutcome
                    }
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
                viewModel.onEmailChanged(replacementDraft)
                model.emailEvents.clear()
                viewModel.onResendEmailVerification()
                advanceUntilIdle()

                val expectedOutcome = configuredOutcome ?: EmailVerificationRequestResult.DeliveryFailed
                assertEquals(listOf("POST:${savedEmail.string}", "GET"), model.emailEvents, expectedOutcome.name)
                assertTrue(model.savedEmails.isEmpty(), expectedOutcome.name)
                assertEquals(listOf(savedEmail), model.requestedEmails, expectedOutcome.name)
                assertEquals(replacementDraft, viewModel.emailInputState.value, expectedOutcome.name)
                assertTrue(viewModel.isDirtyState.value, expectedOutcome.name)
                assertEquals(expectedOutcome, viewModel.emailVerificationResultState.value, expectedOutcome.name)
            } finally {
                viewModel.scope.cancel()
            }
        }
    }

    @Test
    fun refreshAndResumePreserveReplacementDraft() = runTest {
        val savedEmail = Email("saved@example.com")
        val replacementDraft = "replacement@example.com"
        var profileFails = false
        val node = userEditTestNode(ownerId)
        val model = UserEditTestUsersModel(ownerId, owner.copy(email = savedEmail, emailApproved = false)).apply {
            profileHandler = {
                if (profileFails) throw IllegalStateException("private read failed")
                profileState.value
            }
        }
        val viewModel = UserEditViewModel(
            node,
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            viewModel.onEmailChanged(replacementDraft)
            viewModel.onRefreshEmail()
            advanceUntilIdle()
            assertEquals(replacementDraft, viewModel.emailInputState.value)
            assertTrue(viewModel.isDirtyState.value)

            node.resume()
            advanceUntilIdle()
            assertEquals(replacementDraft, viewModel.emailInputState.value)

            profileFails = true
            viewModel.onRefreshEmail()
            advanceUntilIdle()
            assertTrue(viewModel.emailLoadFailedState.value)
            assertEquals(replacementDraft, viewModel.emailInputState.value)

            profileFails = false
            viewModel.onRefreshEmail()
            advanceUntilIdle()
            assertFalse(viewModel.emailLoadFailedState.value)
            assertEquals(replacementDraft, viewModel.emailInputState.value)
            assertTrue(viewModel.isDirtyState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun refreshDuringMutationIsCoalesced() = runTest {
        val putEntered = CompletableDeferred<Unit>()
        val releasePut = CompletableDeferred<Unit>()
        val replacementEmail = Email("replacement@example.com")
        val node = userEditTestNode(ownerId)
        val model = UserEditTestUsersModel(
            ownerId,
            owner.copy(email = Email("old@example.com"), emailApproved = false),
        ).apply {
            saveEmailHandler = { email ->
                profileState.value = profileState.value?.copy(email = email, emailApproved = false)
                putEntered.complete(Unit)
                releasePut.await()
                true
            }
        }
        val viewModel = UserEditViewModel(
            node,
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            model.emailEvents.clear()
            viewModel.onEmailChanged(replacementEmail.string)
            viewModel.onSaveEmail()
            runCurrent()
            assertTrue(putEntered.isCompleted)

            viewModel.onRefreshEmail()
            viewModel.onRefreshEmail()
            node.resume()
            runCurrent()
            assertEquals(listOf("PUT:${replacementEmail.string}"), model.emailEvents)

            releasePut.complete(Unit)
            advanceUntilIdle()
            assertEquals(
                listOf(
                    "PUT:${replacementEmail.string}",
                    "GET",
                    "POST:${replacementEmail.string}",
                    "GET",
                    "PROBE",
                    "GET",
                ),
                model.emailEvents,
            )
            assertEquals(replacementEmail.string, viewModel.emailInputState.value)
        } finally {
            releasePut.complete(Unit)
            viewModel.scope.cancel()
        }
    }

    @Test
    fun doubleSubmitAdmitsOnlyOneMutation() = runTest {
        val putEntered = CompletableDeferred<Unit>()
        val releasePut = CompletableDeferred<Unit>()
        val replacementEmail = Email("replacement@example.com")
        val model = UserEditTestUsersModel(
            ownerId,
            owner.copy(email = Email("saved@example.com"), emailApproved = false),
        ).apply {
            saveEmailHandler = { email ->
                profileState.value = profileState.value?.copy(email = email, emailApproved = false)
                putEntered.complete(Unit)
                releasePut.await()
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
            viewModel.onEmailChanged(replacementEmail.string)
            viewModel.onSaveEmail()
            viewModel.onSaveEmail()
            viewModel.onResendEmailVerification()
            runCurrent()
            assertTrue(putEntered.isCompleted)
            assertEquals(listOf<Email?>(replacementEmail), model.savedEmails)

            releasePut.complete(Unit)
            advanceUntilIdle()
            assertEquals(listOf<Email?>(replacementEmail), model.savedEmails)
            assertEquals(listOf(replacementEmail), model.requestedEmails)
        } finally {
            releasePut.complete(Unit)
            viewModel.scope.cancel()
        }
    }

    @Test
    fun emailChangeWhileBusyDoesNotReplaceSubmittedDraft() = runTest {
        val putEntered = CompletableDeferred<Unit>()
        val releasePut = CompletableDeferred<Unit>()
        val submittedEmail = Email("submitted@example.com")
        val model = UserEditTestUsersModel(
            ownerId,
            owner.copy(email = Email("saved@example.com"), emailApproved = true),
        ).apply {
            saveEmailHandler = { email ->
                profileState.value = profileState.value?.copy(email = email, emailApproved = false)
                putEntered.complete(Unit)
                releasePut.await()
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
            viewModel.onEmailChanged(submittedEmail.string)
            viewModel.onSaveEmail()
            runCurrent()
            assertTrue(putEntered.isCompleted)

            viewModel.onEmailChanged("late@example.com")
            assertEquals(submittedEmail.string, viewModel.emailInputState.value)

            releasePut.complete(Unit)
            advanceUntilIdle()
            assertEquals(listOf<Email?>(submittedEmail), model.savedEmails)
            assertEquals(listOf(submittedEmail), model.requestedEmails)
            assertEquals(submittedEmail.string, viewModel.emailInputState.value)
        } finally {
            releasePut.complete(Unit)
            viewModel.scope.cancel()
        }
    }

    @Test
    fun anonymousNonOwnerAndRootOtherCannotAccessPrivateEmail() = runTest {
        val otherId = UserId(8L)
        val cases = listOf(
            Triple<UserId?, Boolean, Boolean>(null, false, false),
            Triple<UserId?, Boolean, Boolean>(otherId, true, false),
            Triple<UserId?, Boolean, Boolean>(otherId, true, true),
        )
        cases.forEach { (callerId, authorised, root) ->
            val model = UserEditTestUsersModel(
                callerId,
                owner.copy(email = Email("private@example.com"), emailApproved = true),
                initiallyAuthorised = authorised,
            ).apply { rootState.value = root }
            val viewModel = UserEditViewModel(
                userEditTestNode(ownerId),
                model,
                RecordingUserEditInteractor(),
                StandardTestDispatcher(testScheduler),
            )
            try {
                advanceUntilIdle()
                viewModel.onEmailChanged("forbidden@example.com")
                viewModel.onSaveEmail()
                viewModel.onResendEmailVerification()
                viewModel.onRefreshEmail()
                advanceUntilIdle()

                assertFalse(viewModel.canManageOwnEmailState.value)
                assertFalse(viewModel.canMutateOwnEmailState.value)
                assertEquals(0, model.probeReads)
                assertEquals(0, model.profileReads)
                assertTrue(model.savedEmails.isEmpty())
                assertTrue(model.requestedEmails.isEmpty())
                assertNull(viewModel.ownEmailProfileState.value)
                assertFalse(viewModel.emailOperationInterruptedState.value)
            } finally {
                viewModel.scope.cancel()
            }
        }
    }

    @Test
    fun mismatchedInitialPrivateProfileFailsClosed() = runTest {
        val model = UserEditTestUsersModel(
            ownerId,
            owner.copy(id = UserId(99L), email = Email("private@example.com"), emailApproved = true),
        )
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            assertEquals(EmailCapabilityState.Failed, viewModel.emailCapabilityState.value)
            assertTrue(viewModel.emailLoadFailedState.value)
            assertFalse(viewModel.canMutateOwnEmailState.value)
            assertNull(viewModel.ownEmailProfileState.value)
            assertEquals("", viewModel.emailInputState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun rawOwnerAndTargetChecksRejectLaggingEligibility() = runTest {
        listOf("caller", "authorization", "target").forEach { changedField ->
            val node = userEditTestNode(ownerId)
            val model = UserEditTestUsersModel(
                ownerId,
                owner.copy(email = Email("saved@example.com"), emailApproved = false),
            )
            val viewModel = UserEditViewModel(
                node,
                model,
                RecordingUserEditInteractor(),
                StandardTestDispatcher(testScheduler),
            )
            try {
                advanceUntilIdle()
                viewModel.onEmailChanged("draft@example.com")
                when (changedField) {
                    "caller" -> model.currentUserIdState.value = UserId(8L)
                    "authorization" -> model.authorisedState.value = false
                    else -> node.retarget(UserId(8L))
                }

                viewModel.onEmailChanged("forbidden@example.com")
                viewModel.onSaveEmail()
                viewModel.onResendEmailVerification()
                viewModel.onRefreshEmail()
                advanceUntilIdle()

                assertTrue(model.savedEmails.isEmpty(), changedField)
                assertTrue(model.requestedEmails.isEmpty(), changedField)
                assertNull(viewModel.ownEmailProfileState.value, changedField)
                assertEquals("", viewModel.emailInputState.value, changedField)
            } finally {
                viewModel.scope.cancel()
            }
        }
    }

    @Test
    fun queuedReplacementRejectsNodeRetarget() = runTest {
        val replacementEmail = Email("replacement@example.com")
        val node = userEditTestNode(ownerId)
        val model = UserEditTestUsersModel(
            ownerId,
            owner.copy(email = Email("saved@example.com"), emailApproved = true),
        )
        val viewModel = UserEditViewModel(
            node,
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            viewModel.onEmailChanged(replacementEmail.string)
            viewModel.onSaveEmail()
            node.retarget(UserId(8L))
            advanceUntilIdle()

            assertTrue(model.savedEmails.isEmpty())
            assertTrue(model.requestedEmails.isEmpty())
            assertNull(viewModel.ownEmailProfileState.value)
            assertEquals("", viewModel.emailInputState.value)
            assertTrue(viewModel.emailOperationInterruptedState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun retargetDuringPutPreventsSuccessorRequests() = runTest {
        val putEntered = CompletableDeferred<Unit>()
        val releasePut = CompletableDeferred<Unit>()
        val node = userEditTestNode(ownerId)
        val model = UserEditTestUsersModel(
            ownerId,
            owner.copy(email = Email("saved@example.com"), emailApproved = true),
        ).apply {
            saveEmailHandler = { email ->
                profileState.value = profileState.value?.copy(email = email, emailApproved = false)
                putEntered.complete(Unit)
                withContext(NonCancellable) { releasePut.await() }
                true
            }
        }
        val viewModel = UserEditViewModel(
            node,
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            val readsBeforeSave = model.profileReads
            viewModel.onEmailChanged("replacement@example.com")
            viewModel.onSaveEmail()
            runCurrent()
            assertTrue(putEntered.isCompleted)

            node.retarget(UserId(8L))
            runCurrent()
            releasePut.complete(Unit)
            advanceUntilIdle()

            assertTrue(model.requestedEmails.isEmpty())
            assertEquals(readsBeforeSave, model.profileReads)
            assertNull(viewModel.emailSavedState.value)
            assertNull(viewModel.ownEmailProfileState.value)
            assertTrue(viewModel.emailOperationInterruptedState.value)
        } finally {
            releasePut.complete(Unit)
            viewModel.scope.cancel()
        }
    }

    @Test
    fun retargetDuringPrivateRefreshPreventsPostAndPublication() = runTest {
        val firstGetEntered = CompletableDeferred<Unit>()
        val releaseFirstGet = CompletableDeferred<Unit>()
        val node = userEditTestNode(ownerId)
        val model = UserEditTestUsersModel(
            ownerId,
            owner.copy(email = Email("saved@example.com"), emailApproved = true),
        )
        val viewModel = UserEditViewModel(
            node,
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            var mutationReads = 0
            model.profileHandler = {
                mutationReads += 1
                if (mutationReads == 1) {
                    firstGetEntered.complete(Unit)
                    withContext(NonCancellable) { releaseFirstGet.await() }
                }
                model.profileState.value
            }
            viewModel.onEmailChanged("replacement@example.com")
            viewModel.onSaveEmail()
            runCurrent()
            assertTrue(firstGetEntered.isCompleted)

            node.retarget(UserId(8L))
            runCurrent()
            releaseFirstGet.complete(Unit)
            advanceUntilIdle()

            assertTrue(model.requestedEmails.isEmpty())
            assertNull(viewModel.emailSavedState.value)
            assertNull(viewModel.emailVerificationResultState.value)
            assertNull(viewModel.ownEmailProfileState.value)
            assertTrue(viewModel.emailOperationInterruptedState.value)
        } finally {
            releaseFirstGet.complete(Unit)
            viewModel.scope.cancel()
        }
    }

    @Test
    fun retargetDuringPostSuppressesFinalPublication() = runTest {
        listOf("post", "final-get").forEach { boundary ->
            val boundaryEntered = CompletableDeferred<Unit>()
            val releaseBoundary = CompletableDeferred<Unit>()
            val node = userEditTestNode(ownerId)
            val model = UserEditTestUsersModel(
                ownerId,
                owner.copy(email = Email("saved@example.com"), emailApproved = true),
            ).apply {
                requestHandler = {
                    if (boundary == "post") {
                        boundaryEntered.complete(Unit)
                        withContext(NonCancellable) { releaseBoundary.await() }
                    }
                    EmailVerificationRequestResult.Sent
                }
            }
            val viewModel = UserEditViewModel(
                node,
                model,
                RecordingUserEditInteractor(),
                StandardTestDispatcher(testScheduler),
            )
            try {
                advanceUntilIdle()
                var mutationReads = 0
                model.profileHandler = {
                    mutationReads += 1
                    if (boundary == "final-get" && mutationReads == 2) {
                        boundaryEntered.complete(Unit)
                        withContext(NonCancellable) { releaseBoundary.await() }
                    }
                    model.profileState.value
                }
                viewModel.onEmailChanged("replacement@example.com")
                viewModel.onSaveEmail()
                runCurrent()
                assertTrue(boundaryEntered.isCompleted, boundary)
                assertNull(viewModel.emailVerificationResultState.value, boundary)

                node.retarget(UserId(8L))
                runCurrent()
                releaseBoundary.complete(Unit)
                advanceUntilIdle()

                assertNull(viewModel.emailSavedState.value, boundary)
                assertNull(viewModel.emailVerificationResultState.value, boundary)
                assertNull(viewModel.ownEmailProfileState.value, boundary)
                assertTrue(viewModel.emailOperationInterruptedState.value, boundary)
            } finally {
                releaseBoundary.complete(Unit)
                viewModel.scope.cancel()
            }
        }
    }

    @Test
    fun targetLeaveAndReturnDoesNotReviveOldMutation() = runTest {
        val putEntered = CompletableDeferred<Unit>()
        val releasePut = CompletableDeferred<Unit>()
        val node = userEditTestNode(ownerId)
        val replacementEmail = Email("replacement@example.com")
        val model = UserEditTestUsersModel(
            ownerId,
            owner.copy(email = Email("saved@example.com"), emailApproved = true),
        ).apply {
            saveEmailHandler = { email ->
                profileState.value = profileState.value?.copy(email = email, emailApproved = false)
                putEntered.complete(Unit)
                withContext(NonCancellable) { releasePut.await() }
                true
            }
        }
        val viewModel = UserEditViewModel(
            node,
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            viewModel.onEmailChanged(replacementEmail.string)
            viewModel.onSaveEmail()
            runCurrent()
            assertTrue(putEntered.isCompleted)

            node.retarget(UserId(8L))
            runCurrent()
            node.retarget(ownerId)
            advanceUntilIdle()
            releasePut.complete(Unit)
            advanceUntilIdle()

            assertTrue(model.requestedEmails.isEmpty())
            assertNull(viewModel.emailSavedState.value)
            assertNull(viewModel.emailVerificationResultState.value)
            assertEquals(replacementEmail, viewModel.ownEmailProfileState.value?.email)
            assertTrue(viewModel.emailOperationInterruptedState.value)
        } finally {
            releasePut.complete(Unit)
            viewModel.scope.cancel()
        }
    }

    @Test
    fun logoutExitsWithoutDirtyConfirmation() = runTest {
        val interactor = RecordingUserEditInteractor()
        val model = UserEditTestUsersModel(
            ownerId,
            owner.copy(email = Email("saved@example.com"), emailApproved = false),
        )
        val viewModel = UserEditViewModel(
            userEditTestNode(ownerId),
            model,
            interactor,
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            viewModel.onEmailChanged("draft@example.com")
            runCurrent()
            assertTrue(viewModel.isDirtyState.value)

            model.authorisedState.value = false
            model.currentUserIdState.value = null
            advanceUntilIdle()

            assertEquals(1, interactor.navigateBackCalls)
            assertFalse(viewModel.showConfirmDialogState.value)
            assertNull(viewModel.ownEmailProfileState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun nodeDestroyCancelsEmailWork() = runTest {
        val putEntered = CompletableDeferred<Unit>()
        val releasePut = CompletableDeferred<Unit>()
        val node = userEditTestNode(ownerId)
        node.resume()
        val interactor = RecordingUserEditInteractor()
        val model = UserEditTestUsersModel(
            ownerId,
            owner.copy(email = Email("saved@example.com"), emailApproved = true),
        ).apply {
            saveEmailHandler = { email ->
                profileState.value = profileState.value?.copy(email = email, emailApproved = false)
                putEntered.complete(Unit)
                withContext(NonCancellable) { releasePut.await() }
                true
            }
        }
        val viewModel = UserEditViewModel(
            node,
            model,
            interactor,
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            val readsBeforeSave = model.profileReads
            viewModel.onEmailChanged("replacement@example.com")
            viewModel.onSaveEmail()
            runCurrent()
            assertTrue(putEntered.isCompleted)

            node.destroy()
            val lifecycleJob = viewModel.scope.coroutineContext[Job]
            while (lifecycleJob?.isCancelled != true) {
                yield()
            }
            releasePut.complete(Unit)
            advanceUntilIdle()

            assertTrue(model.requestedEmails.isEmpty())
            assertEquals(readsBeforeSave, model.profileReads)
            assertNull(viewModel.emailSavedState.value)
            assertNull(viewModel.emailVerificationResultState.value)
            assertEquals(0, interactor.savedCalls)
        } finally {
            releasePut.complete(Unit)
            viewModel.scope.cancel()
        }
    }
}
