Model: OpenAI GPT-5.6 Sol (HL)
Changed files: agents/task/03.09.2026_05.03.15-85c7f771-7731-48e8-a20a-7b6a6cfb836b/006-architecturing.md

The Architecture role prefers an HL model. The revised change requires precise reasoning about `SharedFlow` replay and overflow, Compose-effect cancellation, startup ordering, and virtual-time tests, so OpenAI GPT-5.6 Sol was used to define guarantees without overstating queue durability.

## Architectural decision

Introduce this public payload in `Toaster.kt`:

```kotlin
/**
 * One toast request.
 *
 * @property message Text provider invoked only from composition.
 * @property timeout Visibility duration in milliseconds; zero requests immediate hiding.
 */
data class ToastNotification(
    val message: @Composable () -> String,
    val timeout: Long = 2_600L,
) {
    init {
        require(timeout >= 0L) { "timeout must be non-negative milliseconds" }
    }
}
```

`ToastNotification` is preferable to `ToastMessage`: the object represents message content plus display policy. The user-requested `timeout` property name is retained, while KDoc and validation make milliseconds explicit. `Long` matches the existing `delay(2600)` API and avoids a new duration abstraction. The default remains exactly 2600 ms; negative values fail at construction, and zero produces an immediate show/hide transition.

Keep both public publishing forms and add no lambda-only convenience overload:

```kotlin
fun show(notification: ToastNotification) {
    queue.enqueue(notification)
}

fun show(text: String) {
    show(ToastNotification(message = { text }))
}
```

`show(String): Unit` remains source-compatible. Expected-type overload resolution keeps `val callback: (String) -> Unit = Toaster::show` valid in `ClientJSPlugin`; the application compile gate proves that contract. `show(ToastNotification): Unit` is the explicit custom-message/custom-timeout API.

## SharedFlow configuration and guarantees

Use a small internal queue owner so tests receive fresh flow state while the public singleton keeps one process-wide instance:

```kotlin
internal class ToastQueue {
    private val notifications = MutableSharedFlow<ToastNotification>(
        replay = 1,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun enqueue(notification: ToastNotification) {
        check(notifications.tryEmit(notification))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun consume(onCurrentChanged: (ToastNotification?) -> Unit) {
        notifications.collect { notification ->
            notifications.resetReplayCache()
            onCurrentChanged(notification)
            delay(notification.timeout)
            onCurrentChanged(null)
        }
    }
}

object Toaster {
    private val queue = ToastQueue()

    fun show(notification: ToastNotification) {
        queue.enqueue(notification)
    }

    fun show(text: String) {
        show(ToastNotification(message = { text }))
    }

    internal suspend fun consume(onCurrentChanged: (ToastNotification?) -> Unit) {
        queue.consume(onCurrentChanged)
    }
}
```

The configuration is deliberately `replay = 1`, `extraBufferCapacity = 0`, and `DROP_OLDEST`. Replay one is required because `ClientJSPlugin.startPlugin` publishes the approval message before `renderComposable` can mount `ScaffoldView` and `ToastHost`. Zero extra capacity avoids an arbitrary retention number. With no subscriber, only the newest notification is retained; `extraBufferCapacity` and overflow policy are inactive by `SharedFlow` contract. With the single host subscriber busy displaying one notification, the one-slot buffer retains at most one pending notification. A further emission drops that oldest pending notification and retains the newest one. `DROP_OLDEST` also makes `tryEmit` non-suspending and successful for this valid positive-capacity configuration; the `check` documents and protects that configuration invariant.

This is a bounded, lossy FIFO for retained notifications, not a lossless work queue. Retained values are displayed in emission order. During overflow, dropped values have no display guarantee. Before any subscriber exists, multiple emissions collapse to the newest replay value. `SharedFlow` is broadcast by definition, so exactly one production collector is an architectural invariant; the mutable flow remains private and only the single scaffold-mounted host calls `Toaster.consume`.

The planning proposal's fifteen extra slots is rejected because no product requirement supplies a queue depth, and the number would create an unsupported durability expectation. If lossless arbitrary bursts become a requirement, the API must gain backpressure or a different queue primitive; increasing an unexplained `SharedFlow` buffer is not sufficient.

