package dev.inmo.wishlist.features.ui.auth

import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin
import org.koin.core.module.Module
import dev.inmo.wishlist.features.auth.client.AuthCredentialsStorage
import dev.inmo.wishlist.features.auth.client.ClientAuthFeature
import dev.inmo.wishlist.features.auth.common.AuthFeature
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.auth.ui.AuthModel
import dev.inmo.wishlist.features.ui.auth.ui.AuthViewConfig
import dev.inmo.wishlist.features.ui.auth.ui.AuthViewInteractor
import dev.inmo.wishlist.features.ui.auth.ui.AuthViewModel
import dev.inmo.wishlist.features.auth.client.ServerUrlStorage
import dev.inmo.wishlist.features.users.common.models.Username

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        singleWithRandomQualifier {
            SerializersModule {
                polymorphic(Any::class, AuthViewConfig::class, AuthViewConfig.serializer())
                polymorphic(ViewConfig::class, AuthViewConfig::class, AuthViewConfig.serializer())
            }
        }
        factory { AuthViewModel(node = it.get(), model = get(), interactor = get()) }
        single<AuthModel> {
            val authFeature = get<ClientAuthFeature>()
            val credentialsStorage = get<AuthCredentialsStorage>()
            val urlStorage = get<ServerUrlStorage>()
            object : AuthModel {
                override suspend fun isAlreadyLoggedIn(): Boolean {
                    if (credentialsStorage.userAuthorised.value) {
                        val me = authFeature.getMe()
                        return me != null
                    }
                    return false
                }

                override suspend fun getServerAddress(): String? =
                    urlStorage.getServerUrl()

                override suspend fun saveServerAddress(address: String?) {
                    urlStorage.saveServerUrl(address?.takeIf { it.isNotBlank() })
                }

                override suspend fun login(username: Username, password: Password): Boolean =
                    authFeature.login(username, password) != null
            }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)

        val interactor = koin.get<AuthViewInteractor>()
        val storage = koin.get<AuthCredentialsStorage>()
        val scope = koin.get<CoroutineScope>()
        storage.userAuthorised.filter { it == false }.subscribeLoggingDropExceptions(scope) {
            interactor.onUserLoggedOut()
        }
    }
}