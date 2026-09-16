Model: OpenAI GPT-6 (coding)
Changed files: .templates/client_module_kts/{{$module_path}}/src/commonMain/kotlin/Plugin.kt; .templates/client_module_kts/{{$module_path}}/src/commonMain/kotlin/ui/Default{{$module_ui_name}}Model.kt; agents/patterns/auth-ui.md; agents/patterns/mvvm.md; agents/task/16.09.2026_15.13.30-df86933a-6817-4bda-b1f4-8c6519f749b7/003-coding.md; features/ui/adminPanel/README.md; features/ui/adminPanel/src/commonMain/kotlin/Plugin.kt; features/ui/adminPanel/src/commonMain/kotlin/ui/DefaultAdminPanelModel.kt; features/ui/adminPanel/src/commonTest/kotlin/AdminPanelModelTest.kt; features/ui/auth/README.md; features/ui/auth/src/commonMain/kotlin/Plugin.kt; features/ui/auth/src/commonMain/kotlin/ui/DefaultAuthModel.kt; features/ui/auth/src/commonTest/kotlin/ui/AuthModelTest.kt; features/ui/booking/README.md; features/ui/booking/src/commonMain/kotlin/Plugin.kt; features/ui/booking/src/commonMain/kotlin/ui/DefaultBookingModel.kt; features/ui/booking/src/commonTest/kotlin/ui/BookingModelTest.kt; features/ui/sample/README.md; features/ui/sample/src/commonMain/kotlin/Plugin.kt; features/ui/sample/src/commonMain/kotlin/ui/DefaultSampleModel.kt; features/ui/sample/src/commonTest/kotlin/ui/SampleModelTest.kt; features/ui/serverUrl/README.md; features/ui/serverUrl/src/commonMain/kotlin/Plugin.kt; features/ui/serverUrl/src/commonMain/kotlin/ui/DefaultServerUrlModel.kt; features/ui/serverUrl/src/commonTest/kotlin/ui/ServerUrlModelTest.kt; features/ui/sidebar/README.md; features/ui/sidebar/src/commonMain/kotlin/Plugin.kt; features/ui/sidebar/src/commonMain/kotlin/ui/DefaultSidebarModel.kt; features/ui/sidebar/src/commonTest/kotlin/ui/SidebarModelTest.kt; features/ui/users/README.md; features/ui/users/build.gradle; features/ui/users/src/commonMain/kotlin/Plugin.kt; features/ui/users/src/commonMain/kotlin/ui/DefaultUsersModel.kt; features/ui/users/src/commonTest/kotlin/UsersModelTest.kt; features/ui/users/src/jvmTest/kotlin/UsersModelFileTest.kt; features/ui/wishlist/README.md; features/ui/wishlist/build.gradle; features/ui/wishlist/src/commonMain/kotlin/Plugin.kt; features/ui/wishlist/src/commonMain/kotlin/ui/DefaultWishlistsModel.kt; features/ui/wishlist/src/commonTest/kotlin/ui/WishlistsModelTest.kt; features/ui/wishlist/src/jvmTest/kotlin/ui/WishlistsModelFileTest.kt

## Outcome

Issue #83 is implemented without changing any public Model interface or observable production behavior. The eight anonymous Plugin implementations now live in public common-source classes named `DefaultAdminPanelModel`, `DefaultAuthModel`, `DefaultBookingModel`, `DefaultSampleModel`, `DefaultServerUrlModel`, `DefaultSidebarModel`, `DefaultUsersModel`, and `DefaultWishlistsModel`. Each class receives every previously captured outside-world dependency through a primary-constructor `private val`, and each Plugin retains a lazy interface-bound Koin singleton with named constructor arguments. The Users and Wishlist bindings continue to resolve the qualified current-user state through `meStateFlow` explicitly.

The extraction preserves the existing method bodies, state-flow initialization, delegate order, DTO mapping, nullable behavior, error defaults, polling delay, and singleton lifecycle. `WishlistsModel.isOwner` and `isOwnerFlow` remain interface defaults. The Users `ExperimentalCoroutinesApi` opt-in now covers only the two implementation properties that require `mapLatest`.

## Rules, template, and feature documentation

The primary Model rule in `agents/patterns/mvvm.md` now requires a separate public `Default<InterfaceName>.kt` class, requires constructor `private val` dependencies, forbids anonymous Model implementations, and shows explicit Plugin interface binding. The authentication specialization names `DefaultAuthModel` with its two exact dependencies. Existing anonymous-interactor guidance remains intact.

The client-module template now generates a documented Default Model class and imports and binds that class from its Plugin. A smoke generation using `generatedSmoke` and `GeneratedSmoke` produced the expected file, import, singleton binding, and no anonymous Model body; the generated Model and Default Model pair also compiled with `kotlinc`.

All eight affected feature READMEs now describe the named Default implementation and its composition-root binding. A section-level comparison against `HEAD` confirmed every `## Operator Notes` section is byte-for-byte unchanged.

## Tests and verification

