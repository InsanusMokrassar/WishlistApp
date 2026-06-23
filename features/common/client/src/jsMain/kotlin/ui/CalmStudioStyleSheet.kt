package dev.inmo.wishlist.features.common.client.ui

import dev.inmo.micro_utils.coroutines.compose.StyleSheetsAggregator
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.media as cssMedia
import org.jetbrains.compose.web.css.mediaMaxWidth
import org.jetbrains.compose.web.css.px

/**
 * Calm Studio — the single web-client design stylesheet, expressed as a Compose [StyleSheet] instead of
 * a hand-written `.css` file.
 *
 * Every Calm Studio class is a `val name by style { }` delegate. The sheet uses `usePrefix = false`, so
 * a delegate's property name **is** its emitted class name verbatim (`val btn` → `.btn`), and every
 * call site references the class through this object (`classes(CalmStudioStyleSheet.btn)`) — there are
 * no raw class-name strings in views or components.
 *
 * Two kinds of delegate:
 * - **Component classes** carry their own rules and nest their variants/state/descendants (the Style
 *   DSL "inheritance"): inside a `style { }` block `self` is the current selector, `self + className(x)`
 *   builds a compound selector, `self + hover` a pseudo, and `className(token) style { }` a descendant.
 * - **Token classes** are declared first with an empty `by style { }` body: they exist only to give a
 *   typed name to a class that is styled *in context* (e.g. `count` is styled by `.navitem .count`, not
 *   on its own). They are referenced both at HTML call sites (`classes(CalmStudioStyleSheet.count)`) and
 *   inside the owning component's nested rule (`className(count) style { … }`).
 *
 * Selectors that are not a single class — the `:root` `--cs-*` tokens, the element reset, `::selection`,
 * and the grouped `.input, .textarea, select.input` — stay as raw rules in [init]. Two class names can
 * **not** be delegates and stay raw exceptions: `empty` and `right` clash with `SelectorsScope`
 * members (`:empty`, `:right`). Dead Bootstrap utility classes still present on a couple of avatar views
 * (`rounded`, `rounded-circle`, `flex-shrink-0`) are not part of this sheet and keep their raw strings.
 *
 * Self-registers into the [StyleSheetsAggregator] from [init]; nothing references the generated class
 * strings statically, so [ensureRegistered] forces initialization at startup (see `ClientJSPlugin`).
 * Per the "Design System Rule (web — Calm Studio)" in `agents/CODING.md`.
 */
object CalmStudioStyleSheet : StyleSheet(usePrefix = false) {

    // ---- token classes (empty bodies; styled in context by their owning component, below) ----

    /** `.mk` — sidebar logo accent square (styled by `.logo .mk`). */
    val mk by style {}

    /** `.count` — nav item trailing count badge (styled by `.navitem .count`). */
    val count by style {}

    /** `.swatch` — nav item leading color swatch (styled by `.navitem .swatch`). */
    val swatch by style {}

    /** `.spacer` — sidebar flex spacer (styled by `.sidebar .spacer`). */
    val spacer by style {}

    /** `.sp` — top-bar flex spacer (styled by `.topbar .sp`). */
    val sp by style {}

    /** `.kbd` — top-bar shortcut chip (styled by `.topbar .kbd`). */
    val kbd by style {}

    /** `.av` — avatar circle (styled by `.me .av` / `.person .av`); covers when applied to an `<img>`. */
    val av by style { property("object-fit", "cover") }

    /** `.nm` — profile name block (styled by `.me .nm`). */
    val nm by style {}

    /** `.media` — card media area (styled by `.card .media`). */
    val media by style {}

    /** `.badge` — card media priority badge (styled by `.card .badge`). */
    val badge by style {}

    /** `.dot` — small color dot inside badges/pills (styled in context). */
    val dot by style {}

    /** `.c` — card body container (styled by `.card .c` / `.listcard .c`). */
    val c by style {}

    /** `.price` — card price line (styled by `.card .price`). */
    val price by style {}

    /** `.desc` — card/row description line (styled by `.card .desc` / `.row .rmain .desc`). */
    val desc by style {}

    /** `.thumb` — list-row thumbnail (styled by `.row .thumb`). */
    val thumb by style {}

    /** `.rmain` — list-row main column (styled by `.row .rmain`). */
    val rmain by style {}

    /** `.rprice` — list-row trailing price (styled by `.row .rprice`). */
    val rprice by style {}

    /** `.cover` — list-card cover (styled by `.listcard .cover`). */
    val cover by style {}

    /** `.vis` — list-card visibility chip (styled by `.listcard .cover .vis`). */
    val vis by style {}

    /** `.meta` — list-card / person meta line (styled in context). */
    val meta by style {}

    /** `.adm` — person admin chip (styled by `.person .adm`). */
    val adm by style {}

    /** `.gallery` — item-detail gallery column (styled by `.detail .gallery .main-img`). */
    val gallery by style {}

    /** `.field` — item-detail field block (styled by `.detail .field`). */
    val field by style {}

    /** `.lbl` — item-detail field label (styled by `.detail .field .lbl`). */
    val lbl by style {}

    /** `.val` — item-detail field value (styled by `.detail .field .val`). */
    val `val` by style {}

    /** `.pricetag` — item-detail large price (styled by `.detail .pricetag`). */
    val pricetag by style {}

    /** `.linkrow` — item-detail external link row (styled by `.detail .linkrow`). */
    val linkrow by style {}

