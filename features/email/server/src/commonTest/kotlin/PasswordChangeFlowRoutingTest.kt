package dev.inmo.wishlist.features.email.server

import ch.qos.logback.classic.Level as LogbackLevel
import ch.qos.logback.classic.Logger as LogbackLogger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.kroles.repos.RolesRepo
import dev.inmo.wishlist.features.auth.common.models.AuthCredentials
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.auth.common.models.CompletePasswordChangeRequest
import dev.inmo.wishlist.features.auth.common.models.LoginRequest
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequest
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult
import dev.inmo.wishlist.features.auth.common.models.RefreshRequest
import dev.inmo.wishlist.features.auth.common.models.Token
import dev.inmo.wishlist.features.auth.server.Plugin as AuthServerPlugin
import dev.inmo.wishlist.features.auth.server.ServerPasswordChangeFeature
import dev.inmo.wishlist.features.auth.server.UserRoleAuthorization
import dev.inmo.wishlist.features.auth.server.configurators.AuthRoutingsConfigurator
import dev.inmo.wishlist.features.auth.server.configurators.BearerAuthenticationConfigurator
import dev.inmo.wishlist.features.auth.server.configurators.PasswordChangeRoutingsConfigurator
import dev.inmo.wishlist.features.auth.server.repo.PasswordsRepo
import dev.inmo.wishlist.features.auth.server.services.AuthFeatureService
import dev.inmo.wishlist.features.common.common.Plugin as CommonPlugin
import dev.inmo.wishlist.features.common.server.JVMPlugin as CommonServerJVMPlugin
import dev.inmo.wishlist.features.common.server.utils.safeCallLogLine
import dev.inmo.wishlist.features.deeplinks.common.repo.DeepLinksRepo
import dev.inmo.wishlist.features.deeplinks.server.Plugin as DeepLinksServerPlugin
import dev.inmo.wishlist.features.deeplinks.server.configurators.DeepLinksRoutingConfigurator
import dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.models.EmailPasswordChangePayload
import dev.inmo.wishlist.features.email.server.services.FakeEmailsService
import dev.inmo.wishlist.features.email.server.services.FakeRolesFeature
import dev.inmo.wishlist.features.email.server.services.FakeRolesRepo
import dev.inmo.wishlist.features.email.server.services.FakeUsersRepo
import dev.inmo.wishlist.features.email.server.services.PasswordChangeDeepLinksRepo
import dev.inmo.wishlist.features.email.server.services.PasswordChangePasswordsRepo
import dev.inmo.wishlist.features.email.server.services.PasswordChangeRoleAuthorization
import dev.inmo.wishlist.features.roles.server.RolesFeature
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.ReadUsersRepo
import dev.inmo.wishlist.features.users.common.repo.UsersRepo
import dev.inmo.wishlist.features.users.common.repo.WriteUsersRepo
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.CannotTransformContentToTypeException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.PayloadTooLargeException
import io.ktor.server.plugins.UnsupportedMediaTypeException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.koin.core.KoinApplication
import org.koin.dsl.module
import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.micro_utils.ktor.server.configurators.StatusPagesConfigurator
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.util.concurrent.TimeoutException
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Isolated production-service graph used by the HTTP password-change flow assertions. */
private class PasswordChangeFlowGraph(
    /** Koin application containing the actual feature plugin registrations. */
    val application: KoinApplication,
    /** Production aggregated serializer used by both HTTP route and test request bodies. */
    val json: Json,
    /** Real Auth service that issues and validates bearer credentials. */
    val auth: AuthFeatureService,
    /** Real Email-owned password-change service exposed through Auth's optional port. */
    val passwordChange: ServerPasswordChangeFeature,
    /** Real deeplink dispatcher used by the public route. */
    val links: DeepLinksService,
    /** Controlled external delivery capture attached to the real Email service. */
    val emails: FakeEmailsService,
    /** Controlled persistence store behind real deeplink service calls. */
    val linksRepo: PasswordChangeDeepLinksRepo,
    /** Controlled persistence store behind real Auth password writes. */
    val passwords: PasswordChangePasswordsRepo,
    /** Real role repository whose mutation attempts remain observable after fixture bootstrap. */
    val rolesRepo: FakeRolesRepo,
    /** Real Auth direct-role bridge whose creation attempts remain observable after fixture bootstrap. */
    val roleAuthorization: PasswordChangeRoleAuthorization,
    /** Complete owner credentials minted before every password-approval action. */
    val ownerCredentials: AuthCredentials,
    /** Complete account-8 credentials minted before every password-approval action. */
    val unrelatedCredentials: AuthCredentials,
) {
    /** Closes the isolated dependency graph after an HTTP test completes. */
    fun close() {
        application.close()
    }
}

