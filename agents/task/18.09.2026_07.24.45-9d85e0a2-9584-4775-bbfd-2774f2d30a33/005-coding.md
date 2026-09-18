Model: Codex GPT-5 (Coding implementation); gpt-5.6-luna (required LL step-report writer).
Changed files: features/ui/sidebar/src/commonTest/kotlin/ui/SidebarModelTest.kt; features/ui/users/src/commonMain/kotlin/Plugin.kt; features/ui/users/src/commonMain/kotlin/ui/DefaultUsersModel.kt; features/ui/users/src/commonMain/kotlin/ui/UserEditViewModel.kt; features/ui/users/src/commonMain/kotlin/ui/UsersModel.kt; features/ui/users/src/commonTest/kotlin/UsersModelTest.kt; features/ui/users/src/commonTest/kotlin/ui/UserEditTestFixtures.kt; features/ui/users/src/commonTest/kotlin/ui/UserEditViewModelEmailTest.kt; features/ui/users/src/commonTest/kotlin/ui/UserEditViewModelSaveTest.kt; features/ui/users/src/jvmTest/kotlin/ui/UserEditEmailRenderTest.kt.

# Coding: Email-owned profile-edit MVVM boundary

Implemented architecture checkpoint 4 only. The temporary lifecycle fields on RegisteredUser, AuthFeatureUser, and AdminUser remain unchanged. No persistence, email-service behavior, final model contraction, or README/documentation checkpoint was included.

UsersModel now exposes getMyEmailProfile(): EmailProfile?. DefaultUsersModel delegates that surface solely to EmailFeature.getMyEmail(). Its constructor and production Koin binding no longer depend on ClientAuthFeature, while the independent AuthFeatureUser meState flow remains the source for caller identity, credentials, authorization, and role-driven capabilities.

The owner-email ViewModel now carries EmailProfile rather than AuthFeatureUser for every private email state, mutation guard, helper, reconciliation result, feedback publisher, and checked owner comparison. Owner matching uses EmailProfile.userId == UserId.long. The email-owned emailDraftBaseline and verificationCandidate helpers now supply pending-first draft and delivery candidates. Existing caller, session, target, generation, request-version, mutation-token, cancellation, and Main.immediate behavior is unchanged.

EmailFeedbackSnapshot now includes emailChangeRequestedAt at each construction site. A requested-at-only authoritative refresh therefore retires a positive saved or delivery result without rendering the timestamp. New tests prove that delivery feedback retires while a raw dirty draft remains unchanged and that save feedback retires after an otherwise identical requested-at refresh.

All UsersModel fakes, the sidebar consumer fake, DI proof, editor fixtures, save tests, and desktop renderer tests use EmailProfile for private email state. The fixture preserves an approved current address while storing a replacement as pendingEmail with a fresh request time. Existing tests retain exact enabled PUT/GET/POST/GET, approval-skipped PUT/GET/GET, disabled PUT/GET, resend POST/GET, typed cooldown, raw-draft, exact-expiry, captured-IME, cancellation, and reconciliation assertions. The owner/privacy suite proves anonymous, non-owner, and root editing another user make zero capability probes and zero owner email GET calls, with no exposed private owner state; the live renderer target gate remains covered.

Verification completed:

- `./gradlew --no-parallel :wishlist.features.ui.users:jvmTest` passed. XML evidence: 100 tests, zero failures/errors.
- `./gradlew --no-parallel :wishlist.features.ui.users:jsNodeTest` passed. XML evidence: 84 tests, zero failures/errors.
- `./gradlew --no-parallel :wishlist.features.ui.users:jsBrowserTest` passed. XML evidence: 84 tests, zero failures/errors.
- `./gradlew --no-parallel :wishlist.features.ui.users:testDebugUnitTest` passed. XML evidence: 84 tests, zero failures/errors.
- `./gradlew --no-parallel :wishlist.features.ui.users:compileKotlinJvm :wishlist.features.ui.users:compileKotlinJs :wishlist.features.ui.users:compileDebugKotlinAndroid` passed after the final source edit.
- `./gradlew --no-parallel :wishlist.features.ui.adminPanel:jvmTest :wishlist.features.ui.sidebar:jvmTest :wishlist.features.ui.auth:jvmTest` passed.
- `XDG_CACHE_HOME=/tmp/wishlist-email-mvvm-ast ast-index rebuild` completed after the final source edit, indexing 816 files. `ast-index search getMyProfile` returns no references; `ast-index refs ClientAuthFeature` contains no UI-users use; `ast-index refs getMyEmailProfile` finds only the intended model, editor, fixture, sidebar fake, and delegation test paths.
- `git diff --check` passed before report creation and will be repeated for the final staged diff.

No PostgreSQL run was required because this checkpoint changes neither persistence nor service behavior. Gradle retains unrelated deprecated publication/configuration warnings; no test or compilation failure remains.

No source blocker remains. The next checkpoint should remove the temporary lifecycle fields and helpers from RegisteredUser, AuthFeatureUser, AdminUser, and mappings, update the final README surfaces through the required LL workflow, then perform the task-wide validation. No push was performed.
