package dev.inmo.wishlist.features.auth.common.models

import dev.inmo.wishlist.features.auth.common.utils.isAcceptablePasswordChangePassword
import dev.inmo.wishlist.features.auth.common.utils.isCanonicalPasswordChangeApprovalId
import dev.inmo.wishlist.features.auth.common.utils.passwordChangePendingPath
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Verifies password-change wire data is secret-safe and its shared route policy is exact. */
class PasswordChangeContractTest {
    /** The shared password policy counts BCrypt's byte boundary instead of silently truncating UTF-8. */
    /** Verifies character minimum and UTF-8 byte maximum password policy boundaries. */
    @Test
    fun passwordPolicyUsesCharacterMinimumAndUtf8Maximum() {
        assertFalse(isAcceptablePasswordChangePassword(Password("short")))
        assertTrue(isAcceptablePasswordChangePassword(Password("a".repeat(72))))
        assertTrue(isAcceptablePasswordChangePassword(Password("a".repeat(69) + "€")))
        assertFalse(isAcceptablePasswordChangePassword(Password("a".repeat(70) + "€")))
    }

    /** Only the UUID spelling produced by the existing UUID-v4 deeplink minting path can enter routes. */
    /** Verifies only canonical UUID segments produce a pending password route. */
    @Test
    fun pendingPathRejectsNonCanonicalOrUnsafeApprovalIds() {
        val approvalId = DeepLinkId("123e4567-e89b-42d3-a456-426614174000")

        assertTrue(isCanonicalPasswordChangeApprovalId(approvalId))
        assertEquals("/password-change/7/${approvalId.string}", passwordChangePendingPath(UserId(7L), approvalId))
        assertFalse(isCanonicalPasswordChangeApprovalId(DeepLinkId(approvalId.string.uppercase())))
        assertFalse(isCanonicalPasswordChangeApprovalId(DeepLinkId("123e4567-e89b-42d3-a456-426614174000/extra")))
        assertNull(passwordChangePendingPath(UserId(0L), approvalId))
    }

    /** JSON preserves the supplied approval exactly while incidental string output excludes secrets. */
    /** Verifies wire values survive serialization while diagnostics redact secrets. */
    @Test
    fun completionContractRetainsWireValuesButRedactsToString() {
        val request = CompletePasswordChangeRequest(
            userId = UserId(7L),
            approvalId = DeepLinkId("123e4567-e89b-42d3-a456-426614174000"),
            password = Password("new-password"),
        )

        val encoded = Json.encodeToString(request)

        assertEquals(
            "{\"userId\":7,\"approvalId\":\"123e4567-e89b-42d3-a456-426614174000\",\"password\":\"new-password\"}",
            encoded,
        )
        assertFalse(request.toString().contains(request.approvalId.string))
        assertFalse(request.toString().contains(request.password.string))
    }
}
