package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequestResult
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.EmailProfile
import dev.inmo.wishlist.features.users.common.models.UserId
import korlibs.time.DateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Owner eligibility, stale-state, and password-email feedback regression tests. */
@OptIn(ExperimentalCoroutinesApi::class)
class UserEditViewModelPasswordChangeTest {
    /** Approved private profile used by the permitted owner flows. */
    private val ownerId = UserId(7L)

    /** A second account used to prove root authority does not issue for another profile. */
    private val otherId = UserId(8L)

    /** Exact displayed address expected by every admitted request. */
    private val approvedEmail = Email("owner@example.com")

    /** Produces an owner profile with a caller-controlled approval state. */
    private fun profile(
        id: UserId = ownerId,
        email: Email? = approvedEmail,
        approved: Boolean = true,
    ) = EmailProfile(userId = id.long, email = email, emailApproved = approved)

    /** Creates a serial-dispatcher ViewModel and completes its initial owner reconciliation. */
    private suspend fun createViewModel(
        model: UserEditTestUsersModel,
        owner: UserId = ownerId,
        dispatcher: kotlinx.coroutines.CoroutineDispatcher = UnconfinedTestDispatcher(),
    ): UserEditViewModel = UserEditViewModel(
        userEditTestNode(owner),
        model,
        RecordingUserEditInteractor(),
        dispatcher,
    )

    /** Cancels and joins every lifecycle child started by the isolated ViewModel. */
    private suspend fun close(viewModel: UserEditViewModel) {
        viewModel.scope.coroutineContext[Job]?.cancelAndJoin()
    }

    /** Confirms ordinary owners and root editing their own profile issue exactly the displayed address. */
    /** Verifies eligible owner and root-self requests use only the displayed approved email. */
    @Test
    fun ownerAndRootOnSelfIssueOnlyDisplayedApprovedEmailWithoutOtherMutations() = runTest {
        listOf(false, true).forEach { root ->
            val model = UserEditTestUsersModel(ownerId, profile()).apply { rootState.value = root }
            val viewModel = createViewModel(model)
            try {
                advanceUntilIdle()
                assertTrue(viewModel.canRequestPasswordChangeEmailState.value)
                viewModel.onRequestPasswordChangeEmail()
                advanceUntilIdle()
                assertEquals(listOf(approvedEmail), model.passwordChangeRequestedEmails)
                assertEquals(PasswordChangeEmailRequestResult.Sent, viewModel.passwordChangeEmailResultState.value)
                assertTrue(model.requestedEmails.isEmpty())
                assertTrue(model.passwordUpdates.isEmpty())
                assertTrue(model.usernameUpdates.isEmpty())
            } finally {
                close(viewModel)
            }
        }
    }

    /** A pending replacement and email-edit cooldown cannot redirect or suppress approved-address authorization. */
    @Test
    fun approvedCurrentEmailRemainsPasswordRecipientWhileReplacementIsPending() = runTest {
        val pending = Email("pending@example.com")
        val model = UserEditTestUsersModel(
            ownerId,
            profile().copy(
                pendingEmail = pending,
                emailChangeAllowedAt = DateTime.fromUnixMillis(4_102_444_800_000L),
            ),
        )
        val viewModel = createViewModel(model)
        try {
            advanceUntilIdle()
            assertFalse(viewModel.canMutateOwnEmailState.value)
            assertTrue(viewModel.canRequestPasswordChangeEmailState.value)
            viewModel.onRequestPasswordChangeEmail()
            advanceUntilIdle()
            assertEquals(listOf(approvedEmail), model.passwordChangeRequestedEmails)
            assertEquals(PasswordChangeEmailRequestResult.Sent, viewModel.passwordChangeEmailResultState.value)
        } finally {
            close(viewModel)
        }
    }

