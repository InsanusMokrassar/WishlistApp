package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.serialization.Serializable

/** Credential-free navigation states for the email-authorized password-change screen. */
@Serializable
sealed interface PasswordChangeViewConfig : ViewConfig {
    /** Pending screen carrying immutable approval identity but never any entered password. */
    @Serializable
    data class Pending(
        /** Approval-bound account identity. */
        val userId: UserId,
        /** Exact approval UUID from the opened deeplink. */
        val approvalId: DeepLinkId,
    ) : PasswordChangeViewConfig {
        /** Redacts the actionable UUID from navigation and incidental log output. */
        override fun toString(): String = "PasswordChangeViewConfig.Pending(userId=$userId, approvalId=<redacted>)"
    }

    /** Completed screen after the actionable approval has been removed from navigation. */
    @Serializable
    data object Completed : PasswordChangeViewConfig
}