## Host lifecycle and replay acknowledgement

Replace collected `StateFlow` state with host-local state and one effect keyed by `Unit`:

```kotlin
@Composable
fun ToastHost() {
    var current by remember { mutableStateOf<ToastNotification?>(null) }

    LaunchedEffect(Unit) {
        Toaster.consume { current = it }
    }

    Div({
        if (current != null) {
            classes(CalmStudioStyleSheet.toast, CalmStudioStyleSheet.show)
        } else {
            classes(CalmStudioStyleSheet.toast)
        }
    }) {
        Span({ classes(CalmStudioStyleSheet.ok) }) { CalmIcon(CalmIcons.check) }
        Text(current?.message?.invoke() ?: "")
    }
}
```

The single `collect` callback does not return until the current timeout ends, so processing is sequential. The existing toast classes, success icon, null visibility sentinel, and empty fallback remain unchanged. A buffered successor may replace `null` in the same scheduler turn after the preceding timeout; the design guarantees an ordered logical hide transition, not a separately painted inter-toast gap.

`resetReplayCache()` is called immediately after dequeue and before state mutation or any suspension. In kotlinx-coroutines 1.11.0 the API is experimental, so the narrow consuming function must opt in with `@OptIn(ExperimentalCoroutinesApi::class)`. Resetting the replay cache only prevents future subscribers from receiving acknowledged replay values; existing subscribers can still receive values already buffered for them. The call does not cancel, complete, or globally drain the `SharedFlow`.

Cancellation before the collector callback begins leaves a not-yet-acknowledged replay value eligible for a later host. Cancellation after `resetReplayCache()` discards the current notification: the old host is leaving composition, the local rendered state disappears with that host, and a remounted host must not replay the acknowledged notification. If a newer notification arrived while the old host was delaying, the newer notification occupies the replay slot and remains eligible for the remounted host. No guarantee is made for more than one pending notification, because the configured capacity is one.

Remove the superseded public `message: StateFlow<...>` and `clear()` API. Indexed production references show that only `ToastHost` consumes `message`/`clear`; all external producers use `show`. Encapsulating collection is necessary to preserve the acknowledgement and single-collector invariants.

## Compilable test specification

Replace the state-oriented JS test with deterministic `kotlinx-coroutines-test` cases around fresh `ToastQueue` instances. `commonTest` already supplies `libs.kotlin.coroutines.test`, `jsTest` already supplies `kotlin('test-js')`, and the default source-set hierarchy makes both available to `features/common/client/src/jsTest`; no build-file change is required.

Use this shared test scaffold:

```kotlin
private fun notification(timeout: Long = 100L) = ToastNotification(
    message = { "test" },
    timeout = timeout,
)

private fun TestScope.startCollector(
    queue: ToastQueue,
    transitions: MutableList<ToastNotification?>,
): Job = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
    queue.consume { transitions += it }
}
```

The following stubs define the required executable behavior:

```kotlin
@Test
fun publicOverloadsAndTimeoutContractRemainCompatible() {
    val stringCallback: (String) -> Unit = Toaster::show
    val notificationCallback: (ToastNotification) -> Unit = Toaster::show

    assertNotNull(stringCallback)
    assertNotNull(notificationCallback)
    assertEquals(2_600L, ToastNotification(message = { "default" }).timeout)
    assertFailsWith<IllegalArgumentException> {
        ToastNotification(message = { "invalid" }, timeout = -1L)
    }
}

@Test
fun retainedNotificationsDisplaySequentiallyForOwnTimeouts() = runTest {
    val queue = ToastQueue()
    val first = notification(timeout = 100L)
    val second = notification(timeout = 200L)
    val transitions = mutableListOf<ToastNotification?>()
    startCollector(queue, transitions)

    queue.enqueue(first)
    testScheduler.runCurrent()
    queue.enqueue(second)

    testScheduler.advanceTimeBy(99L)
    testScheduler.runCurrent()
    assertEquals(listOf(first), transitions)

    testScheduler.advanceTimeBy(1L)
    testScheduler.runCurrent()
    assertEquals(listOf(first, null, second), transitions)

    testScheduler.advanceTimeBy(199L)
    testScheduler.runCurrent()
    assertEquals(listOf(first, null, second), transitions)

    testScheduler.advanceTimeBy(1L)
    testScheduler.runCurrent()
    assertEquals(listOf(first, null, second, null), transitions)
}

@Test
fun preSubscriberNotificationIsDeliveredFromReplay() = runTest {
    val queue = ToastQueue()
    val approval = notification()
    val transitions = mutableListOf<ToastNotification?>()

    queue.enqueue(approval)
    startCollector(queue, transitions)
    testScheduler.runCurrent()

    assertEquals(listOf(approval), transitions)
}

@Test
fun acknowledgedCurrentNotificationIsNotReplayedAfterRemount() = runTest {
    val queue = ToastQueue()
    val current = notification()
    val firstMount = mutableListOf<ToastNotification?>()
    queue.enqueue(current)
    val firstJob = startCollector(queue, firstMount)
    testScheduler.runCurrent()
    assertEquals(listOf(current), firstMount)

    firstJob.cancelAndJoin()
    val secondMount = mutableListOf<ToastNotification?>()
    val secondJob = startCollector(queue, secondMount)
    testScheduler.runCurrent()
    assertTrue(secondMount.isEmpty())
    secondJob.cancelAndJoin()
}

@Test
fun newerPendingNotificationSurvivesCurrentHostCancellation() = runTest {
    val queue = ToastQueue()
    val current = notification()
    val pending = notification()
    val firstMount = mutableListOf<ToastNotification?>()
    queue.enqueue(current)
    val firstJob = startCollector(queue, firstMount)
    testScheduler.runCurrent()
    queue.enqueue(pending)

    firstJob.cancelAndJoin()
    val secondMount = mutableListOf<ToastNotification?>()
    val secondJob = startCollector(queue, secondMount)
    testScheduler.runCurrent()
    assertEquals(listOf(pending), secondMount)
    secondJob.cancelAndJoin()
}

@Test
fun overflowDropsOldestPendingNotification() = runTest {
    val queue = ToastQueue()
    val current = notification(timeout = 100L)
    val dropped = notification()
    val retained = notification()
    val transitions = mutableListOf<ToastNotification?>()
    startCollector(queue, transitions)

    queue.enqueue(current)
    testScheduler.runCurrent()
    queue.enqueue(dropped)
    queue.enqueue(retained)

    testScheduler.advanceTimeBy(100L)
    testScheduler.runCurrent()
    assertEquals(listOf(current, null, retained), transitions)
    assertFalse(transitions.contains(dropped))
}
```

The Coding agent should add the imports for `CoroutineStart`, `Job`, `cancelAndJoin`, `launch`, `TestScope`, `runTest`, and the listed assertions. Production compilation of `Text(current?.message?.invoke() ?: "")` proves legal composable invocation; the JS application compile proves all five direct string calls, the email-approval function reference, and the single host mount.

Run:

```text
./gradlew :wishlist.features.common.client:jsNodeTest
./gradlew :wishlist.client:compileKotlinJs
ast-index rebuild
ast-index refs Toaster
ast-index refs ToastHost
```

All revised behavior has deterministic automated coverage; no operator decision about untestable functionality is required.

## README updates

Update the Common README Architecture Notes to state that the web toaster accepts `ToastNotification(message, timeout)` with millisecond timeouts, retains only one replay/pending notification, drops the oldest pending value on overflow, consumes retained values sequentially from one scaffold host, and acknowledges each dequeued replay value before suspension so an already displayed notification is not replayed after remount. Do not alter Operator Notes.

## Architecture handoff

ENTITY:
entity_id=toast_notification_queue_architecture; type=bounded_shared_flow_ui_queue; state=specified

CONTEXT:

* task_id=03.09.2026_05.03.15-85c7f771-7731-48e8-a20a-7b6a6cfb836b; agent_id=architecturing; memory_ref=[PROMPT.md,005-planning.md,Toaster.kt,ToasterTest.kt,ClientJSPlugin.kt,ScaffoldView.kt,features/common/README.md]
* constraints=[payload=ToastNotification,timeout_unit=milliseconds,default_timeout=2600,public_overloads=[show(String),show(ToastNotification)],subscriber_count=1]; dependency_versions=[kotlin=2.3.21,kotlinx_coroutines=1.11.0,compose=1.11.1]

ACTION:

1. action=define_payload; target=ToastNotification; params={visibility=public,fields=[message:@Composable_()_to_String,timeout:Long],default_timeout=2600,negative_timeout=IllegalArgumentException,zero_timeout=immediate_hide}
2. action=configure_queue; target=ToastQueue.notifications; params={type=MutableSharedFlow<ToastNotification>,replay=1,extra_buffer_capacity=0,on_buffer_overflow=DROP_OLDEST,active_pending_capacity=1}
3. action=preserve_publishers; target=Toaster; params={string_signature=show(String):Unit,notification_signature=show(ToastNotification):Unit,string_mapping=ToastNotification(message={ text }),emission=tryEmit}
4. action=consume_sequentially; target=ToastQueue.consume_and_ToastHost; params={collector=single_LaunchedEffect_Unit,transition_order=[show,delay,hide],delay_source=ToastNotification.timeout,current_state=host_local}
5. action=acknowledge_replay; target=ToastQueue.consume; params={operation=resetReplayCache,position=immediately_after_dequeue_before_suspension,opt_in=ExperimentalCoroutinesApi,new_subscriber_effect=no_acknowledged_replay,existing_subscriber_effect=buffer_unchanged}
6. action=replace_tests; target=ToasterTest; params={cases=[public_overloads,timeout_contract,FIFO_retained_order,per_item_timeout,pre_subscriber_replay,no_stale_remount,pending_survives_remount,drop_oldest_overflow],clock=kotlinx_coroutines_test_virtual_time}

REASON:

* condition=approval_event_precedes_host_subscription; requirement=replay_one_retains_latest_pre_host_event; condition→action→result=pre_host_emit→replay_cache_store→first_host_delivery
* condition=non_suspending_Unit_publisher_plus_slow_consumer; requirement=bounded_overflow_policy_without_arbitrary_depth; condition→action→result=third_active_burst_value→drop_oldest_pending→newest_pending_retained
* condition=host_remount_after_current_dequeue; requirement=acknowledged_current_value_not_replayed; condition→action→result=dequeue→reset_replay_before_suspend→remount_excludes_current

EXPECTED RESULT:

* entity_id=ToastNotification; new_state=public_composable_message_plus_millisecond_timeout_payload; location=features/common/client/src/jsMain/kotlin/ui/components/Toaster.kt
* entity_id=toast_notification_queue_architecture; new_state=single_consumer_bounded_lossy_FIFO_with_pre_host_latest_replay; location=features/common/client/src/jsMain/kotlin/ui/components/Toaster.kt
* entity_id=toaster_queue_tests; new_state=virtual_time_lifecycle_order_timeout_and_overflow_coverage; location=features/common/client/src/jsTest/kotlin/ui/components/ToasterTest.kt

VERIFICATION:

* check=common_client_js_node_tests; expected=all_queue_timeout_compatibility_replay_remount_overflow_cases_pass
* check=web_client_js_compile; expected=existing_String_publishers_and_composable_host_compile_without_caller_edits
* check=post_change_ast_index; expected={Toaster_production_publishers=6,ToastHost_mounts=1,single_Toaster_consumer=1}

UNCERTAINTY:

* missing=[]; ambiguity=none; queue_guarantee=retained_values_only; lossless_delivery=false

REPETITION OF RESULT:

* entity_id=toast_notification_queue_architecture; stored_in=shared_step_file; status=available_for_coding

COMMUNICATION:

* sender=architecturing; receiver=coding; task_id=03.09.2026_05.03.15-85c7f771-7731-48e8-a20a-7b6a6cfb836b; message_id=dd31f278-555c-448c-8233-b0928c0f0920; protocol=AML-HIP

PERSISTENCE:

* local_memory=false; shared_memory=true; index_keys=[task_id,entity_id,intent]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
