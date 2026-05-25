package project_group.project_name.features.common.server.echo

interface EchoFeature {
    suspend fun getEcho(): String
}
