Model: OpenAI GPT-5 (high-level reasoning class)
Changed files: agents/task/02.09.2026_18.17.45-c25c7421-1393-4a22-911a-51e0342451c9/002-architecturing.md

Model choice: Architecture prioritizes a high-level model before a medium-level model. The available GPT-5 reasoning model was selected because the change alters a shared handler contract and must preserve HTTP, SMTP, dependency-injection, concurrency, and browser-navigation semantics across Kotlin Multiplatform modules.

# Architecture report

## Outcome

The implementation should introduce a common deep-link result hierarchy, pass successful results through the dispatcher unchanged, and let the HTTP route distinguish an ordinary success from a redirect. Email verification should return a same-origin redirect to `/?emailApproval=approved`. The JS shell should recognize only that fixed marker, show `Email has been approved.` through the existing `Toaster`, and remove the marker before navigation startup so refreshes do not repeat the toast.

The current source mounts the web application at the site root: `defaultWebClientSubPath` is `""` and `index.html` uses `<base href="/">`. Therefore `/`, not `/ui`, is the correct main-page target. The conflicting `/ui` statement in `features/common/README.md` is stale documentation and should be corrected during Coding.

The registration invitation should use `EmailsService.sendHtml` with an anchor whose visible label is `Verify email address`; the generated absolute verification URL should appear only in the anchor's `href`. Successful verification should attempt a separate plain-text approval-confirmation email after role promotion. Confirmation-delivery absence, a `false` result, or a non-cancellation failure must not reverse approval or suppress the redirect. Coroutine cancellation must still propagate.

## Contract and ownership

Move `HandleResult` from `features/deeplinks/server` to `features/deeplinks/common`, under `dev.inmo.wishlist.features.deeplinks.common.models`. The result is a domain-level deep-link outcome and the handler contract must be able to name its successful subtype without depending on the server module.

Use this public shape:

```kotlin
@Serializable
sealed interface HandleResult {
    @Serializable
    data object NotFound : HandleResult

    @Serializable
    data object Unhandled : HandleResult

    @Serializable
    sealed interface Handled : HandleResult {
        @Serializable
        data object Common : Handled

        @Serializable
        data class Redirect(val url: String) : Handled
    }
}
```

Change `DeepLinkHandler.tryHandle` to return `HandleResult.Handled?`. `null` is the only handler-level unhandled value. `HandleResult.Handled.Common` preserves the existing successful `200 OK` behavior, while `HandleResult.Handled.Redirect(url)` carries a trusted in-process handler destination to the HTTP boundary. The infrastructure should not parse or rewrite redirect URLs; concrete handlers own destination safety. The email handler must supply a root-relative same-origin destination, never payload-controlled text or an arbitrary external URL.

`DeepLinksService.handle` should retain `NotFound` for a missing stored record and `Unhandled` for a missing handler, then return `handler.tryHandle(...) ?: HandleResult.Unhandled`. This removes the Boolean-to-result translation and preserves the selected success subtype exactly.

`DeepLinksRoutingConfigurator` should keep current id validation and map results exhaustively: `Common` to an empty `200 OK`, `Redirect` through `call.respondRedirect(url, permanent = false)` to `302 Found` with `Location`, and both `NotFound` and `Unhandled` to `404 Not Found`. No redirect response body contract is required.

## Email flow

Add fixed shared marker constants to `EmailConstants`:

```kotlin
const val approvalQueryParameter = "emailApproval"
const val approvalQueryValue = "approved"
const val approvalRedirectPath = "/?emailApproval=approved"
```

The constants are shared by the server handler and root JS client through the existing email-common dependency. They carry protocol values only; the human-facing toast text remains client presentation.

Change `EmailRegistrationInviteSender.sendRegistrationEmail` to call `sendHtml`. Keep recipient selection, deep-link creation, absolute URL construction, false-result cleanup, ordinary-exception cleanup, and non-cancellable cancellation cleanup unchanged. Use a small static HTML body containing an anchor such as `<a href="...">Verify email address</a>` and do not render the raw URL as visible text. The URL consists only of the validated public origin and generated deep-link id, so no new template engine is warranted.

Extend `EmailVerificationDeepLinkHandler` with an optional `EmailsService`. Handling should follow this order:

1. Reject a non-`EmailVerificationPayload` value with `null`.
2. Reject a legacy null recipient with `null`.
3. Call `verifyInvitedEmailAndPromote`; reject a missing, changed, cleared, or otherwise invalid account with `null`.
4. After successful promotion, attempt `sendText` to the verified payload address with a dedicated subject such as `Your WishlistApp account is approved` and a fixed confirmation body.
5. Return `HandleResult.Handled.Redirect(EmailConstants.approvalRedirectPath)` regardless of an absent transport, a `false` delivery result, or an ordinary delivery exception. Re-throw `CancellationException`.

Update `Plugin.setupDI` so the qualified handler receives `getOrNull<EmailsService>()`. Both SMTP-enabled and SMTP-disabled Koin graphs must remain resolvable.

The existing coordinator deliberately treats repeated valid opens as successful and the role transition is idempotent. The narrow implementation should therefore re-attempt the confirmation email on every valid successful open. Sending exactly once would require a durable delivery/transition receipt or a wider roles contract; neither exists, and an in-memory flag would be incorrect across restarts. This fallback preserves existing idempotency and makes failed or interrupted delivery retryable without adding persistence.

## Browser feedback

Keep feedback in the root JS shell because the redirect enters the application before any feature view exists and `ToastHost` is already mounted once by `ScaffoldView`.

Extract an internal JS helper with injected effects, for example:

```kotlin
internal fun consumeEmailApprovalNotification(
    rawUrl: String,
    showMessage: (String) -> Unit,
    replaceUrl: (String) -> Unit,
): Boolean
```

The helper should parse `rawUrl` with the browser `URL` API. Only `emailApproval=approved` is recognized. On a match, delete all occurrences of that parameter, preserve the pathname, unrelated query parameters, and fragment, call `showMessage("Email has been approved.")`, call `replaceUrl(cleanedRelativeUrl)`, and return `true`. Unknown or missing values should cause no callbacks and return `false`; arbitrary query content must never become toast text.

At the beginning of `ClientJSPlugin.startPlugin`, invoke the helper with `window.location.href`, `Toaster::show`, and a lambda calling `window.history.replaceState(null, "", cleanedRelativeUrl)`. Run this before `ClientPlugin.startPlugin(koin)`, ensuring the URL-backed navigation repository starts from the cleaned URL. The toaster's `StateFlow` retains the message until `ToastHost` mounts, so no new view, state store, or snackbar is needed.

## Planned source changes

Coding should limit production changes to these files:

- Move `features/deeplinks/server/src/commonMain/kotlin/models/HandleResult.kt` to `features/deeplinks/common/src/commonMain/kotlin/models/HandleResult.kt` and update the package.
- Update `features/deeplinks/common/src/commonMain/kotlin/DeepLinkHandler.kt`.
- Update `features/deeplinks/server/src/commonMain/kotlin/services/DeepLinksService.kt`.
- Update `features/deeplinks/server/src/commonMain/kotlin/configurators/DeepLinksRoutingConfigurator.kt`.
- Update `features/email/common/src/commonMain/kotlin/Constants.kt`.
- Update `features/email/server/src/commonMain/kotlin/services/EmailRegistrationInviteSender.kt`.
- Update `features/email/server/src/commonMain/kotlin/services/EmailVerificationDeepLinkHandler.kt`.
- Update `features/email/server/src/commonMain/kotlin/Plugin.kt`.
- Update `client/src/jsMain/kotlin/ClientJSPlugin.kt`, keeping the helper in that file or one adjacent JS-only file.

Focused test files may be added under deep-links common/server, email server, and client JS test source sets. Existing affected tests and fakes should be adjusted for the new return types and HTML call recording. No database migration, new UI feature, new route, public deep-link mutation endpoint, navigation model, or persistent notification table is needed.

## Test specifications

### `HandleResult` and `DeepLinkHandler.tryHandle`

Add a common-module serialization test that round-trips `NotFound`, `Unhandled`, `Handled.Common`, and `Handled.Redirect("/target?x=1")`; each decoded value must equal the original and the redirect URL must remain exact.

Update every fake and concrete `DeepLinkHandler` to return `Handled?`. Handler tests must prove that `null` means unhandled, `Common` represents ordinary success, and `Redirect` retains its exact URL. Compilation across JVM, JS, and Android source sets is part of this contract check because the type moved into `deeplinks/common`.

### `DeepLinksService.handle`

Add focused service tests using an in-memory `DeepLinksRepo` and fake handlers:

- A missing id returns `NotFound` and calls no handler.
- A stored record with an unknown handler id returns `Unhandled`.
- A selected handler returning `null` produces `Unhandled`.
- A selected handler returning `Handled.Common` produces the same singleton.
- A selected handler returning `Handled.Redirect("/destination")` produces an equal redirect with the exact URL.
- Duplicate handler ids still fail at service construction.