    /** `.actbar` — item-detail action bar (styled by `.detail .actbar`). */
    val actbar by style {}

    /** `.acts` — page-header actions cluster (styled by `.pagehead .acts`). */
    val acts by style {}

    /** `.sep` — breadcrumb separator (styled by `.crumb .sep`). */
    val sep by style {}

    /** `.ic` — empty-state glyph badge (styled by `.empty .ic`). */
    val ic by style {}

    /** `.ok` — toast success check (styled by `.toast .ok`). */
    val ok by style {}

    /** `.mhead` — modal header (styled by `.modal .mhead`). */
    val mhead by style {}

    /** `.mbody` — modal body (styled by `.modal .mbody`). */
    val mbody by style {}

    /** `.mfoot` — modal footer (styled by `.modal .mfoot`). */
    val mfoot by style {}

    /** `.tabs` — modal tab switch (styled by `.modal .tabs`). */
    val tabs by style {}

    /** `.reserved-flag` — card reserved marker (styled by `.card .reserved-flag`). */
    val `reserved-flag` by style {}

    /** `.main-img` — item-detail main image (styled by `.detail .gallery .main-img`). */
    val `main-img` by style {}

    /** `.input` — text input / select field (styled by the grouped rule in `init`). */
    val input by style {}

    /** `.textarea` — multi-line input field (styled by the grouped rule in `init`). */
    val textarea by style {}

    /** `.primary` — primary button variant modifier (styled by `.btn.primary`). */
    val primary by style {}

    /** `.ghost` — ghost button variant modifier (styled by `.btn.ghost`). */
    val ghost by style {}

    /** `.danger` — destructive button variant modifier (styled by `.btn.danger`). */
    val danger by style {}

    /** `.sm` — small button size modifier (styled by `.btn.sm`). */
    val sm by style {}

    /** `.block` — full-width button modifier (styled by `.btn.block`). */
    val block by style {}

    /** `.on` — active/selected state modifier (styled in context, e.g. `.navitem.on`, `.seg button.on`). */
    val on by style {}

    /** `.show` — visible-toast modifier (styled by `.toast.show`). */
    val show by style {}

    /** `.new` — "new list" call-to-action modifier (styled by `.listcard.new`). */
    val new by style {}

    // ---- component classes ----

    /** `.app` — full-height flex shell holding the sidebar and main column. */
    val app by style {
        property("display", "flex"); property("height", "100vh"); property("overflow", "hidden")
    }

    /** `.sidebar` — fixed-width left navigation column (with its `.spacer` push). */
    val sidebar by style {
        property("width", "var(--cs-sidebar-w)"); property("flex-shrink", "0"); property("background", "var(--cs-surface)")
        property("border-right", "1px solid var(--cs-line)"); property("display", "flex"); property("flex-direction", "column")
        property("padding", "16px 12px")
        className(spacer) style { property("flex", "1") }
    }

    /** `.logo` — brand word-mark row with its `.mk` accent square. */
    val logo by style {
        property("display", "flex"); property("align-items", "center"); property("gap", "9px"); property("font-weight", "800"); property("font-size", "16px")
        property("letter-spacing", "-.01em"); property("padding", "6px 8px 16px")
        className(mk) style {
            property("width", "22px"); property("height", "22px"); property("border-radius", "7px"); property("background", "var(--cs-accent)")
            property("display", "grid"); property("place-items", "center"); property("flex-shrink", "0")
            "svg" style { property("width", "13px"); property("height", "13px"); property("color", "#fff") }
        }
    }

    /** `.navsec` — a vertical group of navigation items. */
    val navsec by style {
        property("display", "flex"); property("flex-direction", "column"); property("gap", "2px")
    }

    /** `.navlabel` — uppercase section caption above a nav group. */
    val navlabel by style {
        property("font-size", "10.5px"); property("font-weight", "700"); property("letter-spacing", ".1em"); property("text-transform", "uppercase")
        property("color", "var(--cs-faint)"); property("padding", "16px 10px 7px")
    }

    /** `.navitem` — one navigation row (hover, active `.on`, disabled, trailing `.count`, leading `.swatch`). */
    val navitem by style {
        property("display", "flex"); property("align-items", "center"); property("gap", "11px"); property("font-size", "13.5px"); property("font-weight", "600")
        property("color", "var(--cs-muted)"); property("text-decoration", "none"); property("padding", "9px 10px"); property("border-radius", "8px")
        property("border", "none"); property("background", "none"); property("cursor", "pointer"); property("width", "100%"); property("text-align", "left")
        "svg" style { property("width", "17px"); property("height", "17px"); property("flex-shrink", "0") }
        self + hover style { property("background", "var(--cs-surface-2)"); property("color", "var(--cs-ink-2)") }
        className(count) style {
            property("margin-left", "auto"); property("font-size", "11px"); property("font-weight", "700"); property("color", "var(--cs-faint)")
            property("background", "var(--cs-surface-2)"); property("padding", "1px 7px"); property("border-radius", "999px")
        }
        className(swatch) style { property("width", "9px"); property("height", "9px"); property("border-radius", "3px"); property("flex-shrink", "0") }
        self + className(on) style {
            property("background", "var(--cs-accent-soft)"); property("color", "var(--cs-accent)")
            className(count) style { property("background", "#fff"); property("color", "var(--cs-accent)") }
        }
        self + disabled style {
            property("opacity", ".5"); property("cursor", "default")
            self + hover style { property("background", "none"); property("color", "var(--cs-muted)") }
        }
    }

