package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

/**
 * Shared "Discard changes?" confirmation for the admin create/edit screens, rendered as a Calm Studio
 * `.scrim` modal (question title + consequence line, ghost Cancel + danger Discard). Replaces the old
 * Bootstrap modal markup the admin edit views used to inline.
 *
 * @param onCancel Invoked when the user dismisses the dialog (scrim click or Cancel).
 * @param onConfirm Invoked when the user confirms discarding unsaved changes.
 */
@Composable
fun DiscardModal(onCancel: () -> Unit, onConfirm: () -> Unit) {
    Div({
        classes(CalmStudioStyleSheet.scrim)
        onClick { onCancel() }
    }) {
        Div({
            classes(CalmStudioStyleSheet.modal)
            onClick { it.stopPropagation() }
        }) {
            Div({ classes(CalmStudioStyleSheet.mhead) }) {
                H2 { Text(AdminPanelStrings.confirmDiscardTitle.translation()) }
                P { Text(AdminPanelStrings.confirmDiscardMessage.translation()) }
            }
            Div({ classes(CalmStudioStyleSheet.mfoot) }) {
                Button({
                    classes(CalmStudioStyleSheet.btn, CalmStudioStyleSheet.ghost)
                    onClick { onCancel() }
                }) { Text(AdminPanelStrings.cancelButton.translation()) }
                Button({
                    classes(CalmStudioStyleSheet.btn, CalmStudioStyleSheet.danger)
                    onClick { onConfirm() }
                }) { Text(AdminPanelStrings.confirmButton.translation()) }
            }
        }
    }
}
