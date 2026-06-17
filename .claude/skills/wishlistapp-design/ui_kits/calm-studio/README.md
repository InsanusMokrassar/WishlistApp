# Calm Studio — WishlistApp web redesign (target spec)

The forward-looking design language for the **web client**. It replaces the stock
Bootstrap look with a premium, tool-like aesthetic and a clearer navigation model.
This folder is the **reference implementation** — read it as the source of truth when
implementing the redesign (the root `implement-calm-studio.sh` drives that build).

## Run it

Open `index.html`. It's a self-contained React + Babel prototype — no design-system
bundle needed; it ships its own `styles.css`.

## Files

| File | Role |
|------|------|
| `styles.css` | The complete redesign stylesheet — the visual contract (mirrors the `--cs-*` tokens in `tokens/calm-studio.css`, expanded into component CSS). |
| `data.js` | Fake domain data (`window.CS_DATA`) incl. list visibility and the **reservation** model. |
| `components.jsx` | Shared UI: Lucide-style `Icon`, `Sidebar`, `TopBar`, `ItemCard`, `ItemRow`, `PriorityPill`. |
| `app.jsx` | Screens + routing + Tweaks: My Lists, Discover, Profile, List, Item, Item edit, Reserved, Settings, Login, Search. |
| `tweaks-panel.jsx` | Tweaks shell (accent color, density). |

## What's new vs the legacy web client

- **Navigation:** persistent left **sidebar** (My Lists · Discover · Reserved ·
  Settings, your lists pinned, profile at the bottom); opens on **My Lists**, not a
  global users list; global **search** (⌘K) over people / lists / items; breadcrumbs
  for depth.
- **Reservations surfaced:** reserve a gift on someone's list → it appears under
  **Reserved** and the sidebar count updates; the list owner sees only *that*
  something is reserved, **never who**.
- **Visuals:** one indigo accent (cascades from `--cs-accent` via `color-mix`), cool
  near-monochrome neutrals, Manrope, rounded cards with soft elevation, Lucide icons,
  grid/list views, filter + sort, toasts, modals, empty states.

## Notes

- Visual prototype: state resets on refresh; "New list" / "Copy to my wishlist" are
  demo toasts; item images are deterministic tinted placeholders (real photos slot in).
- Copy keeps the product's functional, second-person voice; Title Case actions,
  sentence-case messages, question-style confirms, no emoji.
