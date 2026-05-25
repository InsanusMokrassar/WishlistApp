package project_group.project_name.features.auth.server

import dev.inmo.kslog.common.i
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.repos.create
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import project_group.project_name.features.auth.common.models.Password
import project_group.project_name.features.auth.server.repo.ExposedPasswordsRepo
import project_group.project_name.features.auth.server.repo.PasswordsRepo
import project_group.project_name.features.auth.server.services.AuthFeatureService
import project_group.project_name.features.users.common.models.NewUser
import project_group.project_name.features.users.common.models.Username
import project_group.project_name.features.users.common.repo.UsersRepo
import java.security.SecureRandom

object JVMPlugin : StartPlugin {
    private const val rootUsername = "root"
    private const val generatedPasswordLength = 24
    private const val passwordAlphabet =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

    override fun Module.setupDI(config: JsonObject) {
        with(project_group.project_name.features.auth.common.JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }

        single<PasswordsRepo> { ExposedPasswordsRepo(get()) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        project_group.project_name.features.auth.common.JVMPlugin.startPlugin(koin)
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