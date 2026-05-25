package project_group.project_name.features.sample.client

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import project_group.project_name.features.sample.common.Constants

class KtorSampleFeature(
    private val client: HttpClient
) : SampleFeature {
    private val getSampleTextFullPath = "${Constants.prefixPathPart}/${Constants.getTextPathPart}"
    override suspend fun getSampleText(): String {
        return client.get(getSampleTextFullPath).bodyAsText()
    }
}