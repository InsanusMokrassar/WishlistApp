package dev.inmo.wishlist.features.email.server.services

import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerInfo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.common.repo.DeepLinksRepo
import dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.models.EmailVerification
import dev.inmo.wishlist.features.email.server.models.EmailVerificationPayload
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Verifies invite URL creation, message delivery, and disabled-dependency behavior. */
class EmailRegistrationInviteSenderTest {
    /** In-memory deeplink store for sender tests. */
    private class FakeDeepLinksRepo : DeepLinksRepo,
        dev.inmo.micro_utils.repos.KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo> by MapKeyValueRepo()

    private val user = RegisteredUser(UserId(7L), Username("alice"), Email("alice@example.com"))

    /** The URL builder uses scheme, host, port, API prefix, and the persisted id. */
    @Test
    fun urlBuilderUsesConfiguredAddress() {
        assertEquals(
            "https://wishlist.example:9443/api/links/link-7",
            buildEmailVerificationUrl("https:", "wishlist.example/", 9443, DeepLinkId("link-7"))
        )
    }

    /** A configured sender creates the expected payload and sends its absolute URL. */
    @Test
    fun senderCreatesDeepLinkAndSendsInvite() = runTest {
        val repo = FakeDeepLinksRepo()
        val links = DeepLinksService(repo, emptyList())
        val emails = FakeEmailsService()
        val sender = EmailRegistrationInviteSender(emails, links, "http", "localhost", 8196)

        assertTrue(sender.sendRegistrationEmail(user))

        val call = emails.sendTextCalls.single()
        assertEquals(user.email, call.recipient)
        assertTrue(call.text.contains("http://localhost:8196/api/links/"))
        val stored = repo.getAll().values.single()
        assertEquals(EmailVerification.handlerId, stored.handlerId)
        assertEquals(EmailVerificationPayload(user.id), stored.value)
    }

    /** Missing SMTP or deeplink infrastructure fails without attempting a delivery. */
    @Test
    fun senderReturnsFalseWhenDependenciesAreMissing() = runTest {
        val sender = EmailRegistrationInviteSender(null, null, "http", "localhost", 8196)

        assertFalse(sender.sendRegistrationEmail(user))
    }
}
