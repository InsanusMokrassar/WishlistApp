package dev.inmo.wishlist.client

import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import dev.inmo.wishlist.features.auth.client.KtorPasswordChangeFeature
import dev.inmo.wishlist.features.auth.client.PasswordChangeFeature
import dev.inmo.wishlist.features.auth.client.utils.PasswordChangeCompletionUrl
import dev.inmo.wishlist.features.auth.common.models.CompletePasswordChangeRequest
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.json
import kotlin.coroutines.CoroutineContext

/**
 * Queues navigation work until a test explicitly runs it while retaining test-scheduler delay handling.
 *
 * @param schedulerDispatcher Dispatcher that supplies deterministic delay and timeout scheduling.
 */
@OptIn(InternalCoroutinesApi::class)
internal class HeldNavigationDispatcher(
    /** Test dispatcher supplying deterministic virtual-time delay and timeout callbacks. */
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
 * @param failFirstSave Causes the first save attempt to fail before retaining a snapshot.
 */
internal class RecordingPasswordNavigationRepo(
    /** Initial hierarchy returned before any test persistence. */
    private val initialHolder: ConfigHolder<ViewConfig>? = null,
    /** Causes the first synchronous save attempt to fail before retaining a snapshot. */
    private val failFirstSave: Boolean = false,
) : NavigationConfigsRepo<ViewConfig> {
    /** Immutable sequence of holders supplied to synchronous save calls. */
    val holders = mutableListOf<ConfigHolder<ViewConfig>>()

    /** Total synchronous save calls, including calls that retain no holder. */
    var saveAttempts = 0
        private set

    /** Records one hierarchy synchronously. */
    override fun save(holder: ConfigHolder<ViewConfig>) {
        saveAttempts += 1
        if (failFirstSave && saveAttempts == 1) error("first save fails")
        holders += when (holder) {
            is ConfigHolder.Chain -> holder.snapshot()
            is ConfigHolder.Node -> holder.snapshot()
        }
    }

    /** Returns the configured initial hierarchy without treating reads as persistence. */
    override fun get(): ConfigHolder<ViewConfig>? = initialHolder

    /** Clears retained hierarchy snapshots and save-attempt counters between assertions. */
    fun resetObservations() {
        holders.clear()
        saveAttempts = 0
    }
}

/** Traverses every node, nested subnode, and subchain configuration in navigation order. */
internal fun ConfigHolder<ViewConfig>.passwordNavigationConfigs(): List<ViewConfig> = when (this) {
    is ConfigHolder.Chain -> firstNodeConfig?.passwordNavigationConfigs().orEmpty()
    is ConfigHolder.Node -> listOf(config) + subnode?.passwordNavigationConfigs().orEmpty() + subchains.flatMap { it.passwordNavigationConfigs() }
}

/** Serializes a polymorphic navigation holder to inspect credential-free persistence output. */
internal fun serializedPasswordNavigationHolder(
    json: Json,
    holder: ConfigHolder<ViewConfig>,
): String = json.encodeToString(ConfigHolder.serializer(PolymorphicSerializer(ViewConfig::class)), holder)

/** Copies a chain holder while retaining immutable config identity for later assertions. */
private fun ConfigHolder.Chain<ViewConfig>.snapshot(): ConfigHolder.Chain<ViewConfig> =
    ConfigHolder.Chain(firstNodeConfig?.snapshot(), id)

/** Copies a node holder recursively without mutating the live navigation hierarchy. */
private fun ConfigHolder.Node<ViewConfig>.snapshot(): ConfigHolder.Node<ViewConfig> =
    ConfigHolder.Node(config, subnode?.snapshot(), subchains.map { it.snapshot() })

/**
 * Holds one MockEngine completion response while recording the exact transport request.
 *
 * @param json JSON instance used to decode captured request bodies and encode held responses.
 * @param completionUrl Trusted absolute endpoint passed to the Ktor feature under test.
 */
internal class HeldPasswordChangeTransport(
    /** JSON instance used to decode captured request bodies and encode held responses. */
    private val json: Json,
    /** Trusted absolute completion endpoint passed to the Ktor feature under test. */
    val completionUrl: PasswordChangeCompletionUrl = PasswordChangeCompletionUrl(
        "https://wishlist.test/api/auth/completePasswordChange",
    ),
) {
    /** Exact decoded completion requests received by the MockEngine. */
    val requests = mutableListOf<CompletePasswordChangeRequest>()

    /** Signals that the MockEngine has accepted a completion request. */
    val requestReceived = CompletableDeferred<Unit>()

    /** Deferred typed result released by the enclosing test. */
    val response = CompletableDeferred<PasswordChangeResult>()

    /** Last raw HTTP request accepted by the MockEngine. */
    var httpRequest: HttpRequestData? = null
        private set

    /** Ktor feature bound to the held MockEngine client. */
    val feature: PasswordChangeFeature

    /** MockEngine client whose lifecycle ends in [close]. */
    private val client: HttpClient

    init {
        client = HttpClient(MockEngine { request ->
            httpRequest = request
            check(request.method == HttpMethod.Post)
            val body = request.body as OutgoingContent.ByteArrayContent
            requests += json.decodeFromString<CompletePasswordChangeRequest>(body.bytes().decodeToString())
            requestReceived.complete(Unit)
            respond(
                content = json.encodeToString(response.await()),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }) {
            install(ContentNegotiation) { json(json) }
        }
        feature = KtorPasswordChangeFeature(client, completionUrl)
    }

    /** Releases the held HTTP response as a successful password change. */
    fun releaseChanged() {
        response.complete(PasswordChangeResult.Changed)
    }

    /** Closes the MockEngine client and releases transport resources. */
    fun close() {
        client.close()
    }
}
