package dev.inmo.wishlist.features.files.server.models

import kotlinx.serialization.Serializable

/**
 * Files-feature slice of the server config JSON. Decoded from the same root config object that
 * `Config`/`KtorConfig` are decoded from (see `features/common/server` JVMPlugin), so no change to
 * the shared `Config` type is required to add a feature-local setting.
 *
 * @property filesFolder Directory on the server where finalized file payloads are stored.
 */
@Serializable
data class FilesConfig(
    val filesFolder: String = "./uploaded_files"
)
