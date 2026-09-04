Model: GPT-5.6 Sol
Changed files: agents/task/04.09.2026_05.39.27-590a7fd5-52ed-4f56-bae8-264cbac05424/003-architecturing.md

# Architecture report

GPT-5.6 Sol is the actual model used for this step. The Architecture role prefers the HL tier before ML, and Sol is the available HL model best suited to specifying a repository-wide rule together with a compatibility-preserving JVM package relocation across two feature modules.

## Architectural decision

The canonical guidance belongs in `agents/ARCHITECTURE.md` under `## Modules Structure`, directly after the paragraph stating that the `server` module contains `commonMain` and `jvmMain` targets and immediately before `## Gradle Module Names`. Coding must insert this exact standalone paragraph:

> Stateless, reasonably short declarations that serve one clear purpose MUST live in the owning module's `utils` package (`src/<sourceSet>/kotlin/utils/`), not at the module package root.

The rule is intentionally based on declaration properties rather than declaration kind, so a qualifying extension function is covered without implying that every extension, class, or shared service belongs in `utils`. “Owning module” preserves module boundaries, while the source-set placeholder preserves KMP platform placement. The rule changes package organization only and creates no new layer or dependency.

`agents/ALL.md` normally limits Coding to source, resource, and feature README files. The user's prompt explicitly requests the architecture-rule change, so Coding has a narrow exception to edit only the specified paragraph in `agents/ARCHITECTURE.md` for this task. No permission extends to any other agent guidance file.

## Source, package, and import design

Coding must move `features/common/common/src/jvmMain/kotlin/SQLExceptionExtensions.kt` to `features/common/common/src/jvmMain/kotlin/utils/SQLExceptionExtensions.kt`. The only Kotlin-content change in that file is the package declaration, from `dev.inmo.wishlist.features.common.common` to `dev.inmo.wishlist.features.common.common.utils`. The declaration must remain public with the exact signature `fun SQLException.isUniqueViolation(): Boolean`; its KDoc, imports, iterative traversal, identity-based visited set, PostgreSQL SQL-state check, SQLite result-code checks, and return behavior must remain byte-for-byte unchanged.

Coding must move `features/common/common/src/jvmTest/kotlin/IsUniqueViolationTest.kt` to `features/common/common/src/jvmTest/kotlin/utils/IsUniqueViolationTest.kt`. Its package declaration changes to `dev.inmo.wishlist.features.common.common.utils`. Because the test and extension remain in the same package, no extension import is needed in the test. Test names, fixtures, KDoc, inputs, assertions, and bodies remain unchanged.

Coding must change the sole consumer import in `features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt` from `dev.inmo.wishlist.features.common.common.isUniqueViolation` to `dev.inmo.wishlist.features.common.common.utils.isUniqueViolation`. Both calls in `create` and `update`, all exception translation, and all other content remain unchanged.

The old source and test paths must no longer exist. No forwarding function, type alias, deprecated compatibility declaration, wildcard import, or duplicate implementation may retain `dev.inmo.wishlist.features.common.common.isUniqueViolation`. The relocation is an intentional package-level API change required by the prompt.

No `build.gradle`, version-catalog, settings, or dependency edit is required. Kotlin already discovers the `kotlin/utils/` directories inside the existing `jvmMain` and `jvmTest` source roots, Common already owns the Xerial dependency needed by production, and Users already depends on Common.

## README updates

Coding must update `features/common/README.md` without touching `## Operator Notes`. In the `## Models` row for `SQLException.isUniqueViolation`, the module/location cell must identify `common/common` JVM source package `dev.inmo.wishlist.features.common.common.utils`. In `## Architecture Notes`, the classifier bullet must identify `common/common/src/jvmMain/kotlin/utils/SQLExceptionExtensions.kt` and package `dev.inmo.wishlist.features.common.common.utils`; the documented classification semantics remain unchanged.

Coding must update `features/users/README.md` without touching `## Operator Notes`. Both fully qualified references to `dev.inmo.wishlist.features.common.common.isUniqueViolation`—one in the `DuplicateUserFieldException` model row and one in the duplicate-key-to-409 architecture note—must become `dev.inmo.wishlist.features.common.common.utils.isUniqueViolation`. No other Users documentation changes are needed.

## Behavior-preservation test specifications

The relocated `IsUniqueViolationTest` remains the focused contract suite and must continue to execute all thirteen existing test methods. The test inputs and expected results are:

1. A plain `SQLException` with PostgreSQL SQL state `23505` returns `true`.
2. A plain `SQLException` with unrelated SQL state `23503` returns `false`.
3. A plain `SQLException` with a null SQL state returns `false`.
4. A Xerial `SQLiteException` with `SQLITE_CONSTRAINT_UNIQUE` returns `true`.
5. A Xerial `SQLiteException` with `SQLITE_CONSTRAINT_PRIMARYKEY` returns `true`.
6. A PostgreSQL `23505` exception nested below a non-SQL cause wrapper remains reachable and returns `true`.
7. A SQLite UNIQUE exception nested below a non-SQL cause wrapper remains reachable and returns `true`.
8. A SQLite PRIMARY KEY exception linked through `SQLException.nextException` remains reachable and returns `true`.
9. Each of `SQLITE_CONSTRAINT`, `SQLITE_CONSTRAINT_NOTNULL`, `SQLITE_CONSTRAINT_CHECK`, `SQLITE_CONSTRAINT_FOREIGNKEY`, and `SQLITE_CONSTRAINT_ROWID` returns `false`.
10. A plain JDBC exception with error code 19 and a UNIQUE-looking message returns `false`.
11. A Xerial base `SQLITE_CONSTRAINT` exception with a UNIQUE-looking message returns `false`.
12. A cycle spanning `cause` and `nextException` terminates and returns `false` when no unique marker exists.
13. Distinct exceptions with colliding `equals` and `hashCode` implementations are both visited, allowing a reachable `23505` node to return `true`.

