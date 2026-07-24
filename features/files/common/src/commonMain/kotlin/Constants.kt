package dev.inmo.wishlist.features.files.common

import dev.inmo.micro_utils.ktor.common.DefaultTemporalFilesSubPath

/**
 * URL path segment constants for the files feature, shared between the server routing
 * configurator and the client HTTP implementation to avoid out-of-sync strings.
 */
object Constants {
    /** Root path segment for all files routes: `/files/...`. */
    const val filesPrefixPathPart = "files"

    /** Path segment for the finalize route: `.../finalize`. Promotes a temporal upload to permanent storage. */
    const val finalizePathPart = "finalize"

    /** Path segment for the metadata route: `.../meta/{id}`. Returns [dev.inmo.wishlist.features.files.common.models.FilesFeatureMetaInfo]. */
    const val metaPathPart = "meta"

    /**
     * Path segment for the user-avatar routes: `.../avatar/{userId}`.
     * `GET` returns the user's avatar [dev.inmo.wishlist.features.files.common.models.FileId] (public);
     * `PUT` associates an already-finalized file as the user's avatar (owner or root only).
     */
    const val avatarPathPart = "avatar"

    /**
     * Full path of the shared temporal upload endpoint (`/temp_upload`), provided by MicroUtils
     * `TemporalFilesRoutingConfigurator`. Clients upload raw bytes here and receive a
     * [dev.inmo.micro_utils.ktor.common.TemporalFileId].
     */
    const val temporalUploadPathPart = DefaultTemporalFilesSubPath
}
