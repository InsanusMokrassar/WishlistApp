package dev.inmo.wishlist.features.roles.common

import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.core.module.Module

/**
 * Common startup plugin for the `roles` feature.
 *
 * Registers the [FeatureRolesRegistry] realization — [MapFeatureRolesRegistry], built from every
 * contributed [FeatureRolesRegistry.Requirement] via `getAllDistinct` — and the polymorphic-to-`Any`
 * serializer for [FeatureRolesRegistry.Requirement]. The requirements themselves are NOT declared
 * here: each is contributed from the feature that owns the gated functionality (see `roles/README.md`
 * Architecture Notes and `agents/ARCHITECTURE.md` "Role requirement placement"). The Exposed/cache
 * [dev.inmo.kroles.repos.RolesRepo] wiring lives in [JVMPlugin] (JVM-only, Exposed/JDBC-backed).
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single<FeatureRolesRegistry> { MapFeatureRolesRegistry(getAllDistinct()) }

        singleWithRandomQualifier {
            SerializersModule {
                polymorphic(
                    Any::class,
                    FeatureRolesRegistry.Requirement::class,
                    FeatureRolesRegistry.Requirement.serializer()
                )
            }
        }
    }
}
