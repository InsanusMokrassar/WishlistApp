package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequestResult
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerInfo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerId
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.EmailsService
import dev.inmo.wishlist.features.email.server.models.EmailAttachment
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** SMTP double with gates at send entry and immediately before its configured outcome returns. */
private class ControlledIssuanceEmailsService : EmailsService {
    /** Signal completed when SMTP send begins. */
    val started = CompletableDeferred<Unit>()

    /** Optional suspendable action performed before SMTP reports an outcome. */
    var beforeReturn: suspend () -> Unit = {}

    /** SMTP outcome returned after [beforeReturn] completes. */
    var result: Boolean = true

    /** Optional ordinary or cancellation failure reported by SMTP. */
    var failure: Throwable? = null

    /** Plain-text delivery is outside password-approval issuance. */
    override suspend fun sendText(recipient: Email, subject: String, text: String): Boolean = error("unused")

    /** Attachment delivery is outside password-approval issuance. */
    override suspend fun sendTextWithAttachments(
        recipient: Email,
        subject: String,
        text: String,
        attachments: List<EmailAttachment>,
    ): Boolean = error("unused")

    /** Announces SMTP acceptance, runs the gate, then returns or throws the configured outcome. */
    override suspend fun sendHtml(recipient: Email, subject: String, html: String): Boolean {
        started.complete(Unit)
        beforeReturn()
        failure?.let { throw it }
        return result
    }
}

/** Non-copyable sentinel for proving the real mint failure object survives coroutine boundaries. */
private object MintBeforeWriteFailure : IllegalStateException("mint before write")

/** Non-copyable sentinel for proving DeepLinksService preserves a post-write mint failure. */
private object MintWriteThenThrowFailure : IllegalStateException("mint response lost")

/** Non-copyable sentinel for proving exact cleanup-failure precedence and suppression. */
private object CleanupFailure : IllegalStateException("remove failed")

/** Non-copyable sentinel for proving SMTP failure precedence. */
private object SmtpFailure : IllegalStateException("smtp failed")

/** Non-copyable sentinel for proving post-send Auth-read failure precedence. */
private object AuthReadFailure : IllegalStateException("credential read failed")

/** Covers every issuance outcome at real Email, Auth, coordinator, and deeplink boundaries. */
@OptIn(ExperimentalCoroutinesApi::class)
class EmailPasswordChangeIssuanceTest {
    /** Captures the exact throwable object emitted by one suspend operation. */
    private suspend inline fun <reified T : Throwable> captureFailure(action: suspend () -> Unit): T = try {
        action()
        error("Expected ${T::class.simpleName}")
    } catch (error: Throwable) {
        if (error is T) error else throw error
    }

    /** Preserves one unrelated and one sibling record while one owned approval is cleaned. */
    private suspend fun seedUnrelatedRecords(fixture: PasswordChangeFixture): Map<DeepLinkId, DeepLinkHandlerInfo> {
        val siblingId = DeepLinkId("11111111-1111-1111-1111-111111111111")
        val unrelatedId = DeepLinkId("22222222-2222-2222-2222-222222222222")
        fixture.linksRepo.seed(siblingId, DeepLinkHandlerInfo(DeepLinkHandlerId("sibling"), "sibling"))
        fixture.linksRepo.seed(unrelatedId, DeepLinkHandlerInfo(DeepLinkHandlerId("unrelated"), "unrelated"))
        fixture.linksRepo.resetOperationRecords()
        return fixture.linksRepo.getAll()
    }

    /** A mint failure before persistence delegation retains its real DeepLinksService cleanup behavior. */
    @Test
    fun mintBeforeWriteFailurePropagatesAndCleansExactlyAllocatedId() = runTest {
        val linksRepo = PasswordChangeDeepLinksRepo()
        val failure = MintBeforeWriteFailure
        linksRepo.beforeSet = { throw failure }
        val fixture = PasswordChangeTestFixtures.fixture(linksRepo = linksRepo)
        val preserved = seedUnrelatedRecords(fixture)

        val thrown = captureFailure<IllegalStateException> {
            fixture.service.requestPasswordChangeEmail(fixture.user.id, fixture.user.email!!)
        }

        assertSame(failure, thrown)
        assertEquals(1, linksRepo.setIds.size)
        assertEquals(listOf(linksRepo.setIds.single()), linksRepo.unsetIds)
        assertEquals(preserved, linksRepo.getAll())
        assertEquals(0, fixture.passwords.issuedPasswordWriteCount)
    }

