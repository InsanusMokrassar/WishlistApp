package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequestResult
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.ui.users.JSPlugin
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import androidx.compose.runtime.Composition
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.promise
import kotlinx.serialization.json.JsonObject
import org.jetbrains.compose.web.renderComposable
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.w3c.dom.HTMLButtonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Executes the owner-only password-email control in the real JS profile-edit DOM. */
class UserEditViewBrowserTest {
    /** Approved owner sees one request control and receives its distinct delivery failure feedback. */
    @Test
    fun approvedOwnerRequestIsVisibleDisabledWhileBusyAndReportsDeliveryFailure() = MainScope().promise {
        val ownerId = UserId(7L)
        val email = Email("owner@example.com")
        val response = CompletableDeferred<PasswordChangeEmailRequestResult?>()
        val model = UserEditTestUsersModel(
            initialUserId = ownerId,
            initialProfile = AuthFeatureUser(ownerId, Username("owner"), email, emailApproved = true),
        ).apply {
            passwordChangeRequestHandler = { response.await() }
        }
        val fixture = BrowserViewTestFixture.create()
        val application = startKoin {
            modules(
                module { with(JSPlugin) { setupDI(JsonObject(emptyMap())) } },
                module { single<UsersModel> { model } },
                module { single<UserEditViewInteractor> { RecordingUserEditInteractor() } },
            )
        }
        JSPlugin.startPlugin(application.koin)
        val chain = NavigationChain<ViewConfig>(
            parentNode = null,
            nodeFactory = NavigationNodeFactory { navigationChain, config ->
                val edit = config as? UserEditViewConfig
                if (edit == null) NavigationNode.Empty(navigationChain, config)
                else UserEditView(navigationChain, edit)
            },
        )
        val chainJob = chain.start(this)
        val view = checkNotNull(chain.push(UserEditViewConfig(ownerId)) as? UserEditView)
        var composition: Composition? = null
        try {
            composition = renderComposable(fixture.host) { view.onDraw() }
            fixture.awaitRender()
            fixture.awaitRender()
            val buttons = fixture.host.querySelectorAll("button")
            val requestButton = requireNotNull(
                (0 until buttons.length)
                    .mapNotNull { buttons.item(it) as? HTMLButtonElement }
                    .firstOrNull { it.textContent == "Email me a password-change link" },
            )
            assertTrue(fixture.host.textContent.orEmpty().contains("Email verification"))
            fixture.awaitRenderedState("enabled owner password-change action") { !requestButton.disabled }
            assertFalse(requestButton.disabled)

            requestButton.click()
            fixture.awaitRenderedState("disabled owner password-change action") { requestButton.disabled }
            assertTrue(requestButton.disabled)
            assertEquals(listOf(email), model.passwordChangeRequestedEmails)
            response.complete(PasswordChangeEmailRequestResult.DeliveryFailed)
            fixture.awaitRenderedState("password-change delivery failure feedback") {
                fixture.host.textContent.orEmpty().contains("Could not send the password-change email. Try again.")
            }
            assertTrue(
                fixture.host.textContent.orEmpty().contains("Could not send the password-change email. Try again."),
            )
        } finally {
            composition?.let { fixture.dispose(it, view, chain, chainJob, application) }
                ?: fixture.disposeUncomposed(view, chain, chainJob, application)
        }
    }
}
