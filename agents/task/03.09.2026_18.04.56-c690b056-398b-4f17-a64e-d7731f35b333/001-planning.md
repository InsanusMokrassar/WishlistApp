Model: OpenAI GPT-5 (planning, HL)
Changed files: agents/task/03.09.2026_18.04.56-c690b056-398b-4f17-a64e-d7731f35b333/001-planning.md

# Planning report

## Model rationale

Planning prioritizes an HL model, and the active OpenAI GPT-5 model satisfies that requirement. The task is small in file count but requires careful ownership, public-API, dependency-scope, and regression-test decisions across two Kotlin Multiplatform modules, so the HL choice is appropriate.

## Task understanding

The task requires replacing the Users-owned `internal fun SQLException.isUniqueViolation()` with a public JVM extension owned by `features/common/common`. The classifier's validated behavior must remain unchanged: PostgreSQL SQL state `23505` and Xerial's exact `SQLITE_CONSTRAINT_UNIQUE` and `SQLITE_CONSTRAINT_PRIMARYKEY` result codes are positive; generic JDBC code 19, message text, base SQLite constraints, and unrelated constraint codes remain negative. Cause and JDBC next-exception traversal must remain iterative, identity-based, and cycle-safe.

The relocation is an ownership and visibility change, not a behavior redesign. `ExposedUsersRepo` remains the current production consumer, but the extension becomes reusable by any JVM feature that already depends on `wishlist.features.common.common`. No route, schema, exception contract, or multiplatform common API should change.

## Repository evidence

The repository AST index identifies the only definition at `features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt:175`. Production calls are at lines 137 and 156 in the same file. The remaining thirteen direct usages are the focused classifier tests in `features/users/common/src/jvmTest/kotlin/repo/IsUniqueViolationTest.kt`; no other production module currently calls the extension.

`features/users/common` already declares an API dependency on `wishlist.features.common.common`, so moving the extension does not add an internal feature dependency or create a cycle. The Common Operator Notes explicitly require `features/common` not to depend on another internal feature; the proposed extension uses only JVM/JDBC and Xerial types and respects that constraint.

`features/common/common` currently has a `jvmMain` source set but no `jvmTest` sources. Its `jvmMain` dependencies include MicroUtils Exposed support but do not directly declare Xerial SQLite. `features/users/common` currently declares `libs.xerial.sql` in `jvmMain`, while both `IsUniqueViolationTest` and `ExposedUsersRepoSqliteTest` import Xerial types. Dependency scopes therefore need to move with implementation ownership while retaining an explicit Users JVM-test dependency for the integration tests.

Both relevant feature READMEs were read in full, including their Operator Notes. Neither contains an operator constraint that conflicts with this task. The Common README does not yet list the classifier as public JVM infrastructure, while the Users README currently describes the classifier as an implementation detail of `ExposedUsersRepo`.

The baseline command `./gradlew :wishlist.features.users.common:jvmTest :wishlist.features.common.common:jvmTest` succeeds. The Users tests are up to date, and the Common JVM test task currently reports `NO-SOURCE`.

The AST index was initially absent. `ast-index rebuild` succeeded after the required filesystem approval, and subsequent symbol, reference, module, and public-API queries were completed through the index.

## Open questions and answers

No operator questions remain. The prompt explicitly determines the destination module, JVM source set, public visibility, and symbol to replace. Package naming, source filename, dependency scope, test ownership, and documentation updates are implementation-architecture details that can be resolved consistently from repository conventions without changing operator intent.

## Proposed implementation plan

Architecture should place a new Kotlin file under `features/common/common/src/jvmMain/kotlin`, preferably in the existing `dev.inmo.wishlist.features.common.common` package so consumers receive the concise import `dev.inmo.wishlist.features.common.common.isUniqueViolation`. The file should expose `fun SQLException.isUniqueViolation(): Boolean` without an `internal` modifier and should carry KDoc describing supported markers, graph traversal, negative cases, and the Boolean return contract. The implementation body should be moved intact from `ExposedUsersRepo.kt`; semantic edits are outside this task unless regression evidence proves a relocation defect.

`ExposedUsersRepo.kt` should import the new public extension, retain both call sites, and remove only the classifier body plus imports used solely by that body: `SQLiteErrorCode`, `SQLiteException`, `ArrayDeque`, `Collections`, and `IdentityHashMap`. Its repository-level KDoc may continue linking to `isUniqueViolation` through the import. The catch and translation logic must remain unchanged so positive matches still wrap the original `ExposedSQLException` in `DuplicateUserFieldException`, and negative matches still rethrow the original exception.

The focused `IsUniqueViolationTest` suite should move from the Users JVM test source set into a new Common JVM test source set with its package updated to the public extension's package. Moving the existing thirteen cases, instead of duplicating them, gives the new public utility direct ownership coverage and prevents stale parallel suites. `ExposedUsersRepoSqliteTest` should remain in Users because it verifies repository translation, persistence state, and real SQLite behavior rather than the generic utility alone.

