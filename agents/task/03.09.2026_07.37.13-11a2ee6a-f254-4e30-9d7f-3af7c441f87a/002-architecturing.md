Model: GPT-5 (HL)
Changed files: agents/task/03.09.2026_07.37.13-11a2ee6a-f254-4e30-9d7f-3af7c441f87a/002-architecturing.md

Model rationale: The Architecture role prefers an HL model. This design crosses serialized configuration compatibility, SMTP transport security, Koin runtime typing, and cancellation-safe reverse-order compensation, so an HL model is appropriate.

# Architecturing report

## Scope and outcome

Implement findings 1, 5, and 7 only. Add a default-safe SMTP trust option, correct the port-587 sample to STARTTLS mode, and make a successfully delivered verification link compensable during later Auth failure without changing `RegistrationEmailSender.sendRegistrationEmail(RegisteredUser): Boolean`. Findings 2 and 6 remain accepted without changes. Findings 3, 4, and 8 remain explanation or recommendation only.

Every planned behavior is automatable without a live SMTP server. The design does not claim process-crash safety: an in-process rollback cannot run after process termination between SMTP acceptance and Auth finalization.

## Confirmed API and dependency facts

`RegistrationEmailSender` is a public `fun interface` in package `dev.inmo.wishlist.features.auth.server`. Its only abstract method is `suspend fun sendRegistrationEmail(user: RegisteredUser): Boolean`. The email plugin binds an `EmailRegistrationInviteSender` instance as `single<RegistrationEmailSender>`, and `AuthFeatureService` receives only that base type through `getOrNull<RegistrationEmailSender>()`.

`DeepLinksService.createDeepLink` returns the exact persisted `DeepLinkId`; `removeDeepLink(DeepLinkId)` removes that record. Both methods are public members of `dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService`.

MicroUtils `doSuspendTransaction` catches every `Throwable`, including `CancellationException`, then invokes enrolled rollback actions in reverse insertion order. `rollableBackOperation` executes its action first and synchronously appends a rollback lambda only after successful action return. A rollback lambda is a `RollbackContext<T>` receiver and obtains the triggering throwable through the receiver property named `error`; no separate cause parameter exists. `doSuspendTransaction` continues after a rollback throws, but its default `onRollbackStepError` discards that rollback exception, so each important rollback must attach cleanup failure to `RollbackContext.error` itself.

## Finding 1: default-safe SMTP trust

Append `val unsafeSsl: Boolean = false` to the public `@Serializable SmtpConfig` constructor. Appending the defaulted field preserves Kotlin source calls that omit the field. Kotlin serialization maps an absent JSON property to `false`; an explicit JSON `false` also maps to `false`; only explicit JSON `true` enables the trust override. The application `Json` still ignores unknown keys, so preceding application versions can read configuration containing the new property, subject to their existing behavior.

Extract session-property construction from the private `buildSession` method into this exact module-internal seam in package `dev.inmo.wishlist.features.email.server.services`:

```kotlin
internal fun buildSmtpSessionProperties(smtp: SmtpConfig): Properties = Properties().apply {
    setProperty("mail.smtp.host", smtp.host)
    setProperty("mail.smtp.port", smtp.port.toString())
    if (smtp.useTls) setProperty("mail.smtp.starttls.enable", "true")
    if (smtp.useSsl) setProperty("mail.smtp.ssl.enable", "true")
    if (smtp.username != null) setProperty("mail.smtp.auth", "true")
    if (smtp.unsafeSsl) setProperty("mail.smtp.ssl.trust", smtp.host)
}
```

`SmtpEmailService.buildSession` must use `buildSmtpSessionProperties(smtp)` unchanged for authenticator selection. In particular, `mail.smtp.ssl.trust` must be absent when `unsafeSsl` is omitted or false; neither `useTls` nor `useSsl` may implicitly add the property. Existing host, port, STARTTLS, implicit-SSL, and username-driven authentication behavior remains unchanged.

## Finding 5: port-587 sample mode

Change only the SMTP portion of `server/sample.config.json`: retain port `587`, retain `useTls: true`, set `useSsl: false`, and add `unsafeSsl: false`. Leave the accepted SQLite path unchanged. This represents SMTP submission with STARTTLS and normal certificate-chain validation. Port 465 remains the documented option for `useSsl: true`.

## Finding 7: exact delivered-link compensation

