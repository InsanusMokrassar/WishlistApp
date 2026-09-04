Model: gpt-5-codex
Changed files: agents/task/27.07.2026_18.30.32-ae6ccedb-bc93-47f6-9a4e-54c1e81d8e30/001-planning.md

## Model rationale

Per `agents/SHORTCUTS.md`, planning prefers `fable` first. This session is running on `gpt-5-codex`, and the session model is fixed by the harness, so the report uses the actual active model rather than claiming an unavailable one.

## Task understanding

Issue #73 is not a single-module change. The repository already has:

- self-registration with username/password only,
- per-user stored email addresses,
- SMTP-backed test email sending,
- generic deeplink creation/handling,
- default role bootstrap for newly created users.

The missing work is to connect those pieces into an email-verification registration flow:

1. registration must optionally require an email field;
2. the server must send invite/approval links by email using the existing deeplink system;
3. when the requirement is enabled, freshly registered users must start with `NewUser` instead of `User`;
4. opening the approval link must promote `NewUser` to `User`.

## Evidence

### Current registration flow

- `features/auth/common/src/commonMain/kotlin/models/RegisterRequest.kt:13-16` — registration request carries only `username` and `password`.
- `features/auth/common/src/commonMain/kotlin/AuthFeature.kt:12-21` — shared auth API exposes `register(username, password)` and only a boolean registration-availability probe.
- `features/auth/client/src/commonMain/kotlin/KtorAuthFeature.kt:52-56` — client posts `RegisterRequest(username, password)` as-is.
- `features/auth/server/src/commonMain/kotlin/configurators/AuthRoutingsConfigurator.kt:29-36` — `/auth/register` reads `RegisterRequest` and forwards only username/password to the server feature.
- `features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt:113-123` — server registration creates `NewUser(username)` with no email and immediately issues credentials.
- `features/ui/auth/src/commonMain/kotlin/ui/AuthModel.kt:48-53` and `features/ui/auth/src/commonMain/kotlin/Plugin.kt:61-62` — UI auth model exposes registration with only username/password.
- `features/ui/auth/src/commonMain/kotlin/ui/AuthViewModel.kt:34-38,153-175` — registration screen state contains only username/password and submits only those two fields.

### Current user/email model

- `features/users/common/src/commonMain/kotlin/models/User.kt:17-38` — `User`/`NewUser` already support optional `email`.
- `features/email/client/src/commonMain/kotlin/EmailFeature.kt:37-47` and `features/email/client/src/commonMain/kotlin/KtorEmailFeature.kt:71-76` — clients can already store or clear the caller email via `PUT /email/myEmail`.
- `features/email/server/src/commonMain/kotlin/services/UpdateStoredEmail.kt:25-27` — email persistence is implemented as `usersRepo.update(callerId, NewUser(user.username, email))`.
- `features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt:60-73` — server email feature currently only supports test sending and self email storage.

### Current deeplink capability

- `features/deeplinks/server/src/commonMain/kotlin/services/DeepLinksService.kt:35-58` — server can mint persisted deeplinks by `handlerId + value`.
- `features/deeplinks/server/src/commonMain/kotlin/services/DeepLinksService.kt:73-83` — opening a deeplink dispatches to a registered handler.
- `features/deeplinks/common/src/commonMain/kotlin/DeepLinkHandler.kt:17-38` — features can contribute handlers identified by `DeepLinkHandlerId`.
- `features/deeplinks/server/src/commonMain/kotlin/configurators/DeepLinksRoutingConfigurator.kt:29-44` — deeplinks are opened via `GET /api/links/{deeplinkId}`.
- `features/deeplinks/common/src/commonMain/kotlin/models/DeepLinkHandlerInfo.kt:24-27` — deeplink payload is polymorphic app-defined data, so an email-approval payload can be introduced without changing the deeplink core.

### Current role bootstrap

- `features/roles/common/src/commonMain/kotlin/models/RoleConstants.kt:10-17` — only `SuperAdminRole` and `UserRole` exist today; `NewUser` role constant does not exist yet.
- `features/roles/server/src/commonMain/kotlin/RolesBootstrap.kt:25-30` — newly observed users automatically receive `UserRole`, and `root` additionally receives `SuperAdminRole`.
- `features/roles/server/src/jvmMain/kotlin/JVMPlugin.kt:53-63` — default role assignment happens both reactively for new users and by one-time backfill for existing users.

### Config and deployment surface

- `features/auth/server/src/commonMain/kotlin/models/AuthConfig.kt:10-13` — auth config currently contains only `enableRegistration`.
- `README.md:64-82` and `features/auth/README.md:51-72` — registration behavior is documented as a single boolean flag today.
- `server/docker-compose.yml:1-12` — local dev compose only provisions Postgres; no email-specific environment wiring exists there now.

### Missing existing approval flow

- `ast-index search "approval"` and `ast-index search "approve"` returned no feature flow hits.
- `ast-index search "verify"` returned only SMTP test-email text and unrelated file validation, not account verification.
- `ast-index search "invite link"` returned no existing invite service.

## Constraints and implications

- Registration changes are cross-stack: auth common/client/server plus `features/ui/auth`.
- Email sending cannot be bolted onto the current `EmailFeatureService` API without extending it: current server feature does not expose invite-link delivery, only SMTP test sending and `setMyEmail`.
- Role bootstrap logic will conflict with issue #73 unless it becomes conditional: today every newly created user gets `UserRole` automatically through `newObjectsFlow`.
- Deeplink verification likely belongs in a feature-owned handler rather than in the generic deeplink module, because the core module is intentionally payload-agnostic.

## Assumptions

- “require email during registration as an option” means a new server config flag alongside `enableRegistration`, not a hardcoded behavior.
- “service sends invite links using existing deeplinks” means the server sends an email containing `/api/links/{id}` generated by `DeepLinksService`.
- “email approval” refers to opening that emailed deeplink, because no other approval/verification mechanism exists in the repository today.
- The approval deeplink can serve as proof of mailbox ownership without requiring a second authenticated action.

## QUESTIONS FOR OPERATOR

None. The repository inspection was enough to produce a concrete plan. Remaining unknowns are implementation choices, not blockers to architecture.

## Plan for Architecture/Coding

1. Extend auth configuration and contract.
   - Add a new registration-email requirement flag to auth config and surface it through the auth API instead of a single boolean-only contract.
   - Expand registration DTOs and client/server `register(...)` methods to carry optional email data and the server-reported requirement state.

2. Update registration UI flow.
   - Add email field/state/validation to `features/ui/auth` registration mode.
   - Gate submit behavior on the server-reported “email required” flag so login mode stays unchanged and registration mode enforces email only when configured.

3. Introduce email-approval/invite sending service.
   - Extend the email server feature with a dedicated method that creates a deeplink payload, renders a link, and sends it to the registered email address.
   - Reuse existing SMTP wiring when configured; define behavior for disabled SMTP explicitly during architecture.

4. Add a feature-owned deeplink approval handler.
   - Define a typed deeplink payload for email approval.
   - Register a `DeepLinkHandler` that resolves the target user and promotes role state when the link is opened.

5. Add `NewUser` role semantics.
   - Introduce a `NewUser` role constant.
   - Refactor default-role bootstrap so new users receive `NewUser` instead of `User` when email-required registration is enabled, while preserving current behavior for root and for installations that do not require email.
   - Revisit the existing backfill rule so it does not incorrectly rewrite role state for already-approved users.

6. Connect registration to role assignment and invite sending.
   - After self-registration, assign either `User` or `NewUser` based on config.
   - When `NewUser` is assigned, send the invite/approval email using the newly minted deeplink.

7. Implement approval promotion.
   - Deeplink open should remove `NewUser` and grant `User`.
   - Make the promotion idempotent so repeated link opens are safe.

8. Update tests and docs.
   - Auth tests for registration payload/config branching.
   - Email tests for invite sending behavior.
   - Role/bootstrap tests for `NewUser` vs `User` assignment and promotion.
   - Deeplink handler tests for success/idempotency/not-found cases.
   - README/config docs for the new option and resulting flow.

## Architecture risks to resolve in the next stage

- Whether the new registration-state probe should stay a boolean or become a richer DTO consumed by the UI.
- Whether invite sending lives inside `features/email/server` or in a small auth-adjacent orchestration service that depends on email + deeplinks + roles.
- How the existing roles backfill should behave for pre-existing installations once `NewUser` is introduced, so startup migration does not downgrade or incorrectly promote accounts.
- What server response is correct when email is required but SMTP/email-delivery support is unavailable.

