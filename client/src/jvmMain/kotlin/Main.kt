package project_group.project_name.client

import dev.inmo.micro_utils.startup.launcher.Config
import dev.inmo.micro_utils.startup.launcher.StartLauncherPlugin
import kotlinx.coroutines.Job

suspend fun main() {
    val appJob = Job()
    StartLauncherPlugin.start(
        Config(
            listOf(
                ClientJVMPlugin(appJob),
                project_group.project_name.features.common.common.JVMPlugin,
                project_group.project_name.features.common.client.JVMPlugin,
                project_group.project_name.features.sample.client.JVMPlugin,
                project_group.project_name.features.auth.client.JVMPlugin,

                project_group.project_name.features.ui.sample.JVMPlugin,
                project_group.project_name.features.ui.auth.JVMPlugin,
            )
        )
    )

    appJob.join()
}
