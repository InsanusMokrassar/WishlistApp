package dev.inmo.wishlist.features.ui.sample

import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import korlibs.time.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin
import org.koin.core.module.Module
import dev.inmo.wishlist.features.common.client.echo.EchoFeature
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.sample.client.SampleFeature
import dev.inmo.wishlist.features.ui.sample.ui.SampleModel
import dev.inmo.wishlist.features.ui.sample.ui.SampleViewConfig
import dev.inmo.wishlist.features.ui.sample.ui.SampleViewModel

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        singleWithRandomQualifier {
            SerializersModule {
                polymorphic(Any::class, SampleViewConfig::class, SampleViewConfig.serializer())
                polymorphic(ViewConfig::class, SampleViewConfig::class, SampleViewConfig.serializer())
            }
        }
        factory { SampleViewModel(it.get(), get()) }
        single<SampleModel> {
            val feature = get<SampleFeature>()
            val echoFeature = get<EchoFeature>()
            object : SampleModel {
                override suspend fun getSampleText(): String {
                    return feature.getSampleText()
                }

                override fun serverStatusFlow(): Flow<String?> = flow {
                    while (true) {
                        val result = runCatchingLogging {
                            echoFeature.getEcho()
                        }

                        emit(result.getOrNull())
                        delay(1.seconds)
                    }
                }
            }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}