Model: GPT-5.6 Codex (LL; selected for Markdown documentation per `agents/SHORTCUTS.md`)
Changed files: README.md, agents/task/20.09.2026_07.45.16-690c5d8d-8049-4561-a685-49f067bff3d9/025-coding.md

## Documentation update

Corrected the served Web browser section to report the current six-test gate: two
served Chromium smoke tests and four focused browser-response classifier tests.
Reviewed the complete root browser section; it contains no stale tolerated-error
language or other obsolete test-count wording.

## Verification

`git diff --check` passed. Only the root README and this step report were changed.
The latest Coding report records the six-test browser gate passing with zero failures
and zero errors.
