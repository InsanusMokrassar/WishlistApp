package dev.inmo.wishlist.features.roles.server

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.wishlist.features.roles.common.SuperAdminRole
import dev.inmo.wishlist.features.roles.common.UserRole
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies [grantDefaultRoles] (the per-user grant rule and its idempotency), [backfillDefaultRoles]
 * (the one-time migration body and its idempotency across repeated runs), and the reactive-subscription
 * *pattern* [dev.inmo.wishlist.features.roles.server.JVMPlugin.startPlugin] wires up — replicated here
 * against a real [FakeUsersRepo.newObjectsFlow] (the same MicroUtils `MapCRUDRepo`/`WriteMapCRUDRepo`
 * machinery the production `CacheUsersRepo` is ultimately built on) since exercising `JVMPlugin` itself
 * would require a live Koin/Ktor/Postgres boot, outside this repo's unit-test convention.
 */
class RolesBootstrapTest {

    private val rootUser = RegisteredUser(UserId(1L), Username("root"))
    private val plainUser = RegisteredUser(UserId(2L), Username("alice"))

    /** Non-root user → only User is granted, never SuperAdmin. */
    @Test
    fun grantDefaultRolesGrantsOnlyUserRoleForNonRootUser() = runTest {
        val rolesRepo = FakeRolesRepo()

        grantDefaultRoles(rolesRepo, plainUser)

        val subject = BaseRoleSubject.Direct(plainUser.id.long.toString())
        assertTrue(rolesRepo.contains(subject, UserRole))
        assertFalse(rolesRepo.contains(subject, SuperAdminRole))
    }

    /** A user named `root` → both User and SuperAdmin are granted. */
    @Test
    fun grantDefaultRolesGrantsUserAndSuperAdminRoleForRootUser() = runTest {
        val rolesRepo = FakeRolesRepo()

        grantDefaultRoles(rolesRepo, rootUser)

        val subject = BaseRoleSubject.Direct(rootUser.id.long.toString())
        assertTrue(rolesRepo.contains(subject, UserRole))
        assertTrue(rolesRepo.contains(subject, SuperAdminRole))
    }

    /** Calling [grantDefaultRoles] twice for the same user is a no-op the second time — no error, no duplicate grant. */
    @Test
    fun grantDefaultRolesIsIdempotent() = runTest {
        val rolesRepo = FakeRolesRepo()

        grantDefaultRoles(rolesRepo, rootUser)
        grantDefaultRoles(rolesRepo, rootUser)

        val subject = BaseRoleSubject.Direct(rootUser.id.long.toString())
        assertEquals(
            setOf(UserRole, SuperAdminRole),
            rolesRepo.getDirectRoles(subject).toSet()
        )
    }

    /** [backfillDefaultRoles] grants User to every pre-existing user and SuperAdmin only to `root`. */
    @Test
    fun backfillDefaultRolesGrantsRolesToAllPreExistingUsers() = runTest {
        val usersRepo = FakeUsersRepo(mapOf(rootUser.id to rootUser, plainUser.id to plainUser))
        val rolesRepo = FakeRolesRepo()

        backfillDefaultRoles(usersRepo, rolesRepo)

        assertTrue(rolesRepo.contains(BaseRoleSubject.Direct(rootUser.id.long.toString()), SuperAdminRole))
        assertTrue(rolesRepo.contains(BaseRoleSubject.Direct(rootUser.id.long.toString()), UserRole))
        assertTrue(rolesRepo.contains(BaseRoleSubject.Direct(plainUser.id.long.toString()), UserRole))
        assertFalse(rolesRepo.contains(BaseRoleSubject.Direct(plainUser.id.long.toString()), SuperAdminRole))
    }

    /**
     * Running [backfillDefaultRoles] a second time (simulating what a non-version-gated re-run would
     * look like) does not change the outcome — verifies the migration body itself is safe to
     * double-run, independent of whatever gates how many times production actually invokes it
     * (`VersionsRepo.setTableVersion`, not re-tested here — it is already-tested library code with no
     * app-specific branching).
     */
    @Test
    fun backfillDefaultRolesIsIdempotentAcrossRepeatedRuns() = runTest {
        val usersRepo = FakeUsersRepo(mapOf(rootUser.id to rootUser, plainUser.id to plainUser))
        val rolesRepo = FakeRolesRepo()

        backfillDefaultRoles(usersRepo, rolesRepo)
        val afterFirstRun = rolesRepo.getAll().mapValues { it.value.toSet() }
        backfillDefaultRoles(usersRepo, rolesRepo)
        val afterSecondRun = rolesRepo.getAll().mapValues { it.value.toSet() }

        assertEquals(afterFirstRun, afterSecondRun)
    }

    /**
     * Replicates [dev.inmo.wishlist.features.roles.server.JVMPlugin.startPlugin]'s
     * `usersRepo.newObjectsFlow.subscribeLoggingDropExceptions(scope) { user -> grantDefaultRoles(rolesRepo, user) }`
     * subscription pattern directly against [FakeUsersRepo.newObjectsFlow]: a user created *after* the
     * subscription starts is granted default roles reactively. Uses [UnconfinedTestDispatcher] so the
     * launched collector coroutine actively subscribes before `create(...)` runs and processes the
     * emission synchronously within the same test step — the standard idiom for testing a
     * flow-triggered side effect deterministically, avoiding a `StandardTestDispatcher` race between
     * "collector scheduled" and "collector actually subscribed."
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun reactiveSubscriptionGrantsDefaultRolesOnNewUserCreation() = runTest(UnconfinedTestDispatcher()) {
        val usersRepo = FakeUsersRepo()
        val rolesRepo = FakeRolesRepo()

        val job = launch {
            usersRepo.newObjectsFlow.collect { user -> grantDefaultRoles(rolesRepo, user) }
        }

        usersRepo.create(listOf(NewUser(Username("root"))))
        usersRepo.create(listOf(NewUser(Username("bob"))))

        val createdRoot = usersRepo.getUserByUsername(Username("root"))!!
        val createdBob = usersRepo.getUserByUsername(Username("bob"))!!

        assertTrue(rolesRepo.contains(BaseRoleSubject.Direct(createdRoot.id.long.toString()), SuperAdminRole))
        assertTrue(rolesRepo.contains(BaseRoleSubject.Direct(createdRoot.id.long.toString()), UserRole))
        assertTrue(rolesRepo.contains(BaseRoleSubject.Direct(createdBob.id.long.toString()), UserRole))
        assertFalse(rolesRepo.contains(BaseRoleSubject.Direct(createdBob.id.long.toString()), SuperAdminRole))

        job.cancel()
    }
}
