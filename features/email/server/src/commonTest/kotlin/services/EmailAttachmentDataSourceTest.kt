package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.server.models.EmailAttachment
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Verifies [EmailAttachmentDataSource], the suspend-to-blocking [jakarta.activation.DataSource]
 * bridge used to stream [EmailAttachment] content into Jakarta Mail. Exercises the bridge
 * directly from a plain (non-suspend) thread, mirroring how Jakarta Mail calls it from inside
 * `Transport.send` on the `Dispatchers.IO` worker.
 */
class EmailAttachmentDataSourceTest {

    /** Fixed content bytes each test attachment streams, used to assert fresh-stream reads. */
    private val payload = "bridge payload".encodeToByteArray()

    /**
     * Builds an [EmailAttachmentDataSource] whose backing [EmailAttachment.content] provider
     * increments [onInvocation] and returns a fresh stream over [payload] on every call.
     */
    private fun buildDataSource(onInvocation: () -> Unit): EmailAttachmentDataSource =
        EmailAttachmentDataSource(
            EmailAttachment("report.pdf", "application/pdf") {
                onInvocation()
                ByteArrayInputStream(payload)
            }
        )

    /**
     * Each [EmailAttachmentDataSource.getInputStream] call must re-invoke the attachment's
     * content provider and return a fresh, fully-readable stream over the full content.
     */
    @Test
    fun getInputStreamReturnsFreshStreamOnEachCall() {
        var invocations = 0
        val dataSource = buildDataSource { invocations++ }

        val firstRead = dataSource.getInputStream().readBytes()
        val secondRead = dataSource.getInputStream().readBytes()

        assertEquals(2, invocations)
        assertContentEquals(payload, firstRead)
        assertContentEquals(payload, secondRead)
    }

    /** [EmailAttachmentDataSource] must echo the backing attachment's MIME type and file name. */
    @Test
    fun metadataEchoesAttachment() {
        val dataSource = buildDataSource {}

        assertEquals("application/pdf", dataSource.getContentType())
        assertEquals("report.pdf", dataSource.getName())
    }

    /** [EmailAttachmentDataSource.getOutputStream] must always throw: the source is read-only. */
    @Test
    fun getOutputStreamThrowsUnsupportedOperation() {
        val dataSource = buildDataSource {}

        assertFailsWith<UnsupportedOperationException> { dataSource.getOutputStream() }
    }
}
