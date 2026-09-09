package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.auth.common.models.CompletePasswordChangeRequest
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequestResult
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerId
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerInfo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.email.common.models.Email
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Non-copyable sentinel for removal failure before underlying persistence delegation. */
private object RemovalBeforeDelegationFailure : IllegalStateException("remove before delegation")

/** Non-copyable sentinel for removal failure after underlying persistence delegation. */
private object RemovalAfterDelegationFailure : IllegalStateException("remove after delegation")

/** Non-copyable sentinel for password-write failure before underlying persistence delegation. */
private object PasswordBeforeDelegationFailure : IllegalStateException("password before delegation")

/** Non-copyable sentinel for password-write failure after underlying persistence delegation. */
private object PasswordAfterDelegationFailure : IllegalStateException("password after delegation")

/** Exercises final approval reread, consume, password-write, and lock-release integrity boundaries. */
@OptIn(ExperimentalCoroutinesApi::class)
class EmailPasswordChangeCommitTest {
    /** Returns one delivered approval and clears issuance evidence before the commit assertion starts. */
    private suspend fun issuedFixture(): Pair<PasswordChangeFixture, DeepLinkId> {
        val fixture = PasswordChangeTestFixtures.fixture()
        assertEquals(
            PasswordChangeEmailRequestResult.Sent,
            fixture.service.requestPasswordChangeEmail(fixture.user.id, fixture.user.email!!),
        )
        val approvalId = fixture.linksRepo.setIds.single()
        fixture.linksRepo.resetOperationRecords()
        fixture.passwords.resetIssuedPasswordWriteCount()
        return fixture to approvalId
    }

    /** Creates one valid completion request for the fixture's approval-bound subject. */
    private fun completionRequest(fixture: PasswordChangeFixture, approvalId: DeepLinkId): CompletePasswordChangeRequest =
        CompletePasswordChangeRequest(fixture.user.id, approvalId, Password("new-password"))

    /** Captures the exact throwable object emitted by one suspend operation. */
    private suspend inline fun <reified T : Throwable> captureFailure(action: suspend () -> Unit): T = try {
        action()
        error("Expected ${T::class.simpleName}")
    } catch (error: Throwable) {
        if (error is T) error else throw error
    }

    /** Proves a subsequent coordinator-then-Auth operation completes after a failed commit boundary. */
    private suspend fun assertLocksCanBeReacquired(fixture: PasswordChangeFixture) {
        assertTrue(fixture.coordinator.getCurrentUser(fixture.user.id) != null)
        assertTrue(fixture.auth.passwordChangeState(fixture.user.id) != null)
        assertEquals(
            PasswordChangeEmailRequestResult.Sent,
            fixture.service.requestPasswordChangeEmail(fixture.user.id, fixture.user.email!!),
        )
    }

    /** Two overlapping valid submitters serialize at final reread and produce one consume/write only. */
    @Test
    fun overlappingSubmittersProduceOneChangedAndOnePasswordWrite() = runTest {
        val (fixture, approvalId) = issuedFixture()
        val finalRereadEntered = CompletableDeferred<Unit>()
        val releaseFinalReread = CompletableDeferred<Unit>()
        val secondInitialRead = CompletableDeferred<Unit>()
        var readCount = 0
        fixture.linksRepo.beforeGet = {
            readCount++
            when (readCount) {
                2 -> {
                    finalRereadEntered.complete(Unit)
                    releaseFinalReread.await()
                }
                3 -> secondInitialRead.complete(Unit)
            }
        }
        val request = completionRequest(fixture, approvalId)
        val first = async { fixture.service.completePasswordChange(request) }
        runCurrent()
        finalRereadEntered.await()
        val second = async { fixture.service.completePasswordChange(request) }
        runCurrent()
        secondInitialRead.await()
        assertEquals(0, fixture.passwords.issuedPasswordWriteCount)
        releaseFinalReread.complete(Unit)

        val results = listOf(first.await(), second.await())
        assertEquals(1, results.count { it == PasswordChangeResult.Changed })
        assertEquals(1, results.count { it == PasswordChangeResult.InvalidApproval })
        assertEquals(1, fixture.passwords.issuedPasswordWriteCount)
        assertEquals(listOf(approvalId), fixture.linksRepo.unsetIds)
        assertNull(fixture.linksRepo.get(approvalId))
    }

