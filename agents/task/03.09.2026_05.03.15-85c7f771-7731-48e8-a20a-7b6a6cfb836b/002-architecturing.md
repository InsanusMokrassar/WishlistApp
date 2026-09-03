Model: OpenAI GPT-5.6 Sol (HL)
Changed files: agents/task/03.09.2026_05.03.15-85c7f771-7731-48e8-a20a-7b6a6cfb836b/002-architecturing.md

The Architecture role prefers an HL model. Although the edit is small, the contract crosses an annotated Compose function type, `StateFlow` equality, and `LaunchedEffect` key identity, so OpenAI GPT-5.6 Sol was used to preserve the compatibility and timer semantics precisely.

## Architectural decision

Keep `Toaster` as the process-wide JS singleton and keep `fun show(text: String)` as the only public publishing function. Change only the stored and exposed message payload from `String?` to a nullable composable string provider. Do not add an overload, abstraction, dependency, queue, toast variant, or caller migration.

Use these exact state declarations:

```kotlin
private val _message = MutableStateFlow<(@Composable () -> String)?>(null)
val message: StateFlow<(@Composable () -> String)?> = _message.asStateFlow()
```

Keep the existing string entry point and replace only its assignment:

```kotlin
fun show(text: String) {
    _message.value = { text }
}
```

The capturing lambda is intentionally created on every `show` call. Two calls carrying equal strings therefore publish different function objects; `MutableStateFlow` does not conflate the second value, recomposition occurs, and the existing `LaunchedEffect(message)` cancels and restarts the dismissal timer.

In `ToastHost`, keep state collection, the nullable guard, effect key, 2600 ms delay, `clear()` call, DOM structure, Calm Studio classes, and success icon unchanged. Replace only the text expression:

```kotlin
Text(message?.invoke() ?: "")
```

Invocation remains inside the `@Composable` host, satisfying the composable-function contract. The `null` value remains the sole hidden-state sentinel, so `clear()` continues assigning `null`, the hidden toast continues rendering an empty text node, and CSS visibility continues depending on `message != null`.

## Compatibility boundary

All six indexed `Toaster.show(String)` calls remain source-compatible, including the `Toaster::show` function reference passed by `ClientJSPlugin`. The single indexed `ToastHost()` mount in `ScaffoldView` remains unchanged. No API outside `Toaster.message` changes type, and no non-JS target is affected because `Toaster.kt` is in `jsMain`.

## Test specification

Add one focused JS state test at `features/common/client/src/jsTest/kotlin/ui/components/ToasterTest.kt`. A test named `repeatedSameTextPublishesFreshProvider` must clear the singleton in setup/finally, call `show("Approved")`, retain the non-null `message.value`, call `show("Approved")` again, and assert that the second non-null provider is not the same reference. The test proves that equal visible text still produces a new `StateFlow` value and preserves the `LaunchedEffect` restart prerequisite. The same test must call `clear()` and assert that `message.value` becomes `null`, preserving the empty-state contract.

No DOM timing test is required for this type-only adaptation: `LaunchedEffect(message)`, `delay(2600)`, `Toaster.clear()`, the class predicate, and the icon subtree remain byte-for-byte unchanged. The production `Text(message?.invoke() ?: "")` expression is the compile-time test of legal composable invocation; the application compilation also checks every existing direct call, the `Toaster::show` function reference, and the sole host mount.

Run these executable gates after the Kotlin changes:

```text
./gradlew :wishlist.features.common.client:jsNodeTest
./gradlew :wishlist.features.common.client:compileKotlinJs :wishlist.client:compileKotlinJs
ast-index rebuild
ast-index refs Toaster
ast-index refs ToastHost
```

Expected results are a passing state test, successful JS compilation for both the defining module and application graph, six indexed `Toaster` usages, and one indexed `ToastHost` usage. All planned behavior is automatable through the focused state test and compile gates; no operator decision about untestable functionality is required.

## README updates

No Common README change is required. The existing Architecture Notes already identify `Toaster` and `ToastHost` as the shared web toast component, while the payload representation is a local Kotlin implementation detail with no new dependency or ownership boundary.

## Architecture handoff

ENTITY:
entity_id=toaster_composable_message_refactor; type=js_client_state_api; state=specified

CONTEXT:

* task_id=03.09.2026_05.03.15-85c7f771-7731-48e8-a20a-7b6a6cfb836b; agent_id=architecturing; memory_ref=[PROMPT.md,001-planning.md,features/common/README.md,Toaster.kt]
* constraints=[source_scope=Toaster.kt,public_publisher=show(String),null_sentinel=preserved,dismiss_delay_ms=2600,caller_changes=none,new_dependencies=false]; indexed_references=[Toaster=6,ToastHost=1]

ACTION:

1. action=change_state_payload; target=Toaster._message_and_message; params={private_type=MutableStateFlow<(@Composable () -> String)?>,public_type=StateFlow<(@Composable () -> String)?>,initial_value=null}
2. action=wrap_string; target=Toaster.show(String); params={signature_unchanged=true,assignment=_message.value={ text },allocation=fresh_provider_per_call}
3. action=invoke_provider; target=ToastHost; params={render_expression=Text(message?.invoke() ?: ""),effect_key=message,visibility_condition=message!=null}
4. action=preserve_behavior; target=ToastHost_and_Toaster.clear; params={delay_ms=2600,terminal_state=null,css_classes=[toast,show],icon=CalmIcons.check}
5. action=verify; target=common_client_js_and_web_client_js; params={state_test=repeatedSameTextPublishesFreshProvider,compile_tasks=[jsNodeTest,compileKotlinJs],index_rebuild=true}

REASON:

* condition=message_payload_requires_composable_provider; requirement=provider_invocation_occurs_inside_Composable_host; condition→action→result=annotated_function_type→store_and_invoke_provider→composition_time_string_available
* condition=equal_text_may_be_published_consecutively; requirement=fresh_lambda_identity_prevents_StateFlow_conflation; condition→action→result=equal_string→allocate_new_provider→dismiss_timer_restarts

EXPECTED RESULT:

* entity_id=toaster_composable_message_refactor; new_state=nullable_composable_provider_is_stored_and_rendered; location=features/common/client/src/jsMain/kotlin/ui/components/Toaster.kt
* entity_id=toaster_string_compatibility; new_state=all_existing_String_callers_compile_without_edits; location=[ClientJSPlugin.kt,BookingView.kt,WishlistView.kt]

VERIFICATION:

* check=common_client_js_node_test; expected=repeated_equal_text_providers_have_distinct_identity_and_clear_restores_null
* check=common_and_application_js_compilation; expected=annotated_state_type_provider_invocation_and_existing_callers_compile
* check=post_change_ast_index; expected={Toaster_usages=6,ToastHost_usages=1,source_change_indexed=true}

UNCERTAINTY:

* missing=[]; ambiguity=none; operator_questions=none

REPETITION OF RESULT:

* entity_id=toaster_composable_message_refactor; stored_in=shared_step_file; status=available_for_coding

COMMUNICATION:

* sender=architecturing; receiver=coding; task_id=03.09.2026_05.03.15-85c7f771-7731-48e8-a20a-7b6a6cfb836b; message_id=ec2915bf-62d2-415f-a052-0e415e39b70d; protocol=AML-HIP

PERSISTENCE:

* local_memory=false; shared_memory=true; index_keys=[task_id,entity_id,intent]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
