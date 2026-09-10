package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.auth.common.models.CompletePasswordChangeRequest
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequestResult
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult
import dev.inmo.wishlist.features.auth.server.services.AuthFeatureService
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerId
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerInfo
import dev.inmo.wishlist.features.deeplinks.common.models.HandleResult
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.EmailsService
import dev.inmo.wishlist.features.email.server.models.EmailPasswordChange
import dev.inmo.wishlist.features.email.server.models.EmailPasswordChangePayload
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.micro_utils.repos.deleteById
import dev.inmo.micro_utils.repos.unset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Email transport test double that mutates credentials immediately before success.
 *
 * @param beforeSuccess Controlled credential mutation executed before successful delivery returns.
 */
private class CredentialReplacingEmailsService(
    /** Concurrent mutation invoked before a successful SMTP result. */
    private val beforeSuccess: suspend () -> Unit,
) : EmailsService by FakeEmailsService() {
    /** Applies the configured concurrent credential mutation before reporting successful delivery. */
    override suspend fun sendHtml(recipient: Email, subject: String, html: String): Boolean {
        beforeSuccess()
        return true
    }
}

/** Service-level authorization, delivery, and completion tests for password approvals. */
class EmailPasswordChangeServiceTest {
    /** Approved account shared by every password-approval test. */
    private val user = PasswordChangeTestFixtures.user

    /** Original credential used to prove completion changes only the approval subject. */
    private val oldPassword = PasswordChangeTestFixtures.oldPassword

    /** Builds an approval fixture with a deferred handler provider, matching the production DI cycle break. */
    private suspend fun fixture(
        emails: EmailsService? = FakeEmailsService(),
        nowEpochMillis: () -> Long = { 1_000L },
        roleBridgePresent: Boolean = true,
    ): PasswordChangeFixture = PasswordChangeTestFixtures.fixture(emails, nowEpochMillis, roleBridgePresent = roleBridgePresent)

    /** Requests one approval, proves GET is read-only, then consumes the exact id once. */
    /** Verifies read-only redirect and one subject-scoped completion. */
    @Test
    fun deliveredApprovalRedirectsReadOnlyAndChangesOnlyItsSubjectOnce() = runTest {
        val fixture = fixture()

        assertEquals(
            PasswordChangeEmailRequestResult.Sent,
            fixture.service.requestPasswordChangeEmail(fixture.user.id, fixture.user.email!!),
        )
        val approvalId = fixture.linksRepo.getAll().keys.single()
        val payload = fixture.linksRepo.get(approvalId)?.value as EmailPasswordChangePayload
        assertEquals(EmailPasswordChange.handlerId, fixture.linksRepo.get(approvalId)?.handlerId)
        assertTrue(payload.toString().contains("redacted"))

        assertEquals(
            HandleResult.Handled.Redirect("/password-change/${fixture.user.id.long}/${approvalId.string}"),
            fixture.links.handle(approvalId),
        )
        assertTrue(fixture.linksRepo.get(approvalId) != null)

        val request = CompletePasswordChangeRequest(fixture.user.id, approvalId, Password("new-password"))
        assertEquals(PasswordChangeResult.Changed, fixture.service.completePasswordChange(request))
        assertNull(fixture.linksRepo.get(approvalId))
        assertNull(fixture.auth.login(fixture.user.username, oldPassword))
        assertTrue(fixture.auth.login(fixture.user.username, Password("new-password")) != null)
        assertEquals(PasswordChangeResult.InvalidApproval, fixture.service.completePasswordChange(request))
    }

