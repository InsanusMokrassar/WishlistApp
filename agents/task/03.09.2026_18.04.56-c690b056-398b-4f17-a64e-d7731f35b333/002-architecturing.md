Model: OpenAI GPT-5 (architecturing, HL)
Changed files: agents/task/03.09.2026_18.04.56-c690b056-398b-4f17-a64e-d7731f35b333/002-architecturing.md

# Architecturing report

## Model rationale

The Architecture role prioritizes an HL model, and the active OpenAI GPT-5 model satisfies that requirement. Although the implementation is a small relocation, correctness depends on preserving a public JVM API boundary, exception-graph identity semantics, exact Xerial result-code matching, Kotlin Multiplatform dependency scopes, and cross-module regression coverage.

## Architectural outcome

Move `SQLException.isUniqueViolation()` unchanged from the Users repository file into a dedicated public JVM utility owned by `features/common/common`. The destination is `features/common/common/src/jvmMain/kotlin/SQLExceptionExtensions.kt`, in package `dev.inmo.wishlist.features.common.common`. The extension has no visibility modifier, making `fun SQLException.isUniqueViolation(): Boolean` public while keeping every Xerial type out of the function signature.

`ExposedUsersRepo` remains the only production consumer. Its two catch blocks continue to call the same extension name and retain the same exception translation: a positive classification wraps the original `ExposedSQLException` in `DuplicateUserFieldException`, while a negative classification rethrows the same `ExposedSQLException` instance. No route, persistence schema, repository interface, service contract, configuration, or common multiplatform API changes.

All planned behavior is covered by automated JVM tests. No platform-rendering or external-service behavior is involved, so the Architecture step has no untestable item and requires no operator confirmation before Coding. The Common and Users Operator Notes were read in full; both sections are empty placeholders and must remain byte-for-byte unchanged.

## Repository and dependency evidence

The rebuilt AST index reports one current definition in `features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt`, two production calls in that file, and thirteen direct assertions in `IsUniqueViolationTest`. The Common JVM public API currently contains no SQL exception helper. `features/users/common` already exposes an API dependency on `features/common/common`, so the relocation introduces no module edge and respects the Common rule prohibiting dependencies on other internal features.

Gradle dependency inspection confirms that `features/common/common` currently has no Xerial artifact on `jvmCompileClasspath`, while `features/users/common` receives `org.xerial:sqlite-jdbc:3.53.4.0` on `jvmTestCompileClasspath` from its current `jvmMain` declaration. Because the relocated implementation directly references `SQLiteException` and `SQLiteErrorCode`, Common must own the Xerial production dependency. Because the retained Users integration suite directly imports those Xerial types, Users must retain Xerial only as an explicit test dependency.

The AST index initially had no readable database in the sandbox. The required `ast-index rebuild` completed successfully with filesystem approval, indexing 1,392 files and 114 modules; subsequent definition, usage, outline, import, module-map, and public-API checks used that index.

## Source design

Create `features/common/common/src/jvmMain/kotlin/SQLExceptionExtensions.kt` with package `dev.inmo.wishlist.features.common.common`. Move the current implementation and its KDoc into this file without semantic edits. The imports are `org.sqlite.SQLiteErrorCode`, `org.sqlite.SQLiteException`, `java.sql.SQLException`, `java.util.ArrayDeque`, `java.util.Collections`, and `java.util.IdentityHashMap`.

The function contract remains exact. Every reachable `SQLException` whose SQL state is `23505` is positive. Every reachable Xerial `SQLiteException` whose `resultCode` is exactly `SQLITE_CONSTRAINT_UNIQUE` or `SQLITE_CONSTRAINT_PRIMARYKEY` is positive. Generic JDBC vendor code 19, exception message text, base `SQLITE_CONSTRAINT`, and unrelated SQLite constraint codes remain negative.

Traversal remains iterative over a queue of `Throwable` nodes. Each inspected node enqueues `cause`; each inspected `SQLException` additionally enqueues `nextException`. A set backed by `IdentityHashMap<Throwable, Boolean>` marks object identity, not value equality, so cause/next cycles terminate and distinct exceptions with colliding `equals` implementations remain independently visible. Preserve the current cause-before-next queue order and the current mark-on-removal behavior.

The implementation shape to preserve is:

```kotlin
package dev.inmo.wishlist.features.common.common

// Imports listed above.

public fun SQLException.isUniqueViolation(): Boolean {
    val pending = ArrayDeque<Throwable>()
    val visited = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
    pending.addLast(this)

    while (pending.isNotEmpty()) {
        val current = pending.removeFirst()
        if (!visited.add(current)) continue

        if (current is SQLException) {
            if (current.sqlState == "23505") return true
            if (current is SQLiteException) {
                when (current.resultCode) {
                    SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE,
                    SQLiteErrorCode.SQLITE_CONSTRAINT_PRIMARYKEY -> return true
                    else -> Unit
                }
            }
        }

        current.cause?.let(pending::addLast)
        if (current is SQLException) {
            current.nextException?.let(pending::addLast)
        }
    }

    return false
}
```

Kotlin's explicit `public` keyword is optional. Coding should omit the keyword to match repository style unless the public visibility requirement benefits from making the declaration visually explicit; either spelling has the same public JVM API. The KDoc must describe exact supported markers, cause and next-exception traversal, identity-based cycle safety, deliberately rejected heuristics, and the Boolean return contract.

## Users consumer changes

In `features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt`, import `dev.inmo.wishlist.features.common.common.isUniqueViolation`. Delete only the local extension declaration and remove the imports used exclusively by that declaration: `SQLiteErrorCode`, `SQLiteException`, `SQLException`, `ArrayDeque`, `Collections`, and `IdentityHashMap`.

Do not alter either `catch (e: ExposedSQLException)` block. The class-level KDoc link to `[isUniqueViolation]` resolves through the new import and should continue describing the same duplicate-field behavior. No change is needed in `DuplicateUserFieldException`, because the relocation changes ownership and visibility rather than the exception contract.

## Dependency changes

In `features/common/common/build.gradle`, add `implementation libs.xerial.sql` inside the existing `jvmMain.dependencies` block. `implementation` is required rather than `api`: the function's public signature contains only `java.sql.SQLException` and `Boolean`, so consumers do not need Xerial on their compile API surface.

In `features/users/common/build.gradle`, remove `implementation libs.xerial.sql` from `jvmMain` and add the same declaration under `jvmTest.dependencies`. Users production no longer references Xerial directly. `ExposedUsersRepoSqliteTest` still imports `SQLiteException` and `SQLiteErrorCode`, so the test source set must declare its own direct dependency rather than relying on Common's non-exported implementation dependency.

No explicit Common `jvmTest` dependency is needed. Kotlin JVM tests compile with their module's `jvmMain` implementation dependencies, as demonstrated by the current Users source-set arrangement. If Gradle unexpectedly rejects the moved Common test imports, adding the same alias to Common `jvmTest` is an acceptable build-only fallback, but Coding must first record the actual compiler failure; dependency duplication is not the intended design.

## Test relocation and specifications

Move `features/users/common/src/jvmTest/kotlin/repo/IsUniqueViolationTest.kt` to `features/common/common/src/jvmTest/kotlin/IsUniqueViolationTest.kt`. Change the package to `dev.inmo.wishlist.features.common.common` and otherwise retain the fixture and all thirteen test functions. Moving instead of copying establishes one authoritative unit suite beside the public utility and prevents duplicated behavior specifications.

The direct PostgreSQL specifications remain: SQL state `23505` returns true; unrelated state `23503` returns false; and a null SQL state returns false. Inputs use plain `SQLException` values so these cases do not depend on a live database.

The direct SQLite specifications remain: `SQLITE_CONSTRAINT_UNIQUE` returns true; `SQLITE_CONSTRAINT_PRIMARYKEY` returns true; base `SQLITE_CONSTRAINT`, NOTNULL, CHECK, FOREIGNKEY, and ROWID return false; plain JDBC code 19 with a UNIQUE-looking message returns false; and a Xerial base constraint with a UNIQUE-looking message returns false.

The graph specifications remain: PostgreSQL `23505` below a generic cause wrapper returns true; SQLite UNIQUE below a generic cause wrapper returns true; SQLite PRIMARYKEY reachable only through `nextException` returns true; a cross-edge cause/next cycle terminates and returns false; and distinct equality-colliding SQL exceptions are both visited, allowing the positive node to return true.

