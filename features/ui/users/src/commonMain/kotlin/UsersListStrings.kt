package dev.inmo.wishlist.features.ui.users

import dev.inmo.micro_utils.language_codes.IetfLang
import dev.inmo.micro_utils.strings.buildStringResource
import dev.inmo.wishlist.features.common.client.CommonStrings

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
    val cancelButton = CommonStrings.cancel

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

    /** Header button that opens the authenticated caller's own profile. */
    val myProfileButton = buildStringResource("My profile") {
        IetfLang.Russian("Мой профиль")
    }

    /** Title of the user profile detail screen. */
    val profileTitle = buildStringResource("Profile") {
        IetfLang.Russian("Профиль")
    }

    /** Title of the user profile edit screen. */
    val editProfileTitle = buildStringResource("Edit profile") {
        IetfLang.Russian("Изменить профиль")
    }

    /** Back navigation label. */
    val backButton = buildStringResource("Back") {
        IetfLang.Russian("Назад")
    }

    /** Edit action label on the profile detail screen (owner / root). */
    val editButton = buildStringResource("Edit") {
        IetfLang.Russian("Изменить")
    }

    /** Save action label on the edit screen. */
    val saveButton = CommonStrings.save

    /** Read-only user id field label. */
    val userIdLabel = buildStringResource("User ID") {
        IetfLang.Russian("ID пользователя")
    }

    /** Username field label. */
    val usernameLabel = buildStringResource("Username") {
        IetfLang.Russian("Имя пользователя")
    }

    /** New-password field label (root only; blank keeps the current password). */
    val newPasswordLabel = buildStringResource("New password") {
        IetfLang.Russian("Новый пароль")
    }

    /** Password confirmation field label (root only). */
    val confirmPasswordLabel = buildStringResource("Confirm password") {
        IetfLang.Russian("Подтвердите пароль")
    }

    /** Inline error shown when the password and confirmation differ. */
    val passwordMismatch = buildStringResource("Passwords do not match") {
        IetfLang.Russian("Пароли не совпадают")
    }

    /** Note shown to a non-root owner who has no editable fields. */
    val noEditableFields = buildStringResource("No editable fields available.") {
        IetfLang.Russian("Нет полей, доступных для редактирования.")
    }

    /** Avatar section label. */
    val avatarLabel = buildStringResource("Photo") {
        IetfLang.Russian("Фото")
    }

    /** Button that opens the image picker to upload an avatar. */
    val uploadPhotoButton = buildStringResource("Upload photo") {
        IetfLang.Russian("Загрузить фото")
    }

    /** Avatar upload in-progress label. */
    val uploadingPhoto = buildStringResource("Uploading…") {
        IetfLang.Russian("Загрузка…")
    }

    /** Title of the discard-changes confirmation dialog. */
    val confirmDiscardTitle = buildStringResource("Discard changes?") {
        IetfLang.Russian("Отменить изменения?")
    }

    /** Body of the discard-changes confirmation dialog. */
    val confirmDiscardMessage = buildStringResource("Unsaved changes will be lost.") {
        IetfLang.Russian("Несохранённые изменения будут потеряны.")
    }

    /** Generic confirm action (discard dialog). */
    val confirmButton = buildStringResource("Confirm") {
        IetfLang.Russian("Подтвердить")
    }
}
