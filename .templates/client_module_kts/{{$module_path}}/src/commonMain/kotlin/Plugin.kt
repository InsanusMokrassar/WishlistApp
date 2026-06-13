package dev.inmo.wishlist.{{$module_package}}

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin
import org.koin.core.module.Module
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.{{$module_package}}.ui.{{$module_ui_name}}Model
import dev.inmo.wishlist.{{$module_package}}.ui.{{$module_ui_name}}ViewConfig
import dev.inmo.wishlist.{{$module_package}}.ui.{{$module_ui_name}}ViewModel

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        singleWithRandomQualifier {
            SerializersModule {
                polymorphic(Any::class, {{$module_ui_name}}ViewConfig::class, {{$module_ui_name}}ViewConfig.serializer())
                polymorphic(ViewConfig::class, {{$module_ui_name}}ViewConfig::class, {{$module_ui_name}}ViewConfig.serializer())
            }
        }
        factory { {{$module_ui_name}}ViewModel(it.get(), get(), get()) }
        single<{{$module_ui_name}}Model> {
            object : {{$module_ui_name}}Model {

            }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
