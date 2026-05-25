package dev.inmo.wishlist.features.sample.server

interface SampleFeature {
    suspend fun getSampleText(): String
}