    /** `.me` — bottom profile row (hover, `.av` avatar, `.nm` name with its `small` caption). */
    val me by style {
        property("display", "flex"); property("align-items", "center"); property("gap", "10px"); property("padding", "9px 8px"); property("border-top", "1px solid var(--cs-line)")
        property("margin-top", "8px"); property("cursor", "pointer"); property("border-radius", "8px")
        self + hover style { property("background", "var(--cs-surface-2)") }
        className(av) style { property("width", "30px"); property("height", "30px"); property("border-radius", "999px"); property("flex-shrink", "0") }
        className(nm) style {
            property("font-size", "13px"); property("font-weight", "700"); property("line-height", "1.25")
            "small" style { property("display", "block"); property("font-weight", "500"); property("color", "var(--cs-muted)"); property("font-size", "11.5px") }
        }
    }

    /** `.main` — the column right of the sidebar (top bar over scrolling content). */
    val main by style {
        property("flex", "1"); property("display", "flex"); property("flex-direction", "column"); property("min-width", "0")
    }

    /** `.topbar` — top action bar (with its `.sp` spacer and `.kbd` shortcut chip). */
    val topbar by style {
        property("height", "var(--cs-topbar-h)"); property("flex-shrink", "0"); property("display", "flex"); property("align-items", "center"); property("gap", "14px")
        property("padding", "0 24px"); property("border-bottom", "1px solid var(--cs-line)"); property("background", "var(--cs-surface)")
        className(sp) style { property("flex", "1") }
        className(kbd) style {
            property("font-size", "11px"); property("color", "var(--cs-faint)"); property("border", "1px solid var(--cs-line-2)"); property("border-radius", "5px")
            property("padding", "1px 6px"); property("font-weight", "700")
        }
    }

    /** `.search` — top-bar search field (with its `svg` glyph and bare `input`). */
    val search by style {
        property("flex", "1"); property("max-width", "380px"); property("display", "flex"); property("align-items", "center"); property("gap", "9px")
        property("background", "var(--cs-bg)"); property("border", "1px solid var(--cs-line-2)"); property("border-radius", "9px"); property("padding", "8px 12px")
        property("color", "var(--cs-faint)"); property("font-size", "13.5px"); property("cursor", "text")
        "svg" style { property("width", "15px"); property("height", "15px"); property("flex-shrink", "0") }
        "input" style {
            property("border", "none"); property("background", "none"); property("outline", "none"); property("font-family", "inherit"); property("font-size", "13.5px")
            property("color", "var(--cs-ink)"); property("flex", "1"); property("padding", "0")
        }
    }

    /** `.content` — the scrolling content area below the top bar. */
    val content by style {
        property("flex", "1"); property("overflow-y", "auto"); property("padding", "26px 28px 60px")
    }

    /** `.crumb` — breadcrumb row (links `a`, current `b`, `.sep` separators). */
    val crumb by style {
        property("font-size", "12.5px"); property("color", "var(--cs-muted)"); property("margin-bottom", "14px"); property("display", "flex"); property("align-items", "center"); property("gap", "6px")
        "a" style {
            property("text-decoration", "none"); property("cursor", "pointer")
            self + hover style { property("color", "var(--cs-ink)") }
        }
        "b" style { property("color", "var(--cs-ink)"); property("font-weight", "700") }
        className(sep) style { property("color", "var(--cs-line-2)") }
    }

    /** `.crumbbar` — slim breadcrumb strip under the top bar (relaxes its inner `.crumb` margin). */
    val crumbbar by style {
        property("padding", "14px 28px 0")
        className(crumb) style { property("margin-bottom", "0") }
    }

    /** `.btn` — action button: base + `.primary`/`.ghost`/`.danger` variants, `.sm`/`.block` modifiers, hover, disabled. */
    val btn by style {
        property("font-size", "13.5px"); property("font-weight", "700"); property("border", "1px solid var(--cs-line-2)"); property("background", "var(--cs-surface)")
        property("color", "var(--cs-ink)"); property("border-radius", "9px"); property("padding", "8px 15px"); property("cursor", "pointer"); property("display", "inline-flex")
        property("align-items", "center"); property("gap", "7px"); property("white-space", "nowrap"); property("transition", ".12s")
        "svg" style { property("width", "15px"); property("height", "15px") }
        self + hover style { property("background", "var(--cs-surface-2)") }
        self + className(primary) style {
            property("background", "var(--cs-accent)"); property("border-color", "var(--cs-accent)"); property("color", "#fff")
            self + hover style { property("background", "var(--cs-accent-hover)") }
        }
        self + className(ghost) style {
            property("border-color", "transparent"); property("background", "none"); property("color", "var(--cs-muted)")
            self + hover style { property("background", "var(--cs-surface-2)"); property("color", "var(--cs-ink)") }
        }
        self + className(danger) style {
            property("color", "var(--cs-danger)"); property("border-color", "#F3C9C4"); property("background", "#fff")
            self + hover style { property("background", "#FDF1F0") }
        }
        self + className(sm) style { property("padding", "6px 11px"); property("font-size", "12.5px") }
        self + className(block) style { property("width", "100%"); property("justify-content", "center") }
        self + disabled style { property("opacity", ".5"); property("cursor", "default") }
    }

