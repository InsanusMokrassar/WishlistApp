Model: gpt-6-astra
Changed files: agents/task/17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd/001-planning.md

# Planning for the email approval cooldown

Planning uses gpt-6-astra as the available high-level reasoning model, following the Planning priority of HL before ML in `agents/SHORTCUTS.md`. `agents/MODELS.md` gives examples rather than an exhaustive model list; Astra is the available highest-capability model. The role-specific requirement to write and commit the Planning report applies here. Repository caveman mode applies to internal notes only. The lean-build skill supplied the acceptance, non-goal, lifecycle, and stop-condition discipline used below.

The task is a follow-up to issue #79 and open PR #82 on `feat/issue-79-user-email-change`. The latest prior reports, including 015, 017, and 019, establish the privacy and error-handling behavior that must survive the change. This stage inspected source and documentation only; no product file or existing report was modified, and no build or tests were run.

## Investigation result

There is no existing email-change count limit or time restriction in the inspected application path. `EmailVerificationAccountCoordinator.updateStoredEmail` performs only an existence lookup followed by an unconditional replacement at `features/email/server/src/commonMain/kotlin/services/EmailVerificationAccountCoordinator.kt:43`. Its only coordination mechanism is a process-local mutex at line 32. Enabled storage delegates directly to that operation at `features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt:80`; the SMTP-disabled implementation shares the coordinator. The route has bearer ownership and duplicate handling but no throttling at `features/email/server/src/commonMain/kotlin/configurators/EmailRoutingsConfigurator.kt:65`. AST searches for `cooldown` and `rateLimit` returned no results. No claim is made about deployment infrastructure outside the repository.

The existing database cannot preserve the latest approved address across a replacement. `features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt:59` declares the sole nullable unique email column; line 67 stores only a Boolean approval marker. The ordinary update overwrites that column and resets approval for a different address at line 101. Exact-address approval at line 189 sets the Boolean without recording a time. `RegisteredUser` has only `email` and `emailApproved` at `features/users/common/src/commonMain/kotlin/models/User.kt:53`.

The authenticated private profile faithfully exposes that overwritten value: the repository projection is `features/auth/common/src/commonMain/kotlin/models/AuthFeatureUser.kt:43`, called by token-to-user lookup at `features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt:115`. The public projection at `features/users/common/src/commonMain/kotlin/models/UsersFeatureUser.kt:23` intentionally contains only id and username. New pending and restriction data must remain private.

Verification currently compares the invited address to the single stored email, approves that exact address, and then performs the retryable role transition at `features/email/server/src/commonMain/kotlin/services/EmailVerificationAccountCoordinator.kt:100`. `EmailFeatureService.requestMyEmailVerification` checks and rechecks that same address around asynchronous SMTP at `features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt:94`. The invite sender obtains its recipient directly from `user.email` at `features/email/server/src/commonMain/kotlin/services/EmailRegistrationInviteSender.kt:53`. The deeplink handler preserves approval even if the confirmation email fails and returns the established redirect at `features/email/server/src/commonMain/kotlin/services/EmailVerificationDeepLinkHandler.kt:42`. All these comparisons and recipient selection must follow the pending candidate when a replacement exists.

The owner UI already separates the editable draft from an authoritative saved address, but has no separate persisted pending replacement. Storage eligibility has owner, capability, profile, and busy checks at `features/ui/users/src/commonMain/kotlin/ui/UserEditViewModel.kt:231`. Save compares the draft to `profile.email` at line 245 and later requires the refreshed `profile.email` to equal the submitted value at line 866. Resend selects `profile.email` at line 923. Feedback snapshots contain only email and approval at line 1046. Each assumption needs adjustment when approved A remains current while B is pending.

The JS renderer shows one saved email and an approval hint at `features/ui/users/src/jsMain/kotlin/ui/UserEditView.kt:170`; the production desktop panel has the equivalent rendering at `features/ui/users/src/jvmMain/kotlin/ui/UserEditView.kt:281`. Both are guarded by current and immediate navigation targets, at JS line 97 and JVM line 265. Android follows the same shared state. Preserve those guards around every new address, deadline, action, and message.

The complete email, users, auth, admin, and users UI READMEs were read, including Operator Notes. The admin notes require existing feature reuse and root-only access. The ordinary username update already shares the account coordinator at `features/email/server/src/commonMain/kotlin/services/EmailVerificationAccountCoordinator.kt:70`; it must preserve pending state as well as current approval. No Operator Note requires changing the requested behavior or asking the operator.

## Configuration and default recommendation

No existing setting or historical product decision supplies a cooldown duration. `features/email/server/src/commonMain/kotlin/EmailConfig.kt:21` contains only SMTP settings, and that configuration exists only in the SMTP-enabled DI graph at `features/email/server/src/commonMain/kotlin/Plugin.kt:64`. Therefore putting the policy exclusively in that conditional object would make policy configuration unavailable when SMTP is disabled. The coordinator is registered unconditionally at line 69, which is the correct place to supply the independent account policy.

Use one email-owned setting decoded from the root configuration, for example `emailChangeCooldown`, with the existing serializable Kotlin `Duration` convention. Auth already uses that convention at `features/auth/server/src/commonMain/kotlin/Config.kt:9`, and `server/sample.config.json:40` demonstrates ISO durations. Require a finite, nonnegative value and reject malformed, negative, or overflowing values during configuration validation. Do not change the meaning of the optional SMTP object or introduce another SMTP mode.

The repository-grounded compatibility default is zero duration: existing deployments currently impose no restriction, and no evidence authorizes a particular nonzero implicit duration. Document that zero disables the restriction. Demonstrate an explicit positive value in the sample configuration; `P1D` is a proposed illustrative sample, not an existing policy or an inferred user preference. For any configured duration greater than zero, the required prohibition must work on the server and in the profile UI. The report does not claim that an unchanged deployment with an omitted setting will acquire a positive cooldown. Architecture should retain this distinction explicitly in documentation and handoff.

The narrowest durable restriction representation is a nullable deadline, such as `emailChangeAllowedAt`, written from server time plus the configured duration when approval first changes the effective approved address. This avoids a new status endpoint, client-side policy calculation, and a dependency from auth server back into email server. Include the server-issued deadline in the private profile. A subsequent configuration change applies to subsequent approvals; already-issued deadlines remain stable. This is a deliberate lifecycle recommendation, not a claim that the repository already has such a policy. If Architecture chooses an approval timestamp instead, it must provide the client a server-computed eligibility value without creating a module cycle; do not quietly compute policy from an unrelated client default.

## Acceptance criteria

With an approved address A and an expired or absent restriction, saving B stores B as the pending replacement. Private current email remains A, and A remains approved until the valid B approval commits. Refresh, logout/login, and server/database reopen preserve both values. Saving or delivering B must never label B as the current approved email.

Approving the exact current pending B atomically makes B current and approved, removes the pending replacement, and persists the new deadline using server time. Before the deadline, another replacement request is rejected without changing either email state or creating an automatic verification delivery. At the exact deadline, replacement becomes available. Positive, zero, and invalid configured durations have explicit behavior. Both SMTP-enabled and SMTP-disabled storage paths enforce the same restriction. Direct HTTP calls and imperative Save/IME callbacks cannot bypass it.

Opening the same valid approval link again must not restart or extend the restriction. Stale links for superseded pending addresses cannot switch the current address or advance the deadline. Existing retryable role promotion, exact-link compensation, cancellation propagation, legacy null-payload rejection, and fixed success redirect remain intact. No new credentials are issued by approval.

The owner profile displays the approved current address, the distinct pending replacement when present, and an understandable restriction message with when another replacement becomes possible. Save is unavailable during the active restriction. Expiry can be reconciled through the existing Refresh action; an animated countdown is unnecessary. Resend operates on the persisted pending candidate, never an unsaved draft or the retained approved A. SMTP-disabled mode still permits storing a candidate once allowed and still explains that verification delivery is unavailable.

For an account with no approved address yet, preserve registration and first-email behavior: the existing unapproved address remains the verification candidate until its first approval. The latest-approved-address guarantee becomes meaningful once the first approval exists. Do not add an unrelated required-email rule or prevent unverified registration from completing.

Duplicate rejection must continue to protect addresses held by other accounts, including retained current addresses and pending candidates. A failed conflicting request leaves current, pending, and deadline fields unchanged. Owner API responses retain generic conflict/error disclosure; invalid drafts, failed storage, delivery failure, and uncertain transport remain distinguishable to the same extent as issue #79. Refresh may reveal a committed write after a lost response without fabricating successful delivery.

Pending state and deadlines are visible only through authenticated private surfaces. Public users remain id and username only. Anonymous callers, non-owners, and root editing another user's public profile receive no private email panel or private calls. Identity loss or live retargeting removes the new metadata immediately alongside existing private state. Dirty malformed drafts and current failures survive refresh according to the existing issue #79 rules; completed feedback is invalidated when the relevant current/pending/approval/deadline snapshot changes.

## Narrow implementation plan for Architecture

Extend the existing user persistence lifecycle rather than adding a separate change-history service. Retain the current email column as the current address, add a nullable pending replacement column, and add the durable nullable restriction deadline. For already approved accounts, replacement writes only the pending slot. For accounts never approved, retain the existing first-address behavior to avoid rewriting required-registration finalization. Approval conditionally promotes the pending slot when present, otherwise approves the existing unapproved first address. Deadline updates occur only on a new approval transition, not on idempotent replay. Preserve both fields in username-only operations and every cache publication.

