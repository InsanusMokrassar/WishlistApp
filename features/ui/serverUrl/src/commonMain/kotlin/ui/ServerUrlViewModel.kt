package dev.inmo.wishlist.features.ui.serverUrl.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the server URL editor screen.
 *
 * Holds the single `urlState` text input and a `loadingState`. Reads the saved
 * URL on init and prefills the field; on save delegates to [interactor].
 *
 * @param node Navigation node hosting this ViewModel.
 * @param model Persistence facade.
 * @param interactor Navigation delegate invoked after a successful save.
 */
class ServerUrlViewModel(
    private val node: NavigationNode<ServerUrlViewConfig, ViewConfig>,
    private val model: ServerUrlModel,
    private val interactor: ServerUrlViewInteractor
) : ViewModel<ViewConfig>(node) {
    private val _urlState = MutableRedeliverStateFlow("")
    val urlState = _urlState.asStateFlow()

    private val _loadingState = MutableRedeliverStateFlow(false)
    val loadingState = _loadingState.asStateFlow()

    /** `true` when the input is non-blank AND no save is in flight. */
    val saveEnabledState: StateFlow<Boolean> = combine(
        _urlState,
        _loadingState
    ) { url, loading ->
        !loading && url.isNotBlank()
    }.stateIn(scope, SharingStarted.Eagerly, false)

    init {
        scope.launchLoggingDropExceptions {
            _urlState.value = model.getServerUrl().orEmpty()
        }
    }

    /** Handles edits to the URL input field. */
    fun onUrlChanged(input: String) {
        _urlState.value = input
    }

    /** Persists the current URL and triggers [ServerUrlViewInteractor.onSaved]. */
    fun onSave() {
        scope.launchLoggingDropExceptions {
            val value = _urlState.value.trim()
            if (value.isBlank()) return@launchLoggingDropExceptions
            _loadingState.value = true
            try {
                model.saveServerUrl(value)
                interactor.onSaved(node)
            } finally {
                _loadingState.value = false
            }
        }
    }
}
