package project_group.project_name.features.ui.sample.ui

import kotlinx.coroutines.flow.Flow

interface SampleModel {
    suspend fun getSampleText(): String
    fun serverStatusFlow(): Flow<String?>
}