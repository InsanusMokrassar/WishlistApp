package dev.inmo.wishlist.features.ui.serverUrl

import dev.inmo.micro_utils.language_codes.IetfLang
import dev.inmo.micro_utils.strings.buildStringResource

/** Localized strings for the server URL editor screen. */
object ServerUrlStrings {
    /** Screen title. */
    val title = buildStringResource("Server address") {
        IetfLang.Russian("Адрес сервера")
    }

    /** Placeholder for the URL input field. */
    val urlPlaceholder = buildStringResource("https://example.com") {
        IetfLang.Russian("https://example.com")
    }

    /** Save button caption. */
    val saveButton = buildStringResource("Save") {
        IetfLang.Russian("Сохранить")
    }
}
