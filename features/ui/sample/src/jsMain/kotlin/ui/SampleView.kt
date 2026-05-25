package dev.inmo.wishlist.features.ui.sample.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import org.jetbrains.compose.web.dom.Text
import org.koin.core.parameter.parametersOf
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import org.koin.core.component.inject
import dev.inmo.wishlist.features.ui.sample.SampleStrings

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