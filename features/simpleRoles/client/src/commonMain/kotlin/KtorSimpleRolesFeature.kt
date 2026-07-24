package dev.inmo.wishlist.features.simpleRoles.client

import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.wishlist.features.simpleRoles.common.Constants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess

/**
 * HTTP-only [SimpleRolesFeature] realization (issue #68 point 10, realization 1 of 2). Performs no
 * caching or business logic, per the "Ktor realization rule" (`agents/CODING.md`).
 *
 * @param client Shared app-wide [HttpClient].
 */
class KtorSimpleRolesFeature(private val client: HttpClient) : SimpleRolesFeature {
    /** Relative path of the superadmin-status endpoint (`simpleRoles/isSuperAdmin`), built from the shared [Constants]. */
    private val isSuperAdminPath = "${Constants.prefixPathPart}/${Constants.isSuperAdminPathPart}"

    /**
     * Calls `GET /simpleRoles/isSuperAdmin`.
     *
     * @return The decoded response body; `false` on any non-success status or request failure
     *   (network error, missing/expired bearer token before refresh, etc.).
     */
    override suspend fun isSuperAdmin(): Boolean = runCatchingLogging {
        val response = client.get(isSuperAdminPath)
        if (!response.status.isSuccess()) return@runCatchingLogging false
        response.body<Boolean>()
    }.getOrDefault(false)
}
