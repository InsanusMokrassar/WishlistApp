# Feature: ScaffoldView

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Pure layout container view with three optional navigation slots:
- **top** — full-width row pinned to the top of the screen
- **left** — fixed-width column below the top slot, spanning remaining height
- **main** — flexible area filling all remaining width and height

Each non-null slot is rendered as its own navigation sub-chain with a stable id
(`TopNavigationChainId` / `LeftNavigationChainId` / `MainNavigationChainId`).
No ViewModel logic, no Model, no Interactor — scaffold is presentation-only.

## Routes

None — client-only UI feature, no server routes.

## Models

| Type | Description |
|------|-------------|
| `ScaffoldViewConfig` | Carries three nullable `@Polymorphic ViewConfig` fields: `topConfig`, `leftConfig`, `mainConfig` |
| `ScaffoldViewModel` | Minimal ViewModel; exposes `config: ScaffoldViewConfig`; no mutable state |

## Architecture Notes

- `ScaffoldViewConfig` uses `@Polymorphic` on its `ViewConfig?` fields so kotlinx.serialization
  dispatches to the correct concrete serializer via the runtime `SerializersModule`.
- The feature has no `ViewInteractor` — navigation inside each slot is fully owned by
  whatever feature config is passed as `topConfig` / `leftConfig` / `mainConfig`.
- JS platform renders the Calm Studio shell: a full-height left sidebar next to a `.main` column
  that stacks the top bar over a scrolling `.content` area. The layout classes (`app`, `main`,
  `content`) live in the global shell CSS (`client/.../css/calm-studio.css`), and each slot view
  owns its own block class (sidebar → `.sidebar`, top bar → `.topbar`); the former
  `ScaffoldViewStylesheet` is removed. JVM and Android use `Column`/`Row`/`Box` with `Modifier.weight`.
- **Stable slot chain ids (all platforms):** every slot's chain is created with a stable
  `NavigationChainId` — `TopNavigationChainId` / `LeftNavigationChainId` / `MainNavigationChainId` —
  so chains are addressable by slot (the top bar finds the main chain by id; a restored hierarchy can
  be matched to its slot).
- **JS slot restoration:** the JS `ScaffoldView` renders each slot via a private `ScaffoldSlot(id, slotConfig)`
  helper. When a sub-chain with the matching id is **already attached** (a hierarchy restored from the URL
  by `UrlParametersNavigationConfigsRepo`), it draws that chain via the protected `SubchainsHost`; otherwise
  it falls back to a plain `InjectNavigationChain(id) { InjectNavigationNode(slotConfig) }`. This is what lets
  shared/reloaded deep links re-populate the scaffold's main chain instead of being discarded.
  JVM/Android always use `InjectNavigationChain` directly (in-memory repo, no restore path).
- The three slot chain ids live in `features/common/client` (`models/NavigationChainIds.kt`).
- Register `JSPlugin` / `JVMPlugin` / `AndroidPlugin` in each platform's plugin list
  alongside other UI feature plugins.
