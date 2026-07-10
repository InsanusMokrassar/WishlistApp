package dev.inmo.wishlist.features.roles.server

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.kroles.repos.RolesRepo
import dev.inmo.kroles.roles.BaseRole
import dev.inmo.micro_utils.pagination.Pagination
import dev.inmo.micro_utils.pagination.PaginationResult
import dev.inmo.micro_utils.pagination.createPaginationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * In-memory [RolesRepo] test double backed by a plain `MutableMap<BaseRoleSubject, MutableSet<BaseRole>>`
 * — no hierarchy/transitive-role support beyond [RolesRepo]'s own default [getAllRoles] (sufficient
 * for this app's flat `SuperAdmin`/`User` roles; see `roles/README.md` Architecture Notes). The two
 * paginated methods ignore true page slicing (no test in this module exercises them) but stay
 * correctly typed so the class compiles as a full [RolesRepo]. Module-local fixture, mirroring
 * `email/server`'s `FakeUsersRepo` convention.
 */
internal class FakeRolesRepo : RolesRepo {
    private val grants = mutableMapOf<BaseRoleSubject, MutableSet<BaseRole>>()

    private val _roleIncluded = MutableSharedFlow<Pair<BaseRoleSubject, BaseRole>>()
    override val roleIncluded: Flow<Pair<BaseRoleSubject, BaseRole>> = _roleIncluded.asSharedFlow()
    private val _roleExcluded = MutableSharedFlow<Pair<BaseRoleSubject, BaseRole>>()
    override val roleExcluded: Flow<Pair<BaseRoleSubject, BaseRole>> = _roleExcluded.asSharedFlow()
    private val _roleCreated = MutableSharedFlow<BaseRole>()
    override val roleCreated: Flow<BaseRole> = _roleCreated.asSharedFlow()
    private val _roleRemoved = MutableSharedFlow<BaseRole>()
    override val roleRemoved: Flow<BaseRole> = _roleRemoved.asSharedFlow()

    override suspend fun getDirectSubjects(role: BaseRole): List<BaseRoleSubject> =
        grants.filterValues { role in it }.keys.toList()

    override suspend fun getDirectRoles(subject: BaseRoleSubject): List<BaseRole> =
        grants[subject]?.toList() ?: emptyList()

    override suspend fun getAll(): Map<BaseRoleSubject, List<BaseRole>> =
        grants.mapValues { it.value.toList() }

    override suspend fun getAllRolesByPagination(pagination: Pagination, reversed: Boolean): PaginationResult<BaseRole> {
        val allRoles = grants.values.flatten().distinct().let { if (reversed) it.reversed() else it }
        return allRoles.createPaginationResult(pagination, allRoles.size.toLong())
    }

    override suspend fun getAllSubjectsByPagination(pagination: Pagination, reversed: Boolean): PaginationResult<BaseRoleSubject> {
        val allSubjects = grants.keys.toList().let { if (reversed) it.reversed() else it }
        return allSubjects.createPaginationResult(pagination, allSubjects.size.toLong())
    }

    override suspend fun contains(subject: BaseRoleSubject, role: BaseRole): Boolean =
        role in (grants[subject] ?: emptySet())

    override suspend fun containsAny(subject: BaseRoleSubject, roles: List<BaseRole>): Boolean =
        (grants[subject] ?: emptySet()).any { it in roles }

    override suspend fun includeDirect(subject: BaseRoleSubject, role: BaseRole): Boolean {
        val changed = grants.getOrPut(subject) { mutableSetOf() }.add(role)
        if (changed) _roleIncluded.emit(subject to role)
        return changed
    }

    override suspend fun excludeDirect(subject: BaseRoleSubject, role: BaseRole): Boolean {
        val changed = grants[subject]?.remove(role) ?: false
        if (changed) _roleExcluded.emit(subject to role)
        return changed
    }

    override suspend fun createRole(newRole: BaseRole): Boolean = true.also { _roleCreated.emit(newRole) }

    override suspend fun removeRole(role: BaseRole): Boolean {
        var removedAny = false
        grants.values.forEach { removedAny = it.remove(role) || removedAny }
        if (removedAny) _roleRemoved.emit(role)
        return removedAny
    }
}
