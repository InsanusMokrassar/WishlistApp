package dev.inmo.wishlist.features.files.common.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Type-safe identifier of a persisted file.
 *
 * Backed by an opaque [String] (a UUID generated server-side at finalize time). Used as the
 * primary key in both the binary store ([dev.inmo.wishlist.features.files.common.repo.FilesRepo])
 * and the metadata store ([dev.inmo.wishlist.features.files.common.repo.FilesMetaInfoRepo]),
 * and as the value referenced by wishlist items that own images.
 *
 * @property string Raw identifier value.
 */
@Serializable
@JvmInline
value class FileId(val string: String)
