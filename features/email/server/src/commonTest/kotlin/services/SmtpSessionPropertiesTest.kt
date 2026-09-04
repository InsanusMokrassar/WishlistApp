package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.SmtpConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Verifies that SMTP trust override properties require an explicit unsafe configuration opt-in. */
class SmtpSessionPropertiesTest {
    /** Default SMTP settings retain normal certificate-chain validation. */
    @Test
    fun trustOverrideIsAbsentByDefault() {
        val properties = buildSmtpSessionProperties(
            SmtpConfig(host = "smtp.example.com", from = Email("noreply@example.com")),
        )

        assertFalse(properties.containsKey("mail.smtp.ssl.trust"))
        assertEquals("smtp.example.com", properties.getProperty("mail.smtp.host"))
        assertEquals("587", properties.getProperty("mail.smtp.port"))
        assertEquals("true", properties.getProperty("mail.smtp.starttls.enable"))
        assertFalse(properties.containsKey("mail.smtp.ssl.enable"))
    }

    /** Explicit unsafe configuration trusts only the configured SMTP host. */
    @Test
    fun trustOverrideContainsOnlyConfiguredHostWhenExplicitlyUnsafe() {
        val properties = buildSmtpSessionProperties(
            SmtpConfig(
                host = "smtp.example.com",
                from = Email("noreply@example.com"),
                useTls = false,
                useSsl = true,
                unsafeSsl = true,
            ),
        )

        assertEquals("smtp.example.com", properties.getProperty("mail.smtp.ssl.trust"))
        assertFalse(properties.containsKey("mail.smtp.starttls.enable"))
        assertEquals("true", properties.getProperty("mail.smtp.ssl.enable"))
    }
}
