package dev.inmo.wishlist.features.users.common.repo

import dev.inmo.micro_utils.repos.create
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.Username
import korlibs.time.DateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Mutable DateTime source that also records how many admitted operations sampled the clock.
 * @param value Current injected instant.
 */
internal class EmailTestClock(var value: DateTime = DateTime.fromUnixMillis(1_000L)) {
    /** Number of samples since the last reset. */
    var samples: Int = 0
        private set

    /** Optional successive samples for a batch operation. */
    var queued: List<DateTime> = emptyList()

    /** Returns one injected sample without rounding or translating invalid values. */
    fun sample(): DateTime {
        val sampleIndex = samples++
        return queued.getOrNull(sampleIndex) ?: value
    }

    /** Starts a fresh counted sequence, optionally supplying successive samples. */
    fun reset(next: List<DateTime> = emptyList()) {
        samples = 0
        queued = next
    }
}

/** Invalid application samples used against every changing email entry path. */
internal val invalidEmailClockSamples: List<DateTime> = listOf(
    DateTime(1_000.5),
    DateTime(Double.NaN),
    DateTime(Double.POSITIVE_INFINITY),
    DateTime(Double.NEGATIVE_INFINITY),
    DateTime(4_503_599_627_370_497.0),
    DateTime(-4_503_599_627_370_497.0),
    DateTime(Long.MAX_VALUE.toDouble()),
)

