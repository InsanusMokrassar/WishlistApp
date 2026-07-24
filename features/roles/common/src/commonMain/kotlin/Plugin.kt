package dev.inmo.wishlist.features.roles.common

import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.module.Module

/**
 * Common startup plugin for the `roles` feature.
 *
 * Registers the [FeatureRolesRegistry] realization — [MapFeatureRolesRegistry], built from every
 * contributed [FeatureRolesRegistry.Requirement] via `getAllDistinct` — and this app's current
 * requirements through [singleRequirement]. The Exposed/cache [dev.inmo.kroles.repos.RolesRepo] wiring
 * lives in [JVMPlugin] (JVM-only, since the backing store is Exposed/JDBC).
 *
 * Requirements are centralized here today (see `roles/README.md` Architecture Notes), but any feature
 * may contribute its own via [singleRequirement] from its own `setupDI` — [MapFeatureRolesRegistry]
 * aggregates them all regardless of which module declared them.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single<FeatureRolesRegistry> { MapFeatureRolesRegistry(getAllDistinct()) }

        singleRequirement {
            FeatureRolesRegistry.Requirement(RoleGatedFeatureIds.adminPanel, SuperAdminRole)
        }
        singleRequirement {
            FeatureRolesRegistry.Requirement(RoleGatedFeatureIds.filesAvatarChangeForOthers, SuperAdminRole)
        }
        singleRequirement {
            FeatureRolesRegistry.Requirement(RoleGatedFeatureIds.emailSendTest, SuperAdminRole)
        }
    }
}
