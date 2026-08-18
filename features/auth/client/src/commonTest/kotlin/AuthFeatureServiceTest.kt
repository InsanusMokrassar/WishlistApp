package dev.inmo.wishlist.features.auth.client

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.wishlist.features.auth.common.models.AuthConfig
import dev.inmo.wishlist.features.auth.common.models.AuthCredentials
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.RefreshToken
import dev.inmo.wishlist.features.auth.common.models.RegistrationResult
import dev.inmo.wishlist.features.auth.common.models.Token
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Verifies that only authorized registration results are persisted as sessions. */
class AuthFeatureServiceTest {
    /** Pending registration leaves empty credential storage untouched. */
    @Test
    fun pendingRegistrationDoesNotWriteCredentials() = runTest {
        val storage = RecordingStorage()
        val transport = FakeTransport(RegistrationResult.PendingEmailVerification)

        val result = AuthFeatureService(storage, transport).register(Username("alice"), Password("password"))

        assertEquals(RegistrationResult.PendingEmailVerification, result)
        assertEquals(0, storage.saveCalls)
        assertFalse(storage.userAuthorised.value)
    }

    /** Authorized registration persists the returned credentials exactly once. */
    @Test
    fun authorizedRegistrationPersistsCredentialsOnce() = runTest {
        val credentials = AuthCredentials(Token("access"), RefreshToken("refresh"))
        val storage = RecordingStorage()
        val transport = FakeTransport(RegistrationResult.Authorized(credentials))

        val result = AuthFeatureService(storage, transport).register(Username("alice"), Password("password"))

        assertEquals(RegistrationResult.Authorized(credentials), result)
        assertEquals(1, storage.saveCalls)
        assertEquals(credentials, storage.get())
        assertTrue(storage.userAuthorised.value)
    }
}

/** Minimal in-memory credentials storage that records writes. */
private class RecordingStorage : AuthCredentialsStorage {
    /** Reactive authorization flag derived from the stored credential value. */
    private val authorised = MutableRedeliverStateFlow(false)
    /** Last credentials received by the fixture. */
    private var credentials: AuthCredentials? = null

    /** Number of storage writes. */
    var saveCalls = 0
        private set

    override val userAuthorised: StateFlow<Boolean> = authorised.asStateFlow()

    override suspend fun get(): AuthCredentials? = credentials

    override suspend fun save(credentials: AuthCredentials?) {
        saveCalls++
        this.credentials = credentials
        authorised.value = credentials != null
    }
}

/** Client transport fixture returning a configured registration outcome. */
private class FakeTransport(
    /** Registration outcome emitted by every registration request. */
    private val registrationResult: RegistrationResult?,
) : ClientAuthFeature {
    override suspend fun login(username: Username, password: Password): AuthCredentials? = null
    override suspend fun refresh(refreshToken: RefreshToken): AuthCredentials? = null
    override suspend fun logout() = Unit
    override suspend fun getMe(): AuthFeatureUser? = null
    override suspend fun register(username: Username, password: Password): RegistrationResult? = registrationResult
    override suspend fun isRegistrationAvailable(): Boolean = false
    override suspend fun getConfig(): AuthConfig = AuthConfig()
}
