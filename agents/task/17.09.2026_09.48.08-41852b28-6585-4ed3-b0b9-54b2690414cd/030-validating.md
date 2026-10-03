Model: gpt-6-astra (HL independent validation, following HL-before-ML priority); gpt-5.6-luna (LL report filling)
Changed files: agents/task/17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd/030-validating.md

# Independent validation result: FAIL

No Critical or High product defect was found. VEC-07 is Medium at occurrence 2. The remaining VEC-06 documentation mismatch is occurrence 3 and has escalated from Low to Medium under the repeat rule. VEC-05 is resolved with no escalation. VEC-07 concerns data-integrity verification, so the required route is direct return to Coding; no Planning restart or operator question is required.

## VEC-07 — Medium, partially resolved, occurrence 2

At `features/users/common/src/jvmTest/kotlin/repo/ExposedUsersRepoSqliteTest.kt:1038`, cleanup releases holder and SQLite retry gates before joining workers. At `features/users/common/src/jvmTest/kotlin/repo/SqliteUsersTestFixture.kt:108`, `BusyHandler` returns 1 whenever `retryGate.await(15, SECONDS)` succeeds. After the one-shot gate opens, later awaits immediately succeed, so 15 seconds no longer bounds retries. `BusyHandler` replaces the URL busy timeout, and the contender can spin while the holder commits or stalls. Architecture 016 requires a callback-owned deadline and no unlimited busy spin. Fix by retaining unconditional gate release and engine observation while adding an absolute monotonic retry deadline that returns 0 at expiry and exposes timeout to assertions.

At `features/users/common/src/jvmTest/kotlin/repo/PostgresUsersTestFixture.kt:38` and `:45`, schema create/drop use raw environment JDBC URL/statements without bounded driver, network, or statement/lock settings. Setup or teardown can hang on a stalled connection or schema lock despite bounded inner workers. Fix by reusing finite connection and statement/lock bounds for fixture-owned setup and cleanup, while preserving the original failure if cleanup also fails. This is test robustness only; normal PostgreSQL runs passed twice.

The original worker-startup and skipped-cleanup defects are fixed: all 7 SQLite and 3 PostgreSQL worker cases are cleanup-protected; terminal outcomes are captured; gates are released; joins are attempted; interrupt/rejoin and termination are asserted; holder waits are finite; PostgreSQL worker/observer bounds are real; and engine observation remains intact. The finding is limited to retry and outer-fixture timeout boundaries.

## VEC-06 — Medium after repeat escalation, partially resolved, occurrence 3

At `features/email/README.md:19`, `:33`, and `:46`, fallback active-candidate wording describes the latest approved current address. First-time verification actually sends to an unapproved current address; an approved current address returns `AlreadyApproved` without delivery. The exact production references are `features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt:87`, `:106`, and `features/users/common/src/commonMain/kotlin/utils/UserEmailState.kt:7`, whose helper is `pendingEmail ?: email?.takeUnless { emailApproved }`. Fix the wording so delivery targets the pending replacement first, otherwise the unapproved current address; separately describe exact approved-current `AlreadyApproved` and preservation of the latest approved address during replacement.

The same target-document area was inaccurate in Validation 014 and 025, producing the third occurrence and Low-to-Medium escalation. No production change is required. `UsersRepo.setEmail`, promotion, four-field snapshot, retained-current approval, admin 429, and rollout/recovery documentation are fixed. All five Operator Notes remain unchanged.

## Dispositions

VEC-01 is closed: `ExposedUsersRepo.kt:366` performs raw explicit clear, `:383` performs atomic lifecycle clear, and no-op, rollback, and exact-expiry behavior are preserved.

VEC-02 is closed: `UserEditViewModel.kt:968` and `:996` use editable pending-or-current baselines.

VEC-03 is closed: `EmailFeatureService.kt:108` and `:131` use the exact expected address and request-local cleanup.

VEC-04 is closed for the original engine-observation finding; the remaining bound issue is only VEC-07.

VEC-05 is closed: renderer `:830` has a dirty draft, Save eligibility, a non-null IME action, an active deadline via refresh, draft retention, and zero PUT/POST; retarget `:133` uses separate schedulers, an injected clock, visible metadata, then stale metadata with hidden private UI; ViewModel `:2871` proves exact PUT/GET(B with no pending)/GET(B with pending C), with zero POST; and `AuthFeatureServiceSqliteTest.kt:39` uses two real independent SQLite repositories, a live cache, valid authentication, valid lifecycle, stale cache, fresh full `getUser`, and nested cleanup.

VEC-06 remains open only for wording. VEC-07 remains open only for timeout boundaries.

VEC-08 is closed: 028 is monotonic, UUID `b1fdc68a-7678-45c5-bae8-e63a6bc85b05` is valid v4, and explicit causal relations are present.

## Positive behavior

Correction changes are limited to tests, documentation, and reports; no production, schema, configuration, or dependency change, privacy leak, or product regression was found. Root cooldown, zero default, P1D configuration, retained approved address plus pending replacement, post-lock deadlines, clear and promotion, fresh private reads, and owner UI remain intact. No quota, history, or resend-rate overbuild was introduced.

## Reviewed range and checks

Reviewed range `ae31a7d..2dc425a02cead5123a22c4cf38ee23a7e408ec18` contains 12 files, 770 insertions, and 277 deletions. Validation reviewed 025, 026-029, the source diff, production and fixture tests, full READMEs, and earlier architecture and acceptance material for regression and closure only. AST database `/tmp/wishlist-verification-029-ast.db` contains 813 files, 7,698 symbols, 31,636 references, and 49 modules. `git diff --check ae31a7d..2dc425a` passed; the worktree was clean before this report. Operator Notes title-through-Overview hashes equal `origin/master` for email, users, admin, auth, and UI users.

The XML recount found exactly 202 suites and 1,176 tests, all with zero failures, errors, and skips; renderer has 15 tests, auth SQLite has 1, and Exposed SQLite has 28. Green UI, auth, users, full-build, and two PostgreSQL logs were inspected. No new test run was performed. The PostgreSQL count of 7 is reused from 029 because the clean build removed the separate XML; preserved logs show success.

## Residual risks and handoff

Browser DOM beyond KMP, physical Android IME, and live SMTP remain unexecuted. The singleton lock serializes unrelated participating writes. Coordinated versions, no mixed writers, backup, and forward repair remain required. The accepted same-address generation limitation and separate SMTP, role, and database boundaries remain unchanged.

No blocker was found. Apply only fixture timeout bounds and first-verification wording; run affected SQLite and PostgreSQL checks and independent validation. The full build remains valid because production is unchanged. Do not push.
