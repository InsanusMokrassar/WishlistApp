package dev.inmo.wishlist.client

import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlin.coroutines.CoroutineContext

/**
 * Queues navigation work until a test explicitly runs it while retaining test-scheduler delay handling.
 *
 * @param schedulerDispatcher Dispatcher that supplies deterministic delay and timeout scheduling.
 */
@OptIn(InternalCoroutinesApi::class)
internal class HeldNavigationDispatcher(
    private val schedulerDispatcher: TestDispatcher,
) : CoroutineDispatcher(), Delay by schedulerDispatcher {
    /** FIFO work queue withheld from the owning navigation chain until a test releases work. */
    private val queue = ArrayDeque<Runnable>()

    /** Number of queued tasks that have not yet been released by a test. */
    val queuedTaskCount: Int
        get() = queue.size

    /** Enqueues dispatched work without running it. */
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        queue.addLast(block)
    }

    /** Runs one queued task when available. */
    fun runNext(): Boolean = queue.removeFirstOrNull()?.let {
        it.run()
        true
    } ?: false

    /** Runs queued work until the queue is idle, failing if work continuously reschedules itself. */
    fun drain(maxTasks: Int = 1_000) {
        var executed = 0
        while (runNext()) {
            executed += 1
            check(executed <= maxTasks) { "Held navigation dispatcher exceeded $maxTasks queued tasks" }
        }
    }
}

/**
 * Records synchronous password-navigation persistence attempts without adding production test hooks.
 *
 * @param initialHolder Initial hierarchy returned before any test persistence.
 */
internal class RecordingPasswordNavigationRepo(
    private val initialHolder: ConfigHolder<ViewConfig>? = null,
) : NavigationConfigsRepo<ViewConfig> {
    /** Immutable sequence of holders supplied to synchronous save calls. */
    val holders = mutableListOf<ConfigHolder<ViewConfig>>()

    /** Total synchronous save calls, including calls that retain no holder. */
    var saveAttempts = 0
        private set

    /** Records one hierarchy synchronously. */
    override fun save(holder: ConfigHolder<ViewConfig>) {
        saveAttempts += 1
        holders += holder
    }

    /** Returns the configured initial hierarchy without treating reads as persistence. */
    override fun get(): ConfigHolder<ViewConfig>? = initialHolder
}
