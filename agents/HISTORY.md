# Agent Long-Term Memory — WishlistApp

## Session Log

---

### 2026-05-30 — Session 17: Deletion of wishlist items, wishlists, and users

**Prompt:** `Implement opportunity to delete things: wishlist item (owner, danger button on item edit, modal confirm, back after delete, auto-back on reopen if gone); wishlist (same, on wishlist edit); user (root only, danger button on users list row, double modal confirm, cascade remove wishlists+items+password+sessions)`

**Actions:**
- action=update; target=features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt; changes=[added purgeUser(userId): removes password hash + all access/refresh sessions for user]
- action=update; target=features/admin/server/src/commonMain/kotlin/UsersManagementFeature.kt; changes=[ctor +WishlistRepo +WishlistItemRepo; delete() cascades: per owned wishlist delete items then wishlist, then authService.purgeUser, then deleteById user]
- action=update; target=features/admin/server/src/commonMain/kotlin/Plugin.kt; changes=[UsersManagementFeature ctor wired with WishlistRepo + WishlistItemRepo]
- action=update; target=features/ui/wishlist/src/commonMain/kotlin/WishlistStrings.kt; changes=[+deleteButton, confirmDeleteButton, confirmDeleteItem*, confirmDeleteWishlist*]
- action=update; target=features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemEditViewModel.kt; changes=[+canDelete, showDeleteDialogState, onDelete/onConfirmDelete/onCancelDelete → model.deleteWishlistItem then interactor.onNavigateBack]
- action=update; target=features/ui/wishlist/src/commonMain/kotlin/ui/WishlistEditViewModel.kt; changes=[same delete pattern → model.deleteWishlist]
- action=update; target=features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemViewModel.kt; changes=[reload every resume; interactor.onBack when item==null after load]
- action=update; target=features/ui/wishlist/src/commonMain/kotlin/ui/WishlistViewModel.kt; changes=[interactor.onBack when wishlist==null after load]
- action=update; target=features/ui/wishlist/src/{js,jvm,android}Main/.../WishlistItemEditView.kt + WishlistEditView.kt; changes=[danger Delete button when canDelete + delete confirm modal/AlertDialog]
- action=update; target=features/ui/users/build.gradle; changes=[+auth.client +admin.client deps]
- action=update; target=features/ui/users/src/commonMain/kotlin/ui/UsersListModel.kt; changes=[+isCurrentUserRoot(), +deleteUser(id)]
- action=update; target=features/ui/users/src/commonMain/kotlin/Plugin.kt; changes=[model impl uses ClientAuthFeature.getMe username=="root" + AdminFeature.usersManagement.delete]
- action=update; target=features/ui/users/src/commonMain/kotlin/ui/UsersListViewModel.kt; changes=[+isRootState, deleteTargetState, deleteStepState(0/1/2), onDeleteUserRequest/onConfirmDeleteFirst/onConfirmDeleteSecond/onCancelDelete; reload after delete]
- action=update; target=features/ui/users/src/commonMain/kotlin/UsersListStrings.kt; changes=[+delete/double-confirm strings]
- action=update; target=features/ui/users/src/{js,jvm,android}Main/.../UsersListView.kt; changes=[per-row danger Delete button when isRoot + two sequential confirm modals/dialogs]
- action=update; target=READMEs ui/wishlist, ui/users, admin, auth; changes=[documented deletion behavior + cascade + purgeUser]

**outcome:** owner can delete own wishlist/item with confirm + auto-back semantics; root can delete users from users list with double confirm and full server-side cascade. BUILD SUCCESSFUL for :wishlist.features.admin.server, :wishlist.features.ui.wishlist, :wishlist.features.ui.users (JS+JVM+Android+lint). ast-index updated (28 files).

**Constraints respected:** admin user delete reuses existing repos/services (no new repo/table); root gating client-side for UX, server still enforces 403; MVVM (state in VM, dumb views).

---

### 2026-05-30 — Session 16: Refactor — AuthConfig server-side only, isRegistrationAvailable on AuthFeature

**Prompt:** `AuthFeature must now provide whole AuthConfig. It must provide flag isRegistrationAvailable instead. AuthConfig must be presented on server side only`

**Actions:**
- action=delete; target=features/auth/common/src/commonMain/kotlin/models/AuthConfig.kt
- action=create; target=features/auth/server/src/commonMain/kotlin/models/AuthConfig.kt; changes=[moved AuthConfig here; package=dev.inmo.wishlist.features.auth.server.models]
- action=update; target=features/auth/common/src/commonMain/kotlin/AuthFeature.kt; changes=[replaced getConfig(): AuthConfig with isRegistrationAvailable(): Boolean]
- action=update; target=features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt; changes=[import AuthConfig from server.models; getConfig() → isRegistrationAvailable() = enableRegistration]
- action=update; target=features/auth/server/src/commonMain/kotlin/configurators/AuthRoutingsConfigurator.kt; changes=[import server.models.AuthConfig; GET /auth/config responds AuthConfig(authFeature.isRegistrationAvailable())]
- action=update; target=features/auth/client/src/commonMain/kotlin/KtorAuthFeature.kt; changes=[removed AuthConfig import; getConfig() → isRegistrationAvailable() parses JsonObject["enableRegistration"]]
- action=update; target=features/auth/client/src/commonMain/kotlin/AuthFeatureService.kt; changes=[removed AuthConfig import; getConfig() → isRegistrationAvailable() delegate]
- action=update; target=features/ui/auth/src/commonMain/kotlin/Plugin.kt; changes=[authFeature.getConfig().enableRegistration → authFeature.isRegistrationAvailable()]

**outcome:** AuthConfig is server-only DTO; AuthFeature.isRegistrationAvailable() is the single cross-cutting flag; client parses JSON field without importing server type

---

### 2026-05-30 — Session 15: Registration feature in AuthView

**Prompt:** `Add opportunity to register in AuthView. It must be configurable throw server configuration and passed to client via AuthFeature. Registration is also must be there, but also it must respect enableRegistration option in auth feature config`