    /** Rejects every caller, profile, and capability state that cannot prove an eligible current owner. */
    /** Verifies raw admission rejects unauthorized, missing, mismatched, and unavailable states. */
    @Test
    fun rawAdmissionRejectsUnauthorizedMissingUnapprovedMismatchedAndUnavailableStates() = runTest {
        val cases = listOf(
            UserEditTestUsersModel(null, null, initiallyAuthorised = false),
            UserEditTestUsersModel(otherId, profile()),
            UserEditTestUsersModel(otherId, profile()).apply { rootState.value = true },
            UserEditTestUsersModel(ownerId, profile(email = null)),
            UserEditTestUsersModel(ownerId, profile(approved = false)),
            UserEditTestUsersModel(ownerId, profile(id = otherId)),
            UserEditTestUsersModel(ownerId, profile()).apply { emailFeatureEnabled = false },
            UserEditTestUsersModel(ownerId, profile()).apply { probeHandler = { throw IllegalStateException("probe") } },
            UserEditTestUsersModel(ownerId, profile()).apply { profileHandler = { throw IllegalStateException("profile") } },
        )
        cases.forEach { model ->
            val viewModel = createViewModel(model)
            try {
                advanceUntilIdle()
                assertFalse(viewModel.canRequestPasswordChangeEmailState.value)
                viewModel.onRequestPasswordChangeEmail()
                advanceUntilIdle()
                assertTrue(model.passwordChangeRequestedEmails.isEmpty())
                assertTrue(model.requestedEmails.isEmpty())
                assertTrue(model.passwordUpdates.isEmpty())
                assertTrue(model.usernameUpdates.isEmpty())
            } finally {
                close(viewModel)
            }
        }
    }

    /** Rejects an immediate repeated click while the first owner-bound request holds the busy slot. */
    /** Verifies busy admission accepts one password-change email request. */
    @Test
    fun busyPasswordChangeRequestAcceptsOnlyOneClick() = runTest {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val model = UserEditTestUsersModel(ownerId, profile()).apply {
            passwordChangeRequestHandler = {
                entered.complete(Unit)
                withContext(NonCancellable) { release.await() }
                PasswordChangeEmailRequestResult.Sent
            }
        }
        val viewModel = createViewModel(model)
        try {
            advanceUntilIdle()
            viewModel.onRequestPasswordChangeEmail()
            viewModel.onRequestPasswordChangeEmail()
            runCurrent()
            assertTrue(entered.isCompleted)
            assertEquals(listOf(approvedEmail), model.passwordChangeRequestedEmails)
            assertTrue(viewModel.emailBusyState.value)
            assertFalse(viewModel.canRequestPasswordChangeEmailState.value)
            release.complete(Unit)
            advanceUntilIdle()
            assertFalse(viewModel.emailBusyState.value)
        } finally {
            release.complete(Unit)
            close(viewModel)
        }
    }

    /** Separates domain results and null transport uncertainty while reconciling before a retry. */
    /** Verifies distinct feedback and eligible retry reconciliation. */
    @Test
    fun passwordChangeFeedbackIsDistinctAndEligibleRetryReconcilesProfile() = runTest {
        val outcomes = listOf(
            PasswordChangeEmailRequestResult.Sent,
            PasswordChangeEmailRequestResult.Ineligible,
            PasswordChangeEmailRequestResult.DeliveryFailed,
            PasswordChangeEmailRequestResult.Unavailable,
            null,
        )
        outcomes.forEach { outcome ->
            val model = UserEditTestUsersModel(ownerId, profile()).apply { passwordChangeRequestResult = outcome }
            val viewModel = createViewModel(model)
            try {
                advanceUntilIdle()
                val readsBefore = model.profileReads
                viewModel.onRequestPasswordChangeEmail()
                advanceUntilIdle()
                assertEquals(listOf(approvedEmail), model.passwordChangeRequestedEmails)
                assertEquals(outcome, viewModel.passwordChangeEmailResultState.value)
                assertEquals(outcome == null, viewModel.emailErrorState.value == EmailEditorError.PasswordChangeRequestFailed)
                assertTrue(model.profileReads > readsBefore)
                model.passwordChangeRequestResult = PasswordChangeEmailRequestResult.Sent
                viewModel.onRequestPasswordChangeEmail()
                advanceUntilIdle()
                assertEquals(2, model.passwordChangeRequestedEmails.size)
            } finally {
                close(viewModel)
            }
        }
    }