/** Exercises invalid clocks through both database drivers using complete raw rollback snapshots. */
internal suspend fun verifyInvalidEmailClockMatrix(
    url: String,
    repo: ExposedUsersRepo,
    clock: EmailTestClock,
    events: List<RegisteredUser>,
    creationEvents: List<RegisteredUser>,
    settle: suspend () -> Unit,
) {
    val occupied = Email("matrix-occupied@example.com")
    repo.create(NewUser(Username("matrix-occupied"), occupied))
    for (approved in listOf(false, true)) {
        for (futureDeadline in listOf(false, true)) {
            for ((index, invalid) in invalidEmailClockSamples.withIndex()) {
                clock.value = DateTime.fromUnixMillis(1_000L)
                clock.reset()
                val suffix = "${approved}-${futureDeadline}-$index"
                val current = Email("matrix-current-$suffix@example.com")
                val replacement = Email("matrix-replacement-$suffix@example.com")
                val user = repo.create(NewUser(Username("matrix-$suffix"), current)).single()
                when {
                    approved -> {
                        assertNotNull(repo.approveEmail(user.id, current, if (futureDeadline) 5_000L else 0L))
                        clock.value = DateTime.fromUnixMillis(6_000L)
                        assertNotNull(repo.setEmail(user.id, replacement))
                    }
                    futureDeadline -> {
                        // A raw future deadline is valid storage even before first approval.
                        setRawLifecycle(url, user.id.long, requestedAt = 1_000L, allowedAt = 6_000L)
                    }
                }
                settle()
                val before = rawUsersSnapshot(url)
                val eventCount = events.size
                clock.value = invalid
                for (mutation in 0..3) {
                    clock.reset()
                    assertFailsWith<IllegalArgumentException> {
                        when (mutation) {
                            0 -> repo.setEmail(user.id, null)
                            1 -> repo.setEmail(user.id, Email("matrix-new-$suffix@example.com"))
                            2 -> repo.setEmail(user.id, occupied)
                            else -> repo.update(user.id, NewUser(Username("matrix-renamed-$suffix"), null))
                        }
                    }
                    assertEquals(1, clock.samples)
                    settle()
                    assertEquals(before, rawUsersSnapshot(url))
                    assertEquals(eventCount, events.size)
                }
                clock.reset()
                assertFailsWith<IllegalArgumentException> {
                    repo.approveEmail(user.id, if (approved) replacement else current, cooldownMillis = 10L)
                }
                assertEquals(1, clock.samples)
                settle()
                assertEquals(before, rawUsersSnapshot(url))
                assertEquals(eventCount, events.size)
            }
        }
    }

    clock.value = DateTime.fromUnixMillis(1_000L)
    clock.reset()
    val batchFirst = repo.create(NewUser(Username("matrix-batch-first"), Email("matrix-batch-first@example.com"))).single()
    val batchSecond = repo.create(NewUser(Username("matrix-batch-second"), Email("matrix-batch-second@example.com"))).single()
    settle()
    val beforeBatch = rawUsersSnapshot(url)
    val batchEvents = events.size
    clock.reset(listOf(DateTime.fromUnixMillis(7_000L), DateTime(7_000.5)))
    assertFailsWith<IllegalArgumentException> {
        repo.update(listOf(
            batchFirst.id to NewUser(Username("matrix-batch-renamed"), Email("matrix-batch-first-new@example.com")),
            batchSecond.id to NewUser(Username("matrix-batch-second-renamed"), Email("matrix-batch-second-new@example.com")),
        ))
    }
    assertEquals(2, clock.samples)
    settle()
    assertEquals(beforeBatch, rawUsersSnapshot(url))
    assertEquals(batchEvents, events.size)

    val empty = repo.create(NewUser(Username("matrix-empty"))).single()
    val current = Email("matrix-noop@example.com")
    val noOp = repo.create(NewUser(Username("matrix-noop"), current)).single()
    settle()
    val beforeNoOpEvents = events.size
    clock.value = DateTime(Double.NaN)
    clock.reset()
    assertNotNull(repo.setEmail(empty.id, null))
    assertNotNull(repo.setEmail(noOp.id, current))
    settle()
    assertEquals(beforeNoOpEvents, events.size)
    assertNotNull(repo.updateUsername(noOp.id, Username("matrix-noop-renamed")))
    settle()
    assertEquals(beforeNoOpEvents + 1, events.size)
    assertEquals(0, clock.samples)
    clock.value = DateTime.fromUnixMillis(1_000L)
    assertNotNull(repo.approveEmail(noOp.id, current, cooldownMillis = 0L))
    settle()
    assertEquals(beforeNoOpEvents + 2, events.size)
    clock.value = DateTime(Double.NaN)
    clock.reset()
    assertNotNull(repo.approveEmail(noOp.id, current, cooldownMillis = 10L))
    settle()
    assertEquals(beforeNoOpEvents + 2, events.size)
    assertEquals(0, clock.samples)

    clock.value = DateTime.fromUnixMillis(1_000L)
    val pending = Email("matrix-noop-pending@example.com")
    assertNotNull(repo.setEmail(noOp.id, pending))
    settle()
    val pendingEventCount = events.size
    val beforePendingNoOp = rawUsersSnapshot(url)
    clock.value = DateTime(Double.NaN)
    clock.reset()
    assertNotNull(repo.setEmail(noOp.id, pending))
    settle()
    assertEquals(0, clock.samples)
    assertEquals(beforePendingNoOp, rawUsersSnapshot(url))
    assertEquals(pendingEventCount, events.size)

    for ((index, invalid) in invalidEmailClockSamples.withIndex()) {
        clock.value = invalid
        clock.reset()
        val beforeCreate = rawUsersSnapshot(url)
        val priorEvents = events.size
        val priorCreationEvents = creationEvents.size
        assertFailsWith<IllegalArgumentException> {
            repo.create(NewUser(Username("matrix-invalid-create-$index"), Email("matrix-invalid-create-$index@example.com")))
        }
        assertEquals(1, clock.samples)
        settle()
        assertEquals(beforeCreate, rawUsersSnapshot(url))
        assertEquals(priorEvents, events.size)
        assertEquals(priorCreationEvents, creationEvents.size)
    }
}