## AML-HIP

ENTITY:
entity_id=issue73; type=planning_report; state=completed
entity_id=auth_registration_flow; type=feature_flow; state=missing_email_requirement
entity_id=email_invite_service; type=service; state=absent
entity_id=deeplink_approval_handler; type=handler; state=absent
entity_id=role_new_user; type=role; state=absent

CONTEXT:

- task_id=27.07.2026_18.30.32-ae6ccedb-bc93-47f6-9a4e-54c1e81d8e30; agent_id=planning-001; memory_ref=[PROMPT.md, agents/ALL.md, agents/PLAN.md, agents/PROTOCOL.md, agents/GIT.md]
- constraints=[edit_only_current_step_file, preserve_unrelated_worktree_changes, no_source_changes_in_planning_stage, commit_only_step_report]

ACTION:

1. action=inspect; target=auth_registration_flow; params={evidence=[features/auth/common/src/commonMain/kotlin/models/RegisterRequest.kt:13-16,features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt:113-123,features/ui/auth/src/commonMain/kotlin/ui/AuthViewModel.kt:34-38|153-175]}
2. action=inspect; target=email_invite_service; params={evidence=[features/email/server/src/commonMain/kotlin/services/EmailFeatureService.kt:51-73], result=current_api_missing_invite_delivery}
3. action=inspect; target=deeplink_approval_handler; params={evidence=[features/deeplinks/server/src/commonMain/kotlin/services/DeepLinksService.kt:35-58|73-83,features/deeplinks/common/src/commonMain/kotlin/DeepLinkHandler.kt:17-38], result=platform_support_present_feature_handler_missing}
4. action=inspect; target=role_new_user; params={evidence=[features/roles/common/src/commonMain/kotlin/models/RoleConstants.kt:10-17,features/roles/server/src/commonMain/kotlin/RolesBootstrap.kt:25-30], result=current_bootstrap_always_grants_user_role}

REASON:

- condition=issue73_requires_optional_email_registration_plus_invite_links_plus_role_transition; requirement=plan_must_cover_auth+email+deeplinks+roles+ui
- condition=no_existing_account_approval_flow_detected; requirement=record_verification_link_mechanism_as_new_work_not_existing_behavior

EXPECTED RESULT:

- entity_id=auth_registration_flow; new_state=planned_for_config_and_contract_extension; location=architecture_stage
- entity_id=email_invite_service; new_state=planned_for_creation; location=architecture_stage
- entity_id=deeplink_approval_handler; new_state=planned_for_creation; location=architecture_stage
- entity_id=role_new_user; new_state=planned_for_creation_and_promotion_logic; location=architecture_stage

VERIFICATION:

- check=step_file_scope; expected=only_agents/task/27.07.2026_18.30.32-ae6ccedb-bc93-47f6-9a4e-54c1e81d8e30/001-planning.md_changed
- check=operator_questions_blocking; expected=none
- check=evidence_contains_line_references; expected=true

UNCERTAINTY:

- missing=final_shape_of_registration_state_api; ambiguity=boolean_probe_vs_richer_config_dto
- missing=final_owner_feature_of_invite_orchestration; ambiguity=email_feature_vs_auth_adjacent_service
- missing=startup_behavior_for_existing_users_after_new_role_introduction; ambiguity=backfill_policy_requires_architecture_decision

REPETITION OF RESULT:

- entity_id=issue73; stored_in=shared_memory; status=available
- entity_id=role_new_user; stored_in=shared_memory; status=available

COMMUNICATION:

- sender=planning-001; receiver=architecture-002; task_id=27.07.2026_18.30.32-ae6ccedb-bc93-47f6-9a4e-54c1e81d8e30; message_id=ebf18d62-9b0a-4427-a902-8f543bf2f217; protocol=AML-HIP

PERSISTENCE:

- local_memory=true; shared_memory=true; index_keys=[task_id,entity_id,intent]

VALIDATION:

- format_valid=true
- no_pronouns=true
- entities_explicit=true
- high_density=true
- causal_chain_present=true
- ambiguity_detected=true
