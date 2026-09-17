package dev.inmo.wishlist.features.ui.adminPanel.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.CalmButton
import dev.inmo.wishlist.features.common.client.ui.components.CalmButtonVariant
import dev.inmo.wishlist.features.common.client.ui.components.CalmTextField
import dev.inmo.wishlist.features.common.client.ui.components.ContentColumn
import dev.inmo.wishlist.features.common.client.ui.components.ListRow
import dev.inmo.wishlist.features.common.client.ui.components.PageHead
import dev.inmo.wishlist.features.common.client.ui.components.RowsList
import dev.inmo.wishlist.features.common.client.ui.components.Subline
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the admin panel dashboard screen (Calm Studio). */
class AdminPanelView(
    chain: NavigationChain<ViewConfig>,
    config: AdminPanelViewConfig,
) : ComposeView<AdminPanelViewConfig, ViewConfig, AdminPanelViewModel>(config, chain) {
    override val viewModel: AdminPanelViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@AdminPanelView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()

        var recipientInput by remember { mutableStateOf("") }
        val users by viewModel.usersState.collectAsState()
        val usersLoading by viewModel.usersLoadingState.collectAsState()
        val usersLoadFailed by viewModel.usersLoadFailedState.collectAsState()
        val emailFeatureEnabled by viewModel.emailFeatureEnabledState.collectAsState()
        val sendingTestEmail by viewModel.sendTestEmailInProgressState.collectAsState()
        val sendState by viewModel.sendTestEmailState.collectAsState()

        ContentColumn {
            PageHead(
                title = AdminPanelStrings.title.translation(),
                actions = {
                    CalmButton(
                        text = AdminPanelStrings.usersSection.translation(),
                        onClick = { viewModel.onOpenUsers() },
                        variant = CalmButtonVariant.Primary,
                    )
                    CalmButton(
                        text = AdminPanelStrings.wishlistsSection.translation(),
                        onClick = { viewModel.onOpenWishlists() },
                    )
                },
            )

            P { Text(AdminPanelStrings.dashboardUsersSection.translation()) }
            when {
                usersLoading -> Subline(AdminPanelStrings.loading.translation())
                usersLoadFailed -> {
                    Subline(AdminPanelStrings.dashboardUsersLoadFailed.translation())
                    CalmButton(
                        text = AdminPanelStrings.retryButton.translation(),
                        onClick = { viewModel.onRetryUsers() },
                    )
                }
                users.isEmpty() -> Subline(AdminPanelStrings.dashboardUsersEmpty.translation())
                else -> RowsList {
                    users.forEach { user ->
                        ListRow(
                            text = "${user.username.string}  #${user.id.long}",
                            onSelect = { viewModel.onUserSelected(user.id) },
                        )
                    }
                }
            }

            if (emailFeatureEnabled) {
                P { Text(AdminPanelStrings.sendTestEmailSection.translation()) }
                P { Text(AdminPanelStrings.sendTestEmailExplanation.translation()) }
                CalmTextField(
                    value = recipientInput,
                    onValueChange = { recipientInput = it },
                    label = AdminPanelStrings.sendTestEmailRecipientLabel.translation(),
                    disabled = sendingTestEmail,
                    hint = when {
                        sendingTestEmail -> AdminPanelStrings.sendTestEmailSending.translation()
                        recipientInput.isNotEmpty() && Email.parse(recipientInput).isFailure ->
                            AdminPanelStrings.sendTestEmailInvalid.translation()
                        sendState == true -> AdminPanelStrings.sendTestEmailSuccess.translation()
                        sendState == false -> AdminPanelStrings.sendTestEmailFailure.translation()
                        else -> null
                    }
                )
                CalmButton(
                    text = if (sendingTestEmail) {
                        AdminPanelStrings.sendTestEmailSending.translation()
                    } else {
                        AdminPanelStrings.sendTestEmailButton.translation()
                    },
                    onClick = {
                        Email.parse(recipientInput).onSuccess { viewModel.onSendTestEmail(it) }
                    },
                    disabled = Email.parse(recipientInput).isFailure || sendingTestEmail,
                )
            } else {
                Subline(AdminPanelStrings.sendTestEmailUnavailable.translation())
            }
        }
    }
}
