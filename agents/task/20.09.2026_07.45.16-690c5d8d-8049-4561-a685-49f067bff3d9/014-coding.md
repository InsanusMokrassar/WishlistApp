Model: GPT-5.6 Codex (LL; selected for Markdown documentation per `agents/SHORTCUTS.md`)
Changed files: README.md, agents/VERIFICATION.md, agents/task/20.09.2026_07.45.16-690c5d8d-8049-4561-a685-49f067bff3d9/014-coding.md

## Documentation update

Updated the root README to describe the current five-test browser gate: two served
Chromium smoke tests and three focused browser-response classifier tests. The README
now also documents the pinned headless-launch cache health check, automatic repair of
empty or partial pinned caches, preservation of unrelated browser revisions, and
healthy-cache reuse.

Updated `agents/VERIFICATION.md` with the same five-test breakdown and the required
cache probe and repair reporting. The existing requirements for exact command and
exit status, browser and test counts, artifacts, setup limitations, skip rationale,
and FAIL on an unavailable required gate remain in force.

## Verification

`git diff --check` passed. Only the root README, Verification guide, and this step
report were changed. The latest Coding report records successful browser runs for a
healthy cache, empty cache, wrong-revision-only cache, and partial pinned cache, plus
the unrelated-task provisioning check.
