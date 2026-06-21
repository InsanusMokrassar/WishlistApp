ENTITY:
entity_id=issue-53; type=github_issue; state=fixed
title="On logout each edit view must implement its exit (it must be changed with its non-edit view)"

CONTEXT:
* task_id=3517e0f7-120e-47b5-a16e-e16e705a16c6; role=issue-executor
* repo=InsanusMokrassar/WishlistApp; branch=issue/53-logout-exit-edit-views
* constraints=[CODING.md, MVVM rules, KDoc, README update, no else-if]

ROOT_CAUSE:
* AuthViewModel.onLogout() called model.logout() only; AuthViewInteractor.onUserLoggedOut() flipped an unused flag.
  => logout had zero navigation effect; edit views stayed open for anonymous caller.

DESIGN:
* signal=reactive current-user-id flow (null == anonymous). Backed by auth meStateFlow, which reloads getMe() (->null) on AuthCredentialsStorage.userAuthorised=false. confirmed path: features/auth/client/Plugin.kt L53.
* new util=features/common/client/src/commonMain/kotlin/utils/SubscribeOnLoggedOut.kt
  fun <T:Any> Flow<T?>.subscribeOnLoggedOut(scope, action): map{it==null}.distinctUntilChanged().drop(1).filter{it}.subscribeLoggingDropExceptions
  drop(1) => cold-start anonymous / first async id resolution never fires; only genuine non-null->null fires.

ACTION (edit VMs subscribe in init, call existing read-view exit interactor):
1. WishlistItemEditViewModel -> interactor.onNavigateBackToParent(node)  [item read view EDIT / wishlist CREATE]
2. WishlistEditViewModel -> when{isCreating->onNavigateBack; else->onNavigateBackToParent}  [wishlist detail EDIT / pop CREATE]
3. UserEditViewModel -> interactor.onNavigateBack(node)  [pop reveals underlying UserView]

SCOPE_NOTE:
* covered=3 user-facing edit screens (model exposes currentUserIdFlow + read-view exit method).
* excluded=adminPanel edit views (AdminWishlistEditView/AdminWishlistItemEditView/AdminUserEditView): AdminPanelModel exposes NO auth/me flow; admin section is root-gated separately. Adding requires plumbing auth state into AdminPanelModel — not done; flag for operator if desired.

VERIFICATION:
* check=`./gradlew :wishlist.features.common.client:build :wishlist.features.ui.wishlist:build :wishlist.features.ui.users:build`; result=BUILD SUCCESSFUL (1m8s)
* check=ast-index rebuild; result=indexed 619 files

EXPECTED RESULT:
* entity_id=issue-53; new_state=ready_for_PR; location=branch issue/53-logout-exit-edit-views
