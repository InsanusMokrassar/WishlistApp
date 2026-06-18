# Task: Calm Studio CSS → Compose StyleSheet + component library

UUID: 87dc36f4-87e2-41cd-b364-37a6f3077335
Date: 2026-06-18
Role: coding (single-session: plan + architecture + coding inline)

## Request

1. Replace `client/src/jsMain/resources/css/calm-studio.css` with a Compose `StyleSheet` in
   `features/common/client/src/jsMain/`, carrying all its styles.
2. Create a `@Composable` in the same module for each component in the Calm Studio design.
3. The created `StyleSheet` must self-register via `StyleSheetsAggregator.addStyleSheet(this)` in its
   `init` block.
4. Add the governing rule to `agents/CODING.md`: future web design changes use a StyleSheet (never CSS
   files) + `@Composable` components.

## Plan

- **P0 — Investigate.** Confirm `StyleSheetsAggregator` API (`dev.inmo.micro_utils.coroutines.compose`),
  the Compose `StyleSheet` DSL (raw selectors, `property`, `media`), the in-use CSS, the design source,
  and the consumer surface (every `classes("…")` call site).
- **P1 — Port the stylesheet.** 1:1 port of the in-use CSS as raw global selectors so the class-name
  contract is preserved and no view needs rewriting. Self-register in `init`.
- **P2 — Load trigger.** Force object init once at JS startup (object is otherwise unreferenced).
- **P3 — Remove CSS.** Delete the `.css` file + its `<link>` in `index.html`.
- **P4 — Component library.** One `@Composable` per Calm Studio design component.
- **P5 — Rule + docs.** Add the design-system rule to `agents/CODING.md`; update `features/common/README.md`.
- **P6 — Verify.** Compile JS (module + full client graph), check port fidelity, rebuild ast-index.

## Key decisions

- **Global raw selectors, not scoped `by style`.** The design ships ~300 literal `classes("…")` call
  sites across ~30 views. Raw global selectors (`".btn" style { … }`) keep every class name verbatim, so
  the port is behavior-identical and zero-churn. Scoped names would have forced a 300-site rewrite.
- **Pure `property(name, value)`** for every declaration → mechanical, exact transcription (gradients,
  `color-mix`, `--cs-*` vars, `-webkit-*`, `backdrop-filter`, the `760px` media query all port verbatim).
- **"Claude design components" = the Calm Studio kit** in `local.new_design/ui_kits/calm-studio/`
  (`components.jsx` + `app.jsx`), NOT the stale Bootstrap `local.new_design/components/*.jsx`.
- **Did not migrate existing views** to the new components — out of scope ("create components", not
  "migrate"). Global selectors keep all views working; migration is a follow-up.
