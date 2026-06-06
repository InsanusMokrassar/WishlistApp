# STEP_3 — Coding B (T4,T5)

task_id=pr31-review-fixes; agent_id=coding-B; protocol=AML-HIP; branch=issue/29-book-functionality; pr=31
memory_ref=[STEP_0.md, STEP_1.md, STEP_2.md]; scope=T4+T5; constraints=[no-commit/push/stash/revert, preserve when-restructure (untouched by B), no Operator Notes edits, KDoc mandatory]

---

## STATE ON ARRIVAL (pre-existing from agent-A, STEP_2)

entity=commonMain+platform files; state=T1+T2+T3=COMPLETE-ON-ARRIVAL
WishlistsModel.isOwner() present; all 4 ownership sites routed; sort clamp + sortSelectorVisibleState in both VMs; WishlistSelectorsRow showSortSelector param; CreateWishlistButton x3 platforms; 6 call-site replacements; build=SUCCESS(STEP_2)

AGENT_B scope=T4+T5; files (pre-edit):
- WishlistAdditionalConfigsProvider.kt: val chainId: NavigationChainId (non-null abstract, T4 target)
- BookingConfigsProvider.kt: override val chainId: NavigationChainId = ... (T4 type widen target)
- WishlistItemAdditionalConfigView.kt: ABSENT (T5 NEW)
- jsMain/WishlistItemView.kt: InjectNavigationChain+InjectNavigationNode per provider (T5 edit target)
- jvmMain/WishlistItemView.kt: same (T5 edit target)
- androidMain/WishlistItemView.kt: same (T5 edit target)

---

## T4 — nullable chainId

### File edited: commonMain/kotlin/ui/WishlistAdditionalConfigsProvider.kt
action=EDIT; change=[
  val chainId: NavigationChainId → val chainId: NavigationChainId? get() = null (interface default, operator decision binding),
  interface KDoc: rewritten to document inline-drawing as default + non-null chainId search-push-fallback semantics + reference to WishlistItemAdditionalConfigView,
  chainId property KDoc: rewritten to describe null=anonymous-inline-chain, non-null=search-root-then-push-else-inline, isolation requirement
]
verification: check=property type; expected=NavigationChainId?; actual=NavigationChainId? PASS

### File edited: commonMain/kotlin/ui/BookingConfigsProvider.kt
action=EDIT; change=[
  override val chainId: NavigationChainId = ... → override val chainId: NavigationChainId? = NavigationChainId("wishlistItemAdditionalConfig_booking") (declared type widened to nullable, value UNCHANGED),
  chainId property KDoc: replaced with binding text (non-null stable id; external host may pre-create chain with id; otherwise drawn inline)
]
verification: check=value unchanged; expected=NavigationChainId("wishlistItemAdditionalConfig_booking"); actual=SAME PASS

---

## T5 — search-root-then-push-else-inline

### File created: commonMain/kotlin/ui/WishlistItemAdditionalConfigView.kt
action=NEW; package=dev.inmo.wishlist.features.ui.wishlist.ui
composable=WishlistItemAdditionalConfigView(provider, item, viewNode); helper=isInSubTreeOf(node)

imports=[
  androidx.compose.runtime.{Composable,DisposableEffect,LaunchedEffect,getValue,mutableStateOf,remember,setValue},
  dev.inmo.navigation.compose.{InjectNavigationChain,InjectNavigationNode},
  dev.inmo.navigation.core.{NavigationChain,NavigationNode},
  dev.inmo.navigation.core.extensions.{findInSubTree,rootChain},
  dev.inmo.wishlist.features.common.client.models.ViewConfig,
  dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
]

logic:
  chainId==null → InjectNavigationChain<ViewConfig> { InjectNavigationNode(config) }
  chainId!=null:
    searched: Boolean by remember(chainId) mutableStateOf(false)
    externalChain: NavigationChain<ViewConfig>? by remember(chainId) mutableStateOf(null)
    LaunchedEffect(chainId):
      candidate = viewNode.chain.rootChain().findInSubTree(chainId)
      loop: while candidate!=null && candidate.isInSubTreeOf(viewNode):
        dropItself()=true → re-search (retry; bounded by finite tree)
        dropItself()=false → candidate=null (fall back to inline; removal refused)
      externalChain=candidate; searched=true
    when { !searched → Unit; target!=null → DisposableEffect(target,config){push+onDispose-drop}; else → InjectNavigationChain(id=chainId){InjectNavigationNode} }

