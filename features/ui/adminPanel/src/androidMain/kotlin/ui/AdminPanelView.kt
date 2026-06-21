package dev.inmo.wishlist.features.ui.adminPanel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** Android Compose-Material3 view for the admin panel dashboard screen. */
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
        val resources = LocalResources.current

        var recipientInput by remember { mutableStateOf("") }
        val sendState by viewModel.sendTestEmailState.collectAsState()
        val emailValid = Email.parse(recipientInput).isSuccess

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                AdminPanelStrings.title.translation(resources),
                style = MaterialTheme.typography.headlineLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.onOpenUsers() }) {
                    Text(AdminPanelStrings.usersSection.translation(resources))
                }
                Button(onClick = { viewModel.onOpenWishlists() }) {
                    Text(AdminPanelStrings.wishlistsSection.translation(resources))
                }
            }

            // Test-email section.
            Text(
                AdminPanelStrings.sendTestEmailSection.translation(resources),
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = recipientInput,
                onValueChange = { recipientInput = it },
                label = { Text(AdminPanelStrings.sendTestEmailRecipientLabel.translation(resources)) },
                isError = recipientInput.isNotEmpty() && !emailValid
            )
            Button(
                onClick = {
                    Email.parse(recipientInput).onSuccess { viewModel.onSendTestEmail(it) }
                },
                enabled = emailValid
            ) {
                Text(AdminPanelStrings.sendTestEmailButton.translation(resources))
            }
            when (sendState) {
                true -> Text(AdminPanelStrings.sendTestEmailSuccess.translation(resources))
                false -> Text(AdminPanelStrings.sendTestEmailFailure.translation(resources))
                null -> Unit
            }
        }
    }
}
