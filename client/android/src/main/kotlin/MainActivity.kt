package project_group.project_name.client

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import dev.inmo.micro_utils.startup.launcher.Config
import dev.inmo.micro_utils.startup.launcher.StartLauncherPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val clientAndroidPlugin = ClientAndroidPlugin(this)

    private val scope = CoroutineScope(Dispatchers.Default)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scope.launch {
            StartLauncherPlugin.start(
                Config(
                    listOf(
                        clientAndroidPlugin,
                        project_group.project_name.features.common.common.AndroidPlugin,
                        project_group.project_name.features.common.client.AndroidPlugin,
                        project_group.project_name.features.sample.client.AndroidPlugin,
                        project_group.project_name.features.auth.client.AndroidPlugin,

                        project_group.project_name.features.ui.sample.AndroidPlugin,
                        project_group.project_name.features.ui.auth.AndroidPlugin,
                    )
                )
            )
        }

        setContent {
            ClientPlugin.currentDrawingBlock.value()
        }
    }
}
