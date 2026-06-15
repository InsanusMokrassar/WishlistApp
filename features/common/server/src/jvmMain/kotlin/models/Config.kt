package dev.inmo.wishlist.features.common.server.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Default URL sub-path prefix under which the web client (single page application) is served.
 *
 * Used both as the default key for [Config.staticFolders] (when only a single [Config.staticFolder]
 * is provided) and as the target of the root path redirect configured in the server routing.
 */
const val defaultWebClientSubPath = "ui"

/**
 * Top-level server configuration decoded from the launcher JSON config.
 *
 * @property host Ktor bind address.
 * @property port Ktor bind port.
 * @property databaseConfig PostgreSQL/Exposed connection settings.
 * @property wss Whether public WebSocket links should use the `wss://` scheme.
 * @property publicHost Public hostname used for link generation; defaults to [host].
 * @property staticFolder Shortcut for configuring a single static content directory; when set and
 * [staticFolders] is not explicitly provided, the directory is mounted under [defaultWebClientSubPath].
 * @property staticFolders Map of URL path prefix to local directory served as static content. When
 * absent, falls back to mounting [staticFolder] under [defaultWebClientSubPath] (or an empty map).
 */
@Serializable
data class Config(
    val host: String = "0.0.0.0",
    val port: Int = 8082,
    @SerialName("database")
    val databaseConfig: DatabaseConfig = DatabaseConfig(),
    val wss: Boolean = false,
    val publicHost: String = host,
    val staticFolder: String? = null,
    val staticFolders: Map<String, String> = staticFolder ?.let { mapOf(defaultWebClientSubPath to it) } ?: emptyMap(),
)