/** Exercises every invalid changing path through a warmed instrumented cache on one SQL engine. */
internal suspend fun verifyWarmedCacheFailureMatrix(
    url: String,
    backing: ExposedUsersRepo,
    clock: EmailTestClock,
    scope: CoroutineScope,
    settle: suspend () -> Unit,
) {
    val storage = CountingUsersCache()
    val cache = CacheUsersRepo(backing, scope, kvCache = storage)
    settle()
    val backingEvents = mutableListOf<RegisteredUser>()
    val cacheEvents = mutableListOf<RegisteredUser>()
    val backingCollector = scope.launch(start = CoroutineStart.UNDISPATCHED) {
        backing.updatedObjectsFlow.collect(backingEvents::add)
    }
    val cacheCollector = scope.launch(start = CoroutineStart.UNDISPATCHED) {
        cache.updatedObjectsFlow.collect(cacheEvents::add)
    }
    try {
        clock.value = DateTime.fromUnixMillis(1_000L)
        val occupied = Email("cache-matrix-occupied@example.com")
        cache.create(NewUser(Username("cache-matrix-occupied"), occupied))
        for (approved in listOf(false, true)) {
            for (futureDeadline in listOf(false, true)) {
                for ((index, invalid) in invalidEmailClockSamples.withIndex()) {
                    val suffix = "$approved-$futureDeadline-$index"
                    val current = Email("cache-matrix-current-$suffix@example.com")
                    val pending = Email("cache-matrix-pending-$suffix@example.com")
                    clock.value = DateTime.fromUnixMillis(1_000L)
                    clock.reset()
                    val user = cache.create(NewUser(Username("cache-matrix-$suffix"), current)).single()
                    when {
                        approved -> {
                            assertNotNull(cache.approveEmail(user.id, current, if (futureDeadline) 5_000L else 0L))
                            clock.value = DateTime.fromUnixMillis(6_000L)
                            assertNotNull(cache.setEmail(user.id, pending))
                        }
                        futureDeadline -> setRawLifecycle(url, user.id.long, requestedAt = 1_000L, allowedAt = 6_000L)
                    }
                    settle()
                    val before = rawUsersSnapshot(url)
                    val cached = cache.getAll()
                    val writes = storage.setCalls
                    backingEvents.clear()
                    cacheEvents.clear()
                    clock.value = invalid
                    for (mutation in 0..3) {
                        clock.reset()
                        assertFailsWith<IllegalArgumentException> {
                            when (mutation) {
                                0 -> cache.setEmail(user.id, null)
                                1 -> cache.setEmail(user.id, Email("cache-matrix-new-$suffix@example.com"))
                                2 -> cache.setEmail(user.id, occupied)
                                else -> cache.update(user.id, NewUser(Username("cache-matrix-renamed-$suffix"), null))
                            }
                        }
                        assertEquals(1, clock.samples)
                        settle()
                        assertEquals(before, rawUsersSnapshot(url))
                        assertEquals(cached, cache.getAll())
                        assertEquals(writes, storage.setCalls)
                        assertEquals(emptyList(), backingEvents)
                        assertEquals(emptyList(), cacheEvents)
                    }
                    clock.reset()
                    assertFailsWith<IllegalArgumentException> {
                        cache.approveEmail(user.id, if (approved) pending else current, cooldownMillis = 10L)
                    }
                    assertEquals(1, clock.samples)
                    settle()
                    assertEquals(before, rawUsersSnapshot(url))
                    assertEquals(cached, cache.getAll())
                    assertEquals(writes, storage.setCalls)
                    assertEquals(emptyList(), backingEvents)
                    assertEquals(emptyList(), cacheEvents)
                }
            }
        }

        clock.value = DateTime.fromUnixMillis(1_000L)
        val first = cache.create(NewUser(Username("cache-batch-first"), Email("cache-batch-first@example.com"))).single()
        val second = cache.create(NewUser(Username("cache-batch-second"), Email("cache-batch-second@example.com"))).single()
        settle()
        val beforeBatch = rawUsersSnapshot(url)
        val cachedBatch = cache.getAll()
        val batchWrites = storage.setCalls
        backingEvents.clear()
        cacheEvents.clear()
        clock.reset(listOf(DateTime.fromUnixMillis(7_000L), DateTime(7_000.5)))
        assertFailsWith<IllegalArgumentException> {
            cache.update(listOf(
                first.id to NewUser(Username("cache-batch-first-renamed"), Email("cache-batch-first-new@example.com")),
                second.id to NewUser(Username("cache-batch-second-renamed"), Email("cache-batch-second-new@example.com")),
            ))
        }
        assertEquals(2, clock.samples)
        settle()
        assertEquals(beforeBatch, rawUsersSnapshot(url))
        assertEquals(cachedBatch, cache.getAll())
        assertEquals(batchWrites, storage.setCalls)
        assertEquals(emptyList(), backingEvents)
        assertEquals(emptyList(), cacheEvents)

        clock.value = DateTime.fromUnixMillis(1_000L)
        clock.reset()
        val firstEmail = Email("cache-first-overflow@example.com")
        val firstUser = cache.create(NewUser(Username("cache-first-overflow"), firstEmail)).single()
        settle()
        val beforeFirstOverflow = rawUsersSnapshot(url)
        val cachedFirstOverflow = cache.getAll()
        val firstOverflowWrites = storage.setCalls
        val firstRow = beforeFirstOverflow.single { it.id == firstUser.id.long }
        assertEquals(firstEmail.string, firstRow.email, "first-candidate overflow current email")
        assertEquals(false, firstRow.emailApproved, "first-candidate overflow approval state")
        assertNull(firstRow.pendingEmail, "first-candidate overflow pending email")
        assertEquals(1_000L, firstRow.requestedAt, "first-candidate overflow request time")
        assertNull(firstRow.allowedAt, "first-candidate overflow cooldown deadline")
        assertEquals(firstUser, cachedFirstOverflow[firstUser.id], "first-candidate overflow warmed cache")
        backingEvents.clear()
        cacheEvents.clear()
        clock.value = DateTime((4_503_599_627_370_496L - 5L).toDouble())
        clock.reset()
        assertFailsWith<ArithmeticException> {
            cache.approveEmail(firstUser.id, firstEmail, cooldownMillis = 10L)
        }
        assertEquals(1, clock.samples, "first-candidate overflow clock samples")
        settle()
        assertEquals(beforeFirstOverflow, rawUsersSnapshot(url), "first-candidate overflow raw rows")
        assertEquals(cachedFirstOverflow, cache.getAll(), "first-candidate overflow cached users")
        assertEquals(firstOverflowWrites, storage.setCalls, "first-candidate overflow cache writes")
        assertEquals(emptyList(), backingEvents, "first-candidate overflow backing events")
        assertEquals(emptyList(), cacheEvents, "first-candidate overflow cache events")

        val current = Email("cache-overflow-current@example.com")
        val pending = Email("cache-overflow-pending@example.com")
        clock.value = DateTime.fromUnixMillis(1_000L)
        val user = cache.create(NewUser(Username("cache-overflow"), current)).single()
        assertNotNull(cache.approveEmail(user.id, current, cooldownMillis = 0L))
        assertNotNull(cache.setEmail(user.id, pending))
        settle()
        val beforeOverflow = rawUsersSnapshot(url)
        val cachedOverflow = cache.getAll()
        val overflowWrites = storage.setCalls
        backingEvents.clear()
        cacheEvents.clear()
        clock.value = DateTime((4_503_599_627_370_496L - 5L).toDouble())
        clock.reset()
        assertFailsWith<ArithmeticException> { cache.approveEmail(user.id, pending, cooldownMillis = 10L) }
        assertEquals(1, clock.samples)
        settle()
        assertEquals(beforeOverflow, rawUsersSnapshot(url))
        assertEquals(cachedOverflow, cache.getAll())
        assertEquals(overflowWrites, storage.setCalls)
        assertEquals(emptyList(), backingEvents)
        assertEquals(emptyList(), cacheEvents)
    } finally {
        backingCollector.cancel()
        cacheCollector.cancel()
    }
}

