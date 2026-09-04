Model: gpt-5.6-sol
Changed files: agents/task/10.08.2026_09.08.17-c0ecd3ab-3385-4a7f-ab41-649fb5f51d24/009-architecturing.md

## Model rationale

The Architecture preference list names `fable`, `opus`, and `sonnet`, but none is available in the current Codex session. The inherited `gpt-5.6-sol` model is the strongest available frontier agentic coding model and is appropriate for the required concurrency, lock-order, and Koin-graph analysis. The report records the model actually used.

## Evidence and bounded scope

I read `008-planning.md` in full and re-read the current Email, Users, and Roles READMEs before tracing their current source and tests. The current serial address comparison is correct but not atomic: `EmailVerificationDeepLinkHandler` reads and accepts address A before entering `promoteNewUserToUser`, while both `EmailFeatureService` and `DisabledEmailFeature` can independently update the same user row to B through `updateStoredEmail` during that gap.

This cycle changes only the email-server concurrency boundary, its constructor wiring, deterministic tests, the Email README, and the local review status. It does not change the Users or Roles production APIs, repository schemas, route responses, registration flow, deeplink serialization, post-approval editing policy, admin mutation policy, or link lifecycle.

## Coordinator contract

Add `EmailVerificationAccountCoordinator` in `features/email/server/src/commonMain/kotlin/services/EmailVerificationAccountCoordinator.kt`. It is a server-only class with one private process-local `Mutex`, the existing `UsersRepo`, and the existing `RolesRepo`:

```kotlin
class EmailVerificationAccountCoordinator(
    private val usersRepo: UsersRepo,
    private val rolesRepo: RolesRepo,
) {
    private val mutex = Mutex()

    suspend fun updateStoredEmail(userId: UserId, email: Email?): Boolean

    suspend fun verifyInvitedEmailAndPromote(
        userId: UserId,
        invitedEmail: Email?,
    ): Boolean
}
```

`updateStoredEmail` holds `mutex` across `usersRepo.getById(userId)`, preservation of the current username, and `usersRepo.update(userId, NewUser(username, email))`. It returns `false` for a missing user, returns whether the update produced a record, and deliberately does not catch `DuplicateUserFieldException` or other repository failures. The existing HTTP boundary therefore continues mapping a duplicate email to `409`.

`verifyInvitedEmailAndPromote` holds the same `mutex` across the nullable legacy-email check, current-user lookup, exact invited/current email equality check, and `promoteNewUserToUser(rolesRepo, userId)`. It returns `false` without touching roles for a null legacy address, missing user, cleared address, or mismatch. A match promotes and returns `true`; repeated matching opens remain idempotent through the existing Roles transition.

The handler retains only the `Any` type check because that concern belongs to the deeplink boundary. Its implementation becomes a cast followed by one coordinator call. The coordinator, rather than the handler, owns every state-dependent verification step after the cast.

Delete `UpdateStoredEmail.kt` instead of retaining a second helper. Both email-feature implementations call the coordinator directly, leaving one source of truth for the critical section and no raw `UsersRepo` mutation path in the Email service layer.

## Constructor and Koin wiring changes

The production constructors become:

```kotlin
EmailFeatureService(
    emailsService: EmailsService,
    accountCoordinator: EmailVerificationAccountCoordinator,
    rolesFeature: RolesFeature,
)

DisabledEmailFeature(
    accountCoordinator: EmailVerificationAccountCoordinator,
)

EmailVerificationDeepLinkHandler(
    accountCoordinator: EmailVerificationAccountCoordinator,
)
```

`EmailFeatureService` keeps `RolesFeature` only for the unrelated `sendTestEmail` authorization check. Neither email-feature implementation retains `UsersRepo`, and the handler retains neither `ReadUsersRepo` nor `RolesRepo`.

`Plugin.setupDI` registers the coordinator unconditionally and exactly once:

```kotlin
single {
    EmailVerificationAccountCoordinator(
        usersRepo = get<UsersRepo>(),
        rolesRepo = get<RolesRepo>(),
    )
}
```

The unconditional placement is required because email storage and the deeplink handler exist whether SMTP is configured or not. The conditional `single<EmailFeature>` resolves `get<EmailVerificationAccountCoordinator>()` and passes that object to whichever one of `EmailFeatureService` or `DisabledEmailFeature` is selected. The qualified `DeepLinkHandler` definition resolves the same typed singleton. No production definition may call the coordinator constructor directly except the single coordinator definition.

