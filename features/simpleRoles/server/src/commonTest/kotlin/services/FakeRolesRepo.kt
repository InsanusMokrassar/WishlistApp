package dev.inmo.wishlist.features.simpleRoles.server.services

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.kroles.repos.ReadRolesRepo
import dev.inmo.kroles.roles.BaseRole
import dev.inmo.micro_utils.pagination.Pagination
import dev.inmo.micro_utils.pagination.PaginationResult
import dev.inmo.micro_utils.pagination.createPaginationResult

/**
 * Minimal in-memory [ReadRolesRepo] test double: a fixed `BaseRoleSubject -> Set<BaseRole>` map with
 * no hierarchy/transitive-role support (not needed — [SimpleRolesFeatureService] only calls
 * [contains] directly). Module-local, mirroring `email/server`'s `FakeUsersRepo` convention.
 *
 * @param grants Fixed subject-to-roles seed data.
 */
internal class FakeRolesRepo(
    private val grants: Map<BaseRoleSubject, Set<BaseRole>> = emptyMap()
) : ReadRolesRepo {
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
}
