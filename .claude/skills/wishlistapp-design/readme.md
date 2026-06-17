# WishlistApp Design System

A design system for **WishlistApp** — a useful, self-hosted wishlist service.
Users register and log in, own wishlists, and fill them with items (title,
description, price, priority, images) so other people can see what to gift them.
Wishlists are publicly readable; you can browse other users, view their lists, and
copy items into your own. A `root` admin gets a CRUD panel over users and wishlists.

The product ships from a single **Kotlin Multiplatform** codebase to four targets —
**Web** (Compose HTML / Kotlin-JS), **Desktop** (Compose for Desktop), **Android**
(Jetpack Compose + Material 3), and a **Ktor + PostgreSQL** server.

The product ships **two distinct design languages**, both modeled here:

1. **Web** — stock **Bootstrap 5.3.8** (light theme). Tokens are `--wl-*`;
   components emit Bootstrap classes. "Bootstrap, the way WishlistApp uses it."
2. **Desktop & Android** — one shared **Compose Material 3** codebase on the
   **default M3 baseline theme** (purple `#6750A4`). Tokens are `--m3-*`; the
   Material UI kits recreate these screens.

The two surfaces share the same product, data model, copy, and the three SVG
placeholder illustrations — but nothing visual else. Pick the token namespace
(`--wl-*` vs `--m3-*`) for the surface you're designing.

> **▶ Web redesign in progress — "Calm Studio" (`--cs-*`) is the TARGET SPEC for
> the web client.** It replaces the stock-Bootstrap look with a premium, tool-like
> aesthetic (one indigo accent, cool near-monochrome neutrals, Manrope, a persistent
> left-sidebar shell) and a new navigation model (open on **My Lists**, not Users;
> first-class **Discover** + **Reserved**; global search). For NEW web work, build
> against `--cs-*` and the `ui_kits/calm-studio/` reference implementation. The
> `--wl-*` Bootstrap layer is retained only to document the current/legacy web client
> until the redesign ships. (Desktop & Android keep Material 3 for now.)

## Sources

- **GitHub:** <https://github.com/InsanusMokrassar/WishlistApp> (branch `master`).
  Read further to build better WishlistApp designs — especially:
  - `client/src/jsMain/resources/index.html` — the web shell (Bootstrap + bundle).
  - `features/ui/**/src/jsMain/kotlin/ui/*.kt` — every web screen as Compose-HTML
    emitting Bootstrap classes (auth, users, wishlist, item, scaffold, top bar, admin).
  - `features/common/client/.../ui/components/ListComponents.kt` — shared `ListRow`,
    `BackButton`, `ScreenTitle`.
  - `features/ui/wishlist/src/commonMain/kotlin/WishlistStrings.kt` — all UI copy
    (English + Russian).
  - `features/ui/**/src/androidMain/kotlin/ui/*.kt` and `.../jvmMain/kotlin/ui/*.kt`
    — the Android & Desktop screens, near-identical Compose **Material 3** code
    (`Card`, `Badge`, `Button`/`OutlinedButton`, `OutlinedTextField`, `LazyColumn`,
    `LazyVerticalGrid`).
- **Frameworks:** Bootstrap 5.3.8 — <https://getbootstrap.com> (web; loaded via CDN
  from `styles.css`; the app vendors the identical `bootstrap.min.css`). Material 3 —
  <https://m3.material.io> (Desktop & Android; default Compose baseline scheme).

---

## Content fundamentals

How WishlistApp writes copy:

- **Voice — neutral, functional, second person.** UI speaks to *you* ("My Wishlists",
  "Copy to my profile", "You have no wishlists yet. Create one first."). No marketing
  voice, no exclamation, no humor. It reads like a tool, not a brand.
- **Casing — Title Case for actions, sentence case for messages.** Buttons and titles
  use Title Case: "Add Item", "New Wishlist", "Edit Wishlist", "My Profile". Note the
  inconsistency the app actually ships: some labels are sentence-ish ("All items",
  "Copy to my wishlist") — match the source rather than "correcting" it. Body messages
  are sentence case: "No items yet", "Copy queued. It will appear in your profile shortly."
- **Confirmations are questions + consequence.** Destructive dialogs pair a question
  title with a plain-language consequence: "Delete item?" / "This item will be
  permanently removed. Continue?"; "Discard changes?" / "You have unsaved changes.
  Discard and go back?". Confirm buttons restate the verb ("Delete", "Discard").
