# STEP_1 — Architecturing

CONTEXT:
* task_id=pr31-review-fixes; agent_id=architecture; protocol=AML-HIP; branch=issue/29-book-functionality; pr=31
* memory_ref=[STEP_0.md]; constraints=[no-commit, no-push, preserve jsMain/ui/UserWishlistsView.kt when-restructure, no edits beyond STEP_1.md by architecture role, KDoc mandatory per agents/CODING.md]
* base=STEP_0 decisions D1..D9 honored; deviations=0; refinements=2 (REF1=bounded stale-chain retry loop replaces single-shot exclusion; REF2=isInSubTreeOf helper extraction) — both within D6 intent, recorded in T5 section.

ENTITY:
entity_id=STEP_1; type=architecture-spec; state=COMPLETE
module=features/ui/wishlist; gradle=:wishlist.features.ui.wishlist; package=dev.inmo.wishlist.features.ui.wishlist[.ui]

---

## AGENT FILE OWNERSHIP (disjointness)

AGENT_A scope=T1+T2+T3; files=[
  commonMain/kotlin/ui/WishlistsModel.kt (edit),
  commonMain/kotlin/Plugin.kt (edit),
  commonMain/kotlin/ui/UserWishlistsViewModel.kt (edit),
  commonMain/kotlin/ui/WishlistsListViewModel.kt (edit),
  commonMain/kotlin/ui/WishlistViewModel.kt (edit),
  commonMain/kotlin/ui/WishlistItemViewModel.kt (edit),
  jsMain/kotlin/ui/WishlistSelectorsRow.kt (edit),
  jsMain/kotlin/ui/UserWishlistsView.kt (edit),
  jsMain/kotlin/ui/WishlistView.kt (edit),
  jsMain/kotlin/ui/WishlistsListView.kt (edit),
  jsMain/kotlin/ui/CreateWishlistButton.kt (NEW),
  jvmMain/kotlin/ui/UserWishlistsView.kt (edit),
  jvmMain/kotlin/ui/WishlistView.kt (edit),
  jvmMain/kotlin/ui/WishlistsListView.kt (edit),
  jvmMain/kotlin/ui/CreateWishlistButton.kt (NEW),
  androidMain/kotlin/ui/UserWishlistsView.kt (edit),
  androidMain/kotlin/ui/WishlistView.kt (edit),
  androidMain/kotlin/ui/WishlistsListView.kt (edit),
  androidMain/kotlin/ui/CreateWishlistButton.kt (NEW)
]

AGENT_B scope=T4+T5; files=[
  commonMain/kotlin/ui/WishlistAdditionalConfigsProvider.kt (edit),
  commonMain/kotlin/ui/BookingConfigsProvider.kt (edit),
  commonMain/kotlin/ui/WishlistItemAdditionalConfigView.kt (NEW),
  jsMain/kotlin/ui/WishlistItemView.kt (edit),
  jvmMain/kotlin/ui/WishlistItemView.kt (edit),
  androidMain/kotlin/ui/WishlistItemView.kt (edit)
]

VERIFICATION: check=set-intersection(AGENT_A.files, AGENT_B.files); expected=∅; actual=∅ → DISJOINT=true.
NOTE: Plugin.kt owned by AGENT_A only (isOwner impl); provider Koin registration in Plugin.kt UNCHANGED → AGENT_B never touches Plugin.kt. WishlistItemViewModel.kt owned by AGENT_A only (T2 ownership refactor); WishlistItemViewModel.additionalConfigsProviders property UNCHANGED → AGENT_B view edits compile against unchanged VM API.
SHARED POST-STAGE (after A+B both done, sequential): README.md update (haiku agent per agents/ALL.md item 4; NOT `## Operator Notes`); `export PATH="/home/linuxbrew/.linuxbrew/bin:$PATH" && ast-index update`; `cd /home/aleksey/projects/own/WishlistApp && ./gradlew :wishlist.features.ui.wishlist:build`. Build run ONCE after both agents (concurrent gradle on same module forbidden).

---

## T2 — WishlistsModel.isOwner (AGENT_A; ordering=FIRST, T1 VMs build on top)

