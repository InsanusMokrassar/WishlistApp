# Feature: ScaffoldView

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Pure layout container view with three optional navigation slots:
- **top** — full-width row pinned to the top of the screen
- **left** — fixed-width column below the top slot, spanning remaining height
- **main** — flexible area filling all remaining width and height

Each non-null slot config bootstraps its own independent `InjectNavigationChain` / `InjectNavigationNode`.
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
- JS platform defines `ScaffoldViewStylesheet : StyleSheet` for the flex-based CSS layout;
  JVM and Android use `Column`/`Row`/`Box` with `Modifier.weight`.
- Register `JSPlugin` / `JVMPlugin` / `AndroidPlugin` in each platform's plugin list
  alongside other UI feature plugins.
