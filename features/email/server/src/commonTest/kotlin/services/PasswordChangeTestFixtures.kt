package dev.inmo.wishlist.features.email.server.services

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.micro_utils.repos.set
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.server.UserRoleAuthorization
import dev.inmo.wishlist.features.auth.server.repo.PasswordsRepo
import dev.inmo.wishlist.features.auth.server.services.AuthFeatureService
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerInfo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.common.repo.DeepLinksRepo
import dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.EmailsService
import dev.inmo.wishlist.features.email.server.models.EmailPasswordChange
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.UsersRepo

/** Suspendable interception point executed around one delegated repository operation. */
internal typealias PasswordChangeRepositoryHook = suspend () -> Unit

/** Mutable direct User-role decision used by the real Auth service in password-approval tests. */
internal class PasswordChangeRoleAuthorization(
    /** Current direct authorization result. */
    var directRolePresent: Boolean = true,
) : UserRoleAuthorization {
    /** Registration is outside this fixture and never creates a role. */
    override suspend fun ensureUserRole(userId: UserId): Boolean = directRolePresent

    /** Returns the current direct authorization result. */
    override suspend fun hasUserRole(userId: UserId): Boolean = directRolePresent
}

/** Map-backed password store with suspendable read/write hooks and isolated write counters. */
internal class PasswordChangePasswordsRepo(
    /** Underlying in-memory password storage. */
    private val delegate: MapKeyValueRepo<UserId, Password> = MapKeyValueRepo(),
) : PasswordsRepo, KeyValueRepo<UserId, Password> by delegate {
    /** Hook executed before a delegated password read. */
    var beforeGet: PasswordChangeRepositoryHook? = null

    /** Hook executed after a delegated password read. */
    var afterGet: PasswordChangeRepositoryHook? = null

    /** Hook executed before a delegated password batch write. */
    var beforeSet: PasswordChangeRepositoryHook? = null

    /** Hook executed after a delegated password batch write. */
    var afterSet: PasswordChangeRepositoryHook? = null

    /** Hook executed before a delegated password batch removal. */
    var beforeUnset: PasswordChangeRepositoryHook? = null

    /** Hook executed after a delegated password batch removal. */
    var afterUnset: PasswordChangeRepositoryHook? = null

    /** Password batch writes observed after fixture bootstrap. */
    var issuedPasswordWriteCount: Int = 0
        private set

    /** Runs the configured hooks around a delegated password read. */
    override suspend fun get(k: UserId): Password? {
        beforeGet?.invoke()
        return delegate.get(k).also { afterGet?.invoke() }
    }

    /** Runs the configured hooks around a delegated password batch write. */
    override suspend fun set(toSet: Map<UserId, Password>) {
        beforeSet?.invoke()
        delegate.set(toSet)
        issuedPasswordWriteCount++
        afterSet?.invoke()
    }

    /** Runs the configured hooks around a delegated password batch removal. */
    override suspend fun unset(toUnset: List<UserId>) {
        beforeUnset?.invoke()
        delegate.unset(toUnset)
        afterUnset?.invoke()
    }

    /** Clears bootstrap writes before an issuance assertion begins. */
    fun resetIssuedPasswordWriteCount() {
        issuedPasswordWriteCount = 0
    }
}

/** Map-backed deeplink store with suspendable read/write hooks and exact-id operation records. */
internal class PasswordChangeDeepLinksRepo(
    /** Underlying in-memory deeplink storage. */
    private val delegate: MapKeyValueRepo<DeepLinkId, DeepLinkHandlerInfo> = MapKeyValueRepo(),
) : DeepLinksRepo, KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo> by delegate {
    /** Hook executed before a delegated deeplink read. */
    var beforeGet: PasswordChangeRepositoryHook? = null

    /** Hook executed after a delegated deeplink read. */
    var afterGet: PasswordChangeRepositoryHook? = null

    /** Hook executed before a delegated deeplink batch write. */
    var beforeSet: PasswordChangeRepositoryHook? = null

    /** Hook executed after a delegated deeplink batch write. */
    var afterSet: PasswordChangeRepositoryHook? = null

    /** Hook executed before a delegated deeplink batch removal. */
    var beforeUnset: PasswordChangeRepositoryHook? = null

    /** Hook executed after a delegated deeplink batch removal. */
    var afterUnset: PasswordChangeRepositoryHook? = null

    /** Ordered identifiers submitted to delegated deeplink writes. */
    val setIds = mutableListOf<DeepLinkId>()

    /** Ordered identifiers submitted to delegated deeplink reads. */
    val getIds = mutableListOf<DeepLinkId>()

    /** Ordered identifiers submitted to delegated deeplink removals. */
    val unsetIds = mutableListOf<DeepLinkId>()

    /** Runs the configured hooks around a delegated deeplink read. */
    override suspend fun get(k: DeepLinkId): DeepLinkHandlerInfo? {
        getIds += k
        beforeGet?.invoke()
        return delegate.get(k).also { afterGet?.invoke() }
    }

    /** Runs the configured hooks around a delegated deeplink batch write. */
    override suspend fun set(toSet: Map<DeepLinkId, DeepLinkHandlerInfo>) {
        setIds += toSet.keys
        beforeSet?.invoke()
        delegate.set(toSet)
        afterSet?.invoke()
    }

    /** Runs the configured hooks around a delegated deeplink batch removal. */
    override suspend fun unset(toUnset: List<DeepLinkId>) {
        unsetIds += toUnset
        beforeUnset?.invoke()
        delegate.unset(toUnset)
        afterUnset?.invoke()
    }

    /** Persists a non-issued record without adding issuance-operation evidence. */
    suspend fun seed(id: DeepLinkId, info: DeepLinkHandlerInfo) {
        delegate.set(id, info)
    }

    /** Removes historical operation records before one focused assertion. */
    fun resetOperationRecords() {
        setIds.clear()
        getIds.clear()
        unsetIds.clear()
    }
}

