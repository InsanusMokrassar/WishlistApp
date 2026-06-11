package dev.inmo.wishlist.features.auth.client

import dev.inmo.kslog.common.e
import dev.inmo.kslog.common.logger
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge

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

        singleSecretMeMutableStateFlow { MutableRedeliverStateFlow<RegisteredUser?>(null) }
        singleMeStateFlow {
            secretMeMutableStateFlow.asStateFlow()
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)

        val storage = koin.get<AuthCredentialsStorage>()
        val feature = koin.get<ClientAuthFeature>()
        val mutableMe = koin.secretMeMutableStateFlow
        val scope = koin.get<CoroutineScope>()
        merge(flowOf(Unit), storage.userAuthorised).subscribeLoggingDropExceptions(scope) { authorised ->
            mutableMe.value = runCatchingLogging {
                feature.getMe()
            }.getOrElse {
                logger.e("Unable to get me", it)
                null
            }
        }
    }
}
