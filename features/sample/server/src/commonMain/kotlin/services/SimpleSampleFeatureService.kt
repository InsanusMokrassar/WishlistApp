package project_group.project_name.features.sample.server.services

import project_group.project_name.features.sample.server.SampleFeature

class SimpleSampleFeatureService(
    private val sampleText: String
) : SampleFeature {
    override suspend fun getSampleText(): String {
        return sampleText
    }
}