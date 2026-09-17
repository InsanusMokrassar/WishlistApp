package dev.inmo.wishlist.features.ui.sample.ui

import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.wishlist.features.common.client.echo.EchoFeature
import dev.inmo.wishlist.features.sample.client.SampleFeature
import korlibs.time.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Default sample model that combines sample text with periodic echo status polling.

 * @param feature Remote sample-text capability.
 * @param echoFeature Remote echo capability used for status polling.
 */
class DefaultSampleModel(
    private val feature: SampleFeature,
    private val echoFeature: EchoFeature,
) : SampleModel {
    /** @return Sample text supplied by the feature. */
    override suspend fun getSampleText(): String {
        return feature.getSampleText()
    }

    /** @return A polling flow that emits echo text or `null` after each failed request. */
    override fun serverStatusFlow(): Flow<String?> = flow {
        while (true) {
            val result = runCatchingLogging {
                echoFeature.getEcho()
            }

            emit(result.getOrNull())
            delay(1.seconds)
        }
    }
}
