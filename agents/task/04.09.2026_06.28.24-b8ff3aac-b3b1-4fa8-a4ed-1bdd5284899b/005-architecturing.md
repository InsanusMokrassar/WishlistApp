Model: GPT-5.6 Sol
Changed files: agents/task/04.09.2026_06.28.24-b8ff3aac-b3b1-4fa8-a4ed-1bdd5284899b/005-architecturing.md

GPT-5.6 Sol remains the preferred high-level model for Architecturing under `agents/SHORTCUTS.md` and `agents/MODELS.md`. This correction requires independent evidence reconciliation across raw Detekt findings, baseline serialization, Gradle project lifecycle timing, and custom-rule configuration validation.

## Architecture correction result

The implementation in commit `1ea5d9530938f127e0dfd0d216398420266eb6f9` conforms to issue #72 and requires no source, build-tool, baseline, or Verification-instruction change. The only correction is an acceptance-count distinction: the repository has 420 raw legacy KDoc findings, and those findings serialize to 418 unique project-scoped Detekt baseline IDs. The earlier requirement in `003-architecturing.md` for 420 serialized IDs is superseded by this report. Every other scope boundary, rule behavior, test requirement, source-coverage requirement, and Verification ordering requirement from `003-architecturing.md` remains active.

Coding must add a report-only acknowledgement of the corrected acceptance before Verification begins. The acknowledgement must state that no implementation edit is required, preserve commit `1ea5d953` unchanged, and hand the existing implementation plus the corrected `420 raw / 418 serialized` criterion to Verification.

## Independent baseline validation

The no-baseline diagnostic contains exactly 420 raw findings: 172 `UndocumentedPublicClass`, 48 `UndocumentedPublicFunction`, and 200 `UndocumentedPublicProperty`. The 37 checked-in project baseline files contain exactly 418 unique IDs: 170 `UndocumentedPublicClass`, 48 `UndocumentedPublicFunction`, and 200 `UndocumentedPublicProperty`. Every baseline has an empty `ManuallySuppressedIssues` collection, no baseline contains `NoElseIf`, and the custom-rule module contributes no baseline entry.

The two-ID difference is fully explained inside `:wishlist.features.ui.sample`. These three distinct legacy declarations are reported independently by a no-baseline scan:

```text
features/ui/sample/src/androidMain/kotlin/ui/SampleView.kt:17
features/ui/sample/src/jsMain/kotlin/ui/SampleView.kt:14
features/ui/sample/src/jvmMain/kotlin/ui/SampleView.kt:14
```

Detekt serializes all three findings to the same project-local baseline identity:

```text
UndocumentedPublicClass:SampleView.kt:SampleView : ComposeView
```

An XML baseline represents identity membership rather than source-occurrence multiplicity. Three raw occurrences therefore require one serialized identity, producing `172 - 2 = 170` class IDs and `420 - 2 = 418` total IDs. Repeating the identical XML ID would not restore occurrence identity and would fabricate redundant baseline data. Project-scoped files remain necessary: the collision is contained within the one UI sample project and cannot suppress a same-signature declaration in another Gradle project.

No rule is weakened by correcting the serialized count. Removing baselines still yields all 420 raw KDoc findings, the generated baselines suppress only the intended legacy identities, and a new `NoElseIf` violation cannot be suppressed because no `NoElseIf` baseline identity exists. The Coding failure injection also demonstrated that a project without a legacy baseline fails on `UndocumentedPublicClass`, `UndocumentedPublicFunction`, `UndocumentedPublicProperty`, and `NoElseIf`. The unavoidable same-project `SampleView` identity collision is a limitation of Detekt’s baseline signature format, not rule deactivation, severity reduction, source exclusion, or `ignoreFailures` behavior.

The corrected baseline acceptance is therefore exact: `raw_findings=420`, `raw_rule_counts={UndocumentedPublicClass=172,UndocumentedPublicFunction=48,UndocumentedPublicProperty=200}`, `baseline_files=37`, `serialized_ids=418`, and `serialized_rule_counts={UndocumentedPublicClass=170,UndocumentedPublicFunction=48,UndocumentedPublicProperty=200}`.

## Gradle lifecycle assessment