### File: commonMain/kotlin/ui/WishlistsModel.kt
ACTION: ADD method to interface (place after getCurrentUserId, line 111):
```kotlin
    /**
     * Whether the authenticated caller owns the context identified by [userId].
     *
     * Single ownership predicate for every wishlist UI screen: returns `true` iff a caller is
     * authenticated AND ([userId] is `null` OR [userId] equals the caller's id). A `null` [userId]
     * means "the caller's own context" (e.g. the wishlists list opened without a target user) and
     * is owned by any authenticated caller.
     *
     * Callers holding a nullable wishlist MUST treat a `null` wishlist as not-owned BEFORE calling,
     * e.g. `wishlist != null && model.isOwner(wishlist.userId)` — this method cannot distinguish
     * "no wishlist" from "own context".
     *
     * @param userId Owner id of the checked context, or `null` for the caller's own context.
     * @return `true` when the authenticated caller owns the context; `false` for anonymous callers.
     */
    suspend fun isOwner(userId: UserId?): Boolean
```
IMPORTS: none added (UserId already imported line 5).

### File: commonMain/kotlin/Plugin.kt
ACTION: ADD override inside anonymous `object : WishlistsModel` (place after getCurrentUserId override, line 143):
```kotlin
                /**
                 * Single ownership predicate: authenticated caller + (`null` target = own context,
                 * non-null target must equal the caller's id). See [WishlistsModel.isOwner].
                 */
                override suspend fun isOwner(userId: UserId?): Boolean {
                    val currentUserId = getCurrentUserId()
                    return currentUserId != null && (userId == null || userId == currentUserId)
                }
```
IMPORTS: none added. NOTE: object-member KDoc per agents/CODING.md; interface KDoc carries full contract.

### File: commonMain/kotlin/ui/UserWishlistsViewModel.kt — T2 part
ACTION: line 220 `_isOwnerState.value = model.getCurrentUserId() == node.config.userId` → `_isOwnerState.value = model.isOwner(node.config.userId)`. node.config.userId type=UserId non-null → valid arg.
KDOC: isOwnerState (line 201-205) append: derivation routes through [WishlistsModel.isOwner].

### File: commonMain/kotlin/ui/WishlistsListViewModel.kt
ACTION: line 95 `_isOwnerState.value = currentUserId != null && (targetUserId == null || targetUserId == currentUserId)` → `_isOwnerState.value = model.isOwner(targetUserId)`.
KEEP: `val currentUserId = model.getCurrentUserId()` line 86 — still feeds profileUserId line 92. COST: +1 getMe HTTP per load (R4 accepted, STEP_0; getMe short-circuits when storage.userAuthorised==false).
KDOC: isOwnerState (lines 67-73) append routing note [WishlistsModel.isOwner].

### File: commonMain/kotlin/ui/WishlistViewModel.kt — T2 part
ACTION:
1. DELETE line 55 `private val _currentUserIdState = MutableRedeliverStateFlow<UserId?>(null)`.
2. REPLACE lines 56-63 (combine-derived isOwnerState) with:
```kotlin
    private val _isOwnerState = MutableRedeliverStateFlow(false)

    /**
     * `true` when the authenticated caller is the wishlist owner. Assigned on every (re)load via
     * [WishlistsModel.isOwner]; a missing (`null`) wishlist counts as not-owned.
     */
    val isOwnerState = _isOwnerState.asStateFlow()
```
3. loadWishlist(): DELETE line 171 `_currentUserIdState.value = model.getCurrentUserId()`; AFTER line 173 `_wishlistState.value = loaded` ADD `_isOwnerState.value = loaded != null && model.isOwner(loaded.userId)`.
4. IMPORTS: DELETE `import dev.inmo.wishlist.features.users.common.models.UserId` (line 17; sole consumer was _currentUserIdState). KEEP combine/SharingStarted/stateIn/StateFlow (still used by costSortAvailableState/sortedItemsState + new T1 flows).
FATE _currentUserIdState: DELETED; sole consumer was isOwnerState combine (verified STEP_0 R9: set :171, read :61 only).

