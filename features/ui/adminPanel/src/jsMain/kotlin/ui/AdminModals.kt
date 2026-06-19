package dev.inmo.wishlist.features.ui.adminPanel.ui

import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.common.client.ui.components.ConfirmModal
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings

/**
 * Shared "Discard changes?" confirmation for the admin create/edit screens, rendered through the shared
 * [ConfirmModal] component (question title + consequence line, ghost Cancel + danger Discard).
 *
 * @param onCancel Invoked when the user dismisses the dialog (scrim click or Cancel).
 * @param onConfirm Invoked when the user confirms discarding unsaved changes.
 */
@Composable
fun DiscardModal(onCancel: () -> Unit, onConfirm: () -> Unit) {
    ConfirmModal(
        title = AdminPanelStrings.confirmDiscardTitle.translation(),
        body = AdminPanelStrings.confirmDiscardMessage.translation(),
        confirmLabel = AdminPanelStrings.confirmButton.translation(),
        cancelLabel = AdminPanelStrings.cancelButton.translation(),
        danger = true,
        onCancel = onCancel,
        onConfirm = onConfirm,
    )
}
