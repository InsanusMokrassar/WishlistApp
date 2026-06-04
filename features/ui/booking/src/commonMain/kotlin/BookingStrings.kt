package dev.inmo.wishlist.features.ui.booking

import dev.inmo.micro_utils.language_codes.IetfLang
import dev.inmo.micro_utils.strings.buildStringResource

/** Localized string resources for the booking UI scenario (book-item view + my-presents view). */
object BookingStrings {
    /** Section / screen title for the gift-booking view. */
    val bookingLabel = buildStringResource("Gift booking") { IetfLang.Russian("Бронь подарка") }

    /** Button that reserves the current item for gifting. */
    val bookButton = buildStringResource("Book for gifting") { IetfLang.Russian("Забронировать") }

    /** Button that cancels the caller's own reservation. */
    val cancelBookingButton = buildStringResource("Cancel booking") { IetfLang.Russian("Отменить бронь") }

    /** Message shown when the current caller has booked the item. */
    val bookedByYou = buildStringResource("You booked this item") { IetfLang.Russian("Вы забронировали этот товар") }

    /** Message shown when another (anonymous) user has booked the item. */
    val bookedByOther = buildStringResource("Someone is going to gift this") {
        IetfLang.Russian("Кто-то собирается подарить это")
    }

    /** Message shown when the item is not yet booked. */
    val notBooked = buildStringResource("Nobody booked this yet") { IetfLang.Russian("Этот товар ещё не забронирован") }

    /** Back-button label. */
    val backButton = buildStringResource("Back") { IetfLang.Russian("Назад") }

    /** Loading placeholder. */
    val loading = buildStringResource("Loading...") { IetfLang.Russian("Загрузка...") }

    /** Title of the my-presents (booked items) screen. */
    val myPresentsTitle = buildStringResource("My presents") { IetfLang.Russian("Мои подарки") }

    /** Placeholder shown when the caller has booked no items. */
    val emptyPresents = buildStringResource("You have not booked anything yet") {
        IetfLang.Russian("Вы пока ничего не забронировали")
    }
}
