package dev.inmo.wishlist.client

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
                        dev.inmo.wishlist.features.common.common.JSPlugin,
                        dev.inmo.wishlist.features.common.client.JSPlugin,
                        dev.inmo.wishlist.features.sample.client.JSPlugin,
                        dev.inmo.wishlist.features.users.client.JSPlugin,
                        dev.inmo.wishlist.features.auth.client.JSPlugin,
                        dev.inmo.wishlist.features.wishlist.client.JSPlugin,
                        dev.inmo.wishlist.features.files.client.JSPlugin,
                        dev.inmo.wishlist.features.admin.client.JSPlugin,
                        dev.inmo.wishlist.features.currency.client.JSPlugin,

                        dev.inmo.wishlist.features.ui.sample.JSPlugin,
                        dev.inmo.wishlist.features.ui.auth.JSPlugin,
                        dev.inmo.wishlist.features.ui.wishlist.JSPlugin,
                        dev.inmo.wishlist.features.ui.adminPanel.JSPlugin,
                        dev.inmo.wishlist.features.ui.scaffold.JSPlugin,
                        dev.inmo.wishlist.features.ui.users.JSPlugin,
                        dev.inmo.wishlist.features.ui.topBar.JSPlugin,
                    )
                )
            )
        }
    })
}
