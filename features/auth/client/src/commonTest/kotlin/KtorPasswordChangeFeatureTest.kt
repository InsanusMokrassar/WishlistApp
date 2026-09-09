package dev.inmo.wishlist.features.auth.client

import dev.inmo.wishlist.features.auth.client.configurators.BearerAuthHttpClientConfigurator
import dev.inmo.wishlist.features.auth.client.configurators.DefaultUrlHttpClientConfigurator
import dev.inmo.wishlist.features.auth.client.utils.PasswordChangeCompletionUrl
import dev.inmo.wishlist.features.auth.client.utils.skipDefaultServerUrl
import dev.inmo.wishlist.features.auth.common.models.AuthCredentials
import dev.inmo.wishlist.features.auth.common.models.CompletePasswordChangeRequest
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequestResult
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult
import dev.inmo.wishlist.features.auth.common.models.RefreshToken
import dev.inmo.wishlist.features.auth.common.models.Token
import dev.inmo.wishlist.features.common.common.Plugin as CommonPlugin
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.UserId
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.auth.AuthCircuitBreaker
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import org.koin.core.KoinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Recording credentials storage that exposes every Auth-plugin storage interaction to assertions. */
private class RecordingCredentialsStorage(
    /** Initial browser credentials, when the test models an unrelated signed-in account. */
    initialCredentials: AuthCredentials? = null,
) : AuthCredentialsStorage {
    /** Reactive authorization state required by the production storage interface. */
    private val authorised = MutableStateFlow(initialCredentials != null)

    /** Mutable stored credentials behind the public read operation. */
    private var storedCredentials: AuthCredentials? = initialCredentials

    /** Number of credential reads requested by the production bearer configurator. */
    var getCalls: Int = 0
        private set

    /** Number of credential writes requested by transport behavior. */
    var saveCalls: Int = 0
        private set

    /** Read-only authorization flow consumed by normal client startup. */
    override val userAuthorised: StateFlow<Boolean> = authorised

    /** Returns stored credentials and records one bearer-plugin storage read. */
    override suspend fun get(): AuthCredentials? {
        getCalls++
        return storedCredentials
    }

    /** Stores credentials and records one transport-owned write. */
    override suspend fun save(credentials: AuthCredentials?) {
        saveCalls++
        storedCredentials = credentials
        authorised.value = credentials != null
    }
}

/** Recording saved-server URL storage used by the production default-URL configurator. */
private class RecordingServerUrlStorage(
    /** Initial saved server URL, including hostile user-info/path/query/fragment test values. */
    initialUrl: String?,
) : ServerUrlStorage {
    /** Mutable saved URL returned to each unmarked request. */
    private var storedUrl: String? = initialUrl

    /** Number of reads performed by the default-URL configurator. */
    var getCalls: Int = 0
        private set

    /** Number of writes performed through the storage interface. */
    var saveCalls: Int = 0
        private set

    /** Returns the saved URL and records one request-level lookup. */
    override suspend fun getServerUrl(): String? {
        getCalls++
        return storedUrl
    }

    /** Replaces the saved URL and records one explicit persistence write. */
    override suspend fun saveServerUrl(url: String?) {
        saveCalls++
        storedUrl = url
    }
}

/** Verifies password-change transport keeps request identities exact and failures fail closed. */
class KtorPasswordChangeFeatureTest {
    /** An authenticated request carries only the expected current email and decodes typed outcomes. */
    @Test
    fun requestPostsExpectedEmailAndDecodesSuccessfulOutcome() = runTest {
        var body = ""
        var observedPath = ""
        var observedMethod: HttpMethod? = null
        val client = passwordChangeJsonClient { request ->
            observedMethod = request.method
            observedPath = request.url.encodedPath
            body = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            respondPasswordChangeJson("\"Sent\"")
        }
        try {
            assertEquals(
                PasswordChangeEmailRequestResult.Sent,
                KtorPasswordChangeFeature(client).requestPasswordChangeEmail(Email("owner@example.com")),
                "method=$observedMethod path=$observedPath body=$body",
            )
            assertEquals(HttpMethod.Post, observedMethod)
            assertEquals("/auth/requestPasswordChangeEmail", observedPath)
            assertEquals("{\"expectedEmail\":\"owner@example.com\"}", body)
        } finally {
            client.close()
        }
    }

