package dev.inmo.wishlist.features.ui.users

import dev.inmo.micro_utils.common.MPPFile
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.admin.client.AdminFeature
import dev.inmo.wishlist.features.auth.client.meStateFlow
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.files.client.FilesClientService
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.users.client.UsersFeature
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.ui.users.ui.UserEditViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UserEditViewModel
import dev.inmo.wishlist.features.ui.users.ui.UserViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UserViewModel
import dev.inmo.wishlist.features.ui.users.ui.UsersListViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UsersListViewModel
import dev.inmo.wishlist.features.ui.users.ui.UsersModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
            }
        }
        factory { UsersListViewModel(node = it.get(), model = get(), interactor = get()) }
        factory { UserViewModel(node = it.get(), model = get(), interactor = get()) }
        factory { UserEditViewModel(node = it.get(), model = get(), interactor = get(), authCredentialsStorage = get()) }
        single<UsersModel> {
            val feature = get<UsersFeature>()
            val meState = meStateFlow
            val adminFeature = get<AdminFeature>()
            val filesService = get<FilesClientService>()
            val scope = get<CoroutineScope>()
            object : UsersModel {
                override suspend fun getAllUsers(): List<RegisteredUser> = feature.getAll()

                override suspend fun getUser(id: UserId): RegisteredUser? =
                    feature.getAll().find { it.id == id }

                override val currentUserIdFlow: StateFlow<UserId?> =
                    meState.map { it?.id }.stateIn(scope, SharingStarted.Eagerly, meState.value?.id)

                override val isCurrentUserRootFlow: StateFlow<Boolean> =
                    meState.map { it?.username?.string == "root" }
                        .stateIn(scope, SharingStarted.Eagerly, meState.value?.username?.string == "root")

                override suspend fun updateUsername(id: UserId, username: Username): Boolean =
                    adminFeature.usersManagement.update(id, NewUser(username))

                override suspend fun setPassword(id: UserId, password: Password): Boolean =
                    adminFeature.usersManagement.setPassword(id, password)

                override suspend fun deleteUser(id: UserId): Boolean =
                    adminFeature.usersManagement.delete(id)

                override suspend fun getAvatar(userId: UserId): FileId? =
                    filesService.getAvatar(userId)

                override suspend fun uploadAvatar(userId: UserId, file: MPPFile): FileId? =
                    filesService.uploadAvatar(userId, file)

                override fun imageUrl(id: FileId): String = filesService.apiFileUrl(id)

                override suspend fun loadImageBytes(id: FileId): ByteArray? =
                    filesService.downloadBytes(id)
            }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
