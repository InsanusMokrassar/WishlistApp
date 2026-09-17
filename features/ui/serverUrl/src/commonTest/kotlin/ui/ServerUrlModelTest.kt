package dev.inmo.wishlist.features.ui.serverUrl.ui

import dev.inmo.wishlist.features.auth.client.ServerUrlStorage
import dev.inmo.wishlist.features.ui.serverUrl.Plugin
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

/** Verifies URL normalization and the production singleton binding. */
class ServerUrlModelTest {
    @Test
    fun readsAndSavesUrlsWithoutTrimmingNonblankValues() = runTest {
        val storage = RecordingServerUrlStorage("https://stored.example")
        val model = DefaultServerUrlModel(storage = storage)

        assertEquals("https://stored.example", model.getServerUrl())
        model.saveServerUrl(null)
        model.saveServerUrl("")
        model.saveServerUrl(" \t")
        model.saveServerUrl(" https://kept.example ")

        assertEquals(
            listOf(null, null, null, " https://kept.example "),
            storage.savedValues,
        )
    }

    @Test
    fun pluginBindsDefaultModelAsOneInterfaceSingleton() {
        val koin = startKoin {
            modules(
                module {
                    with(Plugin) { setupDI(JsonObject(emptyMap())) }
                    single<ServerUrlStorage> { RecordingServerUrlStorage(null) }
                }
            )
        }
        try {
            val first = koin.koin.get<ServerUrlModel>()
            val second = koin.koin.get<ServerUrlModel>()

            assertIs<DefaultServerUrlModel>(first)
            assertSame(first, second)
        } finally {
            stopKoin()
        }
    }

    private class RecordingServerUrlStorage(
        private val storedValue: String?,
    ) : ServerUrlStorage {
        val savedValues = mutableListOf<String?>()

        override suspend fun getServerUrl(): String? = storedValue

        override suspend fun saveServerUrl(url: String?) {
            savedValues += url
        }
    }
}
