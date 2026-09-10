package dev.inmo.wishlist.client

import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.ui.users.ui.PasswordChangeViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Verifies the browser shell protects password-approval URLs before loading external resources. */
class PasswordChangeHtmlPolicyTest {
    /** Reads the production JS shell through the narrow test-only Gradle property. */
    private fun productionHtml(): String {
        val path = requireNotNull(System.getProperty("passwordChangeHtmlPolicyPath")) {
            "passwordChangeHtmlPolicyPath must point at the production index.html"
        }
        return Files.readString(Paths.get(path))
    }

    /** Ensures the no-referrer declaration occurs before every script and link resource element. */
    /** Verifies the no-referrer meta tag precedes every external HTML resource. */
    @Test
    fun noReferrerMetaPrecedesEveryScriptAndLinkResource() {
        val html = productionHtml()
        val meta = assertNotNull(
            Regex(
                """(?is)<meta\b(?=[^>]*\bname\s*=\s*["']referrer["'])(?=[^>]*\bcontent\s*=\s*["']no-referrer["'])[^>]*>""",
            ).find(html),
        )
        val resourceStarts = Regex("""(?is)<(?:script|link)\b[^>]*(?:src|href)\s*=""")
            .findAll(html)
            .map { it.range.first }
            .toList()
        assertTrue(resourceStarts.isNotEmpty())
        resourceStarts.forEach { resourceStart ->
            assertTrue(meta.range.first < resourceStart)
        }
    }

    /** Ensures password-change navigation diagnostics never expose an actionable approval UUID. */
    /** Verifies pending route diagnostics redact the approval identifier. */
    @Test
    fun pendingPasswordChangeConfigRedactsApprovalIdentifier() {
        val approval = "approval-uuid-sentinel"
        val text = PasswordChangeViewConfig.Pending(UserId(7L), DeepLinkId(approval)).toString()
        assertFalse(text.contains(approval))
        assertTrue(text.contains("<redacted>"))
    }
}