### File: commonMain/kotlin/ui/WishlistItemViewModel.kt
ACTION:
1. DELETE line 53 `private val _wishlistState = ...` + line 54 `private val _currentUserIdState = ...`.
2. REPLACE lines 56-62 (combine-derived isOwnerState) with:
```kotlin
    private val _isOwnerState = MutableRedeliverStateFlow(false)

    /**
     * `true` when the authenticated caller is the parent wishlist owner. Controls visibility of the
     * Edit button. Assigned on every (re)load via [WishlistsModel.isOwner]; a missing (`null`)
     * parent wishlist counts as not-owned.
     */
    val isOwnerState = _isOwnerState.asStateFlow()
```
3. init load block: REPLACE lines 91-92 (`_currentUserIdState.value = ...; _wishlistState.value = model.getWishlist(...)`) with:
```kotlin
                val wishlist = model.getWishlist(node.config.wishlistId)
                _isOwnerState.value = wishlist != null && model.isOwner(wishlist.userId)
```
4. IMPORTS: DELETE `kotlinx.coroutines.flow.combine`, `kotlinx.coroutines.flow.SharingStarted`, `kotlinx.coroutines.flow.stateIn`, `dev.inmo.wishlist.features.users.common.models.UserId`, `dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist` (each freed; verify zero remaining usage before delete). KEEP `StateFlow` (selectedCurrencyState explicit type), `asStateFlow`, `RegisteredWishlistItem`.
FATE _currentUserIdState + _wishlistState: DELETED; sole consumers were isOwnerState combine (verified STEP_0 R9: _wishlistState set :92 read :60; _currentUserIdState set :91 read :60). _wishlistState replaced by local `val wishlist` in load block.

---

## T1 — hide sort + force None when total items < 2 (AGENT_A; depends on T2 VM edits in same files)

RULE (operator, binding): total item count < 2 → sort selector hidden + WishlistSortMode.None forced. Scope per D1: BOTH screens (all-items + detail), ALL 3 platforms. Count semantics per D9: all-items = sum of section item counts; detail = items.size.

### File: commonMain/kotlin/ui/UserWishlistsViewModel.kt — T1 part
ACTION:
1. REPLACE line 130 `val sortModeState = _sortModeState.asStateFlow()` (KDoc lines 126-129 replaced too) with clamped derived flow:
```kotlin
    /**
     * Effective ordering applied by the views. Mirrors the user selection from [onSortModeSelected]
     * but is clamped to [WishlistSortMode.None] while fewer than two items exist across all loaded
     * sections — sorting fewer than two items is meaningless and a non-[WishlistSortMode.None] mode
     * would blank the grouped presentation (PR #31 T1). The raw selection is kept privately, so it
     * re-applies when the item count grows back to two or more.
     */
    val sortModeState: StateFlow<WishlistSortMode> =
        combine(_sortModeState, _sectionsState) { mode, sections ->
            if (sections.sumOf { it.items.size } < 2) WishlistSortMode.None else mode
        }.stateIn(scope, SharingStarted.Eagerly, WishlistSortMode.None)
```
2. ADD new flow directly after sortModeState:
```kotlin
    /**
     * `true` when the sort selector should be rendered: two or more items exist across all loaded
     * sections. Hidden otherwise — with fewer than two items every mode is equivalent to
     * [WishlistSortMode.None], so the selector (including a meaningless Cost option for an empty
     * item set) would only mislead (PR #31 T1).
     */
    val sortSelectorVisibleState: StateFlow<Boolean> =
        _sectionsState.map { sections -> sections.sumOf { it.items.size } >= 2 }
            .stateIn(scope, SharingStarted.Eagerly, false)
```
3. sortedItemsState (lines 166-194): REPLACE effectiveMode computation (lines 170-175) with:
```kotlin
            val effectiveMode = when {
                allItems.size < 2 -> WishlistSortMode.None
                mode == WishlistSortMode.Cost && !isCostSortAvailable(pricedUnits, enabled) -> WishlistSortMode.None
                else -> mode
            }
```
(`allItems` already computed line 168 before pricedUnits.) KDoc of sortedItemsState: append clamp sentence ("Also clamped to None while fewer than two items are loaded.").
4. IMPORT ADD: `import kotlinx.coroutines.flow.map`.
SAFETY: views branch grouped-vs-flat on public sortModeState → clamped public flow keeps `sortMode == None` branch + forced-empty sortedItemsState consistent from one source (fixes STEP_0 R7 stale-mode desync). onSortModeSelected unchanged (writes raw _sortModeState).
costSortAvailableState: UNCHANGED — Cost-option bug (isCostSortAvailable(emptyList(), enabled)==true) neutralized by selector being hidden entirely when <2 items.

