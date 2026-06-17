# STEP 1 — Architecture

## New code
- `features/common/client/src/jsMain/.../ui/components/Toaster.kt`
  - `object Toaster { val message: StateFlow<String?>; fun show(text); fun clear() }`
    — jsMain-only singleton bus (JS is single-threaded; no Koin needed).
  - `@Composable fun ToastHost()` — observes `Toaster.message`, renders
    `Div(classes "toast"[+"show"]) { Span(.ok){CalmIcon(check)}; Text(msg) }`,
    auto-clears after 2600ms via `LaunchedEffect(msg){ delay(2600); Toaster.clear() }`.
  - Add `CalmIcons.check = """<path d="M20 6 9 17l-5-5"/>"""`.

## Mount point
- `ScaffoldView.onDraw`: inside the `.app` Div, after `.main`, call `ToastHost()`
  (sibling — `.toast` is `position:fixed`, so DOM position is cosmetic). Scaffold
  module already depends on common.client.

## Toast wiring (optimistic, view layer)
- `BookingView`: onBook → `Toaster.show(reserveToast)`, onCancelBooking →
  `Toaster.show(cancelReservationToast)`. New `BookingStrings` keys (EN+RU).
- `WishlistItemView`: copy button → `Toaster.show(copyItemToast)`. New `WishlistStrings`
  key (EN+RU) — or reuse existing copy-queued string if present.

## Shared ListComponents (keep public signatures)
- `ScreenTitle(text, …)` → `H1 { Text(text) }` (no Bootstrap h3/margins; vararg kept
  but classes only applied if non-empty to avoid emitting Bootstrap utils). Simplest:
  keep params, emit `H1({ if(extraClasses.isNotEmpty()) classes(*extraClasses) })`.
- `BackButton(text,onClick)` → `Button(classes "btn","ghost"){ CalmIcon? + Text }`.
- `ListRow(...)` → `Div(classes "row"){ leading?; Div(.rmain){content}; trailing? }`.
  Callers wrap rows in `Div(classes "rows")` instead of `Ul(list-group)`.

## Per-view conversions (Bootstrap → Calm)
| Bootstrap | Calm |
|-----------|------|
| `container py-3/py-4` | `content-inner` |
| screen title row | `pagehead` > Div > H1 (+ `.acts` for buttons) |
| `list-group` `Ul` | `rows` `Div` |
| `form-control` / `form-select` | `input` / `select` (inside `.fieldset` + `Label`) |
| `btn btn-primary` | `btn primary` |
| `btn btn-secondary`/`btn-outline-secondary` | `btn ghost` (cancel/back) |
| `btn btn-danger`/`btn-outline-danger` | `btn danger` |
| `btn-sm` | `btn` + `sm` (calm has `.btn.sm`) |
| Bootstrap modal (backdrop+dialog) | `.scrim` > `.modal` > `.mhead`(H2+P)/`.mfoot` |
| `badge bg-secondary` id chip | `pill` |
| `row` + `col` grid (admin item edit) | `form-row` |
| `text-muted` empty line | `subline` (P) |

## AuthView (login/register modal)
- Logged-in: `Button(.btn ghost){ logout }`.
- Logged-out trigger: `Div(style flex/gap){ Button(.btn){Log in}; Button(.btn){Register} }`.
- Expanded: `.scrim` > `.modal` containing `.mhead`(H2 title) + `.mbody` with
  `.tabs`(Log in / Register, `on` for active mode, switch via onToggle*Form) +
  `.fieldset` username + `.fieldset` password + error `.hint`(danger) + `.mfoot`
  (ghost Cancel + primary submit). Reuse AuthStrings (no invented marketing copy).

## Cleanup / verify
- index.html: drop bootstrap css link + bootstrap.bundle script.
- delete vendored `css/bootstrap.min.css`, `js/bootstrap.bundle.min.js`.
- `rg` for residual Bootstrap-only classes in jsMain → must be empty.
- `./gradlew` compileKotlinJs for: common.client, ui.auth, ui.users, ui.topBar,
  ui.booking, ui.wishlist, ui.adminPanel, ui.scaffold (+ client assemble).

## Out of scope / flagged
- `agents/local.CODING.md` ("JS MUST use Bootstrap") is now stale — leave file, flag to
  operator (do not edit operator's local guidance unprompted).
