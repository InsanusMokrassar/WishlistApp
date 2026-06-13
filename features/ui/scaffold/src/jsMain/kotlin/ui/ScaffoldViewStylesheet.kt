package dev.inmo.wishlist.features.ui.scaffold.ui

import dev.inmo.micro_utils.coroutines.compose.StyleSheetsAggregator
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexGrow
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.style
import org.jetbrains.compose.web.css.width

/**
 * Compose-Web CSS stylesheet for [ScaffoldView].
 *
 * Defines flex-based layout classes for the outer container, body row, left slot, and main slot.
 */
object ScaffoldViewStylesheet : StyleSheet() {

    /** Outer column container occupying full viewport width and height. */
    val scaffoldContainer by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        width(100.percent)
        height(100.percent)
    }

    /** Row below the top slot that fills remaining height. */
    val scaffoldBody by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Row)
        flexGrow(1)
        overflow("hidden")
    }

    /** Left slot — natural width, full body height. */
    val scaffoldLeft by style {
        overflow("hidden")
    }

    /** Main slot — fills all remaining width in the body row. */
    val scaffoldMain by style {
        flexGrow(1)
        overflow("hidden")
    }

    init {
        StyleSheetsAggregator.addStyleSheet(this)
    }
}
