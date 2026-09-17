package dev.inmo.wishlist.features.ui.sample.ui

import dev.inmo.kslog.common.KSLog
import dev.inmo.wishlist.features.common.client.echo.EchoFeature
import dev.inmo.wishlist.features.sample.client.SampleFeature
import dev.inmo.wishlist.features.ui.sample.Plugin
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

/** Verifies sample delegation, status polling, and the production singleton binding. */
class SampleModelTest {
    @Test
    fun delegatesSampleText() = runTest {
        val feature = RecordingSampleFeature()
        val model = DefaultSampleModel(feature = feature, echoFeature = SequenceEchoFeature())

        assertEquals("sample", model.getSampleText())
        assertEquals(1, feature.calls)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun statusFlowEmitsSuccessThenNullAfterOneSecondAndContinuesPolling() = runTest {
        val originalLogger = KSLog.default
        KSLog.default = KSLog { _, _, _, _ -> }
        try {
            val echo = SequenceEchoFeature()
            val model = DefaultSampleModel(feature = RecordingSampleFeature(), echoFeature = echo)
            val values = async { model.serverStatusFlow().take(2).toList() }

            runCurrent()
            assertEquals(1, echo.calls)
            advanceTimeBy(1_000L)
            runCurrent()

            assertEquals(listOf("online", null), values.await())
            assertEquals(2, echo.calls)
        } finally {
            KSLog.default = originalLogger
        }
    }

    @Test
    fun pluginBindsDefaultModelAsOneInterfaceSingleton() {
        val koin = startKoin {
            modules(
                module {
                    with(Plugin) { setupDI(JsonObject(emptyMap())) }
                    single<SampleFeature> { RecordingSampleFeature() }
                    single<EchoFeature> { SequenceEchoFeature() }
                }
            )
        }
        try {
            val first = koin.koin.get<SampleModel>()
            val second = koin.koin.get<SampleModel>()

            assertIs<DefaultSampleModel>(first)
            assertSame(first, second)
        } finally {
            stopKoin()
        }
    }

    private class RecordingSampleFeature : SampleFeature {
        var calls = 0
        override suspend fun getSampleText(): String {
            calls += 1
            return "sample"
        }
    }

    private class SequenceEchoFeature : EchoFeature {
        var calls = 0
        override suspend fun getEcho(): String {
            calls += 1
            if (calls == 2) error("offline")
            return "online"
        }
    }
}
