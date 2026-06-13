package dev.inmo.wishlist.features.ui.adminPanel

import dev.inmo.micro_utils.language_codes.IetfLang
import dev.inmo.micro_utils.strings.buildStringResource
import dev.inmo.wishlist.features.common.client.CommonStrings

/** Localized strings for all admin panel screens. */
object AdminPanelStrings {
    val title = buildStringResource("Admin Panel") { IetfLang.Russian("Панель администратора") }
    val usersSection = buildStringResource("Users") { IetfLang.Russian("Пользователи") }
    val wishlistsSection = buildStringResource("Wishlists") { IetfLang.Russian("Списки желаний") }
    val loading = buildStringResource("Loading...") { IetfLang.Russian("Загрузка...") }
    val backButton = buildStringResource("Back") { IetfLang.Russian("Назад") }
    val saveButton = CommonStrings.save
    val addButton = buildStringResource("Add") { IetfLang.Russian("Добавить") }
    val editButton = buildStringResource("Edit") { IetfLang.Russian("Изменить") }
    val deleteButton = buildStringResource("Delete") { IetfLang.Russian("Удалить") }
    val cancelButton = CommonStrings.cancel
    val confirmButton = buildStringResource("Confirm") { IetfLang.Russian("Подтвердить") }

    // Users
    val usersListTitle = buildStringResource("All Users") { IetfLang.Russian("Все пользователи") }
    val addUserButton = buildStringResource("Add User") { IetfLang.Russian("Добавить пользователя") }
    val emptyUsers = buildStringResource("No users found.") { IetfLang.Russian("Пользователи не найдены.") }
    val newUserTitle = buildStringResource("New User") { IetfLang.Russian("Новый пользователь") }
    val editUserTitle = buildStringResource("Edit User") { IetfLang.Russian("Изменить пользователя") }
    val usernameLabel = buildStringResource("Username") { IetfLang.Russian("Имя пользователя") }
    val passwordLabel = buildStringResource("Password") { IetfLang.Russian("Пароль") }
    val userWishlistsSection = buildStringResource("Wishlists") { IetfLang.Russian("Списки желаний") }
    val addWishlistForUserButton = buildStringResource("Add Wishlist") { IetfLang.Russian("Добавить список") }
    val noWishlistsForUser = buildStringResource("No wishlists.") { IetfLang.Russian("Списков нет.") }

    // Wishlists
    val wishlistsListTitle = buildStringResource("All Wishlists") { IetfLang.Russian("Все списки желаний") }
    val addWishlistButton = buildStringResource("Add Wishlist") { IetfLang.Russian("Добавить список желаний") }
    val emptyWishlists = buildStringResource("No wishlists found.") { IetfLang.Russian("Списки желаний не найдены.") }
    val newWishlistTitle = buildStringResource("New Wishlist") { IetfLang.Russian("Новый список желаний") }
    val editWishlistTitle = buildStringResource("Edit Wishlist") { IetfLang.Russian("Изменить список желаний") }
    val wishlistTitleLabel = buildStringResource("Title") { IetfLang.Russian("Название") }
    val ownerLabel = buildStringResource("Owner") { IetfLang.Russian("Владелец") }
    val selectOwner = buildStringResource("Select owner...") { IetfLang.Russian("Выберите владельца...") }
    val itemsSection = buildStringResource("Items") { IetfLang.Russian("Элементы") }
    val addItemButton = buildStringResource("Add Item") { IetfLang.Russian("Добавить элемент") }
    val emptyItems = buildStringResource("No items.") { IetfLang.Russian("Элементов нет.") }

    // Wishlist items
    val newItemTitle = buildStringResource("New Item") { IetfLang.Russian("Новый элемент") }
    val editItemTitle = buildStringResource("Edit Item") { IetfLang.Russian("Изменить элемент") }
    val itemTitleLabel = buildStringResource("Title") { IetfLang.Russian("Название") }
    val itemPriceLabel = buildStringResource("Approximate price") { IetfLang.Russian("Примерная цена") }
    val itemPriceUnitsLabel = buildStringResource("Currency/units") { IetfLang.Russian("Валюта/единицы") }
    val itemDescriptionLabel = buildStringResource("Description") { IetfLang.Russian("Описание") }

    // Discard confirm
    val confirmDiscardTitle = buildStringResource("Discard changes?") { IetfLang.Russian("Отменить изменения?") }
    val confirmDiscardMessage = buildStringResource("Unsaved changes will be lost.") { IetfLang.Russian("Несохранённые изменения будут потеряны.") }

    val ownerIdLabel = buildStringResource("Owner ID") { IetfLang.Russian("ID владельца") }
}
