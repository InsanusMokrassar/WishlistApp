Model: gpt-5.6-terra (ML; Coding prioritizes ML before HL in agents/SHORTCUTS.md.)
Changed files: agents/task/17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd/019-coding.md

## Addendum

This report corrects only the AML-HIP structured handoff in 018. It supersedes that structured block; the VEC-04 implementation, verification results, and commit remain unchanged. No source, fixture, test, configuration, documentation, or Operator Notes file was modified.

ENTITY:
entity_id=VEC-04; type=validation_finding; state=closed

CONTEXT:

* task_id=17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd; agent_id=/root/coding_vec4; memory_ref=[014-validating.md,015-planning.md,016-architecturing.md,018-coding.md]
* constraints=[postgres_engine_observation,sqlite_busyhandler_observation,bounded_worker_cleanup,no_public_runtime_lock_api]; supersedes=018-coding.md:structured_block

ACTION:

1. action=replace_scheduling_assertion; target=PostgresUsersRepoTest; params={observer=pg_stat_activity+pg_blocking_pids,signals=[active,Lock,users_write_lock_UPDATE,holder_pid]}
2. action=install_native_busy_observer; target=SqliteUsersTestFixture; params={driver=Xerial_3.53.4.0,active_sql_proxy=true,retry_gate=bounded}
3. action=add_ordering_proofs; target=ExposedUsersRepoSqliteTest; params={tests=[blockedMutationUsesClockAfterLockAtExactExpiry,blockedApprovalIssuesDeadlineFromPostLockClock,replacementFirstMakesConcurrentApprovalStale,approvalFirstRejectsConcurrentReplacementDuringPositiveCooldown,zeroPolicyApprovalFirstAllowsConcurrentReplacement]}

REASON:

* condition=Thread_start_latch_does_not_prove_database_lock_attempt; requirement=engine_observed_blocking_before_holder_release; causal_chain=thread_start_latch→unproven_JDBC_lock_attempt→engine_observer_requirement→observed_blocking_proof

EXPECTED RESULT:

* entity_id=VEC-04; new_state=closed; location=019-coding.md

VERIFICATION:

* check=users_common_jvmTest; expected=tests=33;failures=0;errors=0;skipped=0
* check=postgresEmailLifecycleTest; expected=tests=7;failures=0;errors=0;skipped=0
* check=git_diff_check; expected=exit=0; commit_ref=9e027bc

UNCERTAINTY:

* missing=none; ambiguity=none; scope_expansion=false

REPETITION OF RESULT:

* entity_id=VEC-04; stored_in=shared_memory; status=available

COMMUNICATION:

* sender=/root/coding_vec4; receiver=/root; task_id=17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd; message_id=9482899f-5404-4c76-a910-194a5dd5b331; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd,VEC-04,engine-observed-contention]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
