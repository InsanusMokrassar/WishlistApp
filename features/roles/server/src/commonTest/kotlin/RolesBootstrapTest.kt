package dev.inmo.wishlist.features.roles.server

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.kroles.repos.RolesRepo
import dev.inmo.kroles.roles.BaseRole
import dev.inmo.micro_utils.repos.deleteById
import dev.inmo.wishlist.features.roles.common.models.SuperAdminRole
import dev.inmo.wishlist.features.roles.common.models.NewUserRole
import dev.inmo.wishlist.features.roles.common.models.UserRole
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Verifies [grantDefaultRoles] (the per-user grant rule and its idempotency), [backfillDefaultRoles]
 * (the one-time migration body and its idempotency across repeated runs), and the reactive-subscription
 * *pattern* [dev.inmo.wishlist.features.roles.server.JVMPlugin.startPlugin] wires up — replicated here
 * against a real [FakeUsersRepo.newObjectsFlow] (the same MicroUtils `MapCRUDRepo`/`WriteMapCRUDRepo`
 * machinery the production `CacheUsersRepo` is ultimately built on) since exercising `JVMPlugin` itself
 * would require a live Koin/Ktor/Postgres boot, outside this repo's unit-test convention.
 */
class RolesBootstrapTest {

    /** Root account fixture used to verify the privileged approved-role state. */
    private val rootUser = RegisteredUser(UserId(1L), Username("root"))

    /** Non-root account fixture used to verify pending and approved transitions. */
    private val plainUser = RegisteredUser(UserId(2L), Username("alice"))

    /** Optional registration receives exactly direct approved-user membership. */
    @Test
    fun ensureUserRoleGrantsAndConfirmsDirectUserRole() = runTest {
        val rolesRepo = FakeRolesRepo()
        val authorization = RolesUserRoleAuthorization(rolesRepo)
        val subject = BaseRoleSubject.Direct(plainUser.id.long.toString())

        assertTrue(authorization.ensureUserRole(plainUser.id))
        assertTrue(authorization.hasUserRole(plainUser.id))
        assertEquals(setOf(UserRole), rolesRepo.getDirectRoles(subject).toSet())
    }

    /** Pending accounts cannot be synchronously approved by optional-registration authorization. */
    @Test
    fun ensureUserRolePreservesPendingAndReturnsFalse() = runTest {
        val rolesRepo = FakeRolesRepo()
        val authorization = RolesUserRoleAuthorization(rolesRepo)
        val subject = BaseRoleSubject.Direct(plainUser.id.long.toString())
        rolesRepo.includeDirect(subject, NewUserRole)

        assertFalse(authorization.ensureUserRole(plainUser.id))
        assertFalse(authorization.hasUserRole(plainUser.id))
        assertEquals(setOf(NewUserRole), rolesRepo.getDirectRoles(subject).toSet())
    }

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

    /** Generic user creation remains approved regardless of self-registration email policy elsewhere. */
    @Test
    fun genericAdministratorCreationGrantsApprovedUserRole() = runTest {
        val rolesRepo = FakeRolesRepo()

        grantDefaultRoles(rolesRepo, plainUser)

        val subject = BaseRoleSubject.Direct(plainUser.id.long.toString())
        assertTrue(rolesRepo.contains(subject, UserRole))
        assertFalse(rolesRepo.contains(subject, NewUserRole))
    }

