package project_group.project_name.features.common.server.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val host: String = "0.0.0.0",
    val port: Int = 8082,
    @SerialName("database")
    val databaseConfig: DatabaseConfig = DatabaseConfig(),
    val wss: Boolean = false,
    val publicHost: String = host,
    val staticFolder: String? = null,
    val staticFolders: Map<String, String> = staticFolder ?.let { mapOf("/" to it) } ?: emptyMap(),
)
