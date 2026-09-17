package dev.inmo.wishlist.features.ui.auth.ui

import dev.inmo.kslog.common.KSLog
import dev.inmo.wishlist.features.auth.client.AuthCredentialsStorage
import dev.inmo.wishlist.features.auth.client.ClientAuthFeature
import dev.inmo.wishlist.features.auth.common.models.AuthConfig
import dev.inmo.wishlist.features.auth.common.models.AuthCredentials
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.RefreshToken
import dev.inmo.wishlist.features.auth.common.models.RegistrationResult
import dev.inmo.wishlist.features.auth.common.models.Token
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.ui.auth.Plugin
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Verifies the default authentication model and its production singleton binding. */
class AuthModelTest {
    @Test
    fun delegatesAuthenticationAndPreservesFallbackBehavior() = runTest {
        val storage = RecordingCredentialsStorage()
        val auth = RecordingAuthFeature()
        val model = DefaultAuthModel(authFeature = auth, credentialsStorage = storage)
        val username = Username("alice")
        val password = Password("secret")
        val email = Email("alice@example.com")

        assertSame(storage.userAuthorised, model.userAuthorisedState)
        assertFalse(model.isAlreadyLoggedIn())
        assertEquals(0, auth.getMeCalls)

        storage.userAuthorised.value = true
        auth.me = null
        assertFalse(model.isAlreadyLoggedIn())
        auth.me = AuthFeatureUser(UserId(7L), username, email, emailApproved = true)
        assertTrue(model.isAlreadyLoggedIn())
        assertEquals(2, auth.getMeCalls)

        auth.loginResult = credentials
        assertTrue(model.login(username, password))
        auth.loginResult = null
        assertFalse(model.login(username, password))
        assertEquals(listOf(username to password, username to password), auth.loginCalls)

        model.logout()
        assertEquals(1, auth.logoutCalls)

        auth.config = AuthConfig(enableRegistration = true, requireEmailForRegistration = true)
        assertEquals(auth.config, model.getConfig())
        assertTrue(model.isRegistrationEnabled())
        auth.configFailure = IllegalStateException("unavailable")
        val originalLogger = KSLog.default
        KSLog.default = KSLog { _, _, _, _ -> }
        try {
            assertEquals(AuthConfig(), model.getConfig())
            assertFalse(model.isRegistrationEnabled())
        } finally {
            KSLog.default = originalLogger
        }

        auth.registrationResult = RegistrationResult.PendingEmailVerification
        assertEquals(auth.registrationResult, model.register(username, password, email))
        assertEquals(auth.registrationResult, model.register(username, password, null))
        assertEquals(
            listOf(Triple(username, password, email), Triple(username, password, null)),
            auth.registrationCalls,
        )
    }

    @Test
    fun pluginBindsDefaultModelAsOneInterfaceSingleton() {
        val koin = startKoin {
            modules(
                module {
                    with(Plugin) { setupDI(JsonObject(emptyMap())) }
                    single<ClientAuthFeature> { RecordingAuthFeature() }
                    single<AuthCredentialsStorage> { RecordingCredentialsStorage() }
                }
            )
        }
        try {
            val first = koin.koin.get<AuthModel>()
            val second = koin.koin.get<AuthModel>()

            assertIs<DefaultAuthModel>(first)
            assertSame(first, second)
        } finally {
            stopKoin()
        }
    }

    private class RecordingCredentialsStorage : AuthCredentialsStorage {
        override val userAuthorised = MutableStateFlow(false)
        override suspend fun get(): AuthCredentials? = null
        override suspend fun save(credentials: AuthCredentials?) = Unit
    }

    private class RecordingAuthFeature : ClientAuthFeature {
        var me: AuthFeatureUser? = null
        var loginResult: AuthCredentials? = null
        var config = AuthConfig()
        var configFailure: Throwable? = null
        var registrationResult: RegistrationResult? = null
        var getMeCalls = 0
        var logoutCalls = 0
        val loginCalls = mutableListOf<Pair<Username, Password>>()
        val registrationCalls = mutableListOf<Triple<Username, Password, Email?>>()

        override suspend fun getMe(): AuthFeatureUser? {
            getMeCalls += 1
            return me
        }

        override suspend fun login(username: Username, password: Password): AuthCredentials? {
            loginCalls += username to password
            return loginResult
        }

        override suspend fun logout() {
            logoutCalls += 1
        }

        override suspend fun refresh(refreshToken: RefreshToken): AuthCredentials? = null

        override suspend fun register(username: Username, password: Password): RegistrationResult? =
            registrationResult

        override suspend fun register(
            username: Username,
            password: Password,
            email: Email?,
        ): RegistrationResult? {
            registrationCalls += Triple(username, password, email)
            return registrationResult
        }

        override suspend fun getConfig(): AuthConfig {
            configFailure?.let { throw it }
            return config
        }

        override suspend fun isRegistrationAvailable(): Boolean = config.enableRegistration
    }

    private companion object {
        val credentials = AuthCredentials(Token("access"), RefreshToken("refresh"))
    }
}
