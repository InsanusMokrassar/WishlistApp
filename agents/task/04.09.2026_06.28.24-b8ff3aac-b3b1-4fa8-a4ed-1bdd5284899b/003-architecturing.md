Model: GPT-5.6 Sol
Changed files: agents/task/04.09.2026_06.28.24-b8ff3aac-b3b1-4fa8-a4ed-1bdd5284899b/003-architecturing.md

GPT-5.6 Sol is the preferred high-level model for Architecturing under `agents/SHORTCUTS.md` and `agents/MODELS.md`. This stage requires exact Gradle lifecycle design, Detekt 2 API resolution, KMP input coverage, safe baseline ownership, and failure-path test planning.

## Architecture result

Issue #72 is ready for Coding with no operator blocker. The implementation is a root-owned Detekt convention plus one pure Kotlin/JVM build-tooling module. The convention applies to every project that applies `org.jetbrains.kotlin.jvm`, `org.jetbrains.kotlin.multiplatform`, or `org.jetbrains.kotlin.android`; the ordinary light-analysis `detekt` task scans every Kotlin file recursively below the owning project’s `src` directory. This input boundary includes `commonMain`, `commonTest`, `jvmMain`, `jvmTest`, `jsMain`, `jsTest`, `androidMain`, Android `main`, and any future source-set directory without maintaining a source-set allowlist.

Use Detekt `2.0.0-alpha.3`. The official compatibility table identifies that release as built against the repository’s exact Gradle `9.3.1` and Kotlin `2.3.21` versions. The rule is syntax-only, so the aggregate gate must use Detekt’s ordinary non-type-resolving task rather than Android variant or KMP compilation tasks. The official Detekt 2 extension contract confirms the `dev.detekt` plugin ID and Maven group, `compileOnly` API dependency, `RuleSetProvider` service loading, `RuleSet(RuleSetId, List<(Config) -> Rule>)` provider shape, `Rule(Config, description)` base constructor, `Finding(Entity.from(...), message)` reporting API, `detektPlugins` integration, and an explicit producer-JAR task dependency.

The checked-in source stays untouched. Existing public-KDoc debt is accepted only through unique project baselines. The new custom rule has no baseline entry, and no default Detekt rules beyond the requested four become active.

## Exact permanent change set

Coding may create or edit only the following permanent files for the implementation:

1. `gradle/libs.versions.toml`
2. `settings.gradle`
3. `build.gradle`
4. `config/detekt/detekt.yml`
5. `detekt-rules/build.gradle`
6. `detekt-rules/src/main/kotlin/dev/inmo/wishlist/detekt/NoElseIf.kt`
7. `detekt-rules/src/main/kotlin/dev/inmo/wishlist/detekt/WishlistRuleSetProvider.kt`
8. `detekt-rules/src/main/resources/META-INF/services/dev.detekt.api.RuleSetProvider`
9. `detekt-rules/src/test/kotlin/dev/inmo/wishlist/detekt/NoElseIfTest.kt`
10. `detekt-rules/src/test/kotlin/dev/inmo/wishlist/detekt/WishlistRuleSetProviderTest.kt`
11. `agents/VERIFICATION.md`
12. The 37 baseline files named in the baseline section below.
13. The Coding step report required by the role protocol.

No feature Kotlin source, feature resource, feature README, root README, dependency lock, generated report, or build output belongs in the final diff. Coding is explicitly authorized by the operator’s issue scope and this Architecture handoff to edit `agents/VERIFICATION.md` for the blocking Detekt stage, despite the usual Coding-role file restriction. That authorization is limited to the Detekt command, failure branch, step numbering, and report-format delta specified below; every unrelated Verification instruction must be preserved verbatim in meaning.

## Version catalog and module registration

Add the following catalog entries, using one version source for the plugin, API, and tests:

```toml
[versions]
detekt = "2.0.0-alpha.3"

[libraries]
detekt-gradle-plugin = { module = "dev.detekt:detekt-gradle-plugin", version.ref = "detekt" }
detekt-api = { module = "dev.detekt:detekt-api", version.ref = "detekt" }
detekt-test = { module = "dev.detekt:detekt-test", version.ref = "detekt" }
```

