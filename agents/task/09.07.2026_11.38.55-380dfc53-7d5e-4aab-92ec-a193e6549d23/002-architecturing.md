Model: fable
Execution time: ~420
Tokens used: ~75000 (estimated from context size; exact counter not exposed to the agent)
Changed files: agents/task/09.07.2026_11.38.55-380dfc53-7d5e-4aab-92ec-a193e6549d23/002-architecturing.md

# 002 — Architecturing

## Model choice argument

`agents/SHORTCUTS.md` sets the Architecture role model priority as `fable / opus / sonnet`. This agent runs on `fable` (claude-fable-5), the highest-priority option, so the best available model per priority is used. No fallback needed.

## Input

`001-planning.md` (read in full) + `PROMPT.md` operator clarifications. Planning's investigation was re-verified against the current sources: `SmtpEmailService.kt`, `Plugin.kt`, server `EmailFeature.kt`, client `EmailFeature.kt`, `EmailFeatureService.kt`, `EmailConfig.kt`, `Email.kt`, `features/email/server/build.gradle`, `features/email/README.md`, and the gradle templates (`mppJavaProject`, `defaultProject`, `enableMPPJvm`). `ast-index symbol` confirms `EmailsService`, `EmailAttachment`, and `EmailAttachmentDataSource` are unused names repo-wide.

## Discretionary decisions (delegated by Planning)

- **(a) Koin binding `single<EmailsService> { get<SmtpEmailService>() }` in `Plugin.kt` — YES.** This is the repository's standard interface-binding pattern (`agents/CODING.md`, "Fill Plugin.kt": `single<MyFeature> { get<MyFeatureService>() }`); the whole point of introducing `EmailsService` is that server-side consumers inject the interface rather than the concrete transport. One server-only DI line; adds no route, client, or DTO surface, so it stays inside the operator-approved scope.
- **(b) Refactor `sendTestEmail` onto the shared `send(...)` helper — YES.** Four send paths share an identical skeleton (disabled-check + warn-log, `buildSession`, envelope, `withContext(Dispatchers.IO)`, `Transport.send`, `runCatching`/warn-log). Routing `sendTestEmail` through the helper changes no public signature and no functional behavior. Two log/encoding deltas are accepted intentionally and documented here: the failure log message becomes `"sendTestEmail failed to send email to <addr>"` (was `"Failed to send test email to <addr>"`), and the subject is set via `setSubject(subject, "UTF-8")` (identical wire output for the pure-ASCII test subject). Log text is not a contract.
- **Bridge placement/visibility (delegated detail):** top-level class in `SmtpEmailService.kt`, but `internal` rather than `private` — named `EmailAttachmentDataSource`. Rationale: `internal` is not public API (module-scoped), and the module's default test compilation (`jvmTest` associates with `jvmMain`, which compiles `commonMain` sources) sees internals, giving a direct unit-test seam for the bridge without any API widening. This satisfies the test-seam requirement without indirection.

## A. Final architecture spec

Target module: `features/email/server` (JVM-only, `mppJavaProject`). All production code in `commonMain` (Jakarta imports already legal there — existing precedent in this module). No other module changes. No `build.gradle` changes (see A.4).

### A.1 New file: `features/email/server/src/commonMain/kotlin/EmailsService.kt`

Package mirrors the server `EmailFeature.kt` placement. Exact content (Coding writes this verbatim):

