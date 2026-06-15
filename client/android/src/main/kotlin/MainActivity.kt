package dev.inmo.wishlist.client

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import dev.inmo.micro_utils.startup.launcher.Config
import dev.inmo.micro_utils.startup.launcher.StartLauncherPlugin
import dev.inmo.wishlist.features.ui.users.utils.AvatarImagePicker
import dev.inmo.wishlist.features.ui.wishlist.utils.AndroidImagePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val clientAndroidPlugin = ClientAndroidPlugin(this)

    private val scope = CoroutineScope(Dispatchers.Default)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidImagePicker.register(this)
        AvatarImagePicker.register(this)
        scope.launch {
            StartLauncherPlugin.start(
                Config(
                    listOf(
                        clientAndroidPlugin,
                        dev.inmo.wishlist.features.common.common.AndroidPlugin,
                        dev.inmo.wishlist.features.common.client.AndroidPlugin,
                        dev.inmo.wishlist.features.sample.client.AndroidPlugin,
                        dev.inmo.wishlist.features.users.client.AndroidPlugin,
                        dev.inmo.wishlist.features.auth.client.AndroidPlugin,
                        dev.inmo.wishlist.features.wishlist.client.AndroidPlugin,
                        dev.inmo.wishlist.features.files.client.AndroidPlugin,
                        dev.inmo.wishlist.features.admin.client.AndroidPlugin,
                        dev.inmo.wishlist.features.currency.client.AndroidPlugin,
                        dev.inmo.wishlist.features.booking.client.AndroidPlugin,
                        dev.inmo.wishlist.features.email.client.AndroidPlugin,

                        dev.inmo.wishlist.features.ui.sample.AndroidPlugin,
                        dev.inmo.wishlist.features.ui.auth.AndroidPlugin,
                        dev.inmo.wishlist.features.ui.wishlist.AndroidPlugin,
                        dev.inmo.wishlist.features.ui.adminPanel.AndroidPlugin,
                        dev.inmo.wishlist.features.ui.scaffold.AndroidPlugin,
                        dev.inmo.wishlist.features.ui.users.AndroidPlugin,
                        dev.inmo.wishlist.features.ui.topBar.AndroidPlugin,
                        dev.inmo.wishlist.features.ui.serverUrl.AndroidPlugin,
                        dev.inmo.wishlist.features.ui.booking.AndroidPlugin,
                    )
                )
            )
        }

        setContent {
            ClientPlugin.currentDrawingBlock.value()
        }
    }
}
