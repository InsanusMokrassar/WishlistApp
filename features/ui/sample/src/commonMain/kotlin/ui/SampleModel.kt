package dev.inmo.wishlist.features.ui.sample.ui

import kotlinx.coroutines.flow.Flow

interface SampleModel {
    suspend fun getSampleText(): String
    fun serverStatusFlow(): Flow<String?>
}