**Actions:**
- action=update; target=features/auth/common/src/commonMain/kotlin/Constants.kt; changes=[added registerPathPart, configPathPart]
- action=create; target=features/auth/common/src/commonMain/kotlin/models/RegisterRequest.kt; changes=[new DTO: username+password]
- action=create; target=features/auth/common/src/commonMain/kotlin/models/AuthConfig.kt; changes=[new DTO: enableRegistration: Boolean]
- action=update; target=features/auth/common/src/commonMain/kotlin/AuthFeature.kt; changes=[added register(), getConfig() to interface]
- action=update; target=features/auth/server/src/commonMain/kotlin/Config.kt; changes=[added enableRegistration: Boolean = false]
- action=update; target=features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt; changes=[added WriteUsersRepo param, enableRegistration param, implemented register() and getConfig()]
- action=update; target=features/auth/server/src/commonMain/kotlin/Plugin.kt; changes=[pass WriteUsersRepo + enableRegistration to AuthFeatureService]
- action=update; target=features/auth/server/src/commonMain/kotlin/configurators/AuthRoutingsConfigurator.kt; changes=[added GET /auth/config and POST /auth/register routes]
- action=update; target=features/auth/client/src/commonMain/kotlin/KtorAuthFeature.kt; changes=[added register() and getConfig() HTTP implementations]
- action=update; target=features/auth/client/src/commonMain/kotlin/AuthFeatureService.kt; changes=[delegated register() (saves creds) and getConfig()]
- action=update; target=features/auth/client/src/commonMain/kotlin/configurators/BearerAuthHttpClientConfigurator.kt; changes=[added register+config paths to sendWithoutRequest exclusions]
- action=update; target=features/ui/auth/src/commonMain/kotlin/ui/AuthModel.kt; changes=[added isRegistrationEnabled() and register() methods]
- action=update; target=features/ui/auth/src/commonMain/kotlin/ui/AuthViewModel.kt; changes=[added registerModeState, registrationEnabledState, onToggleRegisterForm(), onCancelForm(), onRegister()]
- action=update; target=features/ui/auth/src/commonMain/kotlin/Plugin.kt; changes=[implemented isRegistrationEnabled() and register() in AuthModel anon object]
- action=update; target=features/ui/auth/src/commonMain/kotlin/AuthStrings.kt; changes=[added registerButton, submitRegisterButton, errorRegisterFailed strings]
- action=update; target=features/ui/auth/src/jsMain/kotlin/ui/AuthView.kt; changes=[4-state UI: logged-in / collapsed(+register btn) / expanded-login / expanded-register]
- action=update; target=features/ui/auth/src/jvmMain/kotlin/ui/AuthView.kt; changes=[same 4-state layout]
- action=update; target=features/ui/auth/src/androidMain/kotlin/ui/AuthView.kt; changes=[same 4-state layout]

**outcome:** registration fully wired; server gates via enableRegistration in config.json; client discovers capability via GET /auth/config; UI shows Register button only when enabled; all modules build clean

---

### 2026-05-30 — Session 14: WishlistsListView back button via interactor

**Prompt:** `For newly created back button do as you did for other back buttons - in WishlistsListViewModel must be method which will call back in its interactor and in its realization will be called pop`

**Actions:**
- action=update; target=features/ui/wishlist/src/commonMain/kotlin/ui/WishlistsListViewInteractor.kt; changes=[added suspend fun onBack(node)]
- action=update; target=features/ui/wishlist/src/commonMain/kotlin/ui/WishlistsListViewModel.kt; changes=[added fun onBack() → scope.launchLoggingDropExceptions { interactor.onBack(node) }]
- action=update; target=client/src/commonMain/kotlin/ClientPlugin.kt; changes=[added onBack impl in WishlistsListViewInteractor anon object → node.chain.pop()]
- action=update; target=features/ui/wishlist/src/jsMain/kotlin/ui/WishlistsListView.kt; changes=[chain.pop() → viewModel.onBack()]
- action=update; target=features/ui/wishlist/src/jvmMain/kotlin/ui/WishlistsListView.kt; changes=[chain.pop() → viewModel.onBack()]
- action=update; target=features/ui/wishlist/src/androidMain/kotlin/ui/WishlistsListView.kt; changes=[chain.pop() → viewModel.onBack()]

**outcome:** back navigation follows MVVM pattern; views call viewModel.onBack(); interactor impl does node.chain.pop()

---

### 2026-05-30 — Session 13: WishlistsListView back button

**Prompt:** `For all WishlistsListView add back button if this view is not the only in the chain`

**Project state at session start:**
- branch=master; WishlistsListView exists on JS/JVM/Android; no back button present

**Actions:**
- action=update; target=features/ui/wishlist/src/jsMain/kotlin/ui/WishlistsListView.kt; changes=[added `val stack by chain.stackFlow.collectAsState()`; wrapped H1 in d-flex gap-2 div; added btn btn-outline-secondary btn-sm Back button visible when stack.size > 1; onClick→chain.pop()]
- action=update; target=features/ui/wishlist/src/jvmMain/kotlin/ui/WishlistsListView.kt; changes=[added stack collectAsState; added inner Row(spacedBy 8dp) wrapping back Button + title Text; back button visible when stack.size > 1; onClick→chain.pop()]
- action=update; target=features/ui/wishlist/src/androidMain/kotlin/ui/WishlistsListView.kt; changes=[same as JVM but Material3; translation(resources) form kept]

**Pattern:** `chain.stackFlow.collectAsState()` gives reactive stack size; `chain.pop()` navigates back; `chain` is available as `this.chain` inside ComposeView (the view is the node)

**outcome:** back button appears on all platforms when WishlistsListView is not the root of its chain

---

### 2026-05-30 — Session 12: AuthView rewrite to Bootstrap

**Prompt:** `Rewrite features/ui/auth/src/jsMain/kotlin/ui/AuthView.kt onto bootstrap too`

**Project state at session start:**
- branch=master; Bootstrap bundled+loaded; TopBarView already Bootstrap (Session 11)

**Actions:**
- action=rewrite; target=features/ui/auth/src/jsMain/kotlin/ui/AuthView.kt; changes=[removed outer wrapper Div; loggedIn/collapsed states: Button(btn btn-outline-light btn-sm); expanded state: Div(d-flex gap-2 align-items-center) containing form-control form-control-sm inputs + btn-light submit + btn-outline-light cancel + text-warning small error span; updated KDoc]

**outcome:** AuthView renders as inline Bootstrap form inside navbar-dark; consistent with TopBarView Bootstrap context

---

### 2026-05-30 — Session 11: TopBar rewrite to Bootstrap navbar

**Prompt:** `Rewrite top bar onto bootstrap's one`

**Project state at session start:**
- branch=master; last_commit=7379006 (add main page with users list, top bar, and server URL screen)
- status=modified(client/android/src/main/kotlin/ClientAndroidPlugin.kt, client/src/commonMain/kotlin/ClientPlugin.kt, client/src/jsMain/kotlin/ClientJSPlugin.kt, client/src/jvmMain/kotlin/ClientJVMPlugin.kt)
- Bootstrap already bundled: client/src/jsMain/resources/css/bootstrap.min.css + js/bootstrap.bundle.min.js; loaded in index.html

**Actions:**
- action=rewrite; target=features/ui/topBar/src/jsMain/kotlin/ui/TopBarView.kt; changes=[replaced custom Div+TopBarViewStylesheet layout with Bootstrap navbar: Nav(navbar navbar-expand navbar-dark bg-primary) → container-fluid → navbar-brand (title) + d-flex div (auth widget); removed B import; added Nav+A imports]
- action=delete; target=features/ui/topBar/src/jsMain/kotlin/ui/TopBarViewStylesheet.kt; reason=no longer needed; Bootstrap utility classes replace all custom CSS

