# STEP 0 — Plan: Calm Studio redesign, phase 5/5 (web/JS only)

## Goal
Finish + clean up the web (Kotlin/JS Compose-HTML) redesign. Empty states, toasts,
login/register modal, settings; then drop Bootstrap entirely (stylesheet + JS bundle +
all dead `btn-*`/`form-control`/`modal-*`/`list-group`/`container`/grid classes).
Do NOT touch Android/Desktop (Material 3) clients. One commit at end.

## Discovery results
- In-app `client/.../css/calm-studio.css` already defines every needed Calm class
  (`.empty .ic`, `.toast .ok`, `.scrim/.modal/.mhead/.mbody/.mfoot`, `.tabs`,
  `.form/.fieldset/.input/.textarea/.select`, `.rows/.row`, `.listgrid/.listcard`,
  `.btn primary/ghost/danger`, `.pill`, `.form-row`). CSS work = none.
- Empty states (no lists / nothing reserved) already done in phase 3/4
  (WishlistsListView, MyPresentsBooksView). List-detail empty states done in
  WishlistView. Remaining empty state: admin lists (text-muted lines).
- Core screens, item card/row/detail, edit forms + confirm modals (WishlistEditView,
  WishlistItemEditView) ALREADY migrated to Calm (scrim modals). No change.
- Genuinely still on Bootstrap classes → MUST migrate before removing Bootstrap:
  1. `features/ui/auth/.../AuthView.kt` — login/register modal (Bootstrap modal).
  2. `features/ui/users/.../UserEditView.kt` — settings/profile-edit + 2 confirm modals.
  3. `features/common/client/.../ui/components/ListComponents.kt` — ScreenTitle/
     BackButton/ListRow (h3, btn-outline-secondary, list-group) — shared, used by admin.
  4. `features/ui/topBar/.../TopBarView.kt` — `d-flex align-items-center gap-2` auth wrapper.
  5–12. `features/ui/adminPanel/.../Admin*View.kt` (8 files) — container/list-group/
     form-control/form-select/btn-*/Bootstrap modals/`row col` grid. Admin is wired +
     reachable for root, so it blocks Bootstrap removal → migrate to Calm.
- No toast infra exists anywhere. Must add.

## Work items
A. Toasts: add jsMain `Toaster` object (StateFlow bus) + `ToastHost()` composable +
   `CalmIcons.check`; mount ToastHost once in ScaffoldView; wire representative async
   actions (copy item, reserve/cancel) with new bilingual strings.
B. Login/register modal: rebuild AuthView as Calm `.scrim`/`.modal` with `.tabs`
   (Log in / Register), `.fieldset` inputs, `.mfoot` ghost+primary. Reuse AuthStrings.
C. Settings: rebuild UserEditView as `.content-inner`/`.pagehead`/`.form`/`.fieldset`/
   `.input`; convert its 2 ConfirmModals to `.scrim`. Reuse UsersListStrings.
D. ListComponents: ScreenTitle→H1 (no h3), BackButton→`.btn ghost`, ListRow→Calm
   `.row` inside `.rows` (drop list-group). Keep signatures (admin depends on them).
E. Admin panel (8 files): container→`.content-inner`+`.pagehead`; list-group→`.rows`;
   form-control/select→`.input`/`.select` in `.fieldset`; btn-*→`.btn primary/ghost/
   danger`; Bootstrap modals→`.scrim` ConfirmModal; `row col`→`.form-row`. Id chips→`.pill`.
F. TopBarView: replace `d-flex…` auth wrapper with inline flex style (or `.acts`).
G. Cleanup: remove `<link bootstrap.min.css>` + `<script bootstrap.bundle.min.js>` from
   index.html; delete vendored `css/bootstrap.min.css` + `js/bootstrap.bundle.min.js`;
   grep-verify zero Bootstrap-only classes remain in jsMain.
H. Build per touched module (`compileKotlinJs`), fix breakage. Format/lint.

## Constraints honored
- Copy voice: functional, second-person, Title Case actions, sentence-case messages,
  question confirms, no emoji. New strings EN+RU via buildStringResource.
- No Android/Desktop edits. Class names mirror skill `app.jsx`/`styles.css`.

## Risks
- `local.CODING.md` still says "JS views MUST use Bootstrap" — STALE; the 5-phase
  redesign (operator-authorized, phases 1–4 already landed) supersedes it. Will update
  `local.CODING.md` to reflect Calm Studio so it stops contradicting the codebase.
- Admin panel has no dedicated reference in the skill → extrapolate from Calm primitives.
- Toast wiring is optimistic at the view layer (matches the demo reference); fully
  success-accurate toasts need a multiplatform-safe viewModel event channel → follow-up.
