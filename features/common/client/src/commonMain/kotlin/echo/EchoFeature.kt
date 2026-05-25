package project_group.project_name.features.common.client.echo

interface EchoFeature {
    suspend fun getEcho(): String
}
