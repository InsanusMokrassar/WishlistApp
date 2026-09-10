package dev.inmo.wishlist.features.ui.users.ui

import androidx.compose.runtime.Composition
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeState
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.yield
import org.koin.core.KoinApplication
import org.koin.core.context.stopKoin
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import kotlin.coroutines.resume

/** Accesses the JavaScript global object without assuming a preinstalled browser host. */
private fun globalObject(): dynamic = js("globalThis")

/** Creates the named JSDOM export from the local jsdom test dependency. */
private fun createBrowserDom(): dynamic =
    js("new (require('jsdom').JSDOM)('<!doctype html><html><body></body></html>', { pretendToBeVisual: true })")

/** Stores browser constructor globals before jsdom replaces them for Kotlin runtime type checks. */
private fun browserConstructors(global: dynamic): dynamic {
    val constructors: dynamic = js("({})")
    constructors.Node = global.Node
    constructors.Element = global.Element
    constructors.HTMLElement = global.HTMLElement
    constructors.HTMLDivElement = global.HTMLDivElement
    constructors.HTMLInputElement = global.HTMLInputElement
    constructors.HTMLFormElement = global.HTMLFormElement
    constructors.HTMLButtonElement = global.HTMLButtonElement
    constructors.Event = global.Event
    constructors.Text = global.Text
    constructors.Comment = global.Comment
    constructors.DocumentFragment = global.DocumentFragment
    constructors.requestAnimationFrame = global.requestAnimationFrame
    constructors.cancelAnimationFrame = global.cancelAnimationFrame
    return constructors
}

/** Copies every DOM constructor needed by Compose HTML runtime from jsdom into the Node global object. */
private fun installDomConstructors(global: dynamic, browserWindow: dynamic) {
    val copy: dynamic = js("""(global, browserWindow) => {
        for (const key of Object.getOwnPropertyNames(browserWindow)) {
            if (/^(HTML|SVG|Element$|Node$|Text$|Comment$|Document|Event|CSS)/.test(key)) {
                global[key] = browserWindow[key];
            }
        }
    }""")
    copy(global, browserWindow)
}

/**
 * Installs one standards DOM for the Compose HTML test process.
 *
 * Compose caches element builders by JavaScript document identity. Reusing the
 * same JSDOM window keeps those cached builders valid while each fixture still
 * owns and removes an independent temporary host.
 *
 * @param global JavaScript global object whose browser properties are restored during cleanup.
 * @param previousWindow Window value captured before installation.
 * @param previousDocument Document value captured before installation.
 * @param previousConstructors DOM constructor values captured before installation.
 * @param dom Created DOM window closed after Compose releases the fixture.
 */
private class BrowserDomEnvironment private constructor(
    /** JavaScript global object whose browser properties are restored during cleanup. */
    private val global: dynamic,
    /** Previous global window value, if a runner supplied one. */
    private val previousWindow: dynamic,
    /** Previous global document value, if a runner supplied one. */
    private val previousDocument: dynamic,
    /** Previous DOM constructor globals restored after Kotlin DOM casts are no longer needed. */
    private val previousConstructors: dynamic,
    /** Created DOM window, closed after the test releases Compose. */
    private val dom: dynamic,
) {
    /** Installs a temporary document before Compose and Koin initialize. */
    companion object {
        /** Process-shared DOM environment required by Compose's cached element builders. */
        private var installed: BrowserDomEnvironment? = null

        /** Installs or returns the process-shared JSDOM environment. */
        fun install(): BrowserDomEnvironment {
            installed?.let { return it }
            val global = globalObject()
            val dom = createBrowserDom()
            val environment = BrowserDomEnvironment(
                global,
                global.window,
                global.document,
                browserConstructors(global),
                dom,
            )
            global.window = dom.window
            global.document = dom.window.document
            installDomConstructors(global, dom.window)
            global.Node = dom.window.Node
            global.Element = dom.window.Element
            global.HTMLElement = dom.window.HTMLElement
            global.HTMLDivElement = dom.window.HTMLDivElement
            global.HTMLInputElement = dom.window.HTMLInputElement
            global.HTMLFormElement = dom.window.HTMLFormElement
            global.HTMLButtonElement = dom.window.HTMLButtonElement
            global.Event = dom.window.Event
            global.Text = dom.window.Text
            global.Comment = dom.window.Comment
            global.DocumentFragment = dom.window.DocumentFragment
            global.requestAnimationFrame = dom.window.requestAnimationFrame.bind(dom.window)
            global.cancelAnimationFrame = dom.window.cancelAnimationFrame.bind(dom.window)
            installed = environment
            registerRestoreAtProcessExit(global, environment)
            return environment
        }
    }

    /** Closes temporary DOM and restores any runner-provided browser globals. */
    fun restore() {
        dom.window.close()
        global.window = previousWindow
        global.document = previousDocument
        global.Node = previousConstructors.Node
        global.Element = previousConstructors.Element
        global.HTMLElement = previousConstructors.HTMLElement
        global.HTMLDivElement = previousConstructors.HTMLDivElement
        global.HTMLInputElement = previousConstructors.HTMLInputElement
        global.HTMLFormElement = previousConstructors.HTMLFormElement
        global.HTMLButtonElement = previousConstructors.HTMLButtonElement
        global.Event = previousConstructors.Event
        global.Text = previousConstructors.Text
        global.Comment = previousConstructors.Comment
        global.DocumentFragment = previousConstructors.DocumentFragment
        global.requestAnimationFrame = previousConstructors.requestAnimationFrame
        global.cancelAnimationFrame = previousConstructors.cancelAnimationFrame
    }
}

