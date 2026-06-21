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
                dev.inmo.wishlist.features.users.client.JVMPlugin,
                dev.inmo.wishlist.features.auth.client.JVMPlugin,
                dev.inmo.wishlist.features.wishlist.client.JVMPlugin,
                dev.inmo.wishlist.features.files.client.JVMPlugin,
                dev.inmo.wishlist.features.admin.client.JVMPlugin,
                dev.inmo.wishlist.features.currency.client.JVMPlugin,
                dev.inmo.wishlist.features.booking.client.JVMPlugin,
                dev.inmo.wishlist.features.email.client.JVMPlugin,

                dev.inmo.wishlist.features.ui.sample.JVMPlugin,
                dev.inmo.wishlist.features.ui.auth.JVMPlugin,
                dev.inmo.wishlist.features.ui.wishlist.JVMPlugin,
                dev.inmo.wishlist.features.ui.adminPanel.JVMPlugin,
                dev.inmo.wishlist.features.ui.scaffold.JVMPlugin,
                dev.inmo.wishlist.features.ui.users.JVMPlugin,
                dev.inmo.wishlist.features.ui.topBar.JVMPlugin,
                dev.inmo.wishlist.features.ui.serverUrl.JVMPlugin,
                dev.inmo.wishlist.features.ui.booking.JVMPlugin,
            )
        )
    )

    appJob.join()
}