/** Owns isolated Logback appenders and level overrides for production application and Ktor loggers. */
private class KtorLogCapture(
    /** Production logger instances whose emitted events are asserted by the test. */
    private val loggers: List<LogbackLogger>,
) : AutoCloseable {
    /** Direct logger levels before the test-specific DEBUG capture override. */
    private val levels = loggers.map(LogbackLogger::getLevel)

    /** Test-owned appenders paired with [loggers] in construction order. */
    private val appenders = loggers.map { logger ->
        ListAppender<ILoggingEvent>().also { appender ->
            appender.context = logger.loggerContext
            appender.start()
            logger.addAppender(appender)
        }
    }

    init {
        loggers.forEach { logger -> logger.level = LogbackLevel.DEBUG }
    }

    /** Returns an immutable event snapshot before the appenders are detached. */
    fun events(): List<ILoggingEvent> = appenders.flatMap { it.list.toList() }

    /** Detaches and stops every test-owned appender. */
    override fun close() {
        loggers.zip(appenders).zip(levels).forEach { pair ->
            val (loggerAndAppender, level) = pair
            val (logger, appender) = loggerAndAppender
            logger.detachAppender(appender)
            appender.stop()
            logger.level = level
        }
    }
}

/** Exercises the production Auth, Email, and deeplink HTTP boundary for password approvals. */
class PasswordChangeFlowRoutingTest {
    /** Approved account that is the only permitted password-change subject in these flows. */
    private val owner = RegisteredUser(
        id = UserId(7L),
        username = Username("owner"),
        email = Email("owner@example.com"),
        emailApproved = true,
    )

    /** Independently authenticated account used to prove completion ignores ambient bearer identity. */
    private val unrelated = RegisteredUser(
        id = UserId(8L),
        username = Username("other"),
        email = Email("other@example.com"),
        emailApproved = true,
    )

    /** Existing owner credential used to obtain the issuance bearer token. */
    private val ownerPassword = Password("owner-existing-password")

    /** Existing unrelated credential that must remain unchanged by owner completion. */
    private val unrelatedPassword = Password("other-existing-password")

    /** Builds the minimum root configuration consumed by the actual Common/Auth/deeplinks/Email plugins. */
    private fun config(): JsonObject = buildJsonObject {
        put("publicHttpOrigin", "https://wishlist.example")
    }

    /** Creates production services while substituting only external persistence, roles, and email delivery. */
    private suspend fun graph(): PasswordChangeFlowGraph {
        val users = FakeUsersRepo(mapOf(owner.id to owner, unrelated.id to unrelated))
        val passwords = PasswordChangePasswordsRepo()
        val linksRepo = PasswordChangeDeepLinksRepo()
        val emails = FakeEmailsService()
        val rolesRepo = FakeRolesRepo()
        val roleAuthorization = PasswordChangeRoleAuthorization()
        val application = KoinApplication.init()
        application.modules(module {
            single<UsersRepo> { users }
            single<ReadUsersRepo> { users }
            single<WriteUsersRepo> { users }
            single<PasswordsRepo> { passwords }
            single<DeepLinksRepo> { linksRepo }
            single<RolesRepo> { rolesRepo }
            single<RolesFeature> { FakeRolesFeature() }
            single<UserRoleAuthorization> { roleAuthorization }
            single<EmailsService> { emails }
            with(CommonPlugin) { setupDI(config()) }
            with(CommonServerJVMPlugin) { setupDI(config()) }
            with(AuthServerPlugin) { setupDI(config()) }
            with(DeepLinksServerPlugin) { setupDI(config()) }
            with(Plugin) { setupDI(config()) }
        })
        val auth = application.koin.get<AuthFeatureService>()
        auth.setPassword(owner.id, ownerPassword)
        auth.setPassword(unrelated.id, unrelatedPassword)
        passwords.resetIssuedPasswordWriteCount()
        val ownerCredentials = requireNotNull(auth.login(owner.username, ownerPassword))
        val unrelatedCredentials = requireNotNull(auth.login(unrelated.username, unrelatedPassword))
        return PasswordChangeFlowGraph(
            application = application,
            json = application.koin.get(),
            auth = auth,
            passwordChange = application.koin.get(),
            links = application.koin.get(),
            emails = emails,
            linksRepo = linksRepo,
            passwords = passwords,
            rolesRepo = rolesRepo,
            roleAuthorization = roleAuthorization,
            ownerCredentials = ownerCredentials,
            unrelatedCredentials = unrelatedCredentials,
        )
    }

