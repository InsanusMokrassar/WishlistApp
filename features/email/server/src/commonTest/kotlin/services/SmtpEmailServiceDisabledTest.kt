package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.EmailConfig
import dev.inmo.wishlist.features.email.server.SmtpConfig
import dev.inmo.wishlist.features.email.server.models.EmailAttachment
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Verifies [SmtpEmailService]'s remaining no-op trigger: a configured-but-blank SMTP host.
 * [EmailConfig.smtp] is non-nullable now, so "no SMTP configured at all" is a DI-graph-shape fact
 * handled by [DisabledEmailFeature] (see `DisabledEmailFeatureTest`), not something this class can
 * represent — every case here constructs [SmtpEmailService] with a real, non-null [EmailConfig]
 * whose host happens to be blank.
 */
class SmtpEmailServiceDisabledTest {

    /** Shared recipient address used by every blank-host assertion. */
    private val recipient = Email("recipient@example.com")

    /** Builds an [EmailConfig] with a blank SMTP host — the only remaining no-op trigger. */
    private fun blankHostConfig() =
        EmailConfig(smtp = SmtpConfig(host = "", from = Email("noreply@example.com")))

    /** `sendText` must return `false` when the configured SMTP host is blank. */
    @Test
    fun sendTextReturnsFalseWhenHostIsBlank() = runTest {
        val service = SmtpEmailService(blankHostConfig())
        assertFalse(service.sendText(recipient, "subject", "body"))
    }

    /** `sendHtml` must return `false` when the configured SMTP host is blank. */
    @Test
    fun sendHtmlReturnsFalseWhenHostIsBlank() = runTest {
        val service = SmtpEmailService(blankHostConfig())
        assertFalse(service.sendHtml(recipient, "subject", "<p>body</p>"))
    }

    /**
     * `sendTextWithAttachments` must return `false` when the configured SMTP host is blank AND must
     * never invoke an attachment's content provider — the no-op path must not touch attachment
     * content at all.
     */
    @Test
    fun sendTextWithAttachmentsReturnsFalseWhenHostIsBlankAndDoesNotInvokeProvider() = runTest {
        val service = SmtpEmailService(blankHostConfig())
        var invocations = 0
        val attachment = EmailAttachment("file.txt", "text/plain") {
            invocations++
            ByteArrayInputStream(ByteArray(0))
        }

        val result = service.sendTextWithAttachments(recipient, "subject", "body", listOf(attachment))

        assertFalse(result)
        assertEquals(0, invocations)
    }
}
