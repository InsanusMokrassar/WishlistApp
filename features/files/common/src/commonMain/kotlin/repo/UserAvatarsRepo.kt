package dev.inmo.wishlist.features.files.common.repo

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.users.common.models.UserId

/**
 * Association store mapping a [UserId] to the [FileId] of that user's avatar image.
 *
 * Kept separate from [FilesMetaInfoRepo] (which describes any uploaded file) so a user's current
 * avatar can be resolved without scanning file metadata. A user has at most one avatar: setting a
 * new one overwrites the previous mapping. The referenced payload and metadata live in
 * [FilesRepo] / [FilesMetaInfoRepo] like any other finalized file.
 *
 * Implemented on the server by an Exposed-backed [KeyValueRepo] (see `ExposedUserAvatarsRepo`).
 */
interface UserAvatarsRepo : KeyValueRepo<UserId, FileId>