Add `classpath libs.detekt.gradle.plugin` to the existing root `buildscript.dependencies` block. Do not add a second Detekt version or a plugin-DSL alias because the repository already centralizes plugin classpaths in that block.

Add `":detekt-rules"` to the `includes` array in `settings.gradle`. The existing settings loop must remain the sole descriptor transformation: the directory is `detekt-rules/`, the Gradle path becomes `:wishlist.detekt-rules`, and the project name becomes `wishlist.detekt-rules`. Do not special-case the new module outside that loop.

## Custom-rule module and API

`detekt-rules/build.gradle` is an isolated Kotlin/JVM 17 module, not KMP and not Android. It applies only `org.jetbrains.kotlin.jvm`, uses `kotlin { jvmToolchain(17) }`, declares `compileOnly libs.detekt.api`, `testImplementation libs.detekt.test`, and `testImplementation kotlin('test-junit')`, and contains no dependency on an application module. It must not apply a shared KMP template because those templates assume KMP source sets.

`NoElseIf` has one public constructor accepting `Config` and extends `Rule(config, DESCRIPTION)`. `visitIfExpression(expression: KtIfExpression)` must call the superclass visitor and report exactly when `expression.\`else\` is another `KtIfExpression`. The finding entity is the outer `expression`, the stable rule ID is the class-derived `NoElseIf`, and the message directs the author to replace the chain with `when` or another non-chained form. A direct binary else branch has a block or another non-if expression and is accepted. An independent conditional inside `else { ... }` has a `KtBlockExpression` as the direct else branch and is accepted. Every chained link is itself visited, so a three-condition chain intentionally produces two findings. The implementation must use PSI structure, never source text, regex, comment inspection, or whitespace assumptions.

`WishlistRuleSetProvider` implements `RuleSetProvider` with `override val ruleSetId = RuleSetId("wishlist")` and `override fun instance() = RuleSet(ruleSetId, listOf(::NoElseIf))`. The service file contains exactly one line:

```text
dev.inmo.wishlist.detekt.WishlistRuleSetProvider
```

All new Kotlin declarations, including test declarations and overridden functions, must have meaningful KDocs satisfying the broader manual requirements in `agents/CODING.md`; baselining newly created build-tool code is prohibited.

## Root Gradle convention and task graph

Create root lifecycle tasks named `detekt` and `detektBaseline`, both in the verification group. Each is an aggregate only; neither analyzes root sources. Define one idempotent `configureDetektProject(Project target)` closure and register that closure through `target.pluginManager.withPlugin(...)` callbacks for all three Kotlin plugin IDs. The closure applies `dev.detekt` once, configures the extension, then adds the target’s ordinary `detekt` and `detektBaseline` task providers to the corresponding root aggregate.

The extension for every matched project has these exact invariants:

```groovy
toolVersion.set(rootProject.libs.versions.detekt.get())
source.setFrom(target.fileTree(target.file("src")) { include "**/*.kt" })
config.setFrom(rootProject.layout.projectDirectory.file("config/detekt/detekt.yml"))
buildUponDefaultConfig.set(false)
allRules.set(false)
disableDefaultRuleSets.set(false)
ignoreFailures.set(false)
failOnSeverity.set(dev.detekt.gradle.extensions.FailOnSeverity.Error)
basePath.set(rootProject.layout.projectDirectory)
baseline.set(rootProject.layout.projectDirectory.file("config/detekt/baselines/${target.name}.xml"))
```

Keep the plugin’s per-project report directory under each project’s `build/reports/detekt`; report files are generated artifacts and are not committed. Do not aggregate `detektMain`, `detektTest`, `detekt<Variant>`, or their baseline variants. The issue needs one deterministic syntax gate, not a type-resolution or Android-variant matrix.

For every target except `:wishlist.detekt-rules`, add `detektPlugins(target.dependencies.project(path: ":wishlist.detekt-rules"))`. Every consumer task of type `dev.detekt.gradle.Detekt` and `dev.detekt.gradle.DetektCreateBaselineTask` must also `dependsOn(":wishlist.detekt-rules:jar")`, following Detekt’s in-repository extension guidance. The producer project still applies Detekt and runs the three built-in KDoc rules against its own `src`; it adds neither its own `detektPlugins` project dependency nor a dependency from its Detekt tasks to its own JAR. That single branch prevents both configuration self-dependency and the `detekt -> jar -> check -> detekt` class of cycle. The producer JAR itself does not depend on `check`, so consumer `detekt -> producer:jar` remains acyclic.

The current 49 subprojects all apply one of the three Kotlin plugins, including the source-empty `server` launcher. After adding `detekt-rules`, the root aggregate therefore owns 50 subproject `detekt` dependencies. Forty-nine projects are expected to contain Kotlin after the new module is added; the empty server task remains harmless and makes future server source automatically covered.

## Detekt configuration

`config/detekt/detekt.yml` contains only configuration validation and the four requested rules:

```yaml
config:
  validation: true
  warningsAsErrors: true

