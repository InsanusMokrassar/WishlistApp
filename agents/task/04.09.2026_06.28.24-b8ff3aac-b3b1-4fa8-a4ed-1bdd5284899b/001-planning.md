Model: GPT-5.6 Sol
Changed files: agents/task/04.09.2026_06.28.24-b8ff3aac-b3b1-4fa8-a4ed-1bdd5284899b/001-planning.md

GPT-5.6 Sol is the preferred high-level model for Planning under `agents/SHORTCUTS.md` and `agents/MODELS.md`. The work requires repository-wide Gradle/KMP analysis, compatibility judgment, baseline design, and an exact custom-AST-rule boundary, so the high-level planning allocation is warranted.

## Task understanding

Issue #72 asks for one narrow quality gate: make the existing `agents/CODING.md` ban on `else if` and its public KDoc expectations mechanically enforceable with Detekt, then make Verification stop before the build when lint fails. A binary `if`/`else` must remain valid. Existing application behavior, APIs, UI, persistence, and feature documentation are outside the requested behavior.

The three requested built-in Detekt rules cover public classes/objects/interfaces, public functions, and public properties. They do not prove every semantic requirement in `agents/CODING.md`: Detekt does not judge whether prose describes purpose, and these three rules do not cover every private/internal declaration or every `@param`/`@return` obligation. Validator review therefore remains responsible for the non-mechanical remainder.

## Questions for operator

No unclear product or architecture decision remains. No operator question is required. The issue explicitly permits a custom rule, the repository has enough legacy documentation debt to require a baseline rather than a cross-feature cleanup, and the compatible Detekt version follows directly from the checked-in toolchain.

The issue preflight supplied by the Orchestrator reports no issue comments and no linked merged or open pull request, so no operator answer or prior implementation constrains the plan.

## Investigation and baseline evidence

The branch `fix/issue-72-detekt-conventions` starts at `474d49c19abe09d93969d6e326459ab27c3de211`, identical to local `master` and `origin/master`. The only pre-existing untracked path is the current task folder.

The root uses Groovy Gradle scripts and a version catalog. `settings.gradle` includes 49 subprojects through one naming loop; 48 paths currently contain Kotlin and the `server` launcher has no Kotlin source. The root `build.gradle` centralizes plugin classpaths and repository configuration, while module scripts compose shared scripts from `gradle/templates/`. No Detekt plugin, Detekt dependency, Detekt YAML, or Detekt baseline exists.

`ast-index` was installed but initially could not create its cache inside the filesystem sandbox. After the required approved rebuild, the index contained the WishlistApp tree and was used for the module/source inventory. It reports 690 module-owned Kotlin files across `androidMain`, `commonMain`, `commonTest`, `jsMain`, `jsTest`, `jvmMain`, `jvmTest`, and Android `main`. Detekt's documented default source locations are only `src/main/{java,kotlin}` and `src/test/{java,kotlin}`, so a convention that merely applies the plugin would miss the KMP source-set layout. Each project-level `detekt` task must receive that project's complete `src` tree explicitly.

`ast-index agrep` could not execute because the separately packaged `sg` binary is unavailable. The prescribed fallback multiline text scan found zero existing `else if (` sequences in all tracked Kotlin files. The absence means the custom rule needs no legacy suppression; unit fixtures, rather than current product code, must prove both rejection and allowed cases.

The current `./gradlew detekt` exits nonzero because no such task exists. The current `./gradlew build` succeeds in 1 minute 4 seconds with 4,462 actionable tasks, 184 executed and 4,278 up-to-date. Existing Android publication, compile-SDK, JavaScript configuration-time resolution, webpack-size, and Gradle-10 deprecation warnings are unrelated baseline noise.

Detekt CLI `2.0.0-alpha.3`, run in light-analysis mode with only the requested KDoc rules, successfully parsed all 690 application Kotlin files on the repository's Java 17 runtime. The scan found 420 existing violations across 37 modules: 172 `UndocumentedPublicClass`, 48 `UndocumentedPublicFunction`, and 200 `UndocumentedPublicProperty` findings. A repository-wide single baseline collapsed the 420 findings to 321 IDs because Detekt signatures repeat across module-local files such as `Plugin.kt` and `AndroidPlugin.kt`. A shared baseline could therefore hide a future violation with an existing cross-module signature. Project-scoped baseline files are required.

