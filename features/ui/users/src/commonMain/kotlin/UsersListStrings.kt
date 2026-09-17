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

    /** Permission note shown for admin-managed profile fields. */
    val noEditableFields = buildStringResource(
        "Username and password can only be changed by an administrator."
    ) {
        IetfLang.Russian("Имя пользователя и пароль может изменить только администратор.")
    }

    /** Label for the owner-only email input. */
    val emailLabel = buildStringResource("Email address") { IetfLang.Russian("Email-адрес") }

    /** State shown while the private owner-email record is loading. */
    val emailLoading = buildStringResource("Loading email settings…") {
        IetfLang.Russian("Загрузка настроек email…")
    }

    /** Status shown when the owner has not stored an email yet. */
    val emailMissing = buildStringResource("Add an email address to receive a verification link.") {
        IetfLang.Russian("Добавьте email-адрес, чтобы получить ссылку для подтверждения.")
    }

    /** Status shown for an email that still awaits verification. */
    val emailPendingApproval = buildStringResource("This email address is waiting for verification.") {
        IetfLang.Russian("Этот email-адрес ожидает подтверждения.")
    }

    /** Status shown for an approved current email. */
    val emailApproved = buildStringResource("This email address is verified.") {
        IetfLang.Russian("Этот email-адрес подтверждён.")
    }

    /** Requests an email-authorized password-change link for the verified owner address. */
    val requestPasswordChangeButton = buildStringResource("Email me a password-change link") {
        IetfLang.Russian("Отправить ссылку для смены пароля")
    }

    /** Successful delivery feedback for a password-change approval message. */
    val passwordChangeEmailSent = buildStringResource("Password-change email sent.") {
        IetfLang.Russian("Письмо для смены пароля отправлено.")
    }

    /** Generic feedback when an approval request cannot be completed safely. */
    val passwordChangeEmailUnavailable = buildStringResource("Password-change email is unavailable. Refresh and try again.") {
        IetfLang.Russian("Письмо для смены пароля недоступно. Обновите данные и повторите попытку.")
    }

    /** Feedback for an ordinary SMTP delivery failure. */
    val passwordChangeEmailDeliveryFailed = buildStringResource("Could not send the password-change email. Try again.") {
        IetfLang.Russian("Не удалось отправить письмо для смены пароля. Повторите попытку.")
    }

    /** Title of the token-authorized password-change screen. */
    val passwordChangeTitle = buildStringResource("Choose a new password") {
        IetfLang.Russian("Выберите новый пароль")
    }

    /** Action label for submitting a token-authorized password replacement. */
    val changePasswordButton = buildStringResource("Change password") {
        IetfLang.Russian("Изменить пароль")
    }

    /** Explains the password-change policy without modifying entered text. */
    val passwordChangePolicy = buildStringResource("Use at least 8 characters and no more than 72 UTF-8 bytes.") {
        IetfLang.Russian("Используйте не менее 8 символов и не более 72 байт UTF-8.")
    }

    /** Feedback for a consumed, expired, or otherwise invalid approval. */
    val passwordChangeInvalidApproval = buildStringResource("This password-change link is no longer valid. Request a new email.") {
        IetfLang.Russian("Эта ссылка для смены пароля больше недействительна. Запросите новое письмо.")
    }

    /** Feedback for a server-side password-policy rejection. */
    val passwordChangeInvalidPassword = buildStringResource("Choose a password that meets the policy.") {
        IetfLang.Russian("Выберите пароль, соответствующий требованиям.")
    }

    /** Feedback when completion outcome cannot be safely confirmed. */
    val passwordChangeUnconfirmed = buildStringResource("Password change could not be confirmed. Do not retry this link; request a new email.") {
        IetfLang.Russian("Не удалось подтвердить смену пароля. Не повторяйте эту ссылку; запросите новое письмо.")
    }

    /** Credential-free completed-state message after a confirmed password replacement. */
    val passwordChanged = buildStringResource("Password changed. You can now sign in with the new password.") {
        IetfLang.Russian("Пароль изменён. Теперь можно войти с новым паролем.")
    }

    /** Saves a new owner email and immediately requests verification. */
    val saveEmailAndVerifyButton = buildStringResource("Save and send verification") {
        IetfLang.Russian("Сохранить и отправить подтверждение")
    }

    /** Requests another verification message for the saved pending email. */
    val resendEmailVerificationButton = buildStringResource("Resend verification") {
        IetfLang.Russian("Отправить подтверждение повторно")
    }

    /** Refreshes private owner-email state after returning from a verification link. */
    val refreshEmailButton = buildStringResource("Refresh email status") {
        IetfLang.Russian("Обновить статус email")
    }

    /** Local validation error for malformed email input. */
    val emailInvalid = buildStringResource("Enter a valid email address.") {
        IetfLang.Russian("Введите корректный email-адрес.")
    }

    /** Local failure state for a private owner-email refresh. */
    val emailLoadFailed = buildStringResource("Could not load email settings.") {
        IetfLang.Russian("Не удалось загрузить настройки email.")
    }

    /** Local uncertainty state when the email save response could not be confirmed. */
    val emailSaveFailed = buildStringResource(
        "The email save could not be confirmed. Refresh to check the saved address."
    ) {
        IetfLang.Russian("Не удалось подтвердить сохранение email. Обновите данные, чтобы проверить сохранённый адрес.")
    }

    /** Root-save failure before any password request was attempted. */
    val profileUsernameSaveFailed = buildStringResource(
        "The profile save could not be confirmed and no password request was attempted."
    ) {
        IetfLang.Russian("Не удалось подтвердить сохранение профиля; запрос на смену пароля не выполнялся.")
    }

    /** Root-save failure after the username save succeeded. */
    val profilePasswordSaveFailed = buildStringResource(
        "The username was saved but the password change could not be confirmed."
    ) {
        IetfLang.Russian("Имя пользователя сохранено, но не удалось подтвердить смену пароля.")
    }

    /** Server result: verification message accepted for delivery. */
    val emailVerificationSent = buildStringResource("Verification email sent.") {
        IetfLang.Russian("Письмо для подтверждения отправлено.")
    }

    /** Server result: current email is already approved. */
    val emailVerificationAlreadyApproved = buildStringResource("This email address is already verified.") {
        IetfLang.Russian("Этот email-адрес уже подтверждён.")
    }

    /** Server result: SMTP delivery is unavailable. */
    val emailVerificationUnavailable = buildStringResource("Email delivery is unavailable.") {
        IetfLang.Russian("Отправка email недоступна.")
    }

    /** Server result: no current email was available for the request. */
    val emailVerificationNoEmail = buildStringResource("Add an email address before requesting verification.") {
        IetfLang.Russian("Добавьте email-адрес перед запросом подтверждения.")
    }

    /** Server result: the address changed before the request completed. */
    val emailVerificationChanged = buildStringResource("The email address changed. Refresh and try again.") {
        IetfLang.Russian("Email-адрес изменился. Обновите страницу и повторите попытку.")
    }

    /** Server result: verification delivery failed after the address was saved. */
    val emailVerificationDeliveryFailed = buildStringResource("Could not send the verification email. Try again.") {
        IetfLang.Russian("Не удалось отправить письмо для подтверждения. Повторите попытку.")
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

    /** Accessibility text for the default avatar placeholder shown when a user has no photo. */
    val avatarPlaceholderAlt = buildStringResource("User avatar placeholder") {
        IetfLang.Russian("Заполнитель аватара пользователя")
    }
}