```kotlin
package dev.inmo.wishlist.features.email.server

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.models.EmailAttachment

/**
 * Server-only email-sending surface of the email feature.
 *
 * Declares direct message-sending operations for other server-side components, resolved from
 * Koin. Deliberately NOT exposed through [EmailFeature], the client `KtorEmailFeature`, or any
 * HTTP route — this interface never crosses the server boundary.
 *
 * All methods share one result contract: `true` when the SMTP server accepted the message;
 * `false` when SMTP is not configured (disabled no-op mode) or an error occurred. Errors are
 * logged at warn level and never thrown to the caller.
 */
interface EmailsService {

    /**
     * Sends an email with a plain-text body.
     *
     * @param recipient Target email address.
     * @param subject Subject header of the message.
     * @param text Plain-text body (sent as `text/plain`, UTF-8).
     * @return `true` when the SMTP server accepted the message; `false` when SMTP is disabled
     *   or an error occurred (logged at warn level).
     */
    suspend fun sendText(recipient: Email, subject: String, text: String): Boolean

    /**
     * Sends an email with a plain-text body and file attachments.
     *
     * Attachment content is streamed to the SMTP connection during send: each
     * [EmailAttachment.content] provider may be invoked more than once and must return a fresh
     * stream on every call; attachment bytes are never buffered wholesale in memory. An empty
     * [attachments] list produces a valid multipart message containing only the text part.
     *
     * @param recipient Target email address.
     * @param subject Subject header of the message.
     * @param text Plain-text body part (sent as `text/plain`, UTF-8).
     * @param attachments Attachments appended after the text part, in list order.
     * @return `true` when the SMTP server accepted the message; `false` when SMTP is disabled
     *   or an error occurred (logged at warn level).
     */
    suspend fun sendTextWithAttachments(
        recipient: Email,
        subject: String,
        text: String,
        attachments: List<EmailAttachment>
    ): Boolean

    /**
     * Sends an email with an HTML body (`text/html; charset=utf-8`).
     *
     * @param recipient Target email address.
     * @param subject Subject header of the message.
     * @param html HTML markup used as the message body.
     * @return `true` when the SMTP server accepted the message; `false` when SMTP is disabled
     *   or an error occurred (logged at warn level).
     */
    suspend fun sendHtml(recipient: Email, subject: String, html: String): Boolean
}
```

### A.2 New file: `features/email/server/src/commonMain/kotlin/models/EmailAttachment.kt`

New `models` package in the server module (mirrors `email/common` layout). Exact content:

```kotlin
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
```

### A.3 Edit: `features/email/server/src/commonMain/kotlin/services/SmtpEmailService.kt`

Exact resulting file content (Coding replaces the whole file with this; `buildSession` and `isFeatureEnabled` bodies are byte-identical to current):

