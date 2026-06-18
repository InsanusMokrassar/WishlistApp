package dev.inmo.wishlist.features.common.client.ui.components

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Text

/**
 * Visual style of a [CalmButton]. Maps to the Calm Studio `.btn` modifier classes — the base `.btn` is
 * always applied, the variant class (if any) is appended.
 */
enum class CalmButtonVariant(internal val cssClass: String?) {
    /** Neutral surface button (`.btn`) — secondary toolbar actions. */
    Default(null),

    /** Accent-filled primary action (`.btn.primary`). */
    Primary(CalmStudioStyleSheet.primary),

    /** Borderless, muted action (`.btn.ghost`) — Cancel / Log out. */
    Ghost(CalmStudioStyleSheet.ghost),

    /** Destructive action (`.btn.danger`) — Delete. */
    Danger(CalmStudioStyleSheet.danger),
}

/**
 * Size modifier of a [CalmButton], mapping to the Calm Studio `.btn` size classes.
 */
enum class CalmButtonSize(internal val cssClass: String?) {
    /** Default padding/size. */
    Normal(null),

    /** Compact button (`.btn.sm`). */
    Small(CalmStudioStyleSheet.sm),
}

/**
 * Calm Studio button (`.btn`) — the single action primitive for content screens.
 *
 * Mirrors the reference design's `<button className="btn …">`: an optional leading [CalmIcons] glyph
 * followed by the label. Solid [CalmButtonVariant.Primary] drives primary flows; [CalmButtonVariant.Ghost]
 * is the lightweight Cancel/Log-out action; [CalmButtonVariant.Danger] is destructive.
 *
 * @param text Already-translated button label.
 * @param onClick Invoked when the user activates the button.
 * @param variant Visual style; defaults to the neutral [CalmButtonVariant.Default].
 * @param size Size modifier; defaults to [CalmButtonSize.Normal].
 * @param block Whether the button stretches full width (`.btn.block`).
 * @param disabled Whether the button is non-interactive (`:disabled`).
 * @param leadingIcon Optional inner SVG markup (one of [CalmIcons]) rendered before [text].
 * @param type Native button type; defaults to [ButtonType.Button] so the button never submits a form
 * unless the caller opts in.
 */
@Composable
fun CalmButton(
    text: String,
    onClick: () -> Unit,
    variant: CalmButtonVariant = CalmButtonVariant.Default,
    size: CalmButtonSize = CalmButtonSize.Normal,
    block: Boolean = false,
    disabled: Boolean = false,
    leadingIcon: String? = null,
    type: ButtonType = ButtonType.Button,
) {
    val classNames = mutableListOf(CalmStudioStyleSheet.btn)
    variant.cssClass?.let { classNames.add(it) }
    size.cssClass?.let { classNames.add(it) }
    if (block) classNames.add(CalmStudioStyleSheet.block)
    Button(attrs = {
        type(type)
        classes(*classNames.toTypedArray())
        if (disabled) disabled()
        onClick { onClick() }
    }) {
        leadingIcon?.let { CalmIcon(it) }
        Text(text)
    }
}

/**
 * Calm Studio square icon button (`.iconbtn`) — a single [CalmIcons] glyph with no label.
 *
 * @param icon Inner SVG markup (one of [CalmIcons]) to render.
 * @param onClick Invoked when the user activates the button.
 * @param disabled Whether the button is non-interactive.
 */
@Composable
fun IconButton(
    icon: String,
    onClick: () -> Unit,
    disabled: Boolean = false,
) {
    Button(attrs = {
        type(ButtonType.Button)
        classes(CalmStudioStyleSheet.iconbtn)
        if (disabled) disabled()
        onClick { onClick() }
    }) {
        CalmIcon(icon)
    }
}
