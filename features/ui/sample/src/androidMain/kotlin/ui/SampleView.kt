package project_group.project_name.features.ui.sample.ui

import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalResources
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import org.koin.core.parameter.parametersOf
import project_group.project_name.features.common.client.models.ViewConfig
import org.koin.core.component.inject
import project_group.project_name.features.ui.sample.SampleStrings

class SampleView(
    chain: NavigationChain<ViewConfig>,
    config: SampleViewConfig,
) : ComposeView<SampleViewConfig, ViewConfig, SampleViewModel>(config, chain) {
    override val viewModel: SampleViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) { parametersOf(this@SampleView) }

    @Composable
    override fun onDraw() {
        super.onDraw()
        Text(viewModel.textState.collectAsState().value)
    }
}