    /** A persisted link replaced after the initial read cannot be consumed at the final reread. */
    @Test
    fun changedLinkBeforeFinalRereadRejectsWithoutConsumeOrPasswordWrite() = runTest {
        val (fixture, approvalId) = issuedFixture()
        val initialRead = CompletableDeferred<Unit>()
        val releaseInitialRead = CompletableDeferred<Unit>()
        var reads = 0
        fixture.linksRepo.afterGet = {
            reads++
            if (reads == 1) {
                initialRead.complete(Unit)
                releaseInitialRead.await()
            }
        }
        val completion = async { fixture.service.completePasswordChange(completionRequest(fixture, approvalId)) }
        initialRead.await()
        fixture.linksRepo.seed(approvalId, DeepLinkHandlerInfo(DeepLinkHandlerId("changed"), "replacement"))
        releaseInitialRead.complete(Unit)

        assertEquals(PasswordChangeResult.InvalidApproval, completion.await())
        assertTrue(fixture.linksRepo.get(approvalId) != null)
        assertTrue(fixture.linksRepo.unsetIds.isEmpty())
        assertEquals(0, fixture.passwords.issuedPasswordWriteCount)
    }

    /** A persisted link removed after the initial read cannot produce a second removal or password write. */
    @Test
    fun removedLinkBeforeFinalRereadRejectsWithoutPasswordWrite() = runTest {
        val (fixture, approvalId) = issuedFixture()
        val initialRead = CompletableDeferred<Unit>()
        val releaseInitialRead = CompletableDeferred<Unit>()
        var reads = 0
        fixture.linksRepo.afterGet = {
            reads++
            if (reads == 1) {
                initialRead.complete(Unit)
                releaseInitialRead.await()
            }
        }
        val completion = async { fixture.service.completePasswordChange(completionRequest(fixture, approvalId)) }
        initialRead.await()
        fixture.links.removeDeepLink(approvalId)
        fixture.linksRepo.resetOperationRecords()
        releaseInitialRead.complete(Unit)

        assertEquals(PasswordChangeResult.InvalidApproval, completion.await())
        assertNull(fixture.linksRepo.get(approvalId))
        assertTrue(fixture.linksRepo.unsetIds.isEmpty())
        assertEquals(0, fixture.passwords.issuedPasswordWriteCount)
    }

    /** A coordinated email update waits behind final commit and progresses after coordinator release. */
    @Test
    fun coordinatedEmailUpdateWaitsForCommitAndThenProgresses() = runTest {
        val (fixture, approvalId) = issuedFixture()
        val finalRereadEntered = CompletableDeferred<Unit>()
        val releaseFinalReread = CompletableDeferred<Unit>()
        var reads = 0
        fixture.linksRepo.beforeGet = {
            reads++
            if (reads == 2) {
                finalRereadEntered.complete(Unit)
                releaseFinalReread.await()
            }
        }
        val completion = async { fixture.service.completePasswordChange(completionRequest(fixture, approvalId)) }
        runCurrent()
        finalRereadEntered.await()
        val update = async { fixture.coordinator.updateStoredEmail(fixture.user.id, Email("changed@example.com")) }
        runCurrent()
        assertFalse(update.isCompleted)
        releaseFinalReread.complete(Unit)

        assertEquals(PasswordChangeResult.Changed, completion.await())
        assertTrue(update.await())
        assertEquals(1, fixture.passwords.issuedPasswordWriteCount)
    }

    /** An Auth password update waits behind final commit and progresses after Auth lock release. */
    @Test
    fun authSetPasswordWaitsForCommitAndThenProgresses() = runTest {
        val (fixture, approvalId) = issuedFixture()
        val finalRereadEntered = CompletableDeferred<Unit>()
        val releaseFinalReread = CompletableDeferred<Unit>()
        var reads = 0
        fixture.linksRepo.beforeGet = {
            reads++
            if (reads == 2) {
                finalRereadEntered.complete(Unit)
                releaseFinalReread.await()
            }
        }
        val completion = async { fixture.service.completePasswordChange(completionRequest(fixture, approvalId)) }
        runCurrent()
        finalRereadEntered.await()
        val replacement = async { fixture.auth.setPassword(fixture.user.id, Password("admin-password")) }
        runCurrent()
        assertFalse(replacement.isCompleted)
        releaseFinalReread.complete(Unit)

        assertEquals(PasswordChangeResult.Changed, completion.await())
        replacement.await()
        assertEquals(2, fixture.passwords.issuedPasswordWriteCount)
        assertTrue(fixture.auth.login(fixture.user.username, Password("admin-password")) != null)
    }

