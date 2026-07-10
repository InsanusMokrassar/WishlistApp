# PROMPT

## Source operator prompt (verbatim)

> update docs, commit and push

## Context (observed by Root before handoff to Planning)

- Branch: `fix/44-email`. This immediately follows the just-completed and already-pushed task
  `agents/task/10.07.2026_06.40.48-2407c3c3-e09a-4139-976a-305652d931a3` (email feature: moved
  SMTP-enabled/disabled state from a runtime `Boolean` check to DI-graph shape — `EmailConfig.smtp`
  non-nullable and decoded from a new nested `"email"` config key, `SmtpEmailService.isFeatureEnabled()`
  removed, new `DisabledEmailFeature` substituted at DI time, `server/sample.config.json` restructured).
  That task's own `features/email/README.md` updates (Overview/Models/Architecture Notes) were already
  made and committed as part of its Coding step (commit `5b81a9e`) and its Low-severity KDoc follow-up
  (commit `37bc0d1`). Everything from that task is pushed (`origin/fix/44-email` at `141e139`).
- The operator's request here is terse and does not specify which docs. Planning must investigate what
  documentation (if any) is now stale given the full set of changes made on this branch so far
  (`git log master..fix/44-email` / `git diff master...fix/44-email` for the full branch diff, not just
  the most recent task) and produce a concrete plan of exactly which files need which edits — or
  determine that `features/email/README.md` already fully covers everything and no further doc changes
  are needed, in which case say so plainly rather than inventing busywork.
- Candidates to check (not a prescriptive list — verify each, don't assume): `features/email/README.md`
  (verify it's fully current, not just "already touched"), root-level `README.md` (if it references the
  email feature, SMTP config, or per-feature config shape), any `docs/` folder, `server/README.md` or
  similar if config-schema documentation lives there, `AGENTS.md`/`agents/*.md` (only if something about
  the *process* itself changed — unlikely, out of scope unless Planning finds a concrete reason).

## Constraints (from `agents/*.md`, apply regardless of prompt content)

- Per `agents/SHORTCUTS.md`: "All fillings of documentations and other *.md files must be done with
  `haiku` agent." Any step whose output is primarily `.md` content should be executed at `haiku` model
  tier.
- Follow `agents/CODING.md`'s README-update conventions: never touch a feature's `## Operator Notes`
  section.
- Stay on branch `fix/44-email`. Do not create a new branch, do not touch `master`.
- The operator explicitly asked for commit AND push as part of this request (unlike the previous task,
  where push was Root/Orchestrator's own end-of-cycle action per `agents/GIT.md` — here the operator
  named it directly, so it is in scope regardless).
