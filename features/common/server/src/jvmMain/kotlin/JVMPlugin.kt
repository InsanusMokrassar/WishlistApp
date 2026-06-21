package dev.inmo.wishlist.features.common.server

import dev.inmo.wishlist.features.common.server.configurators.GZipConfigurator
import dev.inmo.wishlist.features.common.server.configurators.WebSocketsConfiguration
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.d
import dev.inmo.kslog.common.e
import dev.inmo.kslog.common.i
import dev.inmo.kslog.common.logger
import dev.inmo.kslog.common.w
import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationCachingHeadersConfigurator
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationSessionsConfigurator
import dev.inmo.micro_utils.ktor.server.configurators.KtorApplicationConfigurator
import dev.inmo.micro_utils.ktor.server.configurators.StatusPagesConfigurator
import dev.inmo.micro_utils.repos.exposed.keyvalue.ExposedKeyValueRepo
import dev.inmo.micro_utils.repos.versions.KeyValueBasedVersionsRepoProxy
import dev.inmo.micro_utils.repos.versions.StandardVersionsRepo
import dev.inmo.micro_utils.repos.versions.VersionsRepo
import dev.inmo.wishlist.features.common.common.JVMPlugin
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import io.ktor.server.application.install
import io.ktor.server.application.pluginOrNull
import io.ktor.server.application.serverConfig
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.engine.ApplicationEnvironmentBuilder
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.routing.RoutingNode
import io.ktor.server.routing.RoutingRoot
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.job
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.core.qualifier.StringQualifier
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import dev.inmo.wishlist.features.common.server.configurators.ApplicationAuthenticationConfigurator
import dev.inmo.wishlist.features.common.server.configurators.ContentNegotiationKtorApplicationConfigurator
import dev.inmo.wishlist.features.common.server.configurators.InternalApplicationRoutingConfigurator
import dev.inmo.wishlist.features.common.server.models.Config
import dev.inmo.wishlist.features.common.server.models.KtorConfig
import io.ktor.http.HttpStatusCode
import java.io.File

object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }
        with(JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }
        val configJsonQualifier = StringQualifier("ConfigJsonQualifier")
        val ktorConfigJsonQualifier = StringQualifier("KtorConfigJsonQualifier")

        single(configJsonQualifier) {
            get<Json>().decodeFromJsonElement(
                Config.serializer(),
                config
            )
        }
        single(ktorConfigJsonQualifier) {
            get<Json>().decodeFromJsonElement(
                KtorConfig.serializer(),
                config
            )
        }
        single {
            get<Config>(configJsonQualifier).databaseConfig.database
        }
        single<VersionsRepo<Database>> {
            StandardVersionsRepo<Database>(
                KeyValueBasedVersionsRepoProxy(
                    ExposedKeyValueRepo<String, Int>(
                        get(),
                        { text("table_name") },
                        { integer("version") },
                        "tables_versions"
                    ),
                    get()
                )
            )
        }
        single { InternalApplicationRoutingConfigurator(getAllDistinct()) }
        singleWithRandomQualifier<KtorApplicationConfigurator> { StatusPagesConfigurator(getAllDistinct()) }
        singleWithRandomQualifier<KtorApplicationConfigurator> { ApplicationCachingHeadersConfigurator(getAllDistinct()) }
        singleWithRandomQualifier<KtorApplicationConfigurator> { ApplicationSessionsConfigurator(getAllDistinct()) }
        singleWithRandomQualifier<KtorApplicationConfigurator> { ApplicationAuthenticationConfigurator(getAllDistinct()) }
        singleWithRandomQualifier<KtorApplicationConfigurator> { ContentNegotiationKtorApplicationConfigurator(get()) }
        singleWithRandomQualifier<KtorApplicationConfigurator> { GZipConfigurator() }
        single<EmbeddedServer<*, *>> {

            val serverConfig = serverConfig(
                ApplicationEnvironmentBuilder().apply {
                    this.log = LoggerFactory.getLogger("Ktor")
                    this.config = MapApplicationConfig().apply {
                        if (isInDebugMode) {
                            put("io.ktor.development", true.toString()) // enable development mode
                        }
                    }
                }.build()
            ) {
                @OptIn(DelicateCoroutinesApi::class)
                this.parentCoroutineContext = newFixedThreadPoolContext(32, "Ktor")
                module(
                    body = {
                        with(WebSocketsConfiguration(get())) { // Should be configured before everything else
                            configure()
                        }
                        getAllDistinct<KtorApplicationConfigurator>().forEach {
                            with(it) {
                                configure()
                            }
                        }
                        getOrNull<InternalApplicationRoutingConfigurator>() ?.let {
                            with(it) {
                                configure()
                            }
                        }
                        fun RoutingNode.print() {
                            KSLog.d(this)
                            children.forEach { it.print() }
                        }
                        pluginOrNull(RoutingRoot) ?.print()

                        install(CallLogging) {
                            this.level = if (isInDebugMode) {
                                Level.TRACE
                            } else {
                                Level.WARN
                            }
                            this.logger = LoggerFactory.getLogger("Ktor")
                        }
                    }
                )
            }
            val hostFromConfig = get<KtorConfig>(ktorConfigJsonQualifier).host
            val portFromConfig = get<KtorConfig>(ktorConfigJsonQualifier).port
            val severEngineFactory = getOrNull<ApplicationEngineFactory<ApplicationEngine, out ApplicationEngine.Configuration>>() ?: Netty
            embeddedServer(severEngineFactory, serverConfig, ) {
                connector {
                    this.host = hostFromConfig
                    this.port = portFromConfig
                }
            }
        }

        singleWithRandomQualifier<KtorApplicationConfigurator> {
            val config = get<Config>(configJsonQualifier)
            ApplicationRoutingConfigurator(
                listOf(
                    ApplicationRoutingConfigurator.Element {
                        config.staticFolders.forEach { (path, folderPath) ->
                            val file = File(folderPath)
                            staticFiles(path, file) {
                                // SPA fallback: any sub-path under the mount (e.g. `/ui/users`) with no
                                // matching file serves the web client shell so client-side routing works.
                                default("index.html")
                            }
                        }
                    }
                )
            )
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        JVMPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)

        logger.d("Server")
        runCatchingLogging {
            koin.get<EmbeddedServer<*, *>>().apply {
                logger.i("server","Server up and running")
                start(true)
                application.coroutineContext.job.join()
            }
        }.onFailure {
            logger.e(it) { "Some error in server starting" }
        }
    }
}