The unchanged `ExposedUsersRepoSqliteTest` remains the real-driver integration contract. All six existing cases must continue to pass: duplicate username on create and duplicate non-null email on create both throw `DuplicateUserFieldException` with the exact retained Exposed/Xerial cause chain and leave stored data unchanged; duplicate username on update and duplicate non-null email on update do the same without mutating either row; multiple null emails remain valid; and a non-unique database failure remains an `ExposedSQLException` whose Xerial result code is `SQLITE_ERROR`.

No new functional branch is introduced, so no new test method is required. Package compilation is covered by moving the focused test into the new package and compiling Users with the new explicit import. No planned functionality is untestable: runtime behavior is covered by the two existing suites, while architecture-guidance placement and package topology are statically verifiable. No operator confirmation is required before Coding.

## Verification and stop conditions

After the source changes, Coding must run `ast-index rebuild`, then query `isUniqueViolation` references. The rebuilt index must report exactly one public definition at `features/common/common/src/jvmMain/kotlin/utils/SQLExceptionExtensions.kt`, thirteen calls in `features/common/common/src/jvmTest/kotlin/utils/IsUniqueViolationTest.kt`, and two production calls in `features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt`. It must report no definition, import, or call associated with `dev.inmo.wishlist.features.common.common.isUniqueViolation`, and the Users import must resolve to `dev.inmo.wishlist.features.common.common.utils.isUniqueViolation`.

Run `./gradlew --no-daemon --console=plain --rerun-tasks :wishlist.features.common.common:jvmTest :wishlist.features.users.common:jvmTest`. The Common suite must execute all thirteen classifier tests once, and the Users suite must execute all six SQLite repository tests with zero failures and zero errors. Then run `./gradlew :wishlist.features.common.common:build :wishlist.features.users.common:build` and the repository-wide gates required by the Verification role.

Finally, run `git diff --check` and inspect the diff. Stop only when the architecture paragraph is present exactly once at the specified location; the source and focused test are true moves with only package declarations changed; the Users production diff changes only the import; no old-package compatibility declaration remains; both README package references are current; both Operator Notes sections are unchanged; no Gradle file changed; and all focused tests and affected-module builds pass.

## Coding handoff

ENTITY:
entity_id=utility_package_rule_and_relocation; type=architecture_change; state=specified

CONTEXT:

* task_id=04.09.2026_05.39.27-590a7fd5-52ed-4f56-bae8-264cbac05424; agent_id=architecturing; memory_ref=[PROMPT.md,002-planning.md,003-architecturing.md]
* constraints=[behavior_preservation,operator_notes_immutable,target_package=dev.inmo.wishlist.features.common.common.utils,no_compatibility_alias,no_gradle_change,coding_exception=agents/ARCHITECTURE.md_exact_paragraph_only]

ACTION:

1. action=insert_architecture_rule; target=agents/ARCHITECTURE.md; params={section=Modules_Structure,position=before_Gradle_Module_Names,wording=stateless_reasonably_short_single_purpose_declarations_MUST_live_in_owning_module_utils_package}
2. action=relocate_definition; target=utility_package_rule_and_relocation; params={from=features/common/common/src/jvmMain/kotlin/SQLExceptionExtensions.kt,to=features/common/common/src/jvmMain/kotlin/utils/SQLExceptionExtensions.kt,package=dev.inmo.wishlist.features.common.common.utils,body_change=none}
3. action=relocate_focused_test; target=utility_package_rule_and_relocation; params={from=features/common/common/src/jvmTest/kotlin/IsUniqueViolationTest.kt,to=features/common/common/src/jvmTest/kotlin/utils/IsUniqueViolationTest.kt,package=dev.inmo.wishlist.features.common.common.utils,test_count=13}
4. action=update_consumer_import; target=features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt; params={old=dev.inmo.wishlist.features.common.common.isUniqueViolation,new=dev.inmo.wishlist.features.common.common.utils.isUniqueViolation,call_count=2}
5. action=update_feature_docs; target=utility_package_rule_and_relocation; params={files=[features/common/README.md,features/users/README.md],operator_notes_change=false,semantic_change=package_location_only}
6. action=verify_behavior_and_topology; target=utility_package_rule_and_relocation; params={classifier_tests=13,sqlite_repository_tests=6,affected_builds=2,ast_index_rebuild=true,diff_check=true}

REASON:

* condition=stateless_reasonably_short_single_purpose_classifier_at_module_package_root → requirement=canonical_utils_package_rule_plus_definition_test_import_documentation_relocation_without_runtime_change

EXPECTED RESULT:

* entity_id=utility_package_rule_and_relocation; new_state=documented_and_relocated_with_preserved_behavior; location=dev.inmo.wishlist.features.common.common.utils

VERIFICATION:

* check=architecture_source_test_consumer_documentation_boundary; expected={rule_occurrences=1,new_definitions=1,old_definitions=0,focused_calls=13,production_calls=2,classifier_passed=13,sqlite_repository_passed=6,operator_notes_changed=false,gradle_files_changed=false}

UNCERTAINTY:

* missing=none; ambiguity=none

REPETITION OF RESULT:

* entity_id=utility_package_rule_and_relocation; stored_in=shared_memory; status=available

COMMUNICATION:

* sender=architecturing; receiver=coding; task_id=04.09.2026_05.39.27-590a7fd5-52ed-4f56-bae8-264cbac05424; message_id=7d8f9a0c-e1b2-4c3d-8e5f-60718293a4b5; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[task_id,entity_id,intent]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
