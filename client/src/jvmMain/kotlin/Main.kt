package dev.inmo.wishlist.client

import dev.inmo.micro_utils.startup.launcher.Config
import dev.inmo.micro_utils.startup.launcher.StartLauncherPlugin
import kotlinx.coroutines.Job

suspend fun main() {
    val appJob = Job()
    StartLauncherPlugin.start(
        Config(
            listOf(
                ClientJVMPlugin(appJob),
                dev.inmo.wishlist.features.common.common.JVMPlugin,
                dev.inmo.wishlist.features.common.client.JVMPlugin,
                dev.inmo.wishlist.features.sample.client.JVMPlugin,
                dev.inmo.wishlist.features.auth.client.JVMPlugin,
                dev.inmo.wishlist.features.wishlist.client.JVMPlugin,

                dev.inmo.wishlist.features.ui.sample.JVMPlugin,
                dev.inmo.wishlist.features.ui.auth.JVMPlugin,
                dev.inmo.wishlist.features.ui.wishlist.JVMPlugin,
            )
        )
    )

    appJob.join()
}