    /** A real DeepLinksService mint write that loses its response keeps its own primary and suppressed failures. */
    @Test
    fun mintWriteThenThrowPreservesDeepLinksPrimaryAndCleanupFailure() = runTest {
        val linksRepo = PasswordChangeDeepLinksRepo()
        val mintFailure = MintWriteThenThrowFailure
        val cleanupFailure = CleanupFailure
        linksRepo.afterSet = { throw mintFailure }
        linksRepo.beforeUnset = { throw cleanupFailure }
        val fixture = PasswordChangeTestFixtures.fixture(linksRepo = linksRepo)
        val preserved = seedUnrelatedRecords(fixture)

        val thrown = captureFailure<IllegalStateException> {
            fixture.service.requestPasswordChangeEmail(fixture.user.id, fixture.user.email!!)
        }

        assertSame(mintFailure, thrown)
        assertEquals(listOf(cleanupFailure), thrown.suppressedExceptions.toList())
        assertEquals(1, linksRepo.setIds.size)
        assertEquals(listOf(linksRepo.setIds.single()), linksRepo.unsetIds)
        assertEquals(preserved + (linksRepo.setIds.single() to fixture.linksRepo.get(linksRepo.setIds.single())!!), linksRepo.getAll())
        assertEquals(0, fixture.passwords.issuedPasswordWriteCount)
    }

    /** A false SMTP response returns DeliveryFailed only after exactly one successful owned-id removal. */
    @Test
    fun falseSmtpRemovesOwnedApprovalAndPreservesSiblingRecords() = runTest {
        val emails = ControlledIssuanceEmailsService().apply { result = false }
        val fixture = PasswordChangeTestFixtures.fixture(emails)
        val preserved = seedUnrelatedRecords(fixture)

        assertEquals(
            PasswordChangeEmailRequestResult.DeliveryFailed,
            fixture.service.requestPasswordChangeEmail(fixture.user.id, fixture.user.email!!),
        )

        assertEquals(1, fixture.linksRepo.setIds.size)
        assertEquals(listOf(fixture.linksRepo.setIds.single()), fixture.linksRepo.unsetIds)
        assertEquals(preserved, fixture.linksRepo.getAll())
        assertEquals(0, fixture.passwords.issuedPasswordWriteCount)
    }

    /** A false SMTP response exposes a first removal failure instead of falsely reporting delivery failure. */
    @Test
    fun falseSmtpRemovalFailureIsPrimaryAndAttemptsExactIdOnce() = runTest {
        val cleanupFailure = CleanupFailure
        val fixture = PasswordChangeTestFixtures.fixture(ControlledIssuanceEmailsService().apply { result = false })
        val preserved = seedUnrelatedRecords(fixture)
        fixture.linksRepo.beforeUnset = { throw cleanupFailure }

        val thrown = captureFailure<IllegalStateException> {
            fixture.service.requestPasswordChangeEmail(fixture.user.id, fixture.user.email!!)
        }

        assertSame(cleanupFailure, thrown)
        assertEquals(listOf(fixture.linksRepo.setIds.single()), fixture.linksRepo.unsetIds)
        assertEquals(preserved + (fixture.linksRepo.setIds.single() to fixture.linksRepo.get(fixture.linksRepo.setIds.single())!!), fixture.linksRepo.getAll())
        assertEquals(0, fixture.passwords.issuedPasswordWriteCount)
    }

    /** An ordinary SMTP exception becomes DeliveryFailed after successful one-attempt cleanup. */
    @Test
    fun ordinarySmtpExceptionReturnsDeliveryFailedAfterCleanup() = runTest {
        val fixture = PasswordChangeTestFixtures.fixture(
            ControlledIssuanceEmailsService().apply { failure = SmtpFailure },
        )
        val preserved = seedUnrelatedRecords(fixture)

        assertEquals(
            PasswordChangeEmailRequestResult.DeliveryFailed,
            fixture.service.requestPasswordChangeEmail(fixture.user.id, fixture.user.email!!),
        )

        assertEquals(listOf(fixture.linksRepo.setIds.single()), fixture.linksRepo.unsetIds)
        assertEquals(preserved, fixture.linksRepo.getAll())
        assertEquals(0, fixture.passwords.issuedPasswordWriteCount)
    }

