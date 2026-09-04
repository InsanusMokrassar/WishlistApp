Model: GPT-5.6 Sol
Changed files: agents/task/04.09.2026_05.39.27-590a7fd5-52ed-4f56-bae8-264cbac05424/001-planning.md

# Planning report

GPT-5.6 Sol is the actual model for this step and satisfies the Planning role's preferred HL tier. The task is narrow, but choosing the preferred reasoning tier is appropriate because the package move crosses a public JVM API, production imports, tests, feature documentation, and repository-wide architecture guidance.

## Task understanding

Add one repository-wide architecture rule that classifies a declaration as a utility when the declaration is stateless, reasonably short, and dedicated to one clear purpose, and requires such declarations to live in the owning module's `utils` package. Apply the rule to `SQLException.isUniqueViolation` by moving the JVM source and focused JVM test into `dev.inmo.wishlist.features.common.common.utils`. Preserve the function's public visibility, extension receiver, signature, implementation, KDoc, supported PostgreSQL/Xerial markers, graph traversal, cycle handling, and all negative classifications. The requested move intentionally changes the Kotlin fully qualified name; no root-package forwarding alias should remain because such an alias would leave a utility declaration outside `utils` and undermine the new rule.

## Investigation result

The task folder contained no prior step, so Planning used `PROMPT.md`. No local ALL, PLAN, CODING, or ARCHITECTURE override exists. The Common and Users feature READMEs were read in full, including both empty Operator Notes sections; neither section adds a constraint or conflicts with the request.

The AST index was initially absent. The required rebuild succeeded after filesystem approval and indexed 1,393 files across 114 modules. The index reports exactly one `isUniqueViolation` definition at `features/common/common/src/jvmMain/kotlin/SQLExceptionExtensions.kt`, thirteen focused test calls at `features/common/common/src/jvmTest/kotlin/IsUniqueViolationTest.kt`, and two production calls in `features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt`. No other production consumer exists. The Common public API currently exposes the function from the module-root package. The index also finds twenty-four existing source files under `kotlin/utils/`; representative production/test pairs use matching `utils` packages, which supports moving both the declaration and its directly associated test rather than adding a test-only import from the root package.

No Gradle dependency change is needed. Both moved files remain in the same `features/common/common` JVM source sets, `features/users/common` already has an API dependency on `features/common/common`, and the Xerial dependency remains in Common `jvmMain`. The Users SQLite repository regression suite remains relevant because the two production calls translate supported driver failures into `DuplicateUserFieldException`.

## Questions for operator

There are no unclear architecture decisions, requirements, or constraints to relay. The prompt supplies the utility criteria and exact destination package, the existing package layout establishes the matching directory form, and the word “move” resolves the old-FQCN compatibility question in favor of removing the old declaration.

## Architecture-guidance change

The canonical location is `agents/ARCHITECTURE.md`, in `## Modules Structure`, immediately after the source-set placement guidance and before `## Gradle Module Names`. That location makes the rule a repository-wide package-layout invariant rather than a Common-only implementation note. Architecture should retain the following concise normative wording, optionally under a `### Utility declarations` subheading:

> Stateless, reasonably short declarations that serve one clear purpose MUST live in the owning module's `utils` package (`src/<sourceSet>/kotlin/utils/`), not at the module package root.

The current operator prompt explicitly requests this architecture rule, so the prompt authorizes the task-required edit to `agents/ARCHITECTURE.md`. Although `agents/ALL.md` normally limits Coding to source, resource, and feature README files in addition to the Coding step report, Coding may make and commit this one architecture-guidance edit because the explicit task request overrides that default role restriction for the exact required file. The authorization does not extend to any other agent guidance file or unrelated cleanup.

## Concrete implementation plan

In `agents/ARCHITECTURE.md`, add only the utility-package rule at the location and with the scope above. Do not duplicate the rule in `agents/CODING.md`; `agents/ARCHITECTURE.md` is the canonical repository architecture guide, while the task-specific Coding handoff makes the new rule actionable in this cycle.

Move `features/common/common/src/jvmMain/kotlin/SQLExceptionExtensions.kt` to `features/common/common/src/jvmMain/kotlin/utils/SQLExceptionExtensions.kt` and change only its package declaration from `dev.inmo.wishlist.features.common.common` to `dev.inmo.wishlist.features.common.common.utils`. Keep the filename, imports, KDoc, public signature, and body unchanged.

Move `features/common/common/src/jvmTest/kotlin/IsUniqueViolationTest.kt` to `features/common/common/src/jvmTest/kotlin/utils/IsUniqueViolationTest.kt` and change its package declaration to `dev.inmo.wishlist.features.common.common.utils`. Keep all thirteen tests and their fixtures unchanged. Co-locating the test package with the utility preserves unqualified extension calls and verifies the same behavior through the new package.

