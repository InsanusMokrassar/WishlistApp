Model: gpt-5.6-sol
Changed files: features/email/server/src/commonMain/kotlin/services/EmailVerificationAccountCoordinator.kt, features/email/server/src/commonMain/kotlin/services/UpdateStoredEmail.kt (deleted), features/email/server/src/commonMain/kotlin/Plugin.kt, features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt, features/email/server/src/commonMain/kotlin/services/DisabledEmailFeature.kt, features/email/server/src/commonMain/kotlin/services/EmailVerificationDeepLinkHandler.kt, features/email/server/src/commonTest/kotlin/services/EmailVerificationAccountCoordinatorTest.kt, features/email/server/src/commonTest/kotlin/services/EmailFeatureServiceTest.kt, features/email/server/src/commonTest/kotlin/services/DisabledEmailFeatureTest.kt, features/email/server/src/commonTest/kotlin/services/EmailVerificationDeepLinkHandlerTest.kt, features/email/server/src/commonTest/kotlin/services/EmailDeepLinkIntegrationTest.kt, features/email/server/src/commonTest/kotlin/PluginTest.kt, features/email/README.md, local.review.74.md (ignored local review), agents/task/10.08.2026_09.08.17-c0ecd3ab-3385-4a7f-ab41-649fb5f51d24/010-coding.md

## Model rationale

The Coding preference list names `sonnet`, `opus`, and `fable`, and KDoc filling prefers `haiku`, but none of those models is available in this Codex session. The inherited `gpt-5.6-sol` model was used for the concurrency implementation, deterministic tests, KDocs, feature documentation, and coding report. No optional coding-pattern file matched this change: the cycle adds neither a route, a CRUD repository, bearer authentication, UI, nor client storage, so only the Coding hard rules applied.

## Coding result

The email verification race is closed by one new `EmailVerificationAccountCoordinator`. Its process-local mutex covers the complete self-service read-and-update operation and the complete nullable invited-email check, current-user read, exact comparison, and pending-to-approved role transition. Duplicate and other repository failures are not caught, preserving the existing HTTP-boundary `409` behavior and releasing the mutex through `withLock` on failure or cancellation.

`EmailFeatureService` and `DisabledEmailFeature` now delegate every self-service email mutation to the coordinator. `EmailVerificationDeepLinkHandler` retains only the deeplink payload type boundary and delegates every account-state-dependent step to the same coordinator. The old raw `UpdateStoredEmail.kt` helper is deleted.

`Plugin.setupDI` registers exactly one unconditional coordinator singleton from `UsersRepo` and `RolesRepo`. The conditional enabled/disabled `EmailFeature` definition and the qualified verification handler both resolve that typed singleton. Source-index inspection shows that the Koin definition is the only production constructor call; service and handler production references are constructor injection only.

The new deterministic suite uses `CompletableDeferred` barriers without sleeps or unconstrained dispatchers. Verification-first tests pause the real role transition after address A has matched, prove both enabled and disabled feature updates to B remain incomplete, record A at the `UserRole` grant, release promotion, and then observe the permitted post-approval update to B. The update-first test completes B before verification and proves the stale A link is rejected without entering promotion, leaving exactly `NewUserRole`. Additional tests preserve serial update/clear/missing behavior, duplicate propagation plus lock release, legacy fail-closed behavior, matching idempotency, and wrong-payload role stability.

Two isolated applications constructed from the real Email `Plugin.setupDI` cover SMTP-enabled and SMTP-disabled graph shapes. Each graph contains exactly one coordinator, repeated typed resolutions are referentially identical, the expected `EmailFeature` realization is selected, and handler-versus-feature behavior proves the critical section is shared rather than merely registered.

The Email README contains the architecture-specified model rows and storage, duplicate, DI, and registration-invite notes without changing Operator Notes, Overview policy, or route rows. `local.review.74.md` retains its original PR-head evidence, Request Changes decision, and severities while its address-verification fix status and verification summary now describe the atomic coordinator and forced-ordering coverage. The review remains ignored by `local.*` and is not staged.

## Verification performed

The focused command completed successfully:

```text
./gradlew :wishlist.features.email.server:jvmTest
```

The focused run reported `BUILD SUCCESSFUL in 26s`; the final test results contain 55 passed tests and no skipped, failed, or errored tests. `EmailVerificationAccountCoordinatorTest` contributes eight passing tests, including both forced orderings and both real Koin graph shapes.

The complete repository command also passed:

```text
./gradlew build
```

The full build reported `BUILD SUCCESSFUL in 1m 13s` with 4,284 actionable tasks: 193 executed and 4,091 up to date. The changed Email test compilation and suite executed successfully within that build. Webpack reported only its existing bundle-size warnings.

`ast-index rebuild` completed after the Kotlin changes and indexed 1,378 files across 114 modules. `jq empty server/dev.config.json server/sample.config.json` passed, the master-only Docker deploy and all-push Build workflows remain intact, and `git diff --check` passed.

## Structured handoff

```text
ENTITY:
entity_id=email_verification_account_coordinator; type=email_server_singleton; state=implemented_and_verified
entity_id=email_update_verification_atomicity; type=concurrency_contract; state=linearizable

CONTEXT:
* task_id=10.08.2026_09.08.17-c0ecd3ab-3385-4a7f-ab41-649fb5f51d24; agent_id=/root/pr74_coding; memory_ref=[009-architecturing.md,010-coding.md]
* constraints=[one_process_mutex,one_Koin_definition,no_route_change,no_schema_change,local_review_uncommitted,no_push]; branch=fix/issue-73-email-support

ACTION:
1. action=create_coordinator; target=email_verification_account_coordinator; params={dependencies:[UsersRepo,RolesRepo],lock:Mutex,scope:process_local}
2. action=serialize_mutation; target=EmailVerificationAccountCoordinator.updateStoredEmail; params={critical_section:[user_read,user_update],duplicate_exception:propagate}
3. action=serialize_approval; target=EmailVerificationAccountCoordinator.verifyInvitedEmailAndPromote; params={critical_section:[nullable_email_check,user_read,equality_check,role_promotion],mismatch_result:false}
4. action=inject_singleton; target=Plugin.setupDI; params={definitions:1,consumers:[EmailFeatureService,DisabledEmailFeature,EmailVerificationDeepLinkHandler],SMTP_shapes:[enabled,disabled]}
5. action=force_orderings; target=EmailVerificationAccountCoordinatorTest; params={barriers:[promotionEntered,releasePromotion],orders:[verification_first,update_first],timing_dependencies:0}

REASON:
* condition=mutation_B_can_interleave_between_comparison_A_and_promotion → action=shared_mutex_for_both_operations → result=invited_email_current_at_promotion; requirement=linearizable_approval
* condition=SMTP_configuration_selects_feature_realization → action=unconditional_singleton_injection → result=identical_atomicity_across_graph_shapes; requirement=one_coordinator_per_Koin_application
* condition=repository_update_throws_duplicate → action=allow_exception_propagation_through_withLock → result=HTTP_boundary_retains_409_and_mutex_releases; requirement=serial_behavior_preserved

EXPECTED RESULT:
* entity_id=email_update_verification_atomicity; new_state=verification_first_promotes_A_then_allows_B; location=email_server_services
* entity_id=email_update_verification_atomicity; new_state=update_first_rejects_stale_A; location=email_server_services
* entity_id=email_verification_account_coordinator; new_state=single_instance_in_both_SMTP_shapes; location=email_server_Koin_graph

VERIFICATION:
* check=email_server_jvm_tests; expected=55_pass_0_fail; actual=55_pass_0_fail
* check=forced_verification_first; expected={update_blocked:true,email_at_UserRole:A,final:[UserRole,B]}; actual={update_blocked:true,email_at_UserRole:A,final:[UserRole,B]}
* check=forced_update_first; expected={handled:false,email:B,roles:[NewUserRole],promotion_entered:false}; actual={handled:false,email:B,roles:[NewUserRole],promotion_entered:false}
* check=Koin_graph_identity; expected={enabled_count:1,disabled_count:1,repeated_get_same:true,shared_behavior:true}; actual={enabled_count:1,disabled_count:1,repeated_get_same:true,shared_behavior:true}
* check=full_Gradle_build; expected=BUILD_SUCCESSFUL; actual=BUILD_SUCCESSFUL_1m13s_4284_tasks
* check=index_JSON_workflows_diff; expected=[rebuilt,valid,intact,clean]; actual=[rebuilt,valid,intact,clean]

UNCERTAINTY:
* missing=none; ambiguity=none; deferred_work=independent_Verification_review

REPETITION OF RESULT:
* entity_id=email_verification_account_coordinator; stored_in=shared_worktree_and_010-coding.md; status=available
* entity_id=email_update_verification_atomicity; stored_in=shared_worktree_and_010-coding.md; status=verified

COMMUNICATION:
* sender=/root/pr74_coding; receiver=/root; task_id=10.08.2026_09.08.17-c0ecd3ab-3385-4a7f-ab41-649fb5f51d24; message_id=03618d96-e8b1-43b7-b6e2-c4d81407b2b8; protocol=AML-HIP

PERSISTENCE:
* local_memory=true; shared_memory=true; index_keys=[task_id,email_verification_account_coordinator,email_update_verification_atomicity,Koin,forced_orderings]

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
```
