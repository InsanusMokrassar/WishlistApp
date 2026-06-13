# Feature: UI / Booking

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Client-only UI scenario for gift booking, extracted from `features/ui/wishlist` (issue #29, PR #31). Wraps `features/booking/client` via `BookingModel`. JS views use Bootstrap CSS classes via Compose HTML; JVM uses Material v2; Android uses Material3.

## Screens

| Screen | ViewConfig | ViewModel | Description |
|--------|-----------|-----------|-------------|
| Book item (view A) | `BookingViewConfig(itemId, wishlistId)` | `BookingViewModel` | **Compact, embedded-inline** gift-booking control (single button or short text in one Row / flex row), drawn INLINE on the wishlist item screen via that screen's `InjectNavigationChain` / `InjectNavigationNode`. Owns no back button and no top-bar title. Renders only when `BookingViewModel.bookingState` is non-null; the server returns no state for the item owner and for anonymous callers (rule 3 / authorized-only), so those callers see nothing. States (sealed `BookingState`): `Free` → Book button; `BookedByMe` → "You booked this item" + Cancel button; `Booked` → "Someone is going to gift this" (booked by another, no identity). This is the config returned by the booking `WishlistAdditionalConfigsProvider` from the wishlist item screen. |
| My presents (view B) | `MyPresentsBooksViewConfig()` | `MyPresentsBooksViewModel` | Lists every wishlist item the caller has booked (the presents the caller plans to make). **Unreachable by design** — issue #29 point #6: no navigation path pushes `MyPresentsBooksViewConfig`. The view, ViewModel, config, and `NavigationNodeFactory` are registered, but nothing opens it yet. |

## Models

| Type | Description |
|------|-------------|
| `BookingModel` | Interface wrapping `features/booking/client` `BookingFeature`; methods `getBookingState(itemId)`, `bookItem(itemId)` → `BookingFeature.tryBook`, `cancelBooking(itemId)` → `BookingFeature.cancelBooking`, `myPresentsBooks()`. Single anonymous impl in the feature's common `Plugin.kt`. |
| `MyPresentsBooksViewInteractor` | `suspend fun onBack(node)` → `node.chain.pop()`; impl in `client/ClientPlugin`. |

> `BookingViewInteractor` was REMOVED: view A is embedded inline with no back navigation, so `BookingViewModel` takes only `(node, model)` and the `client/ClientPlugin` binding is gone.

## Architecture Notes

- View A (`BookingView`) is compact and embedded inline — it has NO top-bar title and NO back button. View B (`MyPresentsBooksView`) still implements `TopBarTitleProvider` and reuses the shared `BackButton` / `ListRow` / `ScreenTitle` components.
- All booking business rules stay server-side in `features/booking`; the views only render `BookingState` and forward user intents.
- View A self-gates: when the server hides booking state (owner / anonymous) `bookingState` is `null` and the view renders nothing (defense-in-depth on top of the server gate).
- `BookingViewModel` / `MyPresentsBooksViewModel` are Koin `factory` registrations; both views are registered as `NavigationNodeFactory.Typed` in each platform plugin (`JSPlugin`, `JVMPlugin`, `AndroidPlugin`).
- Localized strings live in `BookingStrings` (EN + RU), moved from the wishlist UI feature.