    /** `.iconbtn` — square icon-only button (hover, inner `svg`). */
    val iconbtn by style {
        property("width", "36px"); property("height", "36px"); property("border-radius", "9px"); property("border", "1px solid var(--cs-line-2)")
        property("background", "var(--cs-surface)"); property("display", "grid"); property("place-items", "center"); property("color", "var(--cs-muted)"); property("cursor", "pointer")
        self + hover style { property("background", "var(--cs-surface-2)"); property("color", "var(--cs-ink)") }
        "svg" style { property("width", "17px"); property("height", "17px") }
    }

    /** `.icon` — inline-flex envelope that centers a [CalmIcon] `<svg>` glyph (kills inline baseline drift). */
    val icon by style {
        property("display", "inline-flex"); property("align-items", "center"); property("justify-content", "center")
    }

    /** `.pagehead` — screen header: title `h1` on the left, `.acts` cluster on the right. */
    val pagehead by style {
        property("display", "flex"); property("justify-content", "space-between"); property("align-items", "flex-start"); property("gap", "20px"); property("margin-bottom", "4px")
        "h1" style { property("font-size", "var(--cs-h1)"); property("font-weight", "800"); property("letter-spacing", "-.02em"); property("margin", "0") }
        className(acts) style { property("display", "flex"); property("gap", "8px"); property("flex-shrink", "0") }
    }

    /** `.subline` — muted secondary line under a page title. */
    val subline by style {
        property("font-size", "13.5px"); property("color", "var(--cs-muted)"); property("margin", "7px 0 0")
    }

    /** `.toolbar` — space-between filter/action row (with its `.right` cluster — `right` is a raw exception). */
    val toolbar by style {
        property("display", "flex"); property("justify-content", "space-between"); property("align-items", "center"); property("gap", "12px")
        property("margin", "22px 0 16px"); property("flex-wrap", "wrap")
        ".right" style { property("display", "flex"); property("gap", "8px"); property("align-items", "center") }
    }

    /** `.seg` — segmented control (its `button`s, active `button.on`). */
    val seg by style {
        property("display", "inline-flex"); property("background", "var(--cs-bg)"); property("border", "1px solid var(--cs-line-2)"); property("border-radius", "9px"); property("padding", "3px"); property("gap", "2px")
        "button" style {
            property("font-size", "12.5px"); property("font-weight", "600"); property("color", "var(--cs-muted)"); property("border", "0"); property("background", "none")
            property("padding", "6px 13px"); property("border-radius", "6px"); property("cursor", "pointer")
            self + className(on) style { property("background", "var(--cs-surface)"); property("color", "var(--cs-ink)"); property("box-shadow", "var(--cs-shadow-sm)") }
        }
    }

    /** `.select` — native dropdown styled for the toolbar. */
    val select by style {
        property("font-size", "12.5px"); property("font-weight", "600"); property("color", "var(--cs-ink-2)"); property("border", "1px solid var(--cs-line-2)")
        property("border-radius", "8px"); property("padding", "7px 11px"); property("background", "var(--cs-surface)"); property("cursor", "pointer"); property("font-family", "inherit")
    }

    /** `.grid` — auto-filling responsive grid of item cards. */
    val grid by style {
        property("display", "grid"); property("grid-template-columns", "repeat(auto-fill, minmax(220px, 1fr))"); property("gap", "var(--cs-gap)")
    }

    /** `.card` — item card: hover lift, `.media` (with `.badge`/`.dot`, `.reserved-flag`), `.c` body (`h3`, `.desc`, `.price`). */
    val card by style {
        property("background", "var(--cs-surface)"); property("border", "1px solid var(--cs-line)"); property("border-radius", "var(--cs-radius-lg)")
        property("overflow", "hidden"); property("cursor", "pointer"); property("transition", ".14s"); property("display", "flex"); property("flex-direction", "column")
        self + hover style { property("border-color", "var(--cs-line-2)"); property("box-shadow", "var(--cs-shadow)"); property("transform", "translateY(-2px)") }
        className(media) style {
            property("height", "150px"); property("position", "relative")
            "img" style { property("width", "100%"); property("height", "100%"); property("object-fit", "cover"); property("display", "block") }
        }
        className(badge) style {
            property("position", "absolute"); property("top", "9px"); property("right", "9px"); property("font-size", "10.5px"); property("font-weight", "700")
            property("padding", "3px 9px"); property("border-radius", "6px"); property("background", "rgba(255,255,255,.92)"); property("color", "var(--cs-ink-2)")
            property("display", "flex"); property("align-items", "center"); property("gap", "5px")
            className(dot) style { property("width", "6px"); property("height", "6px"); property("border-radius", "999px") }
        }
        className(`reserved-flag`) style {
            property("position", "absolute"); property("top", "9px"); property("left", "9px"); property("font-size", "10.5px"); property("font-weight", "700")
            property("padding", "3px 9px"); property("border-radius", "6px"); property("background", "var(--cs-ok)"); property("color", "#fff")
        }
        className(c) style { property("padding", "var(--cs-gap)"); property("flex", "1"); property("display", "flex"); property("flex-direction", "column") }
        "h3" style { property("font-size", "var(--cs-card-title)"); property("font-weight", "700"); property("margin", "0 0 4px"); property("letter-spacing", "-.01em"); property("line-height", "1.3") }
        className(desc) style {
            property("font-size", "12.5px"); property("color", "var(--cs-muted)"); property("margin", "0 0 10px"); property("line-height", "1.45")
            property("display", "-webkit-box"); property("-webkit-line-clamp", "2"); property("-webkit-box-orient", "vertical"); property("overflow", "hidden")
        }
        className(price) style { property("font-size", "13px"); property("color", "var(--cs-ink-2)"); property("font-weight", "700"); property("margin-top", "auto") }
    }

