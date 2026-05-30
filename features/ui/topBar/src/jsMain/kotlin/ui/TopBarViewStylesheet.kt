package dev.inmo.wishlist.features.ui.topBar.ui

import dev.inmo.micro_utils.coroutines.compose.StyleSheetsAggregator
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.style
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.css.percent

/** CSS layout classes for the JS [TopBarView]. */
object TopBarViewStylesheet : StyleSheet() {
    /** Container with flex row layout, padding, and 100% width. */
    val topBarContainer by style {
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        justifyContent(JustifyContent.SpaceBetween)
        width(100.percent)
        padding(8.px, 16.px)
    }

    /** Right-aligned actions cluster containing the auth widget. */
    val topBarActions by style {
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
    }

    init { StyleSheetsAggregator.addStyleSheet(this) }
}