### `DeepLinksRoutingConfigurator`

Add Ktor `testApplication` route tests, mounting the configurator beneath the normal `/api` prefix:

- A common-handled link returns `200 OK`, no `Location` header, and an empty body.
- A redirect-handled link returns `302 Found` and `Location: /destination?result=ok`.
- Handler `null`, unknown handler id, and missing stored id each return `404 Not Found`.
- A blank path parameter, represented by a URL-encoded blank segment accepted by the test router, retains `400 Bad Request`; if Ktor rejects an absent segment before the handler, retain the existing router-level `404` for the absent-route case and test the branch with a directly blank parameter fixture.

### `EmailRegistrationInviteSender.sendRegistrationEmail`

Extend `FakeEmailsService` to record HTML call arguments. Update the success test to derive the minted URL from the stored deep-link id and assert one `sendHtml` call, the correct recipient and subject, an anchor whose `href` equals the absolute `/api/links/{id}` URL, visible text `Verify email address`, no `sendText` call, and no raw URL as visible body text outside the attribute.

Retain and adapt the failure tests so a `false` HTML result, an ordinary HTML-send exception, and cancellation each remove the minted link. Cancellation must propagate only after non-cancellable cleanup. Missing email, SMTP transport, or deep-link service must still return `false` without minting or sending.

### `EmailVerificationDeepLinkHandler.tryHandle`

Update handler and integration tests with a call-recording `EmailsService`:

- Wrong payload type, missing user, legacy null email, cleared email, and changed email return `null`, preserve pending role state, send no confirmation, and produce no redirect.
- A matching pending account becomes `UserRole`, loses `NewUserRole`, sends one confirmation to the exact verified address, and returns `Handled.Redirect("/?emailApproval=approved")`.
- An absent email service still approves and returns the same redirect.
- A service returning `false` still leaves the account approved and returns the same redirect.
- An ordinary service exception still leaves the account approved and returns the same redirect.
- A `CancellationException` propagates; the already-completed approval remains committed.
- Opening the same valid link twice leaves exactly `UserRole`, returns the same redirect twice, and records two confirmation attempts, documenting the deliberate retry fallback.

Update the end-to-end mint-and-dispatch test to expect `Handled.Redirect` instead of the old `Handled` object and to verify confirmation delivery arguments.

### `Plugin.setupDI`

Extend existing Koin graph tests for both config shapes. With SMTP absent, the handler must resolve with the shared coordinator and no transport; with SMTP configured, the handler must resolve with the graph's `EmailsService`. Invoking the resolved handler should prove disabled mode still redirects after approval and enabled mode records the confirmation send.

### `consumeEmailApprovalNotification` and `ClientJSPlugin.startPlugin`

Add JS tests using injected callback recorders:

- `https://host/?emailApproval=approved` emits exactly `Email has been approved.`, replaces the URL with `/`, and returns `true`.
- A recognized marker alongside `keep=1` and `#section` removes only the marker and preserves `/`, `keep=1`, and `#section`.
- Duplicate recognized marker entries are all removed in the cleaned URL and trigger only one message.
- A missing marker or `emailApproval=failed` triggers neither callback, leaves the URL untouched, and returns `false`.
- Query values containing arbitrary text are never displayed.

A focused JS startup test should use the helper seam to verify marker consumption occurs before navigation initialization, or the implementation should keep the startup edit to the single tested helper invocation placed before `ClientPlugin.startPlugin`. All requested behavior is automatable; no operator decision about untestable functionality is required.

## README updates

Coding should update `features/deeplinks/README.md` without changing `## Operator Notes`: document `HandleResult.Handled.Common` and `Redirect(url)`, nullable handler success, and the `302` route outcome with `Location`; retain existing `400`, `404`, and common `200` semantics.

Coding should update `features/email/README.md` without changing `## Operator Notes`: document HTML registration invites with a labeled anchor, the post-promotion confirmation attempt, optional/failing SMTP behavior, root redirect marker, one-shot toast cleanup, cancellation behavior, and the repeated-open confirmation retry fallback.

Coding should correct only the stale web-client-sub-path statements in `features/common/README.md`, without changing `## Operator Notes`: the current source mounts at `/`, uses `<base href="/">`, and has no `/ui` redirect. No `features/ui/scaffold/README.md` change is needed because the existing `ToastHost` ownership remains unchanged.

## Verification commands

Run focused JVM/common and JS tests for the changed modules, then the repository-wide check if focused tests pass. At minimum, execute the deep-links server tests, email server tests, and client JS tests through their Gradle module tasks. Rebuild `ast-index` after source changes, as required by repository rules.

## Coding handoff

```text
ENTITY:
entity_id=deeplink_handled_contract; type=shared_Kotlin_sealed_hierarchy; state=architecture_ready
entity_id=email_approval_feedback; type=server_email_browser_flow; state=architecture_ready

CONTEXT:
* task_id=02.09.2026_18.17.45-c25c7421-1393-4a22-911a-51e0342451c9; agent_id=architecturing; memory_ref=[001-planning.md,002-architecturing.md]
* constraints=[root_web_mount,preserve_HTTP_400_404_200,preserve_role_idempotency,preserve_cancellation_propagation,keep_SMTP_disabled_graph_resolvable,no_operator_note_changes]

ACTION:
1. action=move_and_expand_result_contract; target=deeplink_handled_contract; params={owner=deeplinks_common,handler_return=HandleResult.Handled?,variants=[Common,Redirect(url)]}
2. action=propagate_result_and_map_HTTP; target=deeplink_handled_contract; params={common_status=200,redirect_status=302,missing_status=404,unhandled_status=404}
3. action=send_labeled_HTML_invite; target=email_approval_feedback; params={transport=sendHtml,label=Verify_email_address,href=absolute_verification_URL,cleanup=preserved}
4. action=approve_confirm_redirect; target=email_approval_feedback; params={redirect=/?emailApproval=approved,confirmation_order=after_promotion,ordinary_delivery_failure=redirect_preserved,cancellation=propagated,repeated_open=confirmation_retried}
5. action=consume_fixed_marker; target=email_approval_feedback; params={entrypoint=ClientJSPlugin.startPlugin,message=Email_has_been_approved,cleanup=history.replaceState,execution_order=before_navigation_startup}
6. action=implement_regression_tests; target=email_approval_feedback; params={layers=[common_contract,dispatcher,HTTP_route,invite_sender,verification_handler,Koin_graph,JS_marker_consumer]}

REASON:
* condition=handler_success_requires_optional_navigation_destination; requirement=common_contract_must_expose_success_subtype; action=return_nullable_Handled; result=server_preserves_exact_success_semantics
* condition=verification_completes_before_confirmation_delivery; requirement=delivery_failure_must_not_rollback_account; action=ignore_false_and_ordinary_failure; result=approval_redirect_remains_successful
* condition=no_durable_confirmation_receipt_exists; requirement=avoid_in_memory_exact_once_state; action=retry_confirmation_on_each_valid_open; result=restart_safe_idempotent_approval_with_retryable_delivery
* condition=fixed_marker_present_at_JS_boot; requirement=single_safe_user_message; action=show_constant_and_remove_marker; result=toast_not_replayed_on_refresh

EXPECTED RESULT:
* entity_id=deeplink_handled_contract; new_state=common_success_or_redirect_propagated_to_HTTP_boundary; location=[features/deeplinks/common,features/deeplinks/server]
* entity_id=email_approval_feedback; new_state=HTML_invite_then_approval_then_confirmation_attempt_then_root_toast; location=[features/email,client/src/jsMain]

VERIFICATION:
* check=route_matrix; expected=[Common:200,Redirect:302_with_exact_Location,NotFound:404,Unhandled:404,blank_id:400]
* check=email_matrix; expected=[invite:HTML_anchor,valid_approval:confirmation_attempt_and_redirect,invalid_payload:no_send_or_redirect,SMTP_failure:approval_and_redirect_preserved]
* check=browser_matrix; expected=[known_marker:one_fixed_toast_and_clean_URL,unknown_marker:no_effect,unrelated_query_and_fragment:preserved]

UNCERTAINTY:
* missing=durable_confirmation_delivery_receipt; ambiguity=exactly_once_delivery_not_required_by_prompt_and_not_supported_by_current_storage

REPETITION OF RESULT:
* entity_id=deeplink_handled_contract; stored_in=shared_memory; status=available_for_coding
* entity_id=email_approval_feedback; stored_in=shared_memory; status=available_for_coding

COMMUNICATION:
* sender=architecturing; receiver=coding; task_id=02.09.2026_18.17.45-c25c7421-1393-4a22-911a-51e0342451c9; message_id=f90e89d3-493f-4f19-9522-2dd01810d962; protocol=AML-HIP

PERSISTENCE:
* local_memory=true; shared_memory=true; index_keys=[task_id,deeplink_handled_contract,email_approval_feedback,architecture_handoff]

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
```
