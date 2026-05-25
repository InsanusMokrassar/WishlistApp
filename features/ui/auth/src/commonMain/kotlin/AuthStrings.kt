package dev.inmo.wishlist.features.ui.auth

import dev.inmo.micro_utils.language_codes.IetfLang
import dev.inmo.micro_utils.strings.buildStringResource

object AuthStrings {
    val title = buildStringResource("Sign in") {
        IetfLang.Russian("Вход")
    }
    val usernamePlaceholder = buildStringResource("Username") {
        IetfLang.Russian("Имя пользователя")
    }
    val passwordPlaceholder = buildStringResource("Password") {
        IetfLang.Russian("Пароль")
    }
    val serverAddressPlaceholder = buildStringResource("Server address (https://example.com)") {
        IetfLang.Russian("Адрес сервера (https://example.com)")
    }
    val loginButton = buildStringResource("Log in") {
        IetfLang.Russian("Войти")
    }
    val errorLoginFailed = buildStringResource("Login failed. Check your credentials and server address.") {
        IetfLang.Russian("Не удалось войти. Проверьте логин, пароль и адрес сервера.")
    }
}
