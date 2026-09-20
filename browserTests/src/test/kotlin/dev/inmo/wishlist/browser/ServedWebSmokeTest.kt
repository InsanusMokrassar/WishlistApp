package dev.inmo.wishlist.browser

import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.WaitForSelectorState
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.Assertions.assertTrue
import java.util.UUID

/** Exercises the rendered Web application through the isolated Ktor server in managed Chromium. */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ServedWebSmokeTest {
    companion object {
        private lateinit var fixture: BrowserFixture

        /** Starts Playwright once because the Gradle gate supplies one isolated server per suite. */
        @JvmStatic @BeforeAll fun setUp() { fixture = BrowserFixture() }

        /** Closes the managed browser after all independent-context tests finish. */
        @JvmStatic @AfterAll fun tearDown() { fixture.close() }
    }

    /** Verifies that Compose mounts meaningful UI instead of merely serving an HTML shell. */
    @Test @Order(1) fun rendersApplication() = withPage("renders-application") { _, page ->
        page.navigate(fixture.baseUrl)
        page.locator(".app").waitFor(Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(15_000.0))
        page.locator(".topbar").waitFor(Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(15_000.0))
        assertTrue(page.getByRole(com.microsoft.playwright.options.AriaRole.HEADING, Page.GetByRoleOptions().setName("My Wishlists").setExact(true)).isVisible)
    }

    /** Registers an isolated account and confirms the authenticated wishlist action is rendered. */
    @Test @Order(2) fun registersAndReachesAuthenticatedUi() = withPage("registers-and-reaches-authenticated-ui") { _, page ->
        val username = "browser_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        page.navigate(fixture.baseUrl)
        page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON, Page.GetByRoleOptions().setName("Register").setExact(true)).first().click()
        page.locator("#auth-username").fill(username)
        page.locator("#auth-password").fill("BrowserTest!42")
        page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON, Page.GetByRoleOptions().setName("Create account").setExact(true)).click()
        page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON, Page.GetByRoleOptions().setName("Log out").setExact(true)).first().waitFor(Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(15_000.0))
        page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON, Page.GetByRoleOptions().setName("New Wishlist").setExact(true)).first().waitFor(Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(15_000.0))
    }

    private fun withPage(testName: String, block: (BrowserContext, Page) -> Unit) {
        val context = fixture.newContext()
        val page = context.newPage()
        val unexpectedErrors = mutableListOf<String>()
        page.onPageError { unexpectedErrors += it }
        page.onConsoleMessage { message ->
            if (message.type() == "error" && !isKnownAnonymousBootstrapError(message.text())) {
                unexpectedErrors += message.text()
            }
        }
        try {
            block(context, page)
            assertTrue(unexpectedErrors.isEmpty(), "Unexpected browser errors: $unexpectedErrors")
            context.tracing().stop()
        } catch (failure: Throwable) {
            fixture.captureFailure(context, page, testName)
            throw failure
        } finally {
            context.close()
        }
    }

    /** Allows the current unauthenticated API bootstrap response until the client owns a typed anonymous result. */
    private fun isKnownAnonymousBootstrapError(message: String): Boolean =
        message.contains("server responded with a status of 401") || message.contains("NoTransformationFoundException")
}
