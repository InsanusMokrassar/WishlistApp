package dev.inmo.wishlist.features.ui.users.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.compose.nodeFactory
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequestResult
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.ui.users.AndroidPlugin
import dev.inmo.wishlist.features.ui.users.UsersListStrings
import dev.inmo.wishlist.features.users.common.models.EmailProfile
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/** Production Android editor proof for delivery, independent read failure, uncertainty, and immediate owner privacy. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UserEditViewTest {
    /** Synchronizes production semantics in the explicitly owned Robolectric activity. */
    @get:Rule
    val composeRule = createEmptyComposeRule()

    /** Actual Android factory renders acknowledged Sent beside a failed GET and uses separate unknown-POST copy. */
    @Test
    fun passwordEmailFeedbackAndOwnerPrivacyUseProductionRenderer() {
        val ownerId = UserId(7L)
        val profile = EmailProfile(ownerId.long, Email("owner@example.com"), emailApproved = true)
        val model = UserEditTestUsersModel(ownerId, profile)
        lateinit var capturedViewModel: UserEditViewModel
        val application = startKoin {
            modules(
                module { with(AndroidPlugin) { setupDI(JsonObject(emptyMap())) } },
                module {
                    single<UsersModel> { model }
                    single<UserEditViewInteractor> { RecordingUserEditInteractor() }
                    factory { parameters -> UserEditViewModel(parameters.get(), get(), get(), Dispatchers.Unconfined).also { capturedViewModel = it } }
                },
            )
        }
        val chainScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val chain = NavigationChain<ViewConfig>(null, application.koin.nodeFactory())
        val chainJob = chain.start(chainScope)
        val view = checkNotNull(chain.push(UserEditViewConfig(ownerId)) as? UserEditView)
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        try {
            activity.setContent { view.onDraw() }
            composeRule.waitForIdle()
            composeRule.runOnUiThread {
                model.profileHandler = { throw IllegalStateException("read") }
                capturedViewModel.onRequestPasswordChangeEmail()
            }
            composeRule.waitForIdle()
            composeRule.onNodeWithText(UsersListStrings.passwordChangeEmailSent.translation()).assertExists()
            composeRule.onNodeWithText(UsersListStrings.emailLoadFailed.translation()).assertExists()
            composeRule.onNodeWithText(UsersListStrings.passwordChangeEmailDeliveryFailed.translation()).assertDoesNotExist()
            composeRule.runOnUiThread {
                model.profileHandler = { profile }
                capturedViewModel.onRefreshEmail()
                model.passwordChangeRequestResult = null
                capturedViewModel.onRequestPasswordChangeEmail()
            }
            composeRule.waitForIdle()
            composeRule.onNodeWithText(UsersListStrings.passwordChangeEmailUnconfirmed.translation()).assertExists()
            composeRule.onNodeWithText(UsersListStrings.passwordChangeEmailDeliveryFailed.translation()).assertDoesNotExist()
            composeRule.runOnUiThread {
                model.passwordChangeRequestResult = PasswordChangeEmailRequestResult.DeliveryFailed
                capturedViewModel.onRequestPasswordChangeEmail()
            }
            composeRule.waitForIdle()
            composeRule.onNodeWithText(UsersListStrings.passwordChangeEmailDeliveryFailed.translation()).assertExists()
            composeRule.onNodeWithText(UsersListStrings.passwordChangeEmailUnconfirmed.translation()).assertDoesNotExist()
            composeRule.runOnUiThread { model.authorisedState.value = false }
            composeRule.waitForIdle()
            composeRule.onNodeWithText(UsersListStrings.passwordChangeEmailDeliveryFailed.translation()).assertDoesNotExist()
            assertEquals(3, model.passwordChangeRequestedEmails.size)
        } finally {
            controller.pause().stop().destroy()
            runBlocking {
                chain.drop(view)
                chainJob.cancelAndJoin()
                chainScope.coroutineContext[Job]?.cancelAndJoin()
            }
            application.close()
            stopKoin()
        }
    }
}
