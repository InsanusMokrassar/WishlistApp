Model: GPT-5.6 Codex (LL; selected for Markdown documentation per `agents/SHORTCUTS.md`)
Changed files: README.md, agents/VERIFICATION.md, agents/task/20.09.2026_07.45.16-690c5d8d-8049-4561-a685-49f067bff3d9/004-coding.md

## Documentation update

Updated the root README with the supported Linux/JDK baseline, Ubuntu Chromium
runtime prerequisites, the exact `./gradlew browserTest` command, headed mode,
Playwright 1.52.0 provisioning, temporary SQLite and upload isolation, cleanup
behavior, browser cache reuse, failure artifact locations, CI behavior, and the
distinction between `jsBrowserTest` and the served-application gate.

Updated `agents/VERIFICATION.md` so Web UI changes and Web-affecting server/API
changes require `./gradlew browserTest` alongside the existing build and KMP checks.
The instructions require the exact command and exit status, browser and test
counts, artifacts, setup limitations, and a FAIL result when a required gate fails
or cannot run. They also require a skip rationale for unrelated changes.

## Verification

Documentation-only changes were checked with `git diff --check`. No source,
configuration, workflow, or feature Operator Notes files were edited. The browser
gate itself was previously run successfully twice by Coding; this documentation
step did not rerun the five-minute project build.

## Handoff

The documentation is ready for review. The commit contains only the root README,
the Verification guide, and this step report.
