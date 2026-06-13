# Feature: UI / TopBar

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Top bar feature shown in the scaffold's `topConfig` slot. Hosts the auth widget
(login button / logout button — see `features/ui/auth`) embedded as a
sub-navigation chain, plus a "Change server URL" affordance on platforms where
the server URL is user-configurable (JVM + Android).

JS does not show the URL change button because the browser controls the origin.

## Routes

None — client-only UI feature.

## Models

| Type | Description |
|------|-------------|
| `TopBarViewConfig` | Empty `@Serializable class` |
| `TopBarViewModel` | Stateless except for `onChangeServerUrl()` event |
| `TopBarViewInteractor` | `onChangeServerUrl(node)` — pushes `ServerUrlViewConfig` on the root chain |

## Architecture Notes

- `TopBarView` per platform renders the auth widget via
  `InjectNavigationChain<ViewConfig> { InjectNavigationNode(AuthViewConfig()) }`.
  The embedded chain runs the existing `AuthView` MVVM unmodified except for
  its UI (the auth view is rewritten to render as an inline collapsed widget).
- The "Change server URL" button is only rendered in the JVM and Android views.
- The `TopBarViewInteractor` implementation lives in `client/ClientPlugin`
  and operates on the root `NavigationChain<ViewConfig>`.
