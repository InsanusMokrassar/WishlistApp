package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.onResumeFlow
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.micro_utils.common.MPPFile
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.utils.subscribeOnLoggedOut
import dev.inmo.wishlist.features.common.common.models.Amount
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.Priority
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemLink
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.takeWhile

/**
 * ViewModel for the wishlist item create/edit screen.
 *
 * When [WishlistItemEditViewConfig.wishlistItemId] is null, operates in create mode.
 * When non-null, loads the existing item and pre-fills all fields.
 *
 * [priceState] holds the price as a decimal string (e.g. `"19.99"`). On save it is parsed
 * with [Amount.invoke(Double)] and stored as `null` when blank or not a valid number.
 *
 * Back navigation shows a discard-changes confirmation modal when [isDirtyState] is `true`.
 * Navigation side-effects are delegated to [interactor].
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Wishlist data source.
 * @param interactor Navigation delegate for this screen.
 */
class WishlistItemEditViewModel(
    private val node: NavigationNode<WishlistItemEditViewConfig, ViewConfig>,
    private val model: WishlistsModel,
    private val interactor: WishlistItemEditViewInteractor
) : ViewModel<ViewConfig>(node) {
    /** `true` when this screen is in create mode (no existing item id). */
    val isCreating: Boolean = node.config.wishlistItemId == null

    private val _titleState = MutableRedeliverStateFlow("")

    /** Current value of the title input field. */
    val titleState = _titleState.asStateFlow()

    private val _backLabelState = MutableRedeliverStateFlow<String?>(null)

    /**
     * Label for the contextual Back button. In EDIT mode Back replaces this screen with the item's
     * read view, so the label is that item's title; in CREATE mode Back replaces it with the
     * containing wishlist's detail screen, so the label is the wishlist's title. Both are resolved
     * when the form's initial data loads. `null` until resolved — the view then falls back to the
     * generic back string.
     */
    val backLabelState = _backLabelState.asStateFlow()

    private val _descriptionState = MutableRedeliverStateFlow("")

    /** Current value of the description input field. */
    val descriptionState = _descriptionState.asStateFlow()

    private val _amountState = MutableRedeliverStateFlow("1")

    /** Current desired quantity as a string; always represents a value `>= 1`. */
    val amountState = _amountState.asStateFlow()

    private val _priceState = MutableRedeliverStateFlow("")

    /** Current price as decimal string; blank means no price. */
    val priceState = _priceState.asStateFlow()

    private val _priceUnitsState = MutableRedeliverStateFlow("")

    /** Current currency or units label. */
    val priceUnitsState = _priceUnitsState.asStateFlow()

    private val _priorityState = MutableRedeliverStateFlow<Priority>(Priority.Medium)

    /** Currently selected item [Priority]; defaults to [Priority.Medium]. */
    val priorityState = _priorityState.asStateFlow()

    private val _linksState = MutableRedeliverStateFlow<List<WishlistItemLink>>(emptyList())

    /** Current list of external links (each with a url and optional title). */
    val linksState = _linksState.asStateFlow()

    private val _newLinkState = MutableRedeliverStateFlow("")

    /** Url currently typed in the "add link" input field. */
    val newLinkState = _newLinkState.asStateFlow()

    private val _newLinkTitleState = MutableRedeliverStateFlow("")

    /** Optional title currently typed in the "add link" title input field. */
    val newLinkTitleState = _newLinkTitleState.asStateFlow()

    private val _imageIdsState = MutableRedeliverStateFlow<List<FileId>>(emptyList())

    /** Ids of images attached to the item, in display order. */
    val imageIdsState = _imageIdsState.asStateFlow()

    private val _uploadingImageState = MutableRedeliverStateFlow(false)

    /** `true` while an image upload is in flight. */
    val uploadingImageState = _uploadingImageState.asStateFlow()

    private val _isDirtyState = MutableRedeliverStateFlow(false)

    /** `true` when any field has been modified since the screen was opened. */
    val isDirtyState = _isDirtyState.asStateFlow()

    private val _loadingState = MutableRedeliverStateFlow(false)

    /** `true` while a network request is in flight. */
    val loadingState = _loadingState.asStateFlow()

    private val _showConfirmDialogState = MutableRedeliverStateFlow(false)

    /** `true` when the discard-changes confirmation dialog should be visible. */
    val showConfirmDialogState = _showConfirmDialogState.asStateFlow()

    private val _showDeleteDialogState = MutableRedeliverStateFlow(false)

    /** `true` when the delete-item confirmation dialog should be visible. */
    val showDeleteDialogState = _showDeleteDialogState.asStateFlow()

    /** `true` when this screen edits an existing item and may therefore offer deletion. */
    val canDelete: Boolean = !isCreating

    init {
        var inited = false
        merge(flowOf(Unit), node.onResumeFlow).takeWhile { inited == false }.subscribeLoggingDropExceptions(scope) {
            node.config.wishlistItemId?.let { itemId ->
                _loadingState.value = true
                try {
                    val item = model.getWishlistItems(node.config.wishlistId).find { it.id == itemId }
                    if (item != null) {
                        _titleState.value = item.title
                        _backLabelState.value = item.title
                        _amountState.value = item.amount.toString()
                        _descriptionState.value = item.description
                        _priceState.value = item.approximatePrice?.toString() ?: ""
                        _priceUnitsState.value = item.priceUnits
                        _linksState.value = item.links
                        _priorityState.value = item.priority
                        _imageIdsState.value = item.imageIds
                    }
                } finally {
                    _loadingState.value = false
                }
            } ?: run {
                // Create mode: pre-fill the currency field with the wishlist's default units.
                _loadingState.value = true
                try {
                    val wishlist = model.getWishlist(node.config.wishlistId)
                    if (wishlist != null) {
                        if (_priceUnitsState.value.isBlank()) {
                            _priceUnitsState.value = wishlist.defaultPriceUnits
                        }
                        _backLabelState.value = wishlist.title
                    }
                } finally {
                    _loadingState.value = false
                }
            }
            inited = true
        }
        // On logout this editor must exit, replacing itself with its non-edit view: the item's read
        // view in EDIT mode, the containing wishlist in CREATE mode (see onNavigateBackToParent).
        model.currentUserIdFlow.subscribeOnLoggedOut(scope) {
            interactor.onNavigateBackToParent(node)
        }
    }

    /** @param v New title value. */
    fun onTitleChanged(v: String) { _titleState.value = v; _isDirtyState.value = true }

    /** @param v New description value. */
    fun onDescriptionChanged(v: String) { _descriptionState.value = v; _isDirtyState.value = true }

    /**
     * Updates the desired quantity. Keeps the raw text while the user is typing but never
     * stores a value below `1`: an empty/zero/negative/non-numeric input is normalized to `"1"`
     * only when it is not in the middle of a valid edit. The canonical clamp to `>= 1` is also
     * applied in [onSave].
     *
     * @param v New amount text typed by the user.
     */
    fun onAmountChanged(v: String) {
        val trimmed = v.trim()
        _amountState.value = when {
            trimmed.isEmpty() -> ""
            else -> (trimmed.toUIntOrNull()?.coerceAtLeast(1u)?.toString()) ?: _amountState.value
        }
        _isDirtyState.value = true
    }

    /** @param v New price string (decimal notation). */
    fun onPriceChanged(v: String) { _priceState.value = v; _isDirtyState.value = true }

    /** @param v New currency/units label. */
    fun onPriceUnitsChanged(v: String) { _priceUnitsState.value = v; _isDirtyState.value = true }

    /**
     * Selects a preset [Priority] (preserving any [Priority.Custom] passed in).
     *
     * @param priority New priority value.
     */
    fun onPrioritySelected(priority: Priority) { _priorityState.value = priority; _isDirtyState.value = true }

    /**
     * Switches priority to [Priority.Custom] with the weight parsed from [v].
     * Non-numeric or blank input is treated as weight `0`.
     *
     * @param v New custom weight as a decimal string.
     */
    fun onCustomWeightChanged(v: String) {
        _priorityState.value = Priority.Custom(v.trim().toUIntOrNull() ?: 0u)
        _isDirtyState.value = true
    }

    /** @param v Url typed in the new-link url input. */
    fun onNewLinkChanged(v: String) { _newLinkState.value = v }

    /** @param v Optional title typed in the new-link title input. */
    fun onNewLinkTitleChanged(v: String) { _newLinkTitleState.value = v }

    /**
     * Appends a [WishlistItemLink] built from [newLinkState] (url) and [newLinkTitleState] (optional title)
     * to the links list, then clears both inputs. No-op when the url is blank; a blank title is stored as `null`.
     */
    fun onAddLink() {
        val url = _newLinkState.value.trim()
        if (url.isNotBlank()) {
            val title = _newLinkTitleState.value.trim().takeIf { it.isNotBlank() }
            _linksState.value = _linksState.value + WishlistItemLink(url = url, title = title)
            _newLinkState.value = ""
            _newLinkTitleState.value = ""
            _isDirtyState.value = true
        }
    }

    /**
     * Removes the link at [index] from the links list.
     *
     * @param index Zero-based index of the link to remove.
     */
    fun onRemoveLink(index: Int) {
        _linksState.value = _linksState.value.toMutableList().also { it.removeAt(index) }
        _isDirtyState.value = true
    }

    /**
     * Uploads [file] as an image and appends the resulting [FileId] to [imageIdsState] on success.
     * Toggles [uploadingImageState] for the duration; failures are logged and ignored.
     *
     * @param file Image file chosen by the user.
     */
    fun onAddImage(file: MPPFile) {
        scope.launchLoggingDropExceptions {
            _uploadingImageState.value = true
            try {
                val id = model.uploadImage(file)
                if (id != null) {
                    _imageIdsState.value = _imageIdsState.value + id
                    _isDirtyState.value = true
                }
            } finally {
                _uploadingImageState.value = false
            }
        }
    }

    /**
     * Removes the image at [index] from the attached images list.
     *
     * @param index Zero-based index of the image to remove.
     */
    fun onRemoveImage(index: Int) {
        _imageIdsState.value = _imageIdsState.value.toMutableList().also { it.removeAt(index) }
        _isDirtyState.value = true
    }

    /**
     * Builds the download URL for an attached image so the view can render it.
     *
     * @param id Image identifier.
     * @return Relative download URL.
     */
    fun imageUrl(id: FileId): String = model.imageUrl(id)

    /**
     * Downloads the raw bytes of an attached image (for platforms that decode images locally).
     *
     * @param id Image identifier.
     * @return Payload bytes, or `null` on failure.
     */
    suspend fun loadImageBytes(id: FileId): ByteArray? = model.loadImageBytes(id)

    /**
     * Attempts to navigate back. Shows confirm dialog when [isDirtyState] is `true`, otherwise
     * navigates to the logical parent via [WishlistItemEditViewInteractor.onNavigateBackToParent]
     * (the item's read view in EDIT mode, the containing wishlist in CREATE mode).
     */
    fun onBack() {
        if (_isDirtyState.value) {
            _showConfirmDialogState.value = true
        } else {
            scope.launchLoggingDropExceptions { interactor.onNavigateBackToParent(node) }
        }
    }

    /** Confirms discarding changes and navigates to the logical parent (see [onBack]). */
    fun onConfirmBack() {
        _showConfirmDialogState.value = false
        scope.launchLoggingDropExceptions { interactor.onNavigateBackToParent(node) }
    }

    /** Cancels the confirm dialog, returning the user to the form. */
    fun onCancelBack() {
        _showConfirmDialogState.value = false
    }

    /** Requests item deletion: shows the delete confirmation dialog. No-op in create mode. */
    fun onDelete() {
        if (!canDelete) return
        _showDeleteDialogState.value = true
    }

    /**
     * Confirms deletion: removes the item via the model, then navigates back exactly as a
     * plain "back" would (delegates to [WishlistItemEditViewInteractor.onNavigateBack]).
     */
    fun onConfirmDelete() {
        val itemId = node.config.wishlistItemId ?: run {
            _showDeleteDialogState.value = false
            return
        }
        scope.launchLoggingDropExceptions {
            _showDeleteDialogState.value = false
            _loadingState.value = true
            try {
                model.deleteWishlistItem(itemId)
            } finally {
                _loadingState.value = false
            }
            interactor.onNavigateBack(node)
        }
    }

    /** Cancels the delete dialog, returning the user to the form. */
    fun onCancelDelete() {
        _showDeleteDialogState.value = false
    }

    /**
     * Saves the item (create or update) and delegates to [WishlistItemEditViewInteractor.onSaved] on success.
     * No-op when [titleState] is blank or a request is already in flight.
     */
    fun onSave() {
        scope.launchLoggingDropExceptions {
            val title = _titleState.value.trim()
            if (title.isBlank()) return@launchLoggingDropExceptions
            val links = _linksState.value
            if (links.size != links.distinctBy { it.url.trim() }.size) return@launchLoggingDropExceptions
            _loadingState.value = true
            try {
                val price = _priceState.value.trim().toDoubleOrNull()?.let { Amount(it) }
                val amount = _amountState.value.trim().toUIntOrNull()?.coerceAtLeast(1u) ?: 1u
                val item = NewWishlistItem(
                    wishlistId = node.config.wishlistId,
                    title = title,
                    amount = amount,
                    description = _descriptionState.value.trim(),
                    priceUnits = _priceUnitsState.value.trim(),
                    approximatePrice = price,
                    links = _linksState.value,
                    priority = _priorityState.value,
                    imageIds = _imageIdsState.value
                )
                val itemId = node.config.wishlistItemId
                if (itemId == null) {
                    model.createWishlistItem(item)
                } else {
                    model.updateWishlistItem(itemId, item)
                }
                interactor.onSaved(node)
            } finally {
                _loadingState.value = false
            }
        }
    }
}
