# STEP_1 — Architecture

## Files

### New — `features/common/client/src/jsMain/kotlin/ui/`
- `CalmStudioStyleSheet.kt` — `object CalmStudioStyleSheet : StyleSheet()`. All Calm Studio rules as raw
  global selectors; `init { … ; StyleSheetsAggregator.addStyleSheet(this) }`. `fun ensureRegistered()`
  forces object init at startup.

### New — `features/common/client/src/jsMain/kotlin/ui/components/`
- `CalmButtons.kt` — `CalmButtonVariant`, `CalmButtonSize`, `CalmButton`, `IconButton`.
- `CalmDataDisplay.kt` — `CalmPill`, `PriorityPill`, `CardBadge`, `ReservedFlag`, `ItemGrid`, `ItemCard`,
  `RowsList`, `ItemRow`, `ListCardsGrid`, `ListCard`, `NewListCard`, `PeopleGrid`, `PersonCard`.
- `CalmPage.kt` — `ContentColumn`, `PageHead`, `Subline`, `CrumbItem`, `Breadcrumb`, `Toolbar`,
  `SegmentedControl`.
- `CalmForms.kt` — `CalmForm`, `FieldSet`, `CalmTextField`, `CalmTextArea`, `FormRow`, `FormHint`,
  `PriorityOptions`.
- `CalmDetail.kt` — `DetailLayout`, `DetailMedia`, `DetailField` (×2), `PriceTag`, `LinkRow`, `ActionBar`.
- `CalmFeedback.kt` — `EmptyState`, `CalmModal`, `ModalHeader`, `ModalBody`, `ModalFooter`, `ModalTabs`,
  `ConfirmModal`.

### Edited
- `CalmIcons.kt` — added `home`, `settings`, `search`, `list`, `back`, `user`, `lock` (full design set).
- `client/src/jsMain/kotlin/ClientJSPlugin.kt` — `CalmStudioStyleSheet.ensureRegistered()` before
  `renderComposable`.
- `client/src/jsMain/resources/index.html` — removed `<link href="css/calm-studio.css">` (Manrope kept).
- `agents/CODING.md` — new "Design System Rule (web — Calm Studio)" section.
- `features/common/README.md` — Architecture Notes: design-system bullet; corrected stale "Bootstrap".
- `features/ui/scaffold/.../ScaffoldView.kt`, `features/ui/sidebar/.../SidebarView.kt` — fixed comments
  that referenced the deleted `.css` path → `CalmStudioStyleSheet`.

### Deleted
- `client/src/jsMain/resources/css/calm-studio.css` (the `css/` dir is now empty/gone).

## Registration / load path

`CalmStudioStyleSheet.init` → `StyleSheetsAggregator.addStyleSheet(this)`. The object is referenced
nowhere else (views use literal class strings), so `ClientJSPlugin.startPlugin` calls
`CalmStudioStyleSheet.ensureRegistered()` once, before `renderComposable("content")`, which already runs
`StyleSheetsAggregator.draw()`. The aggregator's `StateFlow` re-emits, so styles render on first frame.

## Component design

Components take already-translated strings + primitive props / `@Composable` slots only — no feature
domain types — so they stay reusable across all UI features. Each mirrors the reference markup and emits
the same Calm Studio class names the stylesheet defines. Control flow uses `when`/binary `if` (no
`else if`), per CODING.md.