```kotlin
package dev.inmo.wishlist.features.email.server.services

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.w
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.EmailConfig
import dev.inmo.wishlist.features.email.server.EmailsService
import dev.inmo.wishlist.features.email.server.SmtpConfig
import dev.inmo.wishlist.features.email.server.models.EmailAttachment
import jakarta.activation.DataHandler
import jakarta.activation.DataSource
import jakarta.mail.Message
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.Properties

/**
 * SMTP delivery service backed by Jakarta Mail (Angus Mail); the server-side [EmailsService]
 * implementation.
 *
 * Performs email delivery on [Dispatchers.IO] to keep blocking socket I/O off the event loop.
 * When [EmailConfig.smtp] is `null` or the host is blank, all delivery methods are no-ops and
 * [isFeatureEnabled] returns `false`.
 *
 * This class is an SMTP transport only — caller-identity checks and user-email persistence are
 * handled by [EmailFeatureService], which wraps this class.
 *
 * @param config Full email config slice decoded from the server config JSON.
 */
class SmtpEmailService(private val config: EmailConfig) : EmailsService {

    private val logger = KSLog("SmtpEmailService")

    /**
     * Returns `true` when an SMTP host is configured and non-blank.
     *
     * @return Whether SMTP delivery is available.
     */
    suspend fun isFeatureEnabled(): Boolean =
        config.smtp != null && config.smtp.host.isNotBlank()

    /**
     * Sends a test email to [recipient] using the configured SMTP settings.
     *
     * Routed through the shared [send] skeleton; runs the blocking [Transport.send] call on
     * [Dispatchers.IO].
     *
     * @param recipient Target address for the test message.
     * @return `true` when the message was accepted by the SMTP server; `false` when the feature
     *   is disabled or an error occurs (errors are logged at warn level).
     */
    suspend fun sendTestEmail(recipient: Email): Boolean = send(
        recipient = recipient,
        subject = "Test email from WishlistApp",
        logLabel = "sendTestEmail"
    ) {
        setText("This is a test email sent from WishlistApp to verify SMTP configuration.")
    }

    /**
     * Sends an email with a plain-text body.
     *
     * @param recipient Target email address.
     * @param subject Subject header of the message.
     * @param text Plain-text body (sent as `text/plain`, UTF-8).
     * @return `true` when the SMTP server accepted the message; `false` when SMTP is disabled
     *   or an error occurred (logged at warn level).
     */
    override suspend fun sendText(recipient: Email, subject: String, text: String): Boolean = send(
        recipient = recipient,
        subject = subject,
        logLabel = "sendText"
    ) {
        setText(text, "UTF-8")
    }

    /**
     * Sends an email with a plain-text body and file attachments.
     *
     * The message is assembled as a [MimeMultipart]: one text part followed by one part per
     * attachment in list order. Each attachment part streams its bytes through
     * [EmailAttachmentDataSource], so content is encoded on the fly during [Transport.send]
     * without whole-payload buffering.
     *
     * @param recipient Target email address.
     * @param subject Subject header of the message.
     * @param text Plain-text body part (sent as `text/plain`, UTF-8).
     * @param attachments Attachments appended after the text part, in list order.
     * @return `true` when the SMTP server accepted the message; `false` when SMTP is disabled
     *   or an error occurred (logged at warn level).
     */
    override suspend fun sendTextWithAttachments(
        recipient: Email,
        subject: String,
        text: String,
        attachments: List<EmailAttachment>
    ): Boolean = send(
        recipient = recipient,
        subject = subject,
        logLabel = "sendTextWithAttachments"
    ) {
        val multipart = MimeMultipart()
        multipart.addBodyPart(
            MimeBodyPart().apply {
                setText(text, "UTF-8")
            }
        )
        attachments.forEach { attachment ->
            multipart.addBodyPart(
                MimeBodyPart().apply {
                    dataHandler = DataHandler(EmailAttachmentDataSource(attachment))
                    fileName = attachment.fileName
                    disposition = Part.ATTACHMENT
                }
            )
        }
        setContent(multipart)
    }

    /**
     * Sends an email with an HTML body (`text/html; charset=utf-8`).
     *
     * @param recipient Target email address.
     * @param subject Subject header of the message.
     * @param html HTML markup used as the message body.
     * @return `true` when the SMTP server accepted the message; `false` when SMTP is disabled
     *   or an error occurred (logged at warn level).
     */
    override suspend fun sendHtml(recipient: Email, subject: String, html: String): Boolean = send(
        recipient = recipient,
        subject = subject,
        logLabel = "sendHtml"
    ) {
        setContent(html, "text/html; charset=utf-8")
    }

    /**
     * Shared send skeleton used by every delivery method.
     *
     * Centralizes the disabled-mode check (warn-log + `false` when [EmailConfig.smtp] is `null`
     * or its host is blank), session construction via [buildSession], the message envelope
     * (`From`, `To`, subject), the blocking [Transport.send] on [Dispatchers.IO], and the
     * `runCatching`/warn-log failure handling.
     *
     * @param recipient Target email address placed into the `To` header.
     * @param subject Subject header, encoded as UTF-8.
     * @param logLabel Method name used to prefix the disabled-mode and failure log messages.
     * @param fillContent Body-filling block applied to the message after the envelope is set.
     * @return `true` when the SMTP server accepted the message; `false` when SMTP is disabled
     *   or an error occurred (logged at warn level).
     */
    private suspend fun send(
        recipient: Email,
        subject: String,
        logLabel: String,
        fillContent: MimeMessage.() -> Unit
    ): Boolean {
        val smtp = config.smtp
        if (smtp == null || smtp.host.isBlank()) {
            logger.w { "$logLabel called but SMTP is not configured — skipping." }
            return false
        }
        return runCatching {
            withContext(Dispatchers.IO) {
                val session = buildSession(smtp)
                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(smtp.from.string))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient.string))
                    setSubject(subject, "UTF-8")
                    fillContent()
                }
                Transport.send(message)
            }
            true
        }.getOrElse { e ->
            logger.w(e) { "$logLabel failed to send email to ${recipient.string}" }
            false
        }
    }

    /**
     * Builds a [Session] from [smtp] settings, configuring STARTTLS and/or SSL as requested.
     *
     * @param smtp Validated SMTP settings.
     * @return Ready-to-use [Session].
     */
    private fun buildSession(smtp: SmtpConfig): Session {
        val props = Properties().apply {
            put("mail.smtp.host", smtp.host)
            put("mail.smtp.port", smtp.port.toString())
            if (smtp.useTls) {
                put("mail.smtp.starttls.enable", "true")
            }
            if (smtp.useSsl) {
                put("mail.smtp.ssl.enable", "true")
            }
            if (smtp.username != null) {
                put("mail.smtp.auth", "true")
            }
        }
        return when {
            smtp.username != null && smtp.password != null -> {
                Session.getInstance(props, object : jakarta.mail.Authenticator() {
                    override fun getPasswordAuthentication() =
                        jakarta.mail.PasswordAuthentication(smtp.username, smtp.password)
                })
            }
            else -> Session.getInstance(props)
        }
    }
}

/**
 * Read-only [DataSource] bridge between the suspend streaming provider of an [EmailAttachment]
 * and Jakarta Mail's blocking attachment API.
 *
 * Jakarta Mail may call [getInputStream] more than once while encoding a message; per the
 * [DataSource] contract every call returns a fresh stream positioned at the beginning, obtained
 * by re-invoking [EmailAttachment.content]. Content therefore streams through the mail encoder
 * without being buffered wholesale.
 *
 * `internal` (not `private`) so the module's own unit tests can exercise the bridge directly;
 * the class is not part of the module's public API.
 *
 * @param attachment Attachment whose metadata and content provider back this data source.
 */
internal class EmailAttachmentDataSource(
    private val attachment: EmailAttachment
) : DataSource {

    /**
     * Returns a fresh stream over the full attachment content.
     *
     * Invokes the suspend provider via a bare [runBlocking] (no outer context is passed in, so
     * there is no dispatcher re-entry). Blocking is safe here: Jakarta Mail calls this method on
     * the [Dispatchers.IO] worker thread inside `Transport.send`, which is already a blocking
     * context.
     *
     * @return New [InputStream] positioned at the beginning of the content.
     */
    override fun getInputStream(): InputStream = runBlocking { attachment.content() }

    /**
     * Always throws: this data source is read-only.
     *
     * @return Never returns.
     * @throws UnsupportedOperationException on every call.
     */
    override fun getOutputStream(): OutputStream =
        throw UnsupportedOperationException("EmailAttachmentDataSource is read-only")

    /**
     * Returns the attachment's MIME type.
     *
     * @return Value of [EmailAttachment.mimeType].
     */
    override fun getContentType(): String = attachment.mimeType

    /**
     * Returns the attachment's file name.
     *
     * @return Value of [EmailAttachment.fileName].
     */
    override fun getName(): String = attachment.fileName
}
```