A Koin application contains only one email-feature realization at a time, so “shared by all three consumers” means the handler and enabled service share the singleton in an SMTP-enabled graph, while the handler and disabled service share the singleton in an SMTP-disabled graph. Two graph tests cover both configurations. `getAll<EmailVerificationAccountCoordinator>()` must contain exactly one element, and repeated typed `get` calls must be referentially identical.

## Linearization behavior

When self-service update to B obtains the coordinator first, the update reads A, commits B, and releases the coordinator before verification can read. Verification then observes B against invited A, returns `false`, does not call role promotion, and leaves exactly `NewUserRole`. This is the stale-link outcome.

When verification obtains the coordinator first, verification reads A, confirms invited A, and completes the full `NewUserRole` to `UserRole` transition before releasing the coordinator. The update to B waits. After approval completes, the update acquires the coordinator and stores B. The final state of `UserRole` plus B is valid in this ordering because A was still current at the promotion linearization point; this preserves the documented ability of an approved user to edit an address without a new verification flow.

An update that writes A again is harmless in either order. An update that clears the address before verification makes the link stale. Wrong payload type, null legacy address, missing user, and serial mismatch continue returning `false`. A duplicate update continues throwing `DuplicateUserFieldException`, and Kotlin `Mutex.withLock` releases the coordinator on every return, exception, or cancellation.

The coordinator serializes all self-service email mutations and email-verification promotions process-wide, including operations for different users. A keyed-lock registry would add lifecycle and cleanup complexity without serving this low-volume path, so one mutex is the minimal correct design.

## Lock ordering and deadlock proof

Let C be the new coordinator mutex, U be transient Users repository/cache/database locks, R be the existing Roles transition mutex, and P be transient Roles repository/cache/database locks.

The self-service update order is C then U, with U released when `UsersRepo.update` returns and C released immediately afterward. The verification order is C then U for the completed lookup, followed by C then R then P for promotion. Verification does not retain U while waiting for R.

The existing delayed-role callback order is R then U for the existence check and then R then P for the generic grant. Roles code never requests C. Email update never requests R. No Users or Roles repository call in the current graph calls back into the coordinator; `ast-index` shows no application consumers of `UsersRepo.updatedObjectsFlow`, `RolesRepo.roleIncluded`, or `RolesRepo.roleExcluded`. Other admin/auth user operations acquire U only and never wait for C or R.

Therefore no cycle exists. A Roles callback may hold R while briefly waiting for U, and verification may hold C while waiting for R, but no participant holds U while waiting for C, and the verification lookup has already released U before requesting R. The chain C to R to U cannot form because verification never requests U after acquiring R. Repository change-flow consumers must not introduce coordinator re-entry while a repository emission is completing; no such consumer exists in the current implementation.

## Concrete file map

Production changes are limited to the new coordinator, `Plugin.kt`, `EmailFeatureService.kt`, `DisabledEmailFeature.kt`, `EmailVerificationDeepLinkHandler.kt`, deletion of `UpdateStoredEmail.kt`, and KDoc/import adjustments directly caused by those constructors. No Users, Roles, Auth, DeepLinks, route, model, Gradle, or configuration production file changes are planned.

Test construction changes are required in `EmailFeatureServiceTest.kt`, `DisabledEmailFeatureTest.kt`, `EmailVerificationDeepLinkHandlerTest.kt`, and `EmailDeepLinkIntegrationTest.kt`. Add a focused coordinator concurrency test and extend `PluginTest.kt` or add a dedicated Koin graph test. A test-only blocking `RolesRepo` wrapper may delegate to the existing `FakeRolesRepo`; production hooks are prohibited.

## Deterministic test specifications

### Coordinator serial contract

With one seeded user, `updateStoredEmail` must store a non-null address, clear it with null, preserve username, return `false` for a missing id, and propagate `DuplicateUserFieldException` unchanged. After a duplicate exception, a subsequent coordinator operation must complete, proving the mutex was released.

With a pending seeded user, `verifyInvitedEmailAndPromote` must return `true` and leave exactly `UserRole` for a matching address, including a repeated matching call. Null legacy email, missing user, cleared current email, and mismatched current email must each return `false`, leave `NewUserRole`, and never include `UserRole`. The handler's existing wrong-type test remains and must prove no coordinator state changes.

### Forced verification-first ordering

Use `runTest`, the test scheduler, and `CompletableDeferred` barriers only. Build one `FakeUsersRepo` storing A, one pending role repository wrapper, one coordinator, one real handler, and one real email-feature implementation using that same coordinator. The role wrapper signals `promotionEntered` when `excludeDirect(NewUserRole)` is reached and suspends on `releasePromotion`. Reaching that point proves that verification holds C and already accepted A.

