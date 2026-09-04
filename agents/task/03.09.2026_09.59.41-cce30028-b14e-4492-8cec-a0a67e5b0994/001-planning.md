Model: OpenAI GPT-5 (HL)
Changed files: agents/task/03.09.2026_09.59.41-cce30028-b14e-4492-8cec-a0a67e5b0994/001-planning.md

Model rationale: The fix is small, but safe classification depends on JDBC exception-graph traversal, driver-specific extended result codes, and two resolved Exposed versions. The Planning role prefers an HL model, and the compatibility and negative-classification proof warrants that level.

# Planning report

## Task understanding

Fix only review finding 4 in the Users repository layer. SQLite UNIQUE and PRIMARY KEY failures must receive the existing `DuplicateUserFieldException` translation used by PostgreSQL SQL state `23505`; unrelated SQLite constraints and arbitrary JDBC error code 19 failures must continue escaping as their original `ExposedSQLException`. Public repository APIs, caller mappings, schema semantics, and PostgreSQL behavior remain unchanged.

The implementation must prove the production Exposed/Xerial wrapper path against a real in-memory SQLite database, not only against constructed `SQLException` values. The Users feature README must describe the expanded implementation behavior without changing Operator Notes.

## Current behavior and compatibility constraints

`ExposedUsersRepo.create` and `update` catch `ExposedSQLException`, call the internal `SQLException.isUniqueViolation`, and translate only a positive classification while retaining the caught exception as the cause. The classifier currently compares only the root SQL state with `23505`. Consequently, SQLite duplicate username and non-null email failures bypass `DuplicateUserFieldException`; existing Auth, Admin, and Email consumers cannot apply their established duplicate contracts and instead receive the raw database failure.

The repository schema has a unique username index and a nullable unique email index. Multiple null emails must remain valid, duplicate non-null values must remain rejected, failed creates must not add rows, failed updates must not mutate the prior row, and missing-id updates must still return null. No route, service, model, or exception signature needs modification.

## Resolved dependency and exception evidence

The version catalog already provides Xerial `sqlite-jdbc` 3.53.4.0 and Exposed 1.4.0. The server runtime resolves both versions, but the Users module's current `jvmTestCompileClasspath` has no Xerial driver and resolves Exposed 1.3.0 transitively through MicroUtils 0.30.1. Resolved Exposed 1.3.0 and 1.4.0 sources have the same relevant behavior: `BlockingExecutable.executeIn` catches the driver's `SQLException` and makes the driver exception the direct cause of `ExposedSQLException`; `ExposedSQLException` exposes only the immediate cause's SQL state and ordinary JDBC error code.

Resolved Xerial 3.53.4.0 sources show that `SQLiteException.resultCode` retains `SQLiteErrorCode`, while the superclass error code is constructed as `resultCode.code & 0xff`. NativeDB enables extended result codes and constructs `SQLiteException` from the full result returned by SQLite. A live in-memory reproduction with the resolved jar confirmed SQL state null and JDBC error code 19 for every tested constraint, with distinct result codes: UNIQUE 2067, PRIMARYKEY 1555, NOTNULL 1299, CHECK 275, and FOREIGNKEY 787. Therefore neither generic error code 19, base `SQLITE_CONSTRAINT`, nor exception-message text is a safe duplicate discriminator.

The existing focused command `./gradlew :wishlist.features.users.common:jvmTest` passes before the change. Existing tests cover only root PostgreSQL `23505`, unrelated `23503`, and absent SQL state, so the SQLite failure is currently untested. No dependency version change is required.

## Implementation plan

### Dependency boundary

Add `implementation libs.xerial.sql` to the Users common module's `jvmMain` source set. The classifier implementation needs the concrete `SQLiteException` and `SQLiteErrorCode` types, and the same dependency makes the driver available to `jvmTest`. Keep the dependency implementation-scoped because no public signature exposes Xerial types. Retain the server's explicit driver dependency and the existing catalog version; do not align or otherwise change Exposed versions as part of this fix.

### Classifier algorithm

Replace the root-only predicate with a small iterative traversal starting from the caught `SQLException`. Visit both `Throwable.cause` and, for every encountered `SQLException`, `SQLException.nextException`. Track visited `Throwable` objects by identity, not `equals`, so cause/next cross-links terminate and unusual exception subclasses with value equality cannot hide distinct nodes. An iterative worklist avoids recursion depth risk.

Return true when any visited `SQLException` has PostgreSQL SQL state `23505`, preserving the current contract even when a wrapper or batch exception carries the state below the root. Return true for a visited `SQLiteException` only when `resultCode` equals `SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE` or `SQLiteErrorCode.SQLITE_CONSTRAINT_PRIMARYKEY`. Return false after exhausting the graph. Do not inspect message text, ordinary `errorCode`, base `SQLITE_CONSTRAINT`, or masked numeric code 19.

