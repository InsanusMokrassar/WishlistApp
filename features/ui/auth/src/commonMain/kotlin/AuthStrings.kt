package dev.inmo.wishlist.features.ui.auth

import dev.inmo.micro_utils.language_codes.IetfLang
import dev.inmo.micro_utils.strings.buildStringResource
import dev.inmo.wishlist.features.common.client.CommonStrings

/** Localized strings used by the auth widget. */
object AuthStrings {
    /** Form heading shown above the inputs when the form is expanded. */
    val title = buildStringResource("Sign in") {
        IetfLang.Russian("Вход")
    }
    /** Username input placeholder. */
    val usernamePlaceholder = buildStringResource("Username") {
        IetfLang.Russian("Имя пользователя")
    }
    /** Password input placeholder. */
    val passwordPlaceholder = buildStringResource("Password") {
        IetfLang.Russian("Пароль")
    }
    /** Email input placeholder shown during registration. */
    val emailPlaceholder = buildStringResource("Email") {
        IetfLang.Russian("Электронная почта")
    }
    /** Caption of the button that opens the login form (collapsed state). */
    val loginButton = buildStringResource("Log in") {
        IetfLang.Russian("Войти")
    }
    /** Caption of the submit button inside the expanded form. */
    val submitButton = buildStringResource("Submit") {
        IetfLang.Russian("Отправить")
    }
    /** Caption of the cancel/close button inside the expanded form. */
    val cancelButton = CommonStrings.cancel
    /** Caption of the logout button shown when the user is authenticated. */
    val logoutButton = buildStringResource("Log out") {
        IetfLang.Russian("Выйти")
    }
    /** Error message shown when the credentials are rejected. */
    val errorLoginFailed = buildStringResource("Login failed. Check your credentials.") {
        IetfLang.Russian("Не удалось войти. Проверьте логин и пароль.")
    }
    /** Caption of the button that opens the registration form. */
    val registerButton = buildStringResource("Register") {
        IetfLang.Russian("Регистрация")
    }
    /** Caption of the submit button inside the registration form. */
    val submitRegisterButton = buildStringResource("Create account") {
        IetfLang.Russian("Создать аккаунт")
    }
    /** Error message shown when registration fails. */
    val errorRegisterFailed = buildStringResource("Registration failed. The username may already be taken.") {
        IetfLang.Russian("Не удалось зарегистрироваться. Возможно, имя пользователя уже занято.")
    }
    /** Heading of the confirmation shown after required-email registration. */
    val pendingEmailVerificationTitle = buildStringResource("Check your email") {
        IetfLang.Russian("Проверьте электронную почту")
    }
    /** Explains that verification is required before a first login can create a session. */
    val pendingEmailVerificationMessage = buildStringResource(
        "Check the submitted email and verify your account before logging in.",
    ) {
        IetfLang.Russian("Проверьте указанную почту и подтвердите аккаунт перед входом.")
    }
    /** Caption for closing the pending-email confirmation. */
    val pendingEmailVerificationDismissButton = buildStringResource("Close") {
        IetfLang.Russian("Закрыть")
    }
}