Retain `features/users/common/src/jvmTest/kotlin/repo/ExposedUsersRepoSqliteTest.kt` unchanged. Its six tests are the consumer-level and public-visibility regressions: duplicate username and email creation translate to `DuplicateUserFieldException`; duplicate username and email updates translate without mutating stored rows; multiple null emails remain valid; and a non-unique database failure remains the original `ExposedSQLException`. Successful compilation of `ExposedUsersRepo` against an extension in another Gradle module proves the extension is not `internal`; the real SQLite tests prove behavior did not change through relocation.

No additional endpoint, UI, or server integration test is required because no endpoint, UI, server registration, or HTTP behavior changes.

## README updates

Coding must update `features/common/README.md` without modifying `## Operator Notes`. Add `SQLException.isUniqueViolation` to the Models section as a public JVM-only utility in `common/common`. Add an Architecture Notes entry stating that Common owns exact PostgreSQL/Xerial unique-violation classification, traverses cause and JDBC next-exception edges iteratively with object-identity cycle protection, and deliberately ignores code 19, messages, base SQLite constraints, and unrelated constraint result codes.

Coding must update `features/users/README.md` without modifying `## Operator Notes`. In the `ExposedUsersRepo` model/duplicate-key documentation, replace ownership wording so the repository is described as consuming `dev.inmo.wishlist.features.common.common.isUniqueViolation`. Preserve all existing statements about translation to `DuplicateUserFieldException`, supported markers, negative markers, current HTTP consumers, and unchanged cache propagation.

## Coding sequence

First add the Common JVM dependency and new source file. Next import the extension into Users and delete the old declaration. Then move the unit suite and adjust both Gradle dependency scopes. Apply the two README deltas last. Do not rebuild the AST index between these tightly coupled source moves; rebuild once after all source changes are present.

The old Users test path must be removed in the same Coding commit that creates the Common test path. The old local function must be removed in the same commit that creates the Common function. These paired moves prevent duplicate definitions, duplicate test execution, and a transient missing API in the delivered change.

## Verification gates and stop conditions

Run `./gradlew :wishlist.features.common.common:jvmTest :wishlist.features.users.common:jvmTest`. The Common task must no longer report `NO-SOURCE`; all thirteen classifier tests must run once from Common, and all six Users SQLite repository tests must continue passing.

Run `./gradlew :wishlist.features.common.common:build :wishlist.features.users.common:build`. This verifies the JVM relocation and confirms that shared JS and Android compilations remain unaffected by the JVM-only source and dependency.

Run dependency inspection for both affected configurations. Common `jvmCompileClasspath` must resolve `org.xerial:sqlite-jdbc:3.53.4.0`; Users `jvmCompileClasspath` must not contain a direct Users declaration; Users `jvmTestCompileClasspath` must still resolve Xerial 3.53.4.0 through its explicit test dependency.

After source changes, run `ast-index rebuild`, then query references and Common public API. The final index must show exactly one `isUniqueViolation` definition in Common `jvmMain`, two production calls in `ExposedUsersRepo`, thirteen unit-test calls in Common `jvmTest`, zero definitions in Users, and the function in `ast-index api "features.common.common"`.

The Coding step stops when the focused tests and both module builds pass, the dependency scopes match the design, the AST invariants hold, the old source/test declarations are absent, and both Operator Notes remain unchanged. Any failure that would require changing classifier semantics, repository contracts, routes, schemas, or Common's internal-feature dependency rule must return to Architecture or the operator rather than broadening the change.

## Coding handoff

ENTITY:
entity_id=sql_exception_unique_violation_extension; type=public_JVM_extension; state=architecture_complete

CONTEXT:

* task_id=03.09.2026_18.04.56-c690b056-398b-4f17-a64e-d7731f35b333; agent_id=architecturing; memory_ref=[PROMPT.md,001-planning.md,features/common/README.md,features/users/README.md,ExposedUsersRepo.kt,IsUniqueViolationTest.kt,ExposedUsersRepoSqliteTest.kt]
* constraints=[public_visibility,common_common_jvmMain_ownership,behavior_preservation,identity_cycle_safety,exact_Xerial_result_codes,no_internal_feature_dependency,Operator_Notes_immutable]; exclusions=[route_change,schema_change,repository_contract_change,message_matching,errorCode_19_matching,base_SQLite_constraint_matching]

ACTION:

