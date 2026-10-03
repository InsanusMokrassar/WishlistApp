Model: gpt-5.6-terra (ML; Coding prioritizes ML before HL in agents/SHORTCUTS.md.)
Changed files: features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt; features/users/common/src/jvmTest/kotlin/repo/ExposedUsersRepoSqliteTest.kt; features/users/common/src/jvmTest/kotlin/repo/PostgresUsersRepoTest.kt; features/ui/users/src/commonMain/kotlin/ui/UserEditViewModel.kt; features/ui/users/src/commonTest/kotlin/ui/UserEditViewModelEmailTest.kt; features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt; features/email/server/src/commonTest/kotlin/services/EmailFeatureServiceTest.kt; agents/task/17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd/017-coding.md

## Result

Implemented only VEC-01 through VEC-03. No schema, API, configuration, README, Operator Notes, VEC-04, VEC-05, or VEC-06 changes are included.

VEC-01 now classifies `null` before non-null slot equality using the raw `ResultRow` lifecycle columns. A clear is a no-op only when raw current email, pending email, approval, and deadline are all empty. Any residual lifecycle state clears all four fields, and an active raw deadline rejects before username or lifecycle mutation. Non-null raw current/pending equality still remains a lifecycle no-op. SQLite and PostgreSQL regression tests cover guarded dedicated/generic/bulk clearing, exact expiry, complete-state reset, event silence, and batch rollback.

VEC-02 now requires `editableEmailBaseline() == submittedEmail` at both post-PUT reconciliation points. Superseding pending state preserves the exact submitted draft, publishes `EmailChanged`, suppresses saved feedback, and stops before later verification or success handling. Shared tests cover first-read enabled/disabled races, a suspended post-delivery final-read race, and an approval-skipped final-read race.

VEC-03 now uses expected-address-aware ordered classification both before SMTP and after fresh lookup. A different approved address returns `EmailChanged`; only the exact approved address returns `AlreadyApproved`; candidate priority and no-address handling remain intact. The new real invite/controlled SMTP tests verify no initial link or delivery for a wrong approved address, exact request-link cleanup with sibling preservation, and post-SMTP approved/change/sent/no-email classifications.

## Verification

- `./gradlew --no-parallel :wishlist.features.users.common:jvmTest` passed: 28 tests, 0 failures, 0 errors, 0 skipped.
- `WISHLIST_POSTGRES_TEST_JDBC_URL='jdbc:postgresql://127.0.0.1:55432/postgres?user=aleksey' ./gradlew --no-parallel --rerun-tasks :wishlist.features.users.common:postgresEmailLifecycleTest` passed: 7 tests, 0 failures, 0 errors, 0 skipped.
- `./gradlew --no-parallel :wishlist.features.email.server:jvmTest --tests '*EmailFeatureServiceTest'` passed: 18 tests, 0 failures, 0 errors, 0 skipped.
- `./gradlew --no-parallel :wishlist.features.ui.users:jvmTest --tests '*UserEditViewModelEmailTest'` passed: 70 tests, 0 failures, 0 errors, 0 skipped.
- `./gradlew --no-parallel :wishlist.features.ui.users:jsNodeTest` passed: 78 tests, 0 failures, 0 errors, 0 skipped.
- `./gradlew --no-parallel :wishlist.features.ui.users:testDebugUnitTest :wishlist.features.ui.users:compileDebugKotlinAndroid` passed: 78 Android debug unit tests, 0 failures, 0 errors, 0 skipped; Android compile passed.
- UI JVM and JS compilation ran as prerequisites of their successful test targets.
- Rebuilt `ast-index` using `XDG_CACHE_HOME=/tmp/wishlist-vec-ast`: 812 files, 49 modules.
- `git diff --check` passed.

The first full aggregate command was interrupted while Gradle was compiling dependency modules; it produced no failure assertion and was not used as evidence. Every requested focused gate above was rerun serially to a real successful exit code.

## Remaining findings

VEC-04, VEC-05, and VEC-06 remain open for later slices. This step deliberately does not strengthen database-contention observation, expand the broader acceptance/privacy matrix, or update documentation.
