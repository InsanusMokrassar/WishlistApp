package dev.inmo.wishlist.features.common.client

import dev.inmo.micro_utils.language_codes.IetfLang
import dev.inmo.micro_utils.strings.buildStringResource

/**
 * Localized string resources shared across UI features.
 *
 * Holds generic action labels that were previously redeclared in each feature's `*Strings`
 * object. Feature objects delegate their own `saveButton` / `cancelButton` vals here to avoid
 * duplicated resources and translation drift.
 */
object CommonStrings {
    /** Generic "save" action label. */
    val save = buildStringResource("Save") {
        IetfLang.Russian("Сохранить")
    }

    /** Generic "cancel" action label. */
    val cancel = buildStringResource("Cancel") {
        IetfLang.Russian("Отмена")
    }
}
