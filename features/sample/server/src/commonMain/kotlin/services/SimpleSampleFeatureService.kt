package dev.inmo.wishlist.features.sample.server.services

import dev.inmo.wishlist.features.sample.server.SampleFeature

class SimpleSampleFeatureService(
    private val sampleText: String
) : SampleFeature {
    override suspend fun getSampleText(): String {
        return sampleText
    }
}