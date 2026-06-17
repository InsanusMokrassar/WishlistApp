# WishlistApp — Material kit internals (shared)

Shared building blocks for the **Android** and **Desktop** UI kits, which recreate
the WishlistApp Compose **Material 3** clients (one shared Compose codebase in the
source). Both clients look identical — same components, same default M3 baseline
theme — so they share everything here and differ only in their device chrome.

| File | Role |
|------|------|
| `m3.css` | Material 3 component styles (cards, buttons, badges, list rows, outlined text fields, dialogs) built on the `--m3-*` tokens. Link `../../styles.css` first. |
| `material-app.jsx` | The whole Material app (`window.MaterialApp`): nav stack + M3 screens. Reuses `../web/data.js`. |
| `android-frame.jsx` | Android device bezel (status bar + gesture nav). Used by `../android/`. |

The entry points are `../android/index.html` and `../desktop/index.html`. The Android
kit wraps `<MaterialApp/>` in `android-frame.jsx`; the Desktop kit wraps it in a plain
window chrome (defined inline in `../desktop/index.html` — the Compose for Desktop app
has no sidebar, so a minimal title-bar window is the faithful frame).

Faithful to the source: plain title-row top bar (not a colored app bar), filled
primary buttons with stadium shape, `surfaceContainer` cards with 12dp corners and
tonal elevation, `secondaryContainer` priority badges, `OutlinedTextField` dialogs,
and an adaptive 160dp grid. The only color accent is the M3 baseline purple
`#6750A4` — there is no Bootstrap blue on these surfaces.
