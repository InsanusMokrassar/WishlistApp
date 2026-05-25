package project_group.project_name.features.auth.server.repo

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.exposed.keyvalue.ExposedKeyValueRepo
import dev.inmo.micro_utils.repos.mappers.withMapper
import org.jetbrains.exposed.v1.jdbc.Database
import project_group.project_name.features.auth.common.models.Password
import project_group.project_name.features.users.common.models.UserId
import project_group.project_name.features.users.common.models.Username

private fun createDelegate(database: Database): KeyValueRepo<UserId, Password> =
    ExposedKeyValueRepo<Long, String>(
        database = database,
        keyColumnAllocator = { long("user_id") },
        valueColumnAllocator = { text("password_hash") },
        tableName = "users_passwords"
    ).withMapper<UserId, Password, Long, String>(
        keyFromToTo = { long },
        valueFromToTo = { string },
        keyToToFrom = { UserId(this) },
        valueToToFrom = { Password(this) }
    )

class ExposedPasswordsRepo(
    database: Database
) : PasswordsRepo, KeyValueRepo<UserId, Password> by createDelegate(database)
