# Feature: UI / ServerUrl

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

JVM (Desktop) + Android only. Provides a dedicated screen for editing the
server URL persisted in `features/auth/client`'s `ServerUrlStorage`.

This view is the **first** node pushed on top of the root chain after
`EmptyConfig` on JVM/Android when the storage holds no URL. On JS the screen
is not registered and not used — the browser controls the origin.

The "save" action delegates to `ServerUrlViewInteractor.onSaved` which is
responsible for arranging the next screen (scaffold) on the root chain.

## Routes

None — client-only UI feature, JVM + Android only.

## Models

| Type | Description |
|------|-------------|
| `ServerUrlViewConfig` | Empty `@Serializable class` |
| `ServerUrlModel` | Wraps `ServerUrlStorage` (suspend `getServerUrl` / `saveServerUrl`) |
| `ServerUrlViewInteractor` | `onSaved(node)` — root-chain navigation owned by `client/ClientPlugin` |
| `ServerUrlViewModel` | Input/loading state, validated save |

## Architecture Notes

- Inherits the suspend-based storage pattern from
  `features/auth/client/ServerUrlStorage` — no new storage interface needed.
- View is only available on JVM + Android. No `jsMain` source set.
- The interactor implementation lives in the top-level `client/` module and
  decides whether to push `ScaffoldViewConfig` or simply pop, depending on
  whether the scaffold is already on the chain.