Add these public types beside `RegistrationEmailSender` in package `dev.inmo.wishlist.features.auth.server`:

```kotlin
fun interface RegistrationEmailDeliveryHandle {
    suspend fun rollback()
}

interface CompensableRegistrationEmailSender : RegistrationEmailSender {
    suspend fun sendRegistrationEmailWithCompensation(
        user: RegisteredUser,
    ): RegistrationEmailDeliveryHandle?
}
```

Do not change the existing `RegistrationEmailSender` declaration or method. Existing lambda implementations and classes therefore remain source-compatible. Do not add another `AuthFeatureService` constructor parameter or another Koin binding. `EmailRegistrationInviteSender` implements `CompensableRegistrationEmailSender`; the existing `single<RegistrationEmailSender>` binding remains sufficient because Auth checks the injected object's runtime type.

Make `sendRegistrationEmailWithCompensation` the owning implementation in `EmailRegistrationInviteSender`. Missing dependencies or recipient return `null`. A false SMTP result or ordinary delivery failure removes the freshly minted link and returns `null`. Cancellation keeps the current behavior: remove the freshly minted link inside `withContext(NonCancellable)`, attach a cleanup exception to the cancellation, and rethrow the same cancellation. Successful delivery returns a `RegistrationEmailDeliveryHandle` whose closure captures only the request's exact `DeepLinksService` and `DeepLinkId` and whose `rollback()` calls `removeDeepLink(deeplinkId)`. The legacy bridge is exact:

```kotlin
override suspend fun sendRegistrationEmail(user: RegisteredUser): Boolean =
    sendRegistrationEmailWithCompensation(user) != null
```

Discarding the handle on the legacy bridge is deliberate: a successful legacy call has no enclosing transaction and retains the usable link, matching current behavior.

Inside the existing `doSuspendTransaction` block in `AuthFeatureService.registerWithRequiredEmail`, replace only the delivery selection with a runtime capability branch. The compensable call must be the action of `rollableBackOperation`, so registration of its nullable result happens synchronously after the sender returns and before another suspension:

```kotlin
val delivered = try {
    when (sender) {
        is CompensableRegistrationEmailSender -> rollableBackOperation(
            rollback = {
                val handle = actionResult
                if (handle != null) {
                    try {
                        withContext(NonCancellable) {
                            handle.rollback()
                        }
                    } catch (cleanupError: Throwable) {
                        error.addSuppressed(cleanupError)
                    }
                }
            },
            action = {
                sender.sendRegistrationEmailWithCompensation(provisionalUser)
            },
        ) != null
        else -> sender.sendRegistrationEmail(provisionalUser)
    }
} catch (error: CancellationException) {
    throw error
} catch (_: Exception) {
    false
}
if (!delivered) throw RequiredEmailRegistrationRejected()
```

The nullable result intentionally enrolls a no-op rollback when delivery returns `null`; the immediately raised rejection then runs the existing provisional cleanup. A successful non-null handle is owned only by that request. No user-to-link map, singleton registry, thread-local, or other shared bookkeeping is permitted.

Enrollment order becomes provisional-user compensation first, pending-role no-op second, and delivered-link compensation third. MicroUtils reverses that order on failure: remove the exact link, run the pending no-op, then delete the user, purge password/session state, and remove roles. Link cleanup runs in `NonCancellable`. A link cleanup failure is caught and appended to the original `RollbackContext.error`, allowing subsequent user cleanup to execute. For unexpected failure or cancellation, the original throwable remains primary and carries cleanup failure as suppressed. For `RequiredEmailRegistrationRejected`, the existing result mapper continues to return `null` when cleanup succeeds and throw the first suppressed cleanup failure when cleanup fails.

Cancellation before the compensable sender returns remains owned by `EmailRegistrationInviteSender`, which already removes a minted link non-cancellably. Cancellation after successful return is owned by the enrolled handle. Successful registration executes no rollback and retains the link. Independent closures make concurrent registrations unable to overwrite one another's link ownership.

## Executable regression-test stubs

Extend `EmailConfigTest` so the fully populated fixture contains `"unsafeSsl": true` and asserts `config.smtp.unsafeSsl`; extend the omitted-fields fixture with `assertFalse(config.smtp.unsafeSsl)`. Add the following test in `features/email/server/src/commonTest/kotlin/services/SmtpSessionPropertiesTest.kt`:

```kotlin
package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.SmtpConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SmtpSessionPropertiesTest {
    @Test
    fun trustOverrideIsAbsentByDefault() {
        val properties = buildSmtpSessionProperties(
            SmtpConfig(host = "smtp.example.com", from = Email("noreply@example.com")),
        )

        assertFalse(properties.containsKey("mail.smtp.ssl.trust"))
        assertEquals("smtp.example.com", properties.getProperty("mail.smtp.host"))
        assertEquals("587", properties.getProperty("mail.smtp.port"))
        assertEquals("true", properties.getProperty("mail.smtp.starttls.enable"))
        assertFalse(properties.containsKey("mail.smtp.ssl.enable"))
    }

    @Test
    fun trustOverrideContainsOnlyConfiguredHostWhenExplicitlyUnsafe() {
        val properties = buildSmtpSessionProperties(
            SmtpConfig(
                host = "smtp.example.com",
                from = Email("noreply@example.com"),
                useTls = false,
                useSsl = true,
                unsafeSsl = true,
            ),
        )

        assertEquals("smtp.example.com", properties.getProperty("mail.smtp.ssl.trust"))
        assertFalse(properties.containsKey("mail.smtp.starttls.enable"))
        assertEquals("true", properties.getProperty("mail.smtp.ssl.enable"))
    }
}
```

Add a direct sample-file assertion to the JVM-only email-server test suite. The ancestor search keeps the test executable whether Gradle uses the repository root or the subproject as `user.dir`:

```kotlin
@Test
fun sampleSubmissionPortUsesStartTlsAndSafeCertificateValidation() {
    val sampleFile = generateSequence(java.io.File(System.getProperty("user.dir")).canonicalFile) {
        it.parentFile
    }.map {
        java.io.File(it, "server/sample.config.json")
    }.firstOrNull {
        it.isFile
    } ?: error("server/sample.config.json not found")
    val smtp = json.parseToJsonElement(sampleFile.readText()).jsonObject
        .getValue("email").jsonObject
        .getValue("smtp").jsonObject

    assertEquals(587, smtp.getValue("port").jsonPrimitive.int)
    assertTrue(smtp.getValue("useTls").jsonPrimitive.boolean)
    assertFalse(smtp.getValue("useSsl").jsonPrimitive.boolean)
    assertFalse(smtp.getValue("unsafeSsl").jsonPrimitive.boolean)
}
```

Add these reusable Auth test doubles beside the existing sender fixtures in `AuthFeatureServiceTest`:

```kotlin
private class TrackingRegistrationEmailDeliveryHandle(
    private val failure: Throwable? = null,
    private val yieldBeforeCompletion: Boolean = false,
) : RegistrationEmailDeliveryHandle {
    var rollbackCalls = 0
        private set

    override suspend fun rollback() {
        if (yieldBeforeCompletion) kotlinx.coroutines.yield()
        rollbackCalls++
        failure?.let { throw it }
    }
}

private class FakeCompensableRegistrationEmailSender(
    private val handle: RegistrationEmailDeliveryHandle?,
) : CompensableRegistrationEmailSender {
    var legacyCalls = 0
        private set
    var compensableCalls = 0
        private set

    override suspend fun sendRegistrationEmail(user: RegisteredUser): Boolean {
        legacyCalls++
        return handle != null
    }

    override suspend fun sendRegistrationEmailWithCompensation(
        user: RegisteredUser,
    ): RegistrationEmailDeliveryHandle? {
        compensableCalls++
        return handle
    }
}

private class SuspendingPasswordsRepo : PasswordsRepo,
    dev.inmo.micro_utils.repos.KeyValueRepo<UserId, Password> by MapKeyValueRepo() {
    val started = CompletableDeferred<Unit>()

    override suspend fun set(toSet: Map<UserId, Password>) {
        started.complete(Unit)
        kotlinx.coroutines.awaitCancellation()
    }
}
```

