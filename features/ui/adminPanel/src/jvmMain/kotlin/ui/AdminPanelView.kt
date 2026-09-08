package dev.inmo.wishlist.features.ui.adminPanel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.ListRow
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JVM Compose-Desktop view for the admin panel dashboard screen. */
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
        val emailValid = Email.parse(recipientInput).isSuccess

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(AdminPanelStrings.title.translation(), style = MaterialTheme.typography.h4)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.onOpenUsers() }) {
                    Text(AdminPanelStrings.usersSection.translation())
                }
                Button(onClick = { viewModel.onOpenWishlists() }) {
                    Text(AdminPanelStrings.wishlistsSection.translation())
                }
            }

            Text(AdminPanelStrings.dashboardUsersSection.translation(), style = MaterialTheme.typography.h6)
            when {
                usersLoading -> CircularProgressIndicator()
                usersLoadFailed -> {
                    Text(AdminPanelStrings.dashboardUsersLoadFailed.translation())
                    Button(onClick = { viewModel.onRetryUsers() }) {
                        Text(AdminPanelStrings.retryButton.translation())
                    }
                }
                users.isEmpty() -> Text(AdminPanelStrings.dashboardUsersEmpty.translation())
                else -> users.forEach { user ->
                    ListRow(
                        text = "${user.username.string}  #${user.id.long}",
                        onSelect = { viewModel.onUserSelected(user.id) },
                    )
                }
            }

            if (emailFeatureEnabled) {
                Text(AdminPanelStrings.sendTestEmailSection.translation(), style = MaterialTheme.typography.h6)
                Text(
                    AdminPanelStrings.sendTestEmailExplanation.translation(),
                    style = MaterialTheme.typography.body2,
                )
                OutlinedTextField(
                    value = recipientInput,
                    onValueChange = { recipientInput = it },
                    label = { Text(AdminPanelStrings.sendTestEmailRecipientLabel.translation()) },
                    isError = recipientInput.isNotEmpty() && !emailValid,
                    enabled = !sendingTestEmail,
                )
                Button(
                    onClick = {
                        Email.parse(recipientInput).onSuccess { viewModel.onSendTestEmail(it) }
                    },
                    enabled = emailValid && !sendingTestEmail,
                ) {
                    Text(
                        if (sendingTestEmail) {
                            AdminPanelStrings.sendTestEmailSending.translation()
                        } else {
                            AdminPanelStrings.sendTestEmailButton.translation()
                        }
                    )
                }
                when (sendState) {
                    true -> Text(AdminPanelStrings.sendTestEmailSuccess.translation())
                    false -> Text(AdminPanelStrings.sendTestEmailFailure.translation())
                    null -> Unit
                }
            } else {
                Text(AdminPanelStrings.sendTestEmailUnavailable.translation())
            }
        }
    }
}
