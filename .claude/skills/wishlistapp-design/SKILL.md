---
name: wishlistapp-design
description: Use this skill to generate well-branded interfaces and assets for WishlistApp, either for production or throwaway prototypes/mocks/etc. Contains essential design guidelines, colors, type, fonts, assets, and UI kit components for prototyping.
user-invocable: true
---

Read the README.md file within this skill, and explore the other available files.
If creating visual artifacts (slides, mocks, throwaway prototypes, etc), copy assets out and create static HTML files for the user to view. If working on production code, you can copy assets and read the rules here to become an expert in designing with this brand.
If the user invokes this skill without any other guidance, ask them what they want to build or design, ask some questions, and act as an expert designer who outputs HTML artifacts _or_ production code, depending on the need.

Quick orientation:
- WishlistApp is a self-hosted wishlist service (register/login, users own wishlists of items). It ships **two design languages**: the **web** client is stock **Bootstrap 5.3.8** (tokens `--wl-*`, accent blue #0d6efd on the navbar); the **Desktop + Android** clients share one **Compose Material 3** surface on the default baseline theme (tokens `--m3-*`, purple #6750A4).
- **Web redesign (target spec): "Calm Studio"** (tokens `--cs-*`) — premium tool aesthetic, one indigo accent #5B5BD6, cool near-monochrome neutrals, Manrope, persistent left-sidebar nav (My Lists / Discover / Reserved / Settings) + global search. For NEW web work build against `--cs-*` and `ui_kits/calm-studio/` (the full interactive reference). `implement-calm-studio.sh` drives the build in the real repo.
- `styles.css` is the single CSS entry point (imports Bootstrap + the `--wl-*`, `--m3-*` and `--cs-*` token layers).
- Web components live under `components/<group>/`, exposed at `window.WishlistApp_ef9ce8.<Name>` after loading `_ds_bundle.js`. See each `<Name>.prompt.md`.
- UI kits: `ui_kits/calm-studio/` (**the web redesign target spec** — start here for new web work), `ui_kits/web/` (legacy Bootstrap), `ui_kits/android/` + `ui_kits/desktop/` (Material 3, sharing `ui_kits/material/`). Best references for composing full screens — match the kit to the surface you're targeting.
- Copy is functional and second-person; Title Case actions, sentence-case messages, question-style confirms, no emoji. See README "Content fundamentals".
