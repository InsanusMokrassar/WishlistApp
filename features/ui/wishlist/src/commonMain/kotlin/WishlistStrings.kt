package dev.inmo.wishlist.features.ui.wishlist

import dev.inmo.micro_utils.language_codes.IetfLang
import dev.inmo.micro_utils.strings.buildStringResource
import dev.inmo.wishlist.features.common.client.CommonStrings
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistSortMode
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistViewMode
import dev.inmo.wishlist.features.wishlist.common.models.Priority

/** Localized string resources for the wishlist UI feature. */
object WishlistStrings {
    val wishlistsTitle = buildStringResource("My Wishlists") { IetfLang.Russian("Мои списки желаний") }
    val backButton = buildStringResource("Back") { IetfLang.Russian("Назад") }

    /**
     * Contextual Back-button label on the all-items screen, naming its parent: the global users list.
     * Used because that parent is identified by a static title rather than a per-entity name.
     */
    val usersListBackLabel = buildStringResource("Users") { IetfLang.Russian("Пользователи") }
    val editButton = buildStringResource("Edit") { IetfLang.Russian("Редактировать") }
    val shareButton = buildStringResource("Share") { IetfLang.Russian("Поделиться") }
    val saveButton = CommonStrings.save
    val addItemButton = buildStringResource("Add Item") { IetfLang.Russian("Добавить товар") }
    val createWishlistButton = buildStringResource("New Wishlist") { IetfLang.Russian("Новый список") }
    val titleLabel = buildStringResource("Title") { IetfLang.Russian("Название") }
    val descriptionLabel = buildStringResource("Description") { IetfLang.Russian("Описание") }
    val amountLabel = buildStringResource("Amount") { IetfLang.Russian("Количество") }
    val priceLabel = buildStringResource("Approximate price") { IetfLang.Russian("Примерная цена") }
    val priceUnitsLabel = buildStringResource("Currency / Units") { IetfLang.Russian("Валюта / Единицы") }
    val defaultCurrencyLabel = buildStringResource("Default currency") { IetfLang.Russian("Валюта по умолчанию") }
    val linksLabel = buildStringResource("Links") { IetfLang.Russian("Ссылки") }
    val addLinkButton = buildStringResource("Add link") { IetfLang.Russian("Добавить ссылку") }
    val newLinkPlaceholder = buildStringResource("https://...") { IetfLang.Russian("https://...") }
    val linkTitlePlaceholder = buildStringResource("Title (optional)") { IetfLang.Russian("Название (необязательно)") }
    val duplicateLinksHint = buildStringResource("Remove duplicate links to save") {
        IetfLang.Russian("Удалите повторяющиеся ссылки, чтобы сохранить")
    }
    val confirmDiscardTitle = buildStringResource("Discard changes?") { IetfLang.Russian("Отменить изменения?") }
    val confirmDiscardMessage = buildStringResource("You have unsaved changes. Discard and go back?") {
        IetfLang.Russian("Есть несохранённые изменения. Отменить и вернуться?")
    }
    val confirmButton = buildStringResource("Discard") { IetfLang.Russian("Отменить") }
    val cancelButton = CommonStrings.cancel
    val emptyWishlists = buildStringResource("No wishlists yet") { IetfLang.Russian("Нет списков желаний") }
    val userWishlistsTitle = buildStringResource("Wishlists") { IetfLang.Russian("Списки желаний") }
    val userWishlistsTitleFormat = buildStringResource("{name}'s Wishlists") {
        IetfLang.Russian("Списки желаний {name}")
    }
    val userWishesTitleFormat = buildStringResource("{name}'s wishes") {
        IetfLang.Russian("Желания {name}")
    }
    val allItemsButton = buildStringResource("All items") { IetfLang.Russian("Все товары") }
    val allItemsTitle = buildStringResource("All items") { IetfLang.Russian("Все товары") }
    val openWishlistButton = buildStringResource("Open") { IetfLang.Russian("Открыть") }
    val profileButton = buildStringResource("Profile") { IetfLang.Russian("Профиль") }
    val emptyItems = buildStringResource("No items yet") { IetfLang.Russian("Нет товаров") }
    val sortLabel = buildStringResource("Sort") { IetfLang.Russian("Сортировка") }
    val sortNone = buildStringResource("Grouped") { IetfLang.Russian("По спискам") }
    val sortDefault = buildStringResource("Default") { IetfLang.Russian("По умолчанию") }
    val sortCost = buildStringResource("Cost") { IetfLang.Russian("Цена") }
    val sortPriority = buildStringResource("Priority") { IetfLang.Russian("Приоритет") }
    val sortTitle = buildStringResource("Title") { IetfLang.Russian("Название") }
    val currencyLabel = buildStringResource("Currency") { IetfLang.Russian("Валюта") }
    val currencyOriginal = buildStringResource("Original") { IetfLang.Russian("Исходная") }
    val viewModeLabel = buildStringResource("View") { IetfLang.Russian("Вид") }
    val viewModeList = buildStringResource("List") { IetfLang.Russian("Список") }
    val viewModeGrid = buildStringResource("Grid") { IetfLang.Russian("Плитка") }
    val loading = buildStringResource("Loading...") { IetfLang.Russian("Загрузка...") }
    val editWishlistTitle = buildStringResource("Edit Wishlist") { IetfLang.Russian("Редактировать список") }
    val newItemTitle = buildStringResource("New Item") { IetfLang.Russian("Новый товар") }
    val editItemTitle = buildStringResource("Edit Item") { IetfLang.Russian("Редактировать товар") }
    val viewItemTitle = buildStringResource("Item") { IetfLang.Russian("Товар") }
    val priorityLabel = buildStringResource("Priority") { IetfLang.Russian("Приоритет") }
    val prioritySmall = buildStringResource("Low") { IetfLang.Russian("Низкий") }
    val priorityMedium = buildStringResource("Medium") { IetfLang.Russian("Средний") }
    val priorityHigh = buildStringResource("High") { IetfLang.Russian("Высокий") }
    val priorityCustom = buildStringResource("Custom") { IetfLang.Russian("Свой") }
    val priorityCustomWeightLabel = buildStringResource("Weight") { IetfLang.Russian("Вес") }
    val priorityHelp = buildStringResource("High-priority items are shown first to gift-givers.") {
        IetfLang.Russian("Товары с высоким приоритетом показываются дарителям первыми.")
    }
    val noPrice = buildStringResource("No price") { IetfLang.Russian("Цена не указана") }
    val noLinks = buildStringResource("No links") { IetfLang.Russian("Ссылки не указаны") }
    val imagesLabel = buildStringResource("Images") { IetfLang.Russian("Изображения") }
    val addImageButton = buildStringResource("Add image") { IetfLang.Russian("Добавить изображение") }
    val removeImageButton = buildStringResource("Remove image") { IetfLang.Russian("Удалить изображение") }
    val uploadingImage = buildStringResource("Uploading image...") { IetfLang.Russian("Загрузка изображения...") }
    val noImages = buildStringResource("No images") { IetfLang.Russian("Нет изображений") }
    val deleteButton = buildStringResource("Delete") { IetfLang.Russian("Удалить") }
    val confirmDeleteButton = buildStringResource("Delete") { IetfLang.Russian("Удалить") }
    val confirmDeleteItemTitle = buildStringResource("Delete item?") { IetfLang.Russian("Удалить товар?") }
    val confirmDeleteItemMessage = buildStringResource("This item will be permanently removed. Continue?") {
        IetfLang.Russian("Этот товар будет удалён безвозвратно. Продолжить?")
    }
    val confirmDeleteWishlistTitle = buildStringResource("Delete wishlist?") { IetfLang.Russian("Удалить список?") }
    val confirmDeleteWishlistMessage = buildStringResource("This wishlist and all its items will be permanently removed. Continue?") {
        IetfLang.Russian("Этот список и все его товары будут удалены безвозвратно. Продолжить?")
    }
    val copyItemButton = buildStringResource("Copy to my wishlist") { IetfLang.Russian("Скопировать в мой список") }
    val copyWishlistButton = buildStringResource("Copy to my profile") { IetfLang.Russian("Скопировать в мой профиль") }
    val copyTargetTitle = buildStringResource("Copy item to…") { IetfLang.Russian("Скопировать товар в…") }
    val copySelectTargetPrompt = buildStringResource("Select a target wishlist") { IetfLang.Russian("Выберите список-получатель") }
    val copyNoTargets = buildStringResource("You have no wishlists yet. Create one first.") {
        IetfLang.Russian("У вас пока нет списков. Сначала создайте список.")
    }
    val copyQueued = buildStringResource("Copy queued. It will appear in your profile shortly.") {
        IetfLang.Russian("Копирование поставлено в очередь. Скоро появится в вашем профиле.")
    }
    val copyFailed = buildStringResource("Copy failed. Please try again.") {
        IetfLang.Russian("Не удалось скопировать. Попробуйте ещё раз.")
    }

