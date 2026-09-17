Model: OpenAI Astra (HL architecturing), OpenAI Luna (LL report transcription)
Changed files: agents/task/16.09.2026_15.13.30-df86933a-6817-4bda-b1f4-8c6519f749b7/002-architecturing.md

## Model choice

Architecture requires an HL investigation and an LL Markdown transcription. OpenAI Astra supplied the design decisions and OpenAI Luna is recording the implementation contract without changing scope.

## Outcome

Issue #83 changes the primary MVVM Model rule from anonymous Plugin objects to named `Default<InterfaceName>` classes. Eight existing production models are extracted into public `commonMain` classes in their existing feature UI packages. Each class keeps the interface, member order, state initialization, method bodies, argument mapping, exception handling, defaults, and public consumers unchanged. Every dependency previously captured by a Plugin `single` becomes a primary-constructor `private val`. Koin remains the composition root and keeps each model as a lazy `single` bound to its interface.

The production count remains eight. Test doubles and anonymous interactors remain permitted where their existing patterns allow them. `WishlistsModel` interface default methods `isOwner` and `isOwnerFlow` remain on the interface and are not duplicated in the implementation.

## Scope and file inventory

Production files to add under each feature's `src/commonMain/kotlin/ui/` directory:

| Feature | New class | Constructor dependencies |
|---|---|---|
| adminPanel | `DefaultAdminPanelModel` | `admin: AdminFeature`, `email: EmailFeature`, `credentialsStorage: AuthCredentialsStorage` |
| auth | `DefaultAuthModel` | `authFeature: ClientAuthFeature`, `credentialsStorage: AuthCredentialsStorage` |
| booking | `DefaultBookingModel` | `bookingFeature: BookingFeature` |
| sample | `DefaultSampleModel` | `feature: SampleFeature`, `echoFeature: EchoFeature` |
| serverUrl | `DefaultServerUrlModel` | `storage: ServerUrlStorage` |
| sidebar | `DefaultSidebarModel` | `wishlistsModel: WishlistsModel`, `bookingModel: BookingModel`, `usersModel: UsersModel` |
| users | `DefaultUsersModel` | `feature: UsersFeature`, `authFeature: ClientAuthFeature`, `emailFeature: EmailFeature`, `meState: StateFlow<AuthFeatureUser?>`, `adminFeature: AdminFeature`, `filesService: FilesClientService`, `scope: CoroutineScope`, `credentialsStorage: AuthCredentialsStorage`, `rolesFeature: RolesFeature` |
| wishlist | `DefaultWishlistsModel` | `wishlistsFeature: WishlistsFeature`, `itemsFeature: WishlistsItemsFeature`, `copyFeature: WishlistCopyFeature`, `meState: StateFlow<AuthFeatureUser?>`, `filesService: FilesClientService`, `usersFeature: UsersFeature`, `currencyService: CurrencyService`, `viewModeStorage: WishlistViewModeStorage`, `scope: CoroutineScope`, `credentialsStorage: AuthCredentialsStorage` |

Exact implementation, composition-root, documentation, and test files:

| Feature | Default class file | Plugin file | README file | Model test file |
|---|---|---|---|---|
| adminPanel | `features/ui/adminPanel/src/commonMain/kotlin/ui/DefaultAdminPanelModel.kt` | `features/ui/adminPanel/src/commonMain/kotlin/Plugin.kt` | `features/ui/adminPanel/README.md` | `features/ui/adminPanel/src/commonTest/kotlin/AdminPanelModelTest.kt` (expand existing) |
| auth | `features/ui/auth/src/commonMain/kotlin/ui/DefaultAuthModel.kt` | `features/ui/auth/src/commonMain/kotlin/Plugin.kt` | `features/ui/auth/README.md` | `features/ui/auth/src/commonTest/kotlin/ui/AuthModelTest.kt` (add) |
| booking | `features/ui/booking/src/commonMain/kotlin/ui/DefaultBookingModel.kt` | `features/ui/booking/src/commonMain/kotlin/Plugin.kt` | `features/ui/booking/README.md` | `features/ui/booking/src/commonTest/kotlin/ui/BookingModelTest.kt` (add) |
| sample | `features/ui/sample/src/commonMain/kotlin/ui/DefaultSampleModel.kt` | `features/ui/sample/src/commonMain/kotlin/Plugin.kt` | `features/ui/sample/README.md` | `features/ui/sample/src/commonTest/kotlin/ui/SampleModelTest.kt` (add) |
| serverUrl | `features/ui/serverUrl/src/commonMain/kotlin/ui/DefaultServerUrlModel.kt` | `features/ui/serverUrl/src/commonMain/kotlin/Plugin.kt` | `features/ui/serverUrl/README.md` | `features/ui/serverUrl/src/commonTest/kotlin/ui/ServerUrlModelTest.kt` (add) |
| sidebar | `features/ui/sidebar/src/commonMain/kotlin/ui/DefaultSidebarModel.kt` | `features/ui/sidebar/src/commonMain/kotlin/Plugin.kt` | `features/ui/sidebar/README.md` | `features/ui/sidebar/src/commonTest/kotlin/ui/SidebarModelTest.kt` (add; retain `SidebarViewModelTest.kt`) |
| users | `features/ui/users/src/commonMain/kotlin/ui/DefaultUsersModel.kt` | `features/ui/users/src/commonMain/kotlin/Plugin.kt` | `features/ui/users/README.md` | `features/ui/users/src/commonTest/kotlin/UsersModelTest.kt` (expand existing) |
| wishlist | `features/ui/wishlist/src/commonMain/kotlin/ui/DefaultWishlistsModel.kt` | `features/ui/wishlist/src/commonMain/kotlin/Plugin.kt` | `features/ui/wishlist/README.md` | `features/ui/wishlist/src/commonTest/kotlin/ui/WishlistsModelTest.kt` (add) |

Exact planned documentation and template paths are `agents/patterns/mvvm.md`, `agents/patterns/auth-ui.md`, `features/ui/adminPanel/README.md`, `features/ui/auth/README.md`, `features/ui/booking/README.md`, `features/ui/sample/README.md`, `features/ui/serverUrl/README.md`, `features/ui/sidebar/README.md`, `features/ui/users/README.md`, `features/ui/wishlist/README.md`, `.templates/client_module_kts/{{$module_path}}/src/commonMain/kotlin/Plugin.kt`, and `.templates/client_module_kts/{{$module_path}}/src/commonMain/kotlin/ui/Default{{$module_ui_name}}Model.kt`.

The eight existing common `Plugin.kt` files are modified to import the matching Default class and replace the anonymous body with a named-argument Koin binding. Users and Wishlist bindings resolve `meState = meStateFlow` through the Koin `Scope` accessor explicitly; an unqualified `get()` is not acceptable for that state. All serializer, ViewModel, interactor, view-factory, and startup registrations remain in their current order and lifecycle. Implementation-only imports are removed.

Generator enforcement adds `.templates/client_module_kts/{{$module_path}}/src/commonMain/kotlin/ui/Default{{$module_ui_name}}Model.kt` with a valid placeholder KDoc and generated interface implementation. The generated Plugin imports and registers `Default{{$module_ui_name}}Model()` using the same named class rule. The template must not emit `object : ...Model`. The implementation should verify the generator's actual placeholder constructor against the generated interface and keep the template compilable for the scaffold's intended model shape.

No source change is planned for `agents/ARCHITECTURE.md`; it has no contradictory anonymous-model rule. No migration or rollback mechanism is needed because this is a source-only, commit-reversible refactor. No UI rendering or external behavior changes are expected, so no untestable functionality needs operator confirmation.

## Class extraction contract

Move each anonymous implementation body as-is into its corresponding class. Preserve private backing flows, initial values, `stateIn` policies, constants, polling, `runCatchingLogging` and default values, DTO construction, nullable handling, and all delegate call order. Do not add Koin imports or resolve dependencies from a container inside a Default class. The constructor is the only source of outside-world dependencies.

Behavior that must remain observable:

* `DefaultAdminPanelModel` keeps every user, wishlist, item, and email delegation. `updateWishlist` continues to ignore `userId` and constructs `NewWishlistInFeature(title)` exactly as before.
* `DefaultAuthModel` retains the `isAlreadyLoggedIn` short-circuit, `runCatchingLogging`/default `AuthConfig` behavior, login/logout/config/registration mapping, and nullable email handling. It uses the current `ClientAuthFeature` and `AuthCredentialsStorage` dependencies.
* `DefaultBookingModel` forwards all four Booking operations without changing arguments or return values.
* `DefaultSampleModel` keeps text delegation and the forever-running status flow: each echo failure emits `null`, and the one-second delay remains after each poll.
* `DefaultServerUrlModel` returns storage values and saves blank or whitespace-only input as `null`; a nonblank string remains untrimmed.
* `DefaultSidebarModel` exposes the exact dependent `StateFlow` instances and preserves list, reserved-count `.size`, and name behavior.
* `DefaultUsersModel` preserves reactive `stateIn`, `map`, and `mapLatest` initial values and constants, all user/auth/role/file/email/admin delegations, and `getUser` first-or-null behavior. Roles are not called when the current user is null. Move `@OptIn(ExperimentalCoroutinesApi::class)` from Plugin setupDI to the smallest valid class or member scope required by `mapLatest`.
* `DefaultWishlistsModel` preserves Grid as the default view mode, exact wishlist/item/copy request DTO mappings, upload-id return behavior, current-user derivation, username first-or-null lookup, all CRUD delegates, currency service identity/delegates, view-mode storage null-to-Grid behavior, and current-user flow initial/update behavior.

Keep all classes public by omitting explicit non-public visibility. Add valid KDocs for every class, constructor parameter, class-level property, and override required by `agents/CODING.md`; KDocs describe purpose and use `@param`/`@return` where applicable. The class package must match the existing interface package, and each file must have one corresponding Default class.

## Plugin wiring

Simple binding shape:

```kotlin
single<BookingModel> {
    DefaultBookingModel(bookingFeature = get())
}
```

Users and Wishlist binding shape:

```kotlin
single<UsersModel> {
    DefaultUsersModel(
        feature = get(),
        authFeature = get(),
        emailFeature = get(),
        meState = meStateFlow,
        adminFeature = get(),
        filesService = get(),
        scope = get(),
        credentialsStorage = get(),
        rolesFeature = get(),
    )
}
```

```kotlin
single<WishlistsModel> {
    DefaultWishlistsModel(
        wishlistsFeature = get(),
        itemsFeature = get(),
        copyFeature = get(),
        meState = meStateFlow,
        filesService = get(),
        usersFeature = get(),
        currencyService = get(),
        viewModeStorage = get(),
        scope = get(),
        credentialsStorage = get(),
    )
}
```

Use the exact constructor names above for all eight registrations. Preserve classic Koin DSL because interface bindings, explicit qualified state access, and project conventions require a visible composition-root mapping. Koin's singleton lifecycle supports the current `single` lifetime and lazy creation; switching all definitions to constructor DSL would obscure the required interface and `meState` wiring.

## Documentation changes

`agents/patterns/mvvm.md` is normative because `CODING.md` routes MVVM work there. Rewrite the Model rule, model examples, Koin checklist, and sample walkthrough to make a separate `DefaultMyModel` file mandatory, with constructor `private val` dependencies and a Plugin interface singleton binding. State that anonymous Model implementations are forbidden. Preserve the existing anonymous-interactor guidance and its examples.

Update `agents/patterns/auth-ui.md` as a subordinate specialization: name `DefaultAuthModel`, show the named binding and its current `ClientAuthFeature`/`AuthCredentialsStorage` dependencies, and leave the interactor design unchanged.

Update all eight feature READMEs in their descriptive sections and architecture notes to name the corresponding Default class and Plugin binding. Correct stale anonymous wording in adminPanel, booking, wishlist, and the sample Plugin walkthrough. Apply the same statement to auth, serverUrl, sidebar, and users. Do not modify any `## Operator Notes` section. README deltas are required because `ARCHITECTURE.md` and `CODING.md` require feature documentation to track model and dependency changes.

## Test specifications

Tests are specifications for the Coding step and are feasible with commonTest fakes for model and Plugin behavior. Each test resolves the interface from the production Plugin, asserts `assertIs<Default…Model>`, and requests the interface twice to assert singleton identity. Recording fakes verify every delegate argument and result. Existing `AdminPanelModelTest` and `UsersModelTest` should be expanded; add commonTest coverage for Auth, Booking, Sample, ServerUrl, Sidebar, and Wishlists models. Users/Wishlist file-taking cases must use `jvmTest` with `java.io.File` plus Ktor `MockEngine` when required by the concrete API; do not claim that an expect `MPPFile` can be constructed in commonTest. CommonTest remains appropriate for all platform-neutral delegation, flow, mapping, and singleton cases.

