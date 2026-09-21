Model: GPT-5.6 Codex (LL; selected for Markdown documentation per `agents/SHORTCUTS.md`)
Changed files: README.md, agents/VERIFICATION.md, features/ui/wishlist/README.md, agents/task/20.09.2026_07.45.16-690c5d8d-8049-4561-a685-49f067bff3d9/036-coding.md

## Documentation update

Updated the browser documentation from six to eight tests: two served Chromium
smoke tests and six strict collector tests. The root README and Verification guide
now specify that the anonymous protected own-list assertion counts Playwright
`onRequest` events before navigation, so a request attempt that receives no response
still fails the zero-request check.

Updated the Verification guide and wishlist Architecture Notes to record that
non-cancellation list or name load failures are logged per trigger while the
long-lived authentication/resume collector remains active for later retries. The
existing strict console, page, and HTTP 401 failure rules and gate commands remain
unchanged. Operator Notes were preserved.

## Verification

`git diff --check` passed. Only the authorized documentation files were changed. The
latest Coding report records `./gradlew browserTest --console=plain` passing with
eight tests, zero failures, and zero errors.