    /** Root remains fully privileged under the generic creation rule. */
    @Test
    fun grantDefaultRolesKeepsRootApproved() = runTest {
        val rolesRepo = FakeRolesRepo()

        grantDefaultRoles(rolesRepo, rootUser)

        val subject = BaseRoleSubject.Direct(rootUser.id.long.toString())
        assertTrue(rolesRepo.contains(subject, UserRole))
        assertTrue(rolesRepo.contains(subject, SuperAdminRole))
        assertFalse(rolesRepo.contains(subject, NewUserRole))
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
        assertFalse(rolesRepo.contains(BaseRoleSubject.Direct(plainUser.id.long.toString()), NewUserRole))
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

    /** Verification promotion removes NewUser and grants User, including on repeated calls. */
    @Test
    fun promoteNewUserToUserIsIdempotent() = runTest {
        val rolesRepo = FakeRolesRepo()
        val subject = BaseRoleSubject.Direct(plainUser.id.long.toString())
        rolesRepo.includeDirect(subject, NewUserRole)

        promoteNewUserToUser(rolesRepo, plainUser.id)
        promoteNewUserToUser(rolesRepo, plainUser.id)

        assertEquals(setOf(UserRole), rolesRepo.getDirectRoles(subject).toSet())
    }

    /** A later approval callback cannot restore ordinary access after an administrator revoked it. */
    @Test
    fun promoteNewUserToUserDoesNotRestoreRevokedAccess() = runTest {
        val rolesRepo = FakeRolesRepo()
        val subject = BaseRoleSubject.Direct(plainUser.id.long.toString())
        rolesRepo.includeDirect(subject, NewUserRole)
        assertTrue(promoteNewUserToUser(rolesRepo, plainUser.id))
        assertTrue(rolesRepo.excludeDirect(subject, UserRole))

        assertFalse(promoteNewUserToUser(rolesRepo, plainUser.id))
        assertTrue(rolesRepo.getDirectRoles(subject).isEmpty())
    }

    /** A failed direct User grant leaves NewUser available for the same link's later retry. */
    @Test
    fun grantFailureBeforeMutationRetainsPendingAndRetries() = runTest {
        val backingRepo = FakeRolesRepo()
        val subject = BaseRoleSubject.Direct(plainUser.id.long.toString())
        backingRepo.includeDirect(subject, NewUserRole)
        val rolesRepo = FaultingRolesRepo(backingRepo).apply {
            nextUserIncludeFailure = FaultPhase.BeforeMutation
        }

        rolesRepo.expectOperationFailure { promoteNewUserToUser(rolesRepo, plainUser.id) }

        assertEquals(setOf(NewUserRole), backingRepo.getDirectRoles(subject).toSet())
        assertTrue(promoteNewUserToUser(rolesRepo, plainUser.id))
        assertEquals(setOf(UserRole), backingRepo.getDirectRoles(subject).toSet())
    }

    /** A grant that commits before throwing leaves both markers so retry finishes pending cleanup. */
    @Test
    fun grantFailureAfterMutationRetriesPendingCleanup() = runTest {
        val backingRepo = FakeRolesRepo()
        val subject = BaseRoleSubject.Direct(plainUser.id.long.toString())
        backingRepo.includeDirect(subject, NewUserRole)
        val rolesRepo = FaultingRolesRepo(backingRepo).apply {
            nextUserIncludeFailure = FaultPhase.AfterMutation
        }

        rolesRepo.expectOperationFailure { promoteNewUserToUser(rolesRepo, plainUser.id) }

        assertEquals(setOf(NewUserRole, UserRole), backingRepo.getDirectRoles(subject).toSet())
        assertTrue(promoteNewUserToUser(rolesRepo, plainUser.id))
        assertEquals(setOf(UserRole), backingRepo.getDirectRoles(subject).toSet())
    }

    /** A failed pending-marker cleanup keeps User and NewUser until a retry can finish cleanup. */
    @Test
    fun removalFailureBeforeMutationRetriesCleanup() = runTest {
        val backingRepo = FakeRolesRepo()
        val subject = BaseRoleSubject.Direct(plainUser.id.long.toString())
        backingRepo.includeDirect(subject, NewUserRole)
        val rolesRepo = FaultingRolesRepo(backingRepo).apply {
            nextPendingExcludeFailure = FaultPhase.BeforeMutation
        }

        rolesRepo.expectOperationFailure { promoteNewUserToUser(rolesRepo, plainUser.id) }

        assertEquals(setOf(NewUserRole, UserRole), backingRepo.getDirectRoles(subject).toSet())
        assertTrue(promoteNewUserToUser(rolesRepo, plainUser.id))
        assertEquals(setOf(UserRole), backingRepo.getDirectRoles(subject).toSet())
    }

    /** A cleanup exception after removal leaves User and therefore retries idempotently. */
    @Test
    fun removalFailureAfterMutationRetriesIdempotently() = runTest {
        val backingRepo = FakeRolesRepo()
        val subject = BaseRoleSubject.Direct(plainUser.id.long.toString())
        backingRepo.includeDirect(subject, NewUserRole)
        val rolesRepo = FaultingRolesRepo(backingRepo).apply {
            nextPendingExcludeFailure = FaultPhase.AfterMutation
        }

        rolesRepo.expectOperationFailure { promoteNewUserToUser(rolesRepo, plainUser.id) }

        assertEquals(setOf(UserRole), backingRepo.getDirectRoles(subject).toSet())
        assertTrue(promoteNewUserToUser(rolesRepo, plainUser.id))
        assertEquals(setOf(UserRole), backingRepo.getDirectRoles(subject).toSet())
    }

    /** A false User insertion result is not confirmation and must not remove pending access. */
    @Test
    fun unconfirmedGrantDoesNotRemovePending() = runTest {
        val backingRepo = FakeRolesRepo()
        val subject = BaseRoleSubject.Direct(plainUser.id.long.toString())
        backingRepo.includeDirect(subject, NewUserRole)
        val rolesRepo = FaultingRolesRepo(backingRepo).apply {
            userIncludeReturnsFalse = true
        }

        assertFalse(promoteNewUserToUser(rolesRepo, plainUser.id))
        assertEquals(setOf(NewUserRole), backingRepo.getDirectRoles(subject).toSet())
    }

    /** Generic grant followed by the registration-specific marker converges on exactly NewUser. */
    @Test
    fun pendingTransitionWinsAfterGenericGrant() = runTest {
        val rolesRepo = FakeRolesRepo()
        val lifecycle = RolesRegistrationRoleLifecycle(rolesRepo)
        val subject = BaseRoleSubject.Direct(plainUser.id.long.toString())

        grantDefaultRoles(rolesRepo, plainUser)
        assertTrue(lifecycle.markPending(plainUser.id))

        assertEquals(setOf(NewUserRole), rolesRepo.getDirectRoles(subject).toSet())
    }

    /** A delayed generic callback preserves a pending state established first. */
    @Test
    fun genericGrantPreservesPendingTransitionEstablishedFirst() = runTest {
        val rolesRepo = FakeRolesRepo()
        val lifecycle = RolesRegistrationRoleLifecycle(rolesRepo)
        val subject = BaseRoleSubject.Direct(plainUser.id.long.toString())

        assertTrue(lifecycle.markPending(plainUser.id))
        grantDefaultRoles(rolesRepo, plainUser)

        assertEquals(setOf(NewUserRole), rolesRepo.getDirectRoles(subject).toSet())
    }

    /**
     * Approval completed before a delayed generic callback leaves exactly [UserRole] after the
     * callback resumes.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun delayedRequiredEmailCallbackCannotReaddPendingRoleAfterPromotion() = runTest {
        val callbackDispatcher = StandardTestDispatcher(testScheduler)
        val usersRepo = FakeUsersRepo()
        val rolesRepo = FakeRolesRepo()
        val callbackJob = launch(callbackDispatcher) {
            usersRepo.newObjectsFlow.collect { user ->
                grantDefaultRolesIfUserExists(usersRepo, rolesRepo, user)
            }
        }
        runCurrent()

        usersRepo.create(listOf(NewUser(Username("bob"))))
        val createdUser = usersRepo.getUserByUsername(Username("bob"))!!
        promoteNewUserToUser(rolesRepo, createdUser.id)

        advanceUntilIdle()

        assertEquals(
            setOf(UserRole),
            rolesRepo.getDirectRoles(BaseRoleSubject.Direct(createdUser.id.long.toString())).toSet()
        )
        callbackJob.cancel()
    }

    /** Backfill preserves an explicitly pending account instead of silently approving it. */
    @Test
    fun backfillPreservesExistingPendingRole() = runTest {
        val usersRepo = FakeUsersRepo(mapOf(plainUser.id to plainUser))
        val rolesRepo = FakeRolesRepo()
        val subject = BaseRoleSubject.Direct(plainUser.id.long.toString())
        rolesRepo.includeDirect(subject, NewUserRole)

        backfillDefaultRoles(usersRepo, rolesRepo)

        assertEquals(setOf(NewUserRole), rolesRepo.getDirectRoles(subject).toSet())
    }

    /** A creation callback that runs after deletion observes absence and creates no orphan roles. */
    @Test
    fun deletedUserIsRejectedByDelayedCreationCallback() = runTest {
        val usersRepo = FakeUsersRepo(mapOf(plainUser.id to plainUser))
        val rolesRepo = FakeRolesRepo()
        val subject = BaseRoleSubject.Direct(plainUser.id.long.toString())

        usersRepo.deleteById(plainUser.id)
        grantDefaultRolesIfUserExists(usersRepo, rolesRepo, plainUser)

        assertTrue(rolesRepo.getDirectRoles(subject).isEmpty())
    }

    /** Deletion cleanup removes roles granted before the account disappears and is idempotent. */
    @Test
    fun deletionCleanupRemovesAlreadyGrantedRoles() = runTest {
        val rolesRepo = FakeRolesRepo()
        val subject = BaseRoleSubject.Direct(plainUser.id.long.toString())
        grantDefaultRoles(rolesRepo, plainUser)

        removeDirectUserRoles(rolesRepo, plainUser.id)
        removeDirectUserRoles(rolesRepo, plainUser.id)

        assertTrue(rolesRepo.getDirectRoles(subject).isEmpty())
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
            usersRepo.newObjectsFlow.collect { user ->
                grantDefaultRolesIfUserExists(usersRepo, rolesRepo, user)
            }
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

    /** Real create/delete flows eventually remove every generic or pending direct role. */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun reactiveDeletionFlowRemovesAllDirectRoles() = runTest(UnconfinedTestDispatcher()) {
        val usersRepo = FakeUsersRepo()
        val rolesRepo = FakeRolesRepo()
        val lifecycle = RolesRegistrationRoleLifecycle(rolesRepo)
        val creationJob = launch {
            usersRepo.newObjectsFlow.collect { user ->
                grantDefaultRolesIfUserExists(usersRepo, rolesRepo, user)
            }
        }
        val deletionJob = launch {
            usersRepo.deletedObjectsIdsFlow.collect { userId ->
                removeDirectUserRoles(rolesRepo, userId)
            }
        }

        usersRepo.create(listOf(NewUser(Username("bob"))))
        val created = usersRepo.getUserByUsername(Username("bob"))!!
        assertTrue(lifecycle.markPending(created.id))
        usersRepo.deleteById(created.id)

        assertTrue(
            rolesRepo.getDirectRoles(BaseRoleSubject.Direct(created.id.long.toString())).isEmpty()
        )
        creationJob.cancel()
        deletionJob.cancel()
    }

    /** Fault timing around a direct role mutation. */
    private enum class FaultPhase {
        /** Throw before delegating to the backing repository. */
        BeforeMutation,

        /** Delegate first, then throw to simulate an uncertain completed mutation. */
        AfterMutation,
    }

    /**
     * Delegating roles repository that injects one-shot failures into the exact promotion mutations.
     *
     * @param delegate Backing in-memory repository preserving the production [RolesRepo] contract.
     */
    private class FaultingRolesRepo(
        private val delegate: RolesRepo,
    ) : RolesRepo by delegate {
        /** One-shot failure applied only while directly granting [UserRole]. */
        var nextUserIncludeFailure: FaultPhase? = null

        /** One-shot failure applied only while directly removing [NewUserRole]. */
        var nextPendingExcludeFailure: FaultPhase? = null

        /** Makes a User inclusion report no change without adding the role. */
        var userIncludeReturnsFalse: Boolean = false

        override suspend fun includeDirect(subject: BaseRoleSubject, role: BaseRole): Boolean {
            val failure = if (role == UserRole) {
                nextUserIncludeFailure.also { nextUserIncludeFailure = null }
            } else {
                null
            }
            if (failure == FaultPhase.BeforeMutation) throw IllegalStateException("include failed before mutation")
            if (role == UserRole && userIncludeReturnsFalse) return false
            val changed = delegate.includeDirect(subject, role)
            if (failure == FaultPhase.AfterMutation) throw IllegalStateException("include failed after mutation")
            return changed
        }

        override suspend fun excludeDirect(subject: BaseRoleSubject, role: BaseRole): Boolean {
            val failure = if (role == NewUserRole) {
                nextPendingExcludeFailure.also { nextPendingExcludeFailure = null }
            } else {
                null
            }
            if (failure == FaultPhase.BeforeMutation) throw IllegalStateException("exclude failed before mutation")
            val changed = delegate.excludeDirect(subject, role)
            if (failure == FaultPhase.AfterMutation) throw IllegalStateException("exclude failed after mutation")
            return changed
        }
    }

    /** Verifies that a suspension-capable promotion operation propagates the injected repository fault. */
    private suspend fun FaultingRolesRepo.expectOperationFailure(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected role operation to fail")
        } catch (_: IllegalStateException) {
        }
    }
}
