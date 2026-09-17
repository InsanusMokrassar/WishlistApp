package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.auth.server.CompensableRegistrationEmailSender
import dev.inmo.wishlist.features.auth.server.RegistrationEmailDeliveryHandle
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService
import dev.inmo.wishlist.features.email.server.EmailsService
import dev.inmo.wishlist.features.email.server.models.EmailVerification
import dev.inmo.wishlist.features.email.server.models.EmailVerificationPayload
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.net.URI

/**
 * Creates an email-verification deeplink and delivers its absolute URL through SMTP.
 *
 * Missing SMTP or deeplink infrastructure is represented by `false`, allowing required-email
 * registration to fail closed without breaking installations that do not load those features.
 *
 * @param emailsService Optional SMTP transport.
 * @param deepLinksService Optional deeplink minting service.
 * @param publicHttpOrigin Externally reachable absolute HTTP origin.
 */
class EmailRegistrationInviteSender(
    private val emailsService: EmailsService?,
    private val deepLinksService: DeepLinksService?,
    publicHttpOrigin: String,
) : CompensableRegistrationEmailSender {
    /** Subject used for new-account verification invites. */
    private val subject = "Verify your WishlistApp email address"

    /** Validated origin without a trailing slash, path, query, fragment, or credentials. */
    private val publicHttpOrigin = normalizePublicHttpOrigin(publicHttpOrigin)

    /**
     * Mints the verification deeplink and sends an HTML invitation with a labeled anchor.
     *
     * @param user Newly persisted account.
     * @return `true` only when a recipient, deeplink service, and SMTP transport all succeed.
     */
    override suspend fun sendRegistrationEmail(user: RegisteredUser): Boolean =
        sendRegistrationEmailWithCompensation(user) != null

    /**
     * Mints the verification deeplink and returns rollback ownership only after SMTP accepts the
     * matching invite.
     *
     * @param user Newly persisted account.
     * @return A handle for the exact delivered deeplink, or `null` when delivery cannot complete.
     */
    override suspend fun sendRegistrationEmailWithCompensation(
        user: RegisteredUser,
    ): RegistrationEmailDeliveryHandle? {
        val recipient = user.email ?: return null
        val links = deepLinksService ?: return null
        val emails = emailsService ?: return null
        val deeplinkId = links.createDeepLink(
            EmailVerification.handlerId,
            EmailVerificationPayload(user.id, recipient)
        )
        val url = buildEmailVerificationUrl(publicHttpOrigin, deeplinkId)
        val delivered = try {
            emails.sendHtml(
                recipient = recipient,
                subject = subject,
                html = "<p>Verify ownership of this email address for your WishlistApp account by <a href=\"$url\">Verify email address</a>.</p>"
            )
        } catch (error: CancellationException) {
            try {
                withContext(NonCancellable) {
                    links.removeDeepLink(deeplinkId)
                }
            } catch (cleanupError: Throwable) {
                error.addSuppressed(cleanupError)
            }
            throw error
        } catch (_: Exception) {
            false
        }
        if (delivered) return RegistrationEmailDeliveryHandle {
            links.removeDeepLink(deeplinkId)
        }
        links.removeDeepLink(deeplinkId)
        return null
    }
}

/**
 * Builds the absolute HTTP URL used in a registration invite.
 *
 * @param publicHttpOrigin Externally reachable absolute HTTP origin.
 * @param deeplinkId Persisted deeplink identifier.
 * @return Absolute URL routed through `/api/links/{id}`.
 */
internal fun buildEmailVerificationUrl(
    publicHttpOrigin: String,
    deeplinkId: DeepLinkId,
): String = "${normalizePublicHttpOrigin(publicHttpOrigin)}/api/links/${deeplinkId.string}"

/**
 * Validates and normalizes the public origin used in verification messages.
 *
 * @param publicHttpOrigin Candidate absolute origin.
 * @return Origin normalized without a trailing slash.
 * @throws IllegalArgumentException When the value is not a plain absolute HTTP(S) origin.
 */
internal fun normalizePublicHttpOrigin(publicHttpOrigin: String): String {
    require(publicHttpOrigin.isNotBlank()) { "publicHttpOrigin must not be blank" }
    val uri = try {
        URI(publicHttpOrigin)
    } catch (error: Exception) {
        throw IllegalArgumentException("publicHttpOrigin must be a valid absolute URI", error)
    }
    val scheme = uri.scheme?.lowercase()
    require(scheme == "http" || scheme == "https") {
        "publicHttpOrigin must use http or https"
    }
    require(!uri.host.isNullOrBlank()) { "publicHttpOrigin must contain a host" }
    require(uri.rawUserInfo == null) { "publicHttpOrigin must not contain credentials" }
    require(uri.rawQuery == null) { "publicHttpOrigin must not contain a query" }
    require(uri.rawFragment == null) { "publicHttpOrigin must not contain a fragment" }
    require(uri.rawPath.isNullOrEmpty() || uri.rawPath == "/") {
        "publicHttpOrigin must not contain a path"
    }
    return "$scheme://${uri.rawAuthority}"
}