    /** Browser completion uses its issuing origin, skips bearer refresh, and preserves the UUID verbatim. */
    @Test
    fun completionUsesIssuingOriginAndExactApprovalBody() = runTest {
        var body = ""
        var observedUrl = ""
        var hasCircuitBreaker = false
        var hasUrlOptOut = false
        val client = passwordChangeJsonClient { request ->
            observedUrl = request.url.toString()
            hasCircuitBreaker = request.attributes.contains(AuthCircuitBreaker)
            hasUrlOptOut = request.attributes.contains(skipDefaultServerUrl)
            body = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            respondPasswordChangeJson("\"Changed\"")
        }
        val request = CompletePasswordChangeRequest(
            userId = UserId(7L),
            approvalId = DeepLinkId("123e4567-e89b-42d3-a456-426614174000"),
            password = Password("new-password"),
        )
        try {
            assertEquals(
                PasswordChangeResult.Changed,
                KtorPasswordChangeFeature(
                    client,
                    PasswordChangeCompletionUrl("https://approval.example/api/auth/completePasswordChange"),
                ).completePasswordChange(request),
                "url=$observedUrl circuitBreaker=$hasCircuitBreaker urlOptOut=$hasUrlOptOut body=$body",
            )
            assertTrue(observedUrl.startsWith("https://approval.example/api/auth/completePasswordChange"))
            assertTrue(hasCircuitBreaker)
            assertTrue(hasUrlOptOut)
            assertEquals(
                "{\"userId\":7,\"approvalId\":\"123e4567-e89b-42d3-a456-426614174000\",\"password\":\"new-password\"}",
                body,
            )
        } finally {
            client.close()
        }
    }

    /** Non-success statuses, malformed JSON, and unknown result values cannot claim a changed password. */
    @Test
    fun completionFailuresReturnNoOutcome() = runTest {
        listOf(
            HttpStatusCode.BadRequest to "\"Changed\"",
            HttpStatusCode.OK to "not-json",
            HttpStatusCode.OK to "\"FutureResult\"",
        ).forEach { (status, content) ->
            val client = passwordChangeJsonClient { respondPasswordChangeJson(content, status) }
            try {
                assertNull(KtorPasswordChangeFeature(client).completePasswordChange(validRequest()))
            } finally {
                client.close()
            }
        }
    }

    /** Browser-origin completion never applies absent or unrelated browser credentials, including a 401 response. */
    @Test
    fun browserCompletionSkipsCredentialsAndDefaultUrlForAbsentOrUnrelatedSessions() = runTest {
        listOf(
            null to HttpStatusCode.OK,
            AuthCredentials(Token("unrelated-access"), RefreshToken("unrelated-refresh")) to HttpStatusCode.Unauthorized,
        ).forEach { (credentials, status) ->
            val credentialStorage = RecordingCredentialsStorage(credentials)
            val serverUrlStorage = RecordingServerUrlStorage(
                "https://hostile-user:hostile-password@other.example:8443/saved/path?sentinel=query#fragment",
            )
            val requests = mutableListOf<HttpRequestData>()
            val client = passwordChangeJsonClient(credentialStorage, serverUrlStorage) { request ->
                requests += request
                respondPasswordChangeJson("\"Changed\"", status)
            }
            try {
                val result = KtorPasswordChangeFeature(
                    client,
                    PasswordChangeCompletionUrl("https://issuing.example/api/auth/completePasswordChange"),
                ).completePasswordChange(validRequest())
                when (status) {
                    HttpStatusCode.OK -> assertEquals(PasswordChangeResult.Changed, result)
                    else -> assertNull(result)
                }
                assertEquals(1, requests.size)
                assertEquals("https://issuing.example/api/auth/completePasswordChange", requests.single().url.toString())
                assertNull(requests.single().headers[HttpHeaders.Authorization])
                assertEquals(1, credentialStorage.getCalls)
                assertEquals(0, credentialStorage.saveCalls)
                assertEquals(0, serverUrlStorage.getCalls)
                assertEquals(0, serverUrlStorage.saveCalls)
            } finally {
                client.close()
            }
        }
    }

