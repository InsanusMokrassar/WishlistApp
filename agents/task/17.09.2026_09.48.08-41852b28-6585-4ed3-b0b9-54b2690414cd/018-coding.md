Model: gpt-5.6-terra (ML; Coding prioritizes ML before HL in agents/SHORTCUTS.md.)
Changed files: features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt; features/users/common/src/jvmTest/kotlin/repo/ExposedUsersRepoSqliteTest.kt; features/users/common/src/jvmTest/kotlin/repo/PostgresUsersRepoTest.kt; features/users/common/src/jvmTest/kotlin/repo/PostgresUsersTestFixture.kt; features/users/common/src/jvmTest/kotlin/repo/SqliteUsersTestFixture.kt; agents/task/17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd/018-coding.md

## Result

VEC-04 is closed without changing users lifecycle behavior, schema, API, routes, documentation, or UI.

The previous immediate-after-`Thread.start` assertions were replaced with database-engine observation. PostgreSQL worker setup captures physical backend PIDs. A separate autocommit observer now requires the contender to be active on an `UPDATE` identifying `users_write_lock`, waiting on a PostgreSQL Lock, and blocked by the holder PID according to `pg_blocking_pids` before the holder gate is opened. The original current-address, pending-versus-current, and unrelated-writer concurrency proofs now assert that the post-lock callback is unreached during that observed interval and join both workers before schema cleanup.

SQLite now uses independent file-backed connections with an installed Xerial `BusyHandler`. A minimal test-local `Connection`/`PreparedStatement` proxy marks SQL only across JDBC execution and preserves original JDBC exceptions. The callback accepts only an active `users_write_lock` `UPDATE`, blocks retry until the holder has completed, and never performs JDBC work itself. Existing SQLite contention tests use this observed native busy interval rather than a start latch; all release paths open every gate, await both workers, and then permit the callback retry.

The internal three-argument repository constructor is now the only friend-test hook carrying the post-lock callback. The public two-argument constructor remains production-compatible; no public runtime lock API was added.

Added SQLite engine-observed ordering proofs:

- `blockedMutationUsesClockAfterLockAtExactExpiry` starts the clear before expiry, waits at the lock, advances to exact expiry, then proves the clear and raw-address release.
- `blockedApprovalIssuesDeadlineFromPostLockClock` proves the persisted deadline is calculated from the clock after contention release.
- `replacementFirstMakesConcurrentApprovalStale` proves replacement C wins and approval B returns null, with raw cross-slot ownership checks.
- `approvalFirstRejectsConcurrentReplacementDuringPositiveCooldown` proves B approval issues the exact deadline and blocked C is classified as `EmailChangeCooldownException` without a raw claim.
- `zeroPolicyApprovalFirstAllowsConcurrentReplacement` proves B approval with zero policy leaves no deadline and blocked C becomes the pending candidate.

Timeout/BUSY infrastructure-classification and additive migration/reopen tests remain unchanged and passing.

## Verification

- `./gradlew --no-parallel :wishlist.features.users.common:jvmTest` passed: 33 tests, 0 failures, 0 errors, 0 skipped.
- `WISHLIST_POSTGRES_TEST_JDBC_URL='jdbc:postgresql://127.0.0.1:55432/postgres?user=aleksey' ./gradlew --no-parallel --rerun-tasks :wishlist.features.users.common:postgresEmailLifecycleTest` passed: 7 tests, 0 failures, 0 errors, 0 skipped.
- The first PostgreSQL command hit the sandbox's read-only Gradle wrapper lock; the required scoped rerun was approved and passed against only the supplied disposable PostgreSQL schema.
- Rebuilt `ast-index` using `XDG_CACHE_HOME=/tmp/wishlist-vec4-ast`: 812 files, 49 modules.
- `git diff --check` passed.

## Remaining findings

VEC-05 and VEC-06 remain open. This step intentionally did not change their UI, service, authorization, privacy, configuration, README, or KDoc scope.

ENTITY:
entity_id=VEC-04; type=validation_finding; state=closed

CONTEXT:

* task_id=17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd; agent_id=/root/coding_vec4; memory_ref=[014-validating.md,015-planning.md,016-architecturing.md]
* constraints=[postgres_engine_observation,sqlite_busyhandler_observation,bounded_worker_cleanup,no_public_runtime_lock_api]

ACTION:

1. action=replace_scheduling_assertion; target=PostgresUsersRepoTest; params={observer=pg_stat_activity+pg_blocking_pids,signals=[active,Lock,users_write_lock_UPDATE,holder_pid]}
2. action=install_native_busy_observer; target=SqliteUsersTestFixture; params={driver=Xerial_3.53.4.0,active_sql_proxy=true,retry_gate=bounded}
3. action=add_ordering_proofs; target=ExposedUsersRepoSqliteTest; params={tests=[blockedMutationUsesClockAfterLockAtExactExpiry,blockedApprovalIssuesDeadlineFromPostLockClock,replacementFirstMakesConcurrentApprovalStale,approvalFirstRejectsConcurrentReplacementDuringPositiveCooldown,zeroPolicyApprovalFirstAllowsConcurrentReplacement]}

REASON:

* condition=Thread_start_latch_does_not_prove_database_lock_attempt; requirement=engine_observed_blocking_before_holder_release

EXPECTED RESULT:

* entity_id=VEC-04; new_state=closed; location=018-coding.md

VERIFICATION:

* check=users_common_jvmTest; expected=tests=33;failures=0;errors=0;skipped=0
* check=postgresEmailLifecycleTest; expected=tests=7;failures=0;errors=0;skipped=0
* check=git_diff_check; expected=exit=0

UNCERTAINTY:

* missing=none; ambiguity=none

REPETITION OF RESULT:

* entity_id=VEC-04; stored_in=shared_memory; status=available

COMMUNICATION:

* sender=/root/coding_vec4; receiver=/root; task_id=17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd; message_id=0a126716-0094-4f19-9015-882625dcff12; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd,VEC-04,engine-observed-contention]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