Focused Model tests cover every platform-neutral operation, exact arguments and request mappings, state identity and updates, null and fallback behavior, interface defaults, and Koin type and singleton identity for all eight implementations. JVM tests use real files and Ktor `MockEngine` for the Users and Wishlist upload and download paths. The Users and Wishlist JVM test source sets therefore add the existing Ktor mock dependency.

The focused eight-module JVM test command passed with 193 actionable tasks. During the full multiplatform build, the Auth and Sample deliberate failure-path tests reached KSLog's Android backend, whose `android.util.Log` implementation is unavailable in local Android unit tests. Those tests now install a silent logger only around the failure assertion and restore the prior global logger in `finally`; production code remains unchanged. The Auth and Sample debug and release Android unit-test variants passed after that correction.

The required combined build command for `adminPanel`, `auth`, `booking`, `sample`, `serverUrl`, `sidebar`, `users`, and `wishlist` completed successfully in 1 minute 28 seconds with 2,585 actionable tasks across Android, JVM, and JS. `ast-index rebuild` completed after the final Kotlin edit and indexed 1,423 files and 114 modules. Implementation queries show exactly one production Default class for each Model interface; named test fakes remain test-only. Static searches find zero production `object : ...Model` declarations, `git diff --check` passes, and no architecture deviation or untestable functionality remains.

## Review handoff

ENTITY:
entity_id=issue83-default-mvvm-models; type=coding-implementation; state=completed

CONTEXT:

* task_id=16.09.2026_15.13.30-df86933a-6817-4bda-b1f4-8c6519f749b7; agent_id=coding; memory_ref=[PROMPT.md,001-planning.md,002-architecturing.md,agents/patterns/mvvm.md,agents/patterns/auth-ui.md,8_feature_READMEs]
* constraints=[production_Default_count=8,anonymous_production_Model_count=0,constructor_dependencies=private_val,public_interfaces=preserved,production_behavior=preserved,Koin_lifecycle=lazy_singleton,Operator_Notes=unchanged]

ACTION:

1. action=extract; target=issue83-default-mvvm-models; params={classes=[DefaultAdminPanelModel,DefaultAuthModel,DefaultBookingModel,DefaultSampleModel,DefaultServerUrlModel,DefaultSidebarModel,DefaultUsersModel,DefaultWishlistsModel],source_set=commonMain,method_bodies=preserved,dependencies=constructor_private_vals}
2. action=rewire; target=issue83-default-mvvm-models; params={plugins=8,bindings=interface_singletons,named_arguments=true,users_meState=meStateFlow,wishlist_meState=meStateFlow,registration_order=preserved}
3. action=enforce; target=issue83-default-mvvm-models; params={primary_rule=Default_class,anonymous_Model=forbidden,auth_specialization=aligned,generator=Default_class_and_binding,feature_READMEs=8}
4. action=specify; target=issue83-default-mvvm-models; params={common_tests=8_models,jvm_file_tests=[UsersModel,WishlistsModel],plugin_type_checks=8,plugin_singleton_checks=8,delegate_and_state_cases=architecture_complete}
5. action=verify; target=issue83-default-mvvm-models; params={combined_build=successful,actionable_tasks=2585,ast_index_files=1423,ast_index_modules=114,diff_check=clean,Operator_Notes=unchanged}

REASON:

* condition=anonymous_Plugin_Model_realizations → action=extract_named_Default_classes → result=explicit_testable_implementations; requirement=issue83_primary_Model_rule
* condition=outside_dependencies_previously_captured_by_Koin_lambdas → action=store_constructor_private_vals → result=dependency_boundaries_explicit; requirement=behavior_preserving_extraction

EXPECTED RESULT:

* entity_id=issue83-default-mvvm-models; new_state=implemented_verified_and_committed; location=issue83_source_documentation_tests_and_003-coding.md

VERIFICATION:

* check=production_Default_class_count; expected=8
* check=production_anonymous_Model_count; expected=0
* check=combined_Gradle_build; expected=BUILD_SUCCESSFUL
* check=template_smoke_and_compilation; expected=Default_class_binding_without_anonymous_Model
* check=public_interfaces_and_behavior; expected=preserved
* check=Operator_Notes_sections; expected=unchanged

UNCERTAINTY:

* missing=none; ambiguity=none; untestable_functionality=none

REPETITION OF RESULT:

* entity_id=issue83-default-mvvm-models; stored_in=shared_memory; status=available; handoff=review_ready

COMMUNICATION:

* sender=coding; receiver=reviewing; task_id=16.09.2026_15.13.30-df86933a-6817-4bda-b1f4-8c6519f749b7; message_id=003-coding; protocol=AML-HIP

PERSISTENCE:

* local_memory=false; shared_memory=step_file_only; index_keys=[task_id,entity_id,intent,DefaultModel,Plugin,generator,tests]

VALIDATION:

* format_valid=true
* no_pronouns=true
* entities_explicit=true
* high_density=true
* causal_chain_present=true
* ambiguity_detected=false