Start handler processing for payload A and await `promotionEntered`. Start `setMyEmail(B)` and call `runCurrent()`. Before releasing promotion, assert that the update job is incomplete and the repository still stores A. Complete `releasePromotion`. The role wrapper records the current repository email when `includeDirect(UserRole)` runs; that value must be A. Await a `true` handler result, then await a successful update. Final state must be stored B with exactly `UserRole`, and the recorded order must be promotion with A before update completion. No sleeps, wall-clock timeouts, or unconstrained dispatchers are permitted.

Run this behavioral test once through `DisabledEmailFeature` and once through `EmailFeatureService`. The enabled variant uses a fake `EmailsService` and `RolesFeature`; neither unrelated dependency participates in the critical section.

### Forced update-first ordering

Using the same fixtures without releasing any timing race, await `setMyEmail(B)` completely before invoking the handler for payload A. The handler must return `false`; the repository must store B; direct roles must remain exactly `NewUserRole`; `UserRole` must be absent; and the promotion barrier must remain incomplete because the coordinator never called `promoteNewUserToUser`.

### Koin singleton graph validation

Build two isolated Koin applications from the real `Plugin.setupDI`: one root config without an `email` block and one with a valid SMTP block. Supply in-memory `UsersRepo`, blocking `RolesRepo`, `RolesFeature`, and `Json` dependencies. In each graph, assert that `getAll<EmailVerificationAccountCoordinator>().single()` is referentially identical to repeated typed `get` calls.

In the SMTP-disabled graph, resolve `EmailFeature` as `DisabledEmailFeature` and the qualified verification handler from `getAll<DeepLinkHandler>()`; force the verification-first barrier and prove the feature update waits. In the SMTP-enabled graph, resolve `EmailFeature` as `EmailFeatureService` and repeat the same behavioral identity proof. These tests catch both a duplicate Koin definition and accidental direct coordinator construction in any consumer. Update `PluginTest` and `Plugin` KDoc to remove the obsolete statement that no Koin harness covers feature selection.

### Existing regression preservation

Adapt existing service and handler fixtures to construct a coordinator from their existing fake repositories. Keep all SMTP-enabled and disabled storage tests, duplicate propagation, send-test authorization, payload serialization, legacy decoding, deeplink dispatch, matching, mismatch, cleared, missing-user, wrong-type, and repeated-open assertions. No existing route test or status expectation changes.

All planned behavior is deterministically automatable. No operator decision or untestable functionality blocks Coding.

## Exact `local.review.74.md` correction

Keep the review target hashes, Request Changes decision, all original PR-head evidence, every other finding, and severity unchanged. Replace the complete address-verification finding with the following text after implementation and successful tests:

```markdown
### High — A verification link approves an address that never received the link

Evidence: registration issues credentials at `features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt:144`; authenticated callers may change their stored address at `features/email/server/src/commonMain/kotlin/configurators/EmailRoutingsConfigurator.kt:64-76`; `features/email/server/src/commonMain/kotlin/models/EmailVerificationPayload.kt:12-14` stores only `userId`; and `features/email/server/src/commonMain/kotlin/services/EmailVerificationDeepLinkHandler.kt:32-36` checks only that the user exists.

A registrant can request a link for address A, use the immediately returned bearer token to replace the account email with unverified address B, then open the link delivered to A. The reviewed handler promotes the account while B is stored.

Required change: bind new payloads to the invited email and serialize self-service email mutation with current-email equality checking plus role promotion through one shared coordinator. An update linearized before verification must make the old link stale; an update linearized after completed promotion must remain allowed by the established post-approval editing policy. Legacy user-id-only payloads must decode safely but fail closed.

Local fix status: fixed. One email-server coordinator now serializes both SMTP-enabled and SMTP-disabled self-service mutation with equality-check-plus-promotion. Deterministic `CompletableDeferred` tests force both legal orders: update-before-verification leaves the account pending, while verification-before-update records address A at the `UserRole` grant and permits B only afterward. Serial matching, mismatch, cleared-address, missing-user, legacy-payload, wrong-type, and repeated-open cases remain covered.
```

After all focused tests and the full build pass, replace `## Focused verification completed locally` with:

```markdown
## Focused verification completed locally

- Auth common/client/server, Roles server, Email server, and Common server JVM tests pass together.
- Email server coordinator tests force both update-before-verification and verification-before-update orderings without timing, and real Koin graph tests prove one shared coordinator in both SMTP-enabled and SMTP-disabled configurations.
- The full Gradle build, checked-in JSON validation, workflow/static checks, and `git diff --check` pass; `local.review.74.md` remains ignored and untracked.
```