/** Restores runner globals only after the final Compose HTML element cache is no longer usable. */
private fun registerRestoreAtProcessExit(global: dynamic, environment: BrowserDomEnvironment) {
    val register: dynamic = js("""(global, restore) => {
        if (global.process && global.process.once) global.process.once('exit', restore);
    }""")
    register(global) { environment.restore() }
}

/** Dispatches the browser's bubbling input event, which Compose's delegated input handler observes. */
internal fun dispatchBrowserInput(target: HTMLInputElement) {
    val dispatch: dynamic = js("""(target) => {
        const view = target.ownerDocument.defaultView;
        const BrowserInputEvent = view.InputEvent || view.Event;
        target.dispatchEvent(new BrowserInputEvent('input', {
            bubbles: true,
            data: target.value,
            inputType: 'insertText'
        }));
    }""")
    dispatch(target)
}

/**
 * Owns a temporary browser DOM host and all test-scoped platform resources.
 *
 * @param host Per-test mount attached to the shared browser document body.
 * @param environment Shared DOM retained for Compose element-cache identity.
 */
internal class BrowserViewTestFixture private constructor(
    /** Temporary element mounted below the real browser document body. */
    val host: HTMLDivElement,
    /** Shared process DOM whose cached Compose element builders own this host. */
    @Suppress("unused") private val environment: BrowserDomEnvironment,
) {
    /** Adds a real browser host for a production Compose HTML composition. */
    companion object {
        /** Creates a temporary host attached to the installed browser document. */
        fun create(): BrowserViewTestFixture {
            val environment = BrowserDomEnvironment.install()
            val host = document.createElement("div") as HTMLDivElement
            checkNotNull(document.body).appendChild(host)
            return BrowserViewTestFixture(host, environment)
        }
    }

    /** Waits for Compose's real browser animation-frame render without a time-based sleep. */
    suspend fun awaitRender() {
        yield()
        suspendCancellableCoroutine { continuation ->
            window.requestAnimationFrame { continuation.resume(Unit) }
        }
        yield()
    }

    /** Waits for an observable render state through bounded Compose microtask turns, never a sleep. */
    suspend fun awaitRenderedState(description: String, state: () -> Boolean) {
        repeat(16) {
            if (state()) return
            awaitRender()
        }
        check(state()) { "Compose HTML did not publish $description; DOM=${host.innerHTML}" }
    }

    /** Releases composition, navigation, Koin, and the temporary DOM host in lifecycle order. */
    suspend fun dispose(
        composition: Composition,
        node: NavigationNode<*, ViewConfig>,
        chain: NavigationChain<ViewConfig>,
        chainJob: Job,
        application: KoinApplication,
    ) {
        composition.dispose()
        disposeUncomposed(node, chain, chainJob, application)
    }

    /** Releases resources when a composition fails before it can be returned to the test. */
    suspend fun disposeUncomposed(
        node: NavigationNode<*, ViewConfig>,
        chain: NavigationChain<ViewConfig>,
        chainJob: Job,
        application: KoinApplication,
    ) {
        try {
            node.changeState(NavigationNodeState.NEW)
            awaitRender()
        } finally {
            chainJob.cancelAndJoin()
            application.close()
            stopKoin()
            host.remove()
        }
    }
}