    /** Auth purge waits behind final commit and progresses after Auth lock release without deadlock. */
    @Test
    fun authPurgeUserWaitsForCommitAndThenProgresses() = runTest {
        val (fixture, approvalId) = issuedFixture()
        val finalRereadEntered = CompletableDeferred<Unit>()
        val releaseFinalReread = CompletableDeferred<Unit>()
        var reads = 0
        fixture.linksRepo.beforeGet = {
            reads++
            if (reads == 2) {
                finalRereadEntered.complete(Unit)
                releaseFinalReread.await()
            }
        }
        val completion = async { fixture.service.completePasswordChange(completionRequest(fixture, approvalId)) }
        runCurrent()
        finalRereadEntered.await()
        val purge = async { fixture.auth.purgeUser(fixture.user.id) }
        runCurrent()
        assertFalse(purge.isCompleted)
        releaseFinalReread.complete(Unit)

        assertEquals(PasswordChangeResult.Changed, completion.await())
        purge.await()
        assertNull(fixture.auth.login(fixture.user.username, Password("new-password")))
    }

    /** A removal failure before delegation leaves approval and old password intact without a write. */
    @Test
    fun removalFailureBeforeDelegationLeavesApprovalAndOldPassword() = runTest {
        val (fixture, approvalId) = issuedFixture()
        fixture.linksRepo.beforeUnset = { throw RemovalBeforeDelegationFailure }

        val thrown = captureFailure<IllegalStateException> {
            fixture.service.completePasswordChange(completionRequest(fixture, approvalId))
        }

        assertTrue(thrown === RemovalBeforeDelegationFailure)
        assertTrue(fixture.linksRepo.get(approvalId) != null)
        assertEquals(0, fixture.passwords.issuedPasswordWriteCount)
        assertTrue(fixture.auth.login(fixture.user.username, PasswordChangeTestFixtures.oldPassword) != null)
        fixture.linksRepo.beforeUnset = null
        assertLocksCanBeReacquired(fixture)
    }

    /** A removal failure after delegation consumes approval but never starts a password write. */
    @Test
    fun removalFailureAfterDelegationConsumesApprovalWithoutPasswordWrite() = runTest {
        val (fixture, approvalId) = issuedFixture()
        fixture.linksRepo.afterUnset = { throw RemovalAfterDelegationFailure }

        val thrown = captureFailure<IllegalStateException> {
            fixture.service.completePasswordChange(completionRequest(fixture, approvalId))
        }

        assertTrue(thrown === RemovalAfterDelegationFailure)
        assertNull(fixture.linksRepo.get(approvalId))
        assertEquals(0, fixture.passwords.issuedPasswordWriteCount)
        assertEquals(PasswordChangeResult.InvalidApproval, fixture.service.completePasswordChange(completionRequest(fixture, approvalId)))
        fixture.linksRepo.afterUnset = null
        assertLocksCanBeReacquired(fixture)
    }

    /** A password failure before delegation consumes approval but keeps the old credential and rejects replay. */
    @Test
    fun passwordFailureBeforeDelegationConsumesApprovalAndKeepsOldCredential() = runTest {
        val (fixture, approvalId) = issuedFixture()
        fixture.passwords.beforeSet = { throw PasswordBeforeDelegationFailure }

        val thrown = captureFailure<IllegalStateException> {
            fixture.service.completePasswordChange(completionRequest(fixture, approvalId))
        }

        assertTrue(thrown === PasswordBeforeDelegationFailure)
        assertNull(fixture.linksRepo.get(approvalId))
        assertEquals(0, fixture.passwords.issuedPasswordWriteCount)
        assertTrue(fixture.auth.login(fixture.user.username, PasswordChangeTestFixtures.oldPassword) != null)
        assertEquals(PasswordChangeResult.InvalidApproval, fixture.service.completePasswordChange(completionRequest(fixture, approvalId)))
        fixture.passwords.beforeSet = null
        assertLocksCanBeReacquired(fixture)
    }

