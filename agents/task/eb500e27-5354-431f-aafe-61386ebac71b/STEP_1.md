# STEP_1 Architecturing — Issue #39 Placeholder images

ENTITY: task=issue#39; type=feature; state=architected
CONTEXT: uuid=eb500e27-5354-431f-aafe-61386ebac71b; prior=STEP_0.md (read=true)

## Strategy DECISION

CHOICE = (a) Compose-drawn vector placeholders. JUSTIFICATION:
- No asset-serving infra exists; the wishlist README explicitly states "No third-party image-loader
  dependency". Bundling static assets (b) would need per-platform resource pipelines (JS static
  serving, Android drawable res, JVM classpath resource), 3x the wiring, and is inconsistent.
- Vector drawing is crisp at any size (48dp thumbnail .. 160dp big avatar), themeable, tiny.
- Cross-platform reality (verified): JS=Compose-HTML(DOM, no Canvas); JVM+Android=Compose-Foundation
  (Canvas/DrawScope). NO single composable spans both worlds. Minimal split = per-platform file.

IMPLEMENTATION PER PLATFORM:
- JS (jsMain): inline SVG delivered as a `data:image/svg+xml,<utf8-encoded markup>` URL passed to the
  existing `Img(src=...)`. Zero new DSL/dependency (avoids relying on transitive html-svg DSL import
  resolution). The SVG markup strings are authored once per entity in a shared jsMain helper object.
- JVM (jvmMain) + Android (androidMain): `androidx.compose.foundation.Canvas` + `DrawScope`
  primitives (drawRect/drawCircle/drawPath/drawRoundRect). PURE foundation API — identical body on
  both; only the file location differs (no shared jvm+android-only source set because commonMain also
  feeds js). Bodies kept near-identical; colors pulled from explicit neutral `Color` values (NOT
  MaterialTheme) so the same drawing code compiles in both material v2 and material3 source sets
  without importing either material package.

## New components (one placeholder composable per entity per platform)

Package (existing view packages):
- users:  `dev.inmo.wishlist.features.ui.users.ui`
- wishlist:`dev.inmo.wishlist.features.ui.wishlist.ui`

### USER silhouette → `UserAvatarPlaceholder`
- JS file:      features/ui/users/src/jsMain/kotlin/ui/UserAvatarPlaceholder.kt
- JVM file:     features/ui/users/src/jvmMain/kotlin/ui/UserAvatarPlaceholder.kt
- Android file: features/ui/users/src/androidMain/kotlin/ui/UserAvatarPlaceholder.kt
- Visual: neutral gray background + lighter head circle + shoulders arc (rounded torso).
- JS signature: `@Composable fun UserAvatarPlaceholder(sizePx: Int, circle: Boolean, alt: String)`
  → renders `Img(src=dataUri, alt=alt)` with width/height=sizePx and `rounded-circle`/`rounded` class.
- JVM/Android signature: `@Composable fun UserAvatarPlaceholder(modifier: Modifier, contentDescription: String?)`
  → `Canvas(modifier)`; caller supplies size+clip (matches RemoteImage call shape).

### WISHLIST ITEM gift-box → `WishlistItemImagePlaceholder`
- JS:      features/ui/wishlist/src/jsMain/kotlin/ui/WishlistItemImagePlaceholder.kt
- JVM:     features/ui/wishlist/src/jvmMain/kotlin/ui/WishlistItemImagePlaceholder.kt
- Android: features/ui/wishlist/src/androidMain/kotlin/ui/WishlistItemImagePlaceholder.kt
- Visual: present/gift box — box body rect + lid rect + vertical ribbon + bow (two small ovals/loops).
- JS signature: `@Composable fun WishlistItemImagePlaceholder(alt: String, attrs)` rendered via Img
  data-uri; reuse for card media (full width/height) and 48px leading. Provide a `sizePx:Int?`
  (null=fill width) + `cssClasses: Array<String>` param to cover both card-media and leading-box uses.
- JVM/Android: `@Composable fun WishlistItemImagePlaceholder(modifier: Modifier, contentDescription)`.

