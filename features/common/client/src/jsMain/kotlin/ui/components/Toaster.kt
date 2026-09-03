package dev.inmo.wishlist.features.common.client.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * One toast request.
 *
 * @property message Text provider invoked only from composition.
 * @property timeout Visibility duration in milliseconds; zero requests immediate hiding.
 */
data class ToastNotification(
    val message: @Composable () -> String,
    val timeout: Long = 2_600L,
) {
    init {
        require(timeout >= 0L) { "timeout must be non-negative milliseconds" }
    }
}

/**
 * Bounded, replay-aware notification queue for one sequential toast host.
 *
 * The replay slot preserves the newest notification emitted before host mounting. During active
 * collection, the same bounded slot retains at most one pending notification and drops an older pending
 * notification in favor of a newer notification. Acknowledging replay before suspension prevents an
 * already dequeued notification from appearing after host remount.
 */
internal class ToastQueue {
    /** Holds the latest pre-host or pending notification with deterministic oldest-value dropping. */
    private val notifications = MutableSharedFlow<ToastNotification>(
        replay = 1,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Enqueues [notification] without suspending under the configured positive-capacity policy. */
    fun enqueue(notification: ToastNotification) {
        check(notifications.tryEmit(notification))
    }

    /**
     * Delivers retained notifications sequentially and reports each visible and hidden transition.
     *
     * @param onCurrentChanged Receives the current notification, then `null` after the configured delay.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun consume(onCurrentChanged: (ToastNotification?) -> Unit) {
        notifications.collect { notification ->
            notifications.resetReplayCache()
            onCurrentChanged(notification)
            delay(notification.timeout)
            onCurrentChanged(null)
        }
    }
}

/**
 * Process-wide transient toast publisher for the Calm Studio web client.
 *
 * Views publish through [show]. The single [ToastHost] mounted by the scaffold consumes retained values
 * in order. Mirrors the design skill `app.jsx` `toast(...)` helper.
 */
object Toaster {
    /** Owns the process-wide queue consumed only by the scaffold-mounted [ToastHost]. */
    private val queue = ToastQueue()

    /** Enqueues [notification] for sequential display by the scaffold toast host. */
    fun show(notification: ToastNotification) {
        queue.enqueue(notification)
    }

    /**
     * Enqueues [text] with the default visibility duration.
     *
     * @param text Already-translated, sentence-case confirmation line.
     */
    fun show(text: String) {
        show(ToastNotification(message = { text }))
    }

    /** Forwards queue transitions to the single scaffold-mounted [ToastHost]. */
    internal suspend fun consume(onCurrentChanged: (ToastNotification?) -> Unit) {
        queue.consume(onCurrentChanged)
    }
}

/**
 * Renders the single Calm Studio toast (`.toast`) driven by [Toaster].
 *
 * Mounted once at the app shell level (see the scaffold view); the element is `position: fixed`, so its
 * place in the DOM is cosmetic. The single collector displays each notification for its own timeout and
 * clears the local state before processing the next retained notification.
 */
@Composable
fun ToastHost() {
    var current by remember { mutableStateOf<ToastNotification?>(null) }

    LaunchedEffect(Unit) {
        Toaster.consume { current = it }
    }

    Div({
        if (current != null) {
            classes(CalmStudioStyleSheet.toast, CalmStudioStyleSheet.show)
        } else {
            classes(CalmStudioStyleSheet.toast)
        }
    }) {
        Span({ classes(CalmStudioStyleSheet.ok) }) { CalmIcon(CalmIcons.check) }
        Text(current?.message?.invoke() ?: "")
    }
}
