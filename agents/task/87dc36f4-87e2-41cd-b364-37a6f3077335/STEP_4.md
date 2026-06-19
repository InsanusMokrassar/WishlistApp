# STEP_4 — Follow-up: typed class references everywhere (`CalmStudioStyleSheet.<name>`)

Operator request (iteration 3): replace every raw class-name string (`classes("row")`) with the typed
`classes(CalmStudioStyleSheet.row)`. For a class that exists only nested inside another style, add an
empty root `val` token, and reference it typed both at the HTML call site and inside the owning rule.

## How

- **Stylesheet** (`CalmStudioStyleSheet`): added empty **token** delegates (`val count by style {}`, …)
  for every context-styled / modifier class, declared *before* the component delegates that use them
  (object property init order). Converted all nested class references to typed:
  `".count" style {}` → `className(count) style {}`; `self + className("primary")` →
  `self + className(primary)`. Element children (`"svg"`, `"h3"`) stay raw. The grouped input rule is now
  `group(className(input), className(textarea), type("select") + className(input)) style {}`. The
  `media()` query is imported as `cssMedia` so `val media` (the `.media` token) doesn't shadow it.
  `.content-inner` / `.form-row` became backtick `val` delegates with their styles.
- **Consumers** (42 view/component files): a scripted migration (`/tmp/migrate_classes.py`, balanced-paren
  scan, only string literals inside `classes(...)`) rewrote `classes("x")` → `classes(CalmStudioStyleSheet.x)`
  and added the import. Hyphenated/keyword names use backticks. Manual follow-ups: `CalmButton` enum
  `cssClass` values + `mutableListOf(CalmStudioStyleSheet.btn)` / `add(CalmStudioStyleSheet.block)`; both
  `tintClass` helpers now `when`-map to `CalmStudioStyleSheet.t0..t7`.

## Exceptions (cannot be typed — kept raw, documented in `agents/CODING.md`)

- `empty`, `right` — `val empty` / `val right` clash with `SelectorsScope.empty` / `:right` (compiler
  demands an impossible `override`). Stay `classes("empty")` / `classes("right")` and raw `.empty…` /
  `.right` rules.
- `rounded`, `rounded-circle`, `flex-shrink-0` — dead Bootstrap utility classes still on
  `features/ui/users/.../UserAvatarPlaceholder.kt`; not Calm Studio classes, left raw (out of scope).

## Probe

Before the bulk edit, a throwaway `ScratchProbe` confirmed which identifiers compile under
`usePrefix = false`: hyphenated backticks, keyword `` `val` ``, `desc`, `media` all OK; only `empty` and
`right` clash. Probe deleted.

## Verification

- `./gradlew :wishlist.features.common.client:compileKotlinJs` → BUILD SUCCESSFUL.
- `./gradlew :wishlist.client:compileKotlinJs` → BUILD SUCCESSFUL (full JS graph, all 42 migrated files).
- `grep` audit: the only raw class strings left inside `classes(...)` are `"empty"` (×7) and `"right"`
  (×2); the stylesheet has no raw `className("…")`; only `.empty…` / `.right` raw selectors remain.
- `ast-index rebuild`.
- Runtime/visual check (browser) still NOT run — recommended before merge.
