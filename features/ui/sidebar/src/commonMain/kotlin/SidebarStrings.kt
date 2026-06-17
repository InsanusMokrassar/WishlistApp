package dev.inmo.wishlist.features.ui.sidebar

import dev.inmo.micro_utils.language_codes.IetfLang
import dev.inmo.micro_utils.strings.buildStringResource

/** Localized strings for the Calm Studio sidebar. */
object SidebarStrings {
    /** Word-mark shown next to the brand glyph at the top of the sidebar. */
    val brand = buildStringResource("wishlist") {
        IetfLang.Russian("желания")
    }

    /** Primary item: the caller's own wishlists. */
    val myLists = buildStringResource("My Lists") {
        IetfLang.Russian("Мои списки")
    }

    /** Primary item: browse other people. */
    val discover = buildStringResource("Discover") {
        IetfLang.Russian("Обзор")
    }

    /** Primary item: the caller's reserved gifts. */
    val reserved = buildStringResource("Reserved") {
        IetfLang.Russian("Брони")
    }

    /** Primary item: account settings. */
    val settings = buildStringResource("Settings") {
        IetfLang.Russian("Настройки")
    }

    /** Section label above the caller's pinned lists. */
    val yourLists = buildStringResource("Your lists") {
        IetfLang.Russian("Ваши списки")
    }

    /** Affordance that opens the create-wishlist form. */
    val newList = buildStringResource("New list") {
        IetfLang.Russian("Новый список")
    }

    /** Secondary caption under the caller's name in the profile row. */
    val viewProfile = buildStringResource("View profile") {
        IetfLang.Russian("Открыть профиль")
    }
}
