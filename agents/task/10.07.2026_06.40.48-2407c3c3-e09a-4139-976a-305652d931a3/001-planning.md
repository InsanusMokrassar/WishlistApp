Model: Sonnet 5 (claude-sonnet-5)
Execution time: ~950
Tokens used: not exposed to this agent; session context at time of writing is roughly 90-100k tokens of reads/tool output
Changed files: agents/task/10.07.2026_06.40.48-2407c3c3-e09a-4139-976a-305652d931a3/001-planning.md

# 001 — Planning

## Model choice note

`agents/SHORTCUTS.md` sets the Planning role's model priority as `fable / opus / sonnet`. This
session was started as `Sonnet 5` by the invoking harness; there is no in-task mechanism available
to this agent to switch its own underlying model mid-run. Recording this as a deviation from the
stated priority for transparency, per the "choice of model must be argued" rule — it was not this
agent's choice.

## Tooling note

`ast-index` (v3.45.0) is installed and available on PATH (`/home/linuxbrew/.linuxbrew/bin/ast-index`)
— no installation attempt was needed. It was used for `outline`, `usages`, and `search` throughout
this investigation (see Investigation section). No grep fallback was required.

## Task understanding

Operator's verbatim 7-point request (from `PROMPT.md`), read together with the existing code, asks
for a redesign of how the email feature's SMTP-sending capability is wired into Koin so that
"disabled" (no SMTP configured) becomes a **DI-time** condition instead of a **runtime** boolean
check scattered through `SmtpEmailService`:

1. Remove `SmtpEmailService.isFeatureEnabled()` (the class-only member, not part of any interface).
2. `EmailFeatureService`'s constructor should accept a nullable `emailsService: EmailsService?`.
3. `EmailConfig.smtp` becomes non-nullable (`SmtpConfig`, not `SmtpConfig?`).
4. In `Plugin.kt`, the `EmailConfig`-decoding `single` should only be registered if an "email"
   config field is present in the server config JSON; if absent, `EmailConfig`'s `single` is never
   invoked at all.
5. `SmtpEmailService`'s single (and the `EmailsService` interface binding) must be registered
   together with `EmailConfig`'s single, in the same conditional — guaranteeing that whenever
   `SmtpEmailService`/`EmailsService` exist in the DI graph, `EmailConfig` also exists.
6. Add `DisabledEmailsService`, a no-op stub implementation of `EmailsService`.
7. Wherever `EmailFeature`'s single resolves `emailsService`, use `getOrNull<EmailsService>()`; if
   that is `null`, use `DisabledEmailsService` instead.

This is a coherent, well-motivated redesign: points 1+3+4+5 together move "is SMTP configured" from
a runtime `config.smtp != null` check (scattered across `SmtpEmailService.isFeatureEnabled()` and
its `send()` skeleton) to a DI-graph-shape fact — `EmailConfig`/`SmtpEmailService`/`EmailsService`
simply don't exist in Koin when SMTP isn't configured, and `EmailConfig.smtp` is guaranteed non-null
whenever it does exist. Point 6+7 replace the old boolean-driven no-op branches inside
`SmtpEmailService.send()` with a dedicated stub class selected at DI time.

However, turning this into literal, unambiguous file diffs surfaced four points where the operator's
wording admits multiple materially different implementations, at least one combination of which
would introduce a silent contract regression (`GET /email/enabled` always reporting `true`). Per
`agents/PLAN.md` step 4 ("do NOT skip this step even if the questions seem minor") and my
orchestrator's explicit instruction not to guess on points that materially affect the plan, these
are raised below instead of resolved by assumption.

## Investigation results

### Current code (read in full)

