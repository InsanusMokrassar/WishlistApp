# PROMPT

## Source operator prompt (verbatim)

> for `SmtpEmailService` add several simple methods - send email with text only, send email with text and attachments, send email with text only as html

## Operator clarifications (collected by Orchestrator before Planning, via direct terminal question)

The Orchestrator asked the operator three design questions before starting Planning. Answers (authoritative — do NOT re-ask, treat as resolved requirements):

1. **Attachment representation.** Attachments MUST NOT be passed as an in-memory `ByteArray`. Each attachment MUST expose its content through a `suspend () -> InputStream` provider so full bytes are never loaded into memory at once (lazy streaming). An attachment model type therefore carries: a file name, a MIME/content type, and the `suspend () -> InputStream` content provider.

2. **Scope / interface.** Do NOT wire these into the shared cross-module `EmailFeature` interface, its client (`KtorEmailFeature`), or any HTTP route. Instead introduce a NEW server-side interface named `EmailsService` declaring the three send methods, and make `SmtpEmailService` implement (inherit) `EmailsService`. Server-only change; no client/common-DTO/route changes.

3. **Subject parameter.** Every new send method takes an explicit `subject: String` parameter.

## Derived requirement summary (for Planning/Architecture)

New server-side capability in `features/email/server` (JVM-only module, `mppJavaProject`; Jakarta Mail imports already legal in its `commonMain`):

- New interface `EmailsService` with three `suspend` methods:
  - `sendText(recipient: Email, subject: String, text: String): Boolean` — plain-text body only.
  - `sendTextWithAttachments(recipient: Email, subject: String, text: String, attachments: List<EmailAttachment>): Boolean` — plain-text body + attachments.
  - `sendHtml(recipient: Email, subject: String, html: String): Boolean` — HTML body only (Content-Type `text/html`).
- New attachment model `EmailAttachment` carrying `fileName: String`, `mimeType: String` (default e.g. `application/octet-stream`), and `content: suspend () -> InputStream`.
- `SmtpEmailService : EmailsService` implements the three methods, reusing existing `buildSession(smtp)` + `Dispatchers.IO` + no-op-when-disabled + `runCatching`/warn-log-on-failure conventions already present in `sendTestEmail`.
- Return `Boolean`: `false` when SMTP not configured or on error (mirror existing `sendTestEmail` behavior), `true` when `Transport.send` accepted the message.
- Attachments implemented via Jakarta Mail `MimeMultipart` + `MimeBodyPart`, streaming from the provider without buffering all bytes in memory. The `DataSource.getInputStream()` bridge is non-suspend; invoking the suspend provider from inside it (already on `Dispatchers.IO`) is acceptable — Architecture to specify the exact bridge.

## Constraints

- Follow `agents/CODING.md`: KDocs on every new declaration, `when` over `else if`, run the module build task after coding.
- Update `features/email/README.md` `## Architecture Notes` / `## Models` to reflect the new `EmailsService` + `EmailAttachment` (Coding/Architecture per role rules). Never touch `## Operator Notes`.
- Branch: work stays on current branch `fix/44-email` (email feature work). Do NOT create a new branch, do NOT touch master.
- Run `ast-index rebuild` after `.kt` changes (Coding).