1. action=create_public_extension; target=sql_exception_unique_violation_extension; params={file=features/common/common/src/jvmMain/kotlin/SQLExceptionExtensions.kt,package=dev.inmo.wishlist.features.common.common,receiver=java.sql.SQLException,name=isUniqueViolation,visibility=public,implementation=move_unchanged}
2. action=replace_local_definition_with_import; target=ExposedUsersRepo; params={import=dev.inmo.wishlist.features.common.common.isUniqueViolation,call_sites=[update,create],catch_semantics=unchanged,obsolete_imports=[SQLiteErrorCode,SQLiteException,SQLException,ArrayDeque,Collections,IdentityHashMap]}
3. action=move_unit_suite; target=IsUniqueViolationTest; params={source=features/users/common/src/jvmTest/kotlin/repo/IsUniqueViolationTest.kt,destination=features/common/common/src/jvmTest/kotlin/IsUniqueViolationTest.kt,target_package=dev.inmo.wishlist.features.common.common,test_count=13,behavior_change=false}
4. action=retain_integration_suite; target=ExposedUsersRepoSqliteTest; params={module=features.users.common,test_count=6,source_change=false,purpose=[cross_module_visibility,repository_translation,state_preservation]}
5. action=move_dependency_ownership; target=libs.xerial.sql; params={common_jvmMain=implementation,users_jvmMain=remove,users_jvmTest=implementation,resolved_version=3.53.4.0,public_API_exposure=false}
6. action=update_documentation; target=feature_READMEs; params={common_models_and_architecture=add_public_classifier,users_duplicate_convention=reference_common_owner,Operator_Notes_change=false}
7. action=verify_and_stop; target=affected_modules; params={gates=[common_jvmTest,users_jvmTest,common_build,users_build,dependencyInsight,ast-index_rebuild,ast-index_refs,ast-index_api]}

REASON:

* condition=cross_feature_database_classifier_owned_by_users_internal_repository_file; requirement=public_reusable_JVM_API_owned_by_common_common
* condition=implementation_references_Xerial_types_while_public_signature_exposes_only_JDK_types; requirement=common_jvmMain_implementation_dependency_plus_users_jvmTest_direct_dependency
* condition=relocation_must_preserve_validated_behavior; requirement=unchanged_algorithm_plus_moved_unit_suite_plus_retained_cross_module_integration_suite

EXPECTED RESULT:

* entity_id=sql_exception_unique_violation_extension; new_state=single_public_definition_with_exact_preserved_semantics; location=features/common/common/src/jvmMain/kotlin/SQLExceptionExtensions.kt
* entity_id=unique_violation_unit_suite; new_state=single_owner_adjacent_13_case_suite; location=features/common/common/src/jvmTest/kotlin/IsUniqueViolationTest.kt
* entity_id=users_repository_consumer; new_state=imports_public_Common_extension_with_unchanged_translation; location=features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt

VERIFICATION:

* check=classifier_exact_positive_markers; expected=[PostgreSQL_23505,Xerial_SQLITE_CONSTRAINT_UNIQUE,Xerial_SQLITE_CONSTRAINT_PRIMARYKEY]
* check=classifier_exact_negative_markers; expected=[null_or_unrelated_SQL_state,JDBC_errorCode_19,message_text,SQLITE_CONSTRAINT,NOTNULL,CHECK,FOREIGNKEY,ROWID]
* check=exception_graph_behavior; expected=[cause_traversed,nextException_traversed,cycles_terminate,distinct_equal_nodes_visited]
* check=module_boundary; expected=[Common_public_API_contains_extension,Users_has_two_production_calls,Users_has_zero_definition]
* check=dependency_boundary; expected=[Common_jvmMain_Xerial_implementation,Users_jvmMain_no_direct_Xerial,Users_jvmTest_Xerial_implementation]
* check=automated_coverage; expected=[Common_classifier_13_pass,Users_SQLite_6_pass,affected_module_builds_pass]

UNCERTAINTY:

* missing=none; ambiguity=none; operator_confirmation_required=false; untestable_functionality=false

REPETITION OF RESULT:

* entity_id=sql_exception_unique_violation_extension; stored_in=shared_step_file; status=available_for_coding

COMMUNICATION:

* sender=architecturing; receiver=coding; task_id=03.09.2026_18.04.56-c690b056-398b-4f17-a64e-d7731f35b333; message_id=architecturing-002-public-unique-violation; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[task_id,sql_exception_unique_violation_extension,common_JVM_public_API]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true
* causal_chain_present=true; ambiguity_detected=false; result_duplication_present=true
