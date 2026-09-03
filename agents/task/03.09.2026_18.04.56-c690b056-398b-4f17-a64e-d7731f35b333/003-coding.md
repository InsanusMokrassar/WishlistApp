Model: OpenAI GPT-5 (coding, ML)
Changed files: `features/common/common/src/jvmMain/kotlin/SQLExceptionExtensions.kt`; `features/common/common/src/jvmTest/kotlin/IsUniqueViolationTest.kt`; `features/common/common/build.gradle`; `features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt`; `features/users/common/src/jvmTest/kotlin/repo/IsUniqueViolationTest.kt` (deleted); `features/users/common/build.gradle`; `features/common/README.md`; `features/users/README.md`; `agents/task/03.09.2026_18.04.56-c690b056-398b-4f17-a64e-d7731f35b333/003-coding.md`

# Coding report

## Model rationale

The Coding role prioritizes an ML model. OpenAI GPT-5 was used at the ML tier, which is appropriate for a narrowly scoped cross-module JVM API relocation requiring exact preservation of exception-graph behavior, Gradle source-set ownership, and test coverage.

## Implementation

Created the public JVM `SQLException.isUniqueViolation()` extension in `features/common/common` and moved the implementation without semantic changes. The Users repository now imports that extension; its two `ExposedSQLException` catch blocks retain the original duplicate translation and rethrow behavior.

Moved the thirteen-case classifier suite into Common's JVM tests and removed the former Users copy. Common now owns Xerial through a JVM implementation dependency; Users retains Xerial only in `jvmTest` for the existing SQLite repository integration tests.

Updated both feature READMEs for the new ownership boundary. The `## Operator Notes` sections in Common and Users were read before edits and were not modified.

## Verification

`./gradlew :wishlist.features.common.common:jvmTest :wishlist.features.users.common:jvmTest` passed. The generated JVM XML reports confirm 13 passing Common classifier tests and 6 passing Users SQLite repository tests, both with zero failures and errors.

`./gradlew :wishlist.features.common.common:build :wishlist.features.users.common:build` passed. Existing Gradle deprecation, Android compile-SDK, and configuration-time JavaScript dependency-resolution warnings remain unrelated to this change.

Dependency inspection confirmed `org.xerial:sqlite-jdbc:3.53.4.0` on Common `jvmCompileClasspath`, no matching Xerial dependency on Users `jvmCompileClasspath`, and Xerial `3.53.4.0` on Users `jvmTestCompileClasspath`.

After source changes, `ast-index rebuild` completed successfully. Final references show one Common definition, thirteen Common JVM test calls, and two Users production calls. The Common public API lists `SQLException.isUniqueViolation`; the Users repository outline no longer contains a local definition. `git diff --check` passed.

## Coding handoff

ENTITY:
entity_id=sql_exception_unique_violation_extension; type=public_JVM_extension; state=implemented_and_verified

CONTEXT:

* task_id=03.09.2026_18.04.56-c690b056-398b-4f17-a64e-d7731f35b333; agent_id=coding; memory_ref=[002-architecturing.md,SQLExceptionExtensions.kt,ExposedUsersRepo.kt,IsUniqueViolationTest.kt]
* constraints=[public_visibility,common_common_jvmMain_ownership,exact_marker_matching,identity_cycle_safety,Operator_Notes_immutable,no_internal_feature_dependency]

ACTION:

1. action=move_extension; target=sql_exception_unique_violation_extension; params={source=features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt,destination=features/common/common/src/jvmMain/kotlin/SQLExceptionExtensions.kt,visibility=public,algorithm=preserved}
2. action=update_consumer; target=ExposedUsersRepo; params={import=dev.inmo.wishlist.features.common.common.isUniqueViolation,production_calls=2,catch_translation=preserved,local_definition=removed}
3. action=move_unit_suite; target=IsUniqueViolationTest; params={source=features/users/common/src/jvmTest/kotlin/repo/IsUniqueViolationTest.kt,destination=features/common/common/src/jvmTest/kotlin/IsUniqueViolationTest.kt,test_functions=13,source_package=removed,target_package=dev.inmo.wishlist.features.common.common}
4. action=move_dependency_ownership; target=libs.xerial.sql; params={common_jvmMain=implementation,users_jvmMain=absent,users_jvmTest=implementation,version=3.53.4.0}
5. action=update_documentation; target=[features/common/README.md,features/users/README.md]; params={ownership_documented=true,Operator_Notes_modified=false}

REASON:

* condition=database_classifier_is_reusable_JVM_utility_with_JDK_only_public_signature; requirement=features.common.common_public_ownership
* condition=Users_SQLite_integration_suite_imports_Xerial_types; requirement=users_jvmTest_direct_Xerial_dependency

EXPECTED RESULT:

* entity_id=sql_exception_unique_violation_extension; new_state=single_public_Common_definition; location=features/common/common/src/jvmMain/kotlin/SQLExceptionExtensions.kt
* entity_id=unique_violation_unit_suite; new_state=single_Common_JVM_13_case_suite; location=features/common/common/src/jvmTest/kotlin/IsUniqueViolationTest.kt
* entity_id=users_repository_consumer; new_state=Common_extension_import_with_preserved_exception_translation; location=features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt

VERIFICATION:

* check=focused_JVM_tests; expected={Common_classifier_tests=13_passed,Users_SQLite_repository_tests=6_passed,failures=0,errors=0}; actual={Common_classifier_tests=13_passed,Users_SQLite_repository_tests=6_passed,failures=0,errors=0}
* check=affected_module_builds; expected={Common=passed,Users=passed}; actual={Common=passed,Users=passed}
* check=dependency_boundary; expected={Common_jvmCompileClasspath=Xerial_3.53.4.0,Users_jvmCompileClasspath=Xerial_absent,Users_jvmTestCompileClasspath=Xerial_3.53.4.0}; actual={Common_jvmCompileClasspath=Xerial_3.53.4.0,Users_jvmCompileClasspath=Xerial_absent,Users_jvmTestCompileClasspath=Xerial_3.53.4.0}
* check=ast_index_boundary; expected={definitions_Common=1,definitions_Users=0,Common_test_calls=13,Users_production_calls=2,Common_public_API_contains_extension=true}; actual={definitions_Common=1,definitions_Users=0,Common_test_calls=13,Users_production_calls=2,Common_public_API_contains_extension=true}
* check=diff_hygiene; expected={git_diff_check=passed,Operator_Notes_modified=false}; actual={git_diff_check=passed,Operator_Notes_modified=false}

UNCERTAINTY:

* missing=none; ambiguity=none; operator_confirmation_required=false; untestable_functionality=false

REPETITION OF RESULT:

* entity_id=sql_exception_unique_violation_extension; stored_in=shared_step_file; status=implemented_and_verified

COMMUNICATION:

* sender=coding; receiver=verification; task_id=03.09.2026_18.04.56-c690b056-398b-4f17-a64e-d7731f35b333; message_id=coding-003-common-sql-exception-extension; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[task_id,sql_exception_unique_violation_extension,Common_JVM_API,Users_dependency_scope]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true
* causal_chain_present=true; ambiguity_detected=false; result_duplication_present=true