The official [Detekt compatibility table](https://detekt.dev/docs/introduction/compatibility/) identifies `2.0.0-alpha.3` as the release built with Gradle 9.3.1 and Kotlin 2.3.21, exactly matching the wrapper and Kotlin plugin in this repository. The project AGP 8.13.2 remains above Detekt 2.0's documented minimum, and the planned syntax-only rules need no Android or JVM type resolution. Official Detekt documentation also confirms that custom rules use a pure JVM plugin JAR, `RuleSetProvider` service loading, `compileOnly` `detekt-api`, explicit rule activation, and an explicit task dependency on the in-repository rules JAR. The [configuration documentation](https://detekt.dev/docs/introduction/configurations/) confirms that a custom configuration with `buildUponDefaultConfig=false` does not inherit unrelated defaults. The [baseline documentation](https://detekt.dev/docs/introduction/baseline/) defines baselines as the supported incremental-adoption mechanism.

## Cause and design decision

The enforcement gap has two independent causes. The build never applies a Kotlin static-analysis plugin, and Detekt has no built-in rule that structurally distinguishes an `else if` chain from a legal binary `if`/`else`. Text or regex matching would be formatting-sensitive and could match comments or strings, so forbidden-comment/import/method configuration is not a valid substitute.

Use Detekt `2.0.0-alpha.3` from the existing version catalog. Apply and configure it from the root build to every Kotlin-bearing subproject, but keep the ordinary non-type-resolving `detekt` task as the gate. Point each task at the owning project's entire `src` directory, the shared YAML, and a uniquely named project baseline. This covers all current KMP and Android source sets without invoking the much larger target/variant task matrix.

Add a small pure-JVM `detekt-rules` build-tooling module. Its `NoElseIf` rule visits `KtIfExpression` nodes and reports the outer conditional exactly when `elseExpression is KtIfExpression`. A binary `if`/`else` has a non-`KtIfExpression` else expression and remains accepted. An independent nested conditional inside `else { ... }` has a `KtBlockExpression` else expression and also remains accepted. A longer chain produces a finding for each chained link, which is acceptable and deterministic.

Keep legacy source unchanged. Generate one baseline per existing project after the custom rule is active, verify that baselines contain only the three KDoc rule IDs, and commit the baseline files. Project isolation prevents same-signature declarations in another module from inheriting a suppression. No feature source or feature README edit is planned, so no feature Operator Notes are implicated.

## Acceptance criteria

1. Detekt `2.0.0-alpha.3` is declared once in `gradle/libs.versions.toml`; the root build obtains the Gradle plugin from the catalog and applies one shared convention to every project after a Kotlin JVM, Kotlin Multiplatform, or Kotlin Android plugin appears.
2. Every configured `detekt` task scans all `.kt` files below that project's `src`, uses the checked-in shared config, uses a project-unique baseline path, loads the custom rule JAR where no self-dependency is created, and fails on error findings.
3. The shared config activates only `UndocumentedPublicClass`, `UndocumentedPublicFunction`, `UndocumentedPublicProperty`, and `NoElseIf`; it does not inherit unrelated Detekt defaults.
4. `NoElseIf` reports braced, expression-body, multiline, commented-separator, and multi-link `else if` chains. Tests prove that binary `if`/`else`, an `if` without `else`, and an independent `if` inside an explicit `else` block remain valid.
5. The checked-in baselines suppress the 420 observed legacy KDoc findings at project scope and contain no `NoElseIf` entry. No application Kotlin file is edited merely to make initial lint adoption pass.
6. `./gradlew detekt` exists, reaches every Kotlin-bearing project, builds/loads the custom rules, and exits zero on the grandfathered repository. A deliberately noncompliant unit fixture for the rule produces the expected finding without relying on source-text matching.
7. `agents/VERIFICATION.md` runs `./gradlew detekt` under `set -o pipefail` with output captured in `/tmp/detekt-output.txt`, records the real exit code and findings, and returns `FAIL` to Coding immediately on a nonzero lint result before starting `./gradlew build`.
8. `./gradlew :wishlist.detekt-rules:test`, `./gradlew detekt`, and `./gradlew build` all pass; `git diff --check` passes; `ast-index rebuild` runs because new Kotlin rule/test files are added.

## Non-goals and stop condition

Do not fill the 420 legacy KDocs, refactor unrelated control flow, enable the broader default Detekt catalog, add formatting/autocorrection, add type-resolution or Android-variant Detekt tasks, enforce value-class naming, change product behavior, or update feature READMEs without a feature-source change. Do not lint ignored template skeletons under `.templates`; module-owned `src` roots are the intended input boundary.

Stop implementation when the four rules are active across every module source tree, legacy-only project baselines are committed, the custom rule's positive and negative tests pass, Verification has a blocking pre-build lint step, and the focused plus aggregate gates pass. Further lint policy or legacy cleanup requires a separate issue.

## Concrete implementation plan for Architecture and Coding

1. Extend `gradle/libs.versions.toml` with the single Detekt version and aliases for `detekt-gradle-plugin`, `detekt-api`, and `detekt-test`. Add the plugin classpath to the existing root `buildscript.dependencies` block.
2. Add `:detekt-rules` to the `settings.gradle` include list. Create `detekt-rules/build.gradle` as a pure Kotlin/JVM 17 module using `compileOnly` Detekt API and test-only Detekt test helpers plus the repository's Kotlin test/JUnit convention.
3. Add KDoc-complete `NoElseIf` rule and provider sources plus `META-INF/services/dev.detekt.api.RuleSetProvider`. Keep the provider and rule IDs stable and descriptive so baseline/suppression identities remain useful.
4. Add focused rule tests for all acceptance cases. Assert finding count and location for chains, including two findings for a three-condition chain, and zero findings for the permitted binary/nested-block cases.
5. Add `config/detekt/detekt.yml` with validation enabled, warnings treated as errors, the three comments rules active, and the custom convention rule active. Keep `buildUponDefaultConfig=false` in Gradle so no unrequested rules leak into the gate.
6. In root `build.gradle`, install the Detekt convention through Kotlin plugin callbacks so future Kotlin subprojects inherit it automatically. Configure the complete project `src` input, root-relative reports, failure severity, shared YAML, unique baseline filename derived from the already-unique full project name, and custom rule JAR dependency. Avoid loading the custom JAR into its own producer task; still run the built-in KDoc rules on the tooling module.
7. Generate project baselines only after the final source set and rule configuration exists. Review the generated IDs and retain only legacy KDoc findings; a `NoElseIf` baseline entry is a failure because the current source scan found no chain.
8. Update `agents/VERIFICATION.md` before its build step and extend the documented report format with a Detekt section containing exit code and findings. Preserve the existing build/test parsing and failure handoff behavior.
9. Run the custom-rule tests, the aggregate Detekt task, a task/input coverage check for every Kotlin-bearing project, the full build, `git diff --check`, and `ast-index rebuild`. Confirm that only build tooling, lint configuration/baselines, Verification instructions, and the role report changed.

## Risks and verification focus

The main integration risk is silently scanning only conventional JVM directories; explicit project `src` inputs and report/task coverage are mandatory proof. The second risk is an unsafe global baseline; unique per-project files are mandatory proof. The third risk is a custom-rule classpath race; every consumer `detekt` task must depend on the rules JAR as recommended by Detekt. The fourth risk is self-dependency in `detekt-rules`; the producer must not declare itself in `detektPlugins`.

The CLI compatibility and rule-count investigation is conclusive for syntax analysis. A temporary Gradle init-script experiment was not a valid plugin compatibility proof because init-script classloader isolation conflicted with the repository Kotlin plugin; no repository change was made by that experiment. Coding must treat the real `./gradlew detekt` configuration run as the compatibility gate and must not suppress a plugin/application error with `ignoreFailures`.

## Architecture handoff

ENTITY:
entity_id=issue_72_detekt_gate; type=quality_gate_plan; state=ready_for_architecture

CONTEXT:

* task_id=04.09.2026_06.28.24-b8ff3aac-b3b1-4fa8-a4ed-1bdd5284899b; agent_id=planning; memory_ref=[PROMPT.md,001-planning.md]
* constraints=[detekt_version_2.0.0-alpha.3,all_project_src_roots,project_scoped_baselines,no_feature_source_cleanup,verification_prebuild_block]

ACTION:

1. action=design_gradle_convention; target=issue_72_detekt_gate; params={plugin_scope:kotlin_bearing_subprojects,source_scope:project_src,analysis_mode:light,config_mode:explicit_only}
2. action=design_custom_rule; target=issue_72_detekt_gate; params={rule_id:NoElseIf,match:KtIfExpression_elseExpression_is_KtIfExpression,allowed:[binary_if_else,if_without_else,else_block_nested_if]}
3. action=design_legacy_adoption; target=issue_72_detekt_gate; params={observed_findings:420,affected_modules:37,baseline_scope:per_project,source_cleanup:false}

REASON:

* condition=missing_lint_plugin_and_missing_structural_else_if_rule; requirement=deterministic_four_rule_gate_without_cross_feature_cleanup

EXPECTED RESULT:

* entity_id=issue_72_detekt_gate; new_state=architecture_specifiable; location=agents/task/04.09.2026_06.28.24-b8ff3aac-b3b1-4fa8-a4ed-1bdd5284899b/001-planning.md

VERIFICATION:

* check=acceptance_gate; expected=detekt_rules_test_pass_and_detekt_pass_and_build_pass_and_project_source_coverage_complete

UNCERTAINTY:

* missing=none; ambiguity=none; residual_risk=real_gradle_plugin_integration_requires_coding_gate

REPETITION OF RESULT:

* entity_id=issue_72_detekt_gate; stored_in=shared_memory; status=available

COMMUNICATION:

* sender=planning; receiver=architecture; task_id=04.09.2026_06.28.24-b8ff3aac-b3b1-4fa8-a4ed-1bdd5284899b; message_id=ba772d65-da46-4073-b7d7-40b06a56782d; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[04.09.2026_06.28.24-b8ff3aac-b3b1-4fa8-a4ed-1bdd5284899b,issue_72_detekt_gate,detekt_convention_plan]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
