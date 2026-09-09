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
import kotlinx.coroutines.CoroutineScope
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
    /** Returns the currently rendered password-email action, never a detached historical node. */
    private fun BrowserViewTestFixture.passwordChangeRequestButton(): HTMLButtonElement? {
        val buttons = host.querySelectorAll("button")
        return (0 until buttons.length)
            .mapNotNull { buttons.item(it) as? HTMLButtonElement }
            .firstOrNull { it.textContent == "Email me a password-change link" }
    }

    /** Mounts the production JS editor and proves the current DOM does not expose the password-email action. */
    private suspend fun CoroutineScope.assertPasswordChangeRequestHidden(
        model: UserEditTestUsersModel,
        ownerId: UserId,
        privateEmailMustBeHidden: Boolean,
        settled: () -> Boolean = { true },
        afterInitialRender: suspend (BrowserViewTestFixture) -> Unit = {},
    ) {
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
            fixture.awaitRenderedState("requested owner-email state") { settled() }
            afterInitialRender(fixture)
            fixture.awaitRender()
            assertEquals(null, fixture.passwordChangeRequestButton())
            assertTrue(model.passwordChangeRequestedEmails.isEmpty())
            if (privateEmailMustBeHidden) {
                assertFalse(fixture.host.textContent.orEmpty().contains("owner@example.com"))
                assertFalse(fixture.host.textContent.orEmpty().contains("Email verification"))
            }
        } finally {
            composition?.let { fixture.dispose(it, view, chain, chainJob, application) }
                ?: fixture.disposeUncomposed(view, chain, chainJob, application)
        }
    }

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
            val requestButton = requireNotNull(fixture.passwordChangeRequestButton())
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

    /** Hides the owner password-email action after a disabled SMTP capability probe. */
    @Test
    fun disabledSmtpHidesPasswordChangeRequest() = MainScope().promise {
        val ownerId = UserId(7L)
        val model = UserEditTestUsersModel(
            ownerId,
            AuthFeatureUser(ownerId, Username("owner"), Email("owner@example.com"), emailApproved = true),
        ).apply { emailFeatureEnabled = false }
        assertPasswordChangeRequestHidden(
            model,
            ownerId,
            privateEmailMustBeHidden = true,
            settled = { model.probeReads > 0 },
        )
    }

    /** Omits the request control while an owner has no stored private email. */
    @Test
    fun missingEmailHidesPasswordChangeRequest() = MainScope().promise {
        val ownerId = UserId(7L)
        val model = UserEditTestUsersModel(
            ownerId,
            AuthFeatureUser(ownerId, Username("owner"), email = null, emailApproved = false),
        )
        assertPasswordChangeRequestHidden(
            model,
            ownerId,
            privateEmailMustBeHidden = false,
            settled = { model.profileReads > 0 },
        )
    }

    /** Omits the request control while an owner's stored email is awaiting approval. */
    @Test
    fun unapprovedEmailHidesPasswordChangeRequest() = MainScope().promise {
        val ownerId = UserId(7L)
        val model = UserEditTestUsersModel(
            ownerId,
            AuthFeatureUser(ownerId, Username("owner"), Email("owner@example.com"), emailApproved = false),
        )
        assertPasswordChangeRequestHidden(
            model,
            ownerId,
            privateEmailMustBeHidden = false,
            settled = { model.profileReads > 0 },
        )
    }

    /** Keeps another profile's private owner-email controls and feedback out of the rendered DOM. */
    @Test
    fun anotherUserHidesPrivatePasswordChangeControls() = MainScope().promise {
        val ownerId = UserId(7L)
        assertPasswordChangeRequestHidden(
            UserEditTestUsersModel(
                UserId(8L),
                AuthFeatureUser(ownerId, Username("owner"), Email("owner@example.com"), emailApproved = true),
            ),
            ownerId,
            privateEmailMustBeHidden = true,
        )
    }

    /** Keeps root authority from exposing another user's private owner-email controls or feedback. */
    @Test
    fun rootOnOtherUserHidesPrivatePasswordChangeControls() = MainScope().promise {
        val ownerId = UserId(7L)
        assertPasswordChangeRequestHidden(
            UserEditTestUsersModel(
                UserId(8L),
                AuthFeatureUser(ownerId, Username("owner"), Email("owner@example.com"), emailApproved = true),
            ).apply { rootState.value = true },
            ownerId,
            privateEmailMustBeHidden = true,
        )
    }

    /** Removes the current request control during a held refresh instead of retaining the old approved branch. */
    @Test
    fun heldRefreshRemovesCurrentPasswordChangeRequest() = MainScope().promise {
        val ownerId = UserId(7L)
        val heldProfile = CompletableDeferred<AuthFeatureUser?>()
        var holdRefresh = false
        val model = UserEditTestUsersModel(
            ownerId,
            AuthFeatureUser(ownerId, Username("owner"), Email("owner@example.com"), emailApproved = true),
        ).apply {
            profileHandler = {
                if (holdRefresh) heldProfile.await() else profileState.value
            }
        }
        assertPasswordChangeRequestHidden(
            model,
            ownerId,
            privateEmailMustBeHidden = false,
            settled = { model.profileReads > 0 },
            afterInitialRender = { fixture ->
                assertNotNull(fixture.passwordChangeRequestButton())
                holdRefresh = true
                val buttons = fixture.host.querySelectorAll("button")
                requireNotNull(
                    (0 until buttons.length)
                        .mapNotNull { buttons.item(it) as? HTMLButtonElement }
                        .firstOrNull { it.textContent == "Refresh email status" },
                ).click()
                fixture.awaitRenderedState("held refresh with cleared private profile") {
                    model.profileReads >= 2 &&
                        fixture.passwordChangeRequestButton() == null
                }
            },
        )
        if (!heldProfile.isCompleted) heldProfile.complete(model.profileState.value)
    }
}
