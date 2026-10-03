Model: gpt-5.6-luna (LL report filling)
Changed files: features/users/common/src/jvmTest/kotlin/repo/SqliteUsersTestFixture.kt, features/users/common/src/jvmTest/kotlin/repo/ExposedUsersRepoSqliteTest.kt, features/users/common/src/jvmTest/kotlin/repo/PostgresUsersTestFixture.kt, features/email/README.md, agents/task/17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd/031-coding.md

# Coding report

VEC-06 and VEC-07 are closed. Implementation changes are limited to the three JVM test-fixture/test files and the email feature README; no production, schema, configuration, or dependency changes were made. No claims are made here about other vectors.

## VEC-07 SQLite

`SqliteBusyObservation.BusyHandler` now owns one absolute monotonic deadline, initialized from the first busy callback. After the release gate opens, callback retries return zero when the deadline is reached and cannot retry past the deadline. Deadline exhaustion is recorded and directly asserted by `sqliteBusyHandlerStopsRetryingAtAbsoluteDeadline`, which uses a direct 100 ms bound. SQL observation starts before `prepareStatement` and remains active through prepared-statement close, allowing native preparation and execution callbacks to identify the real `users_write_lock` UPDATE. All other contention cleanup remains daemon, finite, and unconditional.

The direct timeout test passed 1/1. The full `ExposedUsersRepoSqliteTest` class passed 29/29 with zero failures, errors, or skips in 15.996 seconds. Focused `compileKotlinJvm` exited 0.

## VEC-07 PostgreSQL

Outer generated-schema create/drop now use the bounded driver URL parameters `connectTimeout=5&loginTimeout=5&socketTimeout=25&tcpKeepAlive=true`, a daemon network-timeout executor with a 25-second bound, JDBC statement timeout 25 seconds, and server `statement_timeout` and `lock_timeout` of 25000 ms. Generated schema names are quoted. The primary test failure is retained; cleanup failure is suppressed onto the primary failure, or raised when no primary failure exists.

With `WISHLIST_POSTGRES_TEST_JDBC_URL=jdbc:postgresql://127.0.0.1:55432/postgres?user=aleksey`, forced `postgresEmailLifecycleTest` runs passed twice, both exit 0. The final second-run XML timestamp was 16:58:41 and reports 7/7 tests with zero failures, errors, or skips in 1.412 seconds. A task-only PostgreSQL restart was performed after stale-PID confirmation; only the `/tmp` cluster was used. Cleanup query reported `schemas=0` and `other_connections=0`; no named SQLite or PostgreSQL workers remained.

## VEC-06 documentation

`features/email/README.md` lines 19, 33, and 46 now state the exact lifecycle wording: verification targets the pending replacement first, otherwise the unapproved current address; an exact already-approved current address returns `AlreadyApproved` without delivery; and the latest approved current address remains retained until pending promotion. The approved current address is not used as a replacement target.

## Final checks

The rebuilt AST database `/tmp/wishlist-031-ast.db` contains 813 files, 7724 symbols, 31714 references, and 49 modules. The email README operator heading through `Overview` SHA matches `origin/master` `7d278b...a8505`. Semantic checks passed and `git diff --check` passed. No commit, staging, or push was performed.

## Handoff

ENTITY:
entity_id=VEC-06; type=documentation-correction; state=closed
entity_id=VEC-07; type=test-fixture-concurrency-hardening; state=closed

CONTEXT:

* task_id=17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd; agent_id=coding; memory_ref=[030-validating.md,031-coding.md]
* constraints=[changes limited to three JVM test files and features/email/README.md; no production/schema/configuration/dependency changes; no commit/stage/push]

ACTION:

1. action=correct-documentation; target=VEC-06; params={lines=[19,33,46], pending_replacement_first=true, fallback=unapproved_current, exact_approved_current_result=AlreadyApproved, approved_current_delivery=false, retained_current_until_promotion=true}
2. action=bound-native-retry; target=VEC-07; params={platform=SQLite, deadline=monotonic_absolute_from_first_callback, direct_timeout_ms=100, sql_observation=prepare_through_close, exhaustion=recorded_and_asserted}
3. action=bound-schema-lifecycle; target=VEC-07; params={platform=PostgreSQL, driver_bounds=[connectTimeout=5,loginTimeout=5,socketTimeout=25,tcpKeepAlive=true], network_timeout_s=25, jdbc_statement_timeout_s=25, server_statement_timeout_ms=25000, server_lock_timeout_ms=25000, quoted_schema=true, primary_failure_retained=true, cleanup_failure_suppressed=true}

REASON:

* condition=validation findings limited to VEC-06 wording and VEC-07 timeout boundaries; requirement=close VEC-06 and VEC-07 with direct evidence and preserve scope

EXPECTED RESULT:

* entity_id=VEC-06; new_state=closed; location=features/email/README.md
* entity_id=VEC-07; new_state=closed; location=features/users/common/src/jvmTest/kotlin/repo/SqliteUsersTestFixture.kt,features/users/common/src/jvmTest/kotlin/repo/ExposedUsersRepoSqliteTest.kt,features/users/common/src/jvmTest/kotlin/repo/PostgresUsersTestFixture.kt

VERIFICATION:

* check=sqlite_direct_timeout_test; expected=1/1_green
* check=sqlite_full_exposed_class; expected=29/29_green,failures=0,errors=0,skips=0,duration_s=15.996
* check=focused_compileKotlinJvm; expected=exit_0
* check=postgres_email_lifecycle_forced_runs; expected=2_runs_exit_0
* check=postgres_latest_xml; expected=7/7_green,failures=0,errors=0,skips=0,duration_s=1.412
* check=postgres_cleanup; expected=schemas=0,other_connections=0,named_workers=0
* check=ast_rebuild; expected=files=813,symbols=7724,references=31714,modules=49,path=/tmp/wishlist-031-ast.db
* check=readme_operator_heading_sha; expected=matches_origin_master=7d278b...a8505
* check=semantic_checks_and_diff_check; expected=passed

UNCERTAINTY:

* missing=none; ambiguity=none_for_VEC-06_and_VEC-07_closure

REPETITION OF RESULT:

* entity_id=VEC-06; stored_in=shared_memory; status=available; state=closed
* entity_id=VEC-07; stored_in=shared_memory; status=available; state=closed

COMMUNICATION:

* sender=coding; receiver=orchestrator; task_id=17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd; message_id=c2989bf5-b41e-41f4-82a6-86a5cc9a16d9; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[task_id, VEC-06, VEC-07, closure]

VALIDATION:

* format_valid=true
* no_pronouns=true
* entities_explicit=true
* high_density=true
* causal_chain_present=true
* ambiguity_detected=false
