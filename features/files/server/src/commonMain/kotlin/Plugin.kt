package dev.inmo.wishlist.features.files.server

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.ktor.server.TemporalFilesRoutingConfigurator
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.files.common.Constants
import dev.inmo.wishlist.features.files.server.configurators.FilesRoutingsConfigurator
import dev.inmo.wishlist.features.files.server.services.FilesService
import dev.inmo.wishlist.features.files.server.services.TimedTemporalFilesUtilizer
import dev.inmo.wishlist.features.roles.common.FeatureRolesRegistry
import dev.inmo.wishlist.features.roles.common.SuperAdminRole
import dev.inmo.wishlist.features.roles.common.singleRequirement
import dev.inmo.wishlist.features.simpleRoles.server.SimpleRolesFeature
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Platform-agnostic startup plugin for the files server module.
 *
 * Registers in Koin:
 * - the shared MicroUtils [TemporalFilesRoutingConfigurator] as both a singleton (so [FilesService]
 *   can consume pending temp files) and an [ApplicationRoutingConfigurator.Element] (so it installs
 *   the single `POST /temp_upload` endpoint reusable by any feature)
 * - [FilesService] — promotes temporal uploads into permanent storage
 * - [FilesRoutingsConfigurator] as [ApplicationRoutingConfigurator.Element] — `/files` routes
 *
 * The binary [dev.inmo.wishlist.features.files.common.repo.FilesRepo] and
 * [dev.inmo.wishlist.features.files.common.repo.FilesMetaInfoRepo] bindings are added by [JVMPlugin].
 */
object Plugin : StartPlugin {
    /** Lifetime of an unfinalized temporal upload before it is purged from disk: one hour. */
    private const val temporalFileTtlMillis = 60L * 60L * 1000L

    override fun Module.setupDI(config: JsonObject) {
        single {
            TemporalFilesRoutingConfigurator(
                temporalFilesUtilizer = TimedTemporalFilesUtilizer(
                    scope = get(),
                    ttlMillis = temporalFileTtlMillis
                )
            )
        }
        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            get<TemporalFilesRoutingConfigurator>()
        }

        single { FilesService(get(), get(), get(), get()) }
        singleRequirement {
            FeatureRolesRegistry.Requirement(Constants.avatarChangeForOthersFunctionalityId, SuperAdminRole)
        }
        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            FilesRoutingsConfigurator(get(), get<SimpleRolesFeature>())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