### File: commonMain/kotlin/ui/WishlistViewModel.kt — T1 part
ACTION (mirrors UserWishlistsViewModel; source flow=_itemsState, count=items.size):
1. REPLACE line 76 `val sortModeState = _sortModeState.asStateFlow()` (+KDoc lines 72-75) with:
```kotlin
    /**
     * Effective ordering applied by the views. Mirrors the user selection from [onSortModeSelected]
     * but is clamped to [WishlistSortMode.None] while the wishlist holds fewer than two items —
     * sorting fewer than two items is meaningless (PR #31 T1). The raw selection is kept privately,
     * so it re-applies when the item count grows back to two or more.
     */
    val sortModeState: StateFlow<WishlistSortMode> =
        combine(_sortModeState, _itemsState) { mode, items ->
            if (items.size < 2) WishlistSortMode.None else mode
        }.stateIn(scope, SharingStarted.Eagerly, WishlistSortMode.None)
```
2. ADD after sortModeState:
```kotlin
    /**
     * `true` when the sort selector should be rendered: the wishlist holds two or more items.
     * Hidden otherwise — with fewer than two items every mode is equivalent to
     * [WishlistSortMode.None] (PR #31 T1).
     */
    val sortSelectorVisibleState: StateFlow<Boolean> =
        _itemsState.map { items -> items.size >= 2 }
            .stateIn(scope, SharingStarted.Eagerly, false)
```
3. sortedItemsState (lines 110-130): REPLACE effectiveMode computation (lines 113-118) with:
```kotlin
            val effectiveMode = when {
                items.size < 2 -> WishlistSortMode.None
                mode == WishlistSortMode.Cost && !isCostSortAvailable(pricedUnits, enabled) -> WishlistSortMode.None
                else -> mode
            }
```
KDoc append clamp sentence.
4. IMPORT ADD: `import kotlinx.coroutines.flow.map`.

### File: jsMain/kotlin/ui/WishlistSelectorsRow.kt (operator-mandated signature change)
ACTION: new exact signature (param `showSortSelector` inserted after `costSortAvailable`, NO default per D2 — forces explicit decision at every call site):
```kotlin
@Composable
fun WishlistSelectorsRow(
    sortMode: WishlistSortMode,
    onSortModeSelected: (WishlistSortMode) -> Unit,
    costSortAvailable: Boolean,
    showSortSelector: Boolean,
    isCurrenciesFeatureEnabled: Boolean,
    currencies: List<CurrencyInfo>,
    selectedCurrency: CurrencyCode?,
    onCurrencySelected: (CurrencyCode?) -> Unit,
    viewMode: WishlistViewMode,
    onViewModeSelected: (WishlistViewMode) -> Unit,
    noneLabel: StringResource = WishlistStrings.sortNone,
)
```
BODY: wrap existing WishlistSortSelector call (lines 49-54) in `if (showSortSelector) { ... }`; currency + view-mode selectors unchanged.
KDOC: add `@param showSortSelector Whether the sort selector is rendered; pass the screen's ViewModel `sortSelectorVisibleState` — hidden while fewer than two items are shown.` + extend summary sentence.

### T1 state propagation map (sortSelectorVisibleState → platforms)
relation: UserWishlistsViewModel.sortSelectorVisibleState → {jsMain UserWishlistsView (→ WishlistSelectorsRow.showSortSelector), jvmMain UserWishlistsView (if-wrap WishlistSortSelector lines 105-109), androidMain UserWishlistsView (if-wrap lines 108-112)}.
relation: WishlistViewModel.sortSelectorVisibleState → {jsMain WishlistView (→ WishlistSelectorsRow.showSortSelector), jvmMain WishlistView (if-wrap lines 92-97), androidMain WishlistView (if-wrap lines 94-99)}.
JVM/Android receive rule via ViewModel state (no shared row composable exists there — REPO FACT honored).

### View edits (T1 wiring; per-file exact)
1. jsMain/kotlin/ui/UserWishlistsView.kt: ADD `val sortSelectorVisible by viewModel.sortSelectorVisibleState.collectAsState()` to collect block (after line 60); WishlistSelectorsRow call (lines 92-102) ADD named arg `showSortSelector = sortSelectorVisible` (after `costSortAvailable = costSortAvailable`). PRESERVE uncommitted when-restructure (lines 84-162) — additive edits only.
2. jsMain/kotlin/ui/WishlistView.kt: ADD same collectAsState line; WishlistSelectorsRow call (lines 76-87) ADD `showSortSelector = sortSelectorVisible`.
3. jvmMain/kotlin/ui/UserWishlistsView.kt: ADD collect val; wrap WishlistSortSelector (lines 105-109) in `if (sortSelectorVisible) { ... }`.
4. androidMain/kotlin/ui/UserWishlistsView.kt: ADD collect val; wrap lines 108-112 in `if (sortSelectorVisible) { ... }`.
5. jvmMain/kotlin/ui/WishlistView.kt: ADD collect val; wrap lines 92-97 in `if (sortSelectorVisible) { ... }`.
6. androidMain/kotlin/ui/WishlistView.kt: ADD collect val; wrap lines 94-99 in `if (sortSelectorVisible) { ... }`.
BEHAVIOR RESULT (reviewer states fixed): all-empty wishlists → sections non-empty, totalItems=0 → selector hidden + clamped mode=None → grouped headers + emptyItems placeholders + Add Item buttons render; Cost option unreachable.

---

## T3 — CreateWishlistButton per platform (AGENT_A)

### File: jsMain/kotlin/ui/CreateWishlistButton.kt (NEW)
```kotlin
package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Text

/**
 * Owner-gated "New Wishlist" button shared by [WishlistsListView] and [UserWishlistsView].
 *
 * Renders nothing when [isOwner] is `false` — non-owners and anonymous viewers never see the
 * button. Bootstrap `btn btn-primary` markup, label = [WishlistStrings.createWishlistButton].
 *
 * @param isOwner Whether the authenticated caller owns the displayed list; gates rendering.
 * @param onClick Invoked when the button is pressed; callers delegate to the screen's
 *   `onCreateWishlist()`.
 */
@Composable
fun CreateWishlistButton(isOwner: Boolean, onClick: () -> Unit) {
    if (!isOwner) return
    Button({
        classes("btn", "btn-primary")
        onClick { onClick() }
    }) {
        Text(WishlistStrings.createWishlistButton.translation())
    }
}
```

### File: jvmMain/kotlin/ui/CreateWishlistButton.kt (NEW)
```kotlin
package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings

/** [same KDoc shape as jsMain, markup sentence = "Material `Button`"] */
@Composable
fun CreateWishlistButton(isOwner: Boolean, onClick: () -> Unit) {
    if (!isOwner) return
    Button(onClick = onClick) {
        Text(WishlistStrings.createWishlistButton.translation())
    }
}
```

### File: androidMain/kotlin/ui/CreateWishlistButton.kt (NEW)
```kotlin
package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings

/** [same KDoc shape, markup sentence = "Material3 `Button`"; note label resolved via LocalResources] */
@Composable
fun CreateWishlistButton(isOwner: Boolean, onClick: () -> Unit) {
    if (!isOwner) return
    Button(onClick = onClick) {
        Text(WishlistStrings.createWishlistButton.translation(LocalResources.current))
    }
}
```

### Call-site replacements (6; identical replacement text `CreateWishlistButton(isOwner) { viewModel.onCreateWishlist() }`)
| file | lines replaced |
|---|---|
| jsMain/kotlin/ui/UserWishlistsView.kt | 67-74 (`if (isOwner) { Button...createWishlistButton... }`) |
| jsMain/kotlin/ui/WishlistsListView.kt | 72-79 |
| jvmMain/kotlin/ui/UserWishlistsView.kt | 89-93 |
| jvmMain/kotlin/ui/WishlistsListView.kt | 85-89 |
| androidMain/kotlin/ui/UserWishlistsView.kt | 92-96 |
| androidMain/kotlin/ui/WishlistsListView.kt | 88-92 |

IMPORT CLEANUP at call sites: jvm+android WishlistsListView — `Button` import likely freed (other buttons are OutlinedButton); verify zero remaining `Button(` usage per file before deleting import. js views + jvm/android UserWishlistsView keep `Button` (profile/add-item/open buttons remain). Same composable name in same package per platform source set → call sites need NO new import.
VERIFICATION: check=`createWishlistButton.translation` occurrences outside CreateWishlistButton.kt; expected=0 per source set.

---

## T4 — nullable chainId (AGENT_B)

