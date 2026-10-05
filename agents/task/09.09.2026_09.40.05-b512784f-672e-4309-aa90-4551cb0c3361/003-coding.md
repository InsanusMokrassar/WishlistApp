Model: GPT-5

Changed files:

- `agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/003-coding.md`
- `client/src/commonMain/kotlin/ClientPlugin.kt`
- `client/src/jsMain/kotlin/UrlNavigationConfigsRepo.kt`
- `client/src/jsMain/resources/index.html`
- `client/src/jsTest/kotlin/PasswordChangeNavigationTest.kt`
- `features/auth/README.md`
- `features/auth/client/src/commonMain/kotlin/KtorPasswordChangeFeature.kt`
- `features/auth/client/src/commonMain/kotlin/PasswordChangeFeature.kt`
- `features/auth/client/src/commonMain/kotlin/Plugin.kt`
- `features/auth/client/src/commonMain/kotlin/configurators/DefaultUrlHttpClientConfigurator.kt`
- `features/auth/client/src/commonMain/kotlin/utils/PasswordChangeTransport.kt`
- `features/auth/client/src/commonTest/kotlin/KtorPasswordChangeFeatureTest.kt`
- `features/auth/client/src/jsMain/kotlin/JSPlugin.kt`
- `features/auth/common/build.gradle`
- `features/auth/common/src/commonMain/kotlin/Constants.kt`
- `features/auth/common/src/commonMain/kotlin/models/PasswordChange.kt`
- `features/auth/common/src/commonMain/kotlin/utils/PasswordChangePasswordPolicy.kt`
- `features/auth/common/src/commonMain/kotlin/utils/PasswordChangePaths.kt`
- `features/auth/common/src/commonTest/kotlin/models/PasswordChangeContractTest.kt`
- `features/auth/server/build.gradle`
- `features/auth/server/src/commonMain/kotlin/Plugin.kt`
- `features/auth/server/src/commonMain/kotlin/ServerPasswordChangeFeature.kt`
- `features/auth/server/src/commonMain/kotlin/configurators/PasswordChangeRoutingsConfigurator.kt`
- `features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt`
- `features/auth/server/src/commonMain/kotlin/utils/PasswordChangeCredentialState.kt`
- `features/auth/server/src/commonTest/kotlin/configurators/PasswordChangeRoutingsConfiguratorTest.kt`
- `features/common/README.md`
- `features/common/server/src/jvmMain/kotlin/JVMPlugin.kt`
- `features/common/server/src/jvmMain/kotlin/utils/CallLoggingFormat.kt`
- `features/deeplinks/README.md`
- `features/deeplinks/server/build.gradle`
- `features/deeplinks/server/src/commonMain/kotlin/configurators/DeepLinksRoutingConfigurator.kt`
- `features/deeplinks/server/src/commonMain/kotlin/services/DeepLinksService.kt`
- `features/deeplinks/server/src/commonTest/kotlin/configurators/DeepLinksRoutingConfiguratorTest.kt`
- `features/deeplinks/server/src/commonTest/kotlin/services/DeepLinksServiceTest.kt`
- `features/email/README.md`
- `features/email/server/src/commonMain/kotlin/Plugin.kt`
- `features/email/server/src/commonMain/kotlin/models/EmailPasswordChange.kt`
- `features/email/server/src/commonMain/kotlin/models/EmailPasswordChangePayload.kt`
- `features/email/server/src/commonMain/kotlin/services/EmailPasswordChangeDeepLinkHandler.kt`
- `features/email/server/src/commonMain/kotlin/services/EmailPasswordChangeService.kt`
- `features/email/server/src/commonMain/kotlin/services/EmailRegistrationInviteSender.kt`
- `features/email/server/src/commonMain/kotlin/services/EmailVerificationAccountCoordinator.kt`
- `features/email/server/src/commonMain/kotlin/utils/PublicDeepLinkUrl.kt`
- `features/email/server/src/commonTest/kotlin/services/EmailPasswordChangeServiceTest.kt`
- `features/ui/users/README.md`
- `features/ui/users/src/androidMain/kotlin/AndroidPlugin.kt`
- `features/ui/users/src/androidMain/kotlin/ui/PasswordChangeView.kt`
- `features/ui/users/src/androidMain/kotlin/ui/UserEditView.kt`
- `features/ui/users/src/commonMain/kotlin/Plugin.kt`
- `features/ui/users/src/commonMain/kotlin/UsersListStrings.kt`
- `features/ui/users/src/commonMain/kotlin/ui/PasswordChangeViewConfig.kt`
- `features/ui/users/src/commonMain/kotlin/ui/PasswordChangeViewInteractor.kt`
- `features/ui/users/src/commonMain/kotlin/ui/PasswordChangeViewModel.kt`
- `features/ui/users/src/commonMain/kotlin/ui/UserEditViewModel.kt`
- `features/ui/users/src/commonMain/kotlin/ui/UsersModel.kt`
- `features/ui/users/src/commonTest/kotlin/UsersModelTest.kt`
- `features/ui/users/src/commonTest/kotlin/ui/PasswordChangeViewModelTest.kt`
- `features/ui/users/src/commonTest/kotlin/ui/UserEditTestFixtures.kt`
- `features/ui/users/src/jsMain/kotlin/JSPlugin.kt`
- `features/ui/users/src/jsMain/kotlin/ui/PasswordChangeView.kt`
- `features/ui/users/src/jsMain/kotlin/ui/UserEditView.kt`
- `features/ui/users/src/jvmMain/kotlin/JVMPlugin.kt`
- `features/ui/users/src/jvmMain/kotlin/ui/PasswordChangeView.kt`
- `features/ui/users/src/jvmMain/kotlin/ui/UserEditView.kt`

