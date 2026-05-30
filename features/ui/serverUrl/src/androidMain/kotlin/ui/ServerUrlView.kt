package dev.inmo.wishlist.features.ui.serverUrl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.serverUrl.ServerUrlStrings
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** Android Compose-Material3 view for the server URL editor screen. */
class ServerUrlView(
    chain: NavigationChain<ViewConfig>,
    config: ServerUrlViewConfig,
) : ComposeView<ServerUrlViewConfig, ViewConfig, ServerUrlViewModel>(config, chain) {
    override val viewModel: ServerUrlViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@ServerUrlView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val resources = LocalResources.current
        val url by viewModel.urlState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val saveEnabled by viewModel.saveEnabledState.collectAsState()

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(ServerUrlStrings.title.translation(resources), style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(
                value = url,
                onValueChange = { viewModel.onUrlChanged(it) },
                placeholder = { Text(ServerUrlStrings.urlPlaceholder.translation(resources)) },
                singleLine = true,
                enabled = !loading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { viewModel.onSave() },
                enabled = saveEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(ServerUrlStrings.saveButton.translation(resources))
            }
        }
    }
}
