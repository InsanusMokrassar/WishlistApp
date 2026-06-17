package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.CalmIcon
import dev.inmo.wishlist.features.common.client.ui.components.CalmIcons
import dev.inmo.wishlist.features.common.client.ui.components.tintClass
import dev.inmo.wishlist.features.currency.common.utils.formatItemPriceWithAmount
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.wishlist.common.models.displayText
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * JS Compose-HTML read-only view for a single wishlist item (Calm Studio detail layout).
 *
 * Renders a two-column `.detail`: a `.gallery` (the item's images, or a gradient placeholder) beside a
 * column with the title, priority pill, owner Edit / visitor Copy actions, any registered
 * item-scoped extra views (e.g. booking/reserve), and the description / approximate price / links
 * fields. Class names mirror the design skill's `app.jsx` so the Calm Studio shell CSS styles it.
 */
class WishlistItemView(
    chain: NavigationChain<ViewConfig>,
    config: WishlistItemViewConfig,
) : ComposeView<WishlistItemViewConfig, ViewConfig, WishlistItemViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: WishlistItemViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@WishlistItemView)
    }

    override val title: String
        @Composable get() {
            val item by viewModel.itemState.collectAsState()
            return item?.title ?: WishlistStrings.viewItemTitle.translation()
        }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val item by viewModel.itemState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val isOwner by viewModel.isOwnerState.collectAsState()
        val canCopy by viewModel.canCopyState.collectAsState()
        val currencyEnabled by viewModel.currencyEnabledState.collectAsState()
        val currencies by viewModel.currenciesState.collectAsState()
        val selectedCurrency by viewModel.selectedCurrencyState.collectAsState()
        val rates by viewModel.ratesState.collectAsState()

        Div({ classes("content-inner") }) {
            when {
                loading || item == null -> P({ classes("subline") }) { Text(WishlistStrings.loading.translation()) }
                else -> {
                    val it = item!!
                    Div({ classes("detail") }) {
                        Div({ classes("gallery") }) {
                            val firstImage = it.imageIds.firstOrNull()
                            if (firstImage != null) {
                                Img(src = viewModel.imageUrl(firstImage), alt = "") {
                                    classes("main-img")
                                    style { property("object-fit", "cover") }
                                }
                                if (it.imageIds.size > 1) {
                                    Div({
                                        style {
                                            property("display", "flex")
                                            property("flex-wrap", "wrap")
                                            property("gap", "8px")
                                            property("margin-top", "12px")
                                        }
                                    }) {
                                        it.imageIds.drop(1).forEach { id ->
                                            Img(src = viewModel.imageUrl(id), alt = "") {
                                                style {
                                                    property("width", "72px")
                                                    property("height", "72px")
                                                    property("object-fit", "cover")
                                                    property("border-radius", "10px")
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Div({ classes("main-img", tintClass(it.id.long)) })
                            }
                        }

                        Div {
                            H1 { Text(it.title) }

                            Div({
                                style {
                                    property("display", "flex")
                                    property("align-items", "center")
                                    property("gap", "10px")
                                    property("margin-bottom", "18px")
                                }
                            }) {
                                PriorityBadge(it.priority)
                            }

                            if (isOwner || canCopy) {
                                Div({ classes("actbar") }) {
                                    if (isOwner) {
                                        Button({
                                            classes("btn")
                                            onClick { viewModel.onEditItem() }
                                        }) {
                                            CalmIcon(CalmIcons.edit)
                                            Text(WishlistStrings.editButton.translation())
                                        }
                                    }
                                    if (canCopy) {
                                        Button({
                                            classes("btn", "primary")
                                            onClick { viewModel.onCopyItem() }
                                        }) {
                                            Text(WishlistStrings.copyItemButton.translation())
                                        }
                                    }
                                }
                            }

                            // Each registered WishlistAdditionalConfigsProvider (e.g. booking/reserve)
                            // is drawn inline through the shared WishlistItemAdditionalConfigView.
                            if (viewModel.additionalConfigsProviders.isNotEmpty()) {
                                Div({ classes("actbar") }) {
                                    viewModel.additionalConfigsProviders.forEach { provider ->
                                        WishlistItemAdditionalConfigView(provider, it, this@WishlistItemView)
                                    }
                                }
                            }

                            if (it.description.isNotBlank()) {
                                Div({ classes("field") }) {
                                    Div({ classes("lbl") }) { Text(WishlistStrings.descriptionLabel.translation()) }
                                    Div({ classes("val") }) { Text(it.description) }
                                }
                            }

                            Div({ classes("field") }) {
                                Div({ classes("lbl") }) { Text(WishlistStrings.priceLabel.translation()) }
                                if (currencyEnabled && currencies.isNotEmpty()) {
                                    CurrencySelector(
                                        currencies = currencies,
                                        selected = selectedCurrency,
                                        onCurrencySelected = viewModel::onCurrencySelected
                                    )
                                }
                                val priceText = formatItemPriceWithAmount(
                                    it.approximatePrice, it.priceUnits, it.amount, selectedCurrency, rates
                                )
                                Div({ classes("pricetag") }) {
                                    Text(priceText.ifEmpty { WishlistStrings.noPrice.translation() })
                                }
                            }

                            Div({ classes("field") }) {
                                Div({ classes("lbl") }) { Text(WishlistStrings.linksLabel.translation()) }
                                if (it.links.isEmpty()) {
                                    Div({ classes("val") }) { Text(WishlistStrings.noLinks.translation()) }
                                } else {
                                    it.links.forEach { link ->
                                        A(href = link.url, attrs = {
                                            classes("linkrow")
                                            attr("target", "_blank")
                                            attr("rel", "noreferrer")
                                        }) {
                                            Text(link.displayText)
                                            CalmIcon(CalmIcons.ext)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
