Model: OpenAI GPT-5 (high-level reasoning class)
Changed files: agents/task/02.09.2026_18.17.45-c25c7421-1393-4a22-911a-51e0342451c9/001-planning.md

Model choice: Planning prioritizes a high-level model before a medium-level model. The available GPT-5 reasoning model was selected because the change crosses the shared deep-link contract, server routing, SMTP side effects, and JS startup navigation.

# Planning report

## Task understanding

The registration invitation must be sent as HTML containing a clickable anchor with a human-readable label instead of exposing markup or relying on a plain raw URL. A successfully handled deep link must also be able to tell the HTTP boundary to redirect the browser, while ordinary successful handlers retain the current empty `200 OK` response. Email verification must use that redirect capability to send the browser to the web-client root and surface an “Email has been approved.” confirmation. Finally, successful account approval must attempt a separate confirmation email to the verified address.

The existing account-verification safety rules remain in scope: the payload must have the expected type and non-null invited address, the current stored address must still match, promotion must remain coordinated with address mutation, stale links must remain unhandled, and repeated valid opens must leave the account approved. Missing links and unhandled links must continue returning `404`; malformed IDs must continue returning `400`.

## Investigation

`EmailRegistrationInviteSender.sendRegistrationEmail` currently creates the correct absolute `/api/links/{id}` URL but passes the invitation through `EmailsService.sendText`. `EmailsService.sendHtml` already exists and uses the production SMTP transport, so no new mail transport abstraction is needed.

`DeepLinkHandler.tryHandle` currently returns `Boolean`. `DeepLinksService.handle` converts `true` to the server-owned `HandleResult.Handled` object and `false` to `HandleResult.Unhandled`; `DeepLinksRoutingConfigurator` then converts the handled object to `200 OK`. Because the handler contract lives in `deeplinks/common`, a redirect-capable handled result also belongs in that common contract rather than introducing a dependency from the common module to `deeplinks/server`.

`EmailVerificationDeepLinkHandler` delegates all validation and promotion to `EmailVerificationAccountCoordinator`. The email server plugin already supports both SMTP-enabled and SMTP-disabled dependency graphs through `getOrNull<EmailsService>()`, so the verification handler can receive the same optional service without invalidating disabled deployments. A delivery failure cannot safely roll back a completed role transition; therefore the confirmation delivery is an after-promotion attempt, while the HTTP result continues to report the actual successful approval and redirect. Ordinary mail failures remain covered by the `EmailsService` false-result contract, while coroutine cancellation must retain existing propagation behavior.

The JS client already has the repository-native transient-feedback mechanism: `Toaster.show` stores a message and `ToastHost` is mounted once by `ScaffoldView`. The narrowest presentation is therefore a fixed success query marker on the redirect to `/ui`, consumed during `ClientJSPlugin.startPlugin`, shown through `Toaster`, and removed with `history.replaceState` while preserving unrelated URL parts. No new view or snackbar system is justified.

The relevant feature README operator-note sections contain no additional operator constraints. Code navigation used `ast-index`; its cache required a sandbox-approved rebuild before indexed searches worked.

## Questions for operator

No operator questions remain. The prompt explicitly leaves presentation choice open, and the existing app-wide toast is a direct repository-native fit. The plan treats SMTP confirmation as an attempt after the irreversible approval transition: mail delivery failure does not turn a completed approval into a `404` or suppress the success redirect.

## Architecture-stage checks

Architecture should preserve the current module direction by placing the handler-visible handled-result type in `deeplinks/common`. The preferred shape is a serializable `HandleResult` whose `Handled` member becomes a sealed interface with `Common` and `Redirect(url)` implementations; `DeepLinkHandler.tryHandle` returns a nullable handled subtype, where `null` means unhandled. Architecture should confirm the final package name and whether moving the existing small server result model or introducing an equivalent common model yields the cleanest source compatibility.

Architecture should also make the success marker a fixed shared contract rather than rendering arbitrary query text. The server should emit a same-origin relative target for the canonical `/ui` root, and the client should map only the known marker to the fixed approval message. The marker must be removed after consumption so refresh and subsequent navigation do not replay the toast.

Repeated valid link opens are currently intentionally idempotent. Confirmation-email sending should avoid weakening that behavior. Architecture should decide whether an email is attempted for every successful open or only for the first pending-to-approved transition; the recommended behavior is one email for the actual transition, provided the transition can be distinguished without widening the concurrency boundary or duplicating role-state logic. If that distinction would require broad roles-feature changes, preserving approval idempotency and sending on each successful open is the narrower fallback.

## Final implementation handoff

