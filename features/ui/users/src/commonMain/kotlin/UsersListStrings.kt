package dev.inmo.wishlist.features.ui.users

import dev.inmo.micro_utils.language_codes.IetfLang
import dev.inmo.micro_utils.strings.buildStringResource

/** Localized strings used by the users list view. */
object UsersListStrings {
    /** Screen title. */
    val title = buildStringResource("Users") {
        IetfLang.Russian("Пользователи")
    }

    /** Placeholder text rendered while the list is loading. */
    val loading = buildStringResource("Loading users…") {
        IetfLang.Russian("Загрузка пользователей…")
    }

    /** Placeholder rendered when no users are registered. */
    val empty = buildStringResource("No registered users") {
        IetfLang.Russian("Зарегистрированных пользователей нет")
    }
}