- `features/email/server/src/commonMain/kotlin/EmailConfig.kt` — `EmailConfig(val smtp: SmtpConfig? = null)`. KDoc explicitly documents "When `smtp` is `null` ... the feature operates in disabled/no-op mode."
- `features/email/server/src/commonMain/kotlin/EmailFeature.kt` — server capability interface: `isFeatureEnabled()`, `sendTestEmail(callerId, recipient)`, `setMyEmail(callerId, email?)`. Per the task's own context notes this interface is unaffected by the operator's 7 points and stays as-is; confirmed by re-reading it — nothing here needs to change.
- `features/email/server/src/commonMain/kotlin/EmailsService.kt` — server-only send surface: `sendText`, `sendTextWithAttachments`, `sendHtml`. **Does not declare `isFeatureEnabled()` or `sendTestEmail()`.**
- `features/email/server/src/commonMain/kotlin/services/SmtpEmailService.kt` — `class SmtpEmailService(private val config: EmailConfig) : EmailsService`. Has two extra, non-interface public members: `isFeatureEnabled()` (`config.smtp != null && config.smtp.host.isNotBlank()`) and `sendTestEmail(recipient)`. Every send path funnels through a private `send(recipient, subject, logLabel, fillContent)` skeleton that itself re-checks `config.smtp == null || smtp.host.isBlank()` and warn-logs + returns `false` in that case.
- `features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt` (currently **uncommitted WIP**, see below) — server `EmailFeature` impl wrapping an email-sender + `UsersRepo`.
- `features/email/server/src/commonMain/kotlin/Plugin.kt` (currently **uncommitted WIP**, see below) — registers `EmailConfig`, `SmtpEmailService`, `EmailsService`→`SmtpEmailService`, `EmailFeature`→`EmailFeatureService`, and the routing configurator, all unconditionally.
- `features/email/server/src/commonMain/kotlin/configurators/EmailRoutingsConfigurator.kt` — `GET /email/enabled` is public (outside `authenticate {}`), calls `feature.isFeatureEnabled()`. `PUT /email/myEmail` is self-service auth only, calls `feature.setMyEmail(...)` — **does not depend on SMTP at all**, per the README's explicit "Storage vs sending... independent" architecture note.
- `features/email/server/src/commonTest/kotlin/services/SmtpEmailServiceDisabledTest.kt` — 5 tests, ALL construct `SmtpEmailService(EmailConfig(smtp = null))` and one directly calls `service.isFeatureEnabled()`. Every one of these 5 call sites breaks under points 1+3 (non-nullable `smtp`, no `isFeatureEnabled()` on the class) and needs a full rewrite — "disabled mode" coverage has to move to wherever the disabled behavior now actually lives (`DisabledEmailsService` and/or `EmailFeatureService`'s null-handling).
- `features/email/README.md` — `## Operator Notes` is empty (no standing constraints). `## Architecture Notes` documents the current config-slice pattern (`EmailConfig` decoded via `get<Json>().decodeFromJsonElement(EmailConfig.serializer(), config)` against the **whole root config object**, same as `CurrencyConfig`), and states the sample config's `smtp` key lives flat at the JSON root alongside `openExchangeRatesAppId`. This section will need edits once the design is finalized.
- Confirmed via `ast-index usages EmailsService` / `SmtpEmailService` / `EmailFeatureService`: **no other feature or module references any of these three types.** All usages are internal to `features/email/server`. Blast radius of this change is contained to that module (plus its README and this task's own config files if the JSON schema changes — see Q1/Q2).

### Uncommitted WIP on this branch (git diff, not part of any task folder)

```
Plugin.kt:  single<EmailFeature> { EmailFeatureService(get<SmtpEmailService>(), get<UsersRepo>()) }
        →   single<EmailFeature> { EmailFeatureService(get(), get<UsersRepo>()) }

EmailFeatureService.kt:
  - constructor param smtpEmailService: SmtpEmailService  →  emailsService: EmailsService
  - isFeatureEnabled() = smtpEmailService.isFeatureEnabled()  →  emailsService.isFeatureEnabled()  [WON'T COMPILE — EmailsService has no isFeatureEnabled()]
  - sendTestEmail(...) now calls emailsService.sendText(...) AND THEN ALSO emailsService.sendTestEmail(recipient), returning the second call's result [WON'T COMPILE — EmailsService has no sendTestEmail(); also duplicates the send]
```

**Decision (not blocking, made now):** per the task context notes ("points 1, 2, 6, 7 strongly
suggest superseding") and my own reading of `EmailsService`'s actual surface, I am treating the
operator's 7 points as **fully superseding this WIP**, not building on top of it. Concretely: the
`emailsService.sendTestEmail(recipient)` call added by the WIP must be dropped entirely (the
interface has no such method and the double-send it introduces was never asked for by any of the 7
points) — `sendTestEmail` reverts to issuing exactly one `sendText(...)` call, as it did before the
WIP, adapted for the new nullable-`emailsService` handling. This is a implementation detail, not an
open question — flagged here only so Architecture/Coding know the WIP's second `send` call is
intentionally being discarded, not missed.

### Precedent search for "conditional `single` driven by config-field presence"

Searched (`ast-index search "decodeFromJsonElement"`, `ast-index usages`, targeted greps) for any
existing pattern in this repo where a feature's Koin `single` is conditionally registered based on
whether a JSON key is present in the root server config. **None exists.** The closest analog,
`features/currency/server/src/commonMain/kotlin/Plugin.kt`, decodes `CurrencyConfig` **unconditionally**
every time (`single { get<Json>().decodeFromJsonElement(CurrencyConfig.serializer(), config) }`) and
lets `OpenExchangeRatesService` handle a `null` app-id internally at call time — i.e. "disabled" stays
a **runtime** value-based check there, not a DI-graph-shape fact. So point 4/5 of this task introduce
a genuinely new pattern for this codebase; there is no existing precedent file to copy the exact
`JsonObject` field-presence-check idiom from.

Searched for `getOrNull` usage as a Koin `Scope` extension (confirms point 7's mechanism is idiomatic
here, not novel): confirmed real precedent in `features/common/server/src/jvmMain/kotlin/JVMPlugin.kt:127`
(`getOrNull<InternalApplicationRoutingConfigurator>() ?.let { ... }`), `:151`
(`getOrNull<ApplicationEngineFactory<...>>() ?: Netty`), and
`features/common/client/src/commonMain/kotlin/Plugin.kt:40` (`getOrNull<HttpClientEngineFactory<*>>()`).
All three existing uses follow the same shape point 7 asks for: `getOrNull<T>() ?: <fallback>`. Good
precedent for the *mechanism*; no precedent for the *field-presence-gated single registration* half
of the design (point 4/5).

Searched the tracked server config files for the actual JSON shape in play:
- `server/sample.config.json` (tracked, includes the email plugin in its `"plugins"` list) has a
  **flat, top-level** `"smtp": null` key — there is no `"email"` key anywhere in any tracked config
  file in this repo.
- `server/dev.config.json` (tracked) does not load the email plugin at all and has no `smtp`/`email`
  key.
- `server/local.config.json` is gitignored (`local.*` pattern), not inspected for secrets beyond
  confirming it's untracked.
- `Json { ignoreUnknownKeys = true }` is configured in `features/common/common/.../Plugin.kt:35-36`,
  which is *why* `EmailConfig`/`CurrencyConfig` can decode a narrow slice out of the wide root object
  today without error.

This confirms a concrete, checked-in file (`server/sample.config.json`) whose current content
(`"smtp": null`, present-but-null) directly interacts with the ambiguity in Q1/Q2 below — the exact
resolution changes whether that file needs an edit (and to what) to keep the server bootable.

## Open Questions — plan is BLOCKED pending operator answers

Per `agents/PLAN.md` step 4, none of these were skipped for being "minor" — each one changes actual
code (not just naming) in `Plugin.kt` and/or `EmailFeatureService.kt`, and at least one combination
of plausible readings (Q3, option "b" crossed with a literal reading of point 7) would silently
regress the documented `GET /email/enabled` contract. I did not guess past these.

**Q1 — What does "email config field ('email')" (point 4) refer to, literally?**
Today there is no `"email"` key anywhere in the config schema — `EmailConfig.smtp` is decoded from a
flat, top-level `"smtp"` key on the whole root JSON object (identical pattern to `CurrencyConfig`'s
`openExchangeRatesAppId`). The operator's point 4 names the field to check as `"email"` in quotes.
Two readings:
- (a) **Loose/colloquial** — "email config field" is shorthand for "the field(s) that configure the
  email feature," i.e. keep checking the existing `"smtp"` key; no schema change.
- (b) **Literal schema change** — introduce a new, nested top-level `"email"` JSON object (e.g.
  `"email": { "smtp": { "host": ..., ... } } }`) and decode `EmailConfig` from `config["email"]`
  specifically, not the whole root object. This is a real schema change requiring edits to
  `server/sample.config.json`, the README's "Sample config SMTP block" section, and any other
  tracked config file that references SMTP.

**Q2 — Does "field absent" (point 4) include "field present but `null`"?**
`server/sample.config.json` currently has `"smtp": null` — a **present** key with a `null` value, not
an absent key. Once `EmailConfig.smtp` is non-nullable (point 3), decoding `EmailConfig` from a JSON
element that is (or contains) `null` will throw `SerializationException` — there is no valid
non-nullable value to decode. Two readings:
- (a) Treat "present-with-null" the same as "absent" (skip registering the `single` in both cases).
  This matches today's documented no-op-on-null behavior and keeps `server/sample.config.json`
  bootable as-is.
- (b) Treat "absent" strictly as "JSON key literally missing" (`JsonObject` doesn't contain the key
  at all); a present `null` value is NOT treated as absent, so `server/sample.config.json`'s `"smtp":
  null` (or `"email": null`, depending on Q1's answer) would need to be edited to **remove the key
  entirely** as part of this change, otherwise the server crashes on startup with that config.

**Q3 — Does `EmailFeatureService.emailsService` ever actually receive `null` at runtime, or does
`DisabledEmailsService` always fill the gap — and if the latter, how does `isFeatureEnabled()` still
report `false` when SMTP is unconfigured?**
Point 2 asks for `EmailFeatureService`'s constructor param to be nullable (`EmailsService?`) — the
natural reason being that `isFeatureEnabled()` can no longer delegate to
`SmtpEmailService.isFeatureEnabled()` (removed by point 1) or to anything on the `EmailsService`
interface (which never declared that method), so `emailsService != null` becomes the obvious
replacement check. But point 7 says the `single` that builds `EmailFeature` should resolve
`emailsService` via `getOrNull<EmailsService>()` and substitute `DisabledEmailsService()` **instead**
of `null` when that call returns null. If point 7 is implemented literally at the
`EmailFeatureService`-construction call site, `EmailFeatureService.emailsService` is **never actually
null in production** — it's always either the real SMTP-backed service or the `DisabledEmailsService`
stub, both non-null — so `emailsService != null` would always be `true`, and `GET /email/enabled`
would incorrectly report `true` even with no SMTP configured. That is a regression against the
feature's own documented contract (`features/email/README.md`: "`GET /email/enabled` returns
`false`" when SMTP is absent). Concretely, which of these is intended?
- (a) `EmailFeatureService` keeps the nullable field (mainly for direct-construction unit
  testability), and the `single<EmailFeature>` wiring passes `getOrNull<EmailsService>()` straight
  through **without** substituting `DisabledEmailsService` at that call site — `isFeatureEnabled()` =
  `emailsService != null`. Under this reading, point 7's `DisabledEmailsService` substitution
  applies somewhere else (unclear where, given point 5 already wraps the only `EmailsService` single
  into the same conditional as `EmailConfig`/`SmtpEmailService` — there would be no second, always-on
  `EmailsService` binding for `DisabledEmailsService` to fill, since Koin does not support two
  competing unqualified registrations of the same type).
- (b) The `single<EmailFeature>` wiring does substitute `DisabledEmailsService` per point 7's literal
  wording, `EmailFeatureService.emailsService` is therefore practically never null, and
  `isFeatureEnabled()` needs a different signal than nullness (e.g. adding an `isEnabled: Boolean` — or
  similarly-named — member to the `EmailsService` interface itself, with `SmtpEmailService` always
  `true` and `DisabledEmailsService` always `false`). This is a real interface change beyond the
  literal 7 points and would need separate confirmation before Architecture specs it.
- (c) Some other resolution not covered above.

**Q4 — Is "EmailsFeature" in point 5 a typo for "EmailsService", and does the wrap explicitly
exclude the `EmailFeature`/`EmailFeatureService` single?**
Point 5: *"`SmtpEmailService`'s (as well as `EmailsFeature`) single must be wrapped together with
`EmailConfig`."* No type named `EmailsFeature` exists anywhere in this codebase — only `EmailFeature`
(the always-on capability interface covering both `sendTestEmail` and the SMTP-independent
`setMyEmail`) and `EmailsService` (the server-only send surface). My working assumption is that
"EmailsFeature" is a typo for "EmailsService" — i.e. point 5 wraps `SmtpEmailService`'s single AND
the `single<EmailsService> { get<SmtpEmailService>() }` binding together with `EmailConfig`'s single,
and explicitly does **not** touch the separate `single<EmailFeature> { EmailFeatureService(...) }`
registration, which must keep being registered unconditionally regardless of SMTP configuration —
otherwise `PUT /email/myEmail` (self-service email storage, which the README states is intentionally
independent of SMTP) would stop working entirely whenever SMTP is unconfigured, a clear regression.
Please confirm this reading is correct before Architecture locks in the `Plugin.kt` structure, since
the alternative (wrapping `EmailFeature`'s single itself into the same conditional) is a materially
different, and clearly wrong per the README's own architecture notes, outcome.

## Draft plan skeleton (non-blocking parts only — for Architecture's head start once Q1-Q4 are answered)

These parts of the change are unambiguous regardless of how Q1-Q4 resolve, and can be treated as
settled:

- **`features/email/server/src/commonMain/kotlin/services/SmtpEmailService.kt`**: delete the
  `isFeatureEnabled()` member (point 1). `SmtpEmailService`'s constructor keeps taking `EmailConfig`
  (or could be simplified to take `SmtpConfig` directly, now that any `SmtpEmailService` instance is
  only ever constructed inside a conditional block where SMTP is guaranteed configured — this is a
  free implementation choice for Architecture, not operator-specified). The private `send()`
  skeleton's `smtp == null` half of its guard becomes dead code once `EmailConfig.smtp` is
  non-nullable and should be removed; whether to keep the blank-host guard (`smtp.host.isBlank()`) is
  also a free implementation choice — nothing in the 7 points asks to remove that specific check, so
  the default plan is to keep it as the sole remaining no-op trigger inside `SmtpEmailService`.
- **`features/email/server/src/commonMain/kotlin/EmailConfig.kt`**: `smtp: SmtpConfig? = null` →
  `smtp: SmtpConfig` (point 3), KDoc updated to drop the "disabled/no-op mode" language (that concept
  moves entirely to DI-graph-shape / `DisabledEmailsService`).
- **New file `features/email/server/src/commonMain/kotlin/services/DisabledEmailsService.kt`**
  (point 6): `class DisabledEmailsService : EmailsService` (or `object`, Architecture's call) whose
  three methods are no-ops returning `false` — no warn-log needed (no misconfiguration to report;
  this state is an intentional, expected "email feature not enabled" condition, unlike
  `SmtpEmailService`'s error-path warn-logs). Needs full KDoc per `agents/CODING.md`.
- **`features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt`**: constructor
  param → `emailsService: EmailsService?` (point 2); `sendTestEmail` guards on `emailsService` being
  null (returns `false` immediately, mirroring the existing root-check-then-`return false` early-exit
  style) before calling `emailsService.sendText(...)` — dropping the WIP's erroneous second
  `sendTestEmail(recipient)` call entirely (see "Decision" note above); `setMyEmail` is untouched
  (never touches `emailsService`, matches Q4's reasoning that email storage stays SMTP-independent).
  `isFeatureEnabled()`'s exact body depends on Q3's answer.
- **`features/email/server/src/commonMain/kotlin/Plugin.kt`**: restructure `setupDI` so the
  `EmailConfig` + `SmtpEmailService` + `single<EmailsService>` trio is gated by the field-presence
  check from Q1/Q2, while `single<EmailFeature> { EmailFeatureService(...) }` and the routing
  configurator single stay unconditional (per Q4). Exact `JsonObject` field-check expression and
  whether `getOrNull<EmailsService>()`'s null case substitutes `DisabledEmailsService` at this call
  site depend on Q1-Q3.
- **`features/email/server/src/commonTest/kotlin/services/SmtpEmailServiceDisabledTest.kt`**: full
  rewrite required regardless of Q1-Q4 — all 5 existing tests construct
  `SmtpEmailService(EmailConfig(smtp = null))`, which won't compile once `smtp` is non-nullable, and
  one test calls the now-deleted `isFeatureEnabled()`. New coverage needs to verify: `SmtpEmailService`
  behavior when constructed with a valid, non-blank-host `SmtpConfig` (still needs a blank-host case
  if that guard is kept); `DisabledEmailsService`'s three methods return `false` and (for
  `sendTextWithAttachments`) never invoke the attachment content provider (mirroring the existing
  "must not touch attachment content" assertion style); and `EmailFeatureService`'s behavior when
  `emailsService` is `null` vs. present (exact assertions on `isFeatureEnabled()` depend on Q3).
- **`features/email/README.md`**: `## Architecture Notes` needs a rewrite of the config-slice
  paragraph (exact wording depends on Q1's schema answer) and the "Sample config SMTP block" example
  needs updating to show the new disabled-by-key-absence convention instead of `"smtp": null`. `##
  Models` table row for `EmailConfig` needs its "When smtp is null..." sentence replaced, and a new
  row added for `DisabledEmailsService`.
- **`server/sample.config.json`** (and possibly `server/dev.config.json`/`server/local.config.json` if
  they are later given an email/smtp block): needs an edit whose exact shape depends on Q1/Q2 — either
  no change (if Q2=(a) "present-null == absent" and Q1=(a) key stays `"smtp"`), or removing/renaming
  the key (if Q2=(b) and/or Q1=(b)).
- **Build verification**: after any Coding pass, run the module build task
  (`./gradlew :wishlist.features.email.server:build` or equivalent per `agents/CODING.md`) — not run
  by Planning (no source edits were made in this step; only the step report file was touched).

## Status

**BLOCKED pending operator answers to Q1, Q2, Q3, and Q4 above.** These are not stylistic
preferences — each answer changes the actual control flow of `Plugin.kt` and/or
`EmailFeatureService.kt`, and an unresolved combination (Q3) risks silently breaking the documented
`GET /email/enabled` contract if implemented on a guessed reading. No source file was modified in
this step; only this report was written and will be committed alone, per `agents/GIT.md`.