    /** Keeps password-email admission closed until every cold, held, and failed owner refresh settles. */
    /** Verifies Unknown, Loading, refresh, and failure states remain ineligible. */
    @Test
    fun unknownLoadingAndRefreshKeepPasswordRequestIneligible() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val probeRelease = CompletableDeferred<Boolean>()
        val profileRelease = CompletableDeferred<EmailProfile?>()
        var holdProfile = true
        val model = UserEditTestUsersModel(ownerId, profile()).apply {
            probeHandler = { probeRelease.await() }
            profileHandler = {
                if (holdProfile) profileRelease.await() else profile()
            }
        }
        val viewModel = createViewModel(model, dispatcher = dispatcher)
        try {
            assertEquals(EmailCapabilityState.Unknown, viewModel.emailCapabilityState.value)
            assertFalse(viewModel.canRequestPasswordChangeEmailState.value)
            viewModel.onRequestPasswordChangeEmail()
            assertTrue(model.passwordChangeRequestedEmails.isEmpty())

            runCurrent()
            assertEquals(EmailCapabilityState.Loading, viewModel.emailCapabilityState.value)
            assertFalse(viewModel.canRequestPasswordChangeEmailState.value)
            viewModel.onRequestPasswordChangeEmail()
            assertTrue(model.passwordChangeRequestedEmails.isEmpty())

            probeRelease.complete(true)
            runCurrent()
            assertEquals(EmailCapabilityState.Enabled, viewModel.emailCapabilityState.value)
            assertTrue(viewModel.emailLoadingState.value)
            assertNull(viewModel.ownEmailProfileState.value)
            assertFalse(viewModel.canRequestPasswordChangeEmailState.value)
            viewModel.onRequestPasswordChangeEmail()
            assertTrue(model.passwordChangeRequestedEmails.isEmpty())

            profileRelease.complete(profile())
            advanceUntilIdle()
            assertTrue(viewModel.canRequestPasswordChangeEmailState.value)

            val heldRefresh = CompletableDeferred<EmailProfile?>()
            holdProfile = false
            model.profileHandler = { heldRefresh.await() }
            viewModel.onRefreshEmail()
            runCurrent()
            assertTrue(viewModel.emailLoadingState.value)
            assertNull(viewModel.ownEmailProfileState.value)
            assertFalse(viewModel.canRequestPasswordChangeEmailState.value)
            viewModel.onRequestPasswordChangeEmail()
            assertTrue(model.passwordChangeRequestedEmails.isEmpty())

            heldRefresh.complete(profile())
            advanceUntilIdle()
            assertTrue(viewModel.canRequestPasswordChangeEmailState.value)

            model.profileHandler = { throw IllegalStateException("profile") }
            viewModel.onRefreshEmail()
            advanceUntilIdle()
            assertEquals(EmailCapabilityState.Failed, viewModel.emailCapabilityState.value)
            assertNull(viewModel.ownEmailProfileState.value)
            assertFalse(viewModel.canRequestPasswordChangeEmailState.value)

            model.probeHandler = { throw IllegalStateException("probe") }
            viewModel.onRefreshEmail()
            advanceUntilIdle()
            assertEquals(EmailCapabilityState.Failed, viewModel.emailCapabilityState.value)
            assertNull(viewModel.ownEmailProfileState.value)
            assertFalse(viewModel.canRequestPasswordChangeEmailState.value)
        } finally {
            if (!probeRelease.isCompleted) probeRelease.complete(true)
            if (!profileRelease.isCompleted) profileRelease.complete(profile())
            close(viewModel)
        }
    }

    /** Rejects an action when a queued derived state is stale true but raw authorization is already false. */
    /** Verifies stale derived eligibility cannot authorize a raw-invalid request. */
    @Test
    fun staleTrueCannotAuthorizePasswordEmail() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val model = UserEditTestUsersModel(ownerId, profile())
        val viewModel = createViewModel(model, dispatcher = dispatcher)
        try {
            advanceUntilIdle()
            assertTrue(viewModel.canRequestPasswordChangeEmailState.value)
            model.authorisedState.value = false
            assertTrue(viewModel.canRequestPasswordChangeEmailState.value)
            viewModel.onRequestPasswordChangeEmail()
            assertTrue(model.passwordChangeRequestedEmails.isEmpty())
        } finally {
            close(viewModel)
        }
    }

    /** Admits a raw-valid request when queued derived eligibility is still stale false during refresh completion. */
    /** Verifies stale derived ineligibility cannot block a raw-valid request. */
    @Test
    fun staleFalseDoesNotBlockValidRawAdmission() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val profileRelease = CompletableDeferred<EmailProfile?>()
        val model = UserEditTestUsersModel(ownerId, profile()).apply {
            profileHandler = { profileRelease.await() }
        }
        val viewModel = createViewModel(model, dispatcher = dispatcher)
        var invoked = false
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.emailLoadingState.collect { loading ->
                if (
                    !invoked &&
                    !loading &&
                    viewModel.emailCapabilityState.value == EmailCapabilityState.Enabled &&
                    viewModel.ownEmailProfileState.value == profile()
                ) {
                    invoked = true
                    assertFalse(viewModel.canRequestPasswordChangeEmailState.value)
                    viewModel.onRequestPasswordChangeEmail()
                }
            }
        }
        try {
            runCurrent()
            assertFalse(viewModel.canRequestPasswordChangeEmailState.value)
            profileRelease.complete(profile())
            advanceUntilIdle()
            assertTrue(invoked)
            assertEquals(listOf(approvedEmail), model.passwordChangeRequestedEmails)
            assertEquals(PasswordChangeEmailRequestResult.Sent, viewModel.passwordChangeEmailResultState.value)
        } finally {
            if (!profileRelease.isCompleted) profileRelease.complete(profile())
            collector.cancelAndJoin()
            close(viewModel)
        }
    }

    /** Prevents an obsolete completion from publishing or clearing a distinct later owner mutation. */
    /** Verifies an obsolete completion cannot publish or clear a later mutation. */
    @Test
    fun stalePasswordChangeCompletionCannotPublishOrClearLaterMutation() = runTest {
        val firstEntered = CompletableDeferred<Unit>()
        val firstRelease = CompletableDeferred<Unit>()
        val secondEntered = CompletableDeferred<Unit>()
        val secondRelease = CompletableDeferred<Unit>()
        var requestNumber = 0
        val model = UserEditTestUsersModel(ownerId, profile()).apply {
            passwordChangeRequestHandler = {
                requestNumber += 1
                when (requestNumber) {
                    1 -> {
                        firstEntered.complete(Unit)
                        withContext(NonCancellable) { firstRelease.await() }
                        PasswordChangeEmailRequestResult.Sent
                    }
                    else -> {
                        secondEntered.complete(Unit)
                        withContext(NonCancellable) { secondRelease.await() }
                        PasswordChangeEmailRequestResult.DeliveryFailed
                    }
                }
            }
        }
        val viewModel = createViewModel(model)
        try {
            advanceUntilIdle()
            viewModel.onRequestPasswordChangeEmail()
            runCurrent()
            assertTrue(firstEntered.isCompleted)
            model.currentUserIdState.value = otherId
            runCurrent()
            model.currentUserIdState.value = ownerId
            advanceUntilIdle()
            assertTrue(viewModel.canRequestPasswordChangeEmailState.value)
            viewModel.onRequestPasswordChangeEmail()
            runCurrent()
            assertTrue(secondEntered.isCompleted)
            assertTrue(viewModel.emailBusyState.value)
            val readsBeforeOldRelease = model.profileReads

            firstRelease.complete(Unit)
            advanceUntilIdle()
            assertNull(viewModel.passwordChangeEmailResultState.value)
            assertEquals(readsBeforeOldRelease, model.profileReads)
            assertTrue(viewModel.emailBusyState.value)

            secondRelease.complete(Unit)
            advanceUntilIdle()
            assertEquals(PasswordChangeEmailRequestResult.DeliveryFailed, viewModel.passwordChangeEmailResultState.value)
            assertFalse(viewModel.emailBusyState.value)
        } finally {
            if (!firstRelease.isCompleted) firstRelease.complete(Unit)
            if (!secondRelease.isCompleted) secondRelease.complete(Unit)
            close(viewModel)
        }
    }
    /** Every acknowledged domain outcome survives held, failed, null, and wrong-owner reads and same-address recovery. */
    @Test
    fun acknowledgedOutcomesSurviveReadFailuresAndUnchangedRefreshRecovery() = runTest {
        val outcomes = listOf(PasswordChangeEmailRequestResult.Sent, PasswordChangeEmailRequestResult.DeliveryFailed,
            PasswordChangeEmailRequestResult.Unavailable, PasswordChangeEmailRequestResult.Ineligible)
        for (outcome in outcomes) {
            for (failure in listOf("throw", "null", "wrong")) {
                val model = UserEditTestUsersModel(ownerId, profile()).apply { passwordChangeRequestResult = outcome }
                val viewModel = createViewModel(model, dispatcher = StandardTestDispatcher(testScheduler))
                try {
                    advanceUntilIdle()
                    viewModel.onEmailChanged("draft@example.com")
                    model.profileHandler = {
                        when (failure) {
                            "throw" -> throw IllegalStateException("read")
                            "null" -> null
                            else -> profile(id = otherId)
                        }
                    }
                    viewModel.onRequestPasswordChangeEmail()
                    advanceUntilIdle()
                    assertEquals(outcome, viewModel.passwordChangeEmailResultState.value)
                    assertTrue(viewModel.emailLoadFailedState.value)
                    assertFalse(viewModel.emailBusyState.value)
                    assertFalse(viewModel.canRequestPasswordChangeEmailState.value)
                    assertFalse(viewModel.emailErrorState.value == EmailEditorError.PasswordChangeRequestFailed)
                    assertEquals("draft@example.com", viewModel.emailInputState.value)
                    viewModel.onRefreshEmail()
                    advanceUntilIdle()
                    assertEquals(outcome, viewModel.passwordChangeEmailResultState.value)
                    val refresh = CompletableDeferred<EmailProfile?>()
                    model.profileHandler = { refresh.await() }
                    viewModel.onRefreshEmail()
                    runCurrent()
                    assertNull(viewModel.ownEmailProfileState.value)
                    assertEquals(outcome, viewModel.passwordChangeEmailResultState.value)
                    assertFalse(viewModel.canRequestPasswordChangeEmailState.value)
                    refresh.complete(profile())
                    advanceUntilIdle()
                    assertEquals(outcome, viewModel.passwordChangeEmailResultState.value)
                    assertFalse(viewModel.emailLoadFailedState.value)
                    assertEquals(1, model.passwordChangeRequestedEmails.size)
                } finally { close(viewModel) }
            }
        }
    }

    /** Delivery is published before reconciliation; authoritative address or approval changes retire feedback. */
    @Test
    fun heldReconciliationPublishesSentBeforeGetAndCheckedChangesRetireFeedback() = runTest {
        for (changed in listOf(false, true)) {
            val model = UserEditTestUsersModel(ownerId, profile())
            val viewModel = createViewModel(model, dispatcher = StandardTestDispatcher(testScheduler))
            try {
                advanceUntilIdle()
                val response = CompletableDeferred<EmailProfile?>()
                model.profileHandler = { response.await() }
                viewModel.onRequestPasswordChangeEmail()
                runCurrent()
                assertEquals(PasswordChangeEmailRequestResult.Sent, viewModel.passwordChangeEmailResultState.value)
                response.complete(if (changed) profile(email = Email("changed@example.com")) else profile().copy(pendingEmail = Email("pending@example.com")))
                advanceUntilIdle()
                assertEquals(if (changed) null else PasswordChangeEmailRequestResult.Sent, viewModel.passwordChangeEmailResultState.value)
                if (!changed) {
                    model.profileHandler = { profile(approved = false) }
                    viewModel.onRefreshEmail()
                    advanceUntilIdle()
                    assertNull(viewModel.passwordChangeEmailResultState.value)
                }
                assertFalse(viewModel.emailErrorState.value == EmailEditorError.PasswordChangeRequestFailed)
                assertEquals(1, model.passwordChangeRequestedEmails.size)
            } finally { close(viewModel) }
        }
    }

    /** Ordinary POST failures produce uncertainty without fabricating a delivery result. */
    @Test
    fun thrownPasswordEmailPostIsUnconfirmed() = runTest {
        val model = UserEditTestUsersModel(ownerId, profile()).apply {
            passwordChangeRequestHandler = { throw IllegalStateException("post") }
        }
        val viewModel = createViewModel(model)
        try {
            advanceUntilIdle()
            viewModel.onRequestPasswordChangeEmail()
            advanceUntilIdle()
            assertNull(viewModel.passwordChangeEmailResultState.value)
            assertEquals(EmailEditorError.PasswordChangeRequestFailed, viewModel.emailErrorState.value)
            assertEquals(1, model.passwordChangeRequestedEmails.size)
        } finally { close(viewModel) }
    }

    /** Stored request identity is retired with private feedback; obsolete held GET work cannot restore either outcome. */
    @Test
    fun capturedFeedbackClearsOnOperationLogoutCallerTargetAndDestruction() = runTest {
        for (transition in listOf("operation", "logout", "caller", "target", "destroy")) {
            val node = userEditTestNode(ownerId)
            val model = UserEditTestUsersModel(ownerId, profile())
            val viewModel = UserEditViewModel(node, model, RecordingUserEditInteractor(), StandardTestDispatcher(testScheduler))
            val release = CompletableDeferred<EmailProfile?>()
            try {
                advanceUntilIdle()
                viewModel.onRequestPasswordChangeEmail()
                advanceUntilIdle()
                assertEquals(PasswordChangeEmailRequestResult.Sent, viewModel.passwordChangeEmailResultState.value)
                model.profileHandler = { withContext(NonCancellable) { release.await() } }
                when (transition) {
                    "operation" -> {
                        val post = CompletableDeferred<PasswordChangeEmailRequestResult?>()
                        model.passwordChangeRequestHandler = { post.await() }
                        viewModel.onRequestPasswordChangeEmail()
                        assertNull(viewModel.passwordChangeEmailResultState.value)
                        post.complete(PasswordChangeEmailRequestResult.DeliveryFailed)
                    }
                    else -> {
                        viewModel.onRefreshEmail()
                        runCurrent()
                        when (transition) {
                            "logout" -> model.authorisedState.value = false
                            "caller" -> model.currentUserIdState.value = otherId
                            "target" -> node.retarget(otherId)
                            "destroy" -> viewModel.scope.coroutineContext[Job]?.cancel()
                        }
                    }
                }
                runCurrent()
                if (transition != "operation") assertNull(viewModel.passwordChangeEmailResultState.value)
                release.complete(profile())
                advanceUntilIdle()
                assertEquals(if (transition == "operation") PasswordChangeEmailRequestResult.DeliveryFailed else null, viewModel.passwordChangeEmailResultState.value)
                assertEquals(if (transition == "operation") 2 else 1, model.passwordChangeRequestedEmails.size)
            } finally {
                release.complete(profile())
                close(viewModel)
            }
        }
    }

}
