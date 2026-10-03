Model: gpt-5.6-luna (LL Coding documentation role)
Changed files: features/email/README.md; features/users/README.md; features/auth/README.md; features/admin/README.md; features/ui/users/README.md; features/email/server/src/commonMain/kotlin/configurators/EmailRoutingsConfigurator.kt; features/users/common/src/commonMain/kotlin/models/User.kt; features/users/common/src/commonMain/kotlin/models/UsersFeatureUser.kt; features/users/common/src/commonMain/kotlin/repo/ReadUsersRepo.kt; features/users/common/src/commonMain/kotlin/repo/WriteUsersRepo.kt; features/users/common/src/commonMain/kotlin/repo/CacheUsersRepo.kt; features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt; features/auth/common/src/commonMain/kotlin/models/AuthFeatureUser.kt; features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt; features/admin/common/src/commonMain/kotlin/models/AdminUser.kt; features/admin/client/src/commonMain/kotlin/UsersManagementFeature.kt; features/admin/server/src/commonMain/kotlin/UsersManagementFeature.kt; features/ui/users/src/commonMain/kotlin/Plugin.kt; features/ui/users/src/commonMain/kotlin/ui/UsersModel.kt; features/ui/users/src/commonMain/kotlin/ui/DefaultUsersModel.kt; features/ui/users/src/commonMain/kotlin/ui/UserEditViewModel.kt; agents/task/18.09.2026_07.24.45-9d85e0a2-9584-4775-bbfd-2774f2d30a33/007-coding.md

# Coding: final ownership documentation

Updated only feature README content and KDocs required by architecture 002. No behavior, test,
schema, configuration, or dependency changes were made. `features/ui/adminPanel/README.md` was
reviewed and left unchanged because it contains no stale claim that admin models expose pending
email state.

The email README now defines `EmailProfile` as the owner of current/approval, pending, accepted
request-time, and approval-rooted cooldown state. It documents the authenticated owner GET with
200/404/401 privacy behavior, SMTP-independent reads and storage, pending-first delivery, exact
recipient sending, fresh coordinator reads, legacy null request times, and additive rollout,
matching-version deployment, asset invalidation, and rollback guidance. Requested-at is explicitly
separate from the approval deadline and has its own nullable storage column.

The users README now contracts `RegisteredUser` to identity/current-email/approval fields and
`UsersFeatureUser` to public id/username. It documents the mandatory cache-bypassing
`getEmailProfileFresh` projection, the users-row physical storage retained for atomicity, accepted
timestamp transitions, pending-only committed events despite reduced-model equality, and additive
no-backfill migration behavior. Auth and admin READMEs describe reduced `AuthFeatureUser` and
`AdminUser` models, EmailFeature-owned editing/read state, coordinator-enforced admin mutations,
and preservation of authorized typed 429 responses without pending state in admin responses. The
users UI README now describes `UsersModel.getMyEmailProfile`/`EmailFeature.getMyEmail`, auth `meState`
as identity/authorization only, the five-field feedback snapshot including requested-at, privacy and
reconciliation guards, and the absence of a timestamp control.

KDocs were aligned for the email route count, reduced user/auth/admin models and reverse mappers,
fresh repository reads and cache bypass, Exposed accepted-write timestamp semantics, auth identity
reads, admin management contracts, and the users MVVM email boundary. Existing EmailProfile,
EmailProfile helper, EmailFeature, KtorEmailFeature, coordinator, sender, and UI snapshot KDocs
were checked and already described the new API precisely; no unrelated wording was changed. The
final docs correct the earlier checkpoint shorthand that conflated pending/cooldown storage with
the distinct requested-at column.

Verification:

- `./gradlew --no-parallel :wishlist.features.email.server:compileKotlinJvm :wishlist.features.users.common:compileKotlinJvm :wishlist.features.auth.common:compileKotlinJvm :wishlist.features.auth.server:compileKotlinJvm :wishlist.features.admin.common:compileKotlinJvm :wishlist.features.admin.client:compileKotlinJvm :wishlist.features.admin.server:compileKotlinJvm :wishlist.features.ui.users:compileKotlinJvm` passed. Existing Exposed deprecation and redundant Json-format warnings remain; no compilation errors occurred.
- `XDG_CACHE_HOME=/tmp/wishlist-email-docs-ast ast-index rebuild` passed after Kotlin comment changes and indexed 815 files.
- README semantic assertions passed for EmailProfile ownership, owner GET outcomes/privacy, fresh projection/cache bypass, user/auth/admin model contraction, UI EmailFeature sourcing, five-field feedback, and requested-at/deadline distinction.
- Operator Notes sections for email, users, auth, admin, UI users, and UI admin panel have identical SHA-256 hashes to both pre-change `HEAD` and `origin/master`.
- `git diff --check` passed.

No PostgreSQL or test suite was rerun because this checkpoint changes only documentation and KDocs;
the preceding source checkpoints recorded their required behavioral evidence. No blocker remains.
No push was performed.
