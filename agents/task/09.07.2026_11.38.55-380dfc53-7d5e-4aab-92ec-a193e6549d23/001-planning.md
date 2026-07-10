Model: fable
Execution time: ~280
Tokens used: ~65000 (estimated from context size; exact counter not exposed to the agent)
Changed files: agents/task/09.07.2026_11.38.55-380dfc53-7d5e-4aab-92ec-a193e6549d23/001-planning.md

# 001 — Planning

## Model choice argument

`agents/SHORTCUTS.md` sets the Planning role model priority as `fable / opus / sonnet`. This agent runs on `fable` (claude-fable-5), which is the highest-priority option and is available, so the best-priority model is used. No fallback was needed.

## Task understanding

Add a server-only email-sending capability to `features/email/server`:

1. Introduce a NEW server-side interface `EmailsService` declaring three `suspend` send methods, each taking an explicit `subject: String` and returning `Boolean` (mirroring the `sendTestEmail` contract: `false` when SMTP is not configured or on error, `true` when the SMTP server accepted the message):
   - send email with plain-text body only;
   - send email with plain-text body plus attachments;
   - send email with HTML body only.
2. Introduce a NEW attachment model `EmailAttachment` whose content is exposed exclusively through a `suspend () -> InputStream` provider — attachment bytes must NEVER be materialized as an in-memory `ByteArray`.
3. Make the existing `SmtpEmailService` implement `EmailsService`.
4. Strictly server-only: NO changes to the `EmailFeature` interfaces (server or client variant), NO changes to `KtorEmailFeature`, NO new or changed HTTP routes, NO common-module DTO changes.
5. Update `features/email/README.md` (`## Models`, `## Architecture Notes`) to document the new types; never touch `## Operator Notes`.

Work stays on branch `fix/44-email`.

## Open questions asked & answers received

All three design questions were asked by the Orchestrator to the human operator via direct terminal question BEFORE Planning started, and the answers are recorded verbatim in the task `PROMPT.md` under "## Operator clarifications". Status: asked & answered — treated as resolved requirements, not re-asked.

1. **Attachment representation** — ANSWERED: no `ByteArray`; each attachment exposes content via a `suspend () -> InputStream` provider (lazy streaming). The model carries a file name, a MIME/content type, and the content provider.
2. **Scope / interface** — ANSWERED: do NOT extend `EmailFeature` (either variant), `KtorEmailFeature`, or any HTTP route. Introduce a new server-side interface named `EmailsService` with the three send methods; `SmtpEmailService` implements it. Server-only change.
3. **Subject parameter** — ANSWERED: every new send method takes an explicit `subject: String` parameter.

Per `agents/PLAN.md` step 3/4: no genuinely new blocking ambiguity was discovered during investigation. Two minor discretionary points (Koin binding for `EmailsService`; whether to refactor `sendTestEmail` onto a shared private helper) are non-blocking implementation decisions explicitly delegated to the Architecture role below. **No OPEN QUESTION FOR OPERATOR remains.**

## Investigation results

Code navigation was performed with `ast-index` (`symbol`, `refs`) plus targeted file reads.

### Current state of the code