Make the lifecycle operations explicit on the repository seam rather than passing incomplete `NewUser` objects through generic updates that could erase pending data. The coordinator remains the shared self-service/admin/verification orchestration boundary. Existing full admin updates must not accidentally discard pending state or turn an unapproved replacement into current email; reuse the email lifecycle instead of adding a new admin-only implementation or controls. Existing intentional clear requests must be guarded during the cooldown, and an allowed explicit clear must deliberately clear email state rather than being interpreted as a successful replacement approval. No clear button is required by this task.

Uniqueness requires special care. Two separate unique indexes on current and pending columns do not prevent account X's current address from equaling account Y's pending address. Architecture must select and document one repository-owned invariant covering create, ordinary update, pending replacement, and approval, including registration and admin entry points. The existing process-local coordinator alone does not cover arbitrary repository creates or multiple server processes. Prefer the smallest transactionally sound implementation within `UsersRepo`; if a reservation table or database-specific locking is required to preserve the existing guarantee, justify that addition before Coding. A read-then-write check with no adequate serialization must not be presented as globally safe. PostgreSQL and SQLite are both declared repository targets.

Carry pending replacement and the deadline through `RegisteredUser`, the private `AuthFeatureUser`, their explicit mappers, the cache, and relevant privileged projections. Preserve the public projection's omission of all private data; its reverse mapper must require missing private fields explicitly rather than silently clearing them. The existing authenticated `getMe` read then supplies the profile UI without introducing another endpoint or request lifecycle.

Keep `PUT /email/myEmail` as the mutation route. Preserve 409 for duplicates and introduce a distinct cooldown rejection, preferably 429 with the authoritative retry deadline or Retry-After value. The existing Boolean client surface at `features/email/client/src/commonMain/kotlin/KtorEmailFeature.kt:80` loses non-success details, so add a narrow typed restriction outcome or exception at the transport boundary while preserving generic handling for duplicate and uncertain failures. Do not turn every failed PUT into a guessed cooldown. A rejected request should reconcile the private profile and show authoritative restriction feedback without automatically sending verification.

Update verification target selection in the service, sender, coordinator, and post-SMTP reconciliation. Reuse the existing invite payload and exact-address guard. Update shared profile logic so persisted candidate means pending replacement when present and otherwise the unapproved initial address. Save confirmation, dirty comparisons, resend, automatic verification, and feedback snapshots must use that lifecycle consistently. Add current/pending/restriction rendering and EN/RU copy across JS, JVM, and Android inside the existing privacy boundary. Update affected feature READMEs and sample configuration while preserving Operator Notes verbatim.

## Migration and compatibility

Follow the repository's additive `initTable()` convention at `features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt:207`. Existing rows keep their current email and approval exactly; new pending and deadline columns start null. Existing approved rows have no recorded approval time, so do not invent a historical deadline or restart a restriction on every server boot. Their next real approval establishes the deadline. Existing unapproved records and persisted email-bound links remain usable under the first-address path. Reopening must not reset new pending/deadline state.

Add defaulted nullable fields to serializable private models so old payloads remain decodable. Do not weaken the public model or use caller-supplied timestamps as approval evidence. The new schema is additive, but running the old binary while new pending replacements exist cannot be called behavior-preserving: the old code overwrites email and ignores pending state. Document forward deployment and backup/recovery considerations rather than claiming arbitrary old-binary rollback safety. No destructive conversion or historical backfill is needed for the recommended shape.

## Verification scope and stop condition

Repository tests must cover retained A/pending B, first approval, B promotion, stale B after replacement C, idempotent repeated approval without deadline extension, no mutation on conflicts, current-versus-pending uniqueness in both directions, creation against reserved addresses, cache agreement, username-only preservation, legacy schema upgrade, and database reopen. Use a controllable clock for exact boundary checks without sleeping. Exercise concurrent replacement versus approval and duplicate acquisition at the actual ownership boundary, not only fake repositories.

Email service and route tests must prove positive/zero cooldowns, both SMTP graphs, explicit cooldown status, unchanged duplicate mapping, no SMTP on rejected changes, exact pending-recipient delivery, state changes during SMTP, and retained compensation/cancellation semantics. Config tests must cover omitted, positive, zero, negative, malformed, and nonfinite durations plus graph construction. Auth/private/public/privileged mapping and serialization tests must establish privacy and field preservation; registration and admin regression suites are required because both paths share user persistence.

