Model: gpt-5.6-terra (ML; Coding priority selects ML before HL).
Changed files:
- features/admin/server/src/commonMain/kotlin/configurators/AdminRoutingsConfigurator.kt
- features/admin/server/src/commonTest/kotlin/UsersManagementFeatureTest.kt
- features/admin/server/src/commonTest/kotlin/configurators/AdminRoutingsConfiguratorTest.kt
- features/auth/server/src/commonTest/kotlin/services/AuthFeatureServiceTest.kt
- features/email/client/src/commonTest/kotlin/KtorEmailFeatureTest.kt
- features/email/server/src/commonMain/kotlin/Plugin.kt
- features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt
- features/email/server/src/commonMain/kotlin/services/EmailVerificationAccountCoordinator.kt
- features/email/server/src/commonMain/kotlin/utils/EmailChangePolicy.kt
- features/email/server/src/commonTest/kotlin/EmailChangePolicyConfigTest.kt
- features/email/server/src/commonTest/kotlin/configurators/EmailRoutingsConfiguratorTest.kt
- features/email/server/src/commonTest/kotlin/services/EmailVerificationAccountCoordinatorTest.kt
- features/email/server/src/commonTest/kotlin/services/FakeUsersRepo.kt
- features/roles/server/src/commonTest/kotlin/FakeUsersRepo.kt
- agents/task/17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd/008-coding.md

# Server and client integration coding report

Completed the bounded server/client integration follow-up on top of the persisted lifecycle foundation. The root-level email cooldown setting is now validated independently of SMTP graph shape: zero remains disabled, positive sub-millisecond values round upward, negative and infinite values are rejected, and both duration conversion and startup deadline addition are checked. The coordinator now uses fresh private reads and correctly retains `null` for missing full or username-only admin mutations.

The owner route already had the typed cooldown mapping; the guarded root full-update route now returns the same authenticated HTTP 429 body without partially renaming the account. Username-only mutation remains a separate lifecycle-preserving path. The Ktor client continues to preserve its Boolean contract for ordinary non-success responses while decoding only well-formed 429 bodies into the typed exception. Focused tests cover policy conversion, typed owner/admin route responses, valid and malformed client cooldown replies, post-SMTP approved/no-email classification, retained-current verification ordering, and repository-double contract fallout.

The email server fake now observes current and pending-slot ownership, emits only durable lifecycle changes for its dedicated methods, retains approved current addresses while recording replacements as pending, and clears an address before no-op comparison. Auth, admin, and roles test doubles now implement the expanded users repository contract and preserve lifecycle fields through their dedicated mutations. Existing destructive/current-only coordinator assertions were changed to assert retained current email plus the pending verification candidate.

The private Auth/Admin projections, public Users projection, real Exposed lifecycle persistence, cache/fresh-read path, deep-link candidate selection, role retry behavior, registration compensation, and sample policy setting were already present before this step and were retained. No profile UI/renderers or README prose was changed, per the requested slice boundary.

Verification passed:

- `./gradlew --no-parallel :wishlist.features.users.common:jvmTest :wishlist.features.email.client:jvmTest :wishlist.features.email.server:jvmTest :wishlist.features.auth.common:jvmTest :wishlist.features.auth.server:jvmTest :wishlist.features.admin.common:jvmTest :wishlist.features.admin.server:jvmTest :wishlist.features.roles.server:jvmTest`
- `./gradlew --no-parallel :wishlist.features.email.client:compileKotlinJs :wishlist.features.email.client:compileDebugKotlinAndroid :wishlist.features.auth.common:compileKotlinJs :wishlist.features.admin.common:compileKotlinJs :wishlist.features.users.common:compileKotlinJs`
- `XDG_CACHE_HOME=/tmp/wishlist-email-cooldown-ast ast-index rebuild`
- `git diff --check`

One initial Android command used an ambiguous Gradle task abbreviation; rerunning with `compileDebugKotlinAndroid` passed. The external PostgreSQL lifecycle gate remains unavailable without `WISHLIST_POSTGRES_TEST_JDBC_URL`, as recorded in 007-coding; this step did not access a developer database.

This server/client integration slice is complete. The overall task is not claimed complete because profile UI/renderers, README documentation, and the external PostgreSQL execution proof remain outside this slice or blocked by the missing disposable JDBC URL.