The Auth tests must cover the following exact cases. `requiredEmailRegistrationReturnsPendingAfterInviteAndPasswordStorage` should use `FakeCompensableRegistrationEmailSender`, assert `compensableCalls == 1`, `legacyCalls == 0`, and `rollbackCalls == 0`, proving success retention. `requiredEmailRegistrationPropagatesUnexpectedFinalizationFailureAfterCompensation` should use a compensable sender plus `ThrowingPasswordsRepo`; assert the original password failure remains the same thrown instance, the handle rolls back once, and user/password/roles are empty. A variant with a throwing handle must assert that the original password failure remains primary, the handle failure is its single suppressed exception, and user/role cleanup still runs. A cancellation test must use `SuspendingPasswordsRepo` and a handle with `yieldBeforeCompletion = true`; cancel after `passwordsRepo.started.await()`, assert the same cancellation category propagates, assert one completed handle rollback despite the cancellable `yield`, and assert user/password/roles are empty. Retain at least one existing `FakeRegistrationEmailSender` finalization-failure test unchanged to prove the legacy Boolean-only branch remains functional.

Extend `EmailRegistrationInviteSenderTest` with a compensable-success test that asserts the returned handle is non-null, the link remains before rollback, and `handle.rollback()` removes the link. Add a two-delivery test using one sender and two distinct users: obtain both handles, roll back only the first handle, and assert the repository retains exactly the second payload. Existing false, exception, and during-send cancellation tests remain and continue to prove pre-handle cleanup.

Extend `RegistrationCompensationIntegrationTest` with real `EmailRegistrationInviteSender`, successful `FakeEmailsService`, real in-memory `DeepLinksService`, and a password repo that throws during finalization. Assert the original finalization exception propagates and the user, password, direct roles, and delivered deeplink are all absent afterward. A successful integration variant must assert `PendingEmailVerification` and one retained deeplink. These tests require no external SMTP server.

## README updates

The Coding role should update only Architecture Notes and related model/config prose, never Operator Notes. `features/email/README.md` must document `unsafeSsl = false`, state that `true` bypasses normal certificate-chain validation for the configured host and is an explicit unsafe opt-in, show port 587 with `useTls = true` and `useSsl = false`, and describe the exact-link handle. `features/auth/README.md` must document the additive capability, the legacy Boolean fallback, reverse rollback order, suppressed cleanup failures, and the process-crash limitation. `features/deeplinks/README.md` must state that successful invite delivery transfers exact-link rollback ownership to a request-local Auth handle and that no shared ownership map exists.

## Preserved decisions and explanations

Finding 2 remains unchanged: the relative SQLite sample path is an operator-adapted self-hosting template. No persistence or writability guarantee is added.

Finding 3 is a mixed-version wire break in both directions. A new client falls back from missing `GET /auth/config` on a legacy server, but then decodes the legacy server's successful bare `AuthCredentials` body as polymorphic `RegistrationResult`; decoding fails after account creation, so credentials are not stored. An old installed client, including an old Android or JVM device or a still-running old web bundle, sends a request the new server can decode because email defaults to null. Optional-email mode then returns polymorphic `RegistrationResult` where the old client expects bare `AuthCredentials`, again failing after creation. Required-email mode rejects the legacy request before creation because no email was supplied. Finding 6's accepted Kotlin return-type break does not remove this separate HTTP compatibility problem.

Finding 4 should later traverse `ExposedSQLException`, every `SQLException.nextException`, and cause chains. Preserve PostgreSQL SQL state `23505`; for Xerial classify only `SQLiteException.resultCode` values `SQLITE_CONSTRAINT_UNIQUE` and `SQLITE_CONSTRAINT_PRIMARYKEY`, not every base `SQLITE_CONSTRAINT`. Verify with a real in-memory SQLite `ExposedUsersRepo` duplicate-username test, duplicate-non-null-email test, and negative tests for unrelated SQL state and non-unique SQLite constraint codes. No implementation belongs in this cycle.

Finding 6 remains accepted: do not restore the former two-argument return type in this cycle.

Finding 8 should later wrap `createUserOrNull` and immediate provisional-user rollback enrollment in one bounded `withContext(NonCancellable)` block inside the existing Auth write lock. This lets a committed insert finish post-create emissions/cache work and enroll compensation before parent cancellation is observed. A deterministic repository test should persist, signal, wait on a post-create gate, receive cancellation, resume, and verify cancellation propagates only after user/password/role cleanup. An exception thrown after a hidden commit but before the repository returns an ID cannot be reliably compensated through the current repository contract; full coverage would require a commit receipt or non-throwing post-commit notification. No finding-8 implementation belongs in this cycle.

## Verification commands

Run focused tests with `./gradlew :wishlist.features.email.server:jvmTest :wishlist.features.auth.server:jvmTest`, then run `./gradlew build`. Rebuild the source index with `ast-index rebuild` after Kotlin changes. Stop when selected findings 1, 5, and 7 and their documented regression cases pass; do not expand into findings 2, 3, 4, 6, or 8.

