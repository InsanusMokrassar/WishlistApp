package dev.inmo.wishlist.features.common.client.ui.components

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Builds a toast request with a stable test message provider and configurable visibility duration. */
private fun notification(timeout: Long = 100L) = ToastNotification(
    timeout = timeout,
    message = { "test" },
)

/** Starts a collector that records each visible and hidden queue transition. */
private fun TestScope.startCollector(
    queue: ToastQueue,
    transitions: MutableList<ToastNotification?>,
): Job = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
    queue.consume { transitions += it }
}

/** Verifies public publisher compatibility and deterministic queue lifecycle behavior. */
class ToasterTest {
    /** Public publishing overloads retain function-reference compatibility and timeout validation. */
    @Test
    fun publicOverloadsAndTimeoutContractRemainCompatible() {
        val stringCallback: (String) -> Unit = Toaster::show
        val notificationCallback: (ToastNotification) -> Unit = Toaster::show

        assertNotNull(stringCallback)
        assertNotNull(notificationCallback)
        assertEquals(2_600L, ToastNotification(message = { "default" }).timeout)
        assertFailsWith<IllegalArgumentException> {
            ToastNotification(timeout = -1L, message = { "invalid" })
        }
    }

    /** Retained notifications display and hide in order according to individual timeouts. */
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
        assertEquals(listOf<ToastNotification?>(first), transitions)

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

    /** A pre-host notification is delivered once from the replay slot. */
    @Test
    fun preSubscriberNotificationIsDeliveredFromReplay() = runTest {
        val queue = ToastQueue()
        val approval = notification()
        val transitions = mutableListOf<ToastNotification?>()

        queue.enqueue(approval)
        startCollector(queue, transitions)
        testScheduler.runCurrent()

        assertEquals(listOf<ToastNotification?>(approval), transitions)
    }

    /** An acknowledged current notification does not return after the host collector remounts. */
    @Test
    fun acknowledgedCurrentNotificationIsNotReplayedAfterRemount() = runTest {
        val queue = ToastQueue()
        val current = notification()
        val firstMount = mutableListOf<ToastNotification?>()
        queue.enqueue(current)
        val firstJob = startCollector(queue, firstMount)
        testScheduler.runCurrent()
        assertEquals(listOf<ToastNotification?>(current), firstMount)

        firstJob.cancelAndJoin()
        val secondMount = mutableListOf<ToastNotification?>()
        val secondJob = startCollector(queue, secondMount)
        testScheduler.runCurrent()
        assertTrue(secondMount.isEmpty())
        secondJob.cancelAndJoin()
    }

    /** A pending successor remains eligible for the next host after current-host cancellation. */
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
        assertEquals(listOf<ToastNotification?>(pending), secondMount)
        secondJob.cancelAndJoin()
    }

    /** Active bursts retain pending notifications and display each one sequentially. */
    @Test
    fun activeBurstQueuesAllPendingNotifications() = runTest {
        val queue = ToastQueue()
        val current = notification(timeout = 100L)
        val firstPending = notification(timeout = 200L)
        val secondPending = notification(timeout = 300L)
        val transitions = mutableListOf<ToastNotification?>()
        startCollector(queue, transitions)

        queue.enqueue(current)
        testScheduler.runCurrent()
        queue.enqueue(firstPending)
        queue.enqueue(secondPending)

        testScheduler.advanceTimeBy(100L)
        testScheduler.runCurrent()
        assertEquals(listOf(current, null, firstPending), transitions)

        testScheduler.advanceTimeBy(200L)
        testScheduler.runCurrent()
        assertEquals(listOf(current, null, firstPending, null, secondPending), transitions)

        testScheduler.advanceTimeBy(300L)
        testScheduler.runCurrent()
        assertEquals(listOf(current, null, firstPending, null, secondPending, null), transitions)
    }
}
