# PROMPT

## Source operator prompt (verbatim)

> 1. Remove `isFeatureEnabled` from `SmtpEmailService`
> 2. In `EmailFeatureService` accept nullable `emailsService`
> 3. Make `smtp` in `EmailConfig` non nullable
> 4. In `dev.inmo.wishlist.features.email.server.Plugin` wrap config decode single with checking of email config field ("email"). If field is absent - `EmailConfig`'s single must not be invoked
> 5. `SmtpEmailService`'s (as well as `EmailsFeature`) single must be wrapped together with `EmailConfig` (that will guarantee that `EmailConfig` is available in DI)
> 6. Create `DisabledEmailsService` to stub realization of disabled feature
> 7. For `EmailFeature` use `getOrNull` for `emailsService` and if it returns null - return `DisabledEmailsService` from single instead

## Context (observed by Root before handoff to Planning)

- Branch: `fix/44-email`. Repo already has a completed prior task
  (`agents/task/09.07.2026_11.38.55-380dfc53-7d5e-4aab-92ec-a193e6549d23`) that added `EmailsService`
  (`sendText`/`sendTextWithAttachments`/`sendHtml`) implemented by `SmtpEmailService`, committed on this branch.
- There is **uncommitted, in-progress WIP** on this branch (not part of any task folder) that partially
  overlaps this new prompt:
  - `features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt` — constructor parameter
    renamed from `smtpEmailService: SmtpEmailService` to `emailsService: EmailsService` (non-nullable), and
    `sendTestEmail(...)` now calls `emailsService.sendText(...)` **and then also** `emailsService.sendTestEmail(recipient)`,
    returning the second call's result. `EmailsService` does NOT declare `isFeatureEnabled()` or `sendTestEmail(...)`
    (those exist only on the concrete `SmtpEmailService` class), so `EmailFeatureService.isFeatureEnabled()` and
    `sendTestEmail(...)` as currently written **will not compile** against the `EmailsService` interface type.
  - `features/email/server/src/commonMain/kotlin/Plugin.kt` — the `EmailFeatureService(...)` construction site
    changed from `get<SmtpEmailService>()` to `get()`.
  - This WIP is **not commented on or excluded** by the operator prompt above — Planning must decide whether to
    build on top of it, revert parts of it, or treat the operator's 7 points as superseding it entirely (points 1,
    2, 6, 7 strongly suggest superseding: `isFeatureEnabled` is being removed from `SmtpEmailService` altogether,
    and the disabled/enabled branching moves to a `DisabledEmailsService` no-op implementation selected at DI time
    instead of an `isFeatureEnabled(): Boolean` query method).
- Relevant current file contents Planning should read directly (not reproduced here in full): `EmailConfig.kt`
  (`smtp: SmtpConfig? = null`), `EmailsService.kt` (interface: `sendText`/`sendTextWithAttachments`/`sendHtml`,
  no `isFeatureEnabled`), `SmtpEmailService.kt` (has `isFeatureEnabled()` and `sendTestEmail()` as extra
  non-interface members, plus a private `send` skeleton keyed off `config.smtp` nullability), `EmailFeature.kt`
  (server interface, has its own `isFeatureEnabled(): Boolean` — unaffected by this prompt, remains as-is),
  `EmailFeatureService.kt`, `Plugin.kt` (all under `features/email/server/src/commonMain/kotlin/`).
- `features/email/README.md` has an empty `## Operator Notes` section — no standing constraints recorded there.

## Constraints (from `agents/*.md`, apply regardless of prompt content)

- Follow `agents/CODING.md`: KDocs on every new/changed declaration, `when` over `else if`, run the module build
  task after coding, update `features/email/README.md` (`## Models` / `## Architecture Notes`) to match, never
  touch `## Operator Notes`.
- Follow `agents/ARCHITECTURE.md`: produce test stubs/specs for every planned change before Coding; flag anything
  untestable to the operator before Coding starts.
- Stay on branch `fix/44-email`. Do not create a new branch, do not touch `master`.
- Run `ast-index rebuild` after `.kt` changes.