/** Proves both application endpoints and exact checked approval arithmetic on one real database. */
internal suspend fun verifyApplicationTimestampEndpoints(
    url: String,
    repo: ExposedUsersRepo,
    clock: EmailTestClock,
    events: List<RegisteredUser>,
    settle: suspend () -> Unit,
) {
    val minimum = -4_503_599_627_370_496L
    val maximum = 4_503_599_627_370_496L
    for ((name, endpoint) in listOf("minimum" to minimum, "maximum" to maximum)) {
        val email = Email("endpoint-$name@example.com")
        clock.value = DateTime(endpoint.toDouble())
        clock.reset()
        val user = repo.create(NewUser(Username("endpoint-$name"), email)).single()
        assertEquals(1, clock.samples)
        assertEquals(endpoint, rawUsersSnapshot(url).single { it.id == user.id.long }.requestedAt)
        assertEquals(DateTime(endpoint.toDouble()), repo.getEmailProfileFresh(user.id)?.emailChangeRequestedAt)
        clock.reset()
        assertNotNull(repo.approveEmail(user.id, email, cooldownMillis = 0L))
        assertEquals(0, clock.samples)
        val approved = rawUsersSnapshot(url).single { it.id == user.id.long }
        assertNull(approved.requestedAt)
        assertNull(approved.allowedAt)
    }

    val overflowEmail = Email("endpoint-overflow@example.com")
    clock.value = DateTime.fromUnixMillis(1_000L)
    val overflowUser = repo.create(NewUser(Username("endpoint-overflow"), overflowEmail)).single()
    settle()
    val before = rawUsersSnapshot(url)
    val eventCount = events.size
    clock.value = DateTime((maximum - 5L).toDouble())
    clock.reset()
    assertFailsWith<ArithmeticException> {
        repo.approveEmail(overflowUser.id, overflowEmail, cooldownMillis = 10L)
    }
    assertEquals(1, clock.samples)
    settle()
    assertEquals(before, rawUsersSnapshot(url))
    assertEquals(eventCount, events.size)

    clock.value = DateTime((maximum - 10L).toDouble())
    clock.reset()
    assertNotNull(repo.approveEmail(overflowUser.id, overflowEmail, cooldownMillis = 10L))
    settle()
    assertEquals(before.single { it.id == overflowUser.id.long }.copy(
        emailApproved = true,
        requestedAt = null,
        allowedAt = maximum,
    ), rawUsersSnapshot(url).single { it.id == overflowUser.id.long })
    assertEquals(DateTime(maximum.toDouble()), repo.getEmailProfileFresh(overflowUser.id)?.emailChangeAllowedAt)
    assertEquals(1, clock.samples)
    assertEquals(eventCount + 1, events.size)
    assertEquals(repo.getById(overflowUser.id), events.last())

    val pendingCurrent = Email("endpoint-pending-current@example.com")
    val pendingReplacement = Email("endpoint-pending-replacement@example.com")
    clock.value = DateTime.fromUnixMillis(1_000L)
    val pendingUser = repo.create(NewUser(Username("endpoint-pending"), pendingCurrent)).single()
    assertNotNull(repo.approveEmail(pendingUser.id, pendingCurrent, cooldownMillis = 0L))
    assertNotNull(repo.setEmail(pendingUser.id, pendingReplacement))
    settle()
    val beforePending = rawUsersSnapshot(url)
    val pendingEventCount = events.size
    clock.value = DateTime((maximum - 5L).toDouble())
    clock.reset()
    assertFailsWith<ArithmeticException> {
        repo.approveEmail(pendingUser.id, pendingReplacement, cooldownMillis = 10L)
    }
    assertEquals(1, clock.samples)
    settle()
    assertEquals(beforePending, rawUsersSnapshot(url))
    assertEquals(pendingEventCount, events.size)

    clock.value = DateTime((maximum - 10L).toDouble())
    clock.reset()
    assertNotNull(repo.approveEmail(pendingUser.id, pendingReplacement, cooldownMillis = 10L))
    assertEquals(1, clock.samples)
    settle()
    val expectedPending = beforePending.single { it.id == pendingUser.id.long }.copy(
        email = pendingReplacement.string,
        emailApproved = true,
        pendingEmail = null,
        requestedAt = null,
        allowedAt = maximum,
    )
    assertEquals(beforePending.map { if (it.id == pendingUser.id.long) expectedPending else it }, rawUsersSnapshot(url))
    assertEquals(DateTime(maximum.toDouble()), repo.getEmailProfileFresh(pendingUser.id)?.emailChangeAllowedAt)
    assertEquals(pendingEventCount + 1, events.size)
    assertEquals(repo.getById(pendingUser.id), events.last())
}