    /** Expiry, stale email, revoked direct role, and subject mismatch all reject without password mutation. */
    /** Verifies invalid approval states never replace a password. */
    @Test
    fun invalidApprovalStatesNeverReplaceThePassword() = runTest {
        var now = 1_000L
        val fixture = fixture(nowEpochMillis = { now })
        assertEquals(
            PasswordChangeEmailRequestResult.Sent,
            fixture.service.requestPasswordChangeEmail(fixture.user.id, fixture.user.email!!),
        )
        val approvalId = fixture.linksRepo.getAll().keys.single()
        val validRequest = CompletePasswordChangeRequest(fixture.user.id, approvalId, Password("new-password"))

        now += 15L * 60L * 1000L
        assertEquals(PasswordChangeResult.InvalidApproval, fixture.service.completePasswordChange(validRequest))
        assertTrue(fixture.auth.login(fixture.user.username, oldPassword) != null)

        now = 1_000L
        fixture.coordinator.updateStoredEmail(fixture.user.id, Email("changed@example.com"))
        assertEquals(PasswordChangeResult.InvalidApproval, fixture.service.completePasswordChange(validRequest))
        assertTrue(fixture.auth.login(fixture.user.username, oldPassword) != null)

        fixture.roles.directRolePresent = false
        assertEquals(PasswordChangeResult.InvalidApproval, fixture.service.completePasswordChange(validRequest))
        assertTrue(fixture.auth.login(fixture.user.username, oldPassword) == null)
    }

    /** Missing infrastructure, a false send, and delivery cancellation retain no exact newly minted link. */
    /** Verifies unavailable and cancelled delivery fail closed with exact cleanup. */
    @Test
    fun unavailableAndCancelledDeliveryFailClosedWithExactLinkCleanup() = runTest {
        val unavailable = fixture(emails = null)
        assertEquals(
            PasswordChangeEmailRequestResult.Unavailable,
            unavailable.service.requestPasswordChangeEmail(unavailable.user.id, unavailable.user.email!!),
        )
        assertTrue(unavailable.linksRepo.getAll().isEmpty())

        val failedDelivery = fixture(emails = FakeEmailsService(result = false))
        assertEquals(
            PasswordChangeEmailRequestResult.DeliveryFailed,
            failedDelivery.service.requestPasswordChangeEmail(failedDelivery.user.id, failedDelivery.user.email!!),
        )
        assertTrue(failedDelivery.linksRepo.getAll().isEmpty())

        val cancelledDelivery = fixture(emails = FakeEmailsService(failure = CancellationException("cancelled")))
        assertFailsWith<CancellationException> {
            cancelledDelivery.service.requestPasswordChangeEmail(cancelledDelivery.user.id, cancelledDelivery.user.email!!)
        }
        assertTrue(cancelledDelivery.linksRepo.getAll().isEmpty())

        val thrownDelivery = fixture(emails = FakeEmailsService(failure = IllegalStateException("smtp failed")))
        assertEquals(
            PasswordChangeEmailRequestResult.DeliveryFailed,
            thrownDelivery.service.requestPasswordChangeEmail(thrownDelivery.user.id, thrownDelivery.user.email!!),
        )
        assertTrue(thrownDelivery.linksRepo.getAll().isEmpty())
    }

    /** SMTP receives the approved address and one fixed URL containing the persisted approval id. */
    /** Verifies issuance uses the exact approved recipient and persisted link. */
    @Test
    fun sentApprovalUsesExactApprovedRecipientAndPersistedLink() = runTest {
        val emails = FakeEmailsService()
        val fixture = fixture(emails)
        assertEquals(PasswordChangeEmailRequestResult.Sent, fixture.service.requestPasswordChangeEmail(fixture.user.id, fixture.user.email!!))
        val approvalId = fixture.linksRepo.getAll().keys.single()
        assertEquals(fixture.user.email, emails.sendHtmlCalls.single().recipient)
        assertTrue(emails.sendHtmlCalls.single().html.contains("https://wishlist.example/api/links/${approvalId.string}"))
        assertTrue(fixture.linksRepo.get(approvalId) != null)
    }

    /** Credential replacement during SMTP causes post-delivery validation to remove the new approval. */
    /** Verifies credential change invalidates and cleans the consumed approval. */
    @Test
    fun postDeliveryCredentialChangeInvalidatesAndCleansTheApproval() = runTest {
        lateinit var createdFixture: PasswordChangeFixture
        val emails = CredentialReplacingEmailsService {
            createdFixture.auth.setPassword(createdFixture.user.id, Password("replacement-password"))
        }
        createdFixture = fixture(emails = emails)

        assertEquals(
            PasswordChangeEmailRequestResult.Ineligible,
            createdFixture.service.requestPasswordChangeEmail(createdFixture.user.id, createdFixture.user.email!!),
        )
        assertTrue(createdFixture.linksRepo.getAll().isEmpty())
        assertTrue(createdFixture.auth.login(createdFixture.user.username, Password("replacement-password")) != null)
    }

    /** Concurrent completion attempts with one persisted UUID can accept at most one password write. */
    /** Verifies concurrent completion consumes at most one approval. */
    @Test
    fun concurrentCompletionConsumesAtMostOneApproval() = runTest {
        val fixture = fixture()
        assertEquals(
            PasswordChangeEmailRequestResult.Sent,
            fixture.service.requestPasswordChangeEmail(fixture.user.id, fixture.user.email!!),
        )
        val approvalId = fixture.linksRepo.getAll().keys.single()
        val request = CompletePasswordChangeRequest(fixture.user.id, approvalId, Password("new-password"))

        val outcomes = listOf(
            async { fixture.service.completePasswordChange(request) },
            async { fixture.service.completePasswordChange(request) },
        ).awaitAll()

        assertEquals(1, outcomes.count { it == PasswordChangeResult.Changed })
        assertEquals(1, outcomes.count { it == PasswordChangeResult.InvalidApproval })
        assertNull(fixture.linksRepo.get(approvalId))
        assertFalse(fixture.auth.login(fixture.user.username, oldPassword) != null)
    }

    /** Independent malformed, unknown, consumed, handler, payload, and subject cases never write a password. */
    /** Verifies invalid authorization and policy states produce no password writes. */
    @Test
    fun invalidApprovalMatrixFailsClosedWithoutPasswordWrites() = runTest {
        /** Issues one real approval and clears bootstrap write observations for a negative case. */
        suspend fun issued(): Pair<PasswordChangeFixture, DeepLinkId> {
            val fixture = fixture()
            assertEquals(PasswordChangeEmailRequestResult.Sent, fixture.service.requestPasswordChangeEmail(fixture.user.id, fixture.user.email!!))
            fixture.passwords.resetIssuedPasswordWriteCount()
            return fixture to fixture.linksRepo.getAll().keys.single()
        }
        /** Completes one invalid request and proves the password repository remains untouched. */
        suspend fun invalid(fixture: PasswordChangeFixture, request: CompletePasswordChangeRequest) {
            assertEquals(PasswordChangeResult.InvalidApproval, fixture.service.completePasswordChange(request))
            assertEquals(0, fixture.passwords.issuedPasswordWriteCount)
        }
        run {
            val (fixture, id) = issued()
            invalid(fixture, CompletePasswordChangeRequest(fixture.user.id, DeepLinkId("123e4567-e89b-42d3-a456-426614174001"), Password("new-password")))
            invalid(fixture, CompletePasswordChangeRequest(fixture.user.id, DeepLinkId("not-a-uuid"), Password("new-password")))
            assertEquals(PasswordChangeResult.Changed, fixture.service.completePasswordChange(CompletePasswordChangeRequest(fixture.user.id, id, Password("new-password"))))
            assertEquals(PasswordChangeResult.InvalidApproval, fixture.service.completePasswordChange(CompletePasswordChangeRequest(fixture.user.id, id, Password("another-password"))))
        }
        run { val (fixture, id) = issued(); fixture.linksRepo.seed(id, DeepLinkHandlerInfo(DeepLinkHandlerId("wrong.handler"), "wrong payload")); invalid(fixture, CompletePasswordChangeRequest(fixture.user.id, id, Password("new-password"))) }
        run { val (fixture, id) = issued(); fixture.linksRepo.seed(id, DeepLinkHandlerInfo(EmailPasswordChange.handlerId, "wrong payload")); invalid(fixture, CompletePasswordChangeRequest(fixture.user.id, id, Password("new-password"))) }
        run { val (fixture, id) = issued(); invalid(fixture, CompletePasswordChangeRequest(UserId(99L), id, Password("new-password"))) }
    }