/** Delegating user repository whose current-user reads can fail before or after persistence access. */
internal class PasswordChangeUsersRepo(
    /** Actual in-memory users implementation used for all unmodified operations. */
    private val delegate: UsersRepo,
) : UsersRepo by delegate {
    /** Hook executed before a delegated current-user read. */
    var beforeGetById: PasswordChangeRepositoryHook? = null

    /** Hook executed after a delegated current-user read. */
    var afterGetById: PasswordChangeRepositoryHook? = null

    /** Runs configured hooks around the current-user read used by coordinator and Auth. */
    override suspend fun getById(id: UserId): RegisteredUser? {
        beforeGetById?.invoke()
        return delegate.getById(id).also { afterGetById?.invoke() }
    }
}

/** Fully wired real Email/Auth/deeplink fixture for one approved password-change account. */
internal class PasswordChangeFixture(
    /** Approved account that requests password-change emails. */
    val user: RegisteredUser,
    /** Mutable users storage behind the coordinator and Auth service. */
    val users: FakeUsersRepo,
    /** Hooking users facade shared by the coordinator and Auth service. */
    val trackedUsers: PasswordChangeUsersRepo,
    /** Mutable direct-role authorization exposed to Auth. */
    val roles: PasswordChangeRoleAuthorization,
    /** Hooking password storage used by the real Auth service. */
    val passwords: PasswordChangePasswordsRepo,
    /** Real Auth password and credential-state service. */
    val auth: AuthFeatureService,
    /** Real email-account coordinator. */
    val coordinator: EmailVerificationAccountCoordinator,
    /** Hooking persistence store behind the real deeplink service. */
    val linksRepo: PasswordChangeDeepLinksRepo,
    /** Real deeplink mint/read/remove service. */
    val links: DeepLinksService,
    /** Real Email password-change orchestration service. */
    val service: EmailPasswordChangeService,
)

/** Builds real-service password-change fixtures with only repositories and SMTP controlled by tests. */
internal object PasswordChangeTestFixtures {
    /** Approved account shared by the issuance and completion suites. */
    val user = RegisteredUser(
        id = UserId(7L),
        username = Username("owner"),
        email = Email("owner@example.com"),
        emailApproved = true,
    )

    /** Bootstrap password used to establish a real Auth credential state. */
    val oldPassword = Password("old-password")

    /** Builds a fixture with a deferred handler provider matching production's DI cycle break. */
    suspend fun fixture(
        emails: EmailsService? = FakeEmailsService(),
        nowEpochMillis: () -> Long = { 1_000L },
        linksRepo: PasswordChangeDeepLinksRepo = PasswordChangeDeepLinksRepo(),
        user: RegisteredUser = this.user,
        roleBridgePresent: Boolean = true,
    ): PasswordChangeFixture {
        val users = FakeUsersRepo(mapOf(user.id to user))
        val trackedUsers = PasswordChangeUsersRepo(users)
        val roles = PasswordChangeRoleAuthorization()
        val passwords = PasswordChangePasswordsRepo()
        val auth = AuthFeatureService(
            usersRepo = trackedUsers,
            writeUsersRepo = trackedUsers,
            passwordsRepo = passwords,
            userRoleAuthorization = roles.takeIf { roleBridgePresent },
        )
        auth.setPassword(user.id, oldPassword)
        passwords.resetIssuedPasswordWriteCount()
        val coordinator = EmailVerificationAccountCoordinator(trackedUsers, FakeRolesRepo())
        lateinit var service: EmailPasswordChangeService
        val links = DeepLinksService(
            linksRepo,
            listOf(EmailPasswordChangeDeepLinkHandler { service }),
        )
        service = EmailPasswordChangeService(
            emailsService = emails,
            deepLinksService = links,
            accountCoordinator = coordinator,
            authFeatureService = auth,
            publicHttpOrigin = "https://wishlist.example",
            nowEpochMillis = nowEpochMillis,
        )
        return PasswordChangeFixture(user, users, trackedUsers, roles, passwords, auth, coordinator, linksRepo, links, service)
    }
}