comments:
  active: true
  UndocumentedPublicClass:
    active: true
  UndocumentedPublicFunction:
    active: true
  UndocumentedPublicProperty:
    active: true

wishlist:
  active: true
  NoElseIf:
    active: true
```

`warningsAsErrors` applies to invalid/deprecated configuration warnings; finding failure remains controlled by `failOnSeverity=Error`. `buildUponDefaultConfig=false` and `allRules=false` ensure unrelated default rules do not leak into the gate. The three built-in rules mechanically cover public classes/objects/interfaces, public functions, and public properties only. Validator remains responsible for KDoc meaning, constructor/return tags, non-public declarations, and other manual Coding rules.

## Project baselines

The baseline formula is `config/detekt/baselines/${project.name}.xml`, where `project.name` is the full dotted name produced by `settings.gradle`. The formula is collision-free for the current project graph and avoids the cross-module signature collision that reduced a shared baseline from 420 findings to 321 IDs.

After the final convention, custom-rule JAR, and KDoc-complete tests exist, run the focused tests first and then run the root `detektBaseline` aggregate. Remove generated empty baseline files and retain exactly these 37 files:

```text
config/detekt/baselines/wishlist.client.xml
config/detekt/baselines/wishlist.client.android.xml
config/detekt/baselines/wishlist.features.admin.client.xml
config/detekt/baselines/wishlist.features.admin.common.xml
config/detekt/baselines/wishlist.features.admin.server.xml
config/detekt/baselines/wishlist.features.auth.client.xml
config/detekt/baselines/wishlist.features.auth.common.xml
config/detekt/baselines/wishlist.features.auth.server.xml
config/detekt/baselines/wishlist.features.booking.client.xml
config/detekt/baselines/wishlist.features.booking.common.xml
config/detekt/baselines/wishlist.features.booking.server.xml
config/detekt/baselines/wishlist.features.common.client.xml
config/detekt/baselines/wishlist.features.common.common.xml
config/detekt/baselines/wishlist.features.common.server.xml
config/detekt/baselines/wishlist.features.currency.client.xml
config/detekt/baselines/wishlist.features.currency.common.xml
config/detekt/baselines/wishlist.features.currency.server.xml
config/detekt/baselines/wishlist.features.deeplinks.client.xml
config/detekt/baselines/wishlist.features.deeplinks.common.xml
config/detekt/baselines/wishlist.features.email.common.xml
config/detekt/baselines/wishlist.features.files.client.xml
config/detekt/baselines/wishlist.features.files.common.xml
config/detekt/baselines/wishlist.features.files.server.xml
config/detekt/baselines/wishlist.features.roles.client.xml
config/detekt/baselines/wishlist.features.roles.common.xml
config/detekt/baselines/wishlist.features.sample.client.xml
config/detekt/baselines/wishlist.features.sample.common.xml
config/detekt/baselines/wishlist.features.sample.server.xml
config/detekt/baselines/wishlist.features.ui.adminPanel.xml
config/detekt/baselines/wishlist.features.ui.auth.xml
config/detekt/baselines/wishlist.features.ui.sample.xml
config/detekt/baselines/wishlist.features.ui.serverUrl.xml
config/detekt/baselines/wishlist.features.ui.wishlist.xml
config/detekt/baselines/wishlist.features.users.client.xml
config/detekt/baselines/wishlist.features.users.common.xml
config/detekt/baselines/wishlist.features.wishlist.client.xml
config/detekt/baselines/wishlist.features.wishlist.common.xml
```

Validation must find 37 baseline files and 420 total `CurrentIssues` IDs: 172 `UndocumentedPublicClass`, 48 `UndocumentedPublicFunction`, and 200 `UndocumentedPublicProperty`. `ManuallySuppressedIssues` stays empty. Any rule ID other than those three, any `NoElseIf` ID, any entry for a new `detekt-rules` declaration, or any count drift before intentional source work is a hard failure. Do not hand-edit IDs to make the count pass; regenerate after fixing the convention or new-code documentation.

## Verification-stage blocking text

Insert a Detekt step immediately after Verification reads the latest report and before the existing build command:

```bash
set -o pipefail
./gradlew detekt 2>&1 | tee /tmp/detekt-output.txt
echo "detekt_exit=$?"
```

The surrounding instruction must say: record the real Detekt exit code and all findings; if the exit code is nonzero, write `result=FAIL`, hand back to Coding, and do not start `./gradlew build` or any explicit test task. Renumber the current build, test parsing, build-failure, test-failure, and success steps without changing their behavior. Extend the report template with a `### Detekt` section before `### Build`, containing `Exit code: 0 | <N>` and findings/errors. Preserve `set -o pipefail` independently for both lint and build pipelines. Do not combine lint and build with shell `&&`, because Verification must record the Detekt exit and findings before returning.

