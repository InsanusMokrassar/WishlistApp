package dev.inmo.wishlist.features.email.server

import dev.inmo.kroles.repos.RolesRepo
import dev.inmo.wishlist.features.auth.common.models.CompletePasswordChangeRequest
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequest
import dev.inmo.wishlist.features.auth.common.models.Token
import dev.inmo.wishlist.features.auth.server.Plugin as AuthServerPlugin
import dev.inmo.wishlist.features.auth.server.ServerPasswordChangeFeature
import dev.inmo.wishlist.features.auth.server.UserRoleAuthorization
import dev.inmo.wishlist.features.auth.server.configurators.BearerAuthenticationConfigurator
import dev.inmo.wishlist.features.auth.server.configurators.PasswordChangeRoutingsConfigurator
import dev.inmo.wishlist.features.auth.server.repo.PasswordsRepo
import dev.inmo.wishlist.features.auth.server.services.AuthFeatureService
import dev.inmo.wishlist.features.common.common.Plugin as CommonPlugin
import dev.inmo.wishlist.features.common.server.utils.safeCallLogLine
import dev.inmo.wishlist.features.deeplinks.common.repo.DeepLinksRepo
import dev.inmo.wishlist.features.deeplinks.server.Plugin as DeepLinksServerPlugin
import dev.inmo.wishlist.features.deeplinks.server.configurators.DeepLinksRoutingConfigurator
import dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService
import dev.inmo.wishlist.features.email.common.models.Email
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
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.koin.core.KoinApplication
import org.koin.dsl.module
import org.slf4j.Logger
import org.slf4j.event.Level
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
    /** Existing owner access token minted by the real Auth service. */
    val ownerToken: Token,
    /** Existing unrelated-account access token minted by the real Auth service. */
    val unrelatedToken: Token,
) {
    /** Closes the isolated dependency graph after an HTTP test completes. */
    fun close() {
        application.close()
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
        val application = KoinApplication.init()
        application.modules(module {
            single<UsersRepo> { users }
            single<ReadUsersRepo> { users }
            single<WriteUsersRepo> { users }
            single<PasswordsRepo> { passwords }
            single<DeepLinksRepo> { linksRepo }
            single<RolesRepo> { FakeRolesRepo() }
            single<RolesFeature> { FakeRolesFeature() }
            single<UserRoleAuthorization> { PasswordChangeRoleAuthorization() }
            single<EmailsService> { emails }
            with(CommonPlugin) { setupDI(config()) }
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
            ownerToken = ownerCredentials.token,
            unrelatedToken = unrelatedCredentials.token,
        )
    }

    /** Captures CallLogging output through its normal SLF4J callback without a production logging change. */
    private fun capturedLogger(lines: MutableList<String>): Logger = Proxy.newProxyInstance(
        Logger::class.java.classLoader,
        arrayOf(Logger::class.java),
    ) { proxy, method, arguments ->
        when (method.name) {
            "getName" -> "password-change-flow-test"
            "isTraceEnabled", "isDebugEnabled", "isInfoEnabled", "isWarnEnabled", "isErrorEnabled" -> true
            "trace", "debug", "info", "warn", "error" -> {
                arguments?.filterIsInstance<String>()?.firstOrNull()?.let(lines::add)
                null
            }
            "equals" -> proxy === arguments?.firstOrNull()
            "hashCode" -> System.identityHashCode(proxy)
            else -> null
        }
    } as Logger

    /** Installs the actual bearer, password-change, and deeplink configurators below `/api`. */
    private fun ApplicationTestBuilder.installFlowRoutes(graph: PasswordChangeFlowGraph, logs: MutableList<String>) {
        application {
            install(Authentication) {
                with(BearerAuthenticationConfigurator(graph.auth)) { invoke() }
            }
            install(ContentNegotiation) {
                json(graph.json)
            }
            install(CallLogging) {
                level = Level.WARN
                logger = capturedLogger(logs)
                format { call -> safeCallLogLine(call) }
            }
            routing {
                route("/api") {
                    with(PasswordChangeRoutingsConfigurator(graph.passwordChange)) { invoke() }
                    with(DeepLinksRoutingConfigurator(graph.links)) { invoke() }
                }
            }
        }
    }

    /** Requests one approval as the real owner and returns the sole delivered absolute href. */
    private suspend fun ApplicationTestBuilder.issueApproval(graph: PasswordChangeFlowGraph): String {
        val response = client.post("/api/auth/requestPasswordChangeEmail") {
            header(HttpHeaders.Authorization, "Bearer ${graph.ownerToken.string}")
            contentType(ContentType.Application.Json)
            setBody(graph.json.encodeToString(PasswordChangeEmailRequest(owner.email!!)))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("no-store", response.headers[HttpHeaders.CacheControl])
        assertEquals("no-referrer", response.headers["Referrer-Policy"])
        assertEquals("\"Sent\"", response.bodyAsText())
        assertEquals(1, graph.emails.sendHtmlCalls.size)
        val matches = Regex("""href=\"([^\"]+)\"""").findAll(graph.emails.sendHtmlCalls.single().html).toList()
        assertEquals(1, matches.size)
        return matches.single().groupValues[1]
    }

    /** Extracts the opaque deeplink identifier from an Email-owned generated URL. */
    private fun approvalId(href: String): String = href.substringAfterLast('/')

    /** Verifies completion replaces only the approval-bound owner credential. */
    private suspend fun ApplicationTestBuilder.completeApproval(
        graph: PasswordChangeFlowGraph,
        approvalId: String,
        browserToken: Token?,
    ) {
        val replacement = Password("owner-new-password")
        val response = client.post("/api/auth/completePasswordChange") {
            browserToken?.let { header(HttpHeaders.Authorization, "Bearer ${it.string}") }
            contentType(ContentType.Application.Json)
            setBody(
                graph.json.encodeToString(
                    CompletePasswordChangeRequest(owner.id, dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId(approvalId), replacement),
                ),
            )
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("no-store", response.headers[HttpHeaders.CacheControl])
        assertEquals("no-referrer", response.headers["Referrer-Policy"])
        assertEquals("\"Changed\"", response.bodyAsText())
        assertNotNull(graph.auth.login(owner.username, replacement))
        assertEquals(null, graph.auth.login(owner.username, ownerPassword))
        assertNotNull(graph.auth.login(unrelated.username, unrelatedPassword))
    }

    /** Captures issuance, read-only redirect, and anonymous approval-bound password completion. */
    @Test
    fun issuedEmailRedirectUsesSameApprovalAndAnonymousCompletionChangesOnlyOwner() = testApplication {
        val graph = graph()
        val logs = mutableListOf<String>()
        try {
            installFlowRoutes(graph, logs)
            val href = issueApproval(graph)
            val approvalId = approvalId(href)
            val redirect = createClient { followRedirects = false }.get(href.removePrefix("https://wishlist.example"))
            assertEquals(HttpStatusCode.Found, redirect.status)
            assertEquals("/password-change/${owner.id.long}/$approvalId", redirect.headers[HttpHeaders.Location])
            assertEquals("no-store", redirect.headers[HttpHeaders.CacheControl])
            assertEquals("no-referrer", redirect.headers["Referrer-Policy"])
            assertNotNull(graph.linksRepo.get(dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId(approvalId)))
            assertEquals(0, graph.passwords.issuedPasswordWriteCount)
            completeApproval(graph, approvalId, browserToken = null)
        } finally {
            graph.close()
        }
    }

    /** Proves a valid unrelated browser bearer cannot override the approval-bound completion subject. */
    @Test
    fun unrelatedBrowserBearerCannotSelectPasswordChangeSubject() = testApplication {
        val graph = graph()
        val logs = mutableListOf<String>()
        try {
            installFlowRoutes(graph, logs)
            val approvalId = approvalId(issueApproval(graph))
            completeApproval(graph, approvalId, browserToken = graph.unrelatedToken)
        } finally {
            graph.close()
        }
    }

    /** Ensures a real issuance repository exception reaches no public body or call log detail. */
    @Test
    fun issuanceRepositoryFailureIsSanitizedInResponseAndCallLog() = testApplication {
        val graph = graph()
        val logs = mutableListOf<String>()
        val sentinel = "issuance-repository-sentinel"
        try {
            graph.linksRepo.beforeSet = { error(sentinel) }
            installFlowRoutes(graph, logs)
            val response = client.post("/api/auth/requestPasswordChangeEmail") {
                header(HttpHeaders.Authorization, "Bearer ${graph.ownerToken.string}")
                contentType(ContentType.Application.Json)
                setBody(graph.json.encodeToString(PasswordChangeEmailRequest(owner.email!!)))
            }
            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertEquals("no-store", response.headers[HttpHeaders.CacheControl])
            assertEquals("no-referrer", response.headers["Referrer-Policy"])
            assertFalse(response.bodyAsText().contains(sentinel))
            assertTrue(logs.contains("POST 500"))
            assertTrue(logs.none { it.contains(sentinel) })
        } finally {
            graph.close()
        }
    }

    /** Ensures a real deeplink lookup failure becomes a header-protected 500 without mutation or leakage. */
    @Test
    fun deeplinkLookupFailureIsSanitizedWithoutWritesOrSentinelLeak() = testApplication {
        val graph = graph()
        val logs = mutableListOf<String>()
        val sentinel = "deeplink-lookup-sentinel"
        try {
            installFlowRoutes(graph, logs)
            val approvalId = approvalId(issueApproval(graph))
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
            assertTrue(logs.contains("GET 500"))
            assertTrue(logs.none { it.contains(sentinel) })
        } finally {
            graph.close()
        }
    }
}
