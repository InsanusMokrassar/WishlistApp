Model: gpt-6-astra (HL independent validation; gpt-5.6-luna LL report filling)
Changed files: agents/task/17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd/025-validating.md

# Independent validation result: FAIL

No Critical or High product defect was found. Validation found two Medium findings and two Low findings. Medium VEC-05 touches privacy/auth and data-integrity proof, so `agents/ORCHESTRATOR.md` requires a direct return to Coding; no Planning restart or operator question is required. VEC-05 and VEC-06 are at occurrence 2, with no three-cycle escalation. VEC-07 and VEC-08 are first occurrences.

## Findings

### VEC-05 — Medium, still open

At `features/ui/users/src/jvmTest/kotlin/ui/UserEditEmailRenderTest.kt:830`, the captured-IME cooldown proof starts with a clean current-address draft and never enters a different valid candidate. Zero PUT/POST still passes if cooldown admission is removed because the `UserEditViewModel` same-slot branch at line 930 returns without a write. Nullable `capturedImeAction?.invoke()` permits a missing action to pass. Fix by seeding a distinct valid dirty draft while unrestricted, asserting Save eligibility and a non-null production IME action, activating the deadline with Refresh, asserting that the draft survives, invoking the captured action, and asserting no PUT/POST.

The renderer retarget fixture at line 149 uses deadline 20000 with the real current clock, so no restriction is rendered; lines 197-205 do not assert pending-address or deadline absence. Fix with an injected clock, establish visible pending/deadline before retarget, hold the ViewModel scheduler, then require the pending value/widget and deadline/cooldown semantics to disappear while stale ViewModel metadata still exists.

At `features/ui/users/src/commonTest/kotlin/ui/UserEditViewModelEmailTest.kt:2863`, the skipped-POST final-reconciliation test increments `reads` during initial loading, so the first post-PUT GET already returns B plus C and exits the first reconciliation guard. Fix by scripting/resetting post-PUT reads so the first GET returns approved B with no pending and only the final GET adds C; assert exactly PUT, GET, GET and zero POST.

At `features/auth/server/src/commonTest/kotlin/services/AuthFeatureServiceTest.kt:933`, the promised independent real-repository auth proof instead uses `FakeUsersRepo`, manually seeded cache entries, and a cancelled cache scope. The real independent cache test is good, but the authenticated integration boundary is absent. Fix with a fixture-owned SQLite file, two independent Exposed repos and a live cache scope, warming the first cache, mutating the second through valid approval/expiry/pending transitions, and proving stale ordinary cache plus fresh authenticated `getUser`. These are proof defects, not observed product leaks; production code does not need redesign.

### VEC-07 — Medium, new

At `features/users/common/src/jvmTest/kotlin/repo/ExposedUsersRepoSqliteTest.kt:477`, worker startup and initial blocking assertions occur before cleanup `try/finally`, while the holder callback at line 463 waits indefinitely. Startup/observation failure can leave the release gate closed and create a cleanup race with a non-daemon worker. Existing `finally` lines 495-499 cannot retry-gate release or await the second worker after a failed first await. New ordering cases ignore Boolean bounded holder waits and discard holder exceptions.

PostgreSQL counterparts at `PostgresUsersRepoTest.kt:109` use unbounded holder waits; lines 135-137 can skip second-worker cleanup after a failed first assertion; observer line 384 has no JDBC socket/statement bound, so the outer clock cannot bound a stuck JDBC operation. Fix by protecting all starts with cleanup, bounding and checking every barrier, capturing every worker result, releasing gates unconditionally, joining every started worker before teardown, and applying finite JDBC/lock/statement bounds. This is test-only; the original engine-observed contention proof is fixed and no production locking bug is established.

### VEC-06 — Low, still open

`features/email/README.md:19` and line 33 still promise verification only for the exact current address although replacements target pending first; line 46 calls `expectedEmail` current-only; line 74 names `UsersRepo.update` instead of `setEmail`. `features/ui/users/README.md:77` describes email-and-approval-only feedback snapshots and says replacement approval resets, contradicting retained approved current and four-field snapshots; line 95 repeats the later approval reset. Fix the residual sentences to describe candidate priority, retained-current approval, complete lifecycle snapshots, and the actual mutation entry point. Rollout/recovery/admin 429 additions are otherwise correct; preserve Operator Notes.

### VEC-08 — Low, new report-format issue

At `agents/task/17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd/023-coding.md:81`, the structured handoff uses non-UUID `message_id=023-v-06-docs`; Reason lines 57-58 lack an explicit condition→action→result relation despite claiming `causal_chain_present=true` at line 89. Fix with a monotonic addendum superseding only the structured handoff, using a UUID and explicit causal relation, without rewriting 023. Normal prose in 025 requires no AML block.

## Prior dispositions

VEC-01 is closed: raw explicit-clear classification in `ExposedUsersRepo.kt:366` and complete lifecycle clear at `:383` fix no-pending clear; real SQLite/PostgreSQL/admin coverage includes rejection, exact expiry, and rollback.

VEC-02 is closed as a product defect: both save guards in `UserEditViewModel.kt:968` and `:996` compare the editable pending-or-current baseline; first-read/post-SMTP supersession is meaningful. The skipped-POST test correction belongs to VEC-05.

VEC-03 is closed: `EmailFeatureService.kt:108` and `:133` bind `AlreadyApproved` to `expectedEmail` and clean only the request link after SMTP.

VEC-04 is closed as the original vacuous-observation defect: `pg_stat_activity` plus `pg_blocking_pids` observes blocked UPDATE, Xerial `BusyHandler` observes native SQLite contention, and locked-clock and ordering coverage is present. VEC-07 separately records fixture cleanup.

VEC-05 is partially closed and remains Medium. VEC-06 is partially closed and remains Low.

## Reviewed range and checks

The full PR range was `a220b3d2f9f4224e09880022e3051c374bd69a6a` from `origin/master` through `1572e6419eb6873ed5b3974ac9be638a9c616f2e`: 110 files, 10,867 insertions, and 507 deletions. The correction range was `9c36362..1572e64`: 43 files, 2,536 insertions, and 107 deletions.

Validation reviewed PROMPT, Validation 014 baseline, roles 015-024, acceptance, and source, test, and documentation files. AST database `/tmp/wishlist-verification-final-ast.db` contains 812 files, 7,688 symbols, 31,636 references, and 49 modules. `git diff --check` passed. Independent read-only XML recount found ordinary 1,176 tests plus PostgreSQL 7, all with zero failures, errors, or skips; the recount was not a new execution and reused Verification 024. Independent byte comparison found all five email/users/admin/auth/UI Operator Notes matching `origin/master`. The worktree was clean; validation modified no product, test, schema, configuration, dependency, earlier report, database, or external state.

## Positive behavior and residual risks

No existing application frequency/count restriction was found. Root `emailChangeCooldown` defaults to zero, an explicit P1D sample is present, finite/nonnegative validation and upward fractional-millisecond conversion are present, and checked issuance is present; configuration affects future approvals. Persisted deadlines use the post-lock clock, equality allows, replay does not reissue, approved current remains while pending is replaced/promoted, and raw cross-slot ownership plus transactional rollback are preserved. Owner/root 429 occurs post-authentication and avoids denied-caller deadline disclosure. Public output is exactly id/username. `getByIdFresh` returns private state. UI separates current, pending, draft, and deadline and checks raw identity/target after suspension. No quota/history/resend-rate overbuild was introduced, and no prior V79 product regression was found.

Bounded production corrections follow 015/016. 019 corrected 018 metadata. 020-022 provide meaningful coverage but make overly broad closure claims. 023 improved contracts but left prose/report gaps. 024 accurately shows passing suites, not missing coverage. A singleton users lock serializes unrelated writes by design; participating writers only, with no mixed binaries or direct SQL lifecycle writes. Coordinated backup/deploy/forward repair is documented. Browser DOM, physical Android/IME, and live SMTP were not executed. Accepted B-C-B generation limitation and non-atomic SMTP/role/DB boundaries remain.

## Decision and handoff

No operator or environment blocker was found. Return directly to Coding for bounded test, documentation, and report corrections; rerun affected focused, platform, and PostgreSQL checks; then perform independent validation again. Do not push.
