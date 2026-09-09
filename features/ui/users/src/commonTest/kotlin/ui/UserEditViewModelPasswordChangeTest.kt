package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequestResult
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Exercises synchronous authorization and result handling for the owner password-email action. */
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
    ) = AuthFeatureUser(id, Username("owner"), email = email, emailApproved = approved)

    /** Creates a serial-dispatcher ViewModel and completes its initial owner reconciliation. */
    private suspend fun createViewModel(
        model: UserEditTestUsersModel,
        owner: UserId = ownerId,
    ): UserEditViewModel = UserEditViewModel(
        userEditTestNode(owner),
        model,
        RecordingUserEditInteractor(),
        UnconfinedTestDispatcher(),
    )

    /** Confirms ordinary owners and root editing their own profile issue exactly the displayed address. */
    @Test
    fun ownerAndRootOnSelfIssueOnlyDisplayedApprovedEmailWithoutOtherMutations() = runTest {
        listOf(false, true).forEach { root ->
            val model = UserEditTestUsersModel(ownerId, profile()).apply { rootState.value = root }
            val viewModel = createViewModel(model)
            try {
                advanceUntilIdle()
                viewModel.onRequestPasswordChangeEmail()
                advanceUntilIdle()
                assertEquals(listOf(approvedEmail), model.passwordChangeRequestedEmails)
                assertEquals(PasswordChangeEmailRequestResult.Sent, viewModel.passwordChangeEmailResultState.value)
                assertTrue(model.requestedEmails.isEmpty())
                assertTrue(model.passwordUpdates.isEmpty())
                assertTrue(model.usernameUpdates.isEmpty())
            } finally {
                viewModel.scope.cancel()
            }
        }
    }

    /** Rejects every caller, profile, and capability state that cannot prove an eligible current owner. */
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
        )
        cases.forEach { model ->
            val viewModel = createViewModel(model)
            try {
                advanceUntilIdle()
                viewModel.onRequestPasswordChangeEmail()
                advanceUntilIdle()
                assertTrue(model.passwordChangeRequestedEmails.isEmpty())
                assertTrue(model.requestedEmails.isEmpty())
                assertTrue(model.passwordUpdates.isEmpty())
                assertTrue(model.usernameUpdates.isEmpty())
            } finally {
                viewModel.scope.cancel()
            }
        }
    }

    /** Rejects an immediate repeated click while the first owner-bound request holds the busy slot. */
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
            release.complete(Unit)
            advanceUntilIdle()
            assertFalse(viewModel.emailBusyState.value)
        } finally {
            release.complete(Unit)
            viewModel.scope.cancel()
        }
    }

    /** Separates domain results and null transport uncertainty while reconciling before a retry. */
    @Test
    fun passwordChangeFeedbackIsDistinctAndEligibleRetryReconcilesProfile() = runTest {
        val outcomes = listOf(
            PasswordChangeEmailRequestResult.Sent,
            PasswordChangeEmailRequestResult.Ineligible,
            PasswordChangeEmailRequestResult.DeliveryFailed,
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
                viewModel.scope.cancel()
            }
        }
    }

    /** Rejects stale derived eligibility and admits an immediately reconciled valid raw profile. */
    @Test
    fun rawAdmissionOverridesStaleDerivedEligibilityInBothDirections() = runTest {
        val model = UserEditTestUsersModel(ownerId, profile())
        val viewModel = createViewModel(model)
        try {
            advanceUntilIdle()
            model.authorisedState.value = false
            viewModel.onRequestPasswordChangeEmail()
            runCurrent()
            assertTrue(model.passwordChangeRequestedEmails.isEmpty())

            model.authorisedState.value = true
            model.currentUserIdState.value = ownerId
            viewModel.onRefreshEmail()
            advanceUntilIdle()
            viewModel.onRequestPasswordChangeEmail()
            advanceUntilIdle()
            assertEquals(listOf(approvedEmail), model.passwordChangeRequestedEmails)
        } finally {
            viewModel.scope.cancel()
        }
    }

    /** Suppresses a suspended request after identity loss without feedback or a reconciliation read. */
    @Test
    fun stalePasswordChangeCompletionCannotPublishOrClearLaterMutation() = runTest {
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
            val readsBefore = model.profileReads
            viewModel.onRequestPasswordChangeEmail()
            runCurrent()
            assertTrue(entered.isCompleted)
            model.currentUserIdState.value = otherId
            runCurrent()
            release.complete(Unit)
            advanceUntilIdle()
            assertNull(viewModel.passwordChangeEmailResultState.value)
            assertNull(viewModel.ownEmailProfileState.value)
            assertEquals(readsBefore, model.profileReads)
            assertFalse(viewModel.emailBusyState.value)
        } finally {
            release.complete(Unit)
            viewModel.scope.cancel()
        }
    }
}