Coding’s `afterEvaluate` deferral is acceptable for the current repository and is required by observed Detekt 2/Kotlin Android integration behavior. Applying `dev.detekt` directly inside the Kotlin plugin callback raced Kotlin Android target creation and raised `KotlinPluginLifecycle$IllegalLifecycleException: Future was not completed yet`. The root build registers every `withPlugin` callback before subproject evaluation, each repository Kotlin plugin is applied during its project build script, and `afterEvaluate` runs after that target project’s build script and Kotlin target declarations complete but before Gradle populates the execution graph.

The `configuredDetektProjects` set makes the three plugin callbacks idempotent if more than one matching Kotlin plugin ID appears. The deferred closure still creates and wires every task in time for selection: an independent focused run reached the root aggregate, all 50 subproject `detekt` tasks, the source-empty server task, the producer task, and the producer tests, and completed successfully. The implementation does not defer task mutation into execution time and does not leave any Kotlin project unconfigured.

Gradle documents `afterEvaluate` as a supported parent-to-child configuration hook that runs immediately after a project is evaluated, while generally preferring newer lifecycle APIs for new reusable build logic. Replacing the working compatibility deferral with a larger convention-plugin or isolated-lifecycle migration is outside issue #72 and has no enforcement benefit. The current deferral is constrained to Detekt plugin application, is demonstrably deterministic in this build, and requires no Coding change.

## Configuration-validation exclusion assessment

`config.excludes: ['wishlist.*']` is acceptable. The YAML file is shared, so the exclusion is physically visible to producer and consumer tasks; “producer-only” describes the reason for the exclusion rather than a separate producer file. The producer intentionally does not put its own JAR on its `detektPlugins` classpath to avoid self-dependency. Without the provider, validation cannot resolve the custom `wishlist` namespace and `warningsAsErrors=true` blocks the producer before the three built-in rules can analyze it.