## Automated test specifications

`NoElseIfTest` uses `NoElseIf(Config.empty).lint(code)` from `dev.detekt.test` and Kotlin test assertions. Each test constructs a fresh rule instance or otherwise guarantees no finding state leaks between cases.

The rejected-case tests must prove: a braced statement-body chain reports one finding; an expression-body chain reports one; line breaks between `else` and `if` report one; a comment between `else` and `if` reports one; and a three-condition chain reports two. Assert `subject.ruleName == RuleName("NoElseIf")`, each finding message, and outer-if source locations, not only non-emptiness. Finding order must not be assumed for the two-link case; compare location sets.

The accepted-case tests must prove zero findings for a binary `if`/`else`, an `if` without `else`, and an independent `if` inside an explicit `else { ... }` block. Add a string literal and a comment containing the characters `else if` to one accepted fixture to prove that PSI matching cannot regress into text matching.

`WishlistRuleSetProviderTest` loads `RuleSetProvider` through `ServiceLoader`, selects the Wishlist provider, asserts `RuleSetId("wishlist")`, asserts that the produced rules contain exactly `NoElseIf`, and lints a minimal chain with the service-created rule. This test covers provider construction, rule factory registration, the exact service-resource path, and the exact provider FQCN.

The Gradle integration proof must perform all of the following:

1. `./gradlew :wishlist.detekt-rules:test` passes all positive, negative, and service-loading cases.
2. `./gradlew detekt --dry-run` shows the root aggregate and all 50 Kotlin-plugin subproject `detekt` tasks, including `:wishlist.server:detekt` and `:wishlist.detekt-rules:detekt`.
3. A temporary Groovy init script under `/tmp`, using task-name lookup rather than importing Detekt classes, compares each configured project’s ordinary `detekt.source.files` canonical-path set with that project’s recursively computed `src/**/*.kt` canonical-path set. The comparison must report 50 configured projects, 49 source-bearing projects, `missing=[]`, and `extra=[]`. Compute the Kotlin file count at runtime rather than hard-coding 690, because the four new rule/test files increase the inventory.
4. A temporary `server/src/test/kotlin/DetektFailureProbe.kt` declares one undocumented public class, one undocumented public member property, one undocumented public function, and one structural `else if` chain. With no server baseline, the exact Verification lint pipeline must exit nonzero and `/tmp/detekt-output.txt` must identify `UndocumentedPublicClass`, `UndocumentedPublicProperty`, `UndocumentedPublicFunction`, and `NoElseIf` in `:wishlist.server:detekt`. Remove the probe completely, confirm `git status` has no probe, then rerun `./gradlew detekt` and require exit zero. The temporary probe is failure injection only and must never be committed.
5. `./gradlew build` passes after the clean Detekt run. Because each project’s `check` also receives the plugin’s ordinary Detekt dependency, the build provides a second up-to-date quality-gate traversal without adding variant tasks.
6. Baseline validation reports exactly the 37 files and 420 allowed IDs described above, with zero custom-rule IDs.
7. `git diff --check` passes and `ast-index rebuild` succeeds after the new Kotlin files are final.

The failure-injection test proves the custom rule is loaded through the real Gradle consumer configuration, the three built-in rules are active, the server project receives the convention despite initially having no source, nonzero lint status survives `tee`, and the documented Verification branch blocks the build. No planned behavior is untestable; operator confirmation is not required before Coding.

## README updates

No feature README update is authorized or required. The change adds repository build tooling and agent Verification instructions without changing a feature route, model, behavior, ownership rule, or module dependency.

## Risks and stop conditions

Coding must stop and return a blocker rather than weakening the gate if Detekt `2.0.0-alpha.3` cannot apply on the real build, if the custom provider is not service-loaded, if consumer tasks cannot depend on the producer JAR without a cycle, if recursive `src` input equality fails, if any `NoElseIf` baseline entry is generated, if the clean aggregate Detekt command is nonzero, or if the full build fails because of the integration. `ignoreFailures`, rule deactivation, a shared baseline, source exclusion, feature-source KDoc filling, and deletion of unrelated Verification instructions are not acceptable workarounds.

Implementation is complete only when the exact permanent file scope is respected, the four rules are active, every Kotlin project source tree is covered, the project baselines contain legacy KDoc findings only, the provider and rule tests pass, the injected violation makes the Verification lint command fail before build, the probe is removed, the clean lint and build gates pass, and the final diff contains no feature source or README cleanup. Broader Detekt policy, formatting, autocorrection, type resolution, legacy documentation cleanup, and other Coding conventions belong to separate issues.

