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

    /** Label for the per-row delete (danger) action; root only. */
    val deleteButton = buildStringResource("Delete") {
        IetfLang.Russian("Удалить")
    }

    /** Cancel action shared by both delete confirmation dialogs. */
    val cancelButton = buildStringResource("Cancel") {
        IetfLang.Russian("Отмена")
    }

    /** Title of the first delete confirmation dialog. */
    val confirmDeleteUserTitle = buildStringResource("Delete user?") {
        IetfLang.Russian("Удалить пользователя?")
    }

    /** Body of the first delete confirmation dialog (username appended by the view). */
    val confirmDeleteUserMessageFirst = buildStringResource("All wishlists, items, password and sessions of this user will be removed:") {
        IetfLang.Russian("Все списки, товары, пароль и сессии этого пользователя будут удалены:")
    }

    /** Confirm action of the first dialog (proceeds to the final confirmation). */
    val continueButton = buildStringResource("Continue") {
        IetfLang.Russian("Продолжить")
    }

    /** Title of the second (final) delete confirmation dialog. */
    val confirmDeleteUserFinalTitle = buildStringResource("Are you absolutely sure?") {
        IetfLang.Russian("Вы абсолютно уверены?")
    }

    /** Body of the second (final) delete confirmation dialog (username appended by the view). */
    val confirmDeleteUserMessageSecond = buildStringResource("This action cannot be undone. Permanently delete:") {
        IetfLang.Russian("Это действие необратимо. Безвозвратно удалить:")
    }

    /** Final destructive confirm action. */
    val confirmDeleteButton = buildStringResource("Delete permanently") {
        IetfLang.Russian("Удалить навсегда")
    }
}
