Model: GPT-5.6 Codex (LL; selected for Markdown documentation per `agents/SHORTCUTS.md`)
Changed files: README.md, agents/VERIFICATION.md, features/ui/wishlist/README.md, agents/task/20.09.2026_07.45.16-690c5d8d-8049-4561-a685-49f067bff3d9/024-coding.md

## Documentation update

Updated the wishlist feature Architecture Notes with the authentication gate for
caller-owned list loading, login and resume reload behavior, logout clearing, stale
response generation guards, and anonymous public owner browsing. The note records
that anonymous own-list startup makes no protected `getMyWishlists()` request and
that the served smoke test checks zero same-origin `GET /api/wishlist/getMy` calls.

Updated the root README and Verification guide from eight to six browser-gate tests:
two served smoke tests and four focused collector tests. Removed the obsolete
bootstrap 401 and transformation allowances and documented strict failure for every
console error, page exception, and HTTP 401.

The feature Operator Notes were preserved unchanged.

## Verification

`git diff --check` passed. Only the authorized feature README, root README,
Verification guide, and this step report were changed. The latest Coding report
records `./gradlew browserTest --console=plain` passing with six tests, zero failures,
and zero errors.
