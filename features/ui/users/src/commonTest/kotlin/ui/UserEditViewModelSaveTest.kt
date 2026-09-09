package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UserEditViewModelSaveTest {
    private val userId = UserId(7L)
    private val user = AuthFeatureUser(userId, Username("owner"), email = null)

    @Test
    fun usernameFalseSkipsPasswordAndNavigation() = runTest {
        val model = UserEditTestUsersModel(userId, user).apply {
            rootState.value = true
            updateUsernameResult = false
        }
        val interactor = RecordingUserEditInteractor()
        val viewModel = UserEditViewModel(
            userEditTestNode(userId),
            model,
            interactor,
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            viewModel.onUsernameChanged("renamed")
            advanceUntilIdle()
            viewModel.onSave()
            advanceUntilIdle()

            assertEquals(listOf(userId to Username("renamed")), model.usernameUpdates)
            assertTrue(model.passwordUpdates.isEmpty())
            assertEquals(0, interactor.savedCalls)
            assertEquals(ProfileSaveError.UsernameSaveFailed, viewModel.profileSaveErrorState.value)
            assertTrue(viewModel.isDirtyState.value)
            assertFalse(viewModel.loadingState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun usernameThrowSkipsPasswordAndNavigation() = runTest {
        val model = UserEditTestUsersModel(userId, user).apply {
            rootState.value = true
            updateUsernameHandler = { _, _ -> throw IllegalStateException("username unavailable") }
        }
        val interactor = RecordingUserEditInteractor()
        val viewModel = UserEditViewModel(
            userEditTestNode(userId),
            model,
            interactor,
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            viewModel.onUsernameChanged("renamed")
            advanceUntilIdle()
            viewModel.onSave()
            advanceUntilIdle()

            assertTrue(model.passwordUpdates.isEmpty())
            assertEquals(0, interactor.savedCalls)
            assertEquals(ProfileSaveError.UsernameSaveFailed, viewModel.profileSaveErrorState.value)
            assertFalse(viewModel.loadingState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun passwordFalseAfterUsernameSuccessKeepsEditorOpen() = runTest {
        val model = UserEditTestUsersModel(userId, user).apply {
            rootState.value = true
            setPasswordResult = false
        }
        val interactor = RecordingUserEditInteractor()
        val viewModel = UserEditViewModel(
            userEditTestNode(userId),
            model,
            interactor,
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            viewModel.onUsernameChanged("renamed")
            viewModel.onPasswordChanged("new-password")
            viewModel.onConfirmPasswordChanged("new-password")
            advanceUntilIdle()
            viewModel.onSave()
            advanceUntilIdle()

            assertEquals(listOf(userId to Username("renamed")), model.usernameUpdates)
            assertEquals(1, model.passwordUpdates.size)
            assertEquals(0, interactor.savedCalls)
            assertEquals(ProfileSaveError.PasswordSaveFailed, viewModel.profileSaveErrorState.value)
            assertTrue(viewModel.isDirtyState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun successfulUsernameOnlyAndPasswordSavesNavigateOnceEach() = runTest {
        val usernameOnlyModel = UserEditTestUsersModel(userId, user).apply { rootState.value = true }
        val usernameOnlyInteractor = RecordingUserEditInteractor()
        val usernameOnlyViewModel = UserEditViewModel(
            userEditTestNode(userId),
            usernameOnlyModel,
            usernameOnlyInteractor,
            StandardTestDispatcher(testScheduler),
        )
        val passwordModel = UserEditTestUsersModel(userId, user).apply { rootState.value = true }
        val passwordInteractor = RecordingUserEditInteractor()
        val passwordViewModel = UserEditViewModel(
            userEditTestNode(userId),
            passwordModel,
            passwordInteractor,
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            usernameOnlyViewModel.onUsernameChanged("username-only")
            passwordViewModel.onUsernameChanged("with-password")
            passwordViewModel.onPasswordChanged("new-password")
            passwordViewModel.onConfirmPasswordChanged("new-password")
            advanceUntilIdle()
            usernameOnlyViewModel.onSave()
            passwordViewModel.onSave()
            advanceUntilIdle()

            assertEquals(1, usernameOnlyInteractor.savedCalls)
            assertTrue(usernameOnlyModel.passwordUpdates.isEmpty())
            assertNull(usernameOnlyViewModel.profileSaveErrorState.value)
            assertEquals(1, passwordInteractor.savedCalls)
            assertEquals(1, passwordModel.passwordUpdates.size)
            assertNull(passwordViewModel.profileSaveErrorState.value)
        } finally {
            usernameOnlyViewModel.scope.cancel()
            passwordViewModel.scope.cancel()
        }
    }

    @Test
    fun cancellationClearsLoadingWithoutProfileFailure() = runTest {
        val usernameEntered = CompletableDeferred<Unit>()
        val model = UserEditTestUsersModel(userId, user).apply {
            rootState.value = true
            updateUsernameHandler = { _, _ ->
                usernameEntered.complete(Unit)
                CompletableDeferred<Boolean>().await()
            }
        }
        val viewModel = UserEditViewModel(
            userEditTestNode(userId),
            model,
            RecordingUserEditInteractor(),
            StandardTestDispatcher(testScheduler),
        )
        try {
            advanceUntilIdle()
            viewModel.onUsernameChanged("renamed")
            advanceUntilIdle()
            viewModel.onSave()
            runCurrent()
            assertTrue(usernameEntered.isCompleted)

            val lifecycleJob = checkNotNull(viewModel.scope.coroutineContext[Job])
            viewModel.scope.cancel()
            lifecycleJob.join()

            assertFalse(viewModel.loadingState.value)
            assertNull(viewModel.profileSaveErrorState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }
}