/** Writes raw lifecycle fields for corruption and release-predicate tests. */
internal fun setRawLifecycle(url: String, id: Long, requestedAt: Long?, allowedAt: Long?) {
    java.sql.DriverManager.getConnection(url).use { connection ->
        connection.prepareStatement(
            "UPDATE users SET email_change_requested_at = ?, email_change_allowed_at = ? WHERE id = ?"
        ).use { statement ->
            if (requestedAt == null) statement.setNull(1, java.sql.Types.BIGINT) else statement.setLong(1, requestedAt)
            if (allowedAt == null) statement.setNull(2, java.sql.Types.BIGINT) else statement.setLong(2, allowedAt)
            statement.setLong(3, id)
            assertEquals(1, statement.executeUpdate())
        }
    }
}

/** Proves the release predicate handles nulls, inclusive endpoints, and a doubly invalid row. */
internal suspend fun verifyEmailTimestampScanPredicate(url: String, repo: ExposedUsersRepo) {
    assertEquals(Triple(0L, 0L, 0L), invalidEmailTimestampCounts(url))
    val nullRow = repo.create(NewUser(Username("scan-null"))).single()
    assertEquals(Triple(0L, 0L, 0L), invalidEmailTimestampCounts(url))
    val minimum = repo.create(NewUser(Username("scan-minimum"))).single()
    val maximum = repo.create(NewUser(Username("scan-maximum"))).single()
    val requested = repo.create(NewUser(Username("scan-requested"))).single()
    val allowed = repo.create(NewUser(Username("scan-allowed"))).single()
    val both = repo.create(NewUser(Username("scan-both"))).single()
    setRawLifecycle(url, minimum.id.long, -4_503_599_627_370_496L, -4_503_599_627_370_496L)
    setRawLifecycle(url, maximum.id.long, 4_503_599_627_370_496L, 4_503_599_627_370_496L)
    assertEquals(Triple(0L, 0L, 0L), invalidEmailTimestampCounts(url))
    assertEquals(DateTime(-4_503_599_627_370_496.0), repo.getEmailProfileFresh(minimum.id)?.emailChangeRequestedAt)
    assertEquals(DateTime(-4_503_599_627_370_496.0), repo.getEmailProfileFresh(minimum.id)?.emailChangeAllowedAt)
    assertEquals(DateTime(4_503_599_627_370_496.0), repo.getEmailProfileFresh(maximum.id)?.emailChangeRequestedAt)
    assertEquals(DateTime(4_503_599_627_370_496.0), repo.getEmailProfileFresh(maximum.id)?.emailChangeAllowedAt)
    setRawLifecycle(url, requested.id.long, -4_503_599_627_370_497L, null)
    setRawLifecycle(url, allowed.id.long, null, 4_503_599_627_370_497L)
    setRawLifecycle(url, both.id.long, 4_503_599_627_370_497L, -4_503_599_627_370_497L)
    assertEquals(Triple(2L, 2L, 3L), invalidEmailTimestampCounts(url))
    assertEquals(null, rawUsersSnapshot(url).single { it.id == nullRow.id.long }.requestedAt)
}