Official primary references used to resolve the design are the Detekt `2.0.0-alpha.3` [compatibility and changelog](https://detekt.dev/changelog-2.0.0/), [Gradle plugin guide](https://detekt.dev/docs/2.0.0-alpha.3/gettingstarted/gradle/), [extension guide](https://detekt.dev/docs/2.0.0-alpha.3/introduction/extensions/), [configuration guide](https://detekt.dev/docs/2.0.0-alpha.3/introduction/configurations/), [baseline guide](https://detekt.dev/docs/2.0.0-alpha.3/introduction/baseline/), [comments-rule reference](https://detekt.dev/docs/2.0.0-alpha.3/rules/comments/), and the version-pinned official [sample provider source](https://github.com/detekt/detekt/blob/v2.0.0-alpha.3/detekt-sample-extensions/src/main/kotlin/io/gitlab/arturbosch/detekt/sample/extensions/SampleProvider.kt).

## Coding handoff

ENTITY:
entity_id=issue_72_detekt_architecture; type=build_quality_gate_architecture; state=ready_for_coding

CONTEXT:

* task_id=04.09.2026_06.28.24-b8ff3aac-b3b1-4fa8-a4ed-1bdd5284899b; agent_id=architecturing; memory_ref=[PROMPT.md,001-planning.md,002-planning.md,003-architecturing.md]
* constraints=[detekt_version=2.0.0-alpha.3,gradle_version=9.3.1,kotlin_version=2.3.21,source_scope=project_src_recursive,baseline_scope=project_name_unique,feature_source_edits=zero,feature_readme_edits=zero]

ACTION:

1. action=implement_catalog_and_module; target=issue_72_detekt_architecture; params={module_path=:wishlist.detekt-rules,module_type=kotlin_jvm_17,aliases=[detekt_gradle_plugin,detekt_api,detekt_test]}
2. action=implement_root_convention; target=issue_72_detekt_architecture; params={plugin_callbacks=[kotlin_jvm,kotlin_multiplatform,kotlin_android],aggregate_tasks=[detekt,detektBaseline],source_pattern=src/**/*.kt,fail_on_severity=error}
3. action=implement_custom_rule; target=issue_72_detekt_architecture; params={ruleset_id=wishlist,rule_id=NoElseIf,match=direct_else_branch_is_KtIfExpression,service_provider=dev.inmo.wishlist.detekt.WishlistRuleSetProvider}
4. action=generate_legacy_baselines; target=issue_72_detekt_architecture; params={baseline_files=37,baseline_ids=420,allowed_rule_ids=[UndocumentedPublicClass,UndocumentedPublicFunction,UndocumentedPublicProperty],NoElseIf_ids=0}
5. action=edit_verification_stage; target=agents/VERIFICATION.md; params={authorization=explicit_issue_72_exception,order=detekt_before_build,pipefail=true,lint_failure_blocks_build=true,preserve_unrelated_instructions=true}
6. action=execute_acceptance_proof; target=issue_72_detekt_architecture; params={rule_tests=pass,service_test=pass,input_set_difference=empty,failure_injection=nonzero,clean_detekt=pass,build=pass,ast_index_rebuild=pass}

REASON:

* condition=mechanical_else_if_and_public_kdoc_rules_absent; requirement=deterministic_prebuild_enforcement
* condition=kmp_sources_outside_default_detekt_paths; requirement=recursive_project_src_input
* condition=shared_baseline_signature_collisions_present; requirement=unique_project_baseline_files
* condition=in_repository_rule_jar_required; requirement=consumer_jar_dependency_and_producer_self_dependency_exclusion

EXPECTED RESULT:

* entity_id=issue_72_detekt_architecture; new_state=implemented_and_verifiable; location=repository_build_tooling_and_agents/VERIFICATION.md
* entity_id=issue_72_detekt_architecture; new_state_constraints=[four_rules_active,configured_projects=50,baseline_files=37,legacy_ids=420,NoElseIf_baseline_ids=0,feature_source_edits=0]

VERIFICATION:

* check=custom_rule_unit_contract; expected={rejected_cases=[braced,expression,multiline,commented_separator,multi_link],allowed_cases=[binary_else,no_else,explicit_else_block],service_loading=pass}
* check=gradle_gate_contract; expected={producer_self_dependency=false,consumer_jar_dependency=true,source_set_difference=empty,failure_probe_exit=nonzero,clean_detekt_exit=0,build_exit=0}
* check=scope_contract; expected={permanent_files=authorized_list_only,feature_source_changes=zero,feature_readme_changes=zero,verification_unrelated_changes=zero}

UNCERTAINTY:

* missing=none; ambiguity=none; residual_risk=real_gradle_plugin_application_and_task_graph_require_coding_execution

REPETITION OF RESULT:

* entity_id=issue_72_detekt_architecture; stored_in=shared_memory; status=available

COMMUNICATION:

* sender=architecturing; receiver=coding; task_id=04.09.2026_06.28.24-b8ff3aac-b3b1-4fa8-a4ed-1bdd5284899b; message_id=9b5f7d8c-bf79-4741-8a7b-91cf42c3deb9; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[task_id=04.09.2026_06.28.24-b8ff3aac-b3b1-4fa8-a4ed-1bdd5284899b,entity_id=issue_72_detekt_architecture,intent=detekt_quality_gate_architecture]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