Notes for Coding:
- `dataHandler = ...`, `fileName = ...`, `disposition = ...` are Kotlin synthetic property accessors for `MimeBodyPart.setDataHandler/setFileName/setDisposition` — valid as written.
- `isFeatureEnabled` and `sendTestEmail` keep their exact public signatures; `sendTestEmail`'s body-filling lambda keeps the original default `setText(String)` overload to preserve behavior.
- No `else if` anywhere (single binary `if` in `send` is allowed; `buildSession` keeps its `when`).

### A.4 Dependency check — NO `build.gradle` change needed

Verified against the resolved artifact metadata in the local Gradle cache: `angus-mail-2.0.3.pom` declares compile-scope (no `<scope>` tag) dependencies on `jakarta.activation:jakarta.activation-api` and `jakarta.mail:jakarta.mail-api`; `jakarta.activation` is present in the dependency cache. The module declares `api libs.angus.mail`, and Maven compile-scope dependencies land on the Gradle compile classpath, so `jakarta.activation.DataHandler`/`DataSource` compile without any `build.gradle` edit. Test dependencies also need no edit (see B.1).

### A.5 Edit: `features/email/server/src/commonMain/kotlin/Plugin.kt`

Two edits, no import changes (`EmailsService` is in the same package):

1. In `setupDI`, insert directly after the `single { SmtpEmailService(get<EmailConfig>()) }` line (current line 31):

```kotlin
        single<EmailsService> { get<SmtpEmailService>() }
```

2. In the object KDoc, replace the bullet

```
 * - [SmtpEmailService] as the SMTP delivery transport.
```

with

```
 * - [SmtpEmailService] as the SMTP delivery transport, additionally bound under its
 *   [EmailsService] interface for server-side consumers that need direct sends.
```