safeguards implemented=[ R1-duplicate-push-structural-equality, R2-stale-own-chain-retry-loop, R3-host-contract-documented-in-KDoc, single-frame-blank-while-searching ]
KDoc: full behavior contract (R1/R2/R3) + @param tags + isInSubTreeOf helper KDoc

### File edited: jsMain/kotlin/ui/WishlistItemView.kt
action=EDIT; changes=[
  DELETE imports: InjectNavigationChain, InjectNavigationNode (both freed; verify=0 remaining usages PASS),
  comment lines 113-116: updated to search-push-fallback text,
  forEach body: InjectNavigationChain{InjectNavigationNode} → WishlistItemAdditionalConfigView(provider, it, this@WishlistItemView),
  wrapper Div({ classes("d-flex","flex-wrap","gap-2","mb-3") }): KEPT,
  isNotEmpty() guard: KEPT
]

### File edited: jvmMain/kotlin/ui/WishlistItemView.kt
action=EDIT; changes=[
  DELETE imports: InjectNavigationChain, InjectNavigationNode (freed PASS),
  comment updated to search-push-fallback text,
  forEach body: WishlistItemAdditionalConfigView(provider, it, this@WishlistItemView)
]

### File edited: androidMain/kotlin/ui/WishlistItemView.kt
action=EDIT; changes=[
  DELETE imports: InjectNavigationChain, InjectNavigationNode (freed PASS),
  comment updated to search-push-fallback text,
  forEach body: WishlistItemAdditionalConfigView(provider, it, this@WishlistItemView)
]

TYPE CHECK: this@WishlistItemView : ComposeView<WishlistItemViewConfig,ViewConfig,VM> <: ComposeNode <: NavigationNode<WishlistItemViewConfig,ViewConfig> → matches NavigationNode<*,ViewConfig>; chain property public on ComposeNode → PASS; no new import at call sites (same package) → PASS

---

## VERIFICATION CHECKS

check=InjectNavigationChain import absent from 3 WishlistItemView files; expected=absent; actual=ABSENT PASS
check=InjectNavigationNode import absent from 3 WishlistItemView files; expected=absent; actual=ABSENT PASS
check=chainId type in WishlistAdditionalConfigsProvider; expected=NavigationChainId?; actual=NavigationChainId? PASS
check=chainId value in BookingConfigsProvider unchanged; expected=NavigationChainId("wishlistItemAdditionalConfig_booking"); actual=SAME PASS
check=KDoc present on all changed/new declarations; expected=true; actual=PASS (provider interface+property, BookingConfigsProvider property, WishlistItemAdditionalConfigView composable+helper)

---

## BUILD RESULT

command=JAVA_HOME=/home/aleksey/.sdkman/candidates/java/current ./gradlew :wishlist.features.ui.wishlist:build
result=BUILD SUCCESSFUL in 51s; 1257 actionable tasks: 75 executed, 1182 up-to-date
errors=0; deprecation warnings=pre-existing (Gradle 9/10 compat), unrelated

---

## AST-INDEX UPDATE

command=export PATH="/home/linuxbrew/.linuxbrew/bin:$PATH" && ast-index update
result=Updated 6 files (6 changed, 0 deleted); exit=0

---

## DEVIATIONS

deviation=NONE; all STEP_1 AGENT_B decisions honored exactly; no files outside AGENT_B scope touched; STEP_2 agent-A work preserved intact; when-restructure in jsMain/UserWishlistsView.kt untouched

---

## FILES CHANGED (summary)

| file | action | T |
|---|---|---|
| commonMain/kotlin/ui/WishlistAdditionalConfigsProvider.kt | EDIT: chainId type→NavigationChainId?, KDocs rewritten | T4 |
| commonMain/kotlin/ui/BookingConfigsProvider.kt | EDIT: chainId declared type→NavigationChainId?, KDoc replaced | T4 |
| commonMain/kotlin/ui/WishlistItemAdditionalConfigView.kt | NEW: shared @Composable + isInSubTreeOf helper, full KDoc | T5 |
| jsMain/kotlin/ui/WishlistItemView.kt | EDIT: remove Inject imports, update comment, use WishlistItemAdditionalConfigView | T5 |
| jvmMain/kotlin/ui/WishlistItemView.kt | EDIT: same | T5 |
| androidMain/kotlin/ui/WishlistItemView.kt | EDIT: same | T5 |

---

## REPETITION OF RESULT

entity_id=STEP_3; stored_in=agents/task/undefined/STEP_3.md; status=available
build=SUCCESS; T4=COMPLETE; T5=COMPLETE; deviations=0
remaining=README.md update (haiku agent, NOT `## Operator Notes`); shared post-stage per STEP_1
