package dev.inmo.wishlist.features.ui.users.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasPerformImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.input.ImeAction
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.compose.nodeFactory
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.ui.users.JVMPlugin
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
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Runs the platform-contributed JVM password-change factory through a real Compose semantics host. */
@OptIn(ExperimentalTestApi::class)
class PasswordChangeViewTest {
    /** Pending form keeps its immutable configuration, protects both fields, and admits one Done request. */
    @Test
    fun pendingFactoryDrawsProtectedFormAndAdmitsOneCorrectedImeSubmission() = runComposeUiTest {
        val response = CompletableDeferred<PasswordChangeResult?>()
        val model = UserEditTestUsersModel(null, null, initiallyAuthorised = false).apply {
            passwordChangeHandler = { response.await() }
        }
        val fixture = createFixture(model, pendingConfig())
        try {
            setContent { fixture.view.onDraw() }
            waitForIdle()

            assertEquals(fixture.config, fixture.view.config)
            passwordField(UsersListStrings.newPasswordLabel.translation()).assertExists()
            passwordField(UsersListStrings.confirmPasswordLabel.translation()).assertExists()
            passwordField(UsersListStrings.newPasswordLabel.translation()).assertPasswordDoneInput()
            passwordField(UsersListStrings.confirmPasswordLabel.translation()).assertPasswordDoneInput()

            passwordField(UsersListStrings.newPasswordLabel.translation()).performTextInput("short")
            passwordField(UsersListStrings.confirmPasswordLabel.translation()).performTextInput("different")
            waitForIdle()
            onNodeWithText(UsersListStrings.passwordMismatch.translation()).assertExists()
            onNodeWithText(UsersListStrings.passwordChangeInvalidPassword.translation()).assertExists()
            onNodeWithText(UsersListStrings.changePasswordButton.translation()).assertIsNotEnabled()
            passwordField(UsersListStrings.newPasswordLabel.translation()).performImeAction()
            assertEquals(0, model.passwordChangeRequests.size)

            passwordField(UsersListStrings.newPasswordLabel.translation()).performTextClearance()
            passwordField(UsersListStrings.confirmPasswordLabel.translation()).performTextClearance()
            passwordField(UsersListStrings.newPasswordLabel.translation()).performTextInput("valid-password")
            passwordField(UsersListStrings.confirmPasswordLabel.translation()).performTextInput("valid-password")
            waitForIdle()
            passwordField(UsersListStrings.confirmPasswordLabel.translation()).performImeAction()
            waitUntil { model.passwordChangeRequests.size == 1 }

            assertEquals(1, model.passwordChangeRequests.size)
            passwordControl(UsersListStrings.newPasswordLabel.translation()).assertIsNotEnabled()
            passwordControl(UsersListStrings.confirmPasswordLabel.translation()).assertIsNotEnabled()
            onNodeWithText(UsersListStrings.changePasswordButton.translation()).assertIsNotEnabled()
            assertEquals(1, model.passwordChangeRequests.size)
            response.complete(PasswordChangeResult.InvalidApproval)
            waitForIdle()
        } finally {
            fixture.close()
        }
    }

    /** Completed factory config draws only credential-free completion content. */
    @Test
    fun completedFactoryDrawsCredentialFreeContentWithoutPasswordInputs() = runComposeUiTest {
        val fixture = createFixture(UserEditTestUsersModel(null, null, initiallyAuthorised = false), PasswordChangeViewConfig.Completed)
        try {
            setContent { fixture.view.onDraw() }
            waitForIdle()

            assertEquals(PasswordChangeViewConfig.Completed, fixture.view.config)
            onNodeWithText(UsersListStrings.passwordChanged.translation()).assertExists()
            onNodeWithText(UsersListStrings.newPasswordLabel.translation()).assertDoesNotExist()
            onNodeWithText(UsersListStrings.confirmPasswordLabel.translation()).assertDoesNotExist()
            assertTrue(onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isEmpty())
        } finally {
            fixture.close()
        }
    }

    /** Locates a production text field by its visible label and editable semantics. */
    private fun androidx.compose.ui.test.ComposeUiTest.passwordField(label: String) = onNode(
        hasSetTextAction() and hasAnyDescendant(hasText(label)),
        useUnmergedTree = true,
    )

    /** Locates a password field even after disabled state removes its editable semantics action. */
    private fun androidx.compose.ui.test.ComposeUiTest.passwordControl(label: String) = onNode(
        SemanticsMatcher.keyIsDefined(SemanticsProperties.Password) and hasAnyDescendant(hasText(label)),
        useUnmergedTree = true,
    )

    /** Verifies platform-observable password and IME semantics; Compose exposes no single-line key. */
    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertPasswordDoneInput() {
        assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
        assert(hasImeAction(ImeAction.Done))
        assert(hasPerformImeAction())
    }

    /** Creates a production JVM factory with controlled external model and navigation seams. */
    private fun createFixture(
        model: UserEditTestUsersModel,
        config: PasswordChangeViewConfig,
    ): PasswordChangeViewFixture {
        val application = startKoin {
            modules(
                module { with(JVMPlugin) { setupDI(JsonObject(emptyMap())) } },
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
        return PasswordChangeViewFixture(application, chainScope, chain, chainJob, config, view)
    }

    /** Immutable pending route used to prove the factory preserves the deeplink identity. */
    private fun pendingConfig(): PasswordChangeViewConfig.Pending = PasswordChangeViewConfig.Pending(
        UserId(7L), DeepLinkId("123e4567-e89b-42d3-a456-426614174000"),
    )

    /** Owns all host, Koin, and navigation resources created for one JVM view test. */
    private class PasswordChangeViewFixture(
        /** Test-scoped Koin application providing the real platform factory. */
        private val application: KoinApplication,
        /** Parent job for the started navigation chain. */
        private val chainScope: CoroutineScope,
        /** Started chain whose node lifecycle owns the injected ViewModel. */
        private val chain: NavigationChain<ViewConfig>,
        /** Started chain lifecycle job. */
        private val chainJob: Job,
        /** Exact configuration passed through the actual typed factory. */
        val config: PasswordChangeViewConfig,
        /** Concrete production view returned by the actual typed factory. */
        val view: PasswordChangeView,
    ) {
        /** Destroys the concrete node and joins every started navigation lifecycle before Koin closes. */
        fun close() = runBlocking {
            chain.drop(view)
            chainJob.cancelAndJoin()
            chainScope.coroutineContext[Job]?.cancelAndJoin()
            application.close()
            stopKoin()
        }
    }
}
