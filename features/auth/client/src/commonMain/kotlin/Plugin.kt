package dev.inmo.wishlist.features.auth.client

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import dev.inmo.wishlist.features.auth.client.configurators.BearerAuthHttpClientConfigurator
import dev.inmo.wishlist.features.auth.client.configurators.DefaultUrlHttpClientConfigurator
import dev.inmo.wishlist.features.auth.common.AuthFeature
import dev.inmo.wishlist.features.common.client.configurators.HttpClientConfigurator
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import kotlinx.coroutines.flow.asStateFlow

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        singleWithRandomQualifier<HttpClientConfigurator> {
            DefaultUrlHttpClientConfigurator(get())
        }

        singleWithRandomQualifier<HttpClientConfigurator> {
            BearerAuthHttpClientConfigurator(get())
        }

        single { KtorAuthFeature(get()) }
        single { AuthFeatureService(get(), get<KtorAuthFeature>()) }
        single<ClientAuthFeature> { get<AuthFeatureService>() }
        single<AuthFeature> { get<AuthFeatureService>() }

        single(qualifier = secretMeMutablemeStateFlowQualifier) { MutableRedeliverStateFlow<RegisteredUser?>(null) }
        single<StateFlow<RegisteredUser?>>(qualifier = meQualifier) {
            get<MutableRedeliverStateFlow<RegisteredUser?>>(qualifier = secretMeMutablemeStateFlowQualifier).asStateFlow()
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)

        val storage = koin.get<AuthCredentialsStorage>()
        val feature = koin.get<ClientAuthFeature>()
        val mutableMe = koin.secretMeMutableStateFlow
        storage.userAuthorised.subscribeLoggingDropExceptions(koin.get<CoroutineScope>()) { authorised ->
            mutableMe.value = runCatchingLogging {
                feature.getMe()
            }.getOrElse {
                null
            }
        }
    }
}
