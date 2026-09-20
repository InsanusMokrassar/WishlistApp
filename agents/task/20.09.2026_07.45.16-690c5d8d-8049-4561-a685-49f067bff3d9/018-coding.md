Model: GPT-5.6 Codex (LL; selected for Markdown documentation per `agents/SHORTCUTS.md`)
Changed files: README.md, agents/VERIFICATION.md, agents/task/20.09.2026_07.45.16-690c5d8d-8049-4561-a685-49f067bff3d9/018-coding.md

## Documentation update

Updated the root README and Verification guide from eight total browser-gate tests:
two served Chromium smoke tests and six focused browser-response classifier tests.
Documented the bounded console allowance: only observed exact same-origin
`GET /api/wishlist/getMy` 401 responses can authorize bootstrap diagnostics, generic
or unlocated 401 messages fail, and an unlocated transformation diagnostic consumes
at most one allowance per matching response.

Existing cache provisioning, artifact, CI, skip-rationale, and required-gate FAIL
instructions remain unchanged.

## Verification

`git diff --check` passed. Only the root README, Verification guide, and this step
report were changed. The latest Coding report records `./gradlew browserTest`
passing with eight tests, zero failures, and zero errors.