- `SmtpEmailService` (`features/email/server/src/commonMain/kotlin/services/SmtpEmailService.kt`, line 29) is a plain `class SmtpEmailService(private val config: EmailConfig)` with NO supertype. Adding `: EmailsService` creates no conflict. Its members are `isFeatureEnabled()`, `sendTestEmail(recipient)`, and `private buildSession(smtp)` — none clash with the planned `sendText` / `sendTextWithAttachments` / `sendHtml` names.
- `ast-index symbol EmailsService` and `ast-index symbol EmailAttachment` both return "No symbols found" — the new names are free across the whole repo.
- `EmailFeature` exists as TWO separate interfaces after commit `a9d85bf`: `features/email/server/src/commonMain/kotlin/EmailFeature.kt` (server variant, methods take `callerId: UserId`) and `features/email/client/src/commonMain/kotlin/EmailFeature.kt` (client variant). `SmtpEmailService` no longer implements `EmailFeature` — `EmailFeatureService` (`services/EmailFeatureService.kt`) does, wrapping `SmtpEmailService` + `UsersRepo`. Neither variant is touched by this task.
- DI (`features/email/server/src/commonMain/kotlin/Plugin.kt`): `single { SmtpEmailService(get<EmailConfig>()) }` (line 31), `single<EmailFeature> { EmailFeatureService(get<SmtpEmailService>(), get<UsersRepo>()) }` (line 32), routing configurator with random qualifier. No existing `EmailsService` binding to conflict with.
- Module build (`features/email/server/build.gradle`): `mppJavaProject` (JVM-only), `api libs.angus.mail` (`org.eclipse.angus:angus-mail:2.0.3`). Jakarta Mail imports are already used from `commonMain` of this module, so the new types can live in `commonMain` and legally reference `java.io.InputStream` and `jakarta.mail.*` / `jakarta.activation.*`.
- `Email` model (`features/email/common/src/commonMain/kotlin/models/Email.kt`): `@JvmInline value class Email private constructor(val string: String)` — accessed as `.string` (already used by `SmtpEmailService`). No changes needed.
- Existing conventions in `sendTestEmail` that the new methods MUST mirror: (a) when `config.smtp == null || smtp.host.isBlank()` → warn log + `return false` (no-op mode); (b) body wrapped in `runCatching { withContext(Dispatchers.IO) { ... Transport.send(message) }; true }.getOrElse { e -> logger.w(e) { ... }; false }`; (c) session built via the private `buildSession(smtp)`; (d) from-address `InternetAddress(smtp.from.string)`, recipients `InternetAddress.parse(recipient.string)`.
- `README.md` staleness found: the `## Models` table still describes `EmailFeature` as a single shared interface in `email/common` "implemented by both `SmtpEmailService` (server) and `KtorEmailFeature` (client)", and describes `SmtpEmailService` as the "Server `EmailFeature` impl". Both rows are outdated after the `a9d85bf` split (server/client variants; `EmailFeatureService` is the server impl and is missing from the table entirely). Since the README must be updated for `EmailsService`/`EmailAttachment` anyway, the same edit should correct these stale rows.

### Risks / constraints analyzed

1. **Suspend provider vs. Jakarta Mail's blocking `DataSource` bridge (main technical risk).** Attachments in Jakarta Mail are attached via `MimeBodyPart.setDataHandler(DataHandler(dataSource))`, and `jakarta.activation.DataSource.getInputStream()` is a non-suspend, throwing-IOException method that the mail runtime may call MORE THAN ONCE (the contract requires each call to return a fresh stream positioned at the beginning). Consequences:
   - It is NOT safe to invoke the suspend provider once up front and wrap the resulting single `InputStream` in a `DataSource` — a second `getInputStream()` call would return an already-consumed stream and silently corrupt the attachment.
   - Recommended bridge: a private `DataSource` implementation whose `getInputStream()` invokes the suspend provider via `runBlocking { attachment.content() }`, returning a fresh stream on every call. Blocking is acceptable here because `Transport.send` already executes inside `withContext(Dispatchers.IO)` on an IO worker thread; use a bare `runBlocking { ... }` (do NOT pass the outer coroutine context in, to avoid dispatcher re-entry issues). `getOutputStream()` throws `UnsupportedOperationException`; `getContentType()` returns the attachment's MIME type; `getName()` returns the file name.
   - Memory constraint holds: `DataHandler` + `DataSource` stream the content through Jakarta Mail's encoder (base64/quoted-printable) during `Transport.send` without buffering the whole payload. Forbidden implementations: `ByteArrayDataSource`, `readBytes()`/`readAllBytes()` on the provider stream, `MimeBodyPart.setContent(byteArray, ...)`.
