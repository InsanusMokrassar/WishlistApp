package dev.inmo.wishlist.features.email.server.services

import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerInfo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.common.repo.DeepLinksRepo
import dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.EmailConfig
import dev.inmo.wishlist.features.email.server.EmailsService
import dev.inmo.wishlist.features.email.server.SmtpConfig
import dev.inmo.wishlist.features.email.server.models.EmailAttachment
import dev.inmo.wishlist.features.email.server.models.EmailVerification
import dev.inmo.wishlist.features.email.server.models.EmailVerificationPayload
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Verifies invite URL creation, message delivery, and disabled-dependency behavior. */
class EmailRegistrationInviteSenderTest {
    /** In-memory deeplink store for sender tests. */
    private class FakeDeepLinksRepo : DeepLinksRepo,
        dev.inmo.micro_utils.repos.KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo> by MapKeyValueRepo()

    /** SMTP test double that throws while sending the verification message. */
    private object ThrowingEmailsService : EmailsService {
        /** Throws to exercise invite cleanup after an SMTP exception. */
        override suspend fun sendText(recipient: Email, subject: String, text: String): Boolean =
            error("SMTP failure")

        /** Throws because the operation is not used by the invite sender. */
        override suspend fun sendTextWithAttachments(
            recipient: Email,
            subject: String,
            text: String,
            attachments: List<EmailAttachment>,
        ): Boolean = error("SMTP failure")

        /** Throws because the operation is not used by the invite sender. */
        override suspend fun sendHtml(recipient: Email, subject: String, html: String): Boolean =
            error("SMTP failure")
    }

    /** Account fixture with a valid address for invite tests. */
    private val user = RegisteredUser(UserId(7L), Username("alice"), Email("alice@example.com"))

    /** The URL builder preserves a configured external port and appends the API path. */
    @Test
    fun urlBuilderUsesConfiguredAddress() {
        assertEquals(
            "https://wishlist.example:9443/api/links/link-7",
            buildEmailVerificationUrl("https://wishlist.example:9443", DeepLinkId("link-7"))
        )
    }

    /** Default HTTPS and development origins do not gain an internal or implicit port. */
    @Test
    fun urlBuilderUsesOnlyTheConfiguredPublicOrigin() {
        assertEquals(
            "https://wishlist.example/api/links/link-7",
            buildEmailVerificationUrl("https://wishlist.example/", DeepLinkId("link-7")),
        )
        assertEquals(
            "http://127.0.0.1:8196/api/links/link-7",
            buildEmailVerificationUrl("http://127.0.0.1:8196", DeepLinkId("link-7")),
        )
    }

    /** Invalid schemes, authority parts, and non-origin URL components fail at construction. */
    @Test
    fun senderRejectsInvalidPublicOrigins() {
        val invalidOrigins = listOf(
            "",
            "ftp://wishlist.example",
            "https:///missing-host",
            "https://user:password@wishlist.example",
            "https://wishlist.example/path",
            "https://wishlist.example?query=true",
            "https://wishlist.example#fragment",
        )

        invalidOrigins.forEach { origin ->
            assertFailsWith<IllegalArgumentException>(origin) {
                EmailRegistrationInviteSender(null, null, origin)
            }
        }
    }

    /** A configured sender creates the expected payload and sends a labeled HTML invite. */
    @Test
    fun senderCreatesDeepLinkAndSendsInvite() = runTest {
        val repo = FakeDeepLinksRepo()
        val links = DeepLinksService(repo, emptyList())
        val emails = FakeEmailsService()
        val sender = EmailRegistrationInviteSender(emails, links, "http://localhost:8196")

        assertTrue(sender.sendRegistrationEmail(user))

        val call = emails.sendHtmlCalls.single()
        assertEquals(user.email, call.recipient)
        val stored = repo.getAll().values.single()
        val url = buildEmailVerificationUrl("http://localhost:8196", repo.getAll().keys.single())
        assertEquals("Verify your WishlistApp email address", call.subject)
        assertEquals(
            "<p>Verify ownership of this email address for your WishlistApp account by <a href=\"$url\">Verify email address</a>.</p>",
            call.html,
        )
        assertTrue(emails.sendTextCalls.isEmpty())
        assertEquals(EmailVerification.handlerId, stored.handlerId)
        assertEquals("email.registration_verification", stored.handlerId.string)
        assertEquals(EmailVerificationPayload(user.id, user.email), stored.value)
    }

