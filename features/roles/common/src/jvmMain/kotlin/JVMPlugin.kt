package dev.inmo.wishlist.features.roles.common

import dev.inmo.kroles.repos.RolesRepo
import dev.inmo.micro_utils.koin.singleWithBinds
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.roles.common.repo.cachedRolesRepo
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * JVM startup plugin for the `roles` feature.
 *
 * Registers the Exposed-backed, cache-mirrored [RolesRepo] (see
 * [dev.inmo.wishlist.features.roles.common.repo.cachedRolesRepo]) as [RolesRepo],
 * [dev.inmo.kroles.repos.ReadRolesRepo], and [dev.inmo.kroles.repos.WriteRolesRepo] simultaneously,
 * mirroring this codebase's CRUD Repository Pattern DI wiring (`agents/CODING.md`). The shared
 * `Database`/`CoroutineScope` singletons (registered by `features/common/server/JVMPlugin`) are
 * resolved by plain `get()`, not by delegating to that plugin's `setupDI` — per `agents/CODING.md`'s
 * rule that a plugin may only call `setupDI`/`startPlugin` of a plugin within its own feature.
 */
object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(Plugin) { setupDI(config) }

        singleWithBinds<RolesRepo> {
            cachedRolesRepo(database = get(), scope = get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