`features/common/common/build.gradle` should add `implementation libs.xerial.sql` to `jvmMain`, because the new implementation directly references Xerial classes but its public signature exposes only `java.sql.SQLException`. `features/users/common/build.gradle` should move its Xerial declaration from `jvmMain` to `jvmTest`: Users production code will no longer reference Xerial directly, while `ExposedUsersRepoSqliteTest` still requires Xerial types on its test compile classpath. Architecture should avoid making Xerial an `api` dependency unless Gradle compilation proves that broader exposure is required.

`features/common/README.md` should document `SQLException.isUniqueViolation` as public JVM-only common infrastructure and summarize its exact database markers and traversal guarantees. `features/users/README.md` should state that `ExposedUsersRepo` consumes the Common extension rather than owning the classifier. Both Operator Notes must remain byte-for-byte unchanged.

Verification should first run the moved Common classifier suite and the Users repository suite together, then build both affected modules. At minimum, run `./gradlew :wishlist.features.common.common:jvmTest :wishlist.features.users.common:jvmTest`, `./gradlew :wishlist.features.common.common:build :wishlist.features.users.common:build`, and the repository-wide gates required by the Verification role. Confirm the Common test task is no longer `NO-SOURCE`, all thirteen classifier cases execute once, all six real Users SQLite cases still pass, no old `internal` definition remains, and the public extension is discoverable in the Common JVM API. Rebuild `ast-index` after source changes as required by repository policy.

## Architecture handoff

ENTITY:
entity_id=sql_exception_unique_violation_extension; type=public_JVM_extension; state=planned_relocation_from_users_to_common

CONTEXT:

* task_id=03.09.2026_18.04.56-c690b056-398b-4f17-a64e-d7731f35b333; agent_id=planning; memory_ref=[PROMPT.md,features/common/README.md,features/users/README.md,ExposedUsersRepo.kt,IsUniqueViolationTest.kt]
* constraints=[public_visibility,common_common_jvmMain_ownership,behavior_preservation,no_internal_feature_dependency,operator_notes_immutable]; baseline=[users_jvmTest_pass,common_jvmTest_NO_SOURCE]

ACTION:

1. action=create_public_extension; target=sql_exception_unique_violation_extension; params={module=wishlist.features.common.common,source_set=jvmMain,receiver=java.sql.SQLException,name=isUniqueViolation,visibility=public}
2. action=move_classifier_implementation; target=sql_exception_unique_violation_extension; params={positive_markers=[PostgreSQL_23505,SQLITE_CONSTRAINT_UNIQUE,SQLITE_CONSTRAINT_PRIMARYKEY],graph_edges=[Throwable.cause,SQLException.nextException],visited_semantics=object_identity,cycle_safe=true}
3. action=replace_users_definition_with_import; target=ExposedUsersRepo; params={call_sites=[update,create],translation_contract=unchanged,obsolete_utility_imports=remove}
4. action=move_unit_tests; target=IsUniqueViolationTest; params={source_module=wishlist.features.users.common,target_module=wishlist.features.common.common,case_count=13,behavior_change=false}
5. action=adjust_dependency_scopes; target=Gradle_source_sets; params={common_jvmMain=implementation_libs.xerial.sql,users_jvmMain=remove_libs.xerial.sql,users_jvmTest=implementation_libs.xerial.sql}
6. action=update_feature_documentation; target=feature_READMEs; params={common_public_utility=add,users_shared_classifier_reference=add,operator_notes=preserve}

REASON:

* condition=classifier_is_cross_feature_database_utility_and_current_location_is_users_private; requirement=public_reusable_ownership_in_common_common_jvmMain_with_identical_runtime_semantics

EXPECTED RESULT:

* entity_id=sql_exception_unique_violation_extension; new_state=public_common_JVM_API_with_single_implementation_and_single_unit_suite; location=features/common/common/src/jvmMain/kotlin

VERIFICATION:

* check=focused_and_module_Gradle_gates; expected=[common_classifier_13_pass,users_repository_tests_pass,common_build_pass,users_build_pass]
* check=AST_index_public_API_and_references; expected=[one_definition_in_common_jvmMain,two_production_calls_in_ExposedUsersRepo,zero_internal_users_definition]

UNCERTAINTY:

* missing=none; ambiguity=none; architecture_freedom=[exact_filename,final_root_package_confirmation]

REPETITION OF RESULT:

* entity_id=sql_exception_unique_violation_extension; stored_in=shared_memory; status=available_for_architecture_handoff

COMMUNICATION:

* sender=planning; receiver=architecturing; task_id=03.09.2026_18.04.56-c690b056-398b-4f17-a64e-d7731f35b333; message_id=planning-001-public-unique-violation; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[task_id,sql_exception_unique_violation_extension,public_extension_relocation]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true
* causal_chain_present=true; ambiguity_detected=false; result_duplication_present=true
