package project_group.project_name.features.ui.sample.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.mvvm.ViewModel
import kotlinx.coroutines.flow.asStateFlow
import project_group.project_name.features.common.client.models.ViewConfig

class SampleViewModel(
    private val node: NavigationNode<SampleViewConfig, ViewConfig>,
    private val model: SampleModel
) : ViewModel<ViewConfig>(node) {
    private val _textState = MutableRedeliverStateFlow("Loading...")
    val textState = _textState.asStateFlow()

    init {
        scope.launchLoggingDropExceptions {
            val basePart = model.getSampleText()
            model.serverStatusFlow().collect {
                _textState.value = "$basePart\n\n - Server status answer: $it"
            }
        }
    }
}