    /** Current identity, email, authorization, credential fingerprint, expiry, and policy boundaries stay independent. */
    /** Verifies valid credential state with a missing role bridge fails closed. */
    @Test
    fun authorizationAndPolicyMatrixFailsClosedWithoutPasswordWrites() = runTest {
        /** Issues one approval with an explicit clock and optional role bridge for matrix controls. */
        suspend fun issued(now: () -> Long = { 1_000L }, roleBridgePresent: Boolean = true): Pair<PasswordChangeFixture, DeepLinkId> {
            val fixture = fixture(nowEpochMillis = now, roleBridgePresent = roleBridgePresent)
            assertEquals(PasswordChangeEmailRequestResult.Sent, fixture.service.requestPasswordChangeEmail(fixture.user.id, fixture.user.email!!))
            fixture.passwords.resetIssuedPasswordWriteCount()
            return fixture to fixture.linksRepo.getAll().keys.single()
        }
        /** Completes one matrix case and proves the password repository remains untouched. */
        suspend fun invalid(fixture: PasswordChangeFixture, id: DeepLinkId) {
            assertEquals(PasswordChangeResult.InvalidApproval, fixture.service.completePasswordChange(CompletePasswordChangeRequest(fixture.user.id, id, Password("new-password"))))
            assertEquals(0, fixture.passwords.issuedPasswordWriteCount)
        }
        run { var now = 1_000L; val (fixture, id) = issued({ now }); now += 15L * 60L * 1000L; invalid(fixture, id) }
        run { var now = 1_000L; val (fixture, id) = issued({ now }); now += 15L * 60L * 1000L + 1L; invalid(fixture, id) }
        run { val (fixture, id) = issued(); fixture.coordinator.updateStoredEmail(fixture.user.id, Email("changed@example.com")); invalid(fixture, id) }
        run { val (fixture, id) = issued(); fixture.coordinator.updateStoredEmail(fixture.user.id, null); invalid(fixture, id) }
        run { val (fixture, id) = issued(); fixture.coordinator.updateStoredEmail(fixture.user.id, Email("other@example.com")); fixture.coordinator.updateStoredEmail(fixture.user.id, fixture.user.email!!); invalid(fixture, id) }
        run { val (fixture, id) = issued(); fixture.roles.directRolePresent = false; invalid(fixture, id) }
        run { val (fixture, id) = issued(); fixture.passwords.unset(listOf(fixture.user.id)); invalid(fixture, id) }
        run { val (fixture, id) = issued(); fixture.users.deleteById(fixture.user.id); invalid(fixture, id) }
        run { val (fixture, id) = issued(); fixture.auth.setPassword(fixture.user.id, oldPassword); fixture.passwords.resetIssuedPasswordWriteCount(); invalid(fixture, id) }
        run { val (fixture, id) = issued(); assertEquals(PasswordChangeResult.InvalidPassword, fixture.service.completePasswordChange(CompletePasswordChangeRequest(fixture.user.id, id, Password("short")))); assertTrue(fixture.linksRepo.get(id) != null); assertEquals(PasswordChangeResult.Changed, fixture.service.completePasswordChange(CompletePasswordChangeRequest(fixture.user.id, id, Password("new-password")))) }
    }

