Model: gpt-5.6-luna (LL; documentation/KDoc priority)
Changed files:
- features/users/README.md
- features/email/README.md
- features/auth/README.md
- features/admin/README.md
- features/ui/users/README.md
- features/users/common/src/commonMain/kotlin/repo/ReadUsersRepo.kt
- features/users/common/src/commonMain/kotlin/repo/WriteUsersRepo.kt
- agents/task/17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd/010-coding.md

# Documentation and KDoc report

Updated the five affected feature READMEs to describe the implemented email lifecycle rather than the earlier single-address plan. Users documentation now specifies retained approved current versus pending replacement, first-unapproved compatibility, additive migration fields, raw cross-slot uniqueness, durable singleton-lock participation, post-commit event publication, fresh private reads, exact/idempotent approval, serialization throughput, unsupported mixed old/new writers, and PostgreSQL as a required test gate rather than a product claim.

Email documentation now specifies root-level emailChangeCooldown, omission/zero disabled semantics, the illustrative P1D sample, SMTP independence, persisted approval deadlines, typed authenticated 429 responses, unchanged duplicate 409, pending-candidate recipients, stale/idempotent links, and compensation/role/database boundaries. Auth, Admin, and UI Users documentation now records private lifecycle projections, fresh owner reads, lifecycle-preserving username updates, full-update cooldown behavior, root guard/privacy boundaries, three-way current/pending/draft state, Refresh and expiry, disabled Save/IME admission, typed versus uncertain failures, candidate resend, feedback snapshots, and cross-platform test scope.

Audited changed public Kotlin symbols with ast-index. Existing lifecycle model, policy, exception, mapper, and candidate utility KDocs were valid and current. Added missing interface/lookup KDocs for ReadUsersRepo and parameter/return KDocs for WriteUsersRepo.setEmail and updateUsername; behavior was unchanged.

Verification:

- XDG_CACHE_HOME=/tmp/wishlist-email-cooldown-ast ast-index rebuild — passed; 812 files indexed.
- ./gradlew --no-parallel :wishlist.features.users.common:jvmTest :wishlist.features.email.server:compileKotlinJvm :wishlist.features.auth.server:compileKotlinJvm :wishlist.features.admin.server:compileKotlinJvm :wishlist.features.ui.users:compileKotlinJvm :wishlist.features.ui.users:compileKotlinJs :wishlist.features.ui.users:compileDebugKotlinAndroid — passed.
- git diff --check — passed.
- Operator Notes sections were preserved byte-for-byte; server/sample.config.json was not changed.

Remaining gaps:

- Disposable PostgreSQL execution remains pending because WISHLIST_POSTGRES_TEST_JDBC_URL is unavailable; documentation records the condition as a test requirement, not a product guarantee.
- No new KDoc gaps were found in the audited task symbols.