    /** `.rows` — bordered container wrapping item rows. */
    val rows by style {
        property("display", "flex"); property("flex-direction", "column"); property("border", "1px solid var(--cs-line)"); property("border-radius", "var(--cs-radius-lg)")
        property("overflow", "hidden"); property("background", "var(--cs-surface)")
    }

    /** `.row` — item list row (last-child divider, hover, `.thumb`, `.rmain` with `h3`/`.desc`, `.rprice`). */
    val row by style {
        property("display", "flex"); property("align-items", "center"); property("gap", "16px"); property("padding", "12px 16px"); property("cursor", "pointer")
        property("border-bottom", "1px solid var(--cs-line)"); property("transition", ".1s")
        self + lastChild style { property("border-bottom", "none") }
        self + hover style { property("background", "var(--cs-surface-2)") }
        className(thumb) style { property("width", "46px"); property("height", "46px"); property("border-radius", "8px"); property("flex-shrink", "0"); property("object-fit", "cover") }
        className(rmain) style {
            property("flex", "1"); property("min-width", "0")
            "h3" style { property("font-size", "14px"); property("font-weight", "700"); property("margin", "0"); property("letter-spacing", "-.01em") }
            className(desc) style {
                property("font-size", "12.5px"); property("color", "var(--cs-muted)"); property("margin", "2px 0 0")
                property("white-space", "nowrap"); property("overflow", "hidden"); property("text-overflow", "ellipsis"); property("max-width", "460px")
            }
        }
        className(rprice) style { property("font-size", "13px"); property("font-weight", "700"); property("color", "var(--cs-ink-2)"); property("flex-shrink", "0") }
    }

    /** `.pill` — inline rounded label with its leading `.dot`. */
    val pill by style {
        property("display", "inline-flex"); property("align-items", "center"); property("gap", "6px"); property("font-size", "11.5px"); property("font-weight", "700")
        property("padding", "3px 10px"); property("border-radius", "999px"); property("background", "var(--cs-surface-2)"); property("color", "var(--cs-ink-2)")
        className(dot) style { property("width", "7px"); property("height", "7px"); property("border-radius", "999px") }
    }

    /** `.listgrid` — auto-filling responsive grid of list cards. */
    val listgrid by style {
        property("display", "grid"); property("grid-template-columns", "repeat(auto-fill, minmax(260px, 1fr))"); property("gap", "var(--cs-gap)")
    }

    /** `.listcard` — wishlist tile: hover lift, `.cover` (with `.vis` chip), `.c` body (`h3`, `.meta`), dashed `.new` variant. */
    val listcard by style {
        property("background", "var(--cs-surface)"); property("border", "1px solid var(--cs-line)"); property("border-radius", "var(--cs-radius-lg)")
        property("overflow", "hidden"); property("cursor", "pointer"); property("transition", ".14s")
        self + hover style { property("border-color", "var(--cs-line-2)"); property("box-shadow", "var(--cs-shadow)"); property("transform", "translateY(-2px)") }
        className(cover) style {
            property("height", "96px"); property("display", "flex"); property("align-items", "flex-end"); property("padding", "12px")
            property("position", "relative")
            className(vis) style {
                property("font-size", "10.5px"); property("font-weight", "700"); property("padding", "3px 8px"); property("border-radius", "6px")
                property("background", "rgba(255,255,255,.9)"); property("color", "var(--cs-ink-2)")
            }
        }
        className(c) style { property("padding", "14px 15px 16px") }
        "h3" style { property("font-size", "15.5px"); property("font-weight", "800"); property("margin", "0 0 3px"); property("letter-spacing", "-.01em") }
        className(meta) style { property("font-size", "12.5px"); property("color", "var(--cs-muted)") }
        self + className(new) style {
            property("border-style", "dashed"); property("border-color", "var(--cs-line-2)"); property("background", "var(--cs-bg)")
            property("display", "flex"); property("align-items", "center"); property("justify-content", "center"); property("min-height", "180px"); property("color", "var(--cs-muted)")
            property("font-weight", "700"); property("font-size", "13.5px"); property("flex-direction", "column"); property("gap", "8px")
            "svg" style { property("width", "24px"); property("height", "24px") }
            self + hover style { property("color", "var(--cs-accent)"); property("border-color", "var(--cs-accent)"); property("transform", "none"); property("box-shadow", "none") }
        }
    }

    /** `.people` — auto-filling responsive grid of person cards. */
    val people by style {
        property("display", "grid"); property("grid-template-columns", "repeat(auto-fill, minmax(240px, 1fr))"); property("gap", "var(--cs-gap)")
    }

    /** `.person` — discover tile: hover lift, `.av` avatar, `h3` name, `.meta`, `.adm` chip. */
    val person by style {
        property("background", "var(--cs-surface)"); property("border", "1px solid var(--cs-line)"); property("border-radius", "var(--cs-radius-lg)")
        property("padding", "18px"); property("cursor", "pointer"); property("transition", ".14s"); property("text-align", "center")
        self + hover style { property("border-color", "var(--cs-line-2)"); property("box-shadow", "var(--cs-shadow)"); property("transform", "translateY(-2px)") }
        className(av) style { property("width", "56px"); property("height", "56px"); property("border-radius", "999px"); property("margin", "0 auto 12px") }
        "h3" style { property("font-size", "15px"); property("font-weight", "800"); property("margin", "0 0 2px") }
        className(meta) style { property("font-size", "12.5px"); property("color", "var(--cs-muted)") }
        className(adm) style {
            property("display", "inline-block"); property("margin-top", "8px"); property("font-size", "10.5px"); property("font-weight", "700")
            property("padding", "2px 8px"); property("border-radius", "999px"); property("background", "var(--cs-surface-2)"); property("color", "var(--cs-muted)")
        }
    }

    /** `.detail` — item-detail two-column grid (`.gallery .main-img`, `.field` with `.lbl`/`.val`, `h1`, `.pricetag`, `.linkrow`, `.actbar`); collapses ≤760px. */
    val detail by style {
        property("display", "grid"); property("grid-template-columns", "minmax(0, 420px) 1fr"); property("gap", "34px"); property("align-items", "start")
        className(gallery) style {
            className(`main-img`) style { property("width", "100%"); property("aspect-ratio", "1"); property("border-radius", "var(--cs-radius-xl)"); property("object-fit", "cover") }
        }
        className(field) style {
            property("margin-bottom", "20px")
            className(lbl) style {
                property("font-size", "var(--cs-label)"); property("font-weight", "700"); property("letter-spacing", ".08em"); property("text-transform", "uppercase")
                property("color", "var(--cs-faint)"); property("margin-bottom", "6px")
            }
            className(`val`) style { property("font-size", "15px"); property("color", "var(--cs-ink-2)"); property("line-height", "1.55") }
        }
        "h1" style { property("font-size", "var(--cs-h-detail)"); property("font-weight", "800"); property("letter-spacing", "-.02em"); property("margin", "0 0 10px") }
        className(pricetag) style { property("font-size", "22px"); property("font-weight", "800"); property("letter-spacing", "-.01em") }
        className(linkrow) style {
            property("display", "flex"); property("align-items", "center"); property("gap", "9px"); property("font-size", "14px"); property("font-weight", "600")
            property("color", "var(--cs-accent)"); property("text-decoration", "none"); property("padding", "9px 0"); property("border-bottom", "1px solid var(--cs-line)")
            self + lastChild style { property("border-bottom", "none") }
        }
        className(actbar) style { property("display", "flex"); property("align-items", "center"); property("gap", "9px"); property("margin", "22px 0 26px") }
        cssMedia(mediaMaxWidth(760.px)) {
            self style { property("grid-template-columns", "1fr"); property("gap", "20px") }
        }
    }

    /** `.form` — max-width form column. */
    val form by style {
        property("max-width", "540px")
    }

    /** `.fieldset` — labeled field block (its bold `label`). */
    val fieldset by style {
        property("margin-bottom", "18px")
        "label" style { property("display", "block"); property("font-size", "12.5px"); property("font-weight", "700"); property("color", "var(--cs-ink-2)"); property("margin-bottom", "6px") }
    }

    /** `.hint` — muted helper line under a field (the `.danger` variant tints it for inline errors). */
    val hint by style {
        property("font-size", "12px"); property("color", "var(--cs-muted)"); property("margin-top", "5px")
        self + className(danger) style { property("color", "var(--cs-danger)") }
    }

    /** `.priopts` — row holding the priority option chips. */
    val priopts by style {
        property("display", "flex"); property("gap", "8px")
    }

    /** `.priopt` — one priority option chip (active `.on`). */
    val priopt by style {
        property("flex", "1"); property("text-align", "center"); property("font-size", "12.5px"); property("font-weight", "700"); property("padding", "9px"); property("border-radius", "9px")
        property("border", "1px solid var(--cs-line-2)"); property("background", "var(--cs-surface)"); property("cursor", "pointer"); property("color", "var(--cs-muted)")
        self + className(on) style { property("border-color", "var(--cs-accent)"); property("background", "var(--cs-accent-soft)"); property("color", "var(--cs-accent)") }
    }

    /** `.toast` — fixed bottom-center toast (visible `.show`, `.ok` check with `svg`). */
    val toast by style {
        property("position", "fixed"); property("bottom", "24px"); property("left", "50%"); property("transform", "translateX(-50%) translateY(20px)")
        property("background", "var(--cs-ink)"); property("color", "#fff"); property("font-size", "13.5px"); property("font-weight", "600"); property("padding", "12px 18px")
        property("border-radius", "11px"); property("box-shadow", "var(--cs-shadow-lg)"); property("opacity", "0"); property("pointer-events", "none"); property("transition", ".25s")
        property("display", "flex"); property("align-items", "center"); property("gap", "10px"); property("z-index", "200")
        self + className(show) style { property("opacity", "1"); property("transform", "translateX(-50%) translateY(0)") }
        className(ok) style {
            property("color", "#6ee7a8"); property("display", "grid"); property("place-items", "center")
            "svg" style { property("width", "17px"); property("height", "17px") }
        }
    }

    /** `.scrim` — blurred full-screen modal backdrop that centers its dialog. */
    val scrim by style {
        property("position", "fixed"); property("inset", "0"); property("background", "rgba(24,24,27,.4)"); property("backdrop-filter", "blur(3px)")
        property("display", "grid"); property("place-items", "center"); property("z-index", "100"); property("padding", "20px")
    }

    /** `.modal` — centered dialog (`.mhead` with `h2`/`p`, `.mbody`, `.mfoot`, `.tabs` with `button`/`button.on`). */
    val modal by style {
        property("background", "var(--cs-surface)"); property("border-radius", "var(--cs-radius-xl)"); property("width", "min(420px, 100%)")
        property("box-shadow", "var(--cs-shadow-lg)"); property("overflow", "hidden")
        className(mhead) style {
            property("padding", "22px 24px 0")
            "h2" style { property("font-size", "19px"); property("font-weight", "800"); property("margin", "0 0 4px"); property("letter-spacing", "-.01em") }
            "p" style { property("font-size", "13.5px"); property("color", "var(--cs-muted)"); property("margin", "0") }
        }
        className(mbody) style { property("padding", "18px 24px") }
        className(mfoot) style { property("padding", "14px 24px 20px"); property("display", "flex"); property("gap", "9px"); property("justify-content", "flex-end") }
        className(tabs) style {
            property("display", "flex"); property("gap", "4px"); property("background", "var(--cs-bg)"); property("border", "1px solid var(--cs-line-2)")
            property("border-radius", "9px"); property("padding", "3px"); property("margin-bottom", "16px")
            "button" style {
                property("flex", "1"); property("font-family", "inherit"); property("font-size", "13px"); property("font-weight", "700"); property("color", "var(--cs-muted)")
                property("border", "0"); property("background", "none"); property("padding", "8px"); property("border-radius", "6px"); property("cursor", "pointer")
                self + className(on) style { property("background", "var(--cs-surface)"); property("color", "var(--cs-ink)"); property("box-shadow", "var(--cs-shadow-sm)") }
            }
        }
    }

    /** `.content-inner` — centered max-width content column. */
    val `content-inner` by style {
        property("max-width", "var(--cs-content-max)"); property("margin", "0 auto")
    }

    /** `.form-row` — two-up form row laying its children (`> *`) side by side with equal width. */
    val `form-row` by style {
        property("display", "flex"); property("gap", "12px")
        child(self, universal) style { property("flex", "1") }
    }

    /** `.t0` — deterministic media/avatar gradient tint. */
    val t0 by style { property("background", "linear-gradient(135deg,#e7e7f6,#d3d3ef)") }

    /** `.t1` — deterministic media/avatar gradient tint. */
    val t1 by style { property("background", "linear-gradient(135deg,#eae7f4,#dcd6ee)") }

    /** `.t2` — deterministic media/avatar gradient tint. */
    val t2 by style { property("background", "linear-gradient(135deg,#e9eaf0,#d6d8e6)") }

    /** `.t3` — deterministic media/avatar gradient tint. */
    val t3 by style { property("background", "linear-gradient(135deg,#ecebf2,#dbdaea)") }

    /** `.t4` — deterministic media/avatar gradient tint. */
    val t4 by style { property("background", "linear-gradient(135deg,#e8e9ef,#d5d7e4)") }

    /** `.t5` — deterministic media/avatar gradient tint. */
    val t5 by style { property("background", "linear-gradient(135deg,#eaeaf1,#d8d9e7)") }

    /** `.t6` — deterministic media/avatar gradient tint. */
    val t6 by style { property("background", "linear-gradient(135deg,#e6ecf3,#cfdae8)") }

    /** `.t7` — deterministic media/avatar gradient tint. */
    val t7 by style { property("background", "linear-gradient(135deg,#efe9f3,#ddd0ea)") }

    // ---- layout / utility helpers (extracted from former inline `style {}` blocks in views) ----

    /** `.formactions` — bottom form action bar; a trailing `.danger` (Delete) is pushed to the far right. */
    val formactions by style {
        property("display", "flex"); property("align-items", "center"); property("gap", "9px"); property("margin-top", "24px")
        className(danger) style { property("margin-left", "auto") }
    }

    /** `.sectionhead` — in-page section header row: title on the left, an action button on the right. */
    val sectionhead by style {
        property("display", "flex"); property("justify-content", "space-between"); property("align-items", "center"); property("margin", "18px 0 12px")
    }

    /** `.hstack` — generic inline cluster: a centered flex row with an 8px gap (button groups, input+select). */
    val hstack by style {
        property("display", "flex"); property("align-items", "center"); property("gap", "8px")
    }

    /** `.titlepill` — inline row pairing a heading with its trailing priority pill (9px gap). */
    val titlepill by style {
        property("display", "flex"); property("align-items", "center"); property("gap", "9px")
    }

    /** `.nonclickable` — default-cursor marker for card/row components rendered without an open handler. */
    val nonclickable by style { property("cursor", "default") }

    /** `.dot-pri-low` — low-priority dot fill (applied alongside `.dot`). */
    val `dot-pri-low` by style { property("background", "var(--cs-pri-low)") }

    /** `.dot-pri-med` — medium-priority dot fill (applied alongside `.dot`). */
    val `dot-pri-med` by style { property("background", "var(--cs-pri-med)") }

    /** `.dot-pri-high` — high-priority dot fill (applied alongside `.dot`). */
    val `dot-pri-high` by style { property("background", "var(--cs-pri-high)") }

    /** `.dot-ok` — success/reserved dot fill (applied alongside `.dot`). */
    val `dot-ok` by style { property("background", "var(--cs-ok)") }

    /** `.pill-ok` — success/reserved pill fill + text color (applied alongside `.pill`). */
    val `pill-ok` by style { property("background", "var(--cs-ok-soft)"); property("color", "var(--cs-ok)") }

    init {
        // Non-class rules that cannot be `by style` delegates: the `--cs-*` token block, the element
        // reset, `::selection`, the grouped input selector, and the `.empty` family (a `val empty` would
        // clash with SelectorsScope.empty). Change --cs-accent to retheme; soft/hover/ring cascade via
        // color-mix().
        ":root" style {
            property("--cs-accent", "#5B5BD6")
            property("--cs-accent-soft", "color-mix(in srgb, var(--cs-accent) 13%, #fff)")
            property("--cs-accent-hover", "color-mix(in srgb, var(--cs-accent) 86%, #000)")
            property("--cs-accent-ring", "color-mix(in srgb, var(--cs-accent) 22%, transparent)")

            property("--cs-bg", "#FAFAFB")
            property("--cs-surface", "#FFFFFF")
            property("--cs-surface-2", "#F4F4F6")
            property("--cs-ink", "#18181B")
            property("--cs-ink-2", "#3F3F46")
            property("--cs-muted", "#71717A")
            property("--cs-faint", "#A1A1AA")
            property("--cs-line", "#ECECEF")
            property("--cs-line-2", "#E2E2E6")

            property("--cs-pri-high", "var(--cs-accent)")
            property("--cs-pri-med", "#9A9AB0")
            property("--cs-pri-low", "#C3C3D0")
            property("--cs-ok", "#2F8F5B")
            property("--cs-ok-soft", "#E3F3EA")
            property("--cs-danger", "#B42318")

            property("--cs-radius-sm", "7px")
            property("--cs-radius", "10px")
            property("--cs-radius-lg", "14px")
            property("--cs-radius-xl", "18px")

            property("--cs-shadow-sm", "0 1px 2px rgba(24,24,27,.06)")
            property("--cs-shadow", "0 4px 16px -6px rgba(24,24,27,.14)")
            property("--cs-shadow-lg", "0 20px 48px -24px rgba(24,24,27,.4)")

            property("--cs-sidebar-w", "232px")
            property("--cs-topbar-h", "60px")
            property("--cs-content-max", "1080px")
            property("--cs-gap", "14px")

            property("--cs-font", "\"Manrope\", system-ui, -apple-system, \"Segoe UI\", Roboto, sans-serif")
            property("--cs-h1", "26px")
            property("--cs-h-detail", "28px")
            property("--cs-card-title", "14.5px")
            property("--cs-body", "14px")
            property("--cs-small", "12.5px")
            property("--cs-label", "11px")
            property("--cs-weight-strong", "800")
            property("--cs-weight-bold", "700")
        }

        "*" style { property("box-sizing", "border-box") }
        "html, body" style { property("height", "100%") }
        "body" style {
            property("margin", "0"); property("background", "var(--cs-bg)"); property("color", "var(--cs-ink)")
            property("font-family", "var(--cs-font)")
            property("-webkit-font-smoothing", "antialiased"); property("font-size", "var(--cs-body)"); property("line-height", "1.5")
        }
        "button" style { property("font-family", "inherit") }
        "a" style { property("color", "inherit") }
        "::selection" style { property("background", "var(--cs-accent-soft)") }

        // form inputs (grouped `.input, .textarea, select.input` + shared focus ring; `.textarea` extra)
        group(className(input), className(textarea), type("select") + className(input)) style {
            property("width", "100%"); property("font-family", "inherit"); property("font-size", "14px"); property("color", "var(--cs-ink)")
            property("background", "var(--cs-surface)"); property("border", "1px solid var(--cs-line-2)"); property("border-radius", "9px"); property("padding", "10px 12px"); property("outline", "none")
        }
        group(className(input) + focus, className(textarea) + focus, type("select") + className(input) + focus) style {
            property("border-color", "var(--cs-accent)"); property("box-shadow", "0 0 0 3px var(--cs-accent-soft)")
        }
        className(textarea) style { property("resize", "vertical"); property("min-height", "84px"); property("line-height", "1.5") }

        // search disabled state (raw attribute selector — `.search[disabled]`; applied when the Label carries a disabled HTML attribute)
        ".search[disabled]" style { property("opacity", ".5"); property("cursor", "default") }

        // empty states (flat raw — `empty` as a property would clash with SelectorsScope.empty)
        ".empty" style { property("text-align", "center"); property("padding", "60px 20px"); property("color", "var(--cs-muted)") }
        ".empty .ic" style {
            property("width", "56px"); property("height", "56px"); property("border-radius", "var(--cs-radius-lg)"); property("background", "var(--cs-surface-2)")
            property("display", "grid"); property("place-items", "center"); property("margin", "0 auto 16px"); property("color", "var(--cs-faint)")
        }
        ".empty .ic svg" style { property("width", "26px"); property("height", "26px") }
        ".empty h3" style { property("font-size", "17px"); property("font-weight", "800"); property("color", "var(--cs-ink)"); property("margin", "0 0 6px") }
        ".empty p" style { property("font-size", "13.5px"); property("margin", "0 0 18px") }

        StyleSheetsAggregator.addStyleSheet(this)
    }

    /**
     * Forces this object to initialize so the [init] block registers the stylesheet into the
     * [StyleSheetsAggregator]. Nothing else references this object statically (call sites read its `val`
     * names, which are computed lazily), so call this once at JS startup — see `ClientJSPlugin`.
     */
    fun ensureRegistered() {
        // Touching any member triggers Kotlin object initialization; no body needed.
    }
}
