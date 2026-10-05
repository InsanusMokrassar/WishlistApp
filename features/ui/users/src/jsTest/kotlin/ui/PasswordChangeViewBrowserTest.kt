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

/** Compose HTML browser tests for pending and credential-free completed password screens. */
class PasswordChangeViewBrowserTest {
    /** Verifies native form submission uses password inputs and one guarded request. */
    @Test
    fun pendingFormUsesPasswordInputsAndNativeSubmitOnlyOnce() = MainScope().promise {
        pendingTerminalContent(PasswordChangeResult.InvalidApproval)
    }

    /** Unknown completion draws truthful terminal content through the production browser view. */
    @Test
    fun unknownCompletionStopsFormAndOffersContinue() = MainScope().promise {
        pendingTerminalContent(null)
    }

    /** Exercises captured native submit plus the synchronous ViewModel seam after controls disappear. */
    private suspend fun kotlinx.coroutines.CoroutineScope.pendingTerminalContent(outcome: PasswordChangeResult?) {
        val response = CompletableDeferred<PasswordChangeResult?>()
        val model = UserEditTestUsersModel(null, null, initiallyAuthorised = false).apply {
            passwordChangeHandler = { response.await() }
        }
        val fixture = BrowserViewTestFixture.create()
        lateinit var capturedViewModel: PasswordChangeViewModel
        val interactor = RecordingPasswordChangeInteractor()
        val application = startKoin {
            modules(
                module { with(JSPlugin) { setupDI(JsonObject(emptyMap())) } },
                module { single<UsersModel> { model } },
                module {
                    single<PasswordChangeViewInteractor> { interactor }
                    factory { parameters -> PasswordChangeViewModel(parameters.get(), get(), get()).also { capturedViewModel = it } }
                },
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

            response.complete(outcome)
            val expected = if (outcome == null) "The password may have changed, but the result could not be confirmed." else "This password-change link is no longer valid."
            fixture.awaitRenderedState("terminal approval feedback") { fixture.host.textContent.orEmpty().contains(expected) }
            assertEquals(0, fixture.host.querySelectorAll("input[type='password']").length)
            assertEquals(null, fixture.host.querySelector("form"))
            assertEquals(null, fixture.host.querySelector("button[type='submit']"))
            assertFalse(fixture.host.textContent.orEmpty().contains("Password changed."))
            form.dispatchEvent(Event("submit", js("({bubbles:true,cancelable:true})")))
            capturedViewModel.onSubmitPasswordChange()
            fixture.awaitRender()
            assertEquals(1, model.passwordChangeRequests.size)
            val buttons = fixture.host.querySelectorAll("button")
            requireNotNull((0 until buttons.length).mapNotNull { buttons.item(it) as? HTMLButtonElement }
                .firstOrNull { it.textContent == "Continue" }).click()
            fixture.awaitRenderedState("safe exit") { interactor.continueCalls == 1 }
            assertEquals(0, interactor.changedCalls)
        } finally {
            composition?.let { fixture.dispose(it, view, chain, chainJob, application) }
                ?: fixture.disposeUncomposed(view, chain, chainJob, application)
        }
    }

    /** Completed config mounts the production credential-free screen without either password input. */
    /** Verifies completed content contains no password inputs. */
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
