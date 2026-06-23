# Feature: UI / TopBar

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Top bar feature shown in the scaffold's `topConfig` slot. Hosts the auth widget
(login button / logout button — see `features/ui/auth`) embedded as a
sub-navigation chain, plus a "Change server URL" affordance on platforms where
the server URL is user-configurable (JVM + Android).

JS does not show the URL change button because the browser controls the origin.
On JS the bar follows the Calm Studio shell: a global search field
(people / lists / items) on the left and the auth action on the right, with a
slim breadcrumb (`.crumb`) rendered under the bar for content depth. The
breadcrumb is still fed by the main chain's `TopBarTitleProvider`s.
The JS search field is currently disabled (non-interactive, `<input disabled>`) with a native browser tooltip "This feature will be implemented soon" shown on hover.

## Routes

None — client-only UI feature.

## Models

| Type | Description |
|------|-------------|
| `TopBarViewConfig` | Empty `@Serializable class` |
| `TopBarViewModel` | Tracks `titleProviders` (breadcrumb) and `searchQueryState`; `onChangeServerUrl()` / `onSearchQueryChanged()` / `onCrumbSelected(provider)` events |
| `TopBarViewInteractor` | `onChangeServerUrl(node)` — pushes `ServerUrlViewConfig` on the root chain |

## Architecture Notes

- `TopBarView` per platform renders the auth widget via
  `InjectNavigationChain<ViewConfig> { InjectNavigationNode(AuthViewConfig()) }`.
  The embedded chain runs the existing `AuthView` MVVM unmodified except for
  its UI (the auth view is rewritten to render as an inline collapsed widget).
- The "Change server URL" button is only rendered in the JVM and Android views.
- The `TopBarViewInteractor` implementation lives in `client/ClientPlugin`
  and operates on the root `NavigationChain<ViewConfig>`.
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