    /** Attaches test-owned appenders to both Ktor request logging and the application logger. */
    private fun captureKtorLogs(): KtorLogCapture = KtorLogCapture(
        listOf(
            LoggerFactory.getLogger("Ktor") as LogbackLogger,
            LoggerFactory.getLogger("io.ktor.server.Application") as LogbackLogger,
        ),
    )

    /** Installs the actual bearer, password-change, and deeplink configurators below `/api`. */
    private fun ApplicationTestBuilder.installFlowRoutes(
        graph: PasswordChangeFlowGraph,
        installUnhandledFailureRoute: Boolean = false,
        installClassificationRoutes: Boolean = false,
    ) {
        application {
            install(Authentication) {
                with(BearerAuthenticationConfigurator(graph.auth)) { invoke() }
            }
            install(ContentNegotiation) {
                json(graph.json)
            }
            with(StatusPagesConfigurator(graph.application.koin.getAllDistinct())) { configure() }
            install(CallLogging) {
                level = Level.WARN
                logger = LoggerFactory.getLogger("Ktor")
                format { call -> safeCallLogLine(call) }
            }
            routing {
                route("/api") {
                    with(AuthRoutingsConfigurator(graph.auth)) { invoke() }
                    with(PasswordChangeRoutingsConfigurator(graph.passwordChange)) { invoke() }
                    with(DeepLinksRoutingConfigurator(graph.links)) { invoke() }
                }
                if (installUnhandledFailureRoute) {
                    get("/unhandled/{detail}") {
                        error("unhandled route failure ${call.parameters["detail"]}")
                    }
                }
                if (installClassificationRoutes) {
                    route("/classified/{kind}/{approvalId}") {
                        get {
                            throw classifiedFailure(
                                kind = requireNotNull(call.parameters["kind"]),
                                marker = requireNotNull(call.parameters["approvalId"]),
                            )
                        }
                        post {
                            throw classifiedFailure(
                                kind = requireNotNull(call.parameters["kind"]),
                                marker = requireNotNull(call.parameters["approvalId"]),
                            )
                        }
                    }
                }
            }
        }
    }

    /** Creates a sensitive exception instance for every Ktor status classifier covered by the matrix. */
    @OptIn(ExperimentalStdlibApi::class)
    private fun classifiedFailure(kind: String, marker: String): Throwable {
        val cause = IllegalStateException("cause-$marker").also {
            it.addSuppressed(IllegalArgumentException("suppressed-$marker"))
        }
        return when (kind) {
            "bad-request" -> BadRequestException("bad-request-$marker", cause)
            "not-found" -> NotFoundException("not-found-$marker").also { it.initCause(cause) }
            "unsupported-media" -> UnsupportedMediaTypeException(ContentType.parse("application/$marker")).also {
                it.initCause(cause)
            }
            "cannot-transform" -> CannotTransformContentToTypeException(typeOf<String>()).also { it.initCause(cause) }
            "payload-too-large" -> PayloadTooLargeException(13L).also { it.initCause(cause) }
            "timeout" -> TimeoutException("timeout-$marker").also { it.initCause(cause) }
            "illegal-state" -> IllegalStateException("illegal-state-$marker", cause)
            else -> error("Unknown Ktor classifier case: $kind")
        }
    }

    /** Produces a real coroutine timeout exception rather than constructing a cancellation lookalike. */
    private suspend fun actualTimeoutCancellationException(): TimeoutCancellationException = try {
        withTimeout(1) { awaitCancellation() }
        error("Expected withTimeout to throw TimeoutCancellationException")
    } catch (cause: TimeoutCancellationException) {
        cause
    }

    /** Verifies every captured event has no sensitive formatted, MDC, argument, or throwable data. */
    private fun assertLogEventsRedact(events: List<ILoggingEvent>, forbidden: List<String>) {
        events.forEach { event ->
            assertEquals(null, event.throwableProxy)
            forbidden.forEach { marker ->
                assertFalse(event.formattedMessage.contains(marker))
                assertFalse(event.mdcPropertyMap.values.any { value -> value.contains(marker) })
                assertFalse(event.argumentArray.orEmpty().any { argument -> argument.toString().contains(marker) })
            }
        }
    }