## Coding handoff

ENTITY:
entity_id=review_remediation_architecture_1_5_7; type=architecture_handoff; state=ready_for_coding

CONTEXT:

* task_id=03.09.2026_07.37.13-11a2ee6a-f254-4e30-9d7f-3af7c441f87a; agent_id=architecturing; memory_ref=[001-planning.md,002-architecturing.md]
* constraints=[implement_findings_1_5_7,leave_findings_2_6,explain_findings_3_4_8,preserve_RegistrationEmailSender_method,no_shared_link_map,no_process_crash_claim]

ACTION:

1. action=append_serialized_safe_option; target=SmtpConfig; params={field=unsafeSsl,type=Boolean,default=false,position=constructor_end}
2. action=extract_conditional_property_builder; target=buildSmtpSessionProperties; params={visibility=internal,trust_key=mail.smtp.ssl.trust,trust_condition=unsafeSsl_true,default_state=absent}
3. action=correct_sample_submission_mode; target=server/sample.config.json; params={port=587,useTls=true,useSsl=false,unsafeSsl=false,database_path=unchanged}
4. action=add_compensation_capability; target=auth_server_api; params={base_method=unchanged,capability=CompensableRegistrationEmailSender,handle=RegistrationEmailDeliveryHandle,success=non_null_handle,failure=null}
5. action=transfer_exact_link_ownership; target=EmailRegistrationInviteSender; params={handle_capture=[DeepLinksService,DeepLinkId],legacy_bridge=boolean_from_non_null,shared_map=false,success_retention=true}
6. action=enroll_reverse_order_cleanup; target=AuthFeatureService.registerWithRequiredEmail; params={dsl=rollableBackOperation,link_context=NonCancellable,cleanup_error=RollbackContext.error_suppressed,order=[link,pending_noop,user_auth_roles]}
7. action=implement_focused_regressions; target=[EmailConfigTest,SmtpSessionPropertiesTest,EmailRegistrationInviteSenderTest,AuthFeatureServiceTest,RegistrationCompensationIntegrationTest]; params={coverage=[default_safe,explicit_unsafe,sample_587,legacy_sender,success_retention,finalization_failure,cancellation,cleanup_failure,exact_handle_concurrency]}

REASON:

* condition=unconditional_SMTP_host_trust → action=explicit_unsafe_opt_in → result=normal_certificate_validation_by_default
* condition=delivered_link_without_finalization_owner → action=request_local_exact_link_handle → result=link_removed_before_account_compensation

EXPECTED RESULT:

* entity_id=smtp_certificate_policy; new_state=safe_default_with_explicit_unsafe_override; location=SmtpConfig_and_SMTP_session_properties
* entity_id=verification_link_ownership; new_state=request_local_and_transaction_compensable; location=Auth_Email_capability_boundary
* entity_id=sample_submission_transport; new_state=STARTTLS_without_implicit_SSL; location=server/sample.config.json

VERIFICATION:

* check=unsafeSsl_omitted_or_false; expected=mail.smtp.ssl.trust_absent_and_decode_value_false
* check=unsafeSsl_true; expected=mail.smtp.ssl.trust_equals_configured_host
* check=post_delivery_failure_or_cancellation; expected=exact_link_removed_and_user_password_roles_compensated
* check=successful_or_legacy_delivery; expected=successful_link_retained_and_legacy_Boolean_contract_preserved

UNCERTAINTY:

* missing=durable_registration_outbox_or_provisional_state; ambiguity=process_crash_between_SMTP_acceptance_and_finalization_remains_unhandled
* missing=authorization_for_findings_4_8; ambiguity=SQLite_classification_and_create_enrollment_cancellation_remain_recommendations

REPETITION OF RESULT:

* entity_id=review_remediation_architecture_1_5_7; stored_in=shared_memory; status=available_for_coding

COMMUNICATION:

* sender=architecturing; receiver=coding; task_id=03.09.2026_07.37.13-11a2ee6a-f254-4e30-9d7f-3af7c441f87a; message_id=8fc91d39-e512-4f95-a70c-7274d7331f31; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[task_id,review_remediation_architecture_1_5_7,smtp_certificate_policy,verification_link_ownership]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true
* causal_chain_present=true; ambiguity_detected=false; validation_scope=coding_handoff