## Exact Email README corrections

Do not change Operator Notes, Overview policy, or route rows. Add or replace the affected model rows with exactly this text:

```markdown
| `EmailVerificationAccountCoordinator` | `email/server` | Unconditional server singleton owning `UsersRepo`, `RolesRepo`, and one process-local mutex. Serializes self-service stored-email mutation with invited-email equality checking plus pending-to-approved role promotion. |
| `EmailFeatureService` | `email/server` | SMTP-enabled `EmailFeature` implementation; wraps a non-nullable `EmailsService`, the shared `EmailVerificationAccountCoordinator`, and `RolesFeature`. Test sending remains SuperAdmin-only; `setMyEmail` uses the shared coordinator. |
| `DisabledEmailFeature` | `email/server` | SMTP-disabled `EmailFeature` implementation; sending remains disabled while `setMyEmail` uses the same shared coordinator, so storage behavior and verification atomicity do not depend on SMTP configuration. |
| `EmailVerificationDeepLinkHandler` | `email/server` | Deeplink handler that delegates nullable invited-email validation, current-user lookup, exact email comparison, and role promotion to the shared coordinator; wrong payload types remain unhandled. |
```

Replace the Storage vs sending, Duplicate email, DI placement, and Registration invites Architecture Notes bullets with exactly these paragraphs:

```markdown
- **Storage vs sending:** Per-user email storage (`PUT /email/myEmail`) is independent of SMTP and works through both `EmailFeatureService` and `DisabledEmailFeature`. Both implementations receive the same Koin-singleton `EmailVerificationAccountCoordinator`; no email service updates `UsersRepo` directly. The coordinator is also used by verification, making self-service mutation atomic with current-email equality checking plus role promotion.
- **Duplicate email → 409:** `EmailVerificationAccountCoordinator.updateStoredEmail` deliberately propagates `DuplicateUserFieldException` from `UsersRepo.update`. `EmailFeatureService` and `DisabledEmailFeature` do not catch it; `PUT /email/myEmail` catches it only at the HTTP boundary and responds `409 Conflict`. Coordinator serialization changes no status or duplicate-field disclosure behavior.
- **DI placement:** `EmailVerificationAccountCoordinator` is registered unconditionally with `single` and owns the shared `UsersRepo`/`RolesRepo` concurrency boundary. The conditional `EmailFeature` definition passes that exact singleton to either `EmailFeatureService` or `DisabledEmailFeature`, and the qualified `EmailVerificationDeepLinkHandler` receives the same singleton. Thus SMTP-enabled and SMTP-disabled graphs each contain exactly one coordinator shared by mutation and verification.
- **Registration invites (issue #73):** Every new payload stores the pending user id and exact recipient email; legacy null-email payloads remain decodable but fail closed. Self-service email mutation and equality-check-plus-promotion share one coordinator mutex. If mutation to B linearizes first, verification of A observes the mismatch and leaves `NewUserRole`; if verification linearizes first, promotion completes while A is current and a waiting post-approval edit to B may then succeed. Wrong payload type, missing user, cleared email, and mismatch remain unhandled. This preserves post-approval editing while preventing B from being written between a successful A comparison and promotion. Invite URL validation and false/error/cancellation cleanup remain unchanged.
```

Update the existing DI and service KDoc to match these statements and remove every claim that the old `updateStoredEmail(usersRepo, ...)` helper remains the shared path.

## Verification handoff

Coding must rebuild `ast-index`, run the Email server focused tests including both Koin graph shapes and both forced orderings, then run `./gradlew build` and `git diff --check`. Verification must inspect production constructor references and confirm that the only production `EmailVerificationAccountCoordinator(...)` call is the Koin `single` definition. Verification must also confirm `local.review.74.md` is ignored, untracked, and contains the exact atomic status only after tests pass.

## AML-HIP handoff

ENTITY:
entity_id=email_verification_account_coordinator; type=email_server_singleton; state=architecture_complete
entity_id=email_update_verification_atomicity; type=high_concurrency_finding; state=ready_for_coding
entity_id=email_koin_graph; type=dependency_injection_contract; state=specified
entity_id=email_atomicity_tests; type=deterministic_regression_contract; state=specified

CONTEXT:

* task_id=10.08.2026_09.08.17-c0ecd3ab-3385-4a7f-ab41-649fb5f51d24; agent_id=architecturing-009; memory_ref=[008-planning.md,email_README,users_README,roles_README,current_source,current_tests,local.review.74.md]
* constraints=[edit_only_009-architecturing.md,no_product_changes,no_push,scope=self_service_email_update_vs_verification,post_approval_edit_preserved]

