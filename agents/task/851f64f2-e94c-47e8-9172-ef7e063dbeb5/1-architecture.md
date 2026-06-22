Model: claude-sonnet-4-6
Execution time: 420
Tokens used: ~38000
Changed files: agents/task/851f64f2-e94c-47e8-9172-ef7e063dbeb5/1-architecture.md

Model choice: claude-sonnet-4-6 (Sonnet). ARCHITECTURE.md specifies no explicit model priority. Sonnet
is the current runtime and sufficient for code-reading + design output. No escalation needed — this is
a two-file change on a well-understood codebase with a concrete technical question to resolve.

---

# Architecture: Issue #54 — Clickable Breadcrumbs

## Critical Finding: Planning Step Race Condition Error

The planning step (0-planning.md) proposed a "batch-drop" approach:

```kotlin
val toRemove = stack.subList(targetIndex + 1, stack.size).reversed()
toRemove.forEach { chain.drop(it) }
```

**This is BROKEN due to a race condition in `NavigationChain.drop(node)`.**

Source: `/home/aleksey/projects/own/navigation/core/src/commonMain/kotlin/NavigationChain.kt`, lines 85–110.

The `drop(node)` function computes `newStack = stack.filterNot { it === node }` OUTSIDE the channel lambda,
then enqueues a lambda that sets `_stackFlow.value = newStack`. Because `newStack` is captured from
`stack` at call time (before any prior drop has been applied by the channel), a synchronous loop of
N drops all snapshot the SAME original stack. The channel then applies them in order, each one
overwriting `_stackFlow.value` with a version that still contains the nodes removed by the prior drops.

**Example**: stack = `[root, A, B, C]`, click on `A`.
- `drop(B)` → `newStack_B = [root, A, C]`. Enqueued.
- `drop(C)` → `newStack_C = [root, A, B]`. Enqueued (still snapshots original stack).
- Channel runs lambda 1: `_stackFlow.value = [root, A, C]`.
- Channel runs lambda 2: `_stackFlow.value = [root, A, B]`. Re-adds `C`! Wrong.

`NavigationChain.clear()` also has this same bug (calls `pop()` in a while loop synchronously).
`dropNodesInSubTree(filter)` from `EitherChainOrNode.kt` also has the same bug since `walk` is synchronous.

**There is no atomic multi-drop API in the navigation library.** All extension drop functions ultimately
call individual `chain.drop(node)` calls synchronously.

---

## Chosen Race-Free Approach: Sequential Drop with stackFlow Await

Drop nodes one at a time from the top, awaiting each drop's completion before proceeding.

```kotlin
scope.launchLoggingDropExceptions {
    while (true) {
        val top = chain.stackFlow.value.lastOrNull() ?: break
        if (top === targetNode) break
        chain.drop(top)
        chain.stackFlow.first { it.lastOrNull() !== top }
    }
}
```

**Why race-free:**
1. Each iteration drops exactly ONE node — the current top of the stack.
2. `chain.drop(top)` snapshots `stack` (which currently equals the real top); only `top` is removed.
   `newStack = stack.filterNot { it === top }` is a correct filter since we just verified `top ===
   stack.lastOrNull()`.
3. `chain.stackFlow.first { it.lastOrNull() !== top }` suspends until the channel lambda executes and
   `_stackFlow.value` is updated. Since the channel is a serial FIFO coroutine, and we just enqueued
   exactly one lambda for `top`, this completes as soon as the channel processes that one lambda.
4. No hang possible: if `top` has somehow already been removed (race with external navigation),
   `it.lastOrNull() !== top` is immediately true and `first` returns on the next emission.
5. After the await, the loop re-reads `chain.stackFlow.value.lastOrNull()` fresh — correct state.

This is a coroutine suspension, so it MUST be launched in `scope.launchLoggingDropExceptions`.

---

## 1. TopBarViewModel.kt (commonMain) Changes

**File**: `features/ui/topBar/src/commonMain/kotlin/ui/TopBarViewModel.kt`

### New imports (add to existing import block)

```kotlin
import dev.inmo.navigation.core.NavigationChain
import kotlinx.coroutines.flow.first
```

`kotlinx.coroutines.flow.Flow` import can be REMOVED if currently unused (it IS currently imported
but not directly referenced as a type in the file — check: it appears in imports but all usages are
`StateFlow`, `MutableRedeliverStateFlow`, `.map`, `.merge`, `.flowOf` which don't require an explicit
`Flow` import). Confirm before removing.

### New private field

```kotlin
/**
 * The scaffold's main navigation chain, cached after the first subtree-change event.
 * Updated on every subtree-change alongside [_titleProviders].
 */
private var mainChain: NavigationChain<ViewConfig>? = null
```

Place immediately before or after `private val _titleProviders`.

### Update `init` block

Change the `init` subscription from:

```kotlin
merge(flowOf(Unit), rootChain.changesInSubTreeFlow().map { }).subscribeLoggingDropExceptions(scope) {
    val mainChain = rootChain.findInSubTree(MainNavigationChainId)
    _titleProviders.value = mainChain
        ?.stackFlow
        ?.value
        ?.filterIsInstance<TopBarTitleProvider>()
        ?: emptyList()
}
```

To:

```kotlin
merge(flowOf(Unit), rootChain.changesInSubTreeFlow().map { }).subscribeLoggingDropExceptions(scope) {
    val resolved = rootChain.findInSubTree(MainNavigationChainId)
    mainChain = resolved
    _titleProviders.value = resolved
        ?.stackFlow
        ?.value
        ?.filterIsInstance<TopBarTitleProvider>()
        ?: emptyList()
}
```

(Rename the local `val mainChain` → `val resolved` to avoid shadowing the newly declared field.)

### New method `onCrumbSelected`

Add after `onSearchQueryChanged`:

```kotlin
/**
 * Handles a user tap on a non-current breadcrumb segment.
 *
 * Collapses the main navigation chain so that [provider] becomes the top-most node. Nodes
 * above [provider] in the stack are popped one at a time, each pop awaited before the next
 * is issued, to avoid the race condition in [dev.inmo.navigation.core.NavigationChain.drop]
 * where a synchronous batch of drops would each snapshot the same pre-drop stack and
 * overwrite each other when the channel applies them.
 *
 * No-ops when [provider] is not found in the chain, is already the top node, the chain is
 * not yet resolved, or [provider] is not a [dev.inmo.navigation.core.NavigationNode].
 *
 * @param provider The [TopBarTitleProvider] node to navigate back to.
 */
fun onCrumbSelected(provider: TopBarTitleProvider) {
    val chain = mainChain ?: return
    val targetNode = provider as? NavigationNode<*, ViewConfig> ?: return
    scope.launchLoggingDropExceptions {
        while (true) {
            val top = chain.stackFlow.value.lastOrNull() ?: break
            if (top === targetNode) break
            chain.drop(top)
            chain.stackFlow.first { it.lastOrNull() !== top }
        }
    }
}
```

### Final imports list for TopBarViewModel.kt

```kotlin
import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.extensions.changesInSubTreeFlow
import dev.inmo.navigation.core.extensions.findInSubTree
import dev.inmo.navigation.core.extensions.rootChain
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.common.client.models.MainNavigationChainId
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
```

(Remove `import kotlinx.coroutines.flow.Flow` — not referenced as a type in this file.)

---

## 2. TopBarView.kt (jsMain) Changes

**File**: `features/ui/topBar/src/jsMain/kotlin/ui/TopBarView.kt`

### Use the `Breadcrumb` component (CODING.md rule 4)

Per CODING.md rule 4: "A view MUST compose from the standard Calm Studio @Composable components
wherever one fits." The `Breadcrumb` composable in
`features/common/client/src/jsMain/kotlin/ui/components/CalmPage.kt` accepts
`items: List<CrumbItem>` (each with `onClick: (() -> Unit)?`) and `current: String`. This maps
exactly to our use case: non-last providers are clickable items; the last is the current page.

The current TopBar view renders the crumb section using raw DOM (`Div(.crumb)`) — a rule 4 violation.
This change also fixes that.

### New imports (add)

```kotlin
import dev.inmo.wishlist.features.common.client.ui.components.Breadcrumb
import dev.inmo.wishlist.features.common.client.ui.components.CrumbItem
```

### Remove imports (no longer needed after the change)

```kotlin
import org.jetbrains.compose.web.dom.B       // used only in the old crumb rendering
import org.jetbrains.compose.web.dom.Span    // used only in the old crumb rendering (sep and title)
```

Verify: `Span` is only used in `SearchIcon` (for raw SVG injection). If `Span` is used in `SearchIcon`,
keep it. Reading the current file: `Span` appears in `SearchIcon()` — KEEP `Span`.
`B` is only used in the crumb `B { Text(title) }` block — REMOVE `B`.

### Updated crumb rendering block in `onDraw()`

Replace:

```kotlin
val crumbTitles = titleProviders.map { it.title }.filter { it.isNotBlank() }
if (crumbTitles.isNotEmpty()) {
    Div({ classes(CalmStudioStyleSheet.crumbbar) }) {
        Div({ classes(CalmStudioStyleSheet.crumb) }) {
            crumbTitles.forEachIndexed { index, title ->
                if (index > 0) {
                    Span({ classes(CalmStudioStyleSheet.sep) }) { Text("/") }
                }
                if (index == crumbTitles.lastIndex) {
                    B { Text(title) }
                } else {
                    Span { Text(title) }
                }
            }
        }
    }
}
```

With:

```kotlin
val crumbs = titleProviders.filter { it.title.isNotBlank() }
if (crumbs.isNotEmpty()) {
    Div({ classes(CalmStudioStyleSheet.crumbbar) }) {
        val ancestors = crumbs.dropLast(1).map { provider ->
            CrumbItem(
                label = provider.title,
                onClick = { viewModel.onCrumbSelected(provider) }
            )
        }
        val current = crumbs.last().title
        Breadcrumb(items = ancestors, current = current)
    }
}
```

**Why correct:**
- `crumbs.dropLast(1)` = all non-last providers → rendered as clickable `A` via `Breadcrumb`.
- `crumbs.last().title` = current page → rendered as `B` (bold, non-clickable) via `Breadcrumb`.
- Blank-title filter remains: `filter { it.title.isNotBlank() }` applied before indexing.
- The single-crumb case: `crumbs.dropLast(1)` = empty list; `Breadcrumb(items=[], current=title)`
  renders just `B` with no `A` elements. Correct — no clickable crumb when there is only one.
- `viewModel.onCrumbSelected(provider)` passes the `TopBarTitleProvider` reference to the ViewModel.
  The ViewModel casts to `NavigationNode<*, ViewConfig>` internally.

### `.crumb a` CSS verification

`CalmStudioStyleSheet.kt` line 290–298 defines:

```kotlin
val crumb by style {
    "a" style {
        property("text-decoration", "none"); property("cursor", "pointer")
        self + hover style { property("color", "var(--cs-ink)") }
    }
    ...
}
```

`Breadcrumb` renders its `A` elements inside `.crumb`. The `.crumb a` selector applies. **No stylesheet
change needed.**

### Final imports list for TopBarView.kt

```kotlin
package dev.inmo.wishlist.features.ui.topBar.ui

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.compose.InjectNavigationChain
import dev.inmo.navigation.compose.InjectNavigationNode
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.Breadcrumb
import dev.inmo.wishlist.features.common.client.ui.components.CrumbItem
import dev.inmo.wishlist.features.ui.auth.ui.AuthViewConfig
import dev.inmo.wishlist.features.ui.topBar.TopBarStrings
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
```

(Removed: `B`. `Span` KEPT for `SearchIcon`. `Label` KEPT for search label. `Text` KEPT for
search placeholder, kbd hint, and via `Breadcrumb` composition internally.)

---

## 3. JVM/Android Views

No changes. JVM and Android `TopBarView` implementations do not render breadcrumbs. Confirmed: only
`jsMain/kotlin/ui/TopBarView.kt` renders the `.crumb` section.

---

## 4. Plugin/DI Changes

None. `onCrumbSelected` does not require any new Koin binding. `mainChain` is resolved via the
existing `rootChain` held in `TopBarViewModel`, not via DI.

---

## 5. README Architecture Notes Update

The Coding role MUST append the following text to the `## Architecture Notes` section of
`features/ui/topBar/README.md` (do NOT modify `## Operator Notes`):

```markdown
- Breadcrumb segments (all except the last/current) are clickable. `TopBarView` (JS) maps each
  non-last `TopBarTitleProvider` to a `CrumbItem` with an `onClick` that calls
  `viewModel.onCrumbSelected(provider)`, then passes the resulting list to the `Breadcrumb`
  composable (Calm Studio standard component).
- `TopBarViewModel.onCrumbSelected(provider)` casts the provider to
  `NavigationNode<*, ViewConfig>` and pops the main chain's stack down to that node using a
  sequential-await loop: one `chain.drop(top)` per iteration, each followed by
  `chain.stackFlow.first { it.lastOrNull() !== top }` to await the channel's application of
  that drop. This avoids the race in `NavigationChain.drop()` where a synchronous batch of
  drops would all snapshot the pre-drop stack and overwrite each other.
- `TopBarViewModel` caches the resolved main chain in `private var mainChain` (updated in the
  same `init` subscription that refreshes `_titleProviders`) so `onCrumbSelected` can reference
  it without re-resolving the subtree on each click.
```

---

## 6. Build Command

```
./gradlew :wishlist.features.ui.topBar:build
```

---

## 7. Inconsistency Report to Orchestrator

The planning step (0-planning.md §2 "Better approach: compute drop list upfront, batch-drop") proposes
a synchronous `forEach { chain.drop(it) }` batch. **This approach is incorrect** — it contains the
exact race condition described in the task brief. Concretely:

- `drop(node)` computes `newStack = stack.filterNot { it === node }` **synchronously before enqueuing**
  the channel lambda. Each call in the batch reads the same unmodified `stack` (the channel has not
  processed any lambdas yet). When the channel runs the lambdas, each overwrites `_stackFlow.value`
  with a snapshot that includes the nodes the prior lambdas removed.

The planning step's claim "No await needed: Each `drop(node)` lambda filters by reference. If by the
time a lambda runs the node is already gone..." is incorrect because `newStack` is NOT recomputed
inside the lambda — it is captured at call time. The lambda simply does `_stackFlow.value = newStack`
where `newStack` was computed from the original stack.

The sequential-await approach (used by this architecture) is the correct solution. The planning step
should be treated as wrong on this specific technical point; all other structural decisions (ViewModel
method, mainChain field, no interactor change, no Plugin change) are sound and preserved.
