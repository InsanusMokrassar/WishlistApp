package dev.inmo.wishlist.features.roles.client

import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.wishlist.features.roles.common.FunctionalityId
import dev.inmo.wishlist.features.roles.common.RolesConstants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess

/**
 * HTTP-only [RolesFeature] realization. Performs no caching or business logic, per the "Ktor
 * realization rule" (`agents/CODING.md`); reactive UI caching lives in the consuming Model layer.
 *
 * @param client Shared app-wide [HttpClient].
 */
class KtorRolesFeature(private val client: HttpClient) : RolesFeature {
    /**
     * Calls `GET /roles/isFunctionalityAvailable/{functionalityId}`.
     *
     * @return The decoded response body; `false` on any non-success status or request failure
     *   (network error, missing/expired bearer token, etc.).
     */
    override suspend fun isFunctionalityAvailable(functionalityId: FunctionalityId): Boolean =
        runCatchingLogging {
            val path = "${RolesConstants.prefixPathPart}/${RolesConstants.isFunctionalityAvailablePathPart}/${functionalityId.string}"
            val response = client.get(path)
            if (!response.status.isSuccess()) return@runCatchingLogging false
            response.body<Boolean>()
        }.getOrDefault(false)
}
