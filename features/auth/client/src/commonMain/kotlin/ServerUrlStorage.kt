package project_group.project_name.features.auth.client

interface ServerUrlStorage {
    suspend fun getServerUrl(): String?
    suspend fun saveServerUrl(url: String?)
}
