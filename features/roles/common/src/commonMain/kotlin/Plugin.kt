package dev.inmo.wishlist.features.roles.common

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.module.Module

/**
 * Common startup plugin for the `roles` feature.
 *
 * Registers no Koin dependencies of its own — the Exposed/cache [dev.inmo.kroles.repos.RolesRepo]
 * wiring lives in [dev.inmo.wishlist.features.roles.common.JVMPlugin] (JVM-only, since the backing
 * store is Exposed/JDBC). This object's only responsibility is populating [FeatureRolesRegistry] with
 * the feature/role requirements this app currently has (see `roles/README.md` Architecture Notes for
 * why registration is centralized here rather than self-registered by each gated feature).
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        FeatureRolesRegistry.register(RoleGatedFeatureIds.adminPanel, SuperAdminRole)
        FeatureRolesRegistry.register(RoleGatedFeatureIds.filesAvatarChangeForOthers, SuperAdminRole)
        FeatureRolesRegistry.register(RoleGatedFeatureIds.emailSendTest, SuperAdminRole)
    }
}