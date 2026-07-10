package dev.inmo.wishlist.features.email.server.models

import kotlinx.coroutines.test.runTest
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * Verifies the [EmailAttachment.content] streaming provider contract: every invocation must
 * return a fresh, independently-readable [java.io.InputStream] over the full content, and
 * [EmailAttachment.mimeType] must default to `application/octet-stream`.
 */
class EmailAttachmentTest {

    /**
     * Invoking [EmailAttachment.content] twice must call the underlying provider twice and each
     * returned stream must be readable from the start, even after the previous stream was fully
     * consumed.
     */
    @Test
    fun contentProviderReturnsIndependentFreshStreamsOnEachInvocation() = runTest {
        val payload = "attachment payload".encodeToByteArray()
        var invocations = 0
        val attachment = EmailAttachment("file.bin") {
            invocations++
            ByteArrayInputStream(payload)
        }

        val firstRead = attachment.content().readBytes()
        val secondRead = attachment.content().readBytes()

        assertEquals(2, invocations)
        assertContentEquals(payload, firstRead)
        assertContentEquals(payload, secondRead)
    }

    /** [EmailAttachment.mimeType] defaults to `application/octet-stream` when not specified. */
    @Test
    fun mimeTypeDefaultsToOctetStream() {
        val attachment = EmailAttachment("file.bin") { ByteArrayInputStream(ByteArray(0)) }
        assertEquals("application/octet-stream", attachment.mimeType)
    }
}
