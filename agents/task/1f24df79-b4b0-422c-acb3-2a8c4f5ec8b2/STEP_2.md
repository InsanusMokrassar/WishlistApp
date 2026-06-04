ENTITY:
entity_id=task#21; type=github_issue; state=coding_done

CONTEXT:
- task_id=issue#21; agent_id=coding; memory_ref=[STEP_0.md, STEP_1.md]
- module=features/ui/wishlist; branch=issue/21-persist-view-mode

FILES_CREATED:
1. features/ui/wishlist/src/commonMain/kotlin/ui/WishlistViewModeStorage.kt; type=interface; members=[suspend getViewMode():WishlistViewMode?, suspend saveViewMode(mode)]
2. features/ui/wishlist/src/jsMain/kotlin/ui/LocalStorageWishlistViewModeStorage.kt; backend=kotlinx.browser.localStorage; key="wishlist.items.viewMode"; locker=SmartRWLocker; format=enum.name
3. features/ui/wishlist/src/jvmMain/kotlin/ui/PreferencesWishlistViewModeStorage.kt; backend=java.util.prefs.Preferences node="wishlist/items" key="viewMode"; locker=SmartRWLocker; flush_on_write=true
4. features/ui/wishlist/src/androidMain/kotlin/ui/SharedPreferencesWishlistViewModeStorage.kt; backend=Context.getSharedPreferences file="wishlist.items" key="viewMode"; Context=Koin get(); locker=SmartRWLocker

FILES_EDITED:
5. commonMain/kotlin/ui/WishlistsModel.kt; added=[suspend getSavedViewMode():WishlistViewMode (null→List), suspend saveViewMode(mode)]
6. commonMain/kotlin/Plugin.kt; added=val viewModeStorage=get<WishlistViewModeStorage>(); impl getSavedViewMode/saveViewMode delegate to storage
7. commonMain/kotlin/ui/WishlistViewModel.kt; init+=scope.launchLoggingDropExceptions{_viewModeState.value=model.getSavedViewMode()}; onViewModeSelected+=scope.launchLoggingDropExceptions{model.saveViewMode(mode)}
8. commonMain/kotlin/ui/UserWishlistsViewModel.kt; identical init+onViewModeSelected wiring as #7
9. jsMain/kotlin/JSPlugin.kt; single<WishlistViewModeStorage>{LocalStorageWishlistViewModeStorage()}
10. jvmMain/kotlin/JVMPlugin.kt; single<WishlistViewModeStorage>{PreferencesWishlistViewModeStorage()}
11. androidMain/kotlin/AndroidPlugin.kt; single<WishlistViewModeStorage>{SharedPreferencesWishlistViewModeStorage(get())}
12. features/ui/wishlist/README.md; Architecture Notes: added view-mode persistence paragraph under "Item cards and view mode"

PERSISTENCE_FLOW:
- change: user→onViewModeSelected(mode)→_viewModeState=mode + model.saveViewMode(mode)→storage.saveViewMode→localStorage["wishlist.items.viewMode"]=mode.name
- restore: screen (re)open→ViewModel.init→model.getSavedViewMode()→storage.getViewMode()→localStorage read→parse enum.name→_viewModeState (storage null→List)
- JS refresh recreates navigation node→ViewModel reconstructed→init re-reads localStorage → mode restored.

LOCALSTORAGE_KEY: "wishlist.items.viewMode" (JS). JVM key="viewMode" node="wishlist/items". Android key="viewMode" file="wishlist.items".

VERIFICATION:
- check=./gradlew :wishlist.features.ui.wishlist:build; result=BUILD SUCCESSFUL in 36s (JS+JVM+Android compiled, lint passed)
- check=ast-index rebuild; result=Indexed 511 files, 35 modules
- check=KDoc present on all new symbols; result=true
- check=README Architecture Notes updated; result=true; Operator Notes untouched=true

VALIDATION: format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false

REPETITION OF RESULT:
- entity_id=task#21; state=coding_done; build=SUCCESSFUL; stored_in=agents/task/1f24df79-b4b0-422c-acb3-2a8c4f5ec8b2/STEP_2.md; status=available
- constraints_honored: no_commit, no_push, no_pr, no_branch_switch
