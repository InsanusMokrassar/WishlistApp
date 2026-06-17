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
    val myPresentsBooksTitle = buildStringResource("My presents") { IetfLang.Russian("Мои подарки") }

    /** Placeholder shown when the caller has booked no items. */
    val emptyPresents = buildStringResource("You have not booked anything yet") {
        IetfLang.Russian("Вы пока ничего не забронировали")
    }

    // --- Calm Studio web copy ---
    // First-class "reserve a gift" voice used by the JS (Calm Studio) views only; the JVM/Android
    // booking views keep the strings above. Kept separate so the redesign copy does not change the
    // text rendered by the untouched Material clients.

    /** Calm Studio button that reserves the visited item as a gift. */
    val reserveGiftButton = buildStringResource("Reserve this gift") {
        IetfLang.Russian("Забронировать подарок")
    }

    /** Calm Studio button that cancels the caller's own reservation. */
    val cancelReservationButton = buildStringResource("Cancel reservation") {
        IetfLang.Russian("Отменить бронь")
    }

    /** Calm Studio status pill: the caller has reserved this item. */
    val reservedByYouLabel = buildStringResource("Reserved by you") {
        IetfLang.Russian("Забронировано вами")
    }

    /** Calm Studio status pill: another (anonymous) user has reserved this item. */
    val reservedBySomeoneLabel = buildStringResource("Reserved by someone") {
        IetfLang.Russian("Кто-то уже забронировал")
    }

    /** Title of the Calm Studio "Reserved" section (the caller's reserved gifts). */
    val reservedTitle = buildStringResource("Reserved") {
        IetfLang.Russian("Брони")
    }

    /** Subtitle of the Calm Studio "Reserved" section. */
    val reservedSubline = buildStringResource("Gifts you've committed to give. Only you can see these.") {
        IetfLang.Russian("Подарки, которые вы вызвались подарить. Их видите только вы.")
    }

    /** Calm Studio empty-state heading for the "Reserved" section. */
    val reservedEmptyTitle = buildStringResource("No reservations yet") {
        IetfLang.Russian("Пока нет броней")
    }

    /** Calm Studio empty-state body for the "Reserved" section. */
    val reservedEmptyBody = buildStringResource("When you reserve a gift on someone's list, it shows up here.") {
        IetfLang.Russian("Когда вы забронируете подарок в чьём-то списке, он появится здесь.")
    }

    /** Calm Studio corner flag marking a card on the "Reserved" section. */
    val reservedFlag = buildStringResource("Reserved") {
        IetfLang.Russian("Бронь")
    }
}
