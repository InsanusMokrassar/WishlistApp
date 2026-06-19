package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.ActionBar
import dev.inmo.wishlist.features.common.client.ui.components.CalmButton
import dev.inmo.wishlist.features.common.client.ui.components.CalmButtonVariant
import dev.inmo.wishlist.features.common.client.ui.components.CalmIcons
import dev.inmo.wishlist.features.common.client.ui.components.ContentColumn
import dev.inmo.wishlist.features.common.client.ui.components.DetailField
import dev.inmo.wishlist.features.common.client.ui.components.DetailLayout
import dev.inmo.wishlist.features.common.client.ui.components.DetailMedia
import dev.inmo.wishlist.features.common.client.ui.components.LinkRow
import dev.inmo.wishlist.features.common.client.ui.components.PriceTag
import dev.inmo.wishlist.features.common.client.ui.components.Subline
import dev.inmo.wishlist.features.common.client.ui.components.tintClass
import dev.inmo.wishlist.features.currency.common.utils.formatItemPriceWithAmount
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.wishlist.common.models.displayText
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * JS Compose-HTML read-only view for a single wishlist item (Calm Studio detail layout).
 *
 * Renders a two-column `.detail`: a `.gallery` (the item's images, or a gradient placeholder) beside a
 * column with the title, priority pill, owner Edit / visitor Copy actions, any registered
 * item-scoped extra views (e.g. booking/reserve), and the description / approximate price / links
 * fields. Composed from the shared Calm Studio components ([DetailLayout], [DetailMedia],
 * [DetailField], [PriceTag], [LinkRow], [ActionBar], [CalmButton]) so the Calm Studio shell CSS styles it.
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

        ContentColumn {
            when {
                loading || item == null -> Subline(WishlistStrings.loading.translation())
                else -> {
                    val it = item!!
                    DetailLayout(
                        gallery = {
                            val firstImage = it.imageIds.firstOrNull()
                            if (firstImage != null) {
                                Img(src = viewModel.imageUrl(firstImage), alt = "") {
                                    classes(CalmStudioStyleSheet.`main-img`)
                                }
                                if (it.imageIds.size > 1) {
                                    Div({ classes(WishlistItemViewStylesheet.thumbStrip) }) {
                                        it.imageIds.drop(1).forEach { id ->
                                            Img(src = viewModel.imageUrl(id), alt = "") {
                                                classes(WishlistItemViewStylesheet.thumbImg)
                                            }
                                        }
                                    }
                                }
                            } else {
                                DetailMedia(tintClass(it.id.long))
                            }
                        },
                        body = {
                            H1 { Text(it.title) }

                            Div({ classes(WishlistItemViewStylesheet.priorityRow) }) {
                                PriorityBadge(it.priority)
                            }

                            if (isOwner || canCopy) {
                                ActionBar {
                                    if (isOwner) {
                                        CalmButton(
                                            text = WishlistStrings.editButton.translation(),
                                            onClick = { viewModel.onEditItem() },
                                            leadingIcon = CalmIcons.edit,
                                        )
                                    }
                                    if (canCopy) {
                                        CalmButton(
                                            text = WishlistStrings.copyItemButton.translation(),
                                            onClick = { viewModel.onCopyItem() },
                                            variant = CalmButtonVariant.Primary,
                                        )
                                    }
                                }
                            }

                            // Each registered WishlistAdditionalConfigsProvider (e.g. booking/reserve)
                            // is drawn inline through the shared WishlistItemAdditionalConfigView.
                            if (viewModel.additionalConfigsProviders.isNotEmpty()) {
                                ActionBar {
                                    viewModel.additionalConfigsProviders.forEach { provider ->
                                        WishlistItemAdditionalConfigView(provider, it, this@WishlistItemView)
                                    }
                                }
                            }

                            if (it.description.isNotBlank()) {
                                DetailField(WishlistStrings.descriptionLabel.translation(), it.description)
                            }

                            DetailField(WishlistStrings.priceLabel.translation()) {
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
                                PriceTag(priceText.ifEmpty { WishlistStrings.noPrice.translation() })
                            }

                            DetailField(WishlistStrings.linksLabel.translation()) {
                                if (it.links.isEmpty()) {
                                    Div({ classes(CalmStudioStyleSheet.`val`) }) { Text(WishlistStrings.noLinks.translation()) }
                                } else {
                                    it.links.forEach { link ->
                                        LinkRow(text = link.displayText, href = link.url)
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
