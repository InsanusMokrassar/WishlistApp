ENTITY:
entity_id=task#21; type=github_issue; state=architecturing

CONTEXT:
- task_id=issue#21; agent_id=architecture; memory_ref=[STEP_0.md]
- constraints=[reuse ServerUrlStorage pattern, no new feature module, no expect/actual indirection (single-platform-local-storage note), KDoc]

ARCHITECTURE_DECISIONS:

decision#1: storage_interface
- entity=WishlistViewModeStorage; type=interface; location=features/ui/wishlist/src/commonMain/kotlin/ui/WishlistViewModeStorage.kt
- members: suspend fun getViewMode(): WishlistViewMode?  ; suspend fun saveViewMode(mode: WishlistViewMode)
- rationale: per CODING.md "Single-platform local storage interface" note → suspend get/save, no expect/actual, per-platform single<> registration. Returns nullable → null=never-saved → ViewModel keeps default List.

decision#2: persistence_format
- WishlistViewMode persisted as enum.name (String). get parses via WishlistViewMode.entries.firstOrNull{name==stored}; unknown/null→null.
- rationale: stable string in localStorage, forward-tolerant to unknown values.

decision#3: per_platform_impl (mirror ServerUrlStorage)
- JS: LocalStorageWishlistViewModeStorage(key="wishlist.items.viewMode"); kotlinx.browser.localStorage; SmartRWLocker.
- JVM: PreferencesWishlistViewModeStorage(node="wishlist/items", key="viewMode"); java.util.prefs.Preferences.userRoot().node(...); SmartRWLocker; flush() on write.
- Android: SharedPreferencesWishlistViewModeStorage(context, file="wishlist.items", key="viewMode"); Context from Koin; SmartRWLocker.

decision#4: model_surface (shared cross-screen, mirror selectedCurrency)
- WishlistsModel gains: suspend fun getSavedViewMode(): WishlistViewMode ; suspend fun saveViewMode(mode: WishlistViewMode)
- getSavedViewMode returns non-null (storage null → WishlistViewMode.List default) so ViewModel init is simple.
- anonymous impl in commonMain/Plugin.kt injects WishlistViewModeStorage via get(); delegates.
- rationale: ViewModels already depend only on WishlistsModel (never on storage directly) — keeps MVVM Model boundary (CODING.md UI MVVM Rules).

decision#5: viewmodel_wiring (WishlistViewModel + UserWishlistsViewModel identical)
- init{ scope.launchLoggingDropExceptions { _viewModeState.value = model.getSavedViewMode() } }  → restore on (re)open; node recreated on refresh → init re-reads storage.
- onViewModeSelected(mode){ _viewModeState.value=mode; scope.launchLoggingDropExceptions { model.saveViewMode(mode) } } → persist on change.
- rationale: VM constructor cannot read storage synchronously (suspend) → init coroutine pattern per CODING.md note; field starts List for ~1 frame then populates.

decision#6: DI registration
- single<WishlistViewModeStorage>{ LocalStorage... } in JSPlugin; { Preferences... } in JVMPlugin; { SharedPreferences...(get()) } in AndroidPlugin. Added BEFORE/after existing factory regs, inside setupDI, after with(Plugin){setupDI}.

decision#7: no_new_dependencies
- module already on JVM/JS/Android targets; no new build.gradle deps (localStorage/Preferences/SharedPreferences are platform stdlib; SmartRWLocker from micro_utils.coroutines already transitively present — same as ServerUrlStorage usage in auth/client which wishlist depends on).

FILES_TO_CREATE:
- commonMain/kotlin/ui/WishlistViewModeStorage.kt
- jsMain/kotlin/ui/LocalStorageWishlistViewModeStorage.kt
- jvmMain/kotlin/ui/PreferencesWishlistViewModeStorage.kt
- androidMain/kotlin/ui/SharedPreferencesWishlistViewModeStorage.kt

FILES_TO_EDIT:
- commonMain/kotlin/ui/WishlistsModel.kt (interface)
- commonMain/kotlin/Plugin.kt (model impl)
- commonMain/kotlin/ui/WishlistViewModel.kt (init+onViewModeSelected)
- commonMain/kotlin/ui/UserWishlistsViewModel.kt (init+onViewModeSelected)
- jsMain/kotlin/JSPlugin.kt ; jvmMain/kotlin/JVMPlugin.kt ; androidMain/kotlin/AndroidPlugin.kt (DI)
- features/ui/wishlist/README.md (Architecture Notes)

VERIFICATION:
- check=gradle_build_wishlist_ui; expected=BUILD SUCCESSFUL
- check=localStorage key on JS = "wishlist.items.viewMode"

VALIDATION: format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false

REPETITION OF RESULT:
- entity_id=task#21; architecture_stored_in=agents/task/1f24df79-b4b0-422c-acb3-2a8c4f5ec8b2/STEP_1.md; status=available; next_step=STEP_2 Coding