    /** Calm Studio toast shown after the Share action copies the list link to the clipboard. */
    val shareLinkCopiedToast = buildStringResource("Link copied to clipboard") {
        IetfLang.Russian("Ссылка скопирована в буфер обмена")
    }

    /** Accessibility text for the default gift-box placeholder shown when an item has no image. */
    val itemImagePlaceholderAlt = buildStringResource("Gift placeholder") {
        IetfLang.Russian("Заполнитель подарка")
    }

    /** Accessibility text for the default stacked-items placeholder shown for a wishlist. */
    val wishlistImagePlaceholderAlt = buildStringResource("Wishlist placeholder") {
        IetfLang.Russian("Заполнитель списка желаний")
    }
}

/**
 * Maps a [Priority] to the matching localized label resource.
 *
 * @return The preset's localized name, or the generic "Custom" label for [Priority.Custom].
 */
fun Priority.labelResource() = when (this) {
    Priority.Small -> WishlistStrings.prioritySmall
    Priority.Medium -> WishlistStrings.priorityMedium
    Priority.High -> WishlistStrings.priorityHigh
    is Priority.Custom -> WishlistStrings.priorityCustom
}

/**
 * Weight suffix shown after the [Priority.labelResource] label, e.g. `" (42)"` for a
 * [Priority.Custom] with an arbitrary weight; empty for the named presets whose label already
 * conveys their importance.
 *
 * @return `" (<weight>)"` for [Priority.Custom], otherwise an empty string.
 */
fun Priority.weightSuffix(): String = if (this is Priority.Custom) " ($weight)" else ""

/**
 * Maps a [WishlistSortMode] to the matching localized label resource shown in the sort selector.
 *
 * @return The localized name of the sort mode.
 */
fun WishlistSortMode.labelResource() = when (this) {
    WishlistSortMode.None -> WishlistStrings.sortNone
    WishlistSortMode.Cost -> WishlistStrings.sortCost
    WishlistSortMode.Priority -> WishlistStrings.sortPriority
    WishlistSortMode.Title -> WishlistStrings.sortTitle
}

/**
 * Maps a [WishlistViewMode] to the matching localized label resource shown in the view-mode selector.
 *
 * @return The localized name of the view mode.
 */
fun WishlistViewMode.labelResource() = when (this) {
    WishlistViewMode.List -> WishlistStrings.viewModeList
    WishlistViewMode.Grid -> WishlistStrings.viewModeGrid
}