    /** An ordinary SMTP exception remains primary when its owned-id cleanup also fails. */
    @Test
    fun ordinarySmtpExceptionPreservesPrimaryWhenCleanupFails() = runTest {
        val smtpFailure = SmtpFailure
        val cleanupFailure = CleanupFailure
        val fixture = PasswordChangeTestFixtures.fixture(
            ControlledIssuanceEmailsService().apply { failure = smtpFailure },
        )
        seedUnrelatedRecords(fixture)
        fixture.linksRepo.beforeUnset = { throw cleanupFailure }

        val thrown = captureFailure<IllegalStateException> {
            fixture.service.requestPasswordChangeEmail(fixture.user.id, fixture.user.email!!)
        }

        assertSame(smtpFailure, thrown)
        assertEquals(listOf(cleanupFailure), thrown.suppressedExceptions.toList())
        assertEquals(listOf(fixture.linksRepo.setIds.single()), fixture.linksRepo.unsetIds)
        assertEquals(0, fixture.passwords.issuedPasswordWriteCount)
    }

    /** A post-send Auth state read failure remains primary and does not write a password. */
    @Test
    fun postSendAuthReadFailureCleansOwnedApprovalAndPreservesFailureIdentity() = runTest {
        val readFailure = AuthReadFailure
        val emails = ControlledIssuanceEmailsService()
        val fixture = PasswordChangeTestFixtures.fixture(emails)
        val preserved = seedUnrelatedRecords(fixture)
        emails.beforeReturn = { fixture.passwords.beforeGet = { throw readFailure } }

        val thrown = captureFailure<IllegalStateException> {
            fixture.service.requestPasswordChangeEmail(fixture.user.id, fixture.user.email!!)
        }

        assertSame(readFailure, thrown)
        assertEquals(listOf(fixture.linksRepo.setIds.single()), fixture.linksRepo.unsetIds)
        assertEquals(preserved, fixture.linksRepo.getAll())
        assertEquals(0, fixture.passwords.issuedPasswordWriteCount)
    }

    /** A post-send Auth read failure retains its identity and suppresses a failed owned-id removal. */
    @Test
    fun postSendAuthReadFailureSuppressesCleanupFailure() = runTest {
        val readFailure = AuthReadFailure
        val cleanupFailure = CleanupFailure
        val emails = ControlledIssuanceEmailsService()
        val fixture = PasswordChangeTestFixtures.fixture(emails)
        seedUnrelatedRecords(fixture)
        emails.beforeReturn = { fixture.passwords.beforeGet = { throw readFailure } }
        fixture.linksRepo.beforeUnset = { throw cleanupFailure }

        val thrown = captureFailure<IllegalStateException> {
            fixture.service.requestPasswordChangeEmail(fixture.user.id, fixture.user.email!!)
        }

        assertSame(readFailure, thrown)
        assertEquals(listOf(cleanupFailure), thrown.suppressedExceptions.toList())
        assertEquals(listOf(fixture.linksRepo.setIds.single()), fixture.linksRepo.unsetIds)
        assertEquals(0, fixture.passwords.issuedPasswordWriteCount)
    }

    /** A delivery-time approved-email change returns Ineligible and removes only the issued approval. */
    @Test
    fun changedAccountStateAfterDeliveryReturnsIneligibleWithExactCleanup() = runTest {
        val emails = ControlledIssuanceEmailsService()
        val fixture = PasswordChangeTestFixtures.fixture(emails)
        val preserved = seedUnrelatedRecords(fixture)
        emails.beforeReturn = {
            fixture.coordinator.updateStoredEmail(fixture.user.id, Email("changed@example.com"))
        }

        assertEquals(
            PasswordChangeEmailRequestResult.Ineligible,
            fixture.service.requestPasswordChangeEmail(fixture.user.id, fixture.user.email!!),
        )

        assertEquals(listOf(fixture.linksRepo.setIds.single()), fixture.linksRepo.unsetIds)
        assertEquals(preserved, fixture.linksRepo.getAll())
        assertEquals(0, fixture.passwords.issuedPasswordWriteCount)
    }

