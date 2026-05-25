package project_group.project_name.client

import dev.inmo.micro_utils.startup.launcher.Config
import dev.inmo.micro_utils.startup.launcher.StartLauncherPlugin
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun main() {
    window.addEventListener("load", {
        CoroutineScope(Dispatchers.Default).launch {
            StartLauncherPlugin.start(
                Config(
                    listOf(
                        ClientJSPlugin,
                        project_group.project_name.features.common.common.JSPlugin,
                        project_group.project_name.features.common.client.JSPlugin,
                        project_group.project_name.features.sample.client.JSPlugin,
                        project_group.project_name.features.auth.client.JSPlugin,

                        project_group.project_name.features.ui.sample.JSPlugin,
                        project_group.project_name.features.ui.auth.JSPlugin,
                    )
                )
            )
        }
    })
}