```text
ENTITY:
entity_id=deeplink_result_contract; type=shared_Kotlin_contract; state=boolean_handler_result_without_redirect_destination
entity_id=email_verification_flow; type=server_and_JS_flow; state=approval_without_browser_feedback_or_confirmation_email

CONTEXT:
* task_id=02.09.2026_18.17.45-c25c7421-1393-4a22-911a-51e0342451c9; agent_id=planning; memory_ref=[PROMPT.md,features/deeplinks/README.md,features/email/README.md,features/common/README.md,features/ui/scaffold/README.md]
* constraints=[preserve_HTTP_400_and_HTTP_404_semantics,preserve_common_handled_HTTP_200_semantics,preserve_email_address_match_atomicity,use_repository_native_Toaster,keep_SMTP_disabled_graph_resolvable]

ACTION:
1. action=replace_boolean_handler_contract; target=deeplink_result_contract; params={location=features/deeplinks/common,handled_variants=[Common,Redirect(url)],unhandled_representation=null,serialization=preserved}
2. action=propagate_handled_variant; target=deeplink_result_contract; params={service_behavior=return_handler_handled_variant_or_Unhandled,route_common=HTTP_200,route_redirect=HTTP_302_with_Location,route_missing_or_unhandled=HTTP_404}
3. action=render_registration_invite_as_HTML; target=email_verification_flow; params={sender=EmailRegistrationInviteSender,transport=EmailsService.sendHtml,link_target=absolute_verification_URL,link_label=human_readable_verification_action,cleanup_semantics=preserved}
4. action=return_approval_redirect; target=email_verification_flow; params={handler=EmailVerificationDeepLinkHandler,success_result=Redirect,redirect_target=/ui_with_fixed_success_marker,invalid_or_stale_result=null}
5. action=send_post_approval_confirmation; target=email_verification_flow; params={recipient=verified_payload_email,ordering=after_successful_promotion,transport=optional_EmailsService,delivery_failure=approval_and_redirect_preserved,cancellation=propagated}
6. action=consume_success_marker; target=email_verification_flow; params={entrypoint=ClientJSPlugin.startPlugin,presentation=Toaster.show,message=Email_has_been_approved,marker_cleanup=history.replaceState,arbitrary_query_text=forbidden}
7. action=update_regression_coverage_and_feature_docs; target=deeplink_result_contract; params={tests=[service_result_mapping,route_HTTP_200,route_HTTP_302,route_HTTP_404],docs=[features/deeplinks/README.md,features/email/README.md]}
8. action=update_regression_coverage; target=email_verification_flow; params={tests=[HTML_anchor_delivery,invite_cleanup,invalid_payload_no_redirect,successful_redirect,confirmation_email_after_promotion,SMTP_absence_or_failure_semantics,one_shot_JS_marker_consumption]}

REASON:
* condition=handler_result_contains_redirect_destination → action=route_emits_HTTP_302_Location → result=browser_loads_UI_root; requirement=end_to_end_redirect_capability
* condition=UI_root_contains_fixed_success_marker → action=ClientJSPlugin_calls_Toaster_and_removes_marker → result=single_transient_approval_confirmation; requirement=no_new_notification_framework
* condition=role_promotion_succeeds → action=confirmation_email_delivery_attempt_runs → result=verified_address_receives_approval_notice_when_SMTP_accepts_message; requirement=approval_state_survives_delivery_failure

EXPECTED RESULT:
* entity_id=deeplink_result_contract; new_state=common_success_or_redirect_result_propagated_to_HTTP_boundary; location=[features/deeplinks/common,features/deeplinks/server]
* entity_id=email_verification_flow; new_state=clickable_invite_then_approval_then_confirmation_email_then_UI_toast; location=[features/email/server,client/src/jsMain]

VERIFICATION:
* check=deeplink_route_matrix; expected=[blank_id:400,missing_id:404,unhandled:404,Common:200,Redirect:302_with_exact_Location]
* check=email_invite_message; expected=[content_type:HTML,anchor_href:absolute_verification_URL,anchor_label:human_readable,failed_delivery:minted_link_removed]
* check=email_approval_side_effects; expected=[valid_match:UserRole,stale_or_invalid:no_role_change,successful_transition:confirmation_send_attempt,send_failure:successful_redirect]
* check=JS_feedback_lifecycle; expected=[known_marker:toast_once,marker_removed:true,unknown_marker:no_toast,unrelated_URL_parts:preserved]

UNCERTAINTY:
* missing=none; ambiguity=none_after_documented_narrow_defaults

REPETITION OF RESULT:
* entity_id=deeplink_result_contract; stored_in=shared_memory; status=available_for_architecture
* entity_id=email_verification_flow; stored_in=shared_memory; status=available_for_architecture

COMMUNICATION:
* sender=planning; receiver=architecture; task_id=02.09.2026_18.17.45-c25c7421-1393-4a22-911a-51e0342451c9; message_id=5d7c1b86-0e2a-4e9c-86a7-b1754091299a; protocol=AML-HIP

PERSISTENCE:
* local_memory=true; shared_memory=true; index_keys=[task_id,deeplink_result_contract,email_verification_flow,planning_handoff]

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
```

## Expected changed-code scope

The coding cycle should remain concentrated in the deep-links common/server contract and tests, the email server sender/handler/plugin and tests, the JS client startup hook and focused tests, plus the email and deep-links feature READMEs. No new UI feature, persistent notification store, database migration, public email endpoint, or deep-link creation endpoint is needed.
