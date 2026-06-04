ENTITY:
entity_id=task#21; type=github_issue; state=planning

CONTEXT:
- task_id=issue#21; agent_id=orchestrator/root; memory_ref=[features/ui/wishlist/README.md, agents/CODING.md]
- repo=InsanusMokrassar/WishlistApp; branch=issue/21-persist-view-mode
- constraints=[no_commit, no_push, no_pr, no_branch_switch, follow agents/local.CODING.md Bootstrap, KDoc mandatory, README update mandatory, ast-index rebuild after source change]

ISSUE:
- issue#21.title="It is required to save in local storage latest selected items view mode"
- issue#21.requirement: persist user-selected items view mode to browser localStorage on change → restore on refresh/reopen instead of default.

INVESTIGATION_RESULT:
- entity=WishlistViewMode; type=enum{List,Grid}; location=features/ui/wishlist/src/commonMain/kotlin/ui/WishlistViewMode.kt
- entity=viewModeState; owner=[WishlistViewModel, UserWishlistsViewModel]; default=WishlistViewMode.List; mutator=onViewModeSelected(mode)
- relation: both ViewModels hold WishlistsModel singleton (shared cross-screen state pattern already used for selectedCurrency)
- reference_pattern=ServerUrlStorage (features/auth/client): suspend interface getX/saveX + per-platform impl(JS localStorage / JVM Preferences / Android SharedPreferences) guarded SmartRWLocker, registered single<Storage> in each platform plugin.
- feature README.md has NO Operator Notes constraints blocking change.
- ast-index installed=true.

PLAN:
1. action=create_interface; target=WishlistViewModeStorage; params={commonMain, suspend getViewMode():WishlistViewMode?, saveViewMode(mode:WishlistViewMode)}
2. action=create_impl; target=LocalStorageWishlistViewModeStorage; params={jsMain, key="wishlist.items.viewMode", kotlinx.browser.localStorage, SmartRWLocker}
3. action=create_impl; target=PreferencesWishlistViewModeStorage; params={jvmMain, java.util.prefs.Preferences node="wishlist/items" key="viewMode", SmartRWLocker}
4. action=create_impl; target=SharedPreferencesWishlistViewModeStorage; params={androidMain, Context from Koin, file="wishlist.items" key="viewMode", SmartRWLocker}
5. action=extend_model; target=WishlistsModel; params={add suspend getSavedViewMode():WishlistViewMode, saveViewMode(mode)}
6. action=extend_model_impl; target=Plugin.kt anonymous WishlistsModel; params={inject WishlistViewModeStorage, delegate}
7. action=wire_vm; target=[WishlistViewModel, UserWishlistsViewModel]; params={init: read model.getSavedViewMode() into _viewModeState; onViewModeSelected: persist via model.saveViewMode(mode)}
8. action=register_di; target=[JSPlugin, JVMPlugin, AndroidPlugin]; params={single<WishlistViewModeStorage>{impl}}
9. action=update_readme; target=features/ui/wishlist/README.md Architecture Notes
10. action=rebuild_index; target=ast-index
11. action=verify_build; target=./gradlew :wishlist.features.ui.wishlist:build

REASON:
- condition=enum persisted as name string → action=localStorage put/get → result=restore on reopen
- storage_key="wishlist.items.viewMode" (JS localStorage), namespaced consistent with existing "wishlist.serverAddress.url"

EXPECTED_RESULT:
- entity=viewModeState; new_state=restored_from_storage_on_init; persisted_on_change
- location=browser localStorage key "wishlist.items.viewMode"

UNCERTAINTY:
- issue mentions "JS client" explicitly; pattern mandates multiplatform storage interface → implement all 3 platforms (JS primary) to satisfy expect-pattern + compile of full module. ambiguity=none_blocking.

VALIDATION: format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false

REPETITION OF RESULT:
- entity_id=task#21; plan_stored_in=agents/task/1f24df79-b4b0-422c-acb3-2a8c4f5ec8b2/STEP_0.md; status=available; next_step=STEP_1 Architecturing