2. **`jakarta.activation` compile-time availability.** `DataHandler`/`DataSource` come from `jakarta.activation-api`, a transitive `api`-scope dependency of `jakarta.mail-api` inside `angus-mail`. Expected to compile without build.gradle changes; the Coding role verifies via the module build task and adds an explicit dependency ONLY if compilation proves otherwise (none anticipated).
3. **Code duplication across four send paths.** `sendTestEmail` plus the three new methods share an identical skeleton (disabled-check, session, from/to/subject, runCatching/log). Plan: extract a private helper in `SmtpEmailService`, e.g. `private suspend fun send(recipient: Email, subject: String, failureLogLabel: String, fillContent: MimeMessage.() -> Unit): Boolean`, holding the disabled-warn-log, `buildSession`, message envelope, `Transport.send`, and `runCatching`/`getOrElse` warn-log in ONE place; each public method supplies only its body-filling lambda. Refactoring `sendTestEmail` onto the same helper changes no public signature and no observable behavior — recommended, final call to Architecture.
4. **HTML body.** Use `MimeMessage.setContent(html, "text/html; charset=utf-8")`. For the new plain-text methods prefer `setText(text, "utf-8")` (explicit charset; existing `sendTestEmail` uses the default overload — harmless divergence, Architecture may align it during the helper refactor).
5. **Attachments assembly.** `MimeMultipart()` containing: first a `MimeBodyPart` with the plain text, then one `MimeBodyPart` per attachment with `dataHandler = DataHandler(<bridge DataSource>)`, `fileName = attachment.fileName`, `disposition = Part.ATTACHMENT`. An empty `attachments` list yields a valid single-part multipart message — no special guard required (Architecture may choose to note this in KDoc).
6. **`EmailAttachment` shape.** Plain `class` (not `data class` — structural equality over a lambda is meaningless and `copy` invites misuse; not a value class — three properties, so the value-class property-naming rule from `agents/CODING.md` does not apply). NOT `@Serializable` (server-only, carries a non-serializable suspend lambda). `mimeType` defaults to `"application/octet-stream"`.
7. **Coding rules that apply downstream** (`agents/CODING.md`): KDocs on every new class/interface/fun/property (no empty blocks; `@param`/`@return` tags); `when` instead of any `else if` chain (none anticipated — the disabled-check is a single binary `if`); run `./gradlew :wishlist.features.email.server:build` after changes (module name confirmed from `build.gradle` project references); run `ast-index rebuild` after `.kt` changes. `agents/local.CODING.md` (Bootstrap rule) is web-client-only and irrelevant here.
8. **Git scope.** All work stays on `fix/44-email`; Coding commits step report + source; no pushes (Orchestrator pushes).

## Final plan (handoff to Architecture)

Target module: `features/email/server` (JVM-only, `commonMain`). No other module changes.

### New file 1 — `features/email/server/src/commonMain/kotlin/EmailsService.kt`

Package `dev.inmo.wishlist.features.email.server` (mirrors the server `EmailFeature.kt` placement):

```kotlin
interface EmailsService {
    suspend fun sendText(recipient: Email, subject: String, text: String): Boolean
    suspend fun sendTextWithAttachments(
        recipient: Email,
        subject: String,
        text: String,
        attachments: List<EmailAttachment>
    ): Boolean
    suspend fun sendHtml(recipient: Email, subject: String, html: String): Boolean
}
```

Full KDocs required: interface purpose (server-side SMTP send surface; no-op `false` when SMTP disabled), per-method behavior, `@param`/`@return` on every method (return semantics: `true` = accepted by SMTP server; `false` = disabled or error, errors logged at warn level).

### New file 2 — `features/email/server/src/commonMain/kotlin/models/EmailAttachment.kt`

