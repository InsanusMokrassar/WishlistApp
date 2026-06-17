# STEP_0 — Coding (Calm Studio web shell, phase 2/5)

ENTITY:
entity_id=calm-studio-phase-2; type=ui_redesign_web; state=implemented_and_compiled

CONTEXT:
* task_id=c1d2e3f4-a5b6-47c8-9d0e-1f2a3b4c5d6e; agent_id=root; role=CODING; branch=redesign/calm-studio
* scope=WEB client only (jsMain); constraint=do_not_touch Android/Desktop views
* reference=.claude/skills/wishlistapp-design/ui_kits/calm-studio (components.jsx Sidebar/TopBar)

ACTION (files):
1. NEW feature features/ui/sidebar (multiplatform; commonMain + jsMain only)
   * commonMain: SidebarViewConfig, SidebarSection(enum), SidebarModel, SidebarViewInteractor,
     SidebarViewModel, SidebarStrings(EN+RU), Plugin (serializer + VM factory + SidebarModel single)
   * jsMain: SidebarView (.sidebar shell), LucideIcons+LucideIcon (raw-SVG via Compose-HTML ref), JSPlugin
   * build.gradle deps: common.client, ui.wishlist, ui.users, ui.booking, ui.auth
   * README.md with empty Operator Notes + required structure
2. client/ClientPlugin.kt (commonMain): mainScaffoldConfig → overridable mainScaffoldConfigProvider
   (default UNCHANGED = TopBar + UsersList, no left → Android/Desktop identical); added
   single<SidebarViewInteractor> driving MAIN chain (findInSubTree(MainNavigationChainId) + pushOrBackUntil)
3. client/ClientJSPlugin.kt (jsMain): override provider → ScaffoldViewConfig(top=TopBar, left=Sidebar,
   main=WishlistsList) ⇒ web landing = My Lists + persistent sidebar
4. client/Main.kt (jsMain): + ui.sidebar.JSPlugin
5. settings.gradle + client/build.gradle: + :features:ui:sidebar
6. features/ui/scaffold jsMain ScaffoldView: layout → Calm Studio shell .app>(sidebar,.main>(.topbar,.content));
   deleted ScaffoldViewStylesheet (shell CSS now global calm-studio.css); README updated
7. features/ui/topBar: VM + searchQueryState/onSearchQueryChanged (additive, JVM/Android factory unchanged);
   jsMain TopBarView → .topbar(search field + auth action) + .crumb breadcrumb strip under bar;
   TopBarStrings + searchPlaceholder; README updated
8. client/.../css/calm-studio.css: + .crumbbar strip + .topbar .btn-outline-light legibility shim

NAV MAP (sidebar→MAIN chain):
* My Lists→WishlistsListViewConfig(); Discover→UsersListViewConfig(); Reserved→MyPresentsBooksViewConfig();
  Settings→UserEditViewConfig(me); pinned list→WishlistViewConfig(id); New list→WishlistEditViewConfig(null);
  profile row→UserViewConfig(me)

REASON/DECISIONS:
* condition=scaffold left/main configs are commonMain (shared) → requirement=web-only sidebar without
  altering Android/Desktop ⇒ overridable provider set in ClientJSPlugin (jsMain) only
* condition=Compose-HTML has no SVG DOM builder → Lucide glyphs injected via attrs ref{ innerHTML }
* condition=no JS settings screen exists (ui/serverUrl has no jsMain view) → Settings→UserEditViewConfig(me)
* deviation=JS Stylesheet Rule (per-view StyleSheet object): shell layout uses GLOBAL calm-studio.css
  classes per phase-1 precedent + explicit task instruction ("match reference class names"); justified+documented
* deviation=breadcrumb relocated topbar→content: rendered as .crumb in .crumbbar strip directly under
  .topbar (feeds from existing TopBarTitleProvider; no scaffold↔topBar coupling)
* note=embedded AuthView keeps Bootstrap styling (later phase); CSS shim keeps it legible on light topbar

VERIFICATION:
* check=./gradlew :wishlist.client:compileKotlinJs ⇒ BUILD SUCCESSFUL
* check=./gradlew :wishlist.client:compileKotlinJvm ⇒ BUILD SUCCESSFUL (Android/Desktop commonMain+jvm intact;
  2 pre-existing lib @Warning notes only, none from new code)
* check=ast-index rebuild ⇒ 602 files / 40 modules (sidebar indexed)
* check=Operator Notes sections ⇒ untouched (new sidebar README note empty; scaffold/topBar notes unchanged)

EXPECTED_RESULT:
* web: persistent left sidebar (sections + live reserved count + pinned lists + New list + profile/login),
  top bar (global search + auth), breadcrumb depth in content, default landing = My Lists
* Android/Desktop: unchanged (default provider, no sidebar, users-list landing)

REPETITION_OF_RESULT:
* entity_id=calm-studio-phase-2; state=implemented_and_compiled; new_module=features/ui/sidebar;
  build=SUCCESSFUL(js+jvm); ast-index=rebuilt; stored_in=STEP_0.md; status=available