### File: commonMain/kotlin/ui/WishlistAdditionalConfigsProvider.kt
ACTION: line 27 `val chainId: NavigationChainId` → `val chainId: NavigationChainId? get() = null` (interface default per operator decision).
KDOC REPLACE (property, lines 21-26):
```kotlin
    /**
     * Optional stable id of the navigation chain this provider's inline view is drawn into.
     *
     * `null` (the default): the item screen always injects the provider's view inline, in a fresh
     * anonymous chain. Non-null: the item screen first searches the navigation tree from the root
     * chain for an existing externally hosted chain with this id — when found, the view is pushed
     * into that chain (drawn wherever its host draws it); when absent, the view is injected inline
     * under this id. Each provider must use a distinct id so its view stays isolated from other
     * providers' chains and from the item screen's own chain.
     */
```
INTERFACE KDOC (lines 7-19): update drawing-contract sentence: inline drawing is the fallback; non-null [chainId] enables reuse of an externally hosted chain (see [WishlistItemAdditionalConfigView]).

### File: commonMain/kotlin/ui/BookingConfigsProvider.kt
ACTION: line 17 → `override val chainId: NavigationChainId? = NavigationChainId("wishlistItemAdditionalConfig_booking")` (value unchanged, declared type widened to nullable).
KDOC (line 16): replace with: stable non-null chain id → an external host may pre-create a chain with the id to relocate the compact booking view; otherwise drawn inline on the item screen in an isolated chain.

---

## T5 — search-root-then-push-else-inline (AGENT_B)

### File: commonMain/kotlin/ui/WishlistItemAdditionalConfigView.kt (NEW; D5 single shared commonMain composable — compose.runtime + navigation.compose availability in commonMain verified STEP_0 §2.4)
EXACT imports:
```kotlin
package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.inmo.navigation.compose.InjectNavigationChain
import dev.inmo.navigation.compose.InjectNavigationNode
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.extensions.findInSubTree
import dev.inmo.navigation.core.extensions.rootChain
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
```
(API verified against /home/aleksey/projects/own/navigation sources: `rootChain()` Chain.kt:46; `findInSubTree(id: NavigationChainId): NavigationChain<Base>?` Chain.kt:91-95; `NavigationChain.push(config): NavigationNode<out Base, Base>?` non-suspend NavigationChain.kt:65; `drop(node)` NavigationChain.kt:85; `dropItself(): Boolean` NavigationChain.kt:173; `stackFlow: StateFlow<List<NavigationNode<out Base, Base>>>` NavigationChain.kt:57; `parentNode: NavigationNode<out Base, Base>?` ctor property; `NavigationNode.chain`/`NavigationNode.config` public NavigationNode.kt:21/23; `InjectNavigationChain(onDismiss=null, id=null, beforeNodes)` ComposeNavigationExtensions.kt:102; `InjectNavigationNode(config, onDismiss=null, additionalCodeInNodeContext=null)` :211.)

