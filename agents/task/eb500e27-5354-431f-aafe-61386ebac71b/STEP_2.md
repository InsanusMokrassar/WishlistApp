# STEP_2 Coding — Issue #39 Placeholder images

ENTITY: task=issue#39; type=feature; state=implemented; build=PASS
CONTEXT: uuid=eb500e27-5354-431f-aafe-61386ebac71b; prior=STEP_1.md (read=true)

## Strategy implemented = (a) Compose-drawn vector placeholders
- JS: inline SVG as `data:image/svg+xml` URI rendered via existing `Img` (no html-svg DSL dependency).
- JVM+Android: `androidx.compose.foundation.Canvas` + DrawScope, neutral `Color` literals (no material
  import) so identical bodies compile under material v2 (jvm) and material3 (android).
- No static assets, no image-loader dependency (consistent with existing project stance).

## Files CREATED (9 placeholder components)
- features/ui/users/src/jsMain/kotlin/ui/UserAvatarPlaceholder.kt        (silhouette, data-URI SVG)
- features/ui/users/src/jvmMain/kotlin/ui/UserAvatarPlaceholder.kt       (Canvas)
- features/ui/users/src/androidMain/kotlin/ui/UserAvatarPlaceholder.kt   (Canvas)
- features/ui/wishlist/src/jsMain/kotlin/ui/WishlistItemImagePlaceholder.kt     (gift box, data-URI SVG)
- features/ui/wishlist/src/jvmMain/kotlin/ui/WishlistItemImagePlaceholder.kt    (Canvas)
- features/ui/wishlist/src/androidMain/kotlin/ui/WishlistItemImagePlaceholder.kt(Canvas)
- features/ui/wishlist/src/jsMain/kotlin/ui/WishlistImagePlaceholder.kt         (stacked items, SVG)
- features/ui/wishlist/src/jvmMain/kotlin/ui/WishlistImagePlaceholder.kt        (Canvas)
- features/ui/wishlist/src/androidMain/kotlin/ui/WishlistImagePlaceholder.kt    (Canvas)

## Files MODIFIED
Strings (EN+RU):
- features/ui/users/src/commonMain/kotlin/UsersListStrings.kt   (+avatarPlaceholderAlt)
- features/ui/wishlist/src/commonMain/kotlin/WishlistStrings.kt (+itemImagePlaceholderAlt,
  +wishlistImagePlaceholderAlt)

Render sites — USERS (all replace gray-box / omitted-image branch with placeholder):
- UsersListView js/jvm/android  (48 circular)
- UserView js/jvm/android        (160; converted `avatarId?.let` → if/else)
- UserEditView js/jvm/android    (160; converted `avatarId?.let` → if/else)
  (removed now-unused imports: background/Box/MaterialTheme where applicable)

Render sites — WISHLIST:
- WishlistItemCard js/jvm/android         (media else-branch → item placeholder, full width × 160/180)
- UserWishlistsView js/jvm/android ItemRow (leading else-branch → item placeholder 48; removed
  background import on jvm/android)
- WishlistItemView js/jvm/android         ("Images" empty branch → 160 item placeholder; replaced
  bare noImages text)
- WishlistsListView js/jvm/android        (NEW `leading` slot → wishlist stacked placeholder 48)

READMEs updated (## Architecture Notes):
- features/ui/users/README.md
- features/ui/wishlist/README.md

## Per-entity × per-platform result
USER silhouette:
- JS: data-URI SVG Img (rounded-circle 48 in list; rounded 160 in view/edit)
- JVM: Canvas drawRect+drawCircle+drawPath, size via Modifier
- Android: same Canvas body (material3 source set)
ITEM gift box:
- JS: data-URI SVG Img (card-img-top media class; 48 leading; 160 detail)
- JVM/Android: Canvas drawRect/drawRect/drawRect/drawPath
WISHLIST stacked items:
- JS: data-URI SVG Img (3 offset rounded rects) as ListRow leading 48
- JVM/Android: Canvas drawRoundRect ×3 offset, as ListRow leading 48
Wishlists have no image field → placeholder is permanent wishlist thumbnail.

## Build
CMD: ./gradlew :wishlist.features.ui.users:build :wishlist.features.ui.wishlist:build
RESULT: BUILD SUCCESSFUL (first attempt, no fix cycle needed). All targets compiled
(JS/JVM/Android + lint). 0 errors.

## Post
- ast-index update: done (38 changed, 1 deleted).
- KDocs on every new symbol: yes. No `else if` introduced (if/else + `when` only). JS uses Bootstrap
  utility classes + reuses existing `WishlistItemCardStylesheet.media`; no new structural CSS → no new
  Stylesheet object required.

## Blockers
NONE. adminPanel parallel views left out of scope per STEP_0/STEP_1 product defaults (issue named only
ui/users + ui/wishlist). UserWishlistsView section headers left text-only (wishlist thumbnail surface
= WishlistsListView).

RESULT: state=implemented; build=PASS
