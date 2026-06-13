package dev.inmo.wishlist.features.auth.server

import dev.inmo.kslog.common.i
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.repos.create
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.server.repo.ExposedPasswordsRepo
import dev.inmo.wishlist.features.auth.server.repo.PasswordsRepo
import dev.inmo.wishlist.features.auth.server.services.AuthFeatureService
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.UsersRepo
import java.security.SecureRandom

object JVMPlugin : StartPlugin {
    private const val rootUsername = "root"
    private const val generatedPasswordLength = 24
    private const val passwordAlphabet =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

    override fun Module.setupDI(config: JsonObject) {
        with(dev.inmo.wishlist.features.auth.common.JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }

        single<PasswordsRepo> { ExposedPasswordsRepo(get()) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        dev.inmo.wishlist.features.auth.common.JVMPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)

        bootstrapRootUserIfMissing(koin)
    }

    private suspend fun bootstrapRootUserIfMissing(koin: Koin) {
        val usersRepo = koin.get<UsersRepo>()
        if (usersRepo.count() != 0L) return
        val authService = koin.get<AuthFeatureService>()
        val username = Username(rootUsername)
        val rawPassword = generateRandomPassword(generatedPasswordLength)
        val createdUser = usersRepo.create(NewUser(username)).first()
        authService.setPassword(createdUser.id, Password(rawPassword))
        logger.i(
            "Generated root user '$rootUsername' with password: $rawPassword (printed once; store it now)"
        )
    }

    private fun generateRandomPassword(length: Int): String {
        val random = SecureRandom()
        val charsetSize = passwordAlphabet.length
        return buildString(length) {
            repeat(length) {
                append(passwordAlphabet[random.nextInt(charsetSize)])
            }
        }
    }
}