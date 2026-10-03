package dev.inmo.wishlist.features.files.server.services

import dev.inmo.micro_utils.ktor.common.TemporalFileId
import korlibs.time.DateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Checks temporal cleanup at an injected wall clock without real-time sleeps. */
@OptIn(ExperimentalCoroutinesApi::class)
class TimedTemporalFilesUtilizerTest {
    /** A pending file survives before TTL and is removed from the map and disk at equality. */
    @Test
    fun expiresAtEqualityAndPreservesFinalizedFiles() = runTest {
        var now = 1_000L
        val files = mutableMapOf<TemporalFileId, File>()
        val newFiles = MutableSharedFlow<TemporalFileId>()
        val utilizer = TimedTemporalFilesUtilizer(backgroundScope, ttlMillis = 100L, checkIntervalMillis = 10L) {
            DateTime.fromUnixMillis(now)
        }
        val pending = File.createTempFile("temporal-pending", ".tmp")
        val finalized = File.createTempFile("temporal-finalized", ".tmp")
        val pendingId = TemporalFileId("pending")
        val finalizedId = TemporalFileId("finalized")
        files[pendingId] = pending
        files[finalizedId] = finalized
        val job = utilizer.start(files, Mutex(), newFiles)
        try {
            runCurrent()
            newFiles.emit(pendingId)
            newFiles.emit(finalizedId)
            runCurrent()
            files.remove(finalizedId)
            now = 1_099L
            advanceTimeBy(10L)
            runCurrent()
            assertTrue(pending.exists())
            assertTrue(files.containsKey(pendingId))
            now = 1_100L
            advanceTimeBy(10L)
            runCurrent()
            assertFalse(files.containsKey(pendingId))
            assertFalse(pending.exists())
            assertTrue(finalized.exists())
        } finally {
            job.cancel()
            pending.delete()
            finalized.delete()
        }
    }

    /** Repeated IDs restart the observed TTL; cancelling the returned job stops sweeping. */
    @Test
    fun repeatedIdRestartsClockAndCancellationStopsSweep() = runTest {
        var now = 0L
        val files = mutableMapOf<TemporalFileId, File>()
        val newFiles = MutableSharedFlow<TemporalFileId>()
        val id = TemporalFileId("repeated")
        val file = File.createTempFile("temporal-repeated", ".tmp")
        files[id] = file
        val job = TimedTemporalFilesUtilizer(backgroundScope, 100L, 10L) { DateTime.fromUnixMillis(now) }
            .start(files, Mutex(), newFiles)
        try {
            runCurrent()
            newFiles.emit(id)
            runCurrent()
            now = 99L
            newFiles.emit(id)
            runCurrent()
            now = 100L
            advanceTimeBy(10L)
            runCurrent()
            assertTrue(files.containsKey(id))
            job.cancel()
            now = 200L
            advanceTimeBy(20L)
            runCurrent()
            assertTrue(files.containsKey(id))
            assertTrue(file.exists())
        } finally {
            job.cancel()
            file.delete()
        }
    }

    /** Cancelling the owning scope stops collection and later expiry sweeps. */
    @Test
    fun parentCancellationStopsCleanup() = runTest {
        var now = 0L
        val parent = Job()
        val scope = CoroutineScope(parent + StandardTestDispatcher(testScheduler))
        val files = mutableMapOf<TemporalFileId, File>()
        val id = TemporalFileId("parent-cancelled")
        val file = File.createTempFile("temporal-parent", ".tmp")
        val newFiles = MutableSharedFlow<TemporalFileId>()
        files[id] = file
        val job = TimedTemporalFilesUtilizer(scope, 100L, 10L) { DateTime.fromUnixMillis(now) }
            .start(files, Mutex(), newFiles)
        try {
            runCurrent()
            newFiles.emit(id)
            runCurrent()
            scope.cancel()
            runCurrent()
            assertTrue(job.isCancelled)
            now = 100L
            advanceTimeBy(10L)
            runCurrent()
            assertTrue(files.containsKey(id))
            assertTrue(file.exists())
        } finally {
            job.cancel()
            file.delete()
        }
    }
}