/** Exercises raw corruption without decoding it through the users-feature-owned projection. */
internal suspend fun verifyStoredCorruptionMatrix(
    url: String,
    repo: ExposedUsersRepo,
    events: List<RegisteredUser>,
    settle: suspend () -> Unit,
) {
    val corruptValues = listOf(
        -4_503_599_627_370_497L,
        4_503_599_627_370_497L,
        Long.MIN_VALUE,
        Long.MAX_VALUE,
    )
    for (approved in listOf(false, true)) {
        for (requestedColumn in listOf(false, true)) {
            for ((index, raw) in corruptValues.withIndex()) {
                val suffix = "${approved}-${requestedColumn}-$index"
                val current = Email("corrupt-current-$suffix@example.com")
                val pending = Email("corrupt-pending-$suffix@example.com")
                val user = repo.create(NewUser(Username("corrupt-$suffix"), current)).single()
                if (approved) {
                    assertNotNull(repo.approveEmail(user.id, current, cooldownMillis = 0L))
                    assertNotNull(repo.setEmail(user.id, pending))
                }
                val seeded = rawUsersSnapshot(url).single { it.id == user.id.long }
                setRawLifecycle(
                    url, user.id.long,
                    requestedAt = if (requestedColumn) raw else seeded.requestedAt,
                    allowedAt = if (requestedColumn) seeded.allowedAt else raw,
                )
                settle()
                val before = rawUsersSnapshot(url)
                val eventCount = events.size
                assertFailsWith<IllegalStateException> { repo.getEmailProfileFresh(user.id) }
                assertFailsWith<IllegalStateException> { repo.setEmail(user.id, null) }
                assertFailsWith<IllegalStateException> { repo.setEmail(user.id, Email("corrupt-new-$suffix@example.com")) }
                assertFailsWith<IllegalStateException> {
                    repo.approveEmail(user.id, if (approved) pending else current, cooldownMillis = 10L)
                }
                settle()
                assertEquals(before, rawUsersSnapshot(url))
                assertEquals(eventCount, events.size)
                assertNotNull(repo.getById(user.id))
                val renamed = Username("corrupt-renamed-$suffix")
                assertEquals(renamed, repo.updateUsername(user.id, renamed)?.username)
                settle()
                assertEquals(before.map { if (it.id == user.id.long) it.copy(username = renamed.string) else it }, rawUsersSnapshot(url))
                assertEquals(eventCount + 1, events.size)
            }
        }
    }

    val first = repo.create(NewUser(Username("corrupt-batch-first"), Email("corrupt-batch-first@example.com"))).single()
    val second = repo.create(NewUser(Username("corrupt-batch-second"), Email("corrupt-batch-second@example.com"))).single()
    setRawLifecycle(url, second.id.long, Long.MAX_VALUE, Long.MIN_VALUE)
    settle()
    val beforeBatch = rawUsersSnapshot(url)
    val eventCount = events.size
    assertFailsWith<IllegalStateException> {
        repo.update(listOf(
            first.id to NewUser(Username("corrupt-batch-renamed"), Email("corrupt-batch-first-new@example.com")),
            second.id to NewUser(Username("corrupt-batch-second-renamed"), Email("corrupt-batch-second-new@example.com")),
        ))
    }
    settle()
    assertEquals(beforeBatch, rawUsersSnapshot(url))
    assertEquals(eventCount, events.size)
    assertNull(rawUsersSnapshot(url).first { it.id == first.id.long }.allowedAt)
}