    /** Browser completion retains exact subject, approval, and untrimmed plaintext while ignoring a hostile saved URL. */
    @Test
    fun browserCompletionUsesExactIssuingEndpointAndPayloadDespiteHostileSavedUrl() = runTest {
        val credentialStorage = RecordingCredentialsStorage(AuthCredentials(Token("other"), RefreshToken("refresh")))
        val serverUrlStorage = RecordingServerUrlStorage(
            "https://hostile-user:hostile-password@other.example:8443/saved/path?sentinel=query#fragment",
        )
        var observedUrl = ""
        var decodedRequest: CompletePasswordChangeRequest? = null
        val request = validRequest().copy(password = Password("  intentionally untrimmed password  "))
        val client = passwordChangeJsonClient(credentialStorage, serverUrlStorage) { httpRequest ->
            observedUrl = httpRequest.url.toString()
            val body = (httpRequest.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            decodedRequest = productionJson().decodeFromString(body)
            respondPasswordChangeJson("\"Changed\"")
        }
        try {
            assertEquals(
                PasswordChangeResult.Changed,
                KtorPasswordChangeFeature(
                    client,
                    PasswordChangeCompletionUrl("https://issuing.example/api/auth/completePasswordChange"),
                ).completePasswordChange(request),
            )
            assertEquals("https://issuing.example/api/auth/completePasswordChange", observedUrl)
            val decoded = requireNotNull(decodedRequest)
            assertEquals(request.userId, decoded.userId)
            assertEquals(request.approvalId, decoded.approvalId)
            assertTrue(decoded.password.string.contentEquals(request.password.string))
            assertEquals(1, credentialStorage.getCalls)
            assertEquals(0, credentialStorage.saveCalls)
            assertEquals(0, serverUrlStorage.getCalls)
        } finally {
            client.close()
        }
    }

    /** Unmarked issuance and completion without browser binding preserve the configured server URL semantics. */
    @Test
    fun ordinaryIssuanceAndNativeCompletionRetainSavedServerUrlSemantics() = runTest {
        val credentialStorage = RecordingCredentialsStorage(AuthCredentials(Token("owner-access"), RefreshToken("owner-refresh")))
        val serverUrlStorage = RecordingServerUrlStorage(
            "https://saved-user:saved-password@saved.example:8443/tenant?from=saved#saved-fragment",
        )
        val requests = mutableListOf<HttpRequestData>()
        val client = passwordChangeJsonClient(credentialStorage, serverUrlStorage) { request ->
            requests += request
            when (request.url.encodedPath) {
                "/api/tenant/auth/requestPasswordChangeEmail" -> respondPasswordChangeJson("\"Sent\"")
                "/api/tenant/auth/completePasswordChange" -> respondPasswordChangeJson("\"Changed\"")
                else -> error("Unexpected path ${request.url.encodedPath}")
            }
        }
        try {
            val feature = KtorPasswordChangeFeature(client)
            assertEquals(PasswordChangeEmailRequestResult.Sent, feature.requestPasswordChangeEmail(Email("owner@example.com")))
            assertEquals(PasswordChangeResult.Changed, feature.completePasswordChange(validRequest()))
            assertEquals(2, requests.size)
            val issuance = requests[0]
            val completion = requests[1]
            listOf(issuance, completion).forEach { request ->
                assertEquals("saved.example", request.url.host)
                assertEquals(8443, request.url.port)
                assertEquals("saved-user", request.url.user)
                assertEquals("saved-password", request.url.password)
                assertEquals("saved", request.url.parameters["from"])
                assertEquals("saved-fragment", request.url.fragment)
            }
            assertEquals("Bearer owner-access", issuance.headers[HttpHeaders.Authorization])
            assertNull(completion.headers[HttpHeaders.Authorization])
            assertEquals(2, credentialStorage.getCalls)
            assertEquals(0, credentialStorage.saveCalls)
            assertEquals(2, serverUrlStorage.getCalls)
        } finally {
            client.close()
        }
    }

    /** Cancellation remains observable to callers instead of becoming a null transport outcome. */
    @Test
    fun completionCancellationPropagates() = runTest {
        val client = passwordChangeJsonClient(
            RecordingCredentialsStorage(AuthCredentials(Token("other"), RefreshToken("refresh"))),
            RecordingServerUrlStorage("https://saved.example"),
        ) {
            throw CancellationException("completion-cancelled")
        }
        try {
            assertFailsWith<CancellationException> {
                KtorPasswordChangeFeature(
                    client,
                    PasswordChangeCompletionUrl("https://issuing.example/api/auth/completePasswordChange"),
                ).completePasswordChange(validRequest())
            }
        } finally {
            client.close()
        }
    }

    /** Builds the one valid representative completion DTO used by status-failure tests. */
    private fun validRequest(): CompletePasswordChangeRequest = CompletePasswordChangeRequest(
        userId = UserId(7L),
        approvalId = DeepLinkId("123e4567-e89b-42d3-a456-426614174000"),
        password = Password("new-password"),
    )
}

/** Builds a MockEngine client with production JSON, bearer, and saved-server configurators. */
private fun passwordChangeJsonClient(
    credentialsStorage: RecordingCredentialsStorage = RecordingCredentialsStorage(),
    serverUrlStorage: RecordingServerUrlStorage = RecordingServerUrlStorage(null),
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData,
): HttpClient = HttpClient(MockEngine(handler)) {
    install(ContentNegotiation) { json(productionJson()) }
    with(DefaultUrlHttpClientConfigurator(serverUrlStorage)) { configure() }
    with(BearerAuthHttpClientConfigurator(credentialsStorage)) { configure() }
}

/** Resolves the real Common plugin JSON configuration without replacing production serialization options. */
private fun productionJson(): Json {
    val application = KoinApplication.init()
    return try {
        application.modules(module {
            with(CommonPlugin) { setupDI(buildJsonObject {}) }
        })
        application.koin.get()
    } finally {
        application.close()
    }
}

/** Returns a JSON response with the content type required by Ktor's body decoder. */
private fun MockRequestHandleScope.respondPasswordChangeJson(
    content: String,
    status: HttpStatusCode = HttpStatusCode.OK,
) = respond(
    content = content,
    status = status,
    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
)
