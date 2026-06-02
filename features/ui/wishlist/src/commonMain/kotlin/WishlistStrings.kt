package dev.inmo.wishlist.features.ui.wishlist

import dev.inmo.micro_utils.language_codes.IetfLang
import dev.inmo.micro_utils.strings.buildStringResource
import dev.inmo.wishlist.features.common.client.CommonStrings
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistSortMode
import dev.inmo.wishlist.features.wishlist.common.models.Priority

/** Localized string resources for the wishlist UI feature. */
object WishlistStrings {
    val wishlistsTitle = buildStringResource("My Wishlists") { IetfLang.Russian("Мои списки желаний") }
    val backButton = buildStringResource("Back") { IetfLang.Russian("Назад") }
    val editButton = buildStringResource("Edit") { IetfLang.Russian("Редактировать") }
    val saveButton = CommonStrings.save
    val addItemButton = buildStringResource("Add Item") { IetfLang.Russian("Добавить товар") }
    val createWishlistButton = buildStringResource("New Wishlist") { IetfLang.Russian("Новый список") }
    val titleLabel = buildStringResource("Title") { IetfLang.Russian("Название") }
    val descriptionLabel = buildStringResource("Description") { IetfLang.Russian("Описание") }
    val priceLabel = buildStringResource("Approximate price") { IetfLang.Russian("Примерная цена") }
    val priceUnitsLabel = buildStringResource("Currency / Units") { IetfLang.Russian("Валюта / Единицы") }
    val linksLabel = buildStringResource("Links") { IetfLang.Russian("Ссылки") }
    val addLinkButton = buildStringResource("Add link") { IetfLang.Russian("Добавить ссылку") }
    val newLinkPlaceholder = buildStringResource("https://...") { IetfLang.Russian("https://...") }
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
