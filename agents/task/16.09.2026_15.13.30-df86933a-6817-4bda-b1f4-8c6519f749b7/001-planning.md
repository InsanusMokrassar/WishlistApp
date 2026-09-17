Model: OpenAI Astra (HL planning), OpenAI Luna (LL report transcription)
Changed files: agents/task/16.09.2026_15.13.30-df86933a-6817-4bda-b1f4-8c6519f749b7/001-planning.md

## Model choice

Planning priority is HL/ML. OpenAI Astra was the inherited available HL model and performed the investigation; OpenAI Luna is the LL transcription model required by `agents/SHORTCUTS.md` for Markdown documentation. Astra supplied the findings recorded below, and Luna is transcribing them without changing scope.

## Task understanding

The task changes the MVVM UI Model convention from anonymous Plugin implementations to named commonMain classes prefixed with `Default`. Exactly eight production Model interfaces under `features/ui` currently have anonymous `object : XModel` implementations in Plugin files: AdminPanelModel, AuthModel, BookingModel, SampleModel, ServerUrlModel, SidebarModel, UsersModel, and WishlistsModel. Scaffold and topBar have no Model and are out of scope; test fakes and anonymous interactors are also out of scope.

The new primary rule in `agents/patterns/mvvm.md` requires every MVVM Model interface to have a named commonMain implementation named `Default<MyModelName>`, forbids anonymous Model implementations, and requires all outside-world dependencies formerly captured by the Plugin singleton to become constructor `private val`s. Plugin remains the composition root and registers `single<MyModel> { DefaultMyModel(...) }`. Existing model interfaces, method bodies, state initialization, singleton bindings, and behavior must remain unchanged. The auth UI pattern must align with the same named implementation rule. Feature READMEs must document the Default class and Plugin registration without modifying Operator Notes.

## Evidence/current state

Investigation found eight production Model interfaces and eight corresponding anonymous Plugin implementations. The required constructor dependencies are: `DefaultAdminPanelModel(AdminFeature, EmailFeature, AuthCredentialsStorage)`; `DefaultAuthModel(ClientAuthFeature, AuthCredentialsStorage)`; `DefaultBookingModel(BookingFeature)`; `DefaultSampleModel(SampleFeature, EchoFeature)`; `DefaultServerUrlModel(ServerUrlStorage)`; `DefaultSidebarModel(WishlistsModel, BookingModel, UsersModel)`; `DefaultUsersModel(UsersFeature, ClientAuthFeature, EmailFeature, StateFlow<AuthFeatureUser?> meState, AdminFeature, FilesClientService, CoroutineScope, AuthCredentialsStorage, RolesFeature)`; and `DefaultWishlistsModel(WishlistsFeature, WishlistsItemsFeature, WishlistCopyFeature, StateFlow<AuthFeatureUser?> meState, FilesClientService, UsersFeature, CurrencyService, WishlistViewModeStorage, CoroutineScope, AuthCredentialsStorage)`. Users and Wishlists must receive `Scope.meStateFlow` explicitly during registration.

The eight Plugin bodies are the source of the extracted implementations. New files belong in each feature's commonMain UI package and must be named `DefaultAdminPanelModel.kt`, `DefaultAuthModel.kt`, `DefaultBookingModel.kt`, `DefaultSampleModel.kt`, `DefaultServerUrlModel.kt`, `DefaultSidebarModel.kt`, `DefaultUsersModel.kt`, and `DefaultWishlistsModel.kt`. Required KDoc applies to each new class, private constructor property, and override under CODING.md. The eight Plugin files must import and register the Default classes and remove implementation bodies and unused imports. Active documentation containing anonymous-model wording is in `agents/patterns/mvvm.md`, `agents/patterns/auth-ui.md`, and the adminPanel, booking, and wishlist READMEs. All eight feature READMEs were read; no Operator Notes conflict was found. `ast-index` was installed but initially lacked a cache; a sandbox rebuild failed because of read-only cache access, then an approved external-cache rebuild succeeded, after which code navigation used ast-index and Markdown searches used rg.

## Scope and acceptance

Scope includes eight new named commonMain Model classes, eight Plugin composition-root updates, the MVVM and auth-ui pattern documentation updates, and consistency updates in all eight feature READMEs. The mvvm documentation examples, checklist, and sample walkthrough must reflect named Default implementations. Anonymous interactor guidance remains intact. The auth-ui Model bullet must use DefaultAuthModel and named registration, with no expansion into interactor changes. Operator Notes remain untouched.

Acceptance requires zero production `object : *Model` implementations under `features/ui`, unchanged Model interfaces and behavior, correct constructor dependency capture and explicit Users/Wishlists `meStateFlow` wiring, complete KDoc, and documentation with no stale anonymous-Model wording while retaining anonymous-interactor guidance. No new tests are required because the extraction is mechanical; existing model and module tests provide behavior and binding coverage.

## Implementation plan

Create each Default class by moving the corresponding Plugin anonymous object's method and property bodies byte-for-byte or semantically unchanged into the feature's commonMain UI package. Declare every listed dependency as a constructor `private val`, including the supplied `meState` StateFlow values and CoroutineScopes. Preserve all state initialization and interface contracts.

Replace each Plugin anonymous object with a Koin singleton registration importing the matching Default class. Keep interface bindings and registration scope unchanged; pass `Scope.meStateFlow` to the Users and Wishlists constructors. Remove imports made obsolete by moving implementation code, while retaining imports needed by registration and preserved bindings.

Update `agents/patterns/mvvm.md` as the authoritative rule, examples, checklist, and sample walkthrough. Update `agents/patterns/auth-ui.md` to describe DefaultAuthModel and named registration. Update all eight feature READMEs to identify the Default implementation and Plugin registration, specifically removing stale anonymous wording in adminPanel, booking, and wishlist and correcting the sample statement that the implementation lives in Plugin. Do not edit Operator Notes.

## Verification plan

After source changes, rebuild ast-index. Query the indexed source to verify that no `object : *Model` remains under `features/ui`. Run Gradle builds for `:wishlist.features.ui.adminPanel:build`, `:wishlist.features.ui.auth:build`, `:wishlist.features.ui.booking:build`, `:wishlist.features.ui.sample:build`, `:wishlist.features.ui.serverUrl:build`, `:wishlist.features.ui.sidebar:build`, `:wishlist.features.ui.users:build`, and `:wishlist.features.ui.wishlist:build`. Inspect the updated documentation for removal of anonymous-Model wording and preservation of anonymous-interactor guidance. Existing AdminPanelModelTest, UsersModelTest, SidebarViewModelTest, and module tests are the relevant behavioral checks.

## Questions for operator

No operator questions. The request is explicit, dependencies and acceptance checks are identified, and investigation found no unresolved architecture decision or Operator Notes conflict.

## Architecture handoff

ENTITY:
entity_id=mvvm-default-model-migration; type=architecture-change; state=planned

CONTEXT:

* task_id=16.09.2026_15.13.30-df86933a-6817-4bda-b1f4-8c6519f749b7; agent_id=planning; memory_ref=[001-planning.md, PROMPT.md, agents/patterns/mvvm.md, agents/patterns/auth-ui.md]
* constraints=[eight production Models; commonMain named Default classes; anonymous Models forbidden; interfaces unchanged; Plugin composition root; Operator Notes immutable; anonymous interactors preserved]

ACTION:

1. action=extract; target=mvvm-default-model-migration; params={classes=[DefaultAdminPanelModel,DefaultAuthModel,DefaultBookingModel,DefaultSampleModel,DefaultServerUrlModel,DefaultSidebarModel,DefaultUsersModel,DefaultWishlistsModel], dependencies=constructor_private_vals, bodies=preserve_behavior}
2. action=register; target=mvvm-default-model-migration; params={plugin_bindings=single_interface_to_default, explicit_me_state=[UsersModel,WishlistsModel], unused_imports=remove}
3. action=document; target=mvvm-default-model-migration; params={mvvm_rule=replace_anonymous_default_rule, auth_ui=DefaultAuthModel, readmes=eight_features, operator_notes=preserve}

REASON:

* condition=production MVVM Models use anonymous Plugin objects; requirement=each Model interface requires a named Default commonMain implementation and Plugin composition-root registration

EXPECTED RESULT:

* entity_id=mvvm-default-model-migration; new_state=implementation-ready; location=architecture-handoff

VERIFICATION:

* check=ast-index query under features/ui; expected=zero object : *Model production implementations
* check=eight Gradle module builds; expected=success
* check=documentation wording; expected=Default Model wording present, anonymous interactor guidance present

UNCERTAINTY:

* missing=none; ambiguity=none

REPETITION OF RESULT:

* entity_id=mvvm-default-model-migration; stored_in=shared_memory; status=available

COMMUNICATION:

* sender=planning; receiver=architecture; task_id=16.09.2026_15.13.30-df86933a-6817-4bda-b1f4-8c6519f749b7; message_id=df86933a-6817-4bda-b1f4-8c6519f749b7; protocol=AML-HIP

PERSISTENCE:

* local_memory=false; shared_memory=step_file_only; index_keys=[task_id, entity_id, intent]

VALIDATION:

* format_valid=true
* no_pronouns=true
* entities_explicit=true
* high_density=true
* causal_chain_present=true
* ambiguity_detected=false
