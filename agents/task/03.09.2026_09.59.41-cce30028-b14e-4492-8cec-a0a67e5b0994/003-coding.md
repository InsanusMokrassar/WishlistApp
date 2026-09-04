Model: OpenAI GPT-5.6 Terra (ML)
Changed files: features/users/README.md, features/users/common/build.gradle, features/users/common/src/commonMain/kotlin/repo/exceptions/DuplicateUserFieldException.kt, features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt, features/users/common/src/jvmTest/kotlin/repo/IsUniqueViolationTest.kt, features/users/common/src/jvmTest/kotlin/repo/ExposedUsersRepoSqliteTest.kt, agents/task/03.09.2026_09.59.41-cce30028-b14e-4492-8cec-a0a67e5b0994/003-coding.md

Model rationale: The Coding role prefers an ML model. Terra was sufficient for the bounded repository-classifier change, exact driver API use, real SQLite fixture, regression coverage, documentation, and repository-wide verification required by the architecture.

# Coding report

## Implementation

`ExposedUsersRepo` now recognizes duplicate-key failures from PostgreSQL SQL state `23505` and Xerial's exact `SQLITE_CONSTRAINT_UNIQUE` or `SQLITE_CONSTRAINT_PRIMARYKEY` result codes anywhere in the reachable exception graph. The classifier uses an iterative breadth-first traversal across throwable causes and JDBC next exceptions, with identity-based visitation to terminate cycles without conflating distinct exceptions whose equality implementations collide.

The implementation deliberately ignores ordinary JDBC error code 19, message text, base `SQLITE_CONSTRAINT`, and unrelated SQLite constraint result codes. Existing create and update catch behavior is preserved: a positive classification wraps the original outer `ExposedSQLException` in `DuplicateUserFieldException`, while a negative classification rethrows the same outer exception. No public API, schema, route, configuration, dependency version, or Operator Notes content changed.

The Users JVM source set now declares Xerial through the existing `libs.xerial.sql` alias as a private implementation dependency. Users README and KDoc describe the cross-database contract and its narrow negative boundary.

## Red-first reproduction

Before changing the classifier, I added only the JVM Xerial dependency and the keeper-backed duplicate-username SQLite regression. The focused command failed as expected: one test ran and one failed because `assertFailsWith<DuplicateUserFieldException>` received the outer `org.jetbrains.exposed.v1.exceptions.ExposedSQLException`. Its direct cause was `org.sqlite.SQLiteException` with null SQL state, ordinary JDBC error code 19, and extended result code `SQLITE_CONSTRAINT_UNIQUE` for `users.username`. This reproduced the reported HTTP-500-producing classification gap through the real Exposed/Xerial stack.

## Regression coverage

The classifier suite contains 13 tests covering root and nested PostgreSQL state, direct and nested SQLite exact codes, next-exception traversal, non-unique SQLite constraints, deceptive messages and generic code 19, a cause/next cross-cycle, and equality-colliding exception nodes. The real SQLite suite contains six tests covering duplicate username and email on create and update, unchanged repository state after each failed write, multiple null emails, retained exception causes, and a raw non-constraint database error. Together the new focused suites pass 19 tests; the complete Users common JVM suite passes 24 tests, including five pre-existing model tests.

## Verification

The focused classifier and SQLite repository tests passed. `:wishlist.features.users.common:jvmTest` passed all 24 tests. Dependency insight resolved `org.xerial:sqlite-jdbc:3.53.4.0` on `jvmTestRuntimeClasspath`. `:wishlist.features.users.common:build` passed 425 tasks. The repository `build` gate passed 4,459 tasks, and the separate `allTests` gate passed 1,912 tasks. `git diff --check` passed, and the required source-index rebuild indexed 1,392 files across 114 modules. Gradle and webpack emitted only the repository's existing deprecation and asset-size warnings.

## Verification handoff

ENTITY:
entity_id=sqlite_unique_violation_classifier; type=repository_error_classifier; state=implemented_and_coding_verified

CONTEXT:

* task_id=03.09.2026_09.59.41-cce30028-b14e-4492-8cec-a0a67e5b0994; agent_id=coding; memory_ref=[PROMPT.md,001-planning.md,002-architecturing.md,ExposedUsersRepo.kt,IsUniqueViolationTest.kt,ExposedUsersRepoSqliteTest.kt,features/users/README.md]
* constraints=[finding_4_only,repository_boundary,public_API_unchanged,PostgreSQL_23505_anywhere,exact_Xerial_codes,identity_cycle_safety]; exclusions=[generic_code_19,message_matching,base_constraint_matching,schema_change,configuration_change,Operator_Notes_change]

ACTION:

1. action=add_private_JVM_dependency; target=features/users/common/build.gradle; params={source_set=jvmMain,configuration=implementation,alias=libs.xerial.sql,resolved_version=3.53.4.0}
2. action=implement_exception_graph_BFS; target=SQLException.isUniqueViolation; params={worklist=ArrayDeque,visited=IdentityHashMap_backed_set,edge_order=[cause,nextException],recursion=false}
3. action=preserve_PostgreSQL_match; target=sqlite_unique_violation_classifier; params={node_type=SQLException,sql_state=23505,graph_scope=all_reachable,result=true}
4. action=add_exact_SQLite_matches; target=sqlite_unique_violation_classifier; params={node_type=SQLiteException,result_codes=[SQLITE_CONSTRAINT_UNIQUE,SQLITE_CONSTRAINT_PRIMARYKEY],result=true}
5. action=reject_broad_heuristics; target=sqlite_unique_violation_classifier; params={rejected=[errorCode_19,message_text,SQLITE_CONSTRAINT,NOTNULL,CHECK,FOREIGNKEY,ROWID],result=false}
6. action=preserve_exception_contract; target=ExposedUsersRepo_create_update; params={positive_cause=original_ExposedSQLException,negative_throw=original_ExposedSQLException,public_signature_change=false}
7. action=add_synthetic_regressions; target=IsUniqueViolationTest.kt; params={tests=13,cases=[root,nested_cause,nextException,cross_cycle,equality_collision,exact_positive,exact_negative]}
8. action=add_real_SQLite_regressions; target=ExposedUsersRepoSqliteTest.kt; params={tests=6,database=named_shared_memory,keeper=true,cases=[create_username,create_email,update_username,update_email,null_email,raw_error]}
9. action=document_contract; target=Users_feature_docs; params={files=[features/users/README.md,ExposedUsersRepo_KDoc,DuplicateUserFieldException_KDoc],Operator_Notes_change=false}
10. action=verify_repository; target=repository; params={gates=[focused_jvmTest,complete_users_jvmTest,dependencyInsight,users_build,root_build,allTests,diff_check,ast_index_rebuild]}

REASON:

* condition=Xerial_extended_constraint_codes_share_JDBC_errorCode_19; requirement=SQLiteException.resultCode_exact_matching; causal_chain=masked_vendor_code_to_exact_enum_to_safe_translation
* condition=exception_graph_contains_cause_and_next_edges_plus_cycles; requirement=deterministic_iterative_identity_traversal; causal_chain=graph_cycles_to_identity_visitation_to_termination
* condition=existing_consumers_depend_on_DuplicateUserFieldException; requirement=outer_ExposedSQLException_retention_plus_public_contract_preservation; causal_chain=repository_translation_to_existing_HTTP_409_handling

EXPECTED RESULT:

* entity_id=sqlite_unique_violation_classifier; new_state=exact_PostgreSQL_and_SQLite_unique_detection; location=features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt
* entity_id=sqlite_repository_regression_suite; new_state=create_update_translation_and_state_preservation_verified; location=features/users/common/src/jvmTest/kotlin/repo/ExposedUsersRepoSqliteTest.kt
* entity_id=users_duplicate_contract; new_state=cross_database_behavior_documented; location=[features/users/README.md,DuplicateUserFieldException.kt]

VERIFICATION:

* check=red_first_real_SQLite_duplicate_username; expected=old_classifier_throws_ExposedSQLException; observed=one_test_one_failure_plus_SQLITE_CONSTRAINT_UNIQUE
* check=focused_new_suites; expected=19_tests_zero_failures; observed=13_synthetic_plus_6_real
* check=complete_Users_JVM_suite; expected=24_tests_zero_failures; observed=24_tests_zero_failures
* check=Xerial_dependency_resolution; expected=version_3.53.4.0; observed=version_3.53.4.0
* check=module_and_repository_gates; expected=all_success; observed=[users_build_425_tasks,root_build_4459_tasks,allTests_1912_tasks]
* check=whitespace_and_source_index; expected=clean_diff_check_plus_successful_rebuild; observed=clean_diff_check_plus_1392_indexed_files

UNCERTAINTY:

* missing=live_PostgreSQL_fixture; ambiguity=existing_root_and_nested_23505_tests_preserve_PostgreSQL_classifier_contract
* missing=natural_Users_write_path_for_every_non_unique_SQLite_constraint; ambiguity=synthetic_exact_negative_matrix_plus_real_SQLITE_ERROR_cover_negative_boundary

REPETITION OF RESULT:

* entity_id=sqlite_unique_violation_classifier; stored_in=shared_memory; status=available_for_verification

COMMUNICATION:

* sender=coding; receiver=verification; task_id=03.09.2026_09.59.41-cce30028-b14e-4492-8cec-a0a67e5b0994; message_id=3f1bc226-3cd2-44b7-aa78-21d3139049fd; protocol=AML-HIP

PERSISTENCE:

* local_memory=false; shared_memory=true; index_keys=[task_id,sqlite_unique_violation_classifier,sqlite_repository_regression_suite,users_duplicate_contract]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true
* causal_chain_present=true; ambiguity_detected=false; validation_scope=verification_handoff