Package `dev.inmo.wishlist.features.email.server.models` (new `models` package in the server module, mirroring `email/common`'s layout):

```kotlin
class EmailAttachment(
    val fileName: String,
    val mimeType: String = "application/octet-stream",
    val content: suspend () -> InputStream
)
```

KDoc must state the streaming contract explicitly: the provider MAY be invoked more than once and MUST return a fresh `InputStream` from the beginning of the content on each invocation; bytes are never buffered wholesale; the caller (Jakarta Mail via the bridge) closes the returned stream.

### Edit — `features/email/server/src/commonMain/kotlin/services/SmtpEmailService.kt`

1. Declaration becomes `class SmtpEmailService(private val config: EmailConfig) : EmailsService`; class KDoc updated to mention the `EmailsService` role.
2. Add the private suspend-to-blocking bridge: a private (nested or file-private) `DataSource` implementation over `EmailAttachment` — `getInputStream() = runBlocking { attachment.content() }` (fresh stream per call), `getOutputStream()` throws `UnsupportedOperationException`, `getContentType() = attachment.mimeType`, `getName() = attachment.fileName`. KDoc explains why `runBlocking` is safe (always invoked on the `Dispatchers.IO` worker inside `Transport.send`).
3. Add private helper `send(recipient, subject, failureLogLabel, fillContent: MimeMessage.() -> Unit): Boolean` centralizing: SMTP-disabled warn-log + `false`; `withContext(Dispatchers.IO)`; `buildSession(smtp)`; envelope (`setFrom`, `setRecipients`, `subject`); `fillContent()`; `Transport.send`; `runCatching`/`getOrElse` warn-log + `false`.
4. Implement the three `override` methods via the helper: `sendText` → `setText(text, "utf-8")`; `sendHtml` → `setContent(html, "text/html; charset=utf-8")`; `sendTextWithAttachments` → `MimeMultipart` (text `MimeBodyPart` + one `MimeBodyPart` per attachment: `DataHandler(bridge)`, `fileName`, `Part.ATTACHMENT` disposition) then `setContent(multipart)`.
5. Recommended (Architecture decides): re-route `sendTestEmail`'s body through the same helper — no signature/behavior change, removes duplication.
6. New imports: `jakarta.activation.DataHandler`, `jakarta.activation.DataSource`, `jakarta.mail.Part`, `jakarta.mail.internet.MimeBodyPart`, `jakarta.mail.internet.MimeMultipart`, `kotlinx.coroutines.runBlocking`, `java.io.InputStream`, plus the two new project types.

### Optional edit — `features/email/server/src/commonMain/kotlin/Plugin.kt` (Architecture decides)

Add `single<EmailsService> { get<SmtpEmailService>() }` after the existing `SmtpEmailService` registration, matching the repository's standard interface-binding pattern (`agents/CODING.md`, "Fill Plugin.kt"). Server-only DI line; adds no route, client, or DTO surface, so it stays inside the operator's approved scope. If Architecture opts for strict minimalism it may be dropped — consumers can still inject the concrete `SmtpEmailService`.

### Documentation — `features/email/README.md`

1. `## Models`: add rows for `EmailsService` (`email/server`; server-only send interface: `sendText` / `sendTextWithAttachments` / `sendHtml`, all with explicit `subject`, `Boolean` result, no HTTP exposure) and `EmailAttachment` (`email/server`; `fileName` + `mimeType` + `content: suspend () -> InputStream` streaming provider). Fix the stale `EmailFeature` row (now two variants: server interface with `callerId` params in `email/server`, client interface in `email/client`), fix the `SmtpEmailService` row (implements `EmailsService`, wrapped by `EmailFeatureService`), and add the missing `EmailFeatureService` row.
2. `## Architecture Notes`: new bullet documenting `EmailsService` as the internal server-side send surface (not wired to `EmailFeature`, `KtorEmailFeature`, or any route) and the attachment streaming design (suspend provider → private `DataSource` bridge with `runBlocking` on the IO dispatcher; fresh stream per `getInputStream()` call; no `ByteArray` buffering).
3. `## Operator Notes` is untouched.

### Verification expectations (for later roles)

- `./gradlew :wishlist.features.email.server:build` passes.
- `ast-index rebuild` after the `.kt` edits.
- Diff review confirms: no changes under `features/email/common`, `features/email/client`, `configurators/`, or any routing/DTO file; `EmailAttachment` content is never read into a `ByteArray`; all new declarations carry KDocs; no `else if`.

## Handoff

Next role: **Architecturing** (step `002-architecturing`). Discretionary decisions delegated: (a) Koin `EmailsService` binding in `Plugin.kt` — recommended yes; (b) `sendTestEmail` refactor onto the shared `send` helper — recommended yes; (c) exact placement of the `DataSource` bridge (nested private class vs. file-private class) and the helper's exact parameter list. Everything else above is fixed by operator answers or existing conventions.