Keep the create/update catch blocks and `DuplicateUserFieldException(cause = e)` construction unchanged. Exact non-unique SQLite failures must therefore rethrow the original outer `ExposedSQLException`, preserving diagnostics and current server-error behavior for genuine integrity/configuration faults.

### Focused classifier tests

Extend `IsUniqueViolationTest` with direct positive cases for Xerial UNIQUE and PRIMARYKEY, while retaining PostgreSQL `23505`. Add positive reachability cases through a generic cause chain and a `SQLException.nextException` chain. Add an exception graph with a cause/next cross-cycle to prove termination, plus distinct custom `SQLException` nodes that compare equal to prove identity-based visitation rather than equality-based deduplication.

Add negative cases for `SQLITE_CONSTRAINT`, NOTNULL, CHECK, FOREIGNKEY, and ROWID, along with a plain `SQLException` whose JDBC error code is 19. Retain unrelated and null SQL-state cases. These cases prove that only the two exact Xerial result constants broaden classification.

### Real SQLite repository regressions

Add a JVM test fixture that creates a uniquely named URI database such as `jdbc:sqlite:file:<uuid>?mode=memory&cache=shared`, opens one keeper JDBC connection for the fixture lifetime, and gives the same URL to Exposed. The keeper prevents the named in-memory database from disappearing between Exposed transactions. Always close/unregister the Exposed `Database` and close the keeper in `finally` to avoid global transaction-manager and native-connection leakage.

Construct the real `ExposedUsersRepo`, seed two valid users, and exercise both overridden write paths. Assert `DuplicateUserFieldException` for create collisions on username and non-null email and for update collisions on username and non-null email. After failures, assert no extra row exists and the updated target retains its original username/email. Include a control proving two users with null email can coexist. These tests traverse the actual `SQLiteException` direct cause inside `ExposedSQLException`, protecting the wrapper assumption observed in both resolved Exposed versions.

### Documentation

Update the Users README implementation notes and duplicate-field contract to name PostgreSQL `23505` and exact Xerial UNIQUE/PRIMARYKEY result codes. Document that cause and next-exception chains are inspected and that unrelated SQLite constraints are deliberately not translated. Leave Operator Notes unchanged.

## Verification and stop condition

Run `./gradlew :wishlist.features.users.common:jvmTest` first. Confirm the Users JVM test runtime resolves sqlite-jdbc 3.53.4.0 and that all classifier and live repository cases pass. Then run `./gradlew build` for cross-module API and consumer coverage, followed by `ast-index rebuild` after source changes. Stop when focused and full builds pass, PostgreSQL coverage remains green, the four live duplicate write cases translate correctly, null-email behavior remains valid, and every targeted negative constraint remains unclassified.

## Risks and operator questions

The main test-fixture risk is SQLite in-memory lifetime: a bare `jdbc:sqlite::memory:` URL creates a separate database per connection, so the named shared-memory URL and keeper connection are required. The main dependency risk is the Users test/runtime split between Exposed 1.3.0 and the server's resolved 1.4.0; inspected sources show identical wrapper behavior, and the real Users test covers 1.3.0 while the full server build covers 1.4.0 linkage. No migration, configuration, schema, or dependency-version risk is introduced.

No operator question remains. The prompt fixes the exact positive and negative classification boundary.

## Final implementation handoff

ENTITY:
entity_id=sqlite_unique_violation_classifier; type=repository_error_classifier; state=ready_for_architecture

CONTEXT:

* task_id=03.09.2026_09.59.41-cce30028-b14e-4492-8cec-a0a67e5b0994; agent_id=planning; memory_ref=[PROMPT.md,features/users/README.md,ExposedUsersRepo.kt,IsUniqueViolationTest.kt,resolved_Exposed_1.3.0_sources,resolved_Exposed_1.4.0_sources,resolved_Xerial_3.53.4.0_sources]
* constraints=[repository_layer_only,public_API_unchanged,PostgreSQL_23505_preserved,exact_Xerial_result_codes,identity_cycle_protection,real_in_memory_SQLite_tests]; exclusions=[generic_error_code_19,message_matching,schema_change,route_change,dependency_version_change,Operator_Notes_change]

ACTION:

1. action=add_internal_driver_dependency; target=features/users/common/build.gradle; params={source_set=jvmMain,configuration=implementation,alias=libs.xerial.sql,version_change=false}
2. action=traverse_exception_graph; target=SQLException.isUniqueViolation; params={algorithm=iterative_worklist,edges=[Throwable.cause,SQLException.nextException],visited=identity_based,cycle_safe=true}
3. action=preserve_PostgreSQL_match; target=SQLException.isUniqueViolation; params={sql_state=23505,node_scope=all_visited_SQLExceptions,result=true}
4. action=add_exact_SQLite_matches; target=SQLException.isUniqueViolation; params={exception_type=SQLiteException,result_codes=[SQLITE_CONSTRAINT_UNIQUE,SQLITE_CONSTRAINT_PRIMARYKEY],result=true}
5. action=reject_broad_SQLite_matches; target=SQLException.isUniqueViolation; params={rejected_inputs=[errorCode_19,SQLITE_CONSTRAINT,message_text,SQLITE_CONSTRAINT_NOTNULL,SQLITE_CONSTRAINT_CHECK,SQLITE_CONSTRAINT_FOREIGNKEY,SQLITE_CONSTRAINT_ROWID],result=false}
6. action=extend_classifier_regressions; target=IsUniqueViolationTest.kt; params={positive=[PostgreSQL_root,SQLite_UNIQUE,SQLite_PRIMARYKEY,cause_chain,nextException_chain],safety=[cause_next_cycle,equality_collision_identity],negative=[base_constraint,NOTNULL,CHECK,FOREIGNKEY,ROWID,plain_code_19,unrelated_sql_state,null_sql_state]}
7. action=add_live_repository_regressions; target=ExposedUsersRepoSqliteTest.kt; params={database=named_shared_in_memory_SQLite,lifetime=keeper_connection,cases=[create_duplicate_username,create_duplicate_non_null_email,update_duplicate_username,update_duplicate_non_null_email,multiple_null_emails],cleanup=[TransactionManager.closeAndUnregister,keeper_close]}
8. action=preserve_translation_boundary; target=ExposedUsersRepo.kt; params={caught_type=ExposedSQLException,duplicate_cause=outer_ExposedSQLException,non_unique_action=rethrow_original,missing_update_behavior=unchanged}
9. action=update_feature_documentation; target=features/users/README.md; params={topics=[PostgreSQL_23505,Xerial_exact_codes,exception_graph_traversal,negative_constraint_boundary],Operator_Notes=unchanged}
10. action=verify_and_stop; target=repository; params={commands=[./gradlew_:wishlist.features.users.common:jvmTest,./gradlew_build,ast-index_rebuild],stop_condition=focused_and_full_green_with_exact_positive_negative_boundary}

REASON:

* condition=ExposedSQLException_masks_Xerial_extended_result_as_errorCode_19; requirement=inspect_nested_SQLiteException.resultCode_without_broad_constraint_mapping
* condition=cause_and_nextException_graph_can_branch_or_cycle; requirement=iterative_identity_based_graph_traversal
* condition=existing_callers_depend_on_DuplicateUserFieldException; requirement=retain_repository_API_and_translation_cause

EXPECTED RESULT:

* entity_id=sqlite_unique_violation_classifier; new_state=PostgreSQL_and_SQLite_exact_unique_detection; location=ExposedUsersRepo.kt
* entity_id=sqlite_repository_regression_suite; new_state=actual_Exposed_Xerial_wrapper_path_verified; location=ExposedUsersRepoSqliteTest.kt
* entity_id=users_duplicate_contract; new_state=duplicate_username_and_non_null_email_consistently_translated; location=Users_repository_consumers

VERIFICATION:

* check=PostgreSQL_23505_root_or_nested; expected=DuplicateUserFieldException_classification_true
* check=Xerial_UNIQUE_or_PRIMARYKEY_in_cause_or_next_chain; expected=DuplicateUserFieldException_classification_true
* check=Xerial_non_unique_constraint_or_plain_errorCode_19; expected=classification_false_and_original_failure_preserved
* check=exception_graph_cycle_or_equal_distinct_nodes; expected=termination_and_identity_complete_search
* check=real_SQLite_create_and_update_duplicate_fields; expected=DuplicateUserFieldException_without_row_mutation
* check=real_SQLite_multiple_null_emails; expected=both_rows_created

UNCERTAINTY:

* missing=live_PostgreSQL_database_in_focused_suite; ambiguity=existing_constructed_23505_test_preserves_classifier_contract_without_driver_integration
* missing=Users_jvmTest_Exposed_1.4.0_runtime; ambiguity=server_full_build_checks_linkage_while_identical_resolved_1.3.0_and_1.4.0_wrapper_sources_reduce_shape_risk

REPETITION OF RESULT:

* entity_id=sqlite_unique_violation_classifier; stored_in=shared_memory; status=available_for_architecture

COMMUNICATION:

* sender=planning; receiver=architecture; task_id=03.09.2026_09.59.41-cce30028-b14e-4492-8cec-a0a67e5b0994; message_id=3c3f064a-55fd-4d76-a4f0-8cc2f6d9d4c1; protocol=AML-HIP

PERSISTENCE:

* local_memory=false; shared_memory=true; index_keys=[task_id,sqlite_unique_violation_classifier,sqlite_repository_regression_suite,users_duplicate_contract]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true
* causal_chain_present=true; ambiguity_detected=false; validation_scope=final_implementation_handoff