The exclusion applies only to configuration-schema validation paths. It is not a Detekt source exclude, rule exclude, suppression, baseline entry, activity toggle, severity override, or failure-policy override. The consumer tasks still load `:wishlist.detekt-rules`, read `wishlist.NoElseIf.active=true`, and execute the rule. The service-loader unit test and the real four-rule failure injection prove that `NoElseIf` remains active. Detekt’s official `2.0.0-alpha.3` [configuration guide](https://detekt.dev/docs/2.0.0-alpha.3/introduction/configurations/) separately states that custom rule sets are excluded from configuration validation by default and that `config.excludes` exists to omit extension-owned paths from validation. A separate producer YAML would add maintenance and drift risk without strengthening enforcement.

No Coding change is required for the exclusion. Future custom rules under `wishlist` must continue to have direct unit tests and failure-injection coverage because Detekt’s first-party schema validator does not validate extension-owned properties.

## Acceptance and Verification handoff

Verification must use these corrected invariants:

1. The 37 baseline files contain 418 serialized IDs with counts `170/48/200`, not 420 serialized IDs.
2. A baseline-free diagnostic represents the same legacy state as 420 raw findings with counts `172/48/200`.
3. Exactly three platform `SampleView` findings in `:wishlist.features.ui.sample` map to the one class baseline ID shown above; the mapping accounts for the complete two-ID delta.
4. `NoElseIf` baseline IDs remain zero, manually suppressed IDs remain zero, and `detekt-rules` baseline IDs remain zero.
5. The blocking `./gradlew detekt` command still precedes `./gradlew build`, uses `set -o pipefail`, and returns to Coding without starting the build on nonzero lint status.
6. The custom-rule tests, provider service-loading test, recursive source-set equality proof, 50-project aggregate coverage, clean Detekt run, and full build evidence from Coding remain valid.

The independent focused command `./gradlew :wishlist.detekt-rules:test detekt --console=plain --warning-mode=none` completed successfully and traversed all configured project Detekt tasks. No planned behavior is untestable, no operator question remains, and Verification may start after the required report-only Coding acknowledgement.

## README updates

No feature README update is authorized or required. This Architecture correction changes an acceptance count in task memory only and does not change application behavior, routes, models, ownership, feature dependencies, or implementation files.

## Scope and stop condition

Do not edit commit `1ea5d953`, regenerate baselines, duplicate the `SampleView` ID, change rule severity, alter the YAML exclusion, replace the lifecycle mechanism, fill legacy KDocs, edit feature sources, or edit README files for this correction. Stop the correction cycle after a Coding report acknowledges `420 raw findings / 418 serialized IDs` without implementation changes; Verification then executes the existing blocking gate and build sequence against the unchanged implementation.

## Corrected Coding handoff

ENTITY:
entity_id=issue_72_baseline_acceptance_correction; type=architecture_acceptance_correction; state=ready_for_report_only_coding_acknowledgement

CONTEXT:

* task_id=04.09.2026_06.28.24-b8ff3aac-b3b1-4fa8-a4ed-1bdd5284899b; agent_id=architecturing; memory_ref=[003-architecturing.md,004-coding.md,005-architecturing.md,commit=1ea5d9530938f127e0dfd0d216398420266eb6f9]
* constraints=[implementation_edits=zero,feature_source_edits=zero,readme_edits=zero,raw_findings=420,serialized_ids=418,coding_next_action=report_only]

ACTION:

1. action=correct_acceptance_cardinality; target=issue_72_baseline_acceptance_correction; params={raw_counts={UndocumentedPublicClass=172,UndocumentedPublicFunction=48,UndocumentedPublicProperty=200},serialized_counts={UndocumentedPublicClass=170,UndocumentedPublicFunction=48,UndocumentedPublicProperty=200},baseline_files=37}
2. action=accept_gradle_lifecycle_deferral; target=detekt_root_convention; params={mechanism=afterEvaluate,trigger=Kotlin_plugin_callback,idempotence=configuredDetektProjects,configured_projects=50,coding_change_required=false}
3. action=accept_validation_exclusion; target=config/detekt/detekt.yml; params={path=wishlist.*,scope=config_validation_only,rule_execution_suppressed=false,producer_self_dependency=false,coding_change_required=false}
4. action=request_report_only_acknowledgement; target=coding; params={implementation_commit=1ea5d9530938f127e0dfd0d216398420266eb6f9,implementation_change_required=false,next_stage=verification}

REASON:

* condition=three_project_local_SampleView_findings_share_one_Detekt_identity; requirement=generated_baseline_identity_set_without_fabricated_duplicates → action=accept_418_serialized_ids → result=420_raw_findings_fully_grandfathered
* condition=direct_Detekt_application_races_Kotlin_Android_target_creation; requirement=target_lifecycle_completion_before_Detekt_application → action=retain_afterEvaluate_deferral → result=50_project_gate_configuration_success
* condition=producer_excludes_own_rule_JAR_to_prevent_self_dependency; requirement=producer_KDoc_analysis_with_strict_configuration_warnings → action=exclude_wishlist_namespace_from_schema_validation → result=rule_execution_unchanged

EXPECTED RESULT:

* entity_id=issue_72_baseline_acceptance_correction; new_state=acknowledged_without_implementation_change; location=next_coding_step_report
* entity_id=issue_72_baseline_acceptance_correction; new_state_constraints=[raw_findings=420,serialized_ids=418,NoElseIf_baseline_ids=0,implementation_commit=1ea5d9530938f127e0dfd0d216398420266eb6f9]

VERIFICATION:

* check=baseline_cardinality; expected={files=37,raw_counts={class=172,function=48,property=200},serialized_counts={class=170,function=48,property=200},delta_reason=three_SampleView_findings_to_one_ID}
* check=rule_strength; expected={NoElseIf_active=true,KDoc_rules_active=3,ignoreFailures=false,failOnSeverity=Error,source_exclusions=none}
* check=implementation_conformance; expected={coding_change_required=false,report_only_acknowledgement_required=true,verification_allowed_after_acknowledgement=true}

UNCERTAINTY:

* missing=none; ambiguity=none; residual_risk=Detekt_baseline_identity_collision_limited_to_existing_project_local_SampleView_declarations

REPETITION OF RESULT:

* entity_id=issue_72_baseline_acceptance_correction; stored_in=shared_memory; status=available

COMMUNICATION:

* sender=architecturing; receiver=coding; task_id=04.09.2026_06.28.24-b8ff3aac-b3b1-4fa8-a4ed-1bdd5284899b; message_id=55063d39-acb6-4e45-b2ed-86128daa11b9; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[task_id=04.09.2026_06.28.24-b8ff3aac-b3b1-4fa8-a4ed-1bdd5284899b,entity_id=issue_72_baseline_acceptance_correction,intent=correct_baseline_acceptance]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
