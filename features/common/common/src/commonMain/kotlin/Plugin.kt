package project_group.project_name.features.common.common

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.e
import dev.inmo.micro_utils.coroutines.LinkedSupervisorScope
import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import io.ktor.http.ContentType
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.module.Module
import org.koin.dsl.binds

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
//        single { SerializersModuleConfigurator(getAllDistinct()) }
        single<ContentType> { ContentType.Application.Json }
        single {
            CoroutineScope(
                Dispatchers.Default
            ).LinkedSupervisorScope(
                CoroutineExceptionHandler { context, throwable ->
                    KSLog.e("Default CoroutineExceptionHandler", "Unhandled exception in context $context", throwable)
                }
            )
        }

        single {
            Json {
                ignoreUnknownKeys = true
                useArrayPolymorphism = true
                serializersModule = SerializersModule {
                    getAllDistinct<SerializersModule>().forEach {
                        include(it)
                    }
                }
                allowStructuredMapKeys = true
            }
        } binds arrayOf(
            StringFormat::class,
            SerialFormat::class
        )

    }
}
