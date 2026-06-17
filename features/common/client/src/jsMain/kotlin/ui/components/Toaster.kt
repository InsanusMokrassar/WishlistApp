package dev.inmo.wishlist.features.common.client.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Process-wide transient toast bus for the Calm Studio web client.
 *
 * JS is single-threaded, so a plain singleton holding a [MutableStateFlow] is enough — no Koin wiring
 * needed. Any view calls [show] after an action; the [ToastHost] mounted once in the scaffold renders
 * the message and clears it automatically. Mirrors the design skill `app.jsx` `toast(...)` helper.
 */
object Toaster {
    private val _message = MutableStateFlow<String?>(null)

    /** Currently visible toast message, or `null` when nothing is shown. */
    val message: StateFlow<String?> = _message.asStateFlow()

    /**
     * Shows [text] as a transient toast. A subsequent call replaces the current message and restarts
     * the auto-dismiss timer (the [ToastHost] keys its timeout on the message value).
     *
     * @param text Already-translated, sentence-case confirmation line.
     */
    fun show(text: String) {
        _message.value = text
    }

    /** Hides the current toast immediately. */
    fun clear() {
        _message.value = null
    }
}

/**
 * Renders the single Calm Studio toast (`.toast`) driven by [Toaster].
 *
 * Mounted once at the app shell level (see the scaffold view); the element is `position: fixed`, so its
 * place in the DOM is cosmetic. While a message is present the `show` class fades it in; a
 * [LaunchedEffect] keyed on the message clears it after 2600 ms, matching the reference timing.
 */
@Composable
fun ToastHost() {
    val message by Toaster.message.collectAsState()

    LaunchedEffect(message) {
        if (message != null) {
            delay(2600)
            Toaster.clear()
        }
    }

    Div({ if (message != null) classes("toast", "show") else classes("toast") }) {
        Span({ classes("ok") }) { CalmIcon(CalmIcons.check) }
        Text(message ?: "")
    }
}