## B. Test spec

### B.1 Test source set, location, and dependencies (determined from build inspection)

- The module applies `mppJavaProject` = `defaultProject` + `enableMPPJvm` (Kotlin MPP, single JVM target). The templates already wire test dependencies: `commonTest` gets `kotlin("test-common")`, `kotlin("test-annotations-common")`, and `libs.kotlin.coroutines.test` (kotlinx-coroutines-test 1.11.0, provides `runTest`); `jvmTest` gets `kotlin("test-junit")` (JUnit 4 runner). **No `build.gradle` change is needed for tests.**
- **Location: `features/email/server/src/commonTest/kotlin/`.** Rationale: the module keeps ALL production code in `commonMain` (including Jakarta/`java.*` imports — legal because a single-target MPP module compiles its common source sets directly as platform sources); `commonTest` mirrors that convention, is folded into the `jvmTest` compilation, and executes under JUnit 4 via the template's `kotlin("test-junit")`. Java APIs (`ByteArrayInputStream`) and the `internal` bridge class are both visible there (test compilations associate with main by default).
- Test framework surface: `kotlin.test.*` assertions (`assertFalse`, `assertEquals`, `assertContentEquals`, `assertFailsWith`) + `kotlinx.coroutines.test.runTest` for suspend cases. The repository currently ships zero tests anywhere; these are the first, so no existing convention is contradicted.
- Run: `./gradlew :wishlist.features.email.server:jvmTest` (also runs as part of `:wishlist.features.email.server:build`, which Coding must run anyway).
- Note: `readBytes()` in tests is assertion-side only; the no-`ByteArray` rule constrains production send paths, not test verification.

### B.2 Test file 1: `features/email/server/src/commonTest/kotlin/services/SmtpEmailServiceDisabledTest.kt`

Package `dev.inmo.wishlist.features.email.server.services`. Class `SmtpEmailServiceDisabledTest` (KDoc: verifies disabled/no-op mode; no SMTP server involved). Fixtures: `Email("recipient@example.com")` (companion `invoke`), `SmtpEmailService(EmailConfig(smtp = null))`, and `SmtpEmailService(EmailConfig(smtp = SmtpConfig(host = "", from = Email("noreply@example.com"))))`.

| # | Test | Input | Expected |
|---|------|-------|----------|
| 1 | `sendTextReturnsFalseWhenSmtpIsNull` | `smtp = null`; `sendText(recipient, "subject", "body")` | returns `false` |
| 2 | `sendHtmlReturnsFalseWhenSmtpIsNull` | `smtp = null`; `sendHtml(recipient, "subject", "<p>body</p>")` | returns `false` |
| 3 | `sendTextWithAttachmentsReturnsFalseWhenSmtpIsNullAndDoesNotInvokeProvider` | `smtp = null`; one `EmailAttachment("file.txt", "text/plain") { invocations++; ByteArrayInputStream(ByteArray(0)) }` | returns `false` AND `invocations == 0` (disabled mode must not touch attachment content) |
| 4 | `sendTextReturnsFalseWhenHostIsBlank` | `smtp = SmtpConfig(host = "", from = ...)`; `sendText(recipient, "subject", "body")` | returns `false` |
| 5 | `isFeatureEnabledFalseWhenSmtpIsNull` | `smtp = null` | `isFeatureEnabled()` returns `false` |

All suspend tests wrapped in `runTest { ... }`.

### B.3 Test file 2: `features/email/server/src/commonTest/kotlin/models/EmailAttachmentTest.kt`

Package `dev.inmo.wishlist.features.email.server.models`. Class `EmailAttachmentTest` (KDoc: verifies the streaming provider contract).

| # | Test | Input | Expected |
|---|------|-------|----------|
| 1 | `contentProviderReturnsIndependentFreshStreamsOnEachInvocation` | `payload = "attachment payload".encodeToByteArray()`; provider increments a counter and builds a NEW `ByteArrayInputStream(payload)` per call. Invoke `content()` and read fully; invoke `content()` again and read fully | counter `== 2`; both reads `assertContentEquals(payload, ...)` — second stream readable from the start after first was fully consumed |
| 2 | `mimeTypeDefaultsToOctetStream` | `EmailAttachment("file.bin") { ByteArrayInputStream(ByteArray(0)) }` | `mimeType == "application/octet-stream"` |

