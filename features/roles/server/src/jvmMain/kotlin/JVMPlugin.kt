package dev.inmo.wishlist.features.roles.server

import dev.inmo.kroles.repos.RolesRepo
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.micro_utils.repos.versions.VersionsRepo
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.users.common.repo.UsersRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * JVM startup plugin for the `roles` server feature (issue #68 points 1–6).
 *
 * On [startPlugin]:
 * 1. **Subscribes first** to `UsersRepo.newObjectsFlow` — before reading any snapshot — so a user
 *    created concurrently by another plugin's `startPlugin` (e.g. `features/auth/server`'s root
 *    bootstrap; top-level plugins' `startPlugin`s run **concurrently**, not in `sample.config.json`
 *    list order — see `roles/README.md` Architecture Notes) is caught by this live subscription even
 *    if it races ahead of step 2's snapshot read. [grantDefaultRoles] is idempotent, so
 *    double-granting in the overlap window between steps 1 and 2 is harmless.
 * 2. Runs the one-time [backfillDefaultRoles] migration (issue point 6), gated by [VersionsRepo] so
 *    it executes exactly once across the app's lifetime, independent of restarts.
 *
 * Because the migration in step 2 runs against *all* currently-existing users and `root` (created by
 * `auth/server`'s bootstrap) counts as one of them, `root` naturally receives both the SuperAdmin role
 * (point 5) and the User role (point 6) with no special-casing.
 */
object JVMPlugin : StartPlugin {
    /** Version-tracking key under `tables_versions`; not a real schema table name. */
    private const val userRoleBackfillTableName = "users_default_role_backfill"

    /** Target version for [backfillDefaultRoles] — bump only if the backfill rule itself changes and must re-run. */
    private const val userRoleBackfillVersion = 1

    override fun Module.setupDI(config: JsonObject) {
        with(dev.inmo.wishlist.features.roles.common.JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        dev.inmo.wishlist.features.roles.common.JVMPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)

        val usersRepo = koin.get<UsersRepo>()
        val rolesRepo = koin.get<RolesRepo>()
        val scope = koin.get<CoroutineScope>()
        val versionsRepo = koin.get<VersionsRepo<Database>>()

        usersRepo.newObjectsFlow.subscribeLoggingDropExceptions(scope) { user ->
            grantDefaultRoles(rolesRepo, user)
        }

        versionsRepo.setTableVersion(
            tableName = userRoleBackfillTableName,
            version = userRoleBackfillVersion,
            onUpdate = { _, _ ->
                backfillDefaultRoles(usersRepo, rolesRepo)
            }
        )
    }
}
