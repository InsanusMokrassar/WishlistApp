Model: claude-sonnet-4-6
Execution time: 180
Tokens used: ~28000
Changed files: agents/task/851f64f2-e94c-47e8-9172-ef7e063dbeb5/0-planning.md

Model choice: claude-sonnet-4-6 (Sonnet). PLAN.md has no explicit model priority list.
Sonnet is the current runtime model; no escalation to opus/fable required for a planning task
of this scope (two-file change, well-understood domain).

---

# Plan: GitHub Issue #54 — Clickable Breadcrumbs

## Summary

Make breadcrumb segments (except the last/current one) clickable in the JS top bar.
Clicking a crumb collapses the main navigation chain so the clicked node becomes the topmost.

Two files change:
- `features/ui/topBar/src/commonMain/kotlin/ui/TopBarViewModel.kt`
- `features/ui/topBar/src/jsMain/kotlin/ui/TopBarView.kt`

No JVM/Android changes. No interactor changes. No README changes needed unless noted.

---

## 1. Where click handling and navigation logic belong

**MVVM rule**: View is dumb. View fires a named intent to the ViewModel; ViewModel executes logic.

- **View** (`TopBarView.kt` jsMain): detects click on a non-last crumb, calls
  `viewModel.onCrumbSelected(provider)`. No navigation logic in the view.
- **ViewModel** (`TopBarViewModel.kt` commonMain): exposes `fun onCrumbSelected(provider: TopBarTitleProvider)`.
  Resolves the main chain and collapses it. This is purely a navigation concern — correct home for it.

The prior PR (closed) correctly placed `onCrumbSelected` in the ViewModel. That structural decision is sound; only implementation details below may need refinement.

---

## 2. Navigation logic: how to collapse the chain

### Key facts about NavigationChain

- `drop(node)` enqueues a lambda onto `nodesChangesChannel` (via `trySend`). Returns immediately.
- The channel is processed sequentially by `nodesChangesJob` (single coroutine).
- Each drop lambda uses reference equality (`currentNode === node`) against the stack AT execution time.
- `stackFlow` is a `MutableRedeliverStateFlow` — value updates inside the channel coroutine.

### Prior PR approach (sequential drop + await)

```kotlin
var last = chain.stackFlow.value.lastOrNull()
while (last != null && last !== targetNode) {
    chain.drop(last)
    chain.stackFlow.first { it.lastOrNull() !== last } // await removal
    last = chain.stackFlow.value.lastOrNull()
}
```

This is correct in principle but:
- Awaits on `stackFlow.first { ... }` between drops — suspends the coroutine.
- If the node is already gone (race), `stackFlow.first` may hang indefinitely (no timeout).
- Overly complex for what we need.

### Better approach: compute drop list upfront, batch-drop

Since `drop(node)` uses reference equality and the channel is serial, we can:

1. Snapshot `chain.stackFlow.value` immediately.
2. Find index of `targetNode` in that snapshot.
3. Collect all nodes ABOVE `targetNode` (from `targetNode+1` to end), reversed (top-down).
4. Call `chain.drop(node)` for each — no awaiting. All lambdas enqueue in order, execute serially.

```kotlin
fun onCrumbSelected(provider: TopBarTitleProvider) {
    val chain = mainChain ?: return
    val targetNode = provider as? NavigationNode<*, ViewConfig> ?: return
    val stack = chain.stackFlow.value
    val targetIndex = stack.indexOfFirst { it === targetNode }
    if (targetIndex < 0 || targetIndex == stack.lastIndex) return  // not found or already top
    // nodes above target, top-down order
    val toRemove = stack.subList(targetIndex + 1, stack.size).reversed()
    toRemove.forEach { chain.drop(it) }
}
```

No coroutine launch needed (no suspension). No race risk — all drops are reference-based against
nodes captured from the snapshot. Channel serializes execution so state is consistent.

**Why no await needed**: Each `drop(node)` lambda filters by reference. If by the time a lambda
runs the node is already gone (shouldn't happen in this flow since we snapshot upfront), the
lambda is a no-op. No hang possible.

**Note on `mainChain` field**: The ViewModel needs to retain `mainChain` as a `var` field (updated
in the `init` subscription), exactly as the prior PR did. Current `master` code does NOT have this
field — it uses a local val in the lambda. So `mainChain` field must be added.

---

## 3. Edge cases

| Case | Handling |
|------|----------|
| Click on last/current crumb | `targetIndex == stack.lastIndex` → early return (no-op) |
| Provider not found in chain | `targetIndex < 0` → early return |
| Main chain not yet resolved | `mainChain == null` → early return at top |
| Provider is not a NavigationNode | `as? NavigationNode<*, ViewConfig>` returns null → early return |
| Blank-title crumbs | Already filtered in view (`filter { it.title.isNotBlank() }`). The provider list passed to `onCrumbSelected` only contains non-blank providers, but the ViewModel guard on `targetIndex` is the canonical safety net |
| Single crumb (only one item, no clickable ones) | View only renders `A` for `index < crumbs.lastIndex`, so no click possible |

---

## 4. Exact file changes

### `features/ui/topBar/src/commonMain/kotlin/ui/TopBarViewModel.kt`

Changes:
1. Add import: `dev.inmo.navigation.core.NavigationChain`
2. Add private field: `private var mainChain: NavigationChain<ViewConfig>? = null`
3. In `init` block: assign `mainChain = mainChain` (the local val) before setting `_titleProviders.value`
4. Add method `onCrumbSelected(provider: TopBarTitleProvider)` (no suspend, no launch needed)

Remove import of `kotlinx.coroutines.flow.Flow` if it becomes unused (check current unused imports).

### `features/ui/topBar/src/jsMain/kotlin/ui/TopBarView.kt`

Changes:
1. Add import: `org.jetbrains.compose.web.dom.A`
2. In `onDraw`: iterate `titleProviders` directly (keeping provider reference, not just title string).
   Change the crumb rendering loop to:
   - Keep `filter { it.title.isNotBlank() }` on providers (not titles)
   - For `index < crumbs.lastIndex`: render `A(attrs = { onClick { viewModel.onCrumbSelected(provider) } }) { Text(provider.title) }`
   - For `index == crumbs.lastIndex`: render `B { Text(provider.title) }`

The existing `.crumb a` CSS rule (lines 291-296 in CalmStudioStyleSheet.kt:
`text-decoration: none; cursor: pointer`) already styles the `A` elements correctly. No CSS changes.

### README.md (features/ui/topBar/README.md)

Update `## Models` table row for `TopBarViewModel` to mention `onCrumbSelected()`.
Update `## Architecture Notes` to note breadcrumb click navigation.

**IMPORTANT**: Do NOT modify the `## Operator Notes` section.

### Files NOT changed
- JVM TopBarView — breadcrumbs not rendered there
- Android TopBarView — breadcrumbs not rendered there
- TopBarViewInteractor — crumb navigation operates on the already-held `mainChain`, not root chain
- TopBarTitleProvider — interface unchanged
- CalmStudioStyleSheet — existing `.crumb a` styling already correct

---

## 5. Build command

```
./gradlew :wishlist.features.ui.topBar:build
```

---

## 6. Open questions / risks for operator

1. **Why was the prior PR silently closed?** The structural approach looks correct (ViewModel method,
   `A` elements with onClick, batch-drop logic). Possible rejection reasons:
   - The `stackFlow.first { ... }` await could hang on edge cases (mitigated by our batch approach).
   - Possibly a style/code-quality concern with the loop construct.
   - Possibly a concern about `provider as? NavigationNode<*, ViewConfig>` cast (safe given all
     implementors extend ComposeNode → NavigationNode, but relies on a runtime cast).
   - The operator may have had a specific API in mind (e.g., a dedicated `dropAbove(node)` method
     on NavigationChain). **If the operator prefers a cleaner chain API, ask before implementing.**

2. **No-suspend `onCrumbSelected`**: The batch approach requires no coroutine — all `drop()` calls
   are non-suspending. This is simpler than the prior PR's launched coroutine. Should be fine.

3. **`mainChain` field retained between subscription updates**: This field may hold a stale reference
   if the scaffold rebuilds its main chain. However, `_titleProviders` is also updated on each change,
   so by the time a user can click a crumb, `mainChain` will be current. Low risk.

4. **`stackFlow.first` hang risk (mitigated)**: Our approach does NOT use `stackFlow.first` — no
   await, no hang possibility. This is the main improvement over the prior PR.

5. **Operator confirmation needed**: Before implementation, confirm the batch-drop approach (no await)
   is acceptable, or if a different strategy is preferred.
