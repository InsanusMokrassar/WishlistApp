package dev.inmo.wishlist.client

import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.ui.users.ui.PasswordChangeViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Verifies credential-free password-change URL serialization at the browser navigation seam. */
class PasswordChangeNavigationTest {
    /** Verifies canonical approval segments round-trip through the browser route parser. */
    @Test
    fun pendingApprovalRoundTripsThroughExactSegments() {
        val config = PasswordChangeViewConfig.Pending(
            userId = UserId(7),
            approvalId = DeepLinkId("6d80c6bc-9046-4e96-a33f-2aef0cff14e9"),
        )

        val segments = passwordChangeRouteSegments(config)

        assertEquals(listOf("password-change", "7", "6d80c6bc-9046-4e96-a33f-2aef0cff14e9"), segments)
        assertEquals(config, parsePasswordChangePendingSegments(segments!!.drop(1)))
        assertEquals(
            listOf("password-changed"),
            passwordChangeRouteSegments(PasswordChangeViewConfig.Completed),
        )
    }

    /** Verifies malformed approval segments are rejected instead of normalized. */
    @Test
    fun malformedApprovalSegmentsAreRejected() {
        assertNull(
            passwordChangeRouteSegments(
                PasswordChangeViewConfig.Pending(UserId(0), DeepLinkId("6d80c6bc-9046-4e96-a33f-2aef0cff14e9")),
            ),
        )
        assertNull(parsePasswordChangePendingSegments(listOf("7", "6D80C6BC-9046-4E96-A33F-2AEF0CFF14E9")))
        assertNull(parsePasswordChangePendingSegments(listOf("7", "6d80c6bc-9046-4e96-a33f-2aef0cff14e9", "extra")))
    }
}
