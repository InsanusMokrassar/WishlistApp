# STEP_3 — Follow-up: convert stylesheet to `by style` delegates + inheritance

Operator request (iteration 2): rewrite `CalmStudioStyleSheet` so every class is a root-level
`className by style { }` delegate with the Style DSL inheritance (per the JetBrains Style_Dsl tutorial),
instead of raw global selectors in `init`.

## Decision (asked operator)

`by style` changes the emitted class name. Operator chose **keep names global** →
`object CalmStudioStyleSheet : StyleSheet(usePrefix = false)`, so a delegate's property name IS its
emitted class name verbatim (`val btn` → `.btn`). The ~30 existing views + the 7 component files are
**untouched** — they keep using literal `classes("btn")` etc.

## How

- Each single-class rule is a root `val name by style { }`.
- Variants/state/descendants nest inside the base delegate (the "inheritance"):
  - descendant: raw `"svg" style { }` → `self svg` (auto-wrapped by the DSL);
  - compound: `self + className("primary") style { }` → `.btn.primary`;
  - pseudo: `self + hover` / `self + disabled` / `self + lastChild`;
  - compound+descendant: `desc(self + className("on"), className("count"))` → `.navitem.on .count`;
  - media: `media(mediaMaxWidth(760.px)) { self style { } }` inside the `detail` delegate.
- Non-class selectors stay raw `"…" style { }` in `init`: `:root` `--cs-*` tokens, element reset,
  `::selection`, grouped `.input, .textarea, select.input` (+ `:focus`, `.textarea`), hyphenated
  `.content-inner` / `.form-row`, and the `.empty` family.

## One forced exception

`.empty` is **not** a `by style` delegate: a property named `empty` clashes with
`SelectorsScope.empty` (the `:empty` pseudo-class) and the compiler demands an impossible `override`
(String delegate vs CSSSelector). So `.empty` + `.empty .ic` / `.ic svg` / `h3` / `p` are flat raw rules
in `init`. (Compiler caught this; fixed.)

## Verification

- `./gradlew :wishlist.features.common.client:compileKotlinJs` → BUILD SUCCESSFUL.
- `./gradlew :wishlist.client:compileKotlinJs` → BUILD SUCCESSFUL (full JS graph).
- Selector coverage re-checked: all 158 selectors + the media query preserved; no drops, no Bootstrap
  rules reintroduced.
- `ast-index rebuild`.
- Runtime/visual check (browser) NOT run — recommended before merge since the CSS-generation path
  changed (engine same class names, but worth a visual confirm).