ACTION:

1. action=create_singleton; target=email_verification_account_coordinator; params={dependencies=[UsersRepo,RolesRepo],lock=Mutex,scope=Koin_application,definitions=1}
2. action=serialize_mutation; target=email_update_verification_atomicity; params={method=updateStoredEmail,critical_section=[user_read,user_update],duplicate_exception=propagate}
3. action=serialize_approval; target=email_update_verification_atomicity; params={method=verifyInvitedEmailAndPromote,critical_section=[legacy_check,user_read,email_equality,role_promotion],serial_failures=return_false}
4. action=inject_shared_instance; target=email_koin_graph; params={enabled_consumer=EmailFeatureService,disabled_consumer=DisabledEmailFeature,always_consumer=EmailVerificationDeepLinkHandler,direct_consumer_construction=false}
5. action=force_verification_first; target=email_atomicity_tests; params={barriers=[promotionEntered,releasePromotion],expected=[update_blocked,email_at_UserRole=A,final_email=B,final_role=UserRole]}
6. action=force_update_first; target=email_atomicity_tests; params={sequence=[update_B_complete,verify_A],expected=[handled_false,email_B,role_NewUser,promotion_not_entered]}
7. action=correct_review; target=local.review.74.md; params={decision=Request_Changes,severity=High,head_evidence=preserved,atomic_fix_status=deterministically_tested}
8. action=correct_documentation; target=features/email/README.md; params={singleton_identity=documented,linearization_orders=documented,operator_notes=unchanged}

REASON:

* condition=email_update_can_run_between_email_equality_and_role_promotion → action=shared_coordinator_mutex → result=linearizable_update_and_approval; requirement=invited_email_current_at_promotion
* condition=SMTP_graph_shape_selects_two_EmailFeature_realizations → action=unconditional_singleton_plus_constructor_injection → result=identical_atomicity_with_or_without_SMTP; requirement=one_coordinator_per_Koin_application
* condition=verification_first → action=hold_coordinator_through_role_promotion → result=post_approval_update_only; requirement=email_A_at_UserRole_grant
* condition=update_first → action=hold_coordinator_through_user_update → result=stale_link_rejected; requirement=NewUserRole_preserved
* condition=verification_order_C_then_U_release_then_R_and_roles_callback_order_R_then_U → action=prohibit_U_retention_before_R_and_prohibit_C_reentry → result=no_lock_cycle; requirement=deadlock_free_current_graph

EXPECTED RESULT:

* entity_id=email_verification_account_coordinator; new_state=one_shared_production_instance; location=email_server_Koin_graph
* entity_id=email_update_verification_atomicity; new_state=linearizable; location=email_server_services
* entity_id=email_atomicity_tests; new_state=both_orders_forced_without_timing; location=email_server_commonTest
* entity_id=review_and_email_docs; new_state=atomic_fix_described_accurately; location=local.review.74.md+features/email/README.md

VERIFICATION:

* check=Koin_singleton_count; expected={enabled_graph=1,disabled_graph=1,repeated_get_identity=true}
* check=verification_first; expected={update_incomplete_at_promotion=true,email_at_UserRole=A,event_order=[promotion_A,update_B],final=[UserRole,B]}
* check=update_first; expected={handled=false,email=B,direct_roles=[NewUserRole],promotionEntered=false}
* check=serial_fail_closed; expected={wrong_type=false,legacy_null=false,missing_user=false,cleared=false,mismatch=false}
* check=duplicate_update; expected={exception=DuplicateUserFieldException,coordinator_lock_released=true,HTTP_status=409}
* check=lock_graph; expected={orders=[C_to_U,C_to_R_to_P,R_to_U,R_to_P],cycle=false}

UNCERTAINTY:

* missing=none; ambiguity=none

REPETITION OF RESULT:

* entity_id=email_verification_account_coordinator; stored_in=009-architecturing.md; status=ready_for_coding
* entity_id=email_update_verification_atomicity; stored_in=009-architecturing.md; status=High_fix_contract_complete

COMMUNICATION:

* sender=architecturing-009; receiver=coding-next; task_id=10.08.2026_09.08.17-c0ecd3ab-3385-4a7f-ab41-649fb5f51d24; message_id=33e52a88-b7df-43d3-b52d-b38a7f0f0ee5; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[task_id,entity_id,coordinator,linearization,Koin,lock_order,test]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
