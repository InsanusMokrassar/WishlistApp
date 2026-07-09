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
 * Verifies [SmtpEmailService]'s disabled/no-op mode: no SMTP server is configured (or the
 * configured host is blank), so every delivery method must short-circuit to `false` without
 * attempting a connection or touching attachment content.
 */
class SmtpEmailServiceDisabledTest {

    /** Shared recipient address used by every disabled-mode assertion. */
    private val recipient = Email("recipient@example.com")

    /** `sendText` must return `false` when [EmailConfig.smtp] is `null`. */
    @Test
    fun sendTextReturnsFalseWhenSmtpIsNull() = runTest {
        val service = SmtpEmailService(EmailConfig(smtp = null))
        assertFalse(service.sendText(recipient, "subject", "body"))
    }

    /** `sendHtml` must return `false` when [EmailConfig.smtp] is `null`. */
    @Test
    fun sendHtmlReturnsFalseWhenSmtpIsNull() = runTest {
        val service = SmtpEmailService(EmailConfig(smtp = null))
        assertFalse(service.sendHtml(recipient, "subject", "<p>body</p>"))
    }

    /**
     * `sendTextWithAttachments` must return `false` when [EmailConfig.smtp] is `null` AND must
     * never invoke an attachment's content provider — disabled mode must not touch attachment
     * content at all.
     */
    @Test
    fun sendTextWithAttachmentsReturnsFalseWhenSmtpIsNullAndDoesNotInvokeProvider() = runTest {
        val service = SmtpEmailService(EmailConfig(smtp = null))
        var invocations = 0
        val attachment = EmailAttachment("file.txt", "text/plain") {
            invocations++
            ByteArrayInputStream(ByteArray(0))
        }

        val result = service.sendTextWithAttachments(recipient, "subject", "body", listOf(attachment))

        assertFalse(result)
        assertEquals(0, invocations)
    }

    /** `sendText` must return `false` when the configured SMTP host is blank. */
    @Test
    fun sendTextReturnsFalseWhenHostIsBlank() = runTest {
        val service = SmtpEmailService(
            EmailConfig(smtp = SmtpConfig(host = "", from = Email("noreply@example.com")))
        )
        assertFalse(service.sendText(recipient, "subject", "body"))
    }

    /** `isFeatureEnabled` must return `false` when [EmailConfig.smtp] is `null`. */
    @Test
    fun isFeatureEnabledFalseWhenSmtpIsNull() = runTest {
        val service = SmtpEmailService(EmailConfig(smtp = null))
        assertFalse(service.isFeatureEnabled())
    }
}