EXACT signatures + body skeleton:
```kotlin
/**
 * Draws one [WishlistAdditionalConfigsProvider] contribution for [item].
 *
 * Behavior contract (PR #31 T5, operator decision):
 * 1. [WishlistAdditionalConfigsProvider.chainId] == `null` → the provider's view is injected
 *    inline right here, in a fresh anonymous chain.
 * 2. chainId != `null` → a [LaunchedEffect] searches the navigation tree from the root chain for an
 *    existing chain with that id. Found external chain → the provider's config is pushed into that
 *    chain (the view renders wherever the host draws the chain); a host that never draws its chain
 *    keeps the view invisible — host contract, not guarded here. Not found → inline injection
 *    under [WishlistAdditionalConfigsProvider.chainId].
 *
 * Safeguards:
 * - Stale-self exclusion: a previous inline injection of this same screen leaves its chain in the
 *   navigation tree after pause/resume (the composition is disposed, the chain is not). A found
 *   chain lying inside [viewNode]'s own subtree is therefore dropped ([NavigationChain.dropItself])
 *   and the search retried, so the view never renders into an undrawn leftover chain.
 * - Duplicate-push guard: the config is pushed only when no node with a structurally equal config
 *   is already in the target stack; recompositions reuse the existing node.
 * - Cleanup: a node pushed into an external chain is dropped in the [DisposableEffect] dispose so
 *   leaving the item screen removes the pushed view.
 * - Nothing is rendered while the search is in flight (single frame) to avoid a transient inline
 *   chain that would immediately become stale.
 *
 * @param provider Provider whose compact view is drawn.
 * @param item Item currently displayed; forwarded to [WishlistAdditionalConfigsProvider.createConfig].
 * @param viewNode Navigation node of the item screen; anchor for root-chain search and for the
 *   own-subtree staleness test.
 */
@Composable
fun WishlistItemAdditionalConfigView(
    provider: WishlistAdditionalConfigsProvider,
    item: RegisteredWishlistItem,
    viewNode: NavigationNode<*, ViewConfig>,
) {
    val config = remember(provider, item) { provider.createConfig(item) }
    val chainId = provider.chainId
    if (chainId == null) {
        InjectNavigationChain<ViewConfig> {
            InjectNavigationNode(config)
        }
    } else {
        var searched by remember(chainId) { mutableStateOf(false) }
        var externalChain by remember(chainId) { mutableStateOf<NavigationChain<ViewConfig>?>(null) }
        LaunchedEffect(chainId) {
            var candidate = viewNode.chain.rootChain().findInSubTree(chainId)
            while (candidate != null && candidate.isInSubTreeOf(viewNode)) {
                candidate = if (candidate.dropItself()) {
                    viewNode.chain.rootChain().findInSubTree(chainId)
                } else {
                    null // removal refused — treat as not found, fall back to inline
                }
            }
            externalChain = candidate
            searched = true
        }
        val target = externalChain
        when {
            !searched -> Unit // search in flight, render nothing this frame
            target != null -> DisposableEffect(target, config) {
                val pushed = if (target.stackFlow.value.none { it.config == config }) {
                    target.push(config)
                } else {
                    null
                }
                onDispose { pushed ?.let { target.drop(it) } }
            }
            else -> InjectNavigationChain<ViewConfig>(id = chainId) {
                InjectNavigationNode(config)
            }
        }
    }
}

/**
 * Whether this chain lies inside the navigation subtree rooted at [node]: walks parent chains
 * upward and reports `true` when any of them is parented by [node] itself.
 *
 * @param node Subtree root candidate.
 * @return `true` when this chain is a (transitive) subchain of [node].
 */
private fun NavigationChain<ViewConfig>.isInSubTreeOf(node: NavigationNode<*, ViewConfig>): Boolean =
    generateSequence(this) { it.parentNode ?.chain }.any { it.parentNode === node }
```

SAFEGUARD RATIONALE (maps STEP_0 risks):
- R1 duplicate push → structural-equality stack check (`BookingViewConfig` = @Serializable data class → `==` structural) + DisposableEffect keyed (target, config) + drop-on-dispose. Recomposition with same target+config → effect not restarted → zero extra pushes. Item reload producing equal data-class value → remember(provider, item) keeps same config instance (remember keys compare by equality) → no re-push.
- R2 stale self chain after pause/resume → isInSubTreeOf ancestor walk (`===` node identity) + dropItself() cleanup + bounded retry (REF1: loop re-searches after each drop; terminates because each iteration removes one chain from finite tree or exits on dropItself()==false; handles stale-own + external coexistence which single-shot exclusion missed). DEVIATION-FROM-STEP_0=none-semantic (D6 said drop-then-fallback-inline; retry additionally finds a real external host when both exist — strictly closer to operator intent "if found - push there").
- R3 found external chain never drawn by host → accepted, documented in KDoc (host contract).
- LaunchedEffect keying=chainId: per composition lifecycle; pause/resume disposes composition → effect re-runs on resume → fresh search (correct: external host may have appeared/vanished). searched/externalChain remember(chainId)-keyed → reset with effect.
- Single-frame blank while `!searched` prevents transient inline injection flipping to push (would itself create a stale chain).

### Call-site edits (3 platform WishlistItemViews; README documents identical inline drawing on all platforms — consistency honored)
| file | lines | edit |
|---|---|---|
| jsMain/kotlin/ui/WishlistItemView.kt | 113-125 | comment 113-116 → updated text (below); KEEP wrapper `Div({ classes("d-flex","flex-wrap","gap-2","mb-3") })` + `isNotEmpty()` guard; inner forEach body 120-122 → `WishlistItemAdditionalConfigView(provider, it, this@WishlistItemView)`; DELETE imports `dev.inmo.navigation.compose.InjectNavigationChain` + `dev.inmo.navigation.compose.InjectNavigationNode` (lines 7-8; verify freed) |
| jvmMain/kotlin/ui/WishlistItemView.kt | 114-122 | comment → updated; forEach body 119-121 → `WishlistItemAdditionalConfigView(provider, it, this@WishlistItemView)`; DELETE both Inject imports (verify freed) |
| androidMain/kotlin/ui/WishlistItemView.kt | 119-127 | comment → updated; forEach body 124-126 → `WishlistItemAdditionalConfigView(provider, it, this@WishlistItemView)`; DELETE both Inject imports (verify freed) |

