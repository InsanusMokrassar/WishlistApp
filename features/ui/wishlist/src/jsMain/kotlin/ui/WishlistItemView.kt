package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.common.client.ui.components.ListRow
import dev.inmo.wishlist.features.currency.common.utils.formatItemPrice
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H6
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Small
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML read-only view for a single wishlist item. Uses Bootstrap classes. */
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
        val booking by viewModel.bookingState.collectAsState()
        val currencyEnabled by viewModel.currencyEnabledState.collectAsState()
        val currencies by viewModel.currenciesState.collectAsState()
        val selectedCurrency by viewModel.selectedCurrencyState.collectAsState()
        val rates by viewModel.ratesState.collectAsState()

        Div({ classes("container", "py-3") }) {
            Div({ classes("d-flex", "align-items-center", "mb-3", "gap-2") }) {
                BackButton(WishlistStrings.backButton.translation()) { viewModel.onBack() }
                Div({ classes("flex-grow-1") }) {}
                if (isOwner) {
                    Button({
                        classes("btn", "btn-outline-primary")
                        onClick { viewModel.onEditItem() }
                    }) {
                        Text(WishlistStrings.editButton.translation())
                    }
                }
            }

            if (loading) {
                P { Text(WishlistStrings.loading.translation()) }
            } else if (item == null) {
                P({ classes("text-muted") }) { Text(WishlistStrings.loading.translation()) }
            } else {
                val it = item!!

                if (it.description.isNotBlank()) {
                    Div({ classes("mb-3") }) {
                        H6({ classes("text-muted") }) { Text(WishlistStrings.descriptionLabel.translation()) }
                        P { Text(it.description) }
                    }
                }

                if (it.amount != 1u) {
                    Div({ classes("mb-3") }) {
                        H6({ classes("text-muted") }) { Text(WishlistStrings.amountLabel.translation()) }
                        P { Text("×${it.amount}") }
                    }
                }

                if (currencyEnabled && currencies.isNotEmpty()) {
                    CurrencySelector(
                        currencies = currencies,
                        selected = selectedCurrency,
                        onCurrencySelected = viewModel::onCurrencySelected
                    )
                }

                Div({ classes("mb-3") }) {
                    H6({ classes("text-muted") }) { Text(WishlistStrings.priceLabel.translation()) }
                    if (it.approximatePrice != null) {
                        P { Text(formatItemPrice(it.approximatePrice, it.priceUnits, selectedCurrency, rates)) }
                    } else {
                        P({ classes("text-muted") }) { Text(WishlistStrings.noPrice.translation()) }
                    }
                }

                Div({ classes("mb-3") }) {
                    H6({ classes("text-muted") }) { Text(WishlistStrings.priorityLabel.translation()) }
                    P { PriorityBadge(it.priority) }
                }

                // Booking section is shown only to authorized non-owner users: the ViewModel keeps
                // `booking` null for the owner and for anonymous callers (server-enforced).
                booking?.let { state ->
                    Div({ classes("mb-3") }) {
                        H6({ classes("text-muted") }) { Text(WishlistStrings.bookingLabel.translation()) }
                        when {
                            state.bookedByMe -> {
                                P({ classes("text-success") }) { Text(WishlistStrings.bookedByYou.translation()) }
                                Button({
                                    classes("btn", "btn-outline-danger")
                                    if (loading) disabled()
                                    onClick { viewModel.onCancelBooking() }
                                }) {
                                    Text(WishlistStrings.cancelBookingButton.translation())
                                }
                            }
                            state.booked -> {
                                P({ classes("text-warning") }) { Text(WishlistStrings.bookedByOther.translation()) }
                            }
                            else -> {
                                P({ classes("text-muted") }) { Text(WishlistStrings.notBooked.translation()) }
                                Button({
                                    classes("btn", "btn-primary")
                                    if (loading) disabled()
                                    onClick { viewModel.onBook() }
                                }) {
                                    Text(WishlistStrings.bookButton.translation())
                                }
                            }
                        }
                    }
                }

                Div({ classes("mb-3") }) {
                    H6({ classes("text-muted") }) { Text(WishlistStrings.linksLabel.translation()) }
                    if (it.links.isEmpty()) {
                        P({ classes("text-muted") }) { Text(WishlistStrings.noLinks.translation()) }
                    } else {
                        Ul({ classes("list-group") }) {
                            it.links.forEach { link ->
                                ListRow {
                                    A(href = link, attrs = { attr("target", "_blank") }) {
                                        Text(link)
                                    }
                                }
                            }
                        }
                    }
                }

                Div({ classes("mb-3") }) {
                    H6({ classes("text-muted") }) { Text(WishlistStrings.imagesLabel.translation()) }
                    if (it.imageIds.isEmpty()) {
                        P({ classes("text-muted") }) { Text(WishlistStrings.noImages.translation()) }
                    } else {
                        Div({ classes("d-flex", "flex-wrap", "gap-2") }) {
                            it.imageIds.forEach { id ->
                                Img(src = viewModel.imageUrl(id), alt = "") {
                                    classes("rounded", "border")
                                    attr("style", "max-width: 200px; max-height: 200px; object-fit: cover;")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
