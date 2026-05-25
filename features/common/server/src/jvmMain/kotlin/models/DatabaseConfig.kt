package project_group.project_name.features.common.server.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.exposed.v1.jdbc.Database
import org.postgresql.Driver

const val defaultDatabaseParamsName = "defaultDatabase"
inline val Map<String, Any>.database: Database?
    get() = (get(defaultDatabaseParamsName) as? DatabaseConfig) ?.database

@Serializable
data class DatabaseConfig(
    val url: String = "jdbc:postgresql://localhost:5432/tablet",
    val driver: String = Driver::class.qualifiedName!!,
    val username: String = "",
    val password: String = ""
) {
    @Transient
    val database: Database = Database.connect(
        url,
        driver,
        username,
        password
    )
}
