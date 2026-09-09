package dev.inmo.wishlist.features.ui.users.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasPerformImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.compose.nodeFactory
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.ui.users.AndroidPlugin
import dev.inmo.wishlist.features.ui.users.UsersListStrings
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Executes the Android platform password-change factory in a Robolectric-owned ComponentActivity. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
/** Android Compose tests for protected pending and credential-free completed views. */
class PasswordChangeViewTest {
    /** Empty Compose rule synchronizes semantics while each test explicitly owns its activity host. */
    @get:Rule
    val composeRule = createEmptyComposeRule()

    /** Pending form keeps its configuration, exposes password Done semantics, and admits one corrected request. */
    /** Verifies the pending factory masks fields and admits one corrected IME submission. */
    @Test
    fun pendingFactoryDrawsProtectedFormAndAdmitsOneCorrectedImeSubmission() {
        val response = CompletableDeferred<PasswordChangeResult?>()
        val model = UserEditTestUsersModel(null, null, initiallyAuthorised = false).apply {
            passwordChangeHandler = { response.await() }
        }
        val fixture = createFixture(model, pendingConfig())
        try {
            fixture.activity.setContent { fixture.view.onDraw() }
            composeRule.waitForIdle()

            assertEquals(fixture.config, fixture.view.config)
            passwordField(UsersListStrings.newPasswordLabel.translation()).assertExists()
            passwordField(UsersListStrings.confirmPasswordLabel.translation()).assertExists()
            passwordField(UsersListStrings.newPasswordLabel.translation()).assertPasswordDoneInput()
            passwordField(UsersListStrings.confirmPasswordLabel.translation()).assertPasswordDoneInput()

            passwordField(UsersListStrings.newPasswordLabel.translation()).performTextInput("short")
            passwordField(UsersListStrings.confirmPasswordLabel.translation()).performTextInput("different")
            composeRule.waitForIdle()
            composeRule.onNodeWithText(UsersListStrings.passwordMismatch.translation()).assertExists()
            composeRule.onNodeWithText(UsersListStrings.passwordChangeInvalidPassword.translation()).assertExists()
            composeRule.onNodeWithText(UsersListStrings.changePasswordButton.translation()).assertIsNotEnabled()
            passwordField(UsersListStrings.newPasswordLabel.translation()).performImeAction()
            assertEquals(0, model.passwordChangeRequests.size)

            passwordField(UsersListStrings.newPasswordLabel.translation()).performTextClearance()
            passwordField(UsersListStrings.confirmPasswordLabel.translation()).performTextClearance()
            passwordField(UsersListStrings.newPasswordLabel.translation()).performTextInput("valid-password")
            passwordField(UsersListStrings.confirmPasswordLabel.translation()).performTextInput("valid-password")
            composeRule.waitForIdle()
            passwordField(UsersListStrings.confirmPasswordLabel.translation()).performImeAction()
            composeRule.waitUntil { model.passwordChangeRequests.size == 1 }

            assertEquals(1, model.passwordChangeRequests.size)
            passwordControl(UsersListStrings.newPasswordLabel.translation()).assertIsNotEnabled()
            passwordControl(UsersListStrings.confirmPasswordLabel.translation()).assertIsNotEnabled()
            composeRule.onNodeWithText(UsersListStrings.changePasswordButton.translation()).assertIsNotEnabled()
            assertEquals(1, model.passwordChangeRequests.size)
            response.complete(PasswordChangeResult.InvalidApproval)
            composeRule.waitForIdle()
        } finally {
            fixture.close()
        }
    }

    /** Completed factory config draws credential-free content and never renders editable password fields. */
    /** Verifies the completed factory omits password inputs and actions. */
    @Test
    fun completedFactoryDrawsCredentialFreeContentWithoutPasswordInputs() {
        val fixture = createFixture(UserEditTestUsersModel(null, null, initiallyAuthorised = false), PasswordChangeViewConfig.Completed)
        try {
            fixture.activity.setContent { fixture.view.onDraw() }
            composeRule.waitForIdle()

            assertEquals(PasswordChangeViewConfig.Completed, fixture.view.config)
            composeRule.onNodeWithText(UsersListStrings.passwordChanged.translation()).assertExists()
            composeRule.onNodeWithText(UsersListStrings.newPasswordLabel.translation()).assertDoesNotExist()
            composeRule.onNodeWithText(UsersListStrings.confirmPasswordLabel.translation()).assertDoesNotExist()
            assertTrue(composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isEmpty())
        } finally {
            fixture.close()
        }
    }

    /** Locates a production text field by visible label and editable semantics. */
    private fun passwordField(label: String) = composeRule.onNode(
        hasSetTextAction() and hasAnyDescendant(hasText(label)),
        useUnmergedTree = true,
    )

    /** Locates a password field even after disabled state removes its editable semantics action. */
    private fun passwordControl(label: String) = composeRule.onNode(
        SemanticsMatcher.keyIsDefined(SemanticsProperties.Password) and hasAnyDescendant(hasText(label)),
        useUnmergedTree = true,
    )

    /** Verifies observable Password and Done semantics; Compose does not expose a single-line semantics key. */
    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertPasswordDoneInput() {
        assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
        assert(hasImeAction(ImeAction.Done))
        assert(hasPerformImeAction())
    }

    /** Starts a direct Robolectric activity and the actual Android typed factory with controlled external seams. */
    private fun createFixture(
        model: UserEditTestUsersModel,
        config: PasswordChangeViewConfig,
    ): PasswordChangeViewFixture {
        val application = startKoin {
            modules(
                module { with(AndroidPlugin) { setupDI(JsonObject(emptyMap())) } },
                module {
                    single<UsersModel> { model }
                    single<PasswordChangeViewInteractor> { RecordingPasswordChangeInteractor() }
                    factory { parameters -> PasswordChangeViewModel(parameters.get(), get(), get(), Dispatchers.Unconfined) }
                },
            )
        }
        val chainScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val chain = NavigationChain<ViewConfig>(null, application.koin.nodeFactory())
        val chainJob = chain.start(chainScope)
        val view = checkNotNull(chain.push(config) as? PasswordChangeView)
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        return PasswordChangeViewFixture(application, chainScope, chain, chainJob, activity, config, view)
    }

    /** Immutable pending route used to prove the Android factory preserves deeplink identity. */
    private fun pendingConfig(): PasswordChangeViewConfig.Pending = PasswordChangeViewConfig.Pending(
        UserId(7L), DeepLinkId("123e4567-e89b-42d3-a456-426614174000"),
    )

    /** Owns Koin, navigation, and direct ComponentActivity resources for one Android host test. */
    private class PasswordChangeViewFixture(
        /** Test-scoped Koin application supplying the production Android factory. */
        private val application: KoinApplication,
        /** Parent scope for the explicitly started navigation chain. */
        private val chainScope: CoroutineScope,
        /** Started chain owning the production view lifecycle. */
        private val chain: NavigationChain<ViewConfig>,
        /** Started chain job joined during cleanup. */
        private val chainJob: Job,
        /** Directly created Robolectric host that owns the Compose composition. */
        val activity: ComponentActivity,
        /** Exact configuration passed through the real typed factory. */
        val config: PasswordChangeViewConfig,
        /** Concrete Android production view returned by that factory. */
        val view: PasswordChangeView,
    ) {
        /** Destroys host and node, then joins lifecycle jobs before Koin shutdown. */
        fun close() = runBlocking {
            activity.finish()
            chain.drop(view)
            chainJob.cancelAndJoin()
            chainScope.coroutineContext[Job]?.cancelAndJoin()
            application.close()
            stopKoin()
        }
    }
}