### WISHLIST stacked-items → `WishlistImagePlaceholder`
- JS / JVM / Android, same package as item.
- Visual: 3 small gift-box / card silhouettes stacked, each offset by +x/+y (back-to-front), giving
  the "several items behind each other" look. Implemented as 3 offset rounded rects (the rearmost
  fainter) — simple, reads as a stack.
- Used as NEW `leading` slot on WishlistsListView rows (48dp). JS sizePx=48 + rounded.

## Render-site edits (where placeholder replaces the empty/gray/omitted branch)

USERS (features/ui/users), all 3 platforms:
- UsersListView: replace the gray `Div`/`Box` no-avatar branch → `UserAvatarPlaceholder` (48, circle).
- UserView: replace the `avatarId?.let{}` omit → `else` branch rendering 160dp `UserAvatarPlaceholder`.
- UserEditView: same — render 160dp placeholder when `avatarId == null`.
  NOTE: convert `avatarId?.let{ img }` to `when`/`if-else` per CODING (no else-if; simple if/else ok).

WISHLIST (features/ui/wishlist), all 3 platforms:
- WishlistItemCard: replace `if (firstImage != null) { img }` (media omitted) with if/else → else
  draws `WishlistItemImagePlaceholder` at the same media height.
- UserWishlistsView JS `ItemRow`: replace gray `Div` leading → `WishlistItemImagePlaceholder` 48.
  UserWishlistsView JVM/Android `ItemRow`: replace gray `Box` leading → placeholder 48dp.
- WishlistItemView (detail) image gallery: KEEP the existing "No images" TEXT label AND additionally
  render a single `WishlistItemImagePlaceholder` (e.g. 160dp/box) so the default visual is shown.
  DECISION: in the `imageIds.isEmpty()` branch, render the placeholder image (replacing bare text) —
  issue requires the placeholder "shown as the default whenever an image is NOT set". Keep it simple:
  render placeholder box; drop or keep the muted text — KEEP text below placeholder for clarity.
- WishlistsListView (list of wishlists): rows currently `ListRow(text=...)` with NO leading. Add
  `leading = { WishlistImagePlaceholder(...) }` (48). ListRow on all 3 platforms supports `leading`
  (verified ListComponents.kt). Wishlists have no image field, so placeholder is ALWAYS shown (the
  "no image set" state is permanent for wishlists today) — satisfies the issue.

## Strings (EN+RU), added to existing objects

UsersListStrings:
- `avatarPlaceholderAlt` = "User avatar placeholder" / "Заполнитель аватара пользователя"

WishlistStrings:
- `itemImagePlaceholderAlt` = "Gift placeholder" / "Заполнитель подарка"
- `wishlistImagePlaceholderAlt` = "Wishlist placeholder" / "Заполнитель списка желаний"

Used as JS `alt` and JVM/Android `contentDescription`. JS reads `.translation()`, Android needs
`.translation(LocalResources.current)`, JVM `.translation()`.

## Conventions compliance
- KDoc on every new symbol (object/fun/val). No `else if` (use simple if/else or `when`).
- JS custom CSS: the placeholders use Bootstrap utility classes (`rounded`, `rounded-circle`) + inline
  width/height only (matching the existing avatar `Img` styling in the same views) — no NEW layout
  skeleton CSS, so no new Stylesheet object is mandated (existing WishlistItemCardStylesheet already
  covers card media sizing; placeholder Img reuses `WishlistItemCardStylesheet.media`). If any view
  needs new structural CSS a `*Stylesheet` object will be added; expectation: none needed.
- Localized strings via buildStringResource (EN + RU). 

## Build targets
`./gradlew :wishlist.features.ui.users:build :wishlist.features.ui.wishlist:build`
(common/client ListRow NOT modified → not rebuilt). One fix cycle max.

## Blockers / questions
NONE blocking. Product defaults from STEP_0 stand (adminPanel out of scope; wishlist placeholder only
on WishlistsListView, not on UserWishlistsView section headers).

RESULT: state=architected; next=STEP_2 Coding