    /** Verifies an absent Auth role bridge alone rejects an otherwise identical unexpired approval. */
    @Test
    fun missingAuthRoleBridgeRejectsAfterAuthUserReadThenSharedControlChangesPassword() = runTest {
        val nowEpochMillis = { 1_000L }
        val fixture = fixture(nowEpochMillis = nowEpochMillis)
        assertEquals(
            PasswordChangeEmailRequestResult.Sent,
            fixture.service.requestPasswordChangeEmail(fixture.user.id, fixture.user.email!!),
        )
        val approvalId = fixture.linksRepo.getAll().keys.single()
        val storedApproval = assertNotNull(fixture.linksRepo.get(approvalId))
        val payload = assertNotNull(storedApproval.value as? EmailPasswordChangePayload)
        val request = CompletePasswordChangeRequest(fixture.user.id, approvalId, Password("new-password"))
        assertEquals(EmailPasswordChange.handlerId, storedApproval.handlerId)
        assertEquals(fixture.user.id, payload.userId)
        assertEquals(fixture.user.email, payload.approvedEmail)
        assertEquals(901_000L, payload.expiresAtEpochMillis)
        assertEquals(fixture.auth.passwordChangeState(fixture.user.id), payload.credentialState)
        assertTrue(nowEpochMillis() < payload.expiresAtEpochMillis)
        assertEquals("/password-change/${fixture.user.id.long}/${approvalId.string}", fixture.service.pendingPasswordChangePath(approvalId))

        val bridgeAbsentAuth = AuthFeatureService(
            usersRepo = fixture.trackedUsers,
            writeUsersRepo = fixture.trackedUsers,
            passwordsRepo = fixture.passwords,
            userRoleAuthorization = null,
        )
        val bridgeAbsentService = EmailPasswordChangeService(
            emailsService = null,
            deepLinksService = fixture.links,
            accountCoordinator = fixture.coordinator,
            authFeatureService = bridgeAbsentAuth,
            publicHttpOrigin = "https://wishlist.example",
            nowEpochMillis = nowEpochMillis,
        )
        val userReads = mutableListOf<UserId>()
        val passwordOperations = mutableListOf<String>()
        val operationSequence = mutableListOf<String>()
        val previousLinkGetHook = fixture.linksRepo.beforeGet
        val previousLinkUnsetHook = fixture.linksRepo.beforeUnset
        val previousUserHook = fixture.trackedUsers.beforeGetById
        val previousPasswordGetHook = fixture.passwords.beforeGet
        val previousPasswordUnsetHook = fixture.passwords.beforeUnset
        val previousPasswordSetHook = fixture.passwords.beforeSet
        fixture.linksRepo.resetOperationRecords()
        fixture.passwords.resetIssuedPasswordWriteCount()
        fixture.linksRepo.beforeGet = { operationSequence += "link-read" }
        fixture.linksRepo.beforeUnset = { operationSequence += "link-unset" }
        fixture.trackedUsers.beforeGetById = {
            userReads += fixture.user.id
            operationSequence += "user-read"
        }
        fixture.passwords.beforeGet = {
            passwordOperations += "password-read"
            operationSequence += "password-read"
        }
        fixture.passwords.beforeUnset = {
            passwordOperations += "password-removal"
            operationSequence += "password-removal"
        }
        fixture.passwords.beforeSet = {
            passwordOperations += "password-write"
            operationSequence += "password-set"
        }
        try {
            assertEquals(PasswordChangeResult.InvalidApproval, bridgeAbsentService.completePasswordChange(request))
            assertEquals(listOf(approvalId), fixture.linksRepo.getIds)
            assertEquals(listOf(fixture.user.id, fixture.user.id), userReads)
            assertEquals(listOf("link-read", "user-read", "user-read"), operationSequence)
            assertTrue(passwordOperations.isEmpty())
            assertTrue(fixture.linksRepo.unsetIds.isEmpty())
            assertEquals(0, fixture.passwords.issuedPasswordWriteCount)
            assertNotNull(fixture.linksRepo.get(approvalId))

            fixture.linksRepo.resetOperationRecords()
            fixture.passwords.resetIssuedPasswordWriteCount()
            userReads.clear()
            passwordOperations.clear()
            operationSequence.clear()
            assertEquals(PasswordChangeResult.Changed, fixture.service.completePasswordChange(request))
            assertEquals(listOf(approvalId, approvalId), fixture.linksRepo.getIds)
            assertEquals(listOf(fixture.user.id, fixture.user.id), userReads)
            assertEquals(listOf("password-read", "password-write"), passwordOperations)
            assertEquals(
                listOf("link-read", "user-read", "user-read", "password-read", "link-read", "link-unset", "password-set"),
                operationSequence,
            )
            assertEquals(listOf(approvalId), fixture.linksRepo.unsetIds)
            assertEquals(1, fixture.passwords.issuedPasswordWriteCount)
            assertNull(fixture.linksRepo.get(approvalId))
            assertNull(fixture.auth.login(fixture.user.username, oldPassword))
            assertNotNull(fixture.auth.login(fixture.user.username, Password("new-password")))
        } finally {
            fixture.linksRepo.beforeGet = previousLinkGetHook
            fixture.linksRepo.beforeUnset = previousLinkUnsetHook
            fixture.trackedUsers.beforeGetById = previousUserHook
            fixture.passwords.beforeGet = previousPasswordGetHook
            fixture.passwords.beforeUnset = previousPasswordUnsetHook
            fixture.passwords.beforeSet = previousPasswordSetHook
        }
    }
}