## Implementation

Implemented the email-authorized password-change path across the existing Auth, Email, deeplink, client, and users UI modules. Auth now owns serializable request/result contracts, canonical approval-route and UTF-8 BCrypt policy checks, authenticated issuance and anonymous completion routes, a server-side password-change port, a private credential-state fingerprint, and a cancellation-safe Auth write-lock commit. Completion requires the exact persisted subject, purpose, approved current email, credential fingerprint, valid expiry, and valid password. Approval consumption occurs before the password write in one non-cancellable commit region, and an already changed credential invalidates other outstanding approvals.

Email now implements the Auth port. It captures the eligible approved-email and credential snapshot under the shared email coordinator followed by Auth's write lock, mints a 15-minute UUID-backed deeplink, sends only a trusted configured-origin URL, revalidates after delivery, and cleans up only the owned link on false delivery, cancellation, or a stale post-send state. The new handler reads approvals without consuming them and redirects only to the fixed canonical client path. Generic deeplink behavior remains repeatable; the new purpose is consumed only by its completion path. Failed deeplink minting now compensates the exact allocated id.

The client transport preserves ordinary configured-server behavior for normal calls. Browser completion instead uses the issuing window origin and a request-local default-server opt-out, with Ktor's auth circuit breaker so it never refreshes, clears, or sends bearer credentials for the approval redemption. The navigation layer accepts only an exact lowercase UUID-v4/password route and replaces a successful pending route with `/password-changed`.

The users UI now has a shared password-change config, interactor, ViewModel, strings, model transport methods, and JS/JVM/Android views and factories. The owner editor exposes issuance only when SMTP is positively available and the current address is approved. Password fields remain in memory, clear after success or ViewModel destruction, and completion is independent of browser login state. The completed screen is credential-free and cannot submit again.

The server call logger now formats only HTTP method and final status. Deeplink GET, completion POST, and the browser shell apply no-store/no-referrer protections. The affected Auth, Email, deeplink, UI/users, and common READMEs describe the new bounded behavior; Operator Notes were not changed.

## Tests and checks

Added focused contract, Ktor route/transport, deeplink cleanup/header, email/Auth integration, users-model, password ViewModel, and JavaScript route-seam tests. The server integration test covers read-only redirect, single-use completion, expiry, stale email, revoked direct role, missing SMTP, failed/cancelled delivery cleanup, post-delivery credential replacement, and concurrent completion. The ViewModel test covers local policy rejection, exact immutable subject/UUID submission, terminal invalid approval, completed-state non-actionability, and destruction cleanup.

The aggregate JVM verification passed:

`./gradlew :wishlist.features.auth.common:jvmTest :wishlist.features.auth.server:jvmTest :wishlist.features.deeplinks.server:jvmTest :wishlist.features.email.server:jvmTest :wishlist.features.auth.client:jvmTest :wishlist.features.common.server:jvmTest :wishlist.features.ui.users:jvmTest :wishlist.client:jvmTest --console=plain --warning-mode=none`

Focused platform checks also passed:

- `./gradlew :wishlist.client:jsNodeTest --tests '*PasswordChangeNavigationTest' --console=plain --warning-mode=none`
- `./gradlew :wishlist.client:jsBrowserTest --tests '*PasswordChangeNavigationTest' --console=plain --warning-mode=none`
- `./gradlew :wishlist.features.auth.client:jsNodeTest --tests '*KtorPasswordChangeFeatureTest' --console=plain --warning-mode=none`
- `./gradlew :wishlist.features.ui.users:compileDebugKotlinAndroid :wishlist.client:compileDebugKotlinAndroid --console=plain --warning-mode=none`
- `./gradlew :wishlist.features.ui.users:testDebugUnitTest --console=plain --warning-mode=none`
- `git diff --check`
- `ast-index rebuild`

The Android builds emitted the existing Android Gradle Plugin compatibility warning for compile SDK 37; no task failed.

## Explicit test-seam limitation

The repository has no existing controlled Compose HTML DOM test harness in `client/src/jsTest` or `features/ui/users`, so no element-level JS DOM assertion was added for the two password inputs or owner-editor button. The browser target does execute the production JS route helper, and the actual JS view compiles as a dependency. Native typed factories and views are compiled by the JVM and Android targets. The Android local unit-test task also completed, but the module's task graph reports its common test resource bridge as `NO-SOURCE`; the shared ViewModel behavior therefore runs on JVM, with Android proof limited to the native compile and local unit-test target. Adding DOM/render assertions or device instrumentation would require selecting and wiring a test harness beyond this bounded feature change.

One correction was made during verification: the ViewModel destruction assertion now advances the coroutine scheduler after scope cancellation, matching the asynchronous completion callback rather than changing production behavior. No further Coding stage is required.