In `features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt`, replace the old import with `dev.inmo.wishlist.features.common.common.utils.isUniqueViolation`. Do not alter either call site, exception translation, transaction behavior, or KDoc semantics.

In `features/common/README.md`, leave `## Operator Notes` untouched and update the classifier documentation to identify `utils/SQLExceptionExtensions.kt` or the `dev.inmo.wishlist.features.common.common.utils` package while retaining the exact behavior description. In `features/users/README.md`, leave `## Operator Notes` untouched and update both old fully qualified references to `dev.inmo.wishlist.features.common.common.utils.isUniqueViolation`. No route, model shape, module dependency, or Gradle file changes are required.

Architecture should turn the relocation into test specifications without inventing new behavior. Coding should then apply the architecture guidance, source/test moves, one production import change, and two README corrections as one coherent change.

## Verification plan

After all Kotlin moves and import changes, rebuild the AST index once. The rebuilt index must report exactly one public definition at `features/common/common/src/jvmMain/kotlin/utils/SQLExceptionExtensions.kt`, thirteen focused calls in `features/common/common/src/jvmTest/kotlin/utils/IsUniqueViolationTest.kt`, two production calls in `ExposedUsersRepo`, no definition or test file at either old path, no import of `dev.inmo.wishlist.features.common.common.isUniqueViolation`, and the new fully qualified extension in the Common JVM API.

Run `./gradlew :wishlist.features.common.common:jvmTest :wishlist.features.users.common:jvmTest` so the thirteen classifier cases and the six real SQLite repository cases preserve the existing positive, negative, nested-graph, cycle, equality-collision, create-collision, update-collision, and non-collision behavior. Then run `./gradlew :wishlist.features.common.common:build :wishlist.features.users.common:build`, followed by the repository-wide gates required by the later Verification role and `git diff --check`. Review the final diff to confirm that source and test bodies are unchanged apart from package relocation, only the Users import changes in production code, Operator Notes remain byte-for-byte untouched, and the architecture text is present exactly once in the canonical guide.

## Architecture handoff

ENTITY:
entity_id=utility_package_relocation; type=architecture_change; state=planned

CONTEXT:

* task_id=04.09.2026_05.39.27-590a7fd5-52ed-4f56-bae8-264cbac05424; agent_id=planning; memory_ref=[PROMPT.md,001-planning.md]
* constraints=[behavior_preservation,public_signature_preservation,target_package=dev.inmo.wishlist.features.common.common.utils,operator_notes_immutable,no_forwarding_alias,architecture_edit_authorized]

ACTION:

1. action=document_rule; target=utility_package_relocation; params={file=agents/ARCHITECTURE.md,location=Modules_Structure_after_source_sets,rule=stateless+reasonably_short+single_purpose_declarations_MUST_use_utils_package}
2. action=relocate_source_and_test; target=utility_package_relocation; params={source=features/common/common/src/jvmMain/kotlin/utils/SQLExceptionExtensions.kt,test=features/common/common/src/jvmTest/kotlin/utils/IsUniqueViolationTest.kt,package=dev.inmo.wishlist.features.common.common.utils}
3. action=update_consumers_and_docs; target=utility_package_relocation; params={production_import=ExposedUsersRepo.kt,readmes=[features/common/README.md,features/users/README.md],gradle_changes=none}
4. action=verify; target=utility_package_relocation; params={ast_index=rebuild,tests=[common_jvmTest,users_jvmTest],builds=[common_build,users_build],behavior_cases=[13_classifier,6_sqlite_repository]}

REASON:

* condition=public_stateless_short_single_purpose_extension_at_module_root → requirement=move_definition_and_associated_test_to_owning_module_utils_package_without_algorithm_change

EXPECTED RESULT:

* entity_id=utility_package_relocation; new_state=implemented_with_preserved_behavior; location=dev.inmo.wishlist.features.common.common.utils

VERIFICATION:

* check=definition_usage_test_documentation_boundary; expected={definitions_new_package=1,definitions_old_package=0,focused_test_calls=13,production_calls=2,focused_suites=passed,operator_notes_changed=false}

UNCERTAINTY:

* missing=none; ambiguity=none

REPETITION OF RESULT:

* entity_id=utility_package_relocation; stored_in=shared_step_report; status=available_for_architecture

COMMUNICATION:

* sender=planning; receiver=architecture; task_id=04.09.2026_05.39.27-590a7fd5-52ed-4f56-bae8-264cbac05424; message_id=0f53c7bc-8dcf-4ba5-852d-2c4333c66664; protocol=AML-HIP

PERSISTENCE:

* local_memory=false; shared_memory=false; index_keys=[task_id,entity_id,intent]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
