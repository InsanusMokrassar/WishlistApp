package dev.inmo.wishlist.features.email.server.services

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.kroles.repos.RolesRepo
import dev.inmo.kroles.roles.BaseRole
import dev.inmo.micro_utils.pagination.Pagination
import dev.inmo.micro_utils.pagination.PaginationResult
import dev.inmo.micro_utils.pagination.createPaginationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** In-memory role repository used by email verification handler tests. */
internal class FakeRolesRepo : RolesRepo {
    private val grants = mutableMapOf<BaseRoleSubject, MutableSet<BaseRole>>()
    private val _roleIncluded = MutableSharedFlow<Pair<BaseRoleSubject, BaseRole>>()
    private val _roleExcluded = MutableSharedFlow<Pair<BaseRoleSubject, BaseRole>>()
    private val _roleCreated = MutableSharedFlow<BaseRole>()
    private val _roleRemoved = MutableSharedFlow<BaseRole>()

    /** Role inclusion events emitted by the fake repository. */
    override val roleIncluded: Flow<Pair<BaseRoleSubject, BaseRole>> = _roleIncluded.asSharedFlow()

    /** Role exclusion events emitted by the fake repository. */
    override val roleExcluded: Flow<Pair<BaseRoleSubject, BaseRole>> = _roleExcluded.asSharedFlow()

    /** Role creation events emitted by the fake repository. */
    override val roleCreated: Flow<BaseRole> = _roleCreated.asSharedFlow()

    /** Role removal events emitted by the fake repository. */
    override val roleRemoved: Flow<BaseRole> = _roleRemoved.asSharedFlow()

    /** Returns subjects holding [role]. */
    override suspend fun getDirectSubjects(role: BaseRole): List<BaseRoleSubject> =
        grants.filterValues { role in it }.keys.toList()

    /** Returns directly granted roles for [subject]. */
    override suspend fun getDirectRoles(subject: BaseRoleSubject): List<BaseRole> =
        grants[subject]?.toList() ?: emptyList()

    /** Returns all direct grants. */
    override suspend fun getAll(): Map<BaseRoleSubject, List<BaseRole>> =
        grants.mapValues { it.value.toList() }

    /** Returns a paginated role list. */
    override suspend fun getAllRolesByPagination(pagination: Pagination, reversed: Boolean): PaginationResult<BaseRole> {
        val roles = grants.values.flatten().distinct().let { if (reversed) it.reversed() else it }
        return roles.createPaginationResult(pagination, roles.size.toLong())
    }

    /** Returns a paginated subject list. */
    override suspend fun getAllSubjectsByPagination(pagination: Pagination, reversed: Boolean): PaginationResult<BaseRoleSubject> {
        val subjects = grants.keys.toList().let { if (reversed) it.reversed() else it }
        return subjects.createPaginationResult(pagination, subjects.size.toLong())
    }

    /** Checks whether [subject] directly holds [role]. */
    override suspend fun contains(subject: BaseRoleSubject, role: BaseRole): Boolean =
        role in (grants[subject] ?: emptySet())

    /** Checks whether [subject] holds at least one requested role. */
    override suspend fun containsAny(subject: BaseRoleSubject, roles: List<BaseRole>): Boolean =
        (grants[subject] ?: emptySet()).any { it in roles }

    /** Adds [role] to [subject] idempotently. */
    override suspend fun includeDirect(subject: BaseRoleSubject, role: BaseRole): Boolean {
        val changed = grants.getOrPut(subject) { mutableSetOf() }.add(role)
        if (changed) _roleIncluded.emit(subject to role)
        return changed
    }

    /** Removes [role] from [subject] idempotently. */
    override suspend fun excludeDirect(subject: BaseRoleSubject, role: BaseRole): Boolean {
        val changed = grants[subject]?.remove(role) ?: false
        if (changed) _roleExcluded.emit(subject to role)
        return changed
    }

    /** Records a role creation request. */
    override suspend fun createRole(newRole: BaseRole): Boolean = true.also { _roleCreated.emit(newRole) }

    /** Removes [role] from every subject. */
    override suspend fun removeRole(role: BaseRole): Boolean {
        var changed = false
        grants.values.forEach { changed = it.remove(role) || changed }
        if (changed) _roleRemoved.emit(role)
        return changed
    }
}