    /** A password failure after delegation consumes approval, may persist new credential, and rejects replay. */
    @Test
    fun passwordFailureAfterDelegationConsumesApprovalAndMayPersistNewCredential() = runTest {
        val (fixture, approvalId) = issuedFixture()
        fixture.passwords.afterSet = { throw PasswordAfterDelegationFailure }

        val thrown = captureFailure<IllegalStateException> {
            fixture.service.completePasswordChange(completionRequest(fixture, approvalId))
        }

        assertTrue(thrown === PasswordAfterDelegationFailure)
        assertNull(fixture.linksRepo.get(approvalId))
        assertEquals(1, fixture.passwords.issuedPasswordWriteCount)
        assertTrue(fixture.auth.login(fixture.user.username, Password("new-password")) != null)
        assertEquals(PasswordChangeResult.InvalidApproval, fixture.service.completePasswordChange(completionRequest(fixture, approvalId)))
        fixture.passwords.afterSet = null
        assertLocksCanBeReacquired(fixture)
    }

    /** Cancellation before the non-cancellable consume/write region leaves approval and password unchanged. */
    @Test
    fun cancellationBeforeCommitLeavesApprovalAndPasswordUntouched() = runTest {
        val (fixture, approvalId) = issuedFixture()
        val authReadEntered = CompletableDeferred<Unit>()
        val releaseAuthRead = CompletableDeferred<Unit>()
        fixture.passwords.beforeGet = {
            authReadEntered.complete(Unit)
            releaseAuthRead.await()
        }
        val completion = async { fixture.service.completePasswordChange(completionRequest(fixture, approvalId)) }
        runCurrent()
        authReadEntered.await()
        completion.cancel()
        runCurrent()

        captureFailure<CancellationException> { completion.await() }
        assertTrue(fixture.linksRepo.get(approvalId) != null)
        assertEquals(0, fixture.passwords.issuedPasswordWriteCount)
        fixture.passwords.beforeGet = null
        releaseAuthRead.complete(Unit)
        assertLocksCanBeReacquired(fixture)
    }

    /** Cancellation during non-cancellable removal completes consume/write, propagates cancellation, and rejects replay. */
    @Test
    fun cancellationDuringRemovalCompletesCommitThenPropagatesCancellation() = runTest {
        val (fixture, approvalId) = issuedFixture()
        val removalEntered = CompletableDeferred<Unit>()
        val releaseRemoval = CompletableDeferred<Unit>()
        fixture.linksRepo.beforeUnset = {
            removalEntered.complete(Unit)
            releaseRemoval.await()
        }
        val completion = async { fixture.service.completePasswordChange(completionRequest(fixture, approvalId)) }
        runCurrent()
        removalEntered.await()
        completion.cancel()
        releaseRemoval.complete(Unit)
        runCurrent()

        captureFailure<CancellationException> { completion.await() }
        assertNull(fixture.linksRepo.get(approvalId))
        assertEquals(1, fixture.passwords.issuedPasswordWriteCount)
        assertEquals(PasswordChangeResult.InvalidApproval, fixture.service.completePasswordChange(completionRequest(fixture, approvalId)))
        fixture.linksRepo.beforeUnset = null
        assertLocksCanBeReacquired(fixture)
    }

    /** Cancellation during non-cancellable password storage completes write, propagates cancellation, and rejects replay. */
    @Test
    fun cancellationDuringPasswordWriteCompletesCommitThenPropagatesCancellation() = runTest {
        val (fixture, approvalId) = issuedFixture()
        val writeEntered = CompletableDeferred<Unit>()
        val releaseWrite = CompletableDeferred<Unit>()
        fixture.passwords.beforeSet = {
            writeEntered.complete(Unit)
            releaseWrite.await()
        }
        val completion = async { fixture.service.completePasswordChange(completionRequest(fixture, approvalId)) }
        runCurrent()
        writeEntered.await()
        completion.cancel()
        releaseWrite.complete(Unit)
        runCurrent()

        captureFailure<CancellationException> { completion.await() }
        assertNull(fixture.linksRepo.get(approvalId))
        assertEquals(1, fixture.passwords.issuedPasswordWriteCount)
        assertTrue(fixture.auth.login(fixture.user.username, Password("new-password")) != null)
        assertEquals(PasswordChangeResult.InvalidApproval, fixture.service.completePasswordChange(completionRequest(fixture, approvalId)))
        fixture.passwords.beforeSet = null
        assertLocksCanBeReacquired(fixture)
    }
}