**outcome:** JS TopBarView renders as Bootstrap dark primary navbar; custom stylesheet eliminated

---

### 2026-05-30 — Session 10: Users list + TopBar scaffold + server URL view

**Prompt:** `Main page shows list of users (reuse WishlistsListView for each user's wishlists); Auth shows compact Login/Logout widget; Android/Desktop bootstrap from server URL view first.`

**Project state at session start:**
- branch=master; last_commit=093a4af (build fix); status=modified(agents/CODING.md, agents/SHORTCUTS.md, client/src/commonMain/kotlin/ClientPlugin.kt, settings.gradle) + untracked(features/ui/scaffold/)
- features/ui/scaffold already present: ScaffoldViewConfig (topConfig/leftConfig/mainConfig polymorphic), per-platform ScaffoldView (InjectNavigationChain per slot)
- features/users/{server,client}: stubs only (no routes, no implementations)
- features/ui/{users,topBar,serverUrl}: did not exist

**Actions:**
- action=create; target=features/users/server/src/commonMain/kotlin/{UsersFeature.kt,services/UsersService.kt}; changes=[suspend fun getAll(): List<RegisteredUser>; service backed by ReadUsersRepo]
- action=update; target=features/users/common/src/commonMain/kotlin/Constants.kt; changes=[usersPrefixPathPart="users", usersGetAllPathPart="getAll"]
- action=create; target=features/users/server/src/jvmMain/kotlin/configurators/UsersRoutingsConfigurator.kt; changes=[GET /users/getAll (root-only, no auth wrapper)]
- action=update; target=features/users/server/src/{commonMain,jvmMain}/kotlin/{Plugin.kt,JVMPlugin.kt}; changes=[UsersService+Feature binding; UsersRoutingsConfigurator registration]
- action=create; target=features/users/client/src/commonMain/kotlin/{UsersFeature.kt,KtorUsersFeature.kt}; changes=[client interface + Ktor impl calling /users/getAll]
- action=update; target=features/users/client/src/commonMain/kotlin/Plugin.kt; changes=[KtorUsersFeature+UsersFeature binding]
- action=create; target=features/ui/users/*; changes=[new module: build.gradle, README.md, commonMain MVVM (UsersListStrings/ViewConfig/Model/Interactor/ViewModel/Plugin.kt), per-platform UsersListView, JSPlugin/JVMPlugin/AndroidPlugin; Interactor→onUserSelected(userId)→node.chain.push(WishlistsListViewConfig(userId))]
- action=create; target=features/ui/topBar/*; changes=[new module: build.gradle, README.md, commonMain MVVM (TopBarStrings/ViewConfig/Interactor/ViewModel/Plugin.kt), per-platform TopBarView embedding AuthViewConfig via InjectNavigationChain<ViewConfig>{InjectNavigationNode(AuthViewConfig())}, JSPlugin/JVMPlugin/AndroidPlugin; topBar has LogOut button when auth'd, Change-URL button (JS skips it)]
- action=create; target=features/ui/serverUrl/*; changes=[new JVM/Android-only module: build.gradle (mppJvmJsAndroidWithCompose for JS commonMain compat), README.md, commonMain MVVM (ServerUrlStrings/ViewConfig/Model/Interactor/ViewModel/Plugin.kt), jvmMain+androidMain views only; Interactor→onSaved→dropNodesInSubTree{it.config is ServerUrlViewConfig}, push Scaffold if absent]
- action=update; target=features/ui/auth/src/commonMain/kotlin/{AuthStrings.kt,ui/AuthModel.kt,ui/AuthViewModel.kt,Plugin.kt}; changes=[removed serverAddress fields; added userAuthorisedState, logout(); rewritten as compact 3-state widget (collapsed→"Log in", expanded→form, loggedIn→"Log out")]
- action=rewrite; target=features/ui/auth/src/{jsMain,jvmMain,androidMain}/kotlin/ui/AuthView.kt; changes=[compact Login/form/Logout widget; auth state no longer overlay]
- action=delete; target=features/ui/auth/src/{commonMain,jsMain,jvmMain,androidMain}/kotlin/utils/DefaultServerUrl*.kt; changes=[4 files removed]
- action=update; target=features/ui/wishlist/src/commonMain/kotlin/ui/{WishlistsListViewConfig.kt,WishlistsModel.kt,WishlistsListViewModel.kt,Plugin.kt}; changes=[added userId: UserId?=null; getUserWishlists(userId) method; loadWishlists branches on node.config.userId]
- action=update; target=client/src/commonMain/kotlin/ClientPlugin.kt; changes=[added mainScaffoldConfig(top=TopBarViewConfig, main=UsersListViewConfig); UsersListViewInteractor→push WishlistsListViewConfig(userId); TopBarViewInteractor→push ServerUrlViewConfig(); ServerUrlViewInteractor→push Scaffold if absent, drop self; AuthViewInteractor stateless]
- action=update; target=client/src/{jsMain,jvmMain}/kotlin/Main.kt + android/src/main/kotlin/MainActivity.kt; changes=[added users.client + ui.scaffold + ui.users + ui.topBar + ui.serverUrl plugin registrations]
- action=rewrite; target=client/src/jsMain/kotlin/ClientJSPlugin.kt; changes=[dropped UrlParametersNavigationConfigsRepo (was tied to WishlistsListViewConfig root); use InMemoryRepo; push mainScaffoldConfig after startPlugin]
- action=update; target=client/src/jvmMain/kotlin/ClientJVMPlugin.kt + android/src/main/kotlin/ClientAndroidPlugin.kt; changes=[read ServerUrlStorage.getServerUrl() in startPlugin; push ServerUrlViewConfig if blank else ScaffoldViewConfig]
- action=update; target=settings.gradle + client/build.gradle; changes=[include features:ui:{users,topBar,serverUrl}; add commonMain dependencies]
- action=create; target=features/{users,ui/auth,ui/wishlist}/README.md; changes=[per CODING.md feature README rule]

**Navigation flow wired:**
- root chain: EmptyConfig → (if serverUrl blank on Android/Desktop: ServerUrlViewConfig | else: ScaffoldViewConfig); ServerUrlViewConfig.onSaved→dropNodesInSubTree, push Scaffold; JS always ScaffoldViewConfig
- ScaffoldViewConfig: topConfig=TopBarViewConfig (via InjectNavigationChain), mainConfig=UsersListViewConfig (via InjectNavigationChain)
- TopBarView embeds AuthViewConfig via InjectNavigationChain<ViewConfig>{InjectNavigationNode(AuthViewConfig())} — auth owns state, no root overlay
- UsersListView→onUserSelected(userId)→node.chain.push(WishlistsListViewConfig(userId))
- TopBarView→onChangeServerUrl→rootChain.push(ServerUrlViewConfig())
- AuthView: 3 states (logged-out collapsed=login button | logged-out expanded=form | logged-in=logout button)

**Build:** `./gradlew build` BUILD SUCCESSFUL (JVM server+client, JS client, Android)

**outcome:** users CRUD full-stack wired; main page shows users→select→view wishlists; TopBar auth widget compact; Android/Desktop boot from server URL

---

### 2026-05-27 — Session 9: Admin panel UI wiring

**Prompt:** `Add UI (scenario) for admin panel. List→View→Edit canvas for users and wishlists. Support adding users/wishlists/items. Wishlist create has owner dropdown. User view shows wishlists.`

**Project state at session start:**
- branch=master; last_commit=7c898c9 (build fix); status=modified(client/build.gradle, settings.gradle) + untracked(features/ui/adminPanel/)
- features/ui/adminPanel MVVM already present: 8 ViewConfigs, 8 ViewModels, 8 Interactor interfaces, JS/JVM/Android views — all implemented
- missing: interactor implementations in ClientPlugin; plugin registrations in Main.kt files; Android plugin only registered AdminPanelView (not all 8)

**Actions:**
- action=update; target=features/ui/adminPanel/src/androidMain/kotlin/AndroidPlugin.kt; changes=[added 7 missing NavigationNodeFactory entries: AdminUsersListView, AdminUserView, AdminUserEditView, AdminWishlistsListView, AdminWishlistView, AdminWishlistEdit, AdminWishlistItemEditView]
- action=update; target=client/src/commonMain/kotlin/ClientPlugin.kt; changes=[added 8 admin interactor impls (stateless anon objects, all push/pop); added InjectNavigationNode(AdminPanelViewConfig()) inside initNavigation block; added 18 import lines for admin panel types]
- action=update; target=client/src/jsMain/kotlin/Main.kt; changes=[added dev.inmo.wishlist.features.ui.adminPanel.JSPlugin]
- action=update; target=client/src/jvmMain/kotlin/Main.kt; changes=[added dev.inmo.wishlist.features.ui.adminPanel.JVMPlugin]
- action=update; target=client/android/src/main/kotlin/MainActivity.kt; changes=[added dev.inmo.wishlist.features.admin.client.AndroidPlugin + dev.inmo.wishlist.features.ui.adminPanel.AndroidPlugin]
- action=create; target=features/ui/adminPanel/README.md
- action=update; target=HISTORY.md (root); changes=[added session entry]

**Navigation flow wired:**
- root=AdminPanelViewConfig (InjectNavigationNode in initNavigation)
- AdminPanelView→onOpenUsers→AdminUsersListViewConfig; →onOpenWishlists→AdminWishlistsListViewConfig
- AdminUsersListView→onUserSelected(userId)→AdminUserViewConfig(userId); →onCreateUser→AdminUserEditViewConfig(null)
- AdminUserView→onBack→pop; →onEditUser→AdminUserEditViewConfig(node.config.userId); →onOpenWishlist(id)→AdminWishlistViewConfig(id); →onAddWishlist(userId)→AdminWishlistEditViewConfig(null,userId)
- AdminUserEditView→onNavigateBack→pop; →onSaved→pop
- AdminWishlistsListView→onWishlistSelected(id)→AdminWishlistViewConfig(id); →onCreateWishlist→AdminWishlistEditViewConfig(null)
- AdminWishlistView→onBack→pop; →onEditWishlist→AdminWishlistEditViewConfig(wishlistId); →onAddItem→AdminWishlistItemEditViewConfig(null,wishlistId); →onEditItem(itemId,wishlistId)→AdminWishlistItemEditViewConfig(itemId,wishlistId)
- AdminWishlistEditView→onNavigateBack→pop; →onSaved→pop
- AdminWishlistItemEditView→onNavigateBack→pop; →onSaved→pop

**outcome:** admin panel UI fully wired; ast-index rebuild required

---

### 2026-05-26 — Session 8: onboarding

**Prompt:** `USE @AGENTS.md`

**Actions:**
- action=read; target=AGENTS.md; result=AML-HIP protocol loaded
- action=read; target=agents/SHORTCUTS.md; result=task→file map loaded
- action=read; target=agents/ALL.md + local.ALL.md; result=mandatory rules + Bootstrap UI rules loaded
- action=read; target=agents/HISTORY.md; result=project history loaded (Sessions 1-7)

**Project state at session start:**
- branch=master; last_commit=291a2e7 (add WishlistItemView, public read routes, and MVVM interactors); status=clean
- features present: sample, users, auth, common, wishlist (full-stack), ui/sample, ui/auth, ui/wishlist
- wishlist routes: GET /wishlist/getByUserId/{userId}, GET /wishlist/getMy, POST /wishlist/create, PUT /wishlist/update/{id}, DELETE /wishlist/delete/{id}
- wishlistItem routes: GET /wishlistItem/getByWishlistId/{id}, POST /wishlistItem/create, PUT /wishlistItem/update/{id}, DELETE /wishlistItem/delete/{id}
- UI navigation: WishlistsList→WishlistView→WishlistEdit/WishlistItemEdit; back=node.chain.pop(); dirty=confirm modal
- URL navigation (JS): ?wishlist=id, ?edit=true, ?wishlist_item=id via UrlParametersNavigationConfigsRepo

**Source changes:**
- action=update; target=agents/CODING.md; changes=[Interactor section: added subsection "Intra-feature navigation interactors (wishlist pattern)"; content=stateless anon object pattern, node.chain.push/pop, one-interactor-per-ViewModel rule, method-naming convention, contrast with reactive AuthViewInteractor pattern]

---

### 2026-05-26 — Session 7: actualize READMEs after commit 871972b

**Prompt:** Fill *.md files according to changes in last commit (replace configs holder storage)

**Commit:** 871972b; message="replace configs holder storage"

**Changed files in commit:**
- client/src/commonMain/kotlin/ClientPlugin.kt — removed InjectNavigationChain(WishlistsListViewConfig()) block
- client/src/jsMain/kotlin/ClientJSPlugin.kt — NavigationConfigsRepo changed from InMemory to UrlParametersNavigationConfigsRepo; URL param schema: ?wishlist=id, ?edit=true, ?wishlist_item=id; WishlistsListViewConfig always root in decoder
- features/common/client/src/commonMain/kotlin/utils/ConfigHolderFind.kt — NEW: findConfig<T,R> extension on ConfigHolder<T>; DFS Chain→Node→subnode→subchains
- features/common/server/src/jvmMain/kotlin/JVMPlugin.kt — added HttpStatusCode import (minor)
- features/ui/wishlist/src/commonMain/kotlin/ui/WishlistEditViewModel.kt — init: merge(flowOf(Unit), node.onResumeFlow).takeWhile { inited==false } (first-resume only)
- features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemEditViewModel.kt — same first-resume pattern
- features/ui/wishlist/src/commonMain/kotlin/ui/WishlistViewModel.kt — merge(flowOf(Unit), node.onResumeFlow) every resume; extracted loadWishlist() as private suspend fun
- features/ui/wishlist/src/commonMain/kotlin/ui/WishlistsListViewModel.kt — same every-resume pattern; loadWishlists() private suspend fun (was public fun)

**README updates:**
- action=update; target=features/ui/wishlist/README.md; changes=[Architecture Notes: ViewModel reload patterns; JS URL navigation scheme; WishlistsListViewConfig root moved to ClientJSPlugin UrlParametersNavigationConfigsRepo]
- action=update; target=features/common/README.md; changes=[Models: added findConfig<T,R> utility row]

---

### 2026-05-26 — Session 6: onboarding

**Prompt:** `USE @AGENTS.md`

**Actions:**
- action=read; target=AGENTS.md; result=AML-HIP protocol loaded
- action=read; target=agents/SHORTCUTS.md; result=task→file map loaded
- action=read; target=agents/ALL.md; result=mandatory rules loaded
- action=read; target=agents/HISTORY.md; result=project history loaded (Sessions 1-5)
- action=read; target=features/wishlist files (4 files); result=current state verified

**Project state at session start:**
- branch=master; last_commit=81e4f5c (add ast-index); uncommitted_changes=yes
- staged: agents/ALL.md, agents/CODING.md, agents/HISTORY.md
- unstaged: features/wishlist/README.md, JVMPlugin.kt, ExposedWishlistItemRepo.kt, ExposedWishlistRepo.kt
- features present: sample, users, auth, common, wishlist (full-stack), ui/sample, ui/auth
- wishlist routes: GET /wishlist/getByUserId/{userId}, GET /wishlist/getMy, POST /wishlist/create, PUT /wishlist/update/{id}, DELETE /wishlist/delete/{id}
- wishlistItem routes: GET /wishlistItem/getByWishlistId/{id}, POST /wishlistItem/create, PUT /wishlistItem/update/{id}, DELETE /wishlistItem/delete/{id}
- links schema: wishlist_item_links table (item_id FK CASCADE, link TEXT, PK(item_id,link))
- BUG DETECTED: ExposedWishlistRepo.kt has duplicate `init { initTable() }` blocks (lines 48 and 75); initTable() called twice on startup

**Source changes: completed after restart — full wishlist UI implemented**

### 2026-05-26 — Session 6 (continued): Wishlist UI — 4 MVVMs

**Prompt summary:** Add UI for wishlists and wishlist items. 4 MVVMs in `features/ui/wishlist`. JS=Bootstrap+Compose HTML, JVM=Material v2, Android=Material3. Back=`node.chain.pop()`; edit views show discard-modal if dirty.

**Actions:**
- action=create; target=features/ui/wishlist/build.gradle; deps=[common.client, wishlist.client, auth.client]
- action=create; target=features/ui/wishlist/src/commonMain/kotlin/WishlistStrings.kt
- action=create; target=features/ui/wishlist/src/commonMain/kotlin/ui/WishlistsModel.kt
- action=create; target=features/ui/wishlist/src/commonMain/kotlin/ui/WishlistsListViewConfig.kt
- action=create; target=features/ui/wishlist/src/commonMain/kotlin/ui/WishlistsListViewModel.kt
- action=create; target=features/ui/wishlist/src/commonMain/kotlin/ui/WishlistViewConfig.kt (wishlistId: WishlistId)
- action=create; target=features/ui/wishlist/src/commonMain/kotlin/ui/WishlistViewModel.kt
- action=create; target=features/ui/wishlist/src/commonMain/kotlin/ui/WishlistEditViewConfig.kt (wishlistId: WishlistId? — null=create)
- action=create; target=features/ui/wishlist/src/commonMain/kotlin/ui/WishlistEditViewModel.kt
- action=create; target=features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemEditViewConfig.kt (wishlistItemId: WishlistItemId?, wishlistId: WishlistId)
- action=create; target=features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemEditViewModel.kt
- action=create; target=features/ui/wishlist/src/commonMain/kotlin/Plugin.kt; note=WishlistsModel impl uses getMyWishlists().find for getWishlist(id)
- action=create; target=features/ui/wishlist/src/jsMain/kotlin/JSPlugin.kt + 4 views (Bootstrap)
- action=create; target=features/ui/wishlist/src/jvmMain/kotlin/JVMPlugin.kt + 4 views (Material v2)
- action=create; target=features/ui/wishlist/src/androidMain/kotlin/AndroidPlugin.kt + 4 views (Material3)
- action=create; target=features/ui/wishlist/README.md
- action=update; target=settings.gradle; adds=:features:ui:wishlist
- action=update; target=client/build.gradle; adds=api project(":wishlist.features.ui.wishlist")
- action=update; target=client/src/jsMain/kotlin/Main.kt; adds=JSPlugin
- action=update; target=client/src/jvmMain/kotlin/Main.kt; adds=JVMPlugin
- action=update; target=client/android/src/main/kotlin/MainActivity.kt; adds=AndroidPlugin
- action=update; target=client/src/commonMain/kotlin/ClientPlugin.kt; changes=[InjectNavigationNode(SampleViewConfig()) → InjectNavigationNode(WishlistsListViewConfig())]
- action=update; target=agents/local.ALL.md; adds=Bootstrap JS UI rules

**Navigation flow:**
- WishlistsList (root) → click → WishlistView (push to chain)
- WishlistView → Edit button (owner only) → WishlistEdit (push)
- WishlistView → item click (owner only) → WishlistItemEdit (push)
- WishlistView/WishlistEdit/WishlistItemEdit → back → node.chain.pop()
- WishlistEdit/WishlistItemEdit → back when dirty → show confirm modal

**Key decisions:**
- No ViewInteractor: all navigation intra-feature, ViewModel pushes/pops directly
- isOwner = wishlist.userId == getCurrentUserId() via ClientAuthFeature.getMe()?.id
- Price: Double string → Amount(double) on save
- JS modal: inline Bootstrap classes, no Bootstrap JS dependency
- WishlistsListViewConfig is now root node in ClientPlugin (replaces SampleViewConfig)

---

### 2026-05-26 — Session 5: onboarding

**Prompt:** `USE @AGENTS.md`

**Actions:**
- action=read; target=AGENTS.md; result=AML-HIP protocol loaded
- action=read; target=agents/SHORTCUTS.md; result=task→file map loaded
- action=read; target=agents/ALL.md; result=mandatory rules loaded
- action=read; target=agents/HISTORY.md; result=project history loaded (Sessions 1-4)

**Project state at session start:**
- branch=master; last_commit=81e4f5c (add ast-index); status=clean
- features present: sample, users, auth, common, wishlist (full-stack), ui/sample, ui/auth
- wishlist routes: GET /wishlist/getMy, POST /wishlist/create, PUT /wishlist/update/{id}, DELETE /wishlist/delete/{id}
- wishlistItem routes: GET /wishlistItem/getByWishlistId/{id}, POST /wishlistItem/create, PUT /wishlistItem/update/{id}, DELETE /wishlistItem/delete/{id}
- ownership: WishlistService + WishlistItemService enforce callerId == owner; null=not_found, false=unauthorized, true=success
- all feature dirs have README.md with Operator Notes section

---

### 2026-05-26 — Session 5 (continued): Extract links to separate Exposed table

**Prompt summary:** Extract `links` field from `ExposedWishlistItemRepo` into a private internal `wishlist_item_links` table. Table instantiated and used only within that file.

**Actions:**
- action=update; target=features/wishlist/common/src/jvmMain/kotlin/repo/ExposedWishlistItemRepo.kt; changes=[remove linksColumn/linksSerializer/Json param; add private linksTable anon object: Table("wishlist_item_links") {itemId FK CASCADE, link TEXT, PK(itemId,link)}; add linksFor(itemId) helper; update.asObject reads linksFor; update() deletes+reinserts links when id!=null; InsertStatement.asObject inserts links; init creates linksTable schema via SchemaUtils; remove kotlinx.serialization imports; add ReferenceOption/Table/SchemaUtils/deleteWhere/insert/selectAll imports]
- action=update; target=features/wishlist/common/src/jvmMain/kotlin/JVMPlugin.kt; changes=[ExposedWishlistItemRepo(get(),get()) → ExposedWishlistItemRepo(get()): Json param removed]
- action=update; target=features/wishlist/README.md; changes=[Architecture Notes: links table schema, FK CASCADE note, sub-query read note]

**Schema changes:**
- table=wishlist_item_links; columns=[item_id BIGINT FK→wishlist_items.id ON DELETE CASCADE, link TEXT]; PK=(item_id,link)
- wishlist_items: linksColumn (TEXT JSON) removed

**Ownership semantics unchanged. Routes unchanged.**

---

### 2026-05-26 — Session 4 (continued): Initial README.md for all features

**Prompt summary:** Create README.md for all existing features following the rule from ALL.md.

**Actions:**
- action=create; target=features/sample/README.md
- action=create; target=features/users/README.md
- action=create; target=features/auth/README.md
- action=create; target=features/common/README.md
- action=create; target=features/wishlist/README.md
- action=create; target=features/ui/sample/README.md
- action=create; target=features/ui/auth/README.md

**All READMEs contain:**
- `## Operator Notes` section (empty, ready for human input)
- Overview, Routes table, Models table, Architecture Notes

---

### 2026-05-26 — Session 4 (continued): Feature README.md rule

**Prompt summary:** Establish a mandatory README.md rule for all features. Coders and architects must update it after changes. Human operator writes `## Operator Notes`; agents must read and respect it, never modify it.

**Actions:**
- action=update; target=agents/ALL.md; adds=[Feature README.md rule: structure, Operator Notes semantics, agent obligations for coding/architecture agents]
- action=update; target=agents/CODING.md; adds=[Feature README.md cross-ref section at top]
- action=update; target=agents/ARCHITECTURE.md; adds=[Feature README.md cross-ref section at top]

**Rule summary:**
- Every feature must have README.md with `## Operator Notes` section at top
- Agents MUST read README.md (especially Operator Notes) before working on any feature
- Agents MUST NOT modify `## Operator Notes`; violation → stop and ask operator
- Coding agents: update routes/models/behavior/deps after code changes
- Architecture agents: update `## Architecture Notes` after architectural decisions
- README.md structure: Overview + Routes table + Models + Architecture Notes

---

### 2026-05-26 — Session 4 (continued): renames + getMyWishlists + item ownership

**Prompt summary:** Rename WishlistFeature→WishlistsFeature, WishlistItemFeature→WishlistsItemsFeature. Add getMyWishlists to WishlistsFeature. Enforce caller ownership on WishlistItem mutations.

**Actions:**
- action=delete; target=features/wishlist/common/src/commonMain/kotlin/WishlistFeature.kt
- action=delete; target=features/wishlist/common/src/commonMain/kotlin/WishlistItemFeature.kt
- action=create; target=features/wishlist/common/src/commonMain/kotlin/WishlistsFeature.kt; adds=[getMyWishlists(): List<RegisteredWishlist>]
- action=create; target=features/wishlist/common/src/commonMain/kotlin/WishlistsItemsFeature.kt; changes=[KDocs updated to document ownership enforcement]
- action=update; target=features/wishlist/common/src/commonMain/kotlin/Constants.kt; adds=wishlistGetMyPathPart="getMy"
- action=update; target=features/wishlist/server/src/commonMain/kotlin/services/WishlistService.kt; adds=getMyWishlists(callerId: UserId): List<RegisteredWishlist>
- action=update; target=features/wishlist/server/src/commonMain/kotlin/services/WishlistItemService.kt; changes=[drops WishlistItemFeature impl; adds WishlistRepo param; create(item,callerId)->RegisteredWishlistItem?; update(id,item,callerId)->Boolean?; delete(id,callerId)->Boolean?]; semantics=[null=not_found/parent_not_found, false=unauthorized, true=success]
- action=update; target=features/wishlist/server/src/commonMain/kotlin/configurators/WishlistRoutingsConfigurator.kt; adds=GET /wishlist/getMy route using getCallerUserIdOrAnswerUnauthorized
- action=update; target=features/wishlist/server/src/commonMain/kotlin/configurators/WishlistItemRoutingsConfigurator.kt; changes=[takes WishlistItemService; create/update/delete call getCallerUserIdOrAnswerUnauthorized; update/delete return 403 on false / 404 on null]
- action=update; target=features/wishlist/server/src/commonMain/kotlin/Plugin.kt; changes=[WishlistItemService(get(),get()) — two deps; removes WishlistItemFeature binding]
- action=update; target=features/wishlist/client/src/commonMain/kotlin/KtorWishlistFeature.kt; changes=[implements WishlistsFeature; adds getMyWishlists() → GET /wishlist/getMy]
- action=update; target=features/wishlist/client/src/commonMain/kotlin/KtorWishlistItemFeature.kt; changes=[implements WishlistsItemsFeature]
- action=update; target=features/wishlist/client/src/commonMain/kotlin/Plugin.kt; changes=[binds WishlistsFeature, WishlistsItemsFeature]

**Route additions:**
- GET /wishlist/getMy → caller from bearer → List<RegisteredWishlist>

**WishlistItem ownership semantics:**
- create: checks wishlistRepo.getById(newItem.wishlistId).userId == callerId; null=not_found_or_unauthorized
- update/delete: checks item→parent_wishlist.userId == callerId; null=not_found, false=unauthorized, true=success
- routes: update/delete → 403 on false / 404 on null (matching WishlistRoutingsConfigurator pattern)

---

### 2026-05-25 — Session 4: onboarding

**Prompt:** `USE @AGENTS.md`

**Actions:**
- action=read; target=AGENTS.md; result=AML-HIP protocol loaded
- action=read; target=agents/SHORTCUTS.md; result=task→file map loaded
- action=read; target=agents/ALL.md; result=mandatory rules loaded
- action=read; target=agents/HISTORY.md; result=project history loaded (Sessions 1-3)

**Project state at session start:**
- branch=master; last_session=Session 3; status=wishlist ownership enforcement complete
- features present: sample, users, auth, ui/auth, ui/serverAddress, ui/sample, wishlist (full-stack)
- wishlist routes: POST /wishlist/create, PUT /wishlist/update/{id}, DELETE /wishlist/delete/{id}; userId from bearer
- WishlistService: create(NewWishlistInFeature,userId), update(id,NewWishlistInFeature,callerId)->Boolean?, delete(id,callerId)->Boolean?; null=not_found, false=unauthorized, true=success
- client: sends NewWishlistInFeature (no userId); server extracts userId from auth

**No changes made to source code this session.**

---

### 2026-05-25 — Session 3 (continued): wishlist ownership enforcement

**Prompt summary:** Enforce caller ownership on wishlist mutations. Add `NewWishlistInFeature` (no UserId). Server extracts userId from auth call. Client must not send userId.

**Actions:**
- action=update; target=features/wishlist/common/src/commonMain/kotlin/models/Wishlist.kt; adds=NewWishlistInFeature(title: String) — client-facing create/update body model
- action=update; target=features/wishlist/common/src/commonMain/kotlin/WishlistFeature.kt; changes=[create(NewWishlistInFeature), update(WishlistId,NewWishlistInFeature), delete(WishlistId)]; removes=NewWishlist from interface
- action=update; target=features/wishlist/server/src/commonMain/kotlin/services/WishlistService.kt; changes=[drops WishlistFeature impl; create(NewWishlistInFeature,userId), update(id,NewWishlistInFeature,callerId)->Boolean?, delete(id,callerId)->Boolean?]; semantics=[null=not found, false=unauthorized, true=success]
- action=update; target=features/wishlist/server/src/commonMain/kotlin/configurators/WishlistRoutingsConfigurator.kt; changes=[takes WishlistService; uses getCallerUserIdOrAnswerUnauthorized; 403 on false/404 on null]
- action=update; target=features/wishlist/server/src/commonMain/kotlin/Plugin.kt; changes=[removes single<WishlistFeature>; WishlistService registered plain]
- action=update; target=features/wishlist/server/build.gradle; adds=api project(":wishlist.features.auth.server")
- action=update; target=features/wishlist/client/src/commonMain/kotlin/KtorWishlistFeature.kt; changes=[create+update send NewWishlistInFeature body (no userId)]

**Route behavior:**
- POST /wishlist/create: body=NewWishlistInFeature; userId from bearer; 200=created/500=failure
- PUT /wishlist/update/{id}: body=NewWishlistInFeature; 200=ok/403=not owner/404=missing
- DELETE /wishlist/delete/{id}: no body; 200=ok/403=not owner/404=missing

---

### 2026-05-25 — Session 1: Initial onboarding

**Prompt:** `USE @AGENTS.md`

**Actions:**
- action=read; target=AGENTS.md; result=AML-HIP protocol loaded
- action=read; target=agents/ARCHITECTURE.md; result=full architecture doc loaded
- action=create; target=agents/local.HISTORY.md; reason=mandatory per AGENTS.md

**Project state at session start:**
- branch=master; commits=1 (Initial commit); status=clean
- features present: sample, users, auth, ui/auth, ui/serverAddress, ui/sample
- client targets: JS, JVM, Android
- server: Ktor+Netty, Exposed+PostgreSQL, plugin-based startup

**Key architecture facts loaded:**
- root_package=dev.inmo.wishlist
- build_system=Gradle Groovy DSL + TOML version catalog
- DI=Koin; serialization=kotlinx.serialization.json; navigation=dev.inmo:navigation.mvvm
- feature scaffold: run `./generate_feature.sh` (full-stack) or `./generate_scenario.sh` (UI-only)
- plugin registration: server via sample.config.json "plugins" array; client via Main.kt/MainActivity.kt hardcoded list
- CRUD pattern: ExposedRepo → CacheRepo → singleWithBinds (Read+Write+Full interfaces)
- auth pattern: BCrypt passwords, UUID tokens, bearer Ktor plugin, AuthCredentialsStorage per-platform
- UI MVVM: Model (data/IO) + ViewInteractor (app-level behavior) + ViewModel (state) + View (dumb Compose)
- interactor impls live in client/ClientPlugin.kt, NOT in feature plugin
- HttpClientConfigurator pattern: singleWithRandomQualifier, per-request URL override via onRequest suspend

**No changes made to source code this session.**

---

### 2026-05-25 — Session 1 (continued): wishlist feature + Amount.kt

**Prompt summary:** Add Amount.kt to common.common models; create full wishlist feature (Wishlist + WishlistItem CRUD, server + client + exposed + cache repos).

**Actions:**
- action=run; target=generate_feature.sh; params={module_path=wishlist}; result=scaffold created
- action=create; target=features/common/common/src/commonMain/kotlin/models/Amount.kt; source=ResourcesCounter Amount.kt; package=dev.inmo.wishlist.features.common.common.models
- action=create; target=features/wishlist/common/src/commonMain/kotlin/models/Wishlist.kt; entities=[WishlistId, Wishlist, NewWishlist, RegisteredWishlist]
- action=create; target=features/wishlist/common/src/commonMain/kotlin/models/WishlistItem.kt; entities=[WishlistItemId, WishlistItem, NewWishlistItem, RegisteredWishlistItem]; note=approximatePrice=Amount?, priceUnits=String, links=List<String>, description=String
- action=create; target=features/wishlist/common/src/commonMain/kotlin/WishlistFeature.kt; methods=[getByUserId, create, update, delete]
- action=create; target=features/wishlist/common/src/commonMain/kotlin/WishlistItemFeature.kt; methods=[getByWishlistId, create, update, delete]
- action=create; target=features/wishlist/common/src/commonMain/kotlin/repo/[ReadWishlistRepo, WriteWishlistRepo, WishlistRepo, CacheWishlistRepo]
- action=create; target=features/wishlist/common/src/commonMain/kotlin/repo/[ReadWishlistItemRepo, WriteWishlistItemRepo, WishlistItemRepo, CacheWishlistItemRepo]
- action=create; target=features/wishlist/common/src/jvmMain/kotlin/repo/ExposedWishlistRepo.kt; table=wishlists
- action=create; target=features/wishlist/common/src/jvmMain/kotlin/repo/ExposedWishlistItemRepo.kt; table=wishlist_items; note=Amount stored as (approx_price_int BIGINT NULL, approx_price_dec BIGINT NULL); links stored as JSON text
- action=update; target=features/wishlist/common/src/jvmMain/kotlin/JVMPlugin.kt; adds=[ExposedWishlistRepo, CacheWishlistRepo, ExposedWishlistItemRepo, CacheWishlistItemRepo]
- action=update; target=features/wishlist/common/build.gradle; adds=api project(":wishlist.features.users.common")
- action=create; target=features/wishlist/server/src/commonMain/kotlin/services/[WishlistService, WishlistItemService]
- action=create; target=features/wishlist/server/src/commonMain/kotlin/configurators/[WishlistRoutingsConfigurator, WishlistItemRoutingsConfigurator]
- action=update; target=features/wishlist/server/src/commonMain/kotlin/Plugin.kt; registers=[WishlistService, WishlistFeature, WishlistItemService, WishlistItemFeature, routing configurators]
- action=create; target=features/wishlist/client/src/commonMain/kotlin/[KtorWishlistFeature, KtorWishlistItemFeature]
- action=update; target=features/wishlist/client/src/commonMain/kotlin/Plugin.kt; registers=[KtorWishlistFeature as WishlistFeature, KtorWishlistItemFeature as WishlistItemFeature]
- action=update; target=settings.gradle; adds=[:features:wishlist:common, :features:wishlist:server, :features:wishlist:client]
- action=update; target=server/build.gradle; adds=api project(":wishlist.features.wishlist.server")
- action=update; target=client/build.gradle; adds=api project(":wishlist.features.wishlist.client")
- action=update; target=server/sample.config.json; adds="dev.inmo.wishlist.features.wishlist.server.JVMPlugin"
- action=update; target=client/src/jsMain/kotlin/Main.kt; adds=wishlist.client.JSPlugin
- action=update; target=client/src/jvmMain/kotlin/Main.kt; adds=wishlist.client.JVMPlugin
- action=update; target=client/android/src/main/kotlin/MainActivity.kt; adds=wishlist.client.AndroidPlugin

**Route map:**
- GET /wishlist/getByUserId/{userId} → List<RegisteredWishlist>
- POST /wishlist/create (body: NewWishlist) → RegisteredWishlist?
- PUT /wishlist/update/{id} (body: NewWishlist) → 200/404
- DELETE /wishlist/delete/{id} → 200
- GET /wishlistItem/getByWishlistId/{wishlistId} → List<RegisteredWishlistItem>
- POST /wishlistItem/create (body: NewWishlistItem) → RegisteredWishlistItem?
- PUT /wishlistItem/update/{id} (body: NewWishlistItem) → 200/404
- DELETE /wishlistItem/delete/{id} → 200

**All routes inside authenticate {} block.**

---

### 2026-05-25 — Session 2 (continued): KDocs + CODING.md

**Prompt summary:** Add KDocs to all created .kt files; add KDoc rule to agents/CODING.md; fill CODING.md with verbatim coding rules from ARCHITECTURE.md.

**Actions:**
- action=create; target=agents/CODING.md; content=[KDoc requirement rule + verbatim coding sections from ARCHITECTURE.md]
- action=update; target=features/common/common/src/commonMain/kotlin/models/Amount.kt; adds=KDocs (class, all properties, all constructors, all methods, companion)
- action=update; target=features/wishlist/common/src/commonMain/kotlin/models/Wishlist.kt; adds=KDocs (WishlistId, Wishlist, NewWishlist, RegisteredWishlist)
- action=update; target=features/wishlist/common/src/commonMain/kotlin/models/WishlistItem.kt; adds=KDocs (WishlistItemId, WishlistItem, NewWishlistItem, RegisteredWishlistItem)
- action=update; target=features/wishlist/common/src/commonMain/kotlin/WishlistFeature.kt; adds=KDocs (interface + all methods with @param/@return)
- action=update; target=features/wishlist/common/src/commonMain/kotlin/WishlistItemFeature.kt; adds=KDocs (interface + all methods with @param/@return)
- action=update; target=features/wishlist/common/src/commonMain/kotlin/Constants.kt; adds=KDocs (object + all constants)
- action=update; target=features/wishlist/common/src/commonMain/kotlin/repo/ReadWishlistRepo.kt; adds=KDocs
- action=update; target=features/wishlist/common/src/commonMain/kotlin/repo/WriteWishlistRepo.kt; adds=KDocs
- action=update; target=features/wishlist/common/src/commonMain/kotlin/repo/WishlistRepo.kt; adds=KDocs
- action=update; target=features/wishlist/common/src/commonMain/kotlin/repo/CacheWishlistRepo.kt; adds=KDocs (class with @param, getByUserId delegation note)
- action=update; target=features/wishlist/common/src/commonMain/kotlin/repo/ReadWishlistItemRepo.kt; adds=KDocs
- action=update; target=features/wishlist/common/src/commonMain/kotlin/repo/WriteWishlistItemRepo.kt; adds=KDocs
- action=update; target=features/wishlist/common/src/commonMain/kotlin/repo/WishlistItemRepo.kt; adds=KDocs
- action=update; target=features/wishlist/common/src/commonMain/kotlin/repo/CacheWishlistItemRepo.kt; adds=KDocs (class with @param, getByWishlistId delegation note)
- action=update; target=features/wishlist/common/src/jvmMain/kotlin/repo/ExposedWishlistRepo.kt; adds=KDocs (class with schema doc, getByUserId)
- action=update; target=features/wishlist/common/src/jvmMain/kotlin/repo/ExposedWishlistItemRepo.kt; adds=KDocs (class with full schema + Amount/links encoding notes, amountOrNull, getByWishlistId)
- action=update; target=features/wishlist/common/src/jvmMain/kotlin/JVMPlugin.kt; adds=KDocs
- action=update; target=features/wishlist/server/src/commonMain/kotlin/services/WishlistService.kt; adds=KDocs (class + all methods)
- action=update; target=features/wishlist/server/src/commonMain/kotlin/services/WishlistItemService.kt; adds=KDocs (class + all methods)
- action=update; target=features/wishlist/server/src/commonMain/kotlin/configurators/WishlistRoutingsConfigurator.kt; adds=KDocs (class with route table, @param)
- action=update; target=features/wishlist/server/src/commonMain/kotlin/configurators/WishlistItemRoutingsConfigurator.kt; adds=KDocs (class with route table, @param)
- action=update; target=features/wishlist/server/src/commonMain/kotlin/Plugin.kt; adds=KDocs
- action=update; target=features/wishlist/server/src/jvmMain/kotlin/JVMPlugin.kt; adds=KDocs
- action=update; target=features/wishlist/client/src/commonMain/kotlin/KtorWishlistFeature.kt; adds=KDocs (class with auth note, create/update/delete)
- action=update; target=features/wishlist/client/src/commonMain/kotlin/KtorWishlistItemFeature.kt; adds=KDocs (class with auth note, create/update/delete)
- action=update; target=features/wishlist/client/src/commonMain/kotlin/Plugin.kt; adds=KDocs
