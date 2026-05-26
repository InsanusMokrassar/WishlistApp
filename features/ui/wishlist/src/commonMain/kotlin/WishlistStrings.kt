package dev.inmo.wishlist.features.ui.wishlist

import dev.inmo.micro_utils.language_codes.IetfLang
import dev.inmo.micro_utils.strings.buildStringResource

/** Localized string resources for the wishlist UI feature. */
object WishlistStrings {
    val wishlistsTitle = buildStringResource("My Wishlists") { IetfLang.Russian("Мои списки желаний") }
    val backButton = buildStringResource("Back") { IetfLang.Russian("Назад") }
    val editButton = buildStringResource("Edit") { IetfLang.Russian("Редактировать") }
    val saveButton = buildStringResource("Save") { IetfLang.Russian("Сохранить") }
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
    val cancelButton = buildStringResource("Cancel") { IetfLang.Russian("Отмена") }
    val emptyWishlists = buildStringResource("No wishlists yet") { IetfLang.Russian("Нет списков желаний") }
    val emptyItems = buildStringResource("No items yet") { IetfLang.Russian("Нет товаров") }
    val loading = buildStringResource("Loading...") { IetfLang.Russian("Загрузка...") }
    val newWishlistTitle = buildStringResource("New Wishlist") { IetfLang.Russian("Новый список" ) }
    val editWishlistTitle = buildStringResource("Edit Wishlist") { IetfLang.Russian("Редактировать список") }
    val newItemTitle = buildStringResource("New Item") { IetfLang.Russian("Новый товар") }
    val editItemTitle = buildStringResource("Edit Item") { IetfLang.Russian("Редактировать товар") }
}
