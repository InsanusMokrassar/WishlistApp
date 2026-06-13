package dev.inmo.wishlist.features.files.common.repo

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.exposed.keyvalue.ExposedKeyValueRepo
import dev.inmo.micro_utils.repos.mappers.withMapper
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.users.common.models.UserId
import org.jetbrains.exposed.v1.jdbc.Database

/**
 * Builds the mapped [KeyValueRepo] backing [ExposedUserAvatarsRepo].
 *
 * Stores one row per user in the `user_avatars` table (`user_id` long primary key, `file_id` text),
 * reusing the `ExposedKeyValueRepo` + `withMapper` pattern from `ExposedFilesMetaInfoRepo`.
 *
 * @param database Exposed [Database] injected from Koin.
 */
private fun createDelegate(database: Database): KeyValueRepo<UserId, FileId> =
    ExposedKeyValueRepo<Long, String>(
        database = database,
        keyColumnAllocator = { long("user_id") },
        valueColumnAllocator = { text("file_id") },
        tableName = "user_avatars"
    ).withMapper<UserId, FileId, Long, String>(
        keyFromToTo = { long },
        valueFromToTo = { string },
        keyToToFrom = { UserId(this) },
        valueToToFrom = { FileId(this) }
    )

/**
 * Exposed JDBC implementation of [UserAvatarsRepo] backed by the `user_avatars` table.
 *
 * @param database Exposed [Database] injected from Koin.
 */
class ExposedUserAvatarsRepo(
    database: Database
) : UserAvatarsRepo, KeyValueRepo<UserId, FileId> by createDelegate(database)
