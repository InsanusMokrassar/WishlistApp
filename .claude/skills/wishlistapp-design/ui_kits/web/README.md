# WishlistApp — Web client UI kit

An interactive, high-fidelity recreation of the WishlistApp **web client** (the
Compose-HTML / Kotlin-JS app served by the Ktor server). It composes the design
system's component primitives over fake data — no backend.

## Files

| File | Role |
|------|------|
| `index.html` | Entry point. Loads React + Babel + the compiled `_ds_bundle.js`, then `data.js` and `app.jsx`. |
| `data.js` | Fake domain data (`window.WL_DATA`): users, wishlists, items, price formatting. |
| `app.jsx` | The whole app: a small navigation stack plus one component per screen. |

## What it demonstrates

A click-through of the real app's flows:

1. **Users list** (home) — every user with an avatar; "My profile" once logged in.
2. **User wishlists** — a user's wishlists, with "All items" and (for the owner) "New Wishlist".
3. **All items** — aggregated items across one user's wishlists, sortable.
4. **Wishlist detail** — items in Grid or List view, sortable by Cost / Priority / Title; owners get "Add Item"; non-owners get "Copy to my profile" (shows the success alert).
5. **Item detail** — description, price, priority pill, links, image placeholder.
6. **Item edit** — title / description / price / priority form with a delete-confirmation modal.
7. **Log in / Register** — navbar buttons open the modal; logging in as `you` reveals owner controls on your own wishlists.

## Conventions copied from the source

- Blue (`bg-primary`) `navbar-dark` top bar whose brand is the breadcrumb of screen
  titles joined with " / ".
- `container py-3` page wrapper; `list-group` rows; Bootstrap `card` grid
  (`row row-cols-1 row-cols-sm-2 row-cols-md-3 g-3`).
- Title-case action labels ("Add Item", "New Wishlist"), sentence-case messages,
  question-style confirmations ("Delete item?").

This is a recreation, not production code — interactions are faked and the admin
panel is represented only by the `admin` tag on the `root` user.
