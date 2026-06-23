package dev.inmo.wishlist.features.ui.topBar

import dev.inmo.micro_utils.language_codes.IetfLang
import dev.inmo.micro_utils.strings.buildStringResource

/** Localized strings for the top bar. */
object TopBarStrings {
    /** Application title shown on the left side of the top bar. */
    val appTitle = buildStringResource("Wishlist") {
        IetfLang.Russian("Список желаний")
    }

    /** Label of the "change server URL" button. */
    val changeServerUrlButton = buildStringResource("Server") {
        IetfLang.Russian("Сервер")
    }

    /** Placeholder for the global search field (people / lists / items). */
    val searchPlaceholder = buildStringResource("Search people, lists, items…") {
        IetfLang.Russian("Поиск людей, списков, товаров…")
    }

    /** Tooltip shown on the (temporarily disabled) search field — search not yet implemented. */
    val searchComingSoonTooltip = buildStringResource("This feature will be implemented soon") {
        IetfLang.Russian("Эта функция будет добавлена скоро")
    }
}
