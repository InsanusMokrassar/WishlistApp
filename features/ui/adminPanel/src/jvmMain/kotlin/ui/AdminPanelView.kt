package dev.inmo.wishlist.features.ui.adminPanel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
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
        val sendState by viewModel.sendTestEmailState.collectAsState()
        val emailValid = Email.parse(recipientInput).isSuccess

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
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

            // Test-email section.
            Text(AdminPanelStrings.sendTestEmailSection.translation(), style = MaterialTheme.typography.h6)
            OutlinedTextField(
                value = recipientInput,
                onValueChange = { recipientInput = it },
                label = { Text(AdminPanelStrings.sendTestEmailRecipientLabel.translation()) },
                isError = recipientInput.isNotEmpty() && !emailValid
            )
            Button(
                onClick = {
                    Email.parse(recipientInput).onSuccess { viewModel.onSendTestEmail(it) }
                },
                enabled = emailValid
            ) {
                Text(AdminPanelStrings.sendTestEmailButton.translation())
            }
            when (sendState) {
                true -> Text(AdminPanelStrings.sendTestEmailSuccess.translation())
                false -> Text(AdminPanelStrings.sendTestEmailFailure.translation())
                null -> Unit
            }
        }
    }
}
