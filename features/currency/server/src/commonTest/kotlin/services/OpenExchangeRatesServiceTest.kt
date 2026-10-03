package dev.inmo.wishlist.features.currency.server.services

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import korlibs.time.DateTime
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Checks independent OXR cache clocks, expiry, concurrency, and stale fallback. */
class OpenExchangeRatesServiceTest {
    /** The actual default adapter supplies a finite whole-millisecond rates instant. */
    @Test
    fun defaultClockStampsSuccessfulRatesResponse() = runTest {
        val client = HttpClient(MockEngine {
            respond("""{"base":"USD","rates":{"EUR":0.9}}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(ContentNegotiation) { json() } }
        try {
            val before = DateTime.now().unixMillis
            val result = OpenExchangeRatesService("app", client).getRates()!!
            val after = DateTime.now().unixMillis
            val instant = result.fetchedAtMillis.unixMillis
            assertTrue(instant.isFinite() && instant % 1.0 == 0.0)
            assertTrue(instant >= before - 1_000.0 && instant <= after + 1_000.0)
        } finally {
            client.close()
        }
    }

    /** Dictionary age starts after response; concurrent callers share its fetch while rates proceed. */
    @Test
    fun dictionaryResponseTimeConcurrencyAndFailedRetry() = runTest {
        var now = DateTime.fromUnixMillis(100L)
        var dictionaryAttempts = 0
        var ratesAttempts = 0
        var failDictionary = false
        val dictionaryStarted = CompletableDeferred<Unit>()
        val releaseDictionary = CompletableDeferred<Unit>()
        val client = HttpClient(MockEngine { request ->
            if (request.url.encodedPath.endsWith("currencies.json")) {
                dictionaryAttempts++
                if (dictionaryAttempts == 1) {
                    dictionaryStarted.complete(Unit)
                    releaseDictionary.await()
                }
                if (failDictionary) respond("failure", HttpStatusCode.InternalServerError)
                else respond("""{"USD":"US Dollar","EUR":"Euro"}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                ratesAttempts++
                respond("""{"base":"USD","rates":{"EUR":0.9}}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }) { install(ContentNegotiation) { json() } }
        try {
            val service = OpenExchangeRatesService("app", client, ttlMillis = 100L, now = { now })
            val callers = List(8) { async { service.getCurrencies() } }
            dictionaryStarted.await()
            runCurrent()
            assertEquals(1, dictionaryAttempts)
            val rates = async { service.getRates() }
            runCurrent()
            assertEquals(DateTime.fromUnixMillis(100L), rates.await()?.fetchedAtMillis)
            assertEquals(1, ratesAttempts)
            now = DateTime.fromUnixMillis(150L)
            releaseDictionary.complete(Unit)
            assertEquals(1, callers.awaitAll().distinct().size)
            assertEquals(listOf("EUR", "USD"), service.getCurrencies().map { it.code.code })
            now = DateTime.fromUnixMillis(200L)
            service.getCurrencies()
            now = DateTime.fromUnixMillis(249L)
            service.getCurrencies()
            assertEquals(1, dictionaryAttempts)
            now = DateTime.fromUnixMillis(250L)
            failDictionary = true
            assertEquals(listOf("EUR", "USD"), service.getCurrencies().map { it.code.code })
            assertEquals(2, dictionaryAttempts)
            assertEquals(listOf("EUR", "USD"), service.getCurrencies().map { it.code.code })
            assertEquals(3, dictionaryAttempts)
            now = DateTime.fromUnixMillis(251L)
            assertEquals(listOf("EUR", "USD"), service.getCurrencies().map { it.code.code })
            assertEquals(4, dictionaryAttempts)
            failDictionary = false
            service.getCurrencies()
            assertEquals(5, dictionaryAttempts)
            now = DateTime.fromUnixMillis(350L)
            service.getCurrencies()
            assertEquals(5, dictionaryAttempts)
            now = DateTime.fromUnixMillis(351L)
            service.getCurrencies()
            assertEquals(6, dictionaryAttempts)
            now = DateTime.fromUnixMillis(0L)
            service.getCurrencies()
            assertEquals(6, dictionaryAttempts)
        } finally {
            releaseDictionary.complete(Unit)
            client.close()
        }
    }

    /** A disabled service makes no network request. */
    @Test
    fun disabledFeatureMakesNoRequest() = runTest {
        var requests = 0
        val client = HttpClient(MockEngine { requests++; respond("{}") })
        try {
            val service = OpenExchangeRatesService(null, client)
            assertEquals(false, service.isFeatureEnabled())
            assertNull(service.getRates())
            assertEquals(emptyList(), service.getCurrencies())
            assertEquals(0, requests)
        } finally {
            client.close()
        }
    }

    /** Each cache expires at equality; failures retain the last successful instant and answer. */
    @Test
    fun independentExpiryAndFallback() = runTest {
        var now = 1_000L
        var ratesRequests = 0
        var dictionaryRequests = 0
        var failing = false
        val client = HttpClient(MockEngine { request ->
            when {
                failing -> respond("upstream failure", HttpStatusCode.InternalServerError)
                request.url.encodedPath.endsWith("latest.json") -> {
                    ratesRequests++
                    respond("""{"base":"USD","rates":{"EUR":0.9}}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
                }
                else -> {
                    dictionaryRequests++
                    respond("""{"EUR":"Euro","USD":"US Dollar"}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }
        }) { install(ContentNegotiation) { json() } }
        try {
            val service = OpenExchangeRatesService("app", client, ttlMillis = 100L, now = { DateTime.fromUnixMillis(now) })
            val first = service.getRates()!!
            assertEquals(DateTime.fromUnixMillis(1_000L), first.fetchedAtMillis)
            now = 1_050L
            assertEquals(listOf("EUR", "USD"), service.getCurrencies().map { it.code.code })
            now = 1_099L
            assertSame(first, service.getRates())
            assertEquals(1, ratesRequests)
            now = 1_100L
            assertEquals(DateTime.fromUnixMillis(1_100L), service.getRates()!!.fetchedAtMillis)
            assertEquals(2, ratesRequests)
            assertEquals(1, dictionaryRequests)
            now = 1_150L
            service.getCurrencies()
            assertEquals(2, dictionaryRequests)
            now = 1_200L
            failing = true
            val stale = service.getRates()!!
            assertEquals(DateTime.fromUnixMillis(1_100L), stale.fetchedAtMillis)
            assertEquals(0.9, stale.rates["EUR"])
        } finally {
            client.close()
        }
    }

    /** A single mutex protects a cold rates fetch shared by concurrent callers. */
    @Test
    fun concurrentRatesCallersShareFetch() = runTest {
        var requests = 0
        val client = HttpClient(MockEngine {
            requests++
            respond("""{"base":"USD","rates":{"EUR":0.9}}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(ContentNegotiation) { json() } }
        try {
            val service = OpenExchangeRatesService("app", client, now = { DateTime.fromUnixMillis(5L) })
            val results = List(8) { async { service.getRates() } }.awaitAll()
            assertEquals(1, requests)
            assertEquals(1, results.distinct().size)
        } finally {
            client.close()
        }
    }

    /** Failed cold requests return empty answers; a successful fetch stamps after the response. */
    @Test
    fun coldFailureAndPostResponseTimestamp() = runTest {
        var now = 10L
        var failing = true
        val client = HttpClient(MockEngine { request ->
            if (failing) respond("upstream failure", HttpStatusCode.InternalServerError)
            else {
                now = 20L
                val body = if (request.url.encodedPath.endsWith("latest.json")) {
                    """{"base":"USD","rates":{"EUR":0.9}}"""
                } else {
                    """{"EUR":"Euro"}"""
                }
                respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }) { install(ContentNegotiation) { json() } }
        try {
            val service = OpenExchangeRatesService("app", client, ttlMillis = 100L, now = { DateTime.fromUnixMillis(now) })
            assertNull(service.getRates())
            assertEquals(emptyList(), service.getCurrencies())
            failing = false
            assertEquals(DateTime.fromUnixMillis(20L), service.getRates()?.fetchedAtMillis)
            assertEquals(listOf("EUR"), service.getCurrencies().map { it.code.code })
            now = 0L
            assertEquals(DateTime.fromUnixMillis(20L), service.getRates()?.fetchedAtMillis)
        } finally {
            client.close()
        }
    }
}