    /** A compensable delivery retains its link until the returned request-local handle rolls it back. */
    @Test
    fun compensableDeliveryRetainsAndRollsBackItsExactDeepLink() = runTest {
        val repo = FakeDeepLinksRepo()
        val sender = EmailRegistrationInviteSender(
            emailsService = FakeEmailsService(),
            deepLinksService = DeepLinksService(repo, emptyList()),
            publicHttpOrigin = "http://localhost:8196",
        )

        val handle = checkNotNull(sender.sendRegistrationEmailWithCompensation(user))

        assertEquals(1, repo.getAll().size)
        handle.rollback()
        assertTrue(repo.getAll().isEmpty())
    }

    /** Two successful deliveries receive independent handles that never overwrite one another's links. */
    @Test
    fun compensableDeliveryHandlesOwnOnlyTheirExactDeepLinks() = runTest {
        val repo = FakeDeepLinksRepo()
        val sender = EmailRegistrationInviteSender(
            emailsService = FakeEmailsService(),
            deepLinksService = DeepLinksService(repo, emptyList()),
            publicHttpOrigin = "http://localhost:8196",
        )
        val secondUser = RegisteredUser(UserId(8L), Username("bob"), Email("bob@example.com"))

        val firstHandle = checkNotNull(sender.sendRegistrationEmailWithCompensation(user))
        val secondHandle = checkNotNull(sender.sendRegistrationEmailWithCompensation(secondUser))

        firstHandle.rollback()

        assertEquals(1, repo.getAll().size)
        assertEquals(EmailVerificationPayload(secondUser.id, secondUser.email), repo.getAll().values.single().value)
        secondHandle.rollback()
        assertTrue(repo.getAll().isEmpty())
    }

    /** A false SMTP result removes the deeplink minted for the failed invite. */
    @Test
    fun senderRemovesDeepLinkWhenEmailDeliveryFails() = runTest {
        val repo = FakeDeepLinksRepo()
        val links = DeepLinksService(repo, emptyList())
        val sender = EmailRegistrationInviteSender(
            emailsService = FakeEmailsService(result = false),
            deepLinksService = links,
            publicHttpOrigin = "http://localhost:8196",
        )

        assertFalse(sender.sendRegistrationEmail(user))
        assertTrue(repo.getAll().isEmpty())
    }

    /** An SMTP exception removes the deeplink minted before the failed send. */
    @Test
    fun senderRemovesDeepLinkWhenEmailDeliveryThrows() = runTest {
        val repo = FakeDeepLinksRepo()
        val links = DeepLinksService(repo, emptyList())
        val sender = EmailRegistrationInviteSender(
            emailsService = ThrowingEmailsService,
            deepLinksService = links,
            publicHttpOrigin = "http://localhost:8196",
        )

        assertFalse(sender.sendRegistrationEmail(user))
        assertTrue(repo.getAll().isEmpty())
    }

    /** Missing SMTP or deeplink infrastructure fails without attempting a delivery. */
    @Test
    fun senderReturnsFalseWhenDependenciesAreMissing() = runTest {
        val sender = EmailRegistrationInviteSender(null, null, "http://localhost:8196")

        assertFalse(sender.sendRegistrationEmail(user))
    }

    /** Production SMTP cancellation propagates only after the already-minted deeplink is removed. */
    @Test
    fun senderCancellationRemovesMintedDeepLinkAndPropagates() = runTest {
        val repo = FakeDeepLinksRepo()
        val links = DeepLinksService(repo, emptyList())
        val transportStarted = CompletableDeferred<Unit>()
        val transportResult = CompletableDeferred<Unit>()
        val emails = SmtpEmailService.withTransport(
            config = EmailConfig(
                smtp = SmtpConfig(
                    host = "smtp.example.com",
                    from = Email("noreply@example.com"),
                ),
            ),
            transportSend = {
                transportStarted.complete(Unit)
                transportResult.await()
            },
        )
        val sender = EmailRegistrationInviteSender(emails, links, "http://localhost:8196")
        val delivery = async { sender.sendRegistrationEmail(user) }
        transportStarted.await()
        assertEquals(1, repo.getAll().size)

        delivery.cancel()
        assertFailsWith<CancellationException> { delivery.await() }

        assertTrue(repo.getAll().isEmpty())
    }
}
