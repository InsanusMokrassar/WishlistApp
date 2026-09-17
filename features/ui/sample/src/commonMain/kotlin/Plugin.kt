package dev.inmo.wishlist.features.ui.sample

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin
import org.koin.core.module.Module
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.sample.ui.DefaultSampleModel
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
            DefaultSampleModel(
                feature = get(),
                echoFeature = get(),
            )
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
