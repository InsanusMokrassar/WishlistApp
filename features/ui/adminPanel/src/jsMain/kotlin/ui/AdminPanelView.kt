package dev.inmo.wishlist.features.ui.adminPanel.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Text
import androidx.compose.runtime.collectAsState
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the admin panel dashboard screen. Uses Bootstrap classes. */
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

        Div({ classes("container", "py-4") }) {
            H1({ classes("h2", "mb-4") }) {
                Text(AdminPanelStrings.title.translation())
            }
            Div({ classes("d-flex", "gap-3") }) {
                Button({
                    classes("btn", "btn-primary", "btn-lg")
                    onClick { viewModel.onOpenUsers() }
                }) {
                    Text(AdminPanelStrings.usersSection.translation())
                }
                Button({
                    classes("btn", "btn-secondary", "btn-lg")
                    onClick { viewModel.onOpenWishlists() }
                }) {
                    Text(AdminPanelStrings.wishlistsSection.translation())
                }
            }

            val address by viewModel.testEmailAddressState.collectAsState()
            val sending by viewModel.sendingTestEmailState.collectAsState()
            val result by viewModel.testEmailResultState.collectAsState()
            Div({ classes("mt-4") }) {
                H2({ classes("h4", "mb-3") }) {
                    Text(AdminPanelStrings.testEmailSection.translation())
                }
                Div({ classes("mb-3") }) {
                    Input(type = InputType.Email) {
                        classes("form-control")
                        value(address)
                        onInput { viewModel.onTestEmailAddressChanged(it.value) }
                    }
                }
                Button({
                    classes("btn", "btn-outline-primary")
                    if (sending || address.isBlank()) disabled()
                    onClick { viewModel.onSendTestEmail() }
                }) {
                    Text(AdminPanelStrings.testEmailSendButton.translation())
                }
                when (result) {
                    true -> Div({ classes("text-success", "mt-2") }) {
                        Text(AdminPanelStrings.testEmailSuccess.translation())
                    }
                    false -> Div({ classes("text-danger", "mt-2") }) {
                        Text(AdminPanelStrings.testEmailFailure.translation())
                    }
                    null -> Unit
                }
            }
        }
    }
}
