# STEP 2 — Coding report: Calm Studio phase 5/5 (web/JS only)

## Result
Web client fully on `--cs-*` Calm Studio; Bootstrap removed (stylesheet, JS bundle,
vendored files, and every dead Bootstrap class). `:wishlist.client:compileKotlinJs` and
all touched feature-module `compileKotlinJs` tasks: BUILD SUCCESSFUL. No Android/Desktop
(Material 3) files touched.

## New files
- `features/common/client/src/jsMain/.../ui/components/Toaster.kt` — `object Toaster`
  (StateFlow bus) + `@Composable ToastHost()` (`.toast`/`.toast.show`, 2600ms auto-clear).
- `features/ui/adminPanel/src/jsMain/.../ui/AdminModals.kt` — shared `DiscardModal()`
  Calm `.scrim` modal reused by the 3 admin edit screens.

## Changed — components / shell
- `CalmIcons.kt` — added `check` glyph (toast).
- `ScaffoldView.kt` — mounts `ToastHost()` once inside `.app`.
- `ListComponents.kt` — ScreenTitle→`<h1>`, BackButton→`.btn.ghost`, ListRow→`.row`
  (inside `.rows`); dropped all Bootstrap (h3 / btn-outline-secondary / list-group).
- `TopBarView.kt` — auth wrapper `d-flex…`→inline flex style.

## Changed — screens (Bootstrap → Calm)
- `AuthView.kt` — login/register rebuilt as `.scrim`/`.modal` with `.tabs`, `.fieldset`
  inputs, `.mfoot` ghost+primary; error line in danger-tinted `.hint`. Added
  `AuthViewModel.onShowLoginForm()` (additive; lets the "Log in" tab stay open).
- `UserEditView.kt` (settings) — `.content-inner`/`.pagehead`/`.form`/`.fieldset`/
  `.input`; 2 confirm dialogs → `.scrim` ConfirmModal; avatar img via inline style.
- Admin panel (8 views) — container→`.content-inner`+`.pagehead`(+`.acts`);
  list-group→`.rows`/`.row`; form-control/select→`.input`/`.select`; btn-*→
  `.btn primary/ghost/danger(/sm)`; Bootstrap modals→`DiscardModal`; `row col`→
  `.form-row`; id chips→`.pill`; empty/loading lines→`.subline`. Stale "Uses
  Bootstrap classes" docs updated.

## Toasts wired (real async + client-side)
- `WishlistView.kt` — copy result (`copyQueued`/`copyFailed`) now toasts via
  `LaunchedEffect` on the existing view-model state (real async result, not optimistic);
  Share button toasts "Link copied to clipboard".
- `BookingView.kt` — reserve / cancel toast.
- New bilingual strings: `WishlistStrings.shareLinkCopiedToast`,
  `BookingStrings.reserveToast` / `cancelReservationToast`.

## Cleanup
- `client/.../resources/index.html` — removed bootstrap css `<link>` + bundle `<script>`.
- Deleted `resources/css/bootstrap.min.css`, `resources/js/bootstrap.bundle.min.js`.
- `calm-studio.css` header comment updated (no longer "loaded after bootstrap").
- `rg` over `**/jsMain/**/*.kt` for Bootstrap-only class literals → 0 (only Calm
  `.toast.show` matches the `show` token).
- `ast-index rebuild` run after edits.

## Verification
- `./gradlew :wishlist.client:compileKotlinJs` → BUILD SUCCESSFUL.
- `./gradlew` compileKotlinJs for common.client, ui.{auth,users,topBar,booking,
  wishlist,adminPanel,scaffold} → BUILD SUCCESSFUL.
- No ktlint/detekt/spotless in the project; Kotlin compiler is the lint gate.
- NOT run: `jsBrowserDistribution` (sandbox approval friction). Risk nil — only static
  resource copy; index.html + css already validated, deleted files were unreferenced.

## Manual follow-ups (for operator)
1. `agents/local.CODING.md` still says "JS views MUST use Bootstrap classes" — now
   STALE and contradicts the shipped redesign. Left untouched (operator-owned guidance);
   operator should rewrite it for Calm Studio. Not edited unprompted.
2. Toast coverage is representative (copy, share, reserve/cancel). Login/register and
   save/delete success toasts need a multiplatform-safe view-model event channel
   (commonMain view-models can't call the jsMain `Toaster`). Deferred.
3. Admin panel had no dedicated reference in the design skill — migrated by extrapolating
   from Calm primitives; worth a design pass if admin gets first-class polish.
4. Login modal omits the reference's marketing subline ("Self-hosted wishlists, just for
   your people.") to keep the product's functional voice / avoid inventing copy.
