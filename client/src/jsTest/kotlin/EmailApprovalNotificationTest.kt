package dev.inmo.wishlist.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Verifies root-shell handling of the fixed email-approval redirect marker. */
class EmailApprovalNotificationTest {
    /** A recognized marker shows the fixed message and cleans the root URL. */
    @Test
    fun approvedMarkerShowsOneMessageAndCleansRootUrl() {
        val messages = mutableListOf<String>()
        val replacements = mutableListOf<String>()

        assertTrue(
            consumeEmailApprovalNotification(
                rawUrl = "https://host/?emailApproval=approved",
                showMessage = messages::add,
                replaceUrl = replacements::add,
            ),
        )
        assertEquals(listOf("Email has been approved."), messages)
        assertEquals(listOf("/"), replacements)
    }

    /** Cleanup removes every marker occurrence while retaining unrelated browser state. */
    @Test
    fun markerCleanupPreservesOtherQueryParametersAndFragment() {
        val messages = mutableListOf<String>()
        val replacements = mutableListOf<String>()

        assertTrue(
            consumeEmailApprovalNotification(
                rawUrl = "https://host/?keep=1&emailApproval=approved&emailApproval=approved#section",
                showMessage = messages::add,
                replaceUrl = replacements::add,
            ),
        )
        assertEquals(listOf("Email has been approved."), messages)
        assertEquals(listOf("/?keep=1#section"), replacements)
    }

    /** Missing and unrecognized values leave the URL and presentation untouched. */
    @Test
    fun unknownMarkerDoesNothing() {
        val messages = mutableListOf<String>()
        val replacements = mutableListOf<String>()

        assertFalse(
            consumeEmailApprovalNotification(
                rawUrl = "https://host/?emailApproval=arbitrary-text",
                showMessage = messages::add,
                replaceUrl = replacements::add,
            ),
        )
        assertTrue(messages.isEmpty())
        assertTrue(replacements.isEmpty())
    }
}
