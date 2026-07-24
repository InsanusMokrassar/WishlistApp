package dev.inmo.wishlist.features.simpleRoles.client

import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge

/**
 * Caching [SimpleRolesFeature] realization (issue #68 point 10, realization 2 of 2). Caches only the
 * boolean answer, keyed off [meState] rather than a bare login/logout flag — this also re-fetches
 * when the authenticated *identity* changes within one session (e.g. logout then a different login),
 * not only on a true/false auth-state flip. Mirrors `features/auth/client/Plugin.kt`'s `meStateFlow`
 * refresh pattern exactly.
 *
 * Exposes [isSuperAdminStateFlow] beyond the [SimpleRolesFeature] interface, following the "Typed
 * definition & accessor helpers" pattern (`agents/CODING.md`) rather than widening the deliberately
 * narrow point-7 interface — see `simpleRoles/README.md` Architecture Notes.
 *
 * @param delegate HTTP realization this class refreshes its cache from.
 * @param meState Authenticated-caller state; both a login/logout signal and an identity-change signal.
 * @param scope Coroutine scope the refresh subscription runs on.
 */
class CacheSimpleRolesFeature(
    private val delegate: KtorSimpleRolesFeature,
    private val meState: StateFlow<RegisteredUser?>,
    scope: CoroutineScope
) : SimpleRolesFeature {
    /**
     * In-memory cache of the caller's SuperAdmin answer, refreshed on every [meState] emission and
     * exposed reactively through [isSuperAdminStateFlow]. Defaults to `false` (fail-closed) until the
     * first refresh completes.
     */
    private val cached = MutableStateFlow(false)

    /** Read-only view of the cached answer, for consumers that need reactive access (e.g. `ui/users`). */
    val isSuperAdminStateFlow: StateFlow<Boolean> = cached.asStateFlow()

    init {
        merge(flowOf(Unit), meState).subscribeLoggingDropExceptions(scope) {
            cached.value = delegate.isSuperAdmin()
        }
    }

    /**
     * Returns the last cached answer. Never itself performs a network call — always up to date
     * within one [meState] emission's latency (login, logout, or identity change).
     */
    override suspend fun isSuperAdmin(): Boolean = cached.value
}