| Model | Required cases |
|---|---|
| AdminPanel | Every user, wishlist, item, and email method; verify ignored `userId` and exact `NewWishlistInFeature(title)` mapping. |
| Auth | Storage false avoids `getMe`; storage true with nonnull and null user; login/logout/config success, failure, default config, registration, nullable email; flow identity. |
| Booking | All four methods and exact arguments/results. |
| Sample | Text result; `serverStatusFlow().take(2)` success and echo failure under virtual time, value/null emissions, one-second delay, continued polling. |
| ServerUrl | Get; null, blank, and whitespace save as null; nonblank value remains untrimmed. |
| Sidebar | Exact wishlist, booking, and users flow identity; list/name values; reserved count equals `.size`. |
| Users | Every method; `getUser` first/null; auth flow identity; me initial/update; no role call for null; nonnull role constants; file/email/admin delegates. |
| Wishlists | Every abstract override; storage null-to-Grid and save; currency identity/delegates; CRUD and exact request DTOs; upload id; me initial/update; username first/null; file delegates. |

Keep or add tests for `WishlistsModel.isOwner` and `isOwnerFlow` interface defaults, but do not copy those implementations into `DefaultWishlistsModel`. Plugin tests must verify two interface gets return the same instance. Test fakes cover all planned methods; no planned functionality is untestable.

Template smoke coverage generates a scenario into a temporary directory and inspects the generated Default file, import, binding, valid KDocs, and absence of an anonymous Model object. Documentation checks assert the exact Default rule and absence of stale model-anonymous wording while retaining anonymous-interactor text.

## Verification boundaries and implementation order

1. Inventory current interfaces, anonymous bodies, Plugin registrations, and template output with `ast-index`; use `rg` only for Markdown and template text.
2. Add the eight classes by direct extraction, then wire the eight Plugins with named arguments and explicit `meStateFlow`.
3. Move the Users `ExperimentalCoroutinesApi` opt-in to the narrowest valid class/member scope and compile-check the `mapLatest` location.
4. Update the generator template and all normative/readme documentation, leaving Operator Notes unchanged.
5. Rebuild `ast-index` after Kotlin/template Kotlin changes. Confirm each of the eight interfaces has one Default class and one Plugin binding, and zero production `object : *Model` implementations.
6. Run exactly these module builds, including relevant tests: `./gradlew :wishlist.features.ui.adminPanel:build`, `:wishlist.features.ui.auth:build`, `:wishlist.features.ui.booking:build`, `:wishlist.features.ui.sample:build`, `:wishlist.features.ui.serverUrl:build`, `:wishlist.features.ui.sidebar:build`, `:wishlist.features.ui.users:build`, and `:wishlist.features.ui.wishlist:build`.
7. Review the diff for unchanged interfaces and method bodies, unchanged Operator Notes, complete KDocs, correct template output, and no implementation-only imports. No UI/manual/external behavior verification is required.