    /** Cancellation observed after the non-cancellable mint enrollment removes the returned exact id. */
    @Test
    fun cancellationAfterMintEnrollmentCleansExactOwnedApproval() = runTest {
        val fixture = PasswordChangeTestFixtures.fixture()
        val preserved = seedUnrelatedRecords(fixture)
        val request = async(start = CoroutineStart.LAZY) {
            fixture.service.requestPasswordChangeEmail(fixture.user.id, fixture.user.email!!)
        }
        fixture.linksRepo.afterSet = { request.cancel() }
        request.start()
        runCurrent()

        captureFailure<kotlinx.coroutines.CancellationException> { request.await() }
        assertEquals(listOf(fixture.linksRepo.setIds.single()), fixture.linksRepo.unsetIds)
        assertEquals(preserved, fixture.linksRepo.getAll())
        assertEquals(0, fixture.passwords.issuedPasswordWriteCount)
    }

    /** Cancellation after SMTP accepted delivery transfers the pending Sent outcome to exact cleanup. */
    @Test
    fun cancellationAfterSmtpAcceptanceCleansExactOwnedApproval() = runTest {
        val emails = ControlledIssuanceEmailsService()
        val fixture = PasswordChangeTestFixtures.fixture(emails)
        val preserved = seedUnrelatedRecords(fixture)
        val request = async(start = CoroutineStart.LAZY) {
            fixture.service.requestPasswordChangeEmail(fixture.user.id, fixture.user.email!!)
        }
        emails.beforeReturn = { request.cancel() }
        request.start()
        runCurrent()

        captureFailure<kotlinx.coroutines.CancellationException> { request.await() }
        assertTrue(emails.started.isCompleted)
        assertEquals(listOf(fixture.linksRepo.setIds.single()), fixture.linksRepo.unsetIds)
        assertEquals(preserved, fixture.linksRepo.getAll())
        assertEquals(0, fixture.passwords.issuedPasswordWriteCount)
    }

    /** Cancellation while SMTP is suspended performs bounded cleanup without awaiting the SMTP gate. */
    @Test
    fun cancellationWhileSmtpSuspendedCleansExactOwnedApproval() = runTest {
        val emails = ControlledIssuanceEmailsService()
        val releaseSmtp = CompletableDeferred<Unit>()
        emails.beforeReturn = { releaseSmtp.await() }
        val fixture = PasswordChangeTestFixtures.fixture(emails)
        val preserved = seedUnrelatedRecords(fixture)
        val request = async {
            fixture.service.requestPasswordChangeEmail(fixture.user.id, fixture.user.email!!)
        }
        runCurrent()
        assertTrue(emails.started.isCompleted)
        request.cancel()
        runCurrent()

        captureFailure<kotlinx.coroutines.CancellationException> { request.await() }
        assertEquals(listOf(fixture.linksRepo.setIds.single()), fixture.linksRepo.unsetIds)
        assertEquals(preserved, fixture.linksRepo.getAll())
        assertEquals(0, fixture.passwords.issuedPasswordWriteCount)
    }

    /** A Sent result retains the issued approval and does not remove sibling or unrelated records. */
    @Test
    fun sentRetainsIssuedApprovalAndFalseDeliveryRemovesOnlyIssuedApproval() = runTest {
        val sentFixture = PasswordChangeTestFixtures.fixture()
        val sentPreserved = seedUnrelatedRecords(sentFixture)

        assertEquals(
            PasswordChangeEmailRequestResult.Sent,
            sentFixture.service.requestPasswordChangeEmail(sentFixture.user.id, sentFixture.user.email!!),
        )

        val sentId = sentFixture.linksRepo.setIds.single()
        assertTrue(sentFixture.linksRepo.get(sentId) != null)
        assertTrue(sentFixture.linksRepo.unsetIds.isEmpty())
        assertEquals(sentPreserved, sentFixture.linksRepo.getAll() - sentId)
        assertEquals(0, sentFixture.passwords.issuedPasswordWriteCount)
        assertNull(sentFixture.linksRepo.get(DeepLinkId("missing")))
    }
}
