package dev.inmo.wishlist.features.ui.topBar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.compose.InjectNavigationChain
import dev.inmo.navigation.compose.InjectNavigationNode
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.auth.ui.AuthViewConfig
import dev.inmo.wishlist.features.ui.topBar.TopBarStrings
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * Android Compose-Material3 view for the top bar.
 *
 * Renders application title, the "change server URL" button, and embeds the
 * auth widget via a nested navigation chain pointing at [AuthViewConfig].
 */
class TopBarView(
    chain: NavigationChain<ViewConfig>,
    config: TopBarViewConfig,
) : ComposeView<TopBarViewConfig, ViewConfig, TopBarViewModel>(config, chain) {
    override val viewModel: TopBarViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@TopBarView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val resources = LocalResources.current
        val titleProviders by viewModel.titleProviders.collectAsState()
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                titleProviders
                    .map { it.title }
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(" / ")
                    ?: TopBarStrings.appTitle.translation(resources),
                style = MaterialTheme.typography.titleLarge
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { viewModel.onChangeServerUrl() }) {
                    Text(TopBarStrings.changeServerUrlButton.translation(resources))
                }
                InjectNavigationChain<ViewConfig> { InjectNavigationNode(AuthViewConfig()) }
            }
        }
    }
}