Shared UI tests must cover current A/pending B, successful B approval and restriction, expired restriction refresh, blocked Save and IME callback, typed rejection after a stale eligible profile, candidate-based resend, raw invalid draft preservation, failed and lost PUT responses, feedback retirement, owner loss, and retargeting. Extend the existing actual JVM `OwnerEmailEditor` renderer tests for simultaneous current/pending fields, restriction copy and disabled controls, expiry recovery, and complete privacy removal. Run shared tests on JVM, JS Node, and Android debug and compile all three renderers. Preserve the existing route/client/repository suites and verify relevant auth/admin suites, then use the repository's required serial build gates in later roles. Planning claims no fresh execution result.

Stop when the configured cooldown and approved/pending lifecycle pass their focused proofs and the existing privacy/error regressions remain green. Explicit non-goals are rolling count quotas, resend throttling, generic account rate limiting, email history/audit screens, a new notification channel, changing role or credential policy, new admin email controls, a timer animation, browser/device harnesses, dependencies added for UI polish, and changes to unrelated PRs.

## Questions and resolved decisions

No operator question is required to continue Architecture. The repository resolves the ownership boundary, SMTP independence, privacy model, registration compatibility, and available persistence seams. The absent historical default is handled explicitly by the compatibility recommendation above, not by asserting an invented policy. Architecture must finalize the cross-slot uniqueness mechanism, exact field/result names, and timestamp wire representation; those are implementation decisions with concrete constraints, not missing operator intent. Any later proposal for an implicit positive default must be identified as a product-policy choice rather than repository evidence.

AST navigation used an initial index built with `XDG_CACHE_HOME=/tmp/wishlist-email-cooldown-ast`. The first default-cache rebuild failed with a read-only filesystem error; the task-specific temporary cache succeeded with 802 indexed files and 49 modules. No source was changed and no subsequent rebuild was necessary. The only intended commit is this report, without a push.

```aml-hip
ENTITY:
entity_id=email_approval_cooldown_plan; type=planning_handoff; state=ready_for_architecture

CONTEXT:
task_id=17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd; agent_id=planning; memory_ref=[PROMPT,prior_015,prior_017,prior_019,001]; branch=feat/issue-79-user-email-change
constraints=[report_only,no_push,private_owner_state,SMTP_independent_storage,approved_email_retention,duplicate_protection]; issue=79; pull_request=82

ACTION:
action=trace_email_lifecycle; target=email_approval_cooldown_plan; params={layers:[repository,cache,coordinator,verification,configuration,HTTP,private_profile,UI],existing_rate_limit:false,existing_approval_timestamp:false}
action=recommend_minimal_lifecycle; target=email_approval_cooldown_plan; params={storage:[pending_email,approval_deadline],configuration:independent_Duration,omitted_default:zero,sample_positive_value:P1D,deadline_policy:future_approvals}
action=hand_off_required_proofs; target=email_approval_cooldown_plan; params={proofs:[exact_boundary,idempotent_approval,retained_current_email,cross_slot_uniqueness,migration,reopen,privacy,transport_errors,three_platform_UI]}

REASON:
condition=single_email_column_and_Boolean_approval; requirement=retain_latest_approved_email_and_enforce_post_approval_delay; causal_chain=separate_pending_storage+durable_deadline→conditional_promotion_and_rejection→requested_profile_behavior
condition=no_repository_duration_policy; requirement=avoid_invented_implicit_policy; causal_chain=zero_compatibility_default+explicit_positive_configuration→configurable_enforcement_without_fabricated_history

EXPECTED RESULT:
entity_id=email_approval_cooldown_plan; new_state=architecture_input_available; location=agents/task/17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd/001-planning.md

VERIFICATION:
check=source_investigation; expected=no_existing_frequency_limit; observed=coordinator_repository_routes_config_UI_no_time_or_count_guard
check=planning_scope; expected=report_only_and_no_test_execution; observed=report_only_and_no_test_execution
check=implementation_acceptance; expected=focused_future_proofs_and_preserved_issue_79_regressions; status=pending_implementation

UNCERTAINTY:
missing=historical_approval_times; ambiguity=none; resolution=legacy_deadline_null
missing=final_cross_slot_serialization_design; ambiguity=none; resolution=Architecture_required_transactional_invariant

REPETITION OF RESULT:
entity_id=email_approval_cooldown_plan; stored_in=shared_memory; status=available; next_role=Architecture

COMMUNICATION:
sender=planning; receiver=Orchestrator_and_Architecture; task_id=17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd; message_id=192258e0-c338-43f7-9f74-e3bd204f9c28; protocol=AML-HIP

PERSISTENCE:
local_memory=true; shared_memory=true; index_keys=[task_id,email_approval_cooldown_plan,issue_79,PR_82]; storage=001-planning.md; auto_memory=false

VALIDATION:
format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
```
