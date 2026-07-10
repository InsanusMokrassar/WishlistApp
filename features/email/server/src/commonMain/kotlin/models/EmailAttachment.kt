package dev.inmo.wishlist.features.email.server.models

import java.io.InputStream

/**
 * Attachment descriptor for [dev.inmo.wishlist.features.email.server.EmailsService.sendTextWithAttachments].
 *
 * Streaming contract: [content] MAY be invoked more than once (Jakarta Mail's
 * `jakarta.activation.DataSource` contract) and MUST return a NEW [InputStream] positioned at
 * the beginning of the full content on EVERY invocation. Content is streamed to the SMTP
 * connection while the message is transmitted — it is never materialized as a whole
 * `ByteArray`. The mail runtime is responsible for closing each returned stream.
 *
 * Deliberately a plain class: not a `data class` (structural equality over a lambda is
 * meaningless and `copy` invites provider misuse) and not `@Serializable` (server-only type
 * carrying a non-serializable suspend lambda).
 *
 * @property fileName File name presented in the attachment's `Content-Disposition` header.
 * @property mimeType MIME type of the content; defaults to `application/octet-stream`.
 * @property content Suspend provider returning a fresh [InputStream] over the full content on
 *   each invocation.
 */
class EmailAttachment(
    val fileName: String,
    val mimeType: String = "application/octet-stream",
    val content: suspend () -> InputStream
)
