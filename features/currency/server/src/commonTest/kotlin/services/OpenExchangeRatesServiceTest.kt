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
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/** Checks independent OXR cache clocks, expiry, concurrency, and stale fallback. */
class OpenExchangeRatesServiceTest {
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
            if (failing) respond("upstream failure", HttpStatusCode.InternalServerError)
            else if (request.url.encodedPath.endsWith("latest.json")) {
                ratesRequests++
                respond("""{"base":"USD","rates":{"EUR":0.9}}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                dictionaryRequests++
                respond("""{"EUR":"Euro","USD":"US Dollar"}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
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
