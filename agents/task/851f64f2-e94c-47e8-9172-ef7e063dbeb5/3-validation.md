Model: claude-sonnet-4-6
Execution time: 185
Tokens used: ~31000
Changed files: agents/task/851f64f2-e94c-47e8-9172-ef7e063dbeb5/3-validation.md

Model choice: Sonnet. VALIDATOR.md specifies no explicit model priority. This task is code-reading + cross-step consistency check over a two-file change on a well-understood MVVM codebase. Sonnet is sufficient; no escalation to opus/fable required.

---

# Step 3 — Validation: Issue #54 Clickable Breadcrumbs

## Build command and result

```
./gradlew :wishlist.features.ui.topBar:build
```

**BUILD SUCCESSFUL in 17s** (582 actionable tasks: 19 executed, 563 up-to-date)

---

## Checklist

### 1. CORRECTNESS — clicking a non-last crumb navigates to that node

**PASS.**

`TopBarView.kt:78–91`: `titleProviders` is mapped to `(provider, title)` pairs inside `onDraw()` (composable context); blank titles filtered out. `crumbs.dropLast(1)` yields all non-last providers. Each becomes a `CrumbItem(label=title, onClick={ viewModel.onCrumbSelected(provider) })`. Last crumb title passed as `current` to `Breadcrumb` — rendered as `<b>`, no onClick (CalmPage.kt:83–85: `B { Text(current) }`).

`TopBarViewModel.kt:98–109`: `onCrumbSelected` receives the provider, returns early if `mainChain == null`, casts to `NavigationNode<*, ViewConfig>` via `as?` (returns null → early return if not a node). Launches sequential-await loop on `scope`.

**Provider→node identity mapping**: `TopBarTitleProvider` implementors are `NavigationNode` subclasses (ComposeView → ComposeNode → NavigationNode). The cast `provider as? NavigationNode<*, ViewConfig>` is sound at runtime. The provider reference stored in the lambda IS the node itself — same object identity. The loop checks `top === targetNode` (reference equality). Correct.

**Single-crumb no-op**: `crumbs.dropLast(1)` = empty list → `ancestors` empty → no clickable `A` elements. `onCrumbSelected` never called.

**Already-top crumb**: First `top === targetNode` → `break`. No-op. Correct.

### 2. Drop loop terminates / race-free

**PASS.**

`NavigationChain.kt:85–109`: `drop(node)` computes `newStack = stack.filterNot { it === node }` **synchronously before enqueueing** (`stack` is `stackFlow.value` at call time). Lambda just does `_stackFlow.value = newStack`. Channel is serial FIFO.

Architecture correctly overrode planning's batch-drop (1-architecture.md §"Critical Finding"). Sequential-await loop:

```kotlin
while (true) {
    val top = chain.stackFlow.value.lastOrNull() ?: break   // (a)
    if (top === targetNode) break                            // (b)
    chain.drop(top)                                         // (c)
    chain.stackFlow.first { it.lastOrNull() !== top }       // (d)
}
```

Each iteration:
- (a): reads fresh `stackFlow.value` — correct after each await.
- (c): `drop(top)` — `top` IS currently `stackFlow.value.lastOrNull()` so `newStack` correctly excludes exactly `top`.
- (d): suspends until channel lambda runs and `_stackFlow.value` is updated; since only one lambda was enqueued, this completes as soon as it runs.
- No hang possible: if `top` was already removed externally before (d) evaluates, `it.lastOrNull() !== top` is already true on next emission → returns immediately.
- Infinite loop impossible: each iteration either breaks (target found / stack empty) or removes one node. Finite stack → terminates.
- If `targetNode` is not in chain at all: loop drops nodes down to empty, `stack.lastOrNull()` returns null → break. Correct.

### 3. @Composable title reading

**PASS.**

`TopBarView.kt:78–79`: titles are read INSIDE `onDraw()` composable — `titleProviders.map { provider -> provider to provider.title }`. The `provider.title` getter is called here, inside composable context. Lambdas on lines 85 (`onClick = { viewModel.onCrumbSelected(provider) }`) capture `provider` (not title). The lambdas are NOT composable — they don't call `provider.title`.

Blank-title filter preserved: `.filter { (_, title) -> title.isNotBlank() }` at line 79.

### 4. CODING.md compliance

**PASS.**

- **KDocs**: `mainChain` field has KDoc (TopBarViewModel.kt:41–44). `onCrumbSelected` has KDoc with `@param` (TopBarViewModel.kt:85–97). Both present. Existing members (`onSearchQueryChanged`) already had KDocs in prior code.
- **No `else if`**: No `else if` chains present. `onCrumbSelected` uses early returns only.
- **Standard component**: `Breadcrumb` from `CalmPage.kt` used in `TopBarView.kt:89`. CalmPage.kt NOT modified (confirmed below).
- **No raw class-name string literals**: None introduced.

### 5. MVVM compliance

**PASS.**

- View (`TopBarView.kt`) collects state, maps providers to `CrumbItem` with onClick that calls `viewModel.onCrumbSelected(provider)`. No navigation logic in view.
- ViewModel (`TopBarViewModel.kt`) holds `mainChain`, casts provider to `NavigationNode`, runs the drop loop. All navigation logic in ViewModel.
- Intent name: `onCrumbSelected` — verb + subject, standard intent naming.

### 6. SCOPE

**PASS.**

Changed files per coding step:
- `features/ui/topBar/src/commonMain/kotlin/ui/TopBarViewModel.kt` ✓
- `features/ui/topBar/src/jsMain/kotlin/ui/TopBarView.kt` ✓
- `features/ui/topBar/README.md` ✓

No JVM/Android view changes. No Plugin/DI changes. `README.md ## Operator Notes` section (lines 3–5) is empty comment block — untouched. `## Models` table updated (line 29: `onCrumbSelected(provider)` added). `## Architecture Notes` appended (lines 43–54).

### 7. BUILD

**PASS.**

```
BUILD SUCCESSFUL in 17s
582 actionable tasks: 19 executed, 563 up-to-date
```

### 8. Cross-step consistency: architecture overrode planning's batch-drop

**PASS.**

Planning step (0-planning.md §2) proposed `toRemove.forEach { chain.drop(it) }` — synchronous batch with no await. Architecture step (1-architecture.md §"Critical Finding") correctly identified and documented the race: `drop(node)` computes `newStack` synchronously from `stack` (current `stackFlow.value`) **before** the lambda enqueues, so all batch drops read the same original stack and each lambda overwrites `_stackFlow.value` with a version that re-adds nodes the prior lambdas removed.

This is confirmed by reading `NavigationChain.kt:85–109`: `newStack` is captured at call time (line 88: `val newStack = stack.filterNot { ... }`). The channel lambda (lines 98–108) uses `newStack` directly — NOT recomputed inside the lambda.

Architecture's sequential-await replaces batch-drop. Coding followed architecture exactly.

### CalmPage.kt not modified — confirmed

Read `features/common/client/src/jsMain/kotlin/ui/components/CalmPage.kt` — no modifications. `Breadcrumb` at lines 76–86 accepts `items: List<CrumbItem>` and `current: String`. Each `CrumbItem` has `onClick: (() -> Unit)?`. When `onClick != null`, `A` element gets `onClick { handler() }` (line 80–81). When `onClick == null`, `A` renders with no handler (static). Current page rendered as `B { Text(current) }` (line 84). This genuinely supports clickable items.

---

## Issues Found

None of medium or higher importance.

### Minor observation (LOW, not reported to orchestrator)

`TopBarView.kt:78` reads `provider.title` for ALL providers upfront (not just non-blank ones) before filtering. This is one extra composable read per blank-title provider per recomposition. Functionally correct; not a bug.

---

## VERDICT: PASS

All checklist items pass. Build is SUCCESSFUL. No medium+ inconsistencies found.

No report to orchestrator required.
