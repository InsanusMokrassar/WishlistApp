package project_group.project_name.{{$module_package}}.ui

import androidx.compose.runtime.Composable
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import org.koin.core.parameter.parametersOf
import project_group.project_name.features.common.client.models.ViewConfig
import org.koin.core.component.inject

class {{$module_ui_name}}View(
    chain: NavigationChain<ViewConfig>,
    config: {{$module_ui_name}}ViewConfig,
) : ComposeView<{{$module_ui_name}}ViewConfig, ViewConfig, {{$module_ui_name}}ViewModel>(config, chain) {
    override val viewModel: {{$module_ui_name}}ViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) { parametersOf(this@{{$module_ui_name}}View) }

    @Composable
    override fun onDraw() {
        super.onDraw()
    }
}
