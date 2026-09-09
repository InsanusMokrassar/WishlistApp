package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.ui.users.JSPlugin
import dev.inmo.wishlist.features.users.common.models.UserId
import androidx.compose.runtime.Composition
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.promise
import kotlinx.serialization.json.JsonObject
import org.jetbrains.compose.web.renderComposable
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.dom.HTMLButtonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Executes the real JS password-change view against browser DOM events. */
class PasswordChangeViewBrowserTest {
    /** Pending form renders secure inputs, live feedback, and admits one native form submission. */
    @Test
    fun pendingFormUsesPasswordInputsAndNativeSubmitOnlyOnce() = MainScope().promise {
        val response = CompletableDeferred<PasswordChangeResult?>()
        val model = UserEditTestUsersModel(null, null, initiallyAuthorised = false).apply {
            passwordChangeHandler = { response.await() }
        }
        val fixture = BrowserViewTestFixture.create()
        val application = startKoin {
            modules(
                module { with(JSPlugin) { setupDI(JsonObject(emptyMap())) } },
                module { single<UsersModel> { model } },
                module { single<PasswordChangeViewInteractor> { RecordingPasswordChangeInteractor() } },
            )
        }
        JSPlugin.startPlugin(application.koin)
        val chain = NavigationChain<ViewConfig>(
            parentNode = null,
            nodeFactory = NavigationNodeFactory { navigationChain, config ->
                val pending = config as? PasswordChangeViewConfig
                if (pending == null) NavigationNode.Empty(navigationChain, config)
                else PasswordChangeView(navigationChain, pending)
            },
        )
        val chainJob = chain.start(this)
        val view = checkNotNull(
            chain.push(
                PasswordChangeViewConfig.Pending(
                    UserId(7L),
                    DeepLinkId("123e4567-e89b-42d3-a456-426614174000"),
                ),
            ) as? PasswordChangeView,
        )
        var composition: Composition? = null
        try {
            composition = renderComposable(fixture.host) { view.onDraw() }
            fixture.awaitRender()
            val password = requireNotNull(fixture.host.querySelector("#password-change-password") as? HTMLInputElement)
            val confirmation = requireNotNull(fixture.host.querySelector("#password-change-confirmation") as? HTMLInputElement)
            val form = requireNotNull(fixture.host.querySelector("form") as? HTMLFormElement)

            assertEquals("password", password.type)
            assertEquals("password", confirmation.type)
            assertTrue(fixture.host.textContent.orEmpty().contains("New password"))
            assertTrue(fixture.host.textContent.orEmpty().contains("Confirm password"))

            password.value = "short"
            dispatchBrowserInput(password)
            confirmation.value = "different"
            dispatchBrowserInput(confirmation)
            fixture.awaitRenderedState("mismatch feedback") {
                fixture.host.textContent.orEmpty().contains("Passwords do not match")
            }
            assertTrue(fixture.host.textContent.orEmpty().contains("Passwords do not match"))
            assertTrue(fixture.host.textContent.orEmpty().contains("Choose a password that meets the policy"))

            password.value = "valid-password"
            dispatchBrowserInput(password)
            confirmation.value = "valid-password"
            dispatchBrowserInput(confirmation)
            fixture.awaitRenderedState("enabled native submit") {
                val submit = fixture.host.querySelector("button[type='submit']") as? HTMLButtonElement
                submit != null && !submit.disabled
            }
            form.asDynamic().requestSubmit()
            form.asDynamic().requestSubmit()
            fixture.awaitRenderedState("one admitted password-change request") { model.passwordChangeRequests.size == 1 }
            fixture.awaitRenderedState("disabled password-change controls") {
                val currentPassword = fixture.host.querySelector("#password-change-password") as? HTMLInputElement
                val currentConfirmation = fixture.host.querySelector("#password-change-confirmation") as? HTMLInputElement
                val submit = fixture.host.querySelector("button[type='submit']") as? HTMLButtonElement
                currentPassword?.disabled == true && currentConfirmation?.disabled == true && submit?.disabled == true
            }

            assertEquals(1, model.passwordChangeRequests.size)
            assertTrue((fixture.host.querySelector("#password-change-password") as? HTMLInputElement)?.disabled == true)
            assertTrue((fixture.host.querySelector("#password-change-confirmation") as? HTMLInputElement)?.disabled == true)
            assertTrue((fixture.host.querySelector("button[type='submit']") as? HTMLButtonElement)?.disabled == true)

            response.complete(PasswordChangeResult.InvalidApproval)
            fixture.awaitRenderedState("invalid approval feedback") {
                fixture.host.textContent.orEmpty().contains("This password-change link is no longer valid.")
            }
            assertTrue(fixture.host.textContent.orEmpty().contains("This password-change link is no longer valid."))
        } finally {
            composition?.let { fixture.dispose(it, view, chain, chainJob, application) }
                ?: fixture.disposeUncomposed(view, chain, chainJob, application)
        }
    }

    /** Completed config mounts the production credential-free screen without either password input. */
    @Test
    fun completedFormContainsNoPasswordInputs() = MainScope().promise {
        val model = UserEditTestUsersModel(null, null, initiallyAuthorised = false)
        val fixture = BrowserViewTestFixture.create()
        val application = startKoin {
            modules(
                module { with(JSPlugin) { setupDI(JsonObject(emptyMap())) } },
                module { single<UsersModel> { model } },
                module { single<PasswordChangeViewInteractor> { RecordingPasswordChangeInteractor() } },
            )
        }
        JSPlugin.startPlugin(application.koin)
        val chain = NavigationChain<ViewConfig>(
            parentNode = null,
            nodeFactory = NavigationNodeFactory { navigationChain, config ->
                val completed = config as? PasswordChangeViewConfig
                if (completed == null) NavigationNode.Empty(navigationChain, config)
                else PasswordChangeView(navigationChain, completed)
            },
        )
        val chainJob = chain.start(this)
        val view = checkNotNull(chain.push(PasswordChangeViewConfig.Completed) as? PasswordChangeView)
        var composition: Composition? = null
        try {
            composition = renderComposable(fixture.host) { view.onDraw() }
            fixture.awaitRender()
            assertEquals(0, fixture.host.querySelectorAll("input[type='password']").length)
            assertTrue(fixture.host.textContent.orEmpty().contains("Password changed."))
        } finally {
            composition?.let { fixture.dispose(it, view, chain, chainJob, application) }
                ?: fixture.disposeUncomposed(view, chain, chainJob, application)
        }
    }
}
