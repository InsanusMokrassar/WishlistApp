package dev.inmo.wishlist.features.ui.users

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.admin.client.AdminFeature
import dev.inmo.wishlist.features.auth.client.PasswordChangeFeature
import dev.inmo.wishlist.features.auth.client.meStateFlow
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.files.client.FilesClientService
import dev.inmo.wishlist.features.users.client.UsersFeature
import dev.inmo.wishlist.features.ui.users.ui.DefaultUsersModel
import dev.inmo.wishlist.features.ui.users.ui.PasswordChangeViewConfig
import dev.inmo.wishlist.features.ui.users.ui.PasswordChangeViewModel
import dev.inmo.wishlist.features.ui.users.ui.UserEditViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UserEditViewModel
import dev.inmo.wishlist.features.ui.users.ui.UserViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UserViewModel
import dev.inmo.wishlist.features.ui.users.ui.UsersListViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UsersListViewModel
import dev.inmo.wishlist.features.ui.users.ui.UsersModel
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Platform-agnostic startup plugin for the users UI feature.
 *
 * Registers polymorphic serializers and ViewModel factories for the users list, the public
 * profile view and the profile edit screens, plus the single [UsersModel] singleton wrapping the
 * public [UsersFeature], the authenticated-user ("me") state flow, admin [AdminFeature] and
 * [FilesClientService].
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        singleWithRandomQualifier {
            SerializersModule {
                polymorphic(Any::class, UsersListViewConfig::class, UsersListViewConfig.serializer())
                polymorphic(ViewConfig::class, UsersListViewConfig::class, UsersListViewConfig.serializer())
                polymorphic(Any::class, UserViewConfig::class, UserViewConfig.serializer())
                polymorphic(ViewConfig::class, UserViewConfig::class, UserViewConfig.serializer())
                polymorphic(Any::class, UserEditViewConfig::class, UserEditViewConfig.serializer())
                polymorphic(ViewConfig::class, UserEditViewConfig::class, UserEditViewConfig.serializer())
                polymorphic(Any::class, PasswordChangeViewConfig.Pending::class, PasswordChangeViewConfig.Pending.serializer())
                polymorphic(Any::class, PasswordChangeViewConfig.Completed::class, PasswordChangeViewConfig.Completed.serializer())
                polymorphic(ViewConfig::class, PasswordChangeViewConfig.Pending::class, PasswordChangeViewConfig.Pending.serializer())
                polymorphic(ViewConfig::class, PasswordChangeViewConfig.Completed::class, PasswordChangeViewConfig.Completed.serializer())
            }
        }
        factory { UsersListViewModel(node = it.get(), model = get(), interactor = get()) }
        factory { UserViewModel(node = it.get(), model = get(), interactor = get()) }
        factory { UserEditViewModel(node = it.get(), model = get(), interactor = get()) }
        factory { PasswordChangeViewModel(node = it.get(), model = get(), interactor = get()) }
        single<UsersModel> {
            DefaultUsersModel(
                feature = get(),
                authFeature = get(),
                emailFeature = get(),
                passwordChangeFeature = get(),
                meState = meStateFlow,
                adminFeature = get(),
                filesService = get(),
                scope = get(),
                credentialsStorage = get(),
                rolesFeature = get(),
            )
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
