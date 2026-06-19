package dev.inmo.wishlist.features.common.client.ui.components

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Calm Studio inline pill (`.pill`) — a small rounded label with an optional colored leading dot.
 *
 * Used for item priority and the "Reserved" marker. Priority pills track the neutral default surface;
 * the reserved variant overrides the fill/text via a [pillClass] modifier (e.g. `.pill-ok`), keeping all
 * coloring in the stylesheet instead of inline styles.
 *
 * @param text Already-translated pill label.
 * @param dotClass Optional stylesheet class setting the leading dot's fill (e.g. `.dot-pri-high`);
 * `null` hides the dot.
 * @param pillClass Optional stylesheet class overriding the pill fill/text (e.g. `.pill-ok`).
 */
@Composable
fun CalmPill(
    text: String,
    dotClass: String? = null,
    pillClass: String? = null,
) {
    Span(attrs = {
        classes(CalmStudioStyleSheet.pill)
        pillClass?.let { classes(it) }
    }) {
        dotClass?.let { dot -> Span(attrs = { classes(CalmStudioStyleSheet.dot, dot) }) }
        Text(text)
    }
}

/**
 * Calm Studio item-priority pill — a [CalmPill] whose dot is colored by the item's priority.
 *
 * @param label Already-translated priority label (e.g. "High").
 * @param dotClass Stylesheet class setting the priority dot's fill (e.g. `.dot-pri-high`).
 */
@Composable
fun PriorityPill(label: String, dotClass: String) {
    CalmPill(text = label, dotClass = dotClass)
}

/**
 * Calm Studio card media badge (`.card .badge`) — the small priority chip overlaid in a card's
 * top-right corner.
 *
 * @param label Already-translated badge label.
 * @param dotClass Stylesheet class setting the leading dot's fill (e.g. `.dot-pri-high`).
 */
@Composable
fun CardBadge(label: String, dotClass: String) {
    Span(attrs = { classes(CalmStudioStyleSheet.badge) }) {
        Span(attrs = { classes(CalmStudioStyleSheet.dot, dotClass) })
        Text(label)
    }
}

/**
 * Calm Studio reserved flag (`.card .reserved-flag`) — the green marker overlaid in a card's top-left
 * corner when an item is reserved.
 *
 * @param text Already-translated flag text (e.g. "Reserved").
 */
@Composable
fun ReservedFlag(text: String) {
    Span(attrs = { classes(CalmStudioStyleSheet.`reserved-flag`) }) { Text(text) }
}

/**
 * Calm Studio item grid (`.grid`) — the auto-filling responsive container for [ItemCard]s.
 *
 * @param content The cards to lay out in the grid.
 */
@Composable
fun ItemGrid(content: @Composable () -> Unit) {
    Div({ classes(CalmStudioStyleSheet.grid) }) { content() }
}

/**
 * Calm Studio item card (`.card`) — the grid-view building block for a single wishlist item.
 *
 * Media area shows the deterministic tint plus, at most, one overlay: the [reservedFlag] (top-left) when
 * the item is reserved, otherwise the [badge] (top-right). The body carries the title, optional
 * description, and optional pre-formatted price.
 *
 * @param title Already-translated item title.
 * @param tintClass Deterministic media tint class (see [tintClass]).
 * @param description Optional already-translated description (clamped to two lines by the stylesheet).
 * @param priceText Optional pre-formatted price string; omit to hide the price line.
 * @param reservedFlag Optional reserved-marker text; when set it replaces the [badge].
 * @param badge Optional top-right overlay (typically a [CardBadge]); ignored when [reservedFlag] is set.
 * @param onOpen Invoked when the card is clicked; `null` makes the card non-interactive.
 */
@Composable
fun ItemCard(
    title: String,
    tintClass: String,
    description: String? = null,
    priceText: String? = null,
    reservedFlag: String? = null,
    badge: (@Composable () -> Unit)? = null,
    onOpen: (() -> Unit)? = null,
) {
    Div({
        classes(CalmStudioStyleSheet.card)
        if (onOpen != null) onClick { onOpen() } else classes(CalmStudioStyleSheet.nonclickable)
    }) {
        Div({ classes(CalmStudioStyleSheet.media, tintClass) }) {
            when {
                reservedFlag != null -> ReservedFlag(reservedFlag)
                badge != null -> badge()
                else -> Unit
            }
        }
        Div({ classes(CalmStudioStyleSheet.c) }) {
            H3 { Text(title) }
            description?.let { P({ classes(CalmStudioStyleSheet.desc) }) { Text(it) } }
            priceText?.let { Div({ classes(CalmStudioStyleSheet.price) }) { Text(it) } }
        }
    }
}

/**
 * Calm Studio rows container (`.rows`) — the bordered list wrapper for [ItemRow]s (list view).
 *
 * @param content The rows to render.
 */
@Composable
fun RowsList(content: @Composable () -> Unit) {
    Div({ classes(CalmStudioStyleSheet.rows) }) { content() }
}

/**
 * Calm Studio item row (`.row`) — the list-view building block for a single wishlist item.
 *
 * @param title Already-translated item title.
 * @param tintClass Deterministic thumbnail tint class (see [tintClass]).
 * @param description Optional already-translated description (single-line, ellipsized by the stylesheet).
 * @param priceText Optional pre-formatted price string; omit to hide the trailing price.
 * @param pill Optional trailing pill rendered next to the title (typically a [PriorityPill] or the
 * reserved [CalmPill]).
 * @param onOpen Invoked when the row is clicked; `null` makes the row non-interactive.
 */
@Composable
fun ItemRow(
    title: String,
    tintClass: String,
    description: String? = null,
    priceText: String? = null,
    pill: (@Composable () -> Unit)? = null,
    onOpen: (() -> Unit)? = null,
) {
    Div({
        classes(CalmStudioStyleSheet.row)
        if (onOpen != null) onClick { onOpen() } else classes(CalmStudioStyleSheet.nonclickable)
    }) {
        Span({ classes(CalmStudioStyleSheet.thumb, tintClass) })
        Div({ classes(CalmStudioStyleSheet.rmain) }) {
            Div({ classes(CalmStudioStyleSheet.titlepill) }) {
                H3 { Text(title) }
                pill?.invoke()
            }
            description?.let { P({ classes(CalmStudioStyleSheet.desc) }) { Text(it) } }
        }
        priceText?.let { Span({ classes(CalmStudioStyleSheet.rprice) }) { Text(it) } }
    }
}

/**
 * Calm Studio list-cards grid (`.listgrid`) — the responsive container for [ListCard]s (My Lists /
 * profile / search).
 *
 * @param content The list cards to lay out.
 */
@Composable
fun ListCardsGrid(content: @Composable () -> Unit) {
    Div({ classes(CalmStudioStyleSheet.listgrid) }) { content() }
}

/**
 * Calm Studio list card (`.listcard`) — a wishlist tile: a deterministic gradient cover (with an
 * optional visibility chip) over the list title and an optional meta line.
 *
 * @param title Already-translated list title.
 * @param tintClass Deterministic cover tint class (see [tintClass]).
 * @param meta Optional already-translated meta line (e.g. "12 items · 2 reserved").
 * @param visibility Optional already-translated visibility chip text shown on the cover.
 * @param onOpen Invoked when the card is clicked; `null` makes the card non-interactive.
 */
@Composable
fun ListCard(
    title: String,
    tintClass: String,
    meta: String? = null,
    visibility: String? = null,
    onOpen: (() -> Unit)? = null,
) {
    Div({
        classes(CalmStudioStyleSheet.listcard)
        if (onOpen != null) onClick { onOpen() } else classes(CalmStudioStyleSheet.nonclickable)
    }) {
        Div({ classes(CalmStudioStyleSheet.cover, tintClass) }) {
            visibility?.let { Span({ classes(CalmStudioStyleSheet.vis) }) { Text(it) } }
        }
        Div({ classes(CalmStudioStyleSheet.c) }) {
            H3 { Text(title) }
            meta?.let { Div({ classes(CalmStudioStyleSheet.meta) }) { Text(it) } }
        }
    }
}

/**
 * Calm Studio "new list" card (`.listcard.new`) — the dashed call-to-action tile that creates a list.
 *
 * @param label Already-translated call-to-action label.
 * @param onClick Invoked when the card is clicked.
 */
@Composable
fun NewListCard(label: String, onClick: () -> Unit) {
    Div({
        classes(CalmStudioStyleSheet.listcard, CalmStudioStyleSheet.new)
        onClick { onClick() }
    }) {
        CalmIcon(CalmIcons.plus)
        Text(label)
    }
}

/**
 * Calm Studio people grid (`.people`) — the responsive container for [PersonCard]s (Discover / search).
 *
 * @param content The person cards to lay out.
 */
@Composable
fun PeopleGrid(content: @Composable () -> Unit) {
    Div({ classes(CalmStudioStyleSheet.people) }) { content() }
}

/**
 * Calm Studio person card (`.person`) — a discover tile: a tinted avatar, the person's name, an optional
 * meta line, and an optional admin chip.
 *
 * @param name Already-translated display name.
 * @param tintClass Deterministic avatar tint class (see [tintClass]).
 * @param meta Optional already-translated meta line (e.g. "3 public lists · 12 items").
 * @param adminBadge Optional already-translated admin chip text; `null` hides the chip.
 * @param onOpen Invoked when the card is clicked; `null` makes the card non-interactive.
 */
@Composable
fun PersonCard(
    name: String,
    tintClass: String,
    meta: String? = null,
    adminBadge: String? = null,
    onOpen: (() -> Unit)? = null,
) {
    Div({
        classes(CalmStudioStyleSheet.person)
        if (onOpen != null) onClick { onOpen() } else classes(CalmStudioStyleSheet.nonclickable)
    }) {
        Span({ classes(CalmStudioStyleSheet.av, tintClass) })
        H3 { Text(name) }
        meta?.let { Div({ classes(CalmStudioStyleSheet.meta) }) { Text(it) } }
        adminBadge?.let { Div({ classes(CalmStudioStyleSheet.adm) }) { Text(it) } }
    }
}
