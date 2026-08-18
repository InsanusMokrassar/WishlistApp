Model: OpenAI Terra (ML)
Changed files: features/auth/README.md; agents/task/18.08.2026_16.29.02-5f520804-25a3-4c54-9b26-a2350134bad9/006-coding.md

OpenAI Terra is the preferred ML choice for this follow-up Coding stage because the work required careful interpretation of Gradle dependency metadata and commit-history evidence, rather than a broad, speculative toolchain migration. The ML tier matches the Coding-role priority and is sufficient to distinguish a task-induced regression from a pre-existing repository configuration defect.

Corrected the stale `RegistrationEmailSender` description in the Auth feature README. The documentation now correctly states that a successful required-email registration stores the password and returns credential-free `pendingEmailVerification`; normal login after verification is the first credential-producing action.

The Android metadata failure is conclusively pre-existing. A detached worktree at parent commit `f33dac0` (`0bb31b1^`) was checked with `:wishlist.client.android:checkDebugAarMetadata` and failed with the same six metadata violations as the current commit. The parent and `0bb31b1` differ in no Gradle, version-catalog, dependency, or Android configuration file. Existing catalog values are `compileSdk=36`, `android-gradle-plugin=8.12.3`, and `kroles=0.0.3`; the unchanged dependency graph resolves `dev.inmo:kroles.roles-android:0.0.3`, `dev.inmo:kroles.repos-android:0.0.3`, and `androidx.core:core[-ktx]:1.19.0`. The kroles artifacts require compile SDK 37, and AndroidX core additionally requires Android Gradle Plugin 9.1 or later.

No dependency or toolchain change was made. Raising both compile SDK and AGP would be a project-wide upgrade outside this Auth/registration task. Pinning or excluding AndroidX or kroles transitive artifacts would change the unrelated application-wide Compose/Roles dependency policy without evidence that the altered graph remains compatible. The safe resolution therefore requires an operator-approved Android toolchain/dependency update in a dedicated task.

The focused test matrix passed again:

- `./gradlew :wishlist.features.auth.common:allTests :wishlist.features.auth.client:allTests :wishlist.features.auth.server:jvmTest :wishlist.features.roles.server:jvmTest :wishlist.features.email.server:jvmTest :wishlist.features.ui.auth:allTests --console=plain --quiet`

`./gradlew build --rerun-tasks --console=plain --quiet` exited successfully in the current task graph, but `build --dry-run` did not schedule `:wishlist.client.android:checkDebugAarMetadata`. The forced Android metadata command below is the authoritative fresh Android validation and still fails:

- `./gradlew :wishlist.client.android:checkDebugAarMetadata --rerun-tasks --console=plain --quiet`

No Kotlin source changed, so `ast-index rebuild` was not required for this iteration.

ENTITY:
entity_id=android_metadata_baseline_incompatibility; type=build_toolchain_blocker; state=confirmed_preexisting

CONTEXT:
* task_id=18.08.2026_16.29.02-5f520804-25a3-4c54-9b26-a2350134bad9; agent_id=coding; baseline_commit=f33dac0; current_coding_commit=0bb31b1
* constraints=[no_unjustified_toolchain_upgrade,PROMPT_md_untracked,task_scope_auth_roles_email_ui]

ACTION:
1. action=compare; target=commit_diff_f33dac0_to_0bb31b1; params={gradle_or_dependency_files_changed=false}
2. action=execute; target=baseline_client_android_checkDebugAarMetadata; params={result=failed,issues=6,metadata_requirements=[compileSdk_37,AGP_9_1]}
3. action=execute; target=current_client_android_checkDebugAarMetadata_rerun; params={result=failed,issues=6,metadata_requirements=[compileSdk_37,AGP_9_1]}
4. action=update; target=features_auth_README_RegistrationEmailSender; params={result=credential_free_pending_description}

REASON:
* condition=baseline_and_current_metadata_failures_identical; requirement=task_induced_regression_rejected
* condition=required_fix_changes_global_Android_toolchain_or_dependency_policy; requirement=operator_approved_dedicated_scope

EXPECTED RESULT:
* entity_id=android_metadata_baseline_incompatibility; new_state=operator_decision_required; location=gradle_version_catalog_and_dependency_policy
* entity_id=features_auth_README_RegistrationEmailSender; new_state=accurate; location=features/auth/README.md

VERIFICATION:
* check=focused_gradle_test_matrix; expected=passed
* check=baseline_android_metadata_task; expected=failed_with_identical_6_issues
* check=current_android_metadata_task_rerun; expected=failed_with_identical_6_issues

REPETITION OF RESULT:
* entity_id=android_metadata_baseline_incompatibility; stored_in=shared_step_file; status=available

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
