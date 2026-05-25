package project_group.project_name.features.ui.auth.utils

import kotlinx.browser.window

actual fun defaultServerUrl(): String = window.location.origin
