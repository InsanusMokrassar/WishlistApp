package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.auth.server.RegistrationEmailSender
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService
import dev.inmo.wishlist.features.email.server.EmailsService
import dev.inmo.wishlist.features.email.server.models.EmailVerification
import dev.inmo.wishlist.features.email.server.models.EmailVerificationPayload
import dev.inmo.wishlist.features.users.common.models.RegisteredUser

/**
 * Creates an email-verification deeplink and delivers its absolute URL through SMTP.
 *
 * Missing SMTP or deeplink infrastructure is represented by `false`, allowing required-email
 * registration to fail closed without breaking installations that do not load those features.
 *
 * @param emailsService Optional SMTP transport.
 * @param deepLinksService Optional deeplink minting service.
 * @param scheme Public URL scheme, normally `http` or `https`.
 * @param publicHost Public hostname advertised to invite recipients.
 * @param port Public server port.
 */
class EmailRegistrationInviteSender(
    private val emailsService: EmailsService?,
    private val deepLinksService: DeepLinksService?,
    private val scheme: String,
    private val publicHost: String,
    private val port: Int,
) : RegistrationEmailSender {
    /** Subject used for new-account verification invites. */
    private val subject = "Verify your WishlistApp account"

    /**
     * Mints the verification deeplink and sends a plain-text invitation.
     *
     * @param user Newly persisted account.
     * @return `true` only when a recipient, deeplink service, and SMTP transport all succeed.
     */
    override suspend fun sendRegistrationEmail(user: RegisteredUser): Boolean {
        val recipient = user.email ?: return false
        val links = deepLinksService ?: return false
        val emails = emailsService ?: return false
        val deeplinkId = links.createDeepLink(
            EmailVerification.handlerId,
            EmailVerificationPayload(user.id)
        )
        val url = buildEmailVerificationUrl(scheme, publicHost, port, deeplinkId)
        return emails.sendText(
            recipient = recipient,
            subject = subject,
            text = "Open this link to verify your WishlistApp account:\n$url"
        )
    }
}

/**
 * Builds the absolute HTTP URL used in a registration invite.
 *
 * @param scheme URL scheme without `://`.
 * @param publicHost Public hostname without a path.
 * @param port Public server port.
 * @param deeplinkId Persisted deeplink identifier.
 * @return Absolute URL routed through `/api/links/{id}`.
 */
internal fun buildEmailVerificationUrl(
    scheme: String,
    publicHost: String,
    port: Int,
    deeplinkId: DeepLinkId,
): String = "${scheme.trimEnd(':')}://${publicHost.trimEnd('/')}:$port/api/links/${deeplinkId.string}"