Updated comment text (all 3 sites):
```kotlin
// Each registered WishlistAdditionalConfigsProvider (e.g. booking) is drawn through the
// shared WishlistItemAdditionalConfigView: pushed into an existing externally hosted chain
// when the provider declares a chainId that is already present in the navigation tree,
// otherwise injected inline right here.
```
TYPE CHECK: `this@WishlistItemView` : ComposeView<WishlistItemViewConfig, ViewConfig, VM> <: ComposeNode <: NavigationNode<WishlistItemViewConfig, ViewConfig> — matches `NavigationNode<*, ViewConfig>`; `chain` property public on ComposeNode (verified). No new import at call sites (same package).

---

## KDOC OBLIGATION SUMMARY (agents/CODING.md compliance)

| file | obligations |
|---|---|
| WishlistsModel.kt | NEW isOwner full contract (semantics + null-wishlist caller rule) |
| Plugin.kt | NEW isOwner override KDoc (short, links interface) |
| UserWishlistsViewModel.kt | REWRITE sortModeState; NEW sortSelectorVisibleState; APPEND sortedItemsState clamp + isOwnerState routing |
| WishlistViewModel.kt | REWRITE sortModeState + isOwnerState; NEW sortSelectorVisibleState; APPEND sortedItemsState clamp |
| WishlistItemViewModel.kt | REWRITE isOwnerState; class KDoc line 29 sentence "Loads the item ... and the parent wishlist. Exposes [isOwnerState]..." still accurate — keep |
| WishlistsListViewModel.kt | APPEND isOwnerState routing note |
| WishlistSelectorsRow.kt | NEW @param showSortSelector + summary extension |
| CreateWishlistButton.kt ×3 | NEW full KDoc each (shape given above) |
| WishlistAdditionalConfigsProvider.kt | REWRITE chainId property + interface drawing-contract KDoc |
| BookingConfigsProvider.kt | REWRITE chainId KDoc |
| WishlistItemAdditionalConfigView.kt | NEW full KDoc (behavior contract R1/R2/R3) + isInSubTreeOf helper KDoc |
| WishlistItemView.kt ×3 | UPDATE inline comment (text given above) |

## README.md OBLIGATION (post-stage, haiku agent, NOT `## Operator Notes`)
- Screens table: UserWishlists + Wishlist Detail rows — sort selector hidden + None forced when total items < 2.
- Architecture Notes: (a) T1 clamp rule + sortSelectorVisibleState + WishlistSelectorsRow showSortSelector param; (b) WishlistsModel.isOwner single predicate + 4 routed sites + removed _currentUserIdState/_wishlistState combines; (c) CreateWishlistButton per-platform shared composable replacing 6 duplicates; (d) chainId nullable default-null + WishlistItemAdditionalConfigView search-push-fallback semantics + booking provider keeps non-null id; Item Read row drawing description update.

## VERIFICATION (coding agents + orchestrator)
* check=`./gradlew :wishlist.features.ui.wishlist:build`; expected=SUCCESS; run ONCE after A+B both complete.
* check=git diff jsMain/ui/UserWishlistsView.kt retains when-restructure; expected=true.
* check=occurrences `createWishlistButton.translation` outside CreateWishlistButton.kt; expected=0.
* check=occurrences `getCurrentUserId() ==` in ViewModels; expected=0 (all ownership via model.isOwner).
* check=imports InjectNavigationChain/InjectNavigationNode in 3 WishlistItemView files; expected=absent.
* check=`ast-index update` after source changes; expected=exit 0.
* manual T1: 2 wishlists 0 items → no sort selector + headers + emptyItems placeholders; 1 item → same; 2 items → selector visible.
* manual T5: open item → booking button inline; push edit → back → booking button visible (R2 retry guard); zero duplicate booking nodes; external host chain with id `wishlistItemAdditionalConfig_booking` present → booking view renders in host, none inline.

REPETITION OF RESULT:
* entity_id=STEP_1; stored_in=agents/task/undefined/STEP_1.md; status=available; next=coding agents A (T1+T2+T3) + B (T4+T5) execute per ownership map; file sets disjoint=true; build sequential after both.
