package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.micro_utils.common.MPPFile
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.models.UsersFeatureUser
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class EmailTestUsersModel(
    userId: UserId,
    initialProfile: AuthFeatureUser?,
    var emailFeatureEnabled: Boolean = true,
    var saveEmailResult: Boolean = true,
    var requestResult: EmailVerificationRequestResult = EmailVerificationRequestResult.Sent,
) : UsersModel {
    private val _currentUserId = MutableStateFlow<UserId?>(userId)
    private val _profile = MutableStateFlow(initialProfile)

    val savedEmails = mutableListOf<Email?>()
    val requestedEmails = mutableListOf<Email>()

    override val userAuthorisedState: StateFlow<Boolean> = MutableStateFlow(true)
    override val currentUserIdFlow: StateFlow<UserId?> = _currentUserId
    override val isCurrentUserRootFlow: StateFlow<Boolean> = MutableStateFlow(false)
    override val canChangeAvatarForOthersFlow: StateFlow<Boolean> = MutableStateFlow(false)

    override suspend fun getAllUsers(): List<UsersFeatureUser> = emptyList()
    override suspend fun getUser(id: UserId): UsersFeatureUser? = null
    override suspend fun getMyProfile(): AuthFeatureUser? = _profile.value
    override suspend fun isEmailFeatureEnabled(): Boolean = emailFeatureEnabled

    override suspend fun setMyEmail(email: Email?): Boolean {
        savedEmails += email
        if (saveEmailResult) {
            _profile.value = _profile.value?.copy(email = email, emailApproved = false)
        }
        return saveEmailResult
    }

    override suspend fun requestMyEmailVerification(expectedEmail: Email): EmailVerificationRequestResult {
        requestedEmails += expectedEmail
        return requestResult
    }

    override suspend fun updateUsername(id: UserId, username: Username): Boolean = false
    override suspend fun setPassword(id: UserId, password: Password): Boolean = false
    override suspend fun deleteUser(id: UserId): Boolean = false
    override suspend fun getAvatar(userId: UserId): FileId? = null
    override suspend fun uploadAvatar(userId: UserId, file: MPPFile): FileId? = null
    override fun imageUrl(id: FileId): String = ""
    override suspend fun loadImageBytes(id: FileId): ByteArray? = null
}

private object NoopUserEditInteractor : UserEditViewInteractor {
    override suspend fun onNavigateBack(node: NavigationNode<UserEditViewConfig, ViewConfig>) = Unit
    override suspend fun onSaved(node: NavigationNode<UserEditViewConfig, ViewConfig>) = Unit
    override suspend fun onDeleted(node: NavigationNode<UserEditViewConfig, ViewConfig>) = Unit
}

private fun userEditNode(userId: UserId): NavigationNode<UserEditViewConfig, ViewConfig> {
    val chain = NavigationChain<ViewConfig>(
        parentNode = null,
        nodeFactory = NavigationNodeFactory { _, _ -> null },
    )
    return NavigationNode.Empty(chain, UserEditViewConfig(userId))
}

private suspend fun waitUntil(condition: () -> Boolean) {
    withTimeout(5_000) {
        while (!condition()) {
            delay(10)
        }
    }
}

class UserEditViewModelEmailTest {
    private val ownerId = UserId(7L)
    private val owner = AuthFeatureUser(ownerId, Username("owner"), email = null)

    @Test
    fun missingOwnerEmailIsSavedThenVerified() = runTest {
        val model = EmailTestUsersModel(ownerId, owner)
        val viewModel = UserEditViewModel(userEditNode(ownerId), model, NoopUserEditInteractor)
        try {
            waitUntil { viewModel.canManageOwnEmailState.value && !viewModel.emailLoadingState.value }

            viewModel.onEmailChanged("owner@example.com")
            waitUntil { viewModel.isDirtyState.value }
            viewModel.onSaveEmailAndRequestVerification()

            waitUntil { viewModel.emailVerificationResultState.value == EmailVerificationRequestResult.Sent }
            assertEquals(listOf<Email?>(Email("owner@example.com")), model.savedEmails)
            assertEquals(listOf(Email("owner@example.com")), model.requestedEmails)
            waitUntil { !viewModel.isDirtyState.value }
            assertFalse(viewModel.isDirtyState.value)
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun failedEmailSaveDoesNotRequestVerification() = runTest {
        val model = EmailTestUsersModel(ownerId, owner, saveEmailResult = false)
        val viewModel = UserEditViewModel(userEditNode(ownerId), model, NoopUserEditInteractor)
        try {
            waitUntil { viewModel.canManageOwnEmailState.value && !viewModel.emailLoadingState.value }

            viewModel.onEmailChanged("owner@example.com")
            viewModel.onSaveEmailAndRequestVerification()

            waitUntil { viewModel.emailErrorState.value == EmailEditorError.SaveFailed }
            assertEquals(listOf<Email?>(Email("owner@example.com")), model.savedEmails)
            assertTrue(model.requestedEmails.isEmpty())
        } finally {
            viewModel.scope.cancel()
        }
    }

    @Test
    fun pendingOwnerEmailCanRetryVerificationWithoutSavingAgain() = runTest {
        val pendingEmail = Email("owner@example.com")
        val model = EmailTestUsersModel(ownerId, owner.copy(email = pendingEmail, emailApproved = false))
        val viewModel = UserEditViewModel(userEditNode(ownerId), model, NoopUserEditInteractor)
        try {
            waitUntil {
                viewModel.canManageOwnEmailState.value &&
                    !viewModel.emailLoadingState.value &&
                    viewModel.ownEmailProfileState.value?.email == pendingEmail
            }

            viewModel.onResendEmailVerification()

            waitUntil { viewModel.emailVerificationResultState.value == EmailVerificationRequestResult.Sent }
            assertTrue(model.savedEmails.isEmpty())
            assertEquals(listOf(pendingEmail), model.requestedEmails)
        } finally {
            viewModel.scope.cancel()
        }
    }
}