- **Possessive titles.** Other people's screens use `{name}'s Wishlists` and
  `{name}'s wishes`.
- **Field labels are short nouns.** "Title", "Description", "Amount", "Approximate
  price", "Currency / Units", "Links", "Priority", "Weight".
- **Empty / loading / error states are one short line.** "Loading…", "No links",
  "No price", "Copy failed. Please try again."
- **Priority vocabulary:** Low / Medium / High / Custom (Custom shows a weight, e.g.
  "Custom (42)"). Note "Small" in code surfaces to the user as **"Low"**.
- **No emoji.** The UI uses none. Empty visual slots use neutral SVG placeholders,
  not emoji or icons.
- **Bilingual.** Every string ships English + Russian (`buildStringResource(...)`).
  Keep copy short enough to translate cleanly.

---

## Visual foundations — Web (Bootstrap)

- **Framework & palette.** Stock Bootstrap 5.3.8, light theme. The app does **not**
  recolor Bootstrap — `--bs-primary` (#0d6efd) is the only brand accent and it appears
  almost exclusively as the **top navbar background** (`navbar-dark bg-primary`) and on
  primary buttons/links. Everything else is Bootstrap's neutral gray ramp
  (#f8f9fa → #212529) and standard semantic states: success #198754 (create/copy),
  danger #dc3545 (destructive), secondary #6c757d (muted UI).
- **Typography.** Bootstrap's native **system-ui font stack** — no webfont is loaded,
  nothing to download. Headings are weight **500**, line-height 1.2; body is 16px /
  1.5. Screen titles use `.h3` (~28px); card titles `.h5` (20px); card subtitles and
  detail field labels are `.h6` (16px) in `.text-muted`. Prices and metadata use
  `.small` (0.875em) muted.
- **Layout.** Centered `.container` (max 1140px) with `py-3`; vertical rhythm is
  Bootstrap spacers — `mb-3` between blocks, `gap-2` between toolbar buttons, `g-3`
  grid gutters. Item grids are responsive: `row row-cols-1 row-cols-sm-2 row-cols-md-3`.
  A scaffold lays out a top bar + main slot (an optional left slot exists but the web
  client runs top+main).
- **Cards.** Bootstrap `.card` with default radius **0.375rem** and a 1px
  `#dee2e6` border (no shadow by default). Item cards: fixed **180px** media
  (`object-fit: cover`; placeholder uses `contain` on a #eef1f4 field), `.card-body`
  with title/subtitle/description, optional muted `.card-footer` for price, and the
  priority pill absolutely positioned top-right. The whole card is clickable.
- **Lists.** The workhorse layout: `.list-group` / `.list-group-item-action` rows,
  flex with `justify-content-between`, optional leading avatar/thumbnail (48px) and
  trailing actions; clickable rows get `cursor: pointer`.
- **Badges.** Priority is a `.badge.rounded-pill` in `bg-secondary-subtle` /
  `text-secondary-emphasis` — deliberately neutral (metadata, not a status alarm).
- **Buttons.** Solid for primary flows (primary = Save, success = Add Item / Copy,
  danger = Delete), outline for toolbar actions (outline-secondary = Back,
  outline-primary = Edit, outline-success = Copy). `outline-light` `btn-sm` only on
  the blue navbar. Hover/active/focus are Bootstrap defaults (slightly darker fill;
  0.25rem focus ring at `rgba(13,110,253,.25)`).
- **Modals.** Centered Bootstrap modal (`modal-dialog-centered`) over a
  `modal-backdrop fade show`. Used for login/register and destructive confirmations.
- **Alerts.** Inline, compact (`py-2`): success confirms queued copies, danger flags
  failures.
- **Forms.** `.form-control` / `.form-select` inside `mb-3` blocks with `.form-label`;
  default focus ring; `.form-text` helper text.
- **Imagery.** No photography in the chrome. The only brand illustrations are three
  flat, neutral-gray SVG placeholders (see Iconography). Uploaded item/avatar images
  are user content, cropped `cover`.
- **Radii / shadows.** Radius scale 0.25 / 0.375 / 0.5 / 1rem, pill = 50rem, circle =
  50% (avatars). Shadows exist in tokens (`sm` / `md` / `lg`) but the app leans on
  borders over elevation — cards are flat with a hairline border.
- **Motion.** Essentially none in the web client beyond Bootstrap's built-in
  transitions (button/input color/box-shadow ~0.15s, modal fade). No bespoke
  animation, bounce, or parallax. Android adds simple fade/slide transitions; the web
  UI is static.
- **Transparency / blur.** None to speak of beyond the modal backdrop scrim. No
  glassmorphism.

---

## Visual foundations — Desktop & Android (Material 3)

The Desktop (Compose for Desktop) and Android (Jetpack Compose) clients render from
**one shared Compose Material 3 codebase** on the **default M3 baseline theme** — the
app declares no custom `colorScheme`, so it inherits Material's standard baseline.
Tokens live in `tokens/material.css` as `--m3-*`.

- **Palette.** M3 baseline light scheme: primary **#6750A4** (purple), `onPrimary`
  white; tonal containers `primaryContainer` #EADDFF, `secondaryContainer` #E8DEF8
  (the priority badge), `tertiaryContainer` #FFD8E4; surfaces #FFFBFE with
  `surfaceVariant` #E7E0EC and `onSurfaceVariant` #49454F for secondary text;
  `outline` #79747E; `error` #B3261E. **No brand blue here** — the web's #0d6efd does
  not appear on the Material surfaces.
- **Top bar.** Not a colored `TopAppBar` — a plain title **Row** (8–16px padding):
  `titleLarge` breadcrumb on the left (segments joined " / "), filled auth buttons on
  the right. Same breadcrumb behavior as web, different chrome.
- **Typography.** Roboto, M3 type scale: `titleLarge` 22, `titleMedium` 16/500 (card
  title), `bodyLarge` 16 (list primary), `bodyMedium` 14 (description), `bodySmall` 12
  (subtitle/price), `labelLarge` 14/500 (button).
- **Buttons.** Material filled `Button` (primary container, **fully rounded /
  stadium** shape, `labelLarge`) for primary actions; `OutlinedButton` for secondary
  (Profile, All items, Back); Add Item is a **full-width** filled button at the
  bottom. State layers on hover (8% primary overlay), not Bootstrap's darken.
- **Cards.** Material `Card` — `surfaceContainer` fill, **12dp** corners, tonal
  elevation (soft shadow, not a hairline border like web), 160dp media, 12dp padding,
  priority `Badge` overlaid top-end (8dp).
- **Lists.** `LazyColumn` of Material rows with 4dp spacing; 48dp leading thumbnail
  clipped to an **8dp** rounded square (avatars use a circle); ripple/state-layer on
  press.
- **Badges.** Material `Badge` in `secondaryContainer` / `onSecondaryContainer` —
  same neutral-metadata intent as the web pill, different token source.
- **Forms & dialogs.** `OutlinedTextField` (4dp corners, 1px `outline`, 2px primary
  on focus) inside a Material `Dialog` (`Surface`, 16dp corners) — used for
  login/register and delete confirmations. Loading shows a `CircularProgressIndicator`.
- **Grid.** `LazyVerticalGrid(Adaptive(minSize = 160dp))` with 8dp gaps — column
  count flexes to width, so Desktop simply shows more columns than the phone.
- **Shape & motion.** M3 corner scale (4 / 8 / 12 / 16 / full); motion is Compose
  defaults plus Android's simple fade/slide screen transitions. No bespoke animation.
- **Desktop vs Android.** Identical components and theme; Desktop is the same UI in a
  resizable window (wider grids), Android is the phone form factor with a status bar +
  gesture nav. There is **no separate desktop visual language** — it is Material 3 too.

---

## Iconography

### Calm Studio (web redesign) — icon set

The redesign INTRODUCES a line-icon set (the legacy Bootstrap client shipped none).
The reference implementation draws **Lucide-style** glyphs (2px stroke, round caps/
joins, 24×24 viewBox) inline as SVG — sidebar nav (home / compass / bookmark /
settings), search, plus, share, edit, trash, external-link, check, gift, user, lock.
For production, use **Lucide** directly (<https://lucide.dev>) — it matches the
reference 1:1. Icons are monochrome and inherit `currentColor`; never multicolor,
never emoji.

---

## Iconography (current / legacy clients)
each is driven by **text
labels**, not icon buttons (Back, Edit, Add Item, Copy to my wishlist, Log out are
all words). There is **no icon font and no icon sprite** in the web codebase.

The only first-party "graphics" are three inline **SVG placeholder illustrations**,
copied here into `assets/`:

- `assets/giftbox.svg` — gift box, shown for an **item with no image**.
- `assets/stacked-items.svg` — stacked cards, shown for a **wishlist**.
- `assets/user-silhouette.svg` — head-and-shoulders, shown for a **user with no avatar**.

All three are flat, single-tone, drawn on a 100×100 viewBox over a light neutral
field (#eef1f4 / #d0d3d8) so they scale and never crop. They are also embedded as
`data:` URIs inside the `Avatar` and `ItemCard` components, so those need no network
request. **Emoji and Unicode glyphs are never used as icons.**

**If you need icons** (e.g. building a richer screen than the source ships):
substitute **Bootstrap Icons** — `https://cdn.jsdelivr.net/npm/bootstrap-icons/font/bootstrap-icons.css` —
it is the official companion set to the framework the app already uses (same design
language, regular ~1.5px stroke). ⚠️ **Flag this as an addition**: Bootstrap Icons
are *not* present in the WishlistApp repo; the real app would render text instead.

---

## Index / manifest

Root:

- `styles.css` — the single entry point consumers link (imports Bootstrap + tokens).
- `tokens/colors.css`, `tokens/typography.css`, `tokens/spacing.css` — Bootstrap/web
  custom properties (`--wl-*`). No `@font-face` (system fonts).
- `tokens/material.css` — Material 3 baseline tokens (`--m3-*`) for the Desktop &
  Android clients.
- `tokens/calm-studio.css` — **Calm Studio** redesign tokens (`--cs-*`): the target
  spec for the web client (one indigo accent + cool neutrals + Manrope + sidebar shell).
- `assets/` — `giftbox.svg`, `stacked-items.svg`, `user-silhouette.svg` (shared by
  all clients).
- `readme.md` — this guide. `SKILL.md` — Agent-Skill front matter.

The **components** (`window.WishlistApp_ef9ce8.*`) are the **web / Bootstrap**
primitives. The Material clients are recreated in the Material UI kits below (their
M3 styling lives in `ui_kits/material/m3.css`), not as bundled React components.

Components (`window.WishlistApp_ef9ce8.<Name>`):

| Group | Components |
|-------|-----------|
| `components/actions/` | `Button` |
| `components/data-display/` | `Avatar`, `Badge`, `PriorityBadge`, `ItemCard`, `ListRow` |
| `components/forms/` | `Input`, `Select` |
| `components/feedback/` | `Alert`, `Modal` |
| `components/navigation/` | `NavBar` |

Each component directory has `<Name>.jsx` + `<Name>.d.ts` + `<Name>.prompt.md`, and
one `@dsCard` HTML showcasing its states.

Foundation cards (`guidelines/`): web colors (brand / neutral / subtle), Material 3
scheme, **Calm Studio palette (redesign)**, web type (family / scale / body) +
Material 3 type scale + **Calm Studio type (redesign)**, spacing scale, radius &
shadow, brand illustrations.

UI kits (`ui_kits/`):
- `calm-studio/` — **the web REDESIGN target spec** (`--cs-*`): full interactive
  prototype — sidebar nav, global search, My Lists, Discover, profile, list (grid/
  list, filter, sort), item detail, item add/edit, **Reserved** (the surfaced
  reservation feature), settings, login, plus Tweaks (accent color, density). Files:
  `index.html`, `styles.css` (the redesign CSS — distinct from the root token entry),
  `data.js`, `components.jsx`, `app.jsx`, `tweaks-panel.jsx`. Start here for new web work.
- `web/` — interactive Bootstrap web client (legacy look; composes the bundled components).
- `android/` — Material 3 phone client (Compose recreation, in a device frame).
- `desktop/` — Material 3 Compose-for-Desktop client (same M3 surface, window frame).
- `material/` — shared Material kit internals: `m3.css`, `material-app.jsx`, and the
  `android-frame.jsx` device chrome. The Android & Desktop kits both mount
  `material-app.jsx` and reuse `web/data.js` (Desktop adds a plain inline window bar).

Implementation: `implement-calm-studio.sh` (root) — a phased script that installs this
system as a Claude Code skill in your app repo and drives the redesign build.

> Compiler-generated, do not edit: `_ds_bundle.js`, `_ds_manifest.json`,
> `_adherence.oxlintrc.json`.
