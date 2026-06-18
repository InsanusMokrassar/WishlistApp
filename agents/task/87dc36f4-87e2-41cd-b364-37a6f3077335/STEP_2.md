# STEP_2 — Coding results & verification

## Done

All deliverables from STEP_0 implemented per STEP_1.

- Ported every rule of the in-use `calm-studio.css` into `CalmStudioStyleSheet` — 159 selector blocks,
  246 `property()` declarations, the `:root` `--cs-*` token block, the `t0..t7` tints, and the single
  `@media (max-width: 760px)` override. Self-registers via `StyleSheetsAggregator.addStyleSheet(this)`.
- Created 6 component files + extended `CalmIcons` covering the full Calm Studio component inventory.
- Removed the `.css` file and its `<link>`; wired `ensureRegistered()` at JS startup.
- Added the design-system rule to `agents/CODING.md`; updated `features/common/README.md`.

## Verification

- `./gradlew :wishlist.features.common.client:compileKotlinJs` → BUILD SUCCESSFUL (23s). Stylesheet +
  all components compile.
- `./gradlew :wishlist.client:compileKotlinJs` → BUILD SUCCESSFUL (18s). Full JS graph (ClientJSPlugin
  wiring + every UI feature view + the scaffold/sidebar comment edits) compiles.
- Port fidelity cross-check: every selector from the original CSS is present. **Only** the two dead
  Bootstrap-compat rules (`.topbar .btn-outline-light` and its `:hover`) were intentionally dropped —
  no view emits `btn-outline-light` (auth widget was restyled to Calm Studio; Bootstrap removed in phase
  5). Noted here as the single deliberate deviation.
- `ast-index rebuild` → 610 files indexed.

## Not done (out of scope / follow-up)

- **Existing views still use literal `classes("…")`** rather than the new components. The global-selector
  port keeps them working unchanged. Migrating ~30 views (~300 call sites) to the components is a
  separate, larger task and was not requested ("create components", not "migrate usages").
- Number-typed inputs: `CalmTextField` covers string-valued input types only; numeric fields (price /
  amount) still use a raw `Input` + `attr("type","number")` as in `WishlistItemEditView`.

## Notes for the operator

- One deliberate inline `style { }` remains in `ItemRow` (the h3+pill flex cluster) — it mirrors the
  reference's own inline styling and matches existing accepted usage (e.g. `TopBarView`). Everything in
  the design skeleton otherwise lives in `CalmStudioStyleSheet`.
- `agents/local.CODING.md` ("JS UI MUST use Bootstrap") is stale and now explicitly flagged obsolete in
  `agents/CODING.md`.
