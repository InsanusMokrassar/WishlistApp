# STEP_0 Planning — Issue #39 Placeholder images

ENTITY: task=issue#39; type=feature; state=planned
CONTEXT: uuid=eb500e27-5354-431f-aafe-61386ebac71b; role=root/orchestrator; branch=issue/39-placeholder-images

## 1. Task restatement

Goal: render DEFAULT placeholder images whenever an entity has NO image set, on ALL three client
targets (jsMain Compose-HTML/Bootstrap, jvmMain Compose-Desktop Material v2, androidMain Material3).

Three placeholder visuals required by issue text:
- USER: gray profile silhouette (head + shoulders).
- WISHLIST ITEM: present/gift box.
- WISHLIST: several wishlist-item shapes stacked behind each other, each offset by x+y.

## 2. Verified facts (via ast-index + reads)

ENTITY=user; image=avatar FileId via `UsersModel.getAvatar(userId): FileId?` (null = unset)
- Render sites (features/ui/users):
  - UsersListView (js/jvm/android): leading 48dp circular thumbnail. CURRENT no-image branch =
    neutral gray box (JS `Div bg-secondary-subtle`; JVM `Box` onSurface alpha 0.12; Android `Box`
    surfaceVariant).
  - UserView (js/jvm/android): 160dp avatar. CURRENT no-image branch = NOTHING rendered
    (`avatarId?.let { ... }`).
  - UserEditView (js/jvm/android): 160dp avatar above the upload button. CURRENT no-image branch =
    NOTHING rendered.

ENTITY=wishlistItem; image=`item.imageIds: List<FileId>` (empty = unset)
- Render sites (features/ui/wishlist):
  - WishlistItemCard (js/jvm/android): media = first image, fixed height (180 JS / 160 jvm+android).
    CURRENT no-image branch = NOTHING (media omitted).
  - UserWishlistsView.ItemRow (js only has explicit leading; jvm/android ItemRow uses ListRow text):
    JS ItemRow leading = 48px thumbnail, no-image branch = gray Div. (jvm/android UserWishlistsView
    rows are plain `ListRow(text=...)` with NO leading image — see note 6.)
  - WishlistItemView (detail, js/jvm/android): image gallery section; no-image branch = muted
    "No images" TEXT (WishlistStrings.noImages).

ENTITY=wishlist; image=NONE. `RegisteredWishlist` (features/wishlist/common .../models/Wishlist.kt)
has id/userId/title/defaultPriceUnits only — NO image field, NO derived thumbnail anywhere.
- Render site: WishlistsListView (js/jvm/android) lists wishlists as plain `ListRow(text=title)` with
  NO leading slot today. This is THE site to add the wishlist (stacked-items) placeholder as a new
  leading thumbnail. (UserWishlistsView shows section HEADERS as plain text H6 — not a per-wishlist
  thumbnail surface; leave headers alone, primary wishlist-thumbnail surface = WishlistsListView.)

Image rendering primitives per platform:
- JS: Compose-HTML DOM. Real images via `Img(src = viewModel.imageUrl(id))`. Bootstrap classes.
  Drawing = HTML/SVG/CSS only. NO Compose-Foundation Canvas available.
- JVM (Desktop): Compose-Foundation. Real images via feature-local `RemoteImage` (Skia decode of
  bytes from `loadImageBytes`). Canvas / ImageVector available.
- Android: Compose-Foundation (Material3). Real images via feature-local `RemoteImage`
  (BitmapFactory). Canvas / ImageVector available.

Existing scope modules: features/ui/users, features/ui/wishlist. (adminPanel has parallel
UserView/WishlistView etc. but issue Known-Facts name ONLY ui/users + ui/wishlist; adminPanel OUT OF
SCOPE unless architecture step finds it trivial+required — default: out of scope.)

## 3. Key cross-platform constraint (decisive for architecture)

JS uses Compose-HTML (DOM); JVM+Android use Compose-Foundation (Canvas). The two have NO common
drawing API. Therefore a SINGLE `@Composable` cannot cover all three. Minimum shared-source split:
- ONE placeholder source for JS (HTML, ideally inline SVG via Compose-HTML `Svg`/`Path` or a small
  CSS/Bootstrap-icon construction).
- ONE placeholder source for JVM+JimboAndroid... i.e. JVM and Android (both Compose-Foundation) — BUT
  JVM uses material v2 imports and Android material3; the geometry (Canvas drawing / ImageVector) is
  platform-API-agnostic, so a Canvas/vector-based placeholder CAN be written once if placed where
  both source sets see it. Compose-Foundation `Canvas`, `Modifier`, `Path`, `Color` are common to
  jvm+android. There is no shared jvm+android-only source set though (commonMain includes js too).
  => Practical split: js placeholder file (jsMain) + jvm placeholder file (jvmMain) + android
  placeholder file (androidMain), each tiny, but jvm and android bodies near-identical Canvas code.

Decision to finalize in ARCHITECTURE step: (a) Compose-drawn vector (Canvas on jvm/android, inline
SVG on JS) vs (b) bundled static assets. Leaning (a): no asset-serving infra, no new file pipeline,
crisp at any size, matches existing "no third-party image-loader dependency" stance in wishlist
README. Risk: 3 platform files per entity = 9 small drawing functions. Mitigate by simple geometry.

## 4. Planned changes (high level, to be refined in ARCHITECTURE)

Per entity, introduce a placeholder composable rendered in the existing no-image branches:
- USER silhouette placeholder → replaces the gray box / empty branch in UsersListView, UserView,
  UserEditView (all 3 platforms). Circular crop for list; rounded for big.
- ITEM gift-box placeholder → fills the media area of WishlistItemCard, the leading box of JS
  UserWishlistsView.ItemRow, and (decide) the detail no-images area / a small box.
- WISHLIST stacked-items placeholder → NEW leading thumbnail on WishlistsListView rows (all 3
  platforms). Requires giving `ListRow` a leading slot OR rendering a custom row. Check ListRow API
  in ARCHITECTURE (features/common/client ui/components ListComponents.kt) — JS UsersListView already
  uses ListRow `leading=`, so ListRow supports leading on JS; confirm jvm/android ListRow has leading
  param (UsersListView jvm/android use it => yes).

New strings (EN+RU) for alt/contentDescription: user placeholder, item placeholder, wishlist
placeholder. Add to UsersListStrings and WishlistStrings.

KDocs on every new symbol. JS custom CSS (if any) via dedicated `object XxxStylesheet : StyleSheet()`.

## 5. Open questions / ambiguities

- Q-PRODUCT-1 (NON-blocking, default decided): Should the wishlist placeholder also appear on the
  UserWishlistsView section headers? Default: NO — headers are text-only today; primary wishlist
  thumbnail surface is WishlistsListView. Proceeding with WishlistsListView only.
- Q-PRODUCT-2 (NON-blocking, default decided): adminPanel parallel views — default OUT OF SCOPE
  (issue names only ui/users + ui/wishlist).
- No BLOCKING product decision required. No operator note conflicts (both READMEs have empty Operator
  Notes). Proceed to ARCHITECTURE.

VERIFICATION: format=plan; entities_explicit=true; blockers=none
RESULT: state=planned; next=STEP_1 Architecturing
