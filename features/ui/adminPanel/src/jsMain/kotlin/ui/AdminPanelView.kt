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
import dev.inmo.wishlist.features.common.client.ui.components.PageHead
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

        // Local state for the test-email recipient input.
        var recipientInput by remember { mutableStateOf("") }
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

            // Test-email section — send button validates locally before calling ViewModel.
            CalmTextField(
                value = recipientInput,
                onValueChange = { recipientInput = it },
                label = AdminPanelStrings.sendTestEmailRecipientLabel.translation(),
                hint = when {
                    recipientInput.isNotEmpty() && Email.parse(recipientInput).isFailure ->
                        AdminPanelStrings.sendTestEmailInvalid.translation()
                    sendState == true -> AdminPanelStrings.sendTestEmailSuccess.translation()
                    sendState == false -> AdminPanelStrings.sendTestEmailFailure.translation()
                    else -> null
                }
            )
            CalmButton(
                text = AdminPanelStrings.sendTestEmailButton.translation(),
                onClick = {
                    Email.parse(recipientInput).onSuccess { viewModel.onSendTestEmail(it) }
                },
                disabled = Email.parse(recipientInput).isFailure,
            )
        }
    }
}