Research informed the design. Kotlin's official Classes documentation explains primary-constructor parameters declared as properties; the project applies that with `private val` so captured dependencies are stored without widening the model API: [Kotlin Classes](https://kotlinlang.org/docs/classes.html). Koin's Definitions documentation confirms classic `single { Class(get()) }` bindings and singleton lifecycle; the project retains classic explicit interface bindings because qualified `meState` resolution and the composition-root contract matter more than wholesale adoption of constructor DSL: [Koin Definitions](https://insert-koin.io/docs/reference/koin-core/definitions/). Android's state-holder guidance emphasizes passing only required data and improving independent testability; the project adapts that by passing exact StateFlow and service dependencies and never passing Koin Scope/container into a Default class: [Android state holders](https://developer.android.com/topic/architecture/ui-layer/stateholders). These generic recommendations are subordinate to the repository's MVVM, Koin, package, lifecycle, and public-interface conventions.

## README updates

Each feature README should add or revise Architecture Notes to state that the Model interface is implemented by the named `Default<InterfaceName>` class in the feature's common UI package and registered as an interface `single` in Plugin. Include the constructor dependency list where the README currently describes model composition. Remove anonymous Model implementation wording from adminPanel, auth, booking, sample, serverUrl, sidebar, users, and wishlist descriptions. Preserve all Operator Notes exactly.

## Coding handoff

ENTITY:
entity_id=mvvm-default-model-migration; type=architecture-change; state=implementation-ready

CONTEXT:

* task_id=16.09.2026_15.13.30-df86933a-6817-4bda-b1f4-8c6519f749b7; agent_id=architecturing; memory_ref=[001-planning.md,PROMPT.md,agents/patterns/mvvm.md,agents/patterns/auth-ui.md,8_feature_READMEs]
* constraints=[production_model_count=8,commonMain_Default_classes=true,anonymous_production_models_forbidden,interfaces_and_bodies_preserved,constructor_private_val_dependencies,Koin_singleton_composition_root,explicit_meStateFlow,Operator_Notes_immutable,anonymous_interactor_guidance_preserved,generator_enforced]

ACTION:

1. action=extract; target=mvvm-default-model-migration; params={classes=[DefaultAdminPanelModel,DefaultAuthModel,DefaultBookingModel,DefaultSampleModel,DefaultServerUrlModel,DefaultSidebarModel,DefaultUsersModel,DefaultWishlistsModel],locations=[8_existing_feature_commonMain_ui_packages],dependencies=primary_constructor_private_vals,bodies=preserve_as_is}
2. action=register; target=mvvm-default-model-migration; params={bindings=single_interface_to_Default,named_arguments=true,users_meState=meStateFlow,wishlist_meState=meStateFlow,koin_access_inside_Default=false,lifecycle=lazy_single}
3. action=generate; target=mvvm-default-model-migration; params={template_Default_file=true,template_Plugin_binding=true,anonymous_model_template=false,placeholder_KDocs=true}
4. action=document; target=mvvm-default-model-migration; params={normative_mvvm_rule=Default_class_primary,auth_ui=DefaultAuthModel,readmes=8,Operator_Notes=unchanged,anonymous_interactors=preserved}
5. action=verify; target=mvvm-default-model-migration; params={ast_index=rebuild_and_query,builds=8_gradle_modules,tests=all_model_members,docs=stale_anonymous_model_absent}

REASON:

* condition=anonymous_plugin_models_reduce_named_dependency_testability_and_template_compliance; requirement=Default_classes_with_explicit_constructor_dependencies_and_plugin_interface_singletons
* condition=Users_and_Wishlist_require_qualified_current_user_state; requirement=explicit_meStateFlow_scope_accessor_mapping
* condition=behavior_preservation_required; requirement=body_state_initialization_argument_exception_default_semantics_unchanged

EXPECTED RESULT:

* entity_id=mvvm-default-model-migration; new_state=implementation-ready; location=agents/task/16.09.2026_15.13.30-df86933a-6817-4bda-b1f4-8c6519f749b7/002-architecturing.md

VERIFICATION:

* check=production_Default_class_count; expected=8
* check=production_anonymous_Model_count; expected=0
* check=interface_method_bodies_and_state_initialization; expected=unchanged
* check=Plugin_interface_binding_lifecycle; expected=8_lazy_singletons_and_users_wishlist_explicit_meStateFlow
* check=template_generation; expected=Default_file_and_binding_without_anonymous_Model
* check=module_builds; expected=8_successful_Gradle_builds_with_relevant_tests
* check=documentation; expected=Default_primary_rule_and_anonymous_interactor_guidance_present
* check=Operator_Notes; expected=unchanged

UNCERTAINTY:

* missing=none; ambiguity=none; untestable_functionality=none

REPETITION OF RESULT:

* entity_id=mvvm-default-model-migration; stored_in=shared_memory; status=available; handoff=implementation_ready

COMMUNICATION:

* sender=architecturing; receiver=coding; task_id=16.09.2026_15.13.30-df86933a-6817-4bda-b1f4-8c6519f749b7; message_id=002-architecturing; protocol=AML-HIP

PERSISTENCE:

* local_memory=false; shared_memory=step_file_only; index_keys=[task_id,entity_id,intent,DefaultModel,Plugin,generator,tests]

VALIDATION:

* format_valid=true
* no_pronouns=true
* entities_explicit=true
* high_density=true
* causal_chain_present=true
* ambiguity_detected=false
