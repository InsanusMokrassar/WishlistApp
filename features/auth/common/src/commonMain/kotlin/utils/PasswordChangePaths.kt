package dev.inmo.wishlist.features.auth.common.utils

import dev.inmo.wishlist.features.auth.common.Constants
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.users.common.models.UserId

/** Canonical lower-case UUID-v4 pattern used only by password-change approval routes. */
private val passwordChangeApprovalIdPattern = Regex(
    "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
)

/**
 * Verifies that [approvalId] is a lower-case canonical UUID-v4 safe for the password route.
 *
 * @param approvalId Existing deeplink id supplied by an opened approval URL.
 * @return `true` only for the canonical UUID form minted by the password approval service.
 */
fun isCanonicalPasswordChangeApprovalId(approvalId: DeepLinkId): Boolean =
    passwordChangeApprovalIdPattern.matches(approvalId.string)

/**
 * Builds the fixed client route for a validated pending password change.
 *
 * @param userId Persisted approval subject.
 * @param approvalId Exact persisted approval UUID.
 * @return Fixed relative route, or `null` when either input is not route-safe.
 */
fun passwordChangePendingPath(userId: UserId, approvalId: DeepLinkId): String? {
    if (userId.long <= 0L || !isCanonicalPasswordChangeApprovalId(approvalId)) return null
    return "/${Constants.passwordChangePagePathPart}/${userId.long}/${approvalId.string}"
}