Test 1 wrapped in `runTest`; test 2 is plain.

### B.4 Test file 3: `features/email/server/src/commonTest/kotlin/services/EmailAttachmentDataSourceTest.kt`

Package `dev.inmo.wishlist.features.email.server.services`. Class `EmailAttachmentDataSourceTest` (KDoc: verifies the suspend-to-blocking `DataSource` bridge). Direct seam available because the bridge is `internal` (decision above). Fixture: `payload = "bridge payload".encodeToByteArray()`; helper building `EmailAttachmentDataSource(EmailAttachment("report.pdf", "application/pdf") { onInvocation(); ByteArrayInputStream(payload) })`.

| # | Test | Input | Expected |
|---|------|-------|----------|
| 1 | `getInputStreamReturnsFreshStreamOnEachCall` | call `inputStream` twice, read each fully | both reads equal `payload`; provider invocation counter `== 2` |
| 2 | `metadataEchoesAttachment` | fixture above | `contentType == "application/pdf"`; `name == "report.pdf"` |
| 3 | `getOutputStreamThrowsUnsupportedOperation` | call `outputStream` | `assertFailsWith<UnsupportedOperationException>` |

No `runTest` needed — `getInputStream()` itself performs the `runBlocking` bridging (this also directly exercises the production `runBlocking` path from a plain thread).

### B.5 KNOWN LIMITATION (documented decision, NOT an operator question)

The live-SMTP success path (`Transport.send` accepted → method returns `true`) is external-service integration: the module ships no SMTP test harness and the operator scoped this task as "simple methods". It is verified by (a) the module build compiling the full send pipeline and (b) the existing manual verification path `POST /email/sendTest` → `sendTestEmail`, which after decision (b) routes through the very same `send(...)` skeleton as the three new methods — so a manual test-email send exercises the shared envelope/session/transport code end to end. Scope was decided by the Orchestrator before this step; recorded here per `agents/ARCHITECTURE.md` "Test Planning Requirement" as the handling of not-unit-testable functionality. No open question remains.

## C. README edit spec (`features/email/README.md` — applied by Coding, NOT by this role)

`## Operator Notes`, `## Overview`, and `## Routes` stay untouched.

### C.1 `## Models` table

**Replace** the stale `EmailFeature` row

```markdown
| `EmailFeature` | `email/common` | Shared interface: `isFeatureEnabled()`, `sendTestEmail(recipient)`, `setMyEmail(email?)`. Implemented by both `SmtpEmailService` (server) and `KtorEmailFeature` (client). |
```

**with two rows:**

```markdown
| `EmailFeature` (server) | `email/server` | Server-side interface: `isFeatureEnabled()`, `sendTestEmail(callerId, recipient)`, `setMyEmail(callerId, email?)` — every caller-scoped method receives the authenticated `UserId` explicitly. Implemented by `EmailFeatureService`. |
| `EmailFeature` (client) | `email/client` | Client-side interface: `isFeatureEnabled()`, `sendTestEmail(recipient)`, `setMyEmail(email?)` — caller identity is resolved server-side from the bearer token. Implemented by `KtorEmailFeature`. |
```

**Replace** the stale `SmtpEmailService` row

```markdown
| `SmtpEmailService` | `email/server` | Server `EmailFeature` impl; builds `jakarta.mail.Session` from `SmtpConfig` and calls `Transport.send` on `Dispatchers.IO`. Returns `false` when disabled or on error. |
```

**with:**

```markdown
| `SmtpEmailService` | `email/server` | SMTP transport; implements `EmailsService`. Builds `jakarta.mail.Session` from `SmtpConfig` and calls `Transport.send` on `Dispatchers.IO`. Returns `false` when disabled or on error. Wrapped by `EmailFeatureService` for the `EmailFeature` surface. |
```

**Insert three new rows** directly after the (replaced) `SmtpEmailService` row, before the `KtorEmailFeature` row:

```markdown
| `EmailFeatureService` | `email/server` | Server `EmailFeature` impl; wraps `SmtpEmailService` + `UsersRepo`: enforces root-only access for `sendTestEmail` and persists the caller's email address for `setMyEmail`. |
| `EmailsService` | `email/server` | Server-only send interface (no HTTP exposure): `sendText(recipient, subject, text)`, `sendTextWithAttachments(recipient, subject, text, attachments)`, `sendHtml(recipient, subject, html)` — all `suspend`, all return `Boolean` (`false` when SMTP is disabled or on error). Implemented by `SmtpEmailService`; bound in Koin. |
| `EmailAttachment` | `email/server` | Attachment model for `sendTextWithAttachments`: `fileName`, `mimeType` (default `application/octet-stream`), `content: suspend () -> InputStream`. The provider may be invoked multiple times and must return a fresh stream on each call; content is streamed, never buffered as a whole `ByteArray`. |
```

### C.2 `## Architecture Notes`

**Insert a new bullet** directly after the existing "**Jakarta Mail (Angus):**" bullet:

```markdown
- **`EmailsService` (server-only send surface):** `EmailsService` (`sendText` / `sendTextWithAttachments` / `sendHtml`, each with an explicit `subject`) is an internal server capability implemented by `SmtpEmailService` and bound in Koin (`single<EmailsService> { get<SmtpEmailService>() }`). It is deliberately NOT wired into `EmailFeature`, `KtorEmailFeature`, or any HTTP route. All four send paths (including `sendTestEmail`) share one private `send(...)` skeleton (disabled-mode check, session, envelope, `Transport.send` on `Dispatchers.IO`, warn-log on failure). Attachments stream through the `internal` `EmailAttachmentDataSource` bridge: every `getInputStream()` call re-invokes the attachment's `suspend () -> InputStream` provider via a bare `runBlocking` (safe — Jakarta Mail calls it on the `Dispatchers.IO` worker inside `Transport.send`) and must yield a fresh stream, so content is encoded on the fly without whole-payload buffering; `getOutputStream()` throws (read-only source). Disabled-mode and streaming contracts are unit-tested in `src/commonTest`; the live-SMTP success path is intentionally not unit-tested (external integration — verified via build + the manual `POST /email/sendTest` path). |
```

(Coding: drop the trailing `|` if the editor keeps it — the bullet is a list item, not a table row; exact text ends at "…`POST /email/sendTest` path).")

**Replace** the existing "**DI placement:**" bullet

```markdown
- **DI placement:** Server Plugin wires `EmailConfig → SmtpEmailService → EmailFeature` and registers `EmailRoutingsConfigurator` (with random qualifier). `jvmMain/JVMPlugin` is a thin delegator listed in `sample.config.json`.
```

**with:**

```markdown
- **DI placement:** Server Plugin wires `EmailConfig → SmtpEmailService → EmailFeature` (via `EmailFeatureService`), additionally binds `SmtpEmailService` under `EmailsService`, and registers `EmailRoutingsConfigurator` (with random qualifier). `jvmMain/JVMPlugin` is a thin delegator listed in `sample.config.json`.
```

## Handoff to Coding (step `003-coding`)

Deliverables, in order:
1. Create `EmailsService.kt` (A.1) and `models/EmailAttachment.kt` (A.2) verbatim.
2. Replace `services/SmtpEmailService.kt` with A.3.
3. Apply the two `Plugin.kt` edits (A.5).
4. Create the three test files per B.2–B.4 (exact case tables; KDocs on test classes; `kotlin.test` + `runTest`).
5. Apply the README edits per C.1–C.2 (never touch `## Operator Notes`; per `agents/SHORTCUTS.md`, markdown filling is a `haiku`-agent job — the Coding orchestration should delegate the README application accordingly while keeping the exact texts above).
6. Run `./gradlew :wishlist.features.email.server:build` (compiles + runs jvmTest); fix one cycle if needed per `agents/CODING.md`.
7. Run `ast-index rebuild` (source `.kt` files changed).
8. Commit step report + all changed source files on `fix/44-email`; no push.

Verification/Validating roles: check the diff contains NO changes under `features/email/common`, `features/email/client`, `configurators/`, routes, or DTOs; no `ByteArray` materialization of attachment content in production code; KDocs on every new declaration; no `else if`; tests green in the build output.

No OPEN QUESTION FOR OPERATOR remains.