    /** Preserves Auth's malformed-body and explicit authentication/path status contracts globally. */
    @Test
    fun malformedLegacyAuthBodiesRemainBadRequestUnderProductionStatusPages() = testApplication {
        val graph = graph()
        val sentinel = "malformed-auth-body-sentinel"
        try {
            installFlowRoutes(graph)
            listOf("register", "login", "refresh").forEach { endpoint ->
                val response = client.post("/api/auth/$endpoint") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"sentinel\":\"$sentinel\"")
                }
                assertEquals(HttpStatusCode.BadRequest, response.status)
                assertFalse(response.bodyAsText().contains(sentinel))
                val missingFieldResponse = client.post("/api/auth/$endpoint") {
                    contentType(ContentType.Application.Json)
                    setBody("{}")
                }
                assertEquals(HttpStatusCode.BadRequest, missingFieldResponse.status)
                assertFalse(missingFieldResponse.bodyAsText().contains(sentinel))
            }
            val unknown = client.get("/api/auth/$sentinel")
            assertEquals(HttpStatusCode.NotFound, unknown.status)
            assertFalse(unknown.bodyAsText().contains(sentinel))
            assertEquals(HttpStatusCode.Unauthorized, client.get("/api/auth/getMe").status)
        } finally {
            graph.close()
        }
    }

    /** Establishes the unsanitized Ktor baseline used by the production 400 parity regression. */
    @Test
    fun unsanitizedKtorBadRequestBaselineIsBadRequest() = testApplication {
        application {
            routing {
                get("/baseline") {
                    throw BadRequestException("baseline-sensitive-detail")
                }
            }
        }
        assertEquals(HttpStatusCode.BadRequest, client.get("/baseline").status)
    }

    /** Preserves Ktor's typed status classes while redacting request and exception detail in production logs. */
    @Test
    fun productionStatusPagesPreserveTypedKtorStatusesAndRedactAllLogRepresentations() = testApplication {
        val graph = graph()
        val approvalId = "8caed0c4-0c53-4d92-9374-b79bb07a2bf1"
        val queryMarker = "query-sentinel"
        val bodyMarker = "body-sentinel"
        val emailMarker = "private-email@example.com"
        val passwordMarker = "plaintext-password-sentinel"
        val locationMarker = "https://wishlist.example/password-change/$approvalId"
        val cases = listOf(
            "bad-request" to HttpStatusCode.BadRequest,
            "not-found" to HttpStatusCode.NotFound,
            "unsupported-media" to HttpStatusCode.UnsupportedMediaType,
            "cannot-transform" to HttpStatusCode.UnsupportedMediaType,
            "payload-too-large" to HttpStatusCode.PayloadTooLarge,
            "timeout" to HttpStatusCode.GatewayTimeout,
            "illegal-state" to HttpStatusCode.InternalServerError,
        )
        try {
            captureKtorLogs().use { capture ->
                installFlowRoutes(graph, installClassificationRoutes = true)
                cases.forEach { (kind, expectedStatus) ->
                    val response = client.post("/classified/$kind/$approvalId?$queryMarker=$queryMarker") {
                        contentType(ContentType.Application.Json)
                        setBody("{\"email\":\"$emailMarker\",\"password\":\"$passwordMarker\",\"body\":\"$bodyMarker\"}")
                    }
                    assertEquals(expectedStatus, response.status)
                    assertFalse(response.bodyAsText().contains(approvalId))
                    assertFalse(response.bodyAsText().contains(queryMarker))
                    assertFalse(response.bodyAsText().contains(bodyMarker))
                    assertFalse(response.bodyAsText().contains(emailMarker))
                    assertFalse(response.bodyAsText().contains(passwordMarker))
                    assertFalse(response.bodyAsText().contains(locationMarker))
                    assertFalse(response.bodyAsText().contains("cause-$approvalId"))
                    assertFalse(response.bodyAsText().contains("suppressed-$approvalId"))
                }
                val events = capture.events()
                cases.forEach { (_, expectedStatus) ->
                    assertTrue(events.any { event -> event.formattedMessage == "POST ${expectedStatus.value}" })
                }
                assertLogEventsRedact(
                    events,
                    listOf(approvalId, queryMarker, bodyMarker, emailMarker, passwordMarker, locationMarker, "cause-$approvalId", "suppressed-$approvalId"),
                )
            }
        } finally {
            graph.close()
        }
    }

    /** Invokes Common's contributed Throwable callback to prove both cancellation classes rethrow by identity. */
    @Test
    fun contributedThrowableCallbackRethrowsCancellationWithoutResponseOrBoundaryLog() = testApplication {
        val graph = graph()
        val observed = mutableListOf<Throwable>()
        try {
            captureKtorLogs().use { capture ->
                application {
                    val config = io.ktor.server.plugins.statuspages.StatusPagesConfig()
                    graph.application.koin.getAllDistinct<StatusPagesConfigurator.Element>().forEach { element ->
                        with(element) { config.invoke() }
                    }
                    val callback = requireNotNull(config.exceptions[Throwable::class])
                    routing {
                        get("/direct-cancellation/{kind}") {
                            val expected = when (requireNotNull(call.parameters["kind"])) {
                                "cancellation" -> CancellationException("cancellation-sentinel")
                                "timeout-cancellation" -> actualTimeoutCancellationException()
                                else -> error("Unknown cancellation case")
                            }
                            try {
                                callback(call, expected)
                                error("Throwable callback unexpectedly completed")
                            } catch (actual: Throwable) {
                                assertSame(expected, actual)
                                assertEquals(null, call.response.status())
                                observed.add(actual)
                            }
                        }
                    }
                }
                listOf("cancellation", "timeout-cancellation").forEach { kind ->
                    assertEquals(HttpStatusCode.NotFound, client.get("/direct-cancellation/$kind").status)
                }
                assertEquals(2, observed.size)
                assertTrue(observed[1] is TimeoutCancellationException)
                assertTrue(capture.events().isEmpty())
            }
        } finally {
            graph.close()
        }
    }

    /** Requests one approval through Auth's real bearer route and returns the delivered absolute href. */
    private suspend fun ApplicationTestBuilder.issueApproval(
        graph: PasswordChangeFlowGraph,
        requester: RegisteredUser,
        credentials: AuthCredentials,
    ): String {
        val deliveriesBefore = graph.emails.sendHtmlCalls.size
        val response = client.post("/api/auth/requestPasswordChangeEmail") {
            header(HttpHeaders.Authorization, "Bearer ${credentials.token.string}")
            contentType(ContentType.Application.Json)
            setBody(graph.json.encodeToString(PasswordChangeEmailRequest(requireNotNull(requester.email))))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("no-store", response.headers[HttpHeaders.CacheControl])
        assertEquals("no-referrer", response.headers["Referrer-Policy"])
        assertEquals("\"Sent\"", response.bodyAsText())
        assertEquals(deliveriesBefore + 1, graph.emails.sendHtmlCalls.size)
        val matches = Regex("""href=\"([^\"]+)\"""").findAll(graph.emails.sendHtmlCalls.last().html).toList()
        assertEquals(1, matches.size)
        return matches.single().groupValues[1]
    }

    /** Extracts the opaque deeplink identifier from an Email-owned generated URL. */
    private fun approvalId(href: String): String = href.substringAfterLast('/')

    /** Confirms a persisted approval is valid, read-only, and still bound to its original subject. */
    private suspend fun ApplicationTestBuilder.assertApprovalRedirectsReadOnly(
        graph: PasswordChangeFlowGraph,
        subject: RegisteredUser,
        href: String,
    ) {
        val id = approvalId(href)
        val response = createClient { followRedirects = false }.get(href.removePrefix("https://wishlist.example"))
        assertEquals(HttpStatusCode.Found, response.status)
        assertEquals("/password-change/${subject.id.long}/$id", response.headers[HttpHeaders.Location])
        assertNotNull(graph.linksRepo.get(dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId(id)))
    }

    /** Completes an approval through the real public Auth route and returns the exact domain result. */
    private suspend fun ApplicationTestBuilder.completeApproval(
        graph: PasswordChangeFlowGraph,
        subject: RegisteredUser,
        approvalId: String,
        replacement: Password,
        browserToken: Token? = null,
    ): PasswordChangeResult {
        val response = client.post("/api/auth/completePasswordChange") {
            browserToken?.let { header(HttpHeaders.Authorization, "Bearer ${it.string}") }
            contentType(ContentType.Application.Json)
            setBody(
                graph.json.encodeToString(
                    CompletePasswordChangeRequest(
                        subject.id,
                        dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId(approvalId),
                        replacement,
                    ),
                ),
            )
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("no-store", response.headers[HttpHeaders.CacheControl])
        assertEquals("no-referrer", response.headers["Referrer-Policy"])
        return graph.json.decodeFromString(response.bodyAsText())
    }

    /** Uses the real Auth `getMe` route to prove an access credential retains its original subject. */
    private suspend fun ApplicationTestBuilder.assertAccessTokenSubject(
        graph: PasswordChangeFlowGraph,
        token: Token,
        subject: RegisteredUser,
    ) {
        val response = client.get("/api/auth/getMe") {
            header(HttpHeaders.Authorization, "Bearer ${token.string}")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            subject.id,
            graph.json.decodeFromString<AuthFeatureUser>(response.bodyAsText()).id,
        )
    }

    /** Refreshes one pre-change credential through the real Auth route and verifies the returned subject. */
    private suspend fun ApplicationTestBuilder.refreshAndAssertSubject(
        graph: PasswordChangeFlowGraph,
        credentials: AuthCredentials,
        subject: RegisteredUser,
    ) {
        val response = client.post("/api/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(graph.json.encodeToString(RefreshRequest(credentials.refreshToken)))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val refreshed = graph.json.decodeFromString<AuthCredentials>(response.bodyAsText())
        assertAccessTokenSubject(graph, refreshed.token, subject)
    }

    /** Submits one password through Auth's real login route and asserts the selected status. */
    private suspend fun ApplicationTestBuilder.assertLoginStatus(
        graph: PasswordChangeFlowGraph,
        subject: RegisteredUser,
        password: Password,
        expected: HttpStatusCode,
    ) {
        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(graph.json.encodeToString(LoginRequest(subject.username, password)))
        }
        assertEquals(expected, response.status)
    }

    /** Captures issuance, read-only redirect, and anonymous approval-bound password completion. */
    @Test
    fun issuedEmailRedirectUsesSameApprovalAndAnonymousCompletionChangesOnlyOwner() = testApplication {
        val graph = graph()
        try {
            installFlowRoutes(graph)
            val href = issueApproval(graph, owner, graph.ownerCredentials)
            val approvalId = approvalId(href)
            val redirect = createClient { followRedirects = false }.get(href.removePrefix("https://wishlist.example"))
            assertEquals(HttpStatusCode.Found, redirect.status)
            assertEquals("/password-change/${owner.id.long}/$approvalId", redirect.headers[HttpHeaders.Location])
            assertEquals("no-store", redirect.headers[HttpHeaders.CacheControl])
            assertEquals("no-referrer", redirect.headers["Referrer-Policy"])
            assertNotNull(graph.linksRepo.get(dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId(approvalId)))
            assertEquals(0, graph.passwords.issuedPasswordWriteCount)
            assertEquals(
                PasswordChangeResult.Changed,
                completeApproval(graph, owner, approvalId, Password("owner-new-password")),
            )
            assertNotNull(graph.auth.login(owner.username, Password("owner-new-password")))
            assertEquals(null, graph.auth.login(owner.username, ownerPassword))
            assertNotNull(graph.auth.login(unrelated.username, unrelatedPassword))
        } finally {
            graph.close()
        }
    }

    /** Proves a valid unrelated browser bearer cannot override the approval-bound completion subject. */
    @Test
    fun unrelatedBrowserBearerCannotSelectPasswordChangeSubject() = testApplication {
        val graph = graph()
        try {
            installFlowRoutes(graph)
            val approvalId = approvalId(issueApproval(graph, owner, graph.ownerCredentials))
            assertEquals(
                PasswordChangeResult.Changed,
                completeApproval(graph, owner, approvalId, Password("owner-new-password"), graph.unrelatedCredentials.token),
            )
        } finally {
            graph.close()
        }
    }

    /** Proves live approval invalidation, session preservation, and zero role mutation across two real accounts. */
    @Test
    fun siblingInvalidationPreservesExistingSessionsAndAccountEightApprovalWithoutRoleMutation() = testApplication {
        val graph = graph()
        val ownerReplacement = Password("owner-new-password")
        val unrelatedReplacement = Password("other-new-password")
        try {
            installFlowRoutes(graph)
            val ownerSubject = BaseRoleSubject.Direct(owner.id.long.toString())
            val unrelatedSubject = BaseRoleSubject.Direct(unrelated.id.long.toString())
            val rolesBefore = mapOf(
                ownerSubject to graph.rolesRepo.getDirectRoles(ownerSubject),
                unrelatedSubject to graph.rolesRepo.getDirectRoles(unrelatedSubject),
            )
            graph.rolesRepo.resetMutationAttempts()
            graph.roleAuthorization.resetAttemptCounts()

            val ownerApprovalA = issueApproval(graph, owner, graph.ownerCredentials)
            val ownerApprovalB = issueApproval(graph, owner, graph.ownerCredentials)
            val unrelatedApproval = issueApproval(graph, unrelated, graph.unrelatedCredentials)
            assertApprovalRedirectsReadOnly(graph, owner, ownerApprovalA)
            assertApprovalRedirectsReadOnly(graph, owner, ownerApprovalB)
            assertApprovalRedirectsReadOnly(graph, unrelated, unrelatedApproval)
            assertEquals(0, graph.passwords.issuedPasswordWriteCount)

            assertEquals(
                PasswordChangeResult.Changed,
                completeApproval(graph, owner, approvalId(ownerApprovalA), ownerReplacement),
            )
            assertEquals(1, graph.passwords.issuedPasswordWriteCount)
            assertEquals(
                PasswordChangeResult.InvalidApproval,
                completeApproval(graph, owner, approvalId(ownerApprovalB), Password("owner-sibling-password")),
            )
            assertEquals(1, graph.passwords.issuedPasswordWriteCount)

            assertAccessTokenSubject(graph, graph.ownerCredentials.token, owner)
            assertAccessTokenSubject(graph, graph.unrelatedCredentials.token, unrelated)
            refreshAndAssertSubject(graph, graph.ownerCredentials, owner)
            refreshAndAssertSubject(graph, graph.unrelatedCredentials, unrelated)
            assertLoginStatus(graph, owner, ownerPassword, HttpStatusCode.Unauthorized)
            assertLoginStatus(graph, owner, ownerReplacement, HttpStatusCode.OK)
            assertLoginStatus(graph, unrelated, unrelatedPassword, HttpStatusCode.OK)
            assertLoginStatus(graph, unrelated, Password("unrelated-wrong-password"), HttpStatusCode.Unauthorized)

            assertEquals(
                PasswordChangeResult.Changed,
                completeApproval(graph, unrelated, approvalId(unrelatedApproval), unrelatedReplacement),
            )
            assertEquals(2, graph.passwords.issuedPasswordWriteCount)
            assertLoginStatus(graph, unrelated, unrelatedPassword, HttpStatusCode.Unauthorized)
            assertLoginStatus(graph, unrelated, unrelatedReplacement, HttpStatusCode.OK)
            assertLoginStatus(graph, owner, ownerReplacement, HttpStatusCode.OK)

            val rolesAfter = mapOf(
                ownerSubject to graph.rolesRepo.getDirectRoles(ownerSubject),
                unrelatedSubject to graph.rolesRepo.getDirectRoles(unrelatedSubject),
            )
            assertEquals(rolesBefore, rolesAfter)
            assertEquals(0, graph.rolesRepo.includeAttempts)
            assertEquals(0, graph.rolesRepo.excludeAttempts)
            assertEquals(0, graph.rolesRepo.createAttempts)
            assertEquals(0, graph.rolesRepo.removeAttempts)
            assertEquals(0, graph.roleAuthorization.ensureAttempts)
        } finally {
            graph.close()
        }
    }

    /** Verifies known response status and unset status use the production status-only format. */
    @Test
    fun safeCallLogLineUsesKnownStatusAndZeroFallback() = testApplication {
        val beforeResponseLines = mutableListOf<String>()
        captureKtorLogs().use { capture ->
            application {
                intercept(ApplicationCallPipeline.Plugins) {
                    beforeResponseLines.add(safeCallLogLine(context))
                }
                install(CallLogging) {
                    level = Level.WARN
                    logger = LoggerFactory.getLogger("Ktor")
                    format { call -> safeCallLogLine(call) }
                }
                routing {
                    get("/known") {
                        call.respond(HttpStatusCode(418, "Teapot"))
                    }
                }
            }
            assertEquals(HttpStatusCode(418, "Teapot"), client.get("/known").status)
            assertEquals(listOf("GET 0"), beforeResponseLines)
            assertTrue(capture.events().any { event -> event.formattedMessage == "GET 418" })
        }
    }

    /** Verifies production Ktor logging and unhandled errors cannot expose actionable request detail. */
    @Test
    fun productionLoggingRedactsRouteAndUnhandledFailureDetails() = testApplication {
        val graph = graph()
        val approvalPathMarker = "approval-uuid-sentinel"
        val queryMarker = "query-sentinel"
        val bodyMarker = "body-sentinel"
        val repositoryMarker = "repository-exception-sentinel"
        try {
            captureKtorLogs().use { capture ->
                installFlowRoutes(graph, installUnhandledFailureRoute = true)
                val href = issueApproval(graph, owner, graph.ownerCredentials)
                val approvalId = approvalId(href)
                val redirect = createClient { followRedirects = false }
                    .get("/api/links/$approvalId?$queryMarker=$queryMarker")
                assertEquals(HttpStatusCode.Found, redirect.status)
                val location = requireNotNull(redirect.headers[HttpHeaders.Location])
                val rejection = client.post("/api/auth/requestPasswordChangeEmail?$queryMarker=$queryMarker") {
                    header(HttpHeaders.Authorization, "Bearer rejected-$approvalPathMarker")
                    contentType(ContentType.Application.Json)
                    setBody("{\"expectedEmail\":\"owner@example.com\",\"marker\":\"$bodyMarker\"}")
                }
                assertEquals(HttpStatusCode.Unauthorized, rejection.status)
                graph.linksRepo.resetOperationRecords()
                graph.passwords.resetIssuedPasswordWriteCount()
                graph.linksRepo.beforeGet = { error(repositoryMarker) }
                val lookupFailure = createClient { followRedirects = false }
                    .get("/api/links/$approvalId?$queryMarker=$queryMarker")
                assertEquals(HttpStatusCode.InternalServerError, lookupFailure.status)
                assertEquals("no-store", lookupFailure.headers[HttpHeaders.CacheControl])
                assertEquals("no-referrer", lookupFailure.headers["Referrer-Policy"])
                assertFalse(lookupFailure.bodyAsText().contains(repositoryMarker))
                assertTrue(graph.linksRepo.setIds.isEmpty())
                assertTrue(graph.linksRepo.unsetIds.isEmpty())
                assertEquals(0, graph.passwords.issuedPasswordWriteCount)
                val unhandled = client.get("/unhandled/$approvalPathMarker?$queryMarker=$queryMarker")
                assertEquals(HttpStatusCode.InternalServerError, unhandled.status)

                val events = capture.events()
                assertTrue(events.any { event -> event.level == LogbackLevel.WARN && event.formattedMessage == "POST 200" })
                assertTrue(events.any { event -> event.level == LogbackLevel.WARN && event.formattedMessage == "GET 302" })
                assertTrue(events.any { event -> event.level == LogbackLevel.WARN && event.formattedMessage == "POST 401" })
                assertTrue(events.any { event -> event.level == LogbackLevel.WARN && event.formattedMessage == "GET 500" })
                assertTrue(events.any { event -> event.level == LogbackLevel.ERROR && event.formattedMessage == "GET 500" })
                val forbidden = listOf(
                    approvalId,
                    approvalPathMarker,
                    queryMarker,
                    bodyMarker,
                    location,
                    repositoryMarker,
                )
                events.forEach { event ->
                    forbidden.forEach { marker ->
                        assertFalse(event.formattedMessage.contains(marker))
                        assertFalse(event.throwableProxy?.message?.contains(marker) == true)
                    }
                }
            }
        } finally {
            graph.close()
        }
    }

    /** Verifies request and payload diagnostic strings redact all approval and credential-bearing fields. */
    @Test
    fun passwordChangeDiagnosticStringsRedactApprovalPasswordEmailAndCredentialState() {
        val approval = "approval-uuid-sentinel"
        val password = "plaintext-password-sentinel"
        val email = "private-email@example.com"
        val credentialState = "credential-state-sentinel"
        val request = CompletePasswordChangeRequest(owner.id, dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId(approval), Password(password))
        val payload = EmailPasswordChangePayload(owner.id, Email(email), 1L, credentialState)
        listOf(request.toString(), payload.toString()).forEach { text ->
            listOf(approval, password, email, credentialState).forEach { marker ->
                assertFalse(text.contains(marker))
            }
            assertTrue(text.contains("<redacted>"))
        }
    }

    /** Ensures a real issuance repository exception reaches no public body or call log detail. */
    @Test
    fun issuanceRepositoryFailureIsSanitizedInResponseAndCallLog() = testApplication {
        val graph = graph()
        val sentinel = "issuance-repository-sentinel"
        try {
            graph.linksRepo.beforeSet = { error(sentinel) }
            installFlowRoutes(graph)
            val response = client.post("/api/auth/requestPasswordChangeEmail") {
                header(HttpHeaders.Authorization, "Bearer ${graph.ownerCredentials.token.string}")
                contentType(ContentType.Application.Json)
                setBody(graph.json.encodeToString(PasswordChangeEmailRequest(owner.email!!)))
            }
            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertEquals("no-store", response.headers[HttpHeaders.CacheControl])
            assertEquals("no-referrer", response.headers["Referrer-Policy"])
            assertFalse(response.bodyAsText().contains(sentinel))
        } finally {
            graph.close()
        }
    }

    /** Ensures a real deeplink lookup failure becomes a header-protected 500 without mutation or leakage. */
    @Test
    fun deeplinkLookupFailureIsSanitizedWithoutWritesOrSentinelLeak() = testApplication {
        val graph = graph()
        val sentinel = "deeplink-lookup-sentinel"
        try {
            installFlowRoutes(graph)
            val approvalId = approvalId(issueApproval(graph, owner, graph.ownerCredentials))
            graph.linksRepo.resetOperationRecords()
            graph.passwords.resetIssuedPasswordWriteCount()
            graph.linksRepo.beforeGet = { error(sentinel) }
            val response = createClient { followRedirects = false }
                .get("/api/links/$approvalId")
            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertEquals("no-store", response.headers[HttpHeaders.CacheControl])
            assertEquals("no-referrer", response.headers["Referrer-Policy"])
            assertFalse(response.bodyAsText().contains(sentinel))
            assertTrue(graph.linksRepo.setIds.isEmpty())
            assertTrue(graph.linksRepo.unsetIds.isEmpty())
            assertEquals(0, graph.passwords.issuedPasswordWriteCount)
        } finally {
            graph.close()
        }
    }
}
