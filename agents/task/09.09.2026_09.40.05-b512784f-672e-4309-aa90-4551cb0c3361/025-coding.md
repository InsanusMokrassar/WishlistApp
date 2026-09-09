Model: GPT-5.6 Codex (ML; assigned continuation coding model)
Changed files: features/ui/users/build.gradle, gradle/libs.versions.toml, features/ui/users/src/jvmMain/kotlin/ui/PasswordChangeView.kt, features/ui/users/src/androidMain/kotlin/ui/PasswordChangeView.kt, features/ui/users/src/jvmTest/kotlin/ui/PasswordChangeViewTest.kt, features/ui/users/src/androidUnitTest/kotlin/ui/PasswordChangeViewTest.kt, agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/025-coding.md

The Coding role continued from clean commit `5b975ad169ed32e2c115e878cba9131e6a44da3a` on `fix/issue-78-email-authorized-password-change`. It used the repository-required ast-index navigation and rebuilt the index after Kotlin source changes. No nested agent was used.

## T13 JVM and Android factory-host coverage completed

The JVM test mounts the concrete view returned by the actual `JVMPlugin` typed factory through `runComposeUiTest` and `setContent`. The Android test mounts the concrete view returned by the actual `AndroidPlugin` typed factory in a directly created Robolectric `ComponentActivity`, using `RobolectricTestRunner`, SDK 35, and `createEmptyComposeRule`. Both fixtures use real Koin factory registration, a real navigation lifecycle, and controlled model/interactor seams. They explicitly destroy the navigation node and activity, cancel and join lifecycle jobs, and close Koin.

Pending and completed configs preserve their exact configuration in both views. Pending tests prove visible labels, password semantics, IME Done semantics, text input, live mismatch and policy feedback, disabled submit while a deferred completion response holds the admitted request busy, and exactly one submitted DTO. Completed tests prove success content and no editable password semantics or password labels. Compose semantics expose password and IME action but do not expose a single-line property; the production fields retain `singleLine = true` and the observable password/Done APIs are asserted.

The host tests exposed two production parity gaps. JVM and Android originally configured the first password field as `ImeAction.Next`, despite the architecture requirement that both native fields expose guarded Done; both fields now use password keyboard, Done, single line, and `KeyboardActions.onDone`. Native views also now render mismatch and policy feedback independently, matching the already-covered JS behavior rather than suppressing policy guidance when both conditions apply. `onDraw` is public on both native views so the test renders the actual returned view without recreating a form.

Android unit resources are enabled only for this module. Test-only catalog entries add Robolectric 4.16 and AndroidX Compose UI test JUnit4 1.11.2 to `androidUnitTest`; no manifest, emulator, release runtime dependency, screenshot, or Java `--add-opens` change was needed.

## Verification

- Fresh unfiltered `./gradlew :wishlist.features.ui.users:jvmTest --console=plain` — passed; XML records 40 tests, 0 skipped, 0 failures, and 0 errors, including `PasswordChangeViewTest[jvm]` with 2 tests, 0 skipped, 0 failures, and 0 errors.
- Fresh unfiltered `./gradlew :wishlist.features.ui.users:testDebugUnitTest --console=plain` — passed; XML records 40 tests, 0 skipped, 0 failures, and 0 errors, including `PasswordChangeViewTest` with 2 tests, 0 skipped, 0 failures, and 0 errors.
- Fresh unfiltered `./gradlew :wishlist.features.ui.users:testReleaseUnitTest --console=plain` — passed; XML records 40 tests, 0 skipped, 0 failures, and 0 errors, including `PasswordChangeViewTest` with 2 tests, 0 skipped, 0 failures, and 0 errors.
- `./gradlew :wishlist.features.ui.users:compileTestKotlinJvm :wishlist.features.ui.users:compileKotlinJvm :wishlist.features.ui.users:compileDebugKotlinAndroid :wishlist.features.ui.users:compileReleaseKotlinAndroid --console=plain` — passed.
- `ast-index rebuild` — passed after Kotlin source changes; the elevated cache access was required only because the index cache lives outside the workspace sandbox.
- `git diff --check` — passed.

```text
ENTITY:
entity_id=issue_78_coding_025; type=coding_result; state=T13_completed

CONTEXT:
* task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; agent_id=issue78_coding_native; base_head=5b975ad169ed32e2c115e878cba9131e6a44da3a; branch=fix/issue-78-email-authorized-password-change
* constraints=[actual_platform_typed_factories,actual_production_views,JVM_Compose_UI_test,Robolectric_SDK_35,debug_release_execution,no_screenshots,no_test_tags,no_push]; ast_index_rebuilt=true

ACTION:
1. action=add_test; target=jvm_PasswordChangeViewTest; params={host=runComposeUiTest_setContent,factory=JVMPlugin_typed_factory,cases=[Pending,Completed],assertions=[labels,password_semantics,IME_Done,mismatch_policy,busy_disabled,one_DTO,no_password_inputs]}
2. action=add_test; target=android_PasswordChangeViewTest; params={host=Robolectric_ComponentActivity_createEmptyComposeRule,factory=AndroidPlugin_typed_factory,sdk=35,cases=[Pending,Completed],lifecycle=[activity_finish,node_destroy,chain_cancel_join,Koin_close]}
3. action=configure; target=ui_users_androidUnitTest; params={includeAndroidResources=true,deps=[Robolectric_4.16,androidx_compose_ui_test_junit4_1.11.2],catalog_entries=true,java_add_opens=not_required}
4. action=correct; target=[JVM_PasswordChangeView,Android_PasswordChangeView]; params={first_password=[KeyboardType.Password,ImeAction.Done,singleLine,true_onDone],feedback=[mismatch,policy_independent],onDraw=public}

VERIFICATION:
* check=focused_JVM_PasswordChangeViewTest; expected=[2_tests,0_failures,0_errors]; result=pass
* check=focused_debug_Robolectric_PasswordChangeViewTest; expected=[2_tests,0_failures,0_errors]; result=pass
* check=focused_release_Robolectric_PasswordChangeViewTest; expected=[2_tests,0_failures,0_errors]; result=pass
* check=unfiltered_platform_XML; expected=[JVM:40,debug:40,release:40,skipped:0,failures:0,errors:0]; result=pass
* check=JVM_Android_compilation; expected=pass; result=pass
* check=ast_index_rebuild; expected=pass; result=pass
* check=diff_whitespace; expected=pass; result=pass

UNCERTAINTY:
* missing=[]; ambiguity=singleLine_semantics_key_absent_from_Compose_test_API
* acceptance_status=T13_complete; blocker=none

REPETITION OF RESULT:
* entity_id=issue_78_coding_025; stored_in=tracked_step_report; status=T13_completed

COMMUNICATION:
* sender=issue78_coding_native; receiver=orchestrator; task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; message_id=025-coding-result; protocol=AML-HIP

PERSISTENCE:
* local_memory=false; shared_memory=true; index_keys=[task_id,issue_78_coding_025,T13,JVM_Compose,Robolectric_Android]

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
```
