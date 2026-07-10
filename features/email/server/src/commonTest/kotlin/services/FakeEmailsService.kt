package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.EmailsService
import dev.inmo.wishlist.features.email.server.models.EmailAttachment

/**
 * Call-recording [EmailsService] test double. [sendText] appends every call's arguments to
 * [sendTextCalls] before returning [result]; the other two methods only increment a call counter
 * (no test in this module currently asserts their arguments — [EmailFeatureService] only ever calls
 * [sendText]).
 *
 * @param result Value returned by every method on this instance.
 */
internal class FakeEmailsService(
    private val result: Boolean = true
) : EmailsService {

    /** One recorded [sendText] call's arguments. */
    data class SendTextCall(val recipient: Email, val subject: String, val text: String)

    /** Recorded arguments from every [sendText] call, in call order. */
    val sendTextCalls = mutableListOf<SendTextCall>()

    /** Number of times [sendTextWithAttachments] was invoked. */
    var sendTextWithAttachmentsCallCount: Int = 0
        private set

    /** Number of times [sendHtml] was invoked. */
    var sendHtmlCallCount: Int = 0
        private set

    /**
     * Records the call's arguments in [sendTextCalls] and returns [result].
     *
     * @param recipient Target email address.
     * @param subject Subject header of the message.
     * @param text Plain-text body.
     * @return [result].
     */
    override suspend fun sendText(recipient: Email, subject: String, text: String): Boolean {
        sendTextCalls += SendTextCall(recipient, subject, text)
        return result
    }

    /**
     * Increments [sendTextWithAttachmentsCallCount] and returns [result].
     *
     * @param recipient Target email address.
     * @param subject Subject header of the message.
     * @param text Plain-text body part.
     * @param attachments Attachments (contents not recorded).
     * @return [result].
     */
    override suspend fun sendTextWithAttachments(
        recipient: Email,
        subject: String,
        text: String,
        attachments: List<EmailAttachment>
    ): Boolean {
        sendTextWithAttachmentsCallCount++
        return result
    }

    /**
     * Increments [sendHtmlCallCount] and returns [result].
     *
     * @param recipient Target email address.
     * @param subject Subject header of the message.
     * @param html HTML markup body.
     * @return [result].
     */
    override suspend fun sendHtml(recipient: Email, subject: String, html: String): Boolean {
        sendHtmlCallCount++
        return result
    }
}
