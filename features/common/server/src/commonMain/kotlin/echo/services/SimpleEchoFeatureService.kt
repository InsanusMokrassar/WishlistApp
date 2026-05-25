package project_group.project_name.features.common.server.echo.services

import project_group.project_name.features.common.server.echo.EchoFeature

class SimpleEchoFeatureService(
    private val responseText: String = "Echo from server: it works!"
) : EchoFeature {
    override suspend fun getEcho(): String = responseText
}
