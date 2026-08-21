package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.common.currencyLabel
import com.arshadshah.nimaz.core.common.currencySymbolOf
import com.arshadshah.nimaz.core.common.formatCurrency
import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.domain.model.ZakatDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.molecules.NimazAmountField
import com.arshadshah.nimaz.presentation.components.organisms.NimazListPicker
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuDivider
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.organisms.NimazPickerItem
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsItem
import com.arshadshah.nimaz.presentation.components.molecules.ZakatHeroStat
import com.arshadshah.nimaz.presentation.components.molecules.ZakatSummaryHero
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.settings.ZakatSettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.settings.ZakatSettingsUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.ZakatSettingsViewModel

/**
 * The zakat basis: which nisab applies, what gold and silver are worth, and which currency
 * every figure is read in.
 *
 * These four used to sit in an accordion halfway down `ZakatCalculatorScreen`, between the
 * assets the user was typing and the liabilities they were about to — three of them persisted
 * preferences rendered as if they were part of the form. Reading a gold rate off a website is
 * not part of calculating this year's zakat; it is a thing you set once and then stop thinking
 * about, which is what a settings screen is for.
 *
 * ## Ordering
 * 1. **The threshold itself** — a live hero, so every control below has a visible consequence.
 *    The figure is the one `ZakatCalculator` will compare net wealth against, derived through
 *    the same function.
 * 2. **Nisab basis** — gold or silver. It decides which of the two prices below actually
 *    matters, so it comes before them.
 * 3. **Metal prices** — both, always, regardless of basis: gold and silver held as *assets*
 *    are valued from these whichever basis is selected, so hiding the unselected one would
 *    hide a price that is still in the sum.
 * 4. **Currency** — the unit the rest of it is read in.
 *
 * Nothing here is a "save" — every control writes straight through to DataStore, and an open
 * calculator picks the change up through its own observation of the same preferences.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ZakatSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showCurrencyPicker by rememberSaveable { mutableStateOf(false) }

    NimazScreenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.zakat_settings),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // ── 1. The threshold these settings produce ──────────────────────────────────
            item { NisabPreviewHero(state = state) }

            // ── 2. Which basis ──────────────────────────────────────────────────────────
            item {
                NimazSectionHeader(title = stringResource(R.string.zakat_section_nisab))
                // What nisab *is*, above the choice — the same line the calculator's accordion
                // carried when these controls lived there.
                Text(
                    text = stringResource(R.string.zakat_section_nisab_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NisabOptionCard(
                        label = stringResource(R.string.gold),
                        // Formatted, not truncated: `price.toInt()` rendered a silver price of
                        // 0.80 as "0", and a hardcoded "$" ignored the chosen currency.
                        subtitle = stringResource(
                            R.string.zakat_nisab_gold_subtitle,
                            formatCurrency(state.goldPricePerGram, state.currency),
                        ),
                        isSelected = state.nisabType == NisabType.GOLD,
                        accentColor = NimazColors.ZakatColors.Gold,
                        onClick = {
                            viewModel.onEvent(ZakatSettingsEvent.SetNisabType(NisabType.GOLD))
                        },
                        modifier = Modifier.weight(1f),
                    )
                    NisabOptionCard(
                        label = stringResource(R.string.silver),
                        subtitle = stringResource(
                            R.string.zakat_nisab_silver_subtitle,
                            formatCurrency(state.silverPricePerGram, state.currency),
                        ),
                        isSelected = state.nisabType == NisabType.SILVER,
                        accentColor = NimazColors.ZakatColors.Silver,
                        onClick = {
                            viewModel.onEvent(ZakatSettingsEvent.SetNisabType(NisabType.SILVER))
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Text(
                    text = stringResource(R.string.zakat_settings_nisab_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            // ── 3. What the metals are worth ────────────────────────────────────────────
            item { NimazSectionHeader(title = stringResource(R.string.zakat_metal_prices)) }
            item {
                NimazMenuGroup {
                    PriceRow(
                        label = stringResource(R.string.zakat_gold_price_label),
                        price = state.goldPricePerGram,
                        currency = state.currency,
                        onPriceChange = {
                            viewModel.onEvent(ZakatSettingsEvent.SetGoldPrice(it))
                        },
                    )
                    NimazMenuDivider(inset = false)
                    PriceRow(
                        label = stringResource(R.string.zakat_silver_price_label),
                        price = state.silverPricePerGram,
                        currency = state.currency,
                        onPriceChange = {
                            viewModel.onEvent(ZakatSettingsEvent.SetSilverPrice(it))
                        },
                    )
                    // Inside the group, attached to the two fields it is about: these are
                    // starting points, and a default read as a market rate is how a zakat
                    // figure goes quietly wrong.
                    Text(
                        text = stringResource(R.string.zakat_metal_prices_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                    )
                }
            }

            // ── 4. The unit everything is read in ───────────────────────────────────────
            item { NimazSectionHeader(title = stringResource(R.string.zakat_currency)) }
            item {
                NimazMenuGroup {
                    NimazSettingsItem(
                        title = stringResource(R.string.zakat_currency),
                        subtitle = currencyLabel(state.currency),
                        value = state.currency,
                        onClick = { showCurrencyPicker = true },
                        showArrow = true,
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showCurrencyPicker) {
        NimazListPicker(
            title = stringResource(R.string.zakat_currency),
            // Codes only: `java.util.Currency` resolves the display name and symbol in the
            // user's locale, so 32 currencies add no translated strings.
            items = ZakatDefaults.CURRENCIES.map { code ->
                NimazPickerItem(value = code, title = code, description = currencyLabel(code))
            },
            selected = state.currency,
            onSelected = { viewModel.onEvent(ZakatSettingsEvent.SetCurrency(it)) },
            onDismiss = { showCurrencyPicker = false },
        )
    }
}

/**
 * The threshold the settings below add up to, in the calculator's own hero.
 *
 * Deliberately the same component the calculator crowns its result with — the point of the
 * screen is the number `ZakatCalculator` will compare wealth against, and showing it in a
 * different shape here would read as a different quantity.
 */
@Composable
private fun NisabPreviewHero(
    state: ZakatSettingsUiState,
    modifier: Modifier = Modifier,
) {
    ZakatSummaryHero(
        modifier = modifier,
        label = stringResource(R.string.nisab_threshold),
        amount = formatCurrency(state.nisabValue, state.currency),
        // "87.48g @ $65.00/g" — the working, not a restatement of the figure above it.
        subtitle = stringResource(
            when (state.nisabType) {
                NisabType.GOLD -> R.string.zakat_nisab_gold_subtitle
                NisabType.SILVER -> R.string.zakat_nisab_silver_subtitle
            },
            formatCurrency(
                when (state.nisabType) {
                    NisabType.GOLD -> state.goldPricePerGram
                    NisabType.SILVER -> state.silverPricePerGram
                },
                state.currency,
            ),
        ),
        // A threshold priced at zero has not been met, it has failed to be established —
        // `ZakatCalculator` treats it that way too, so the hero must not present it as a
        // full-strength figure.
        muteAmount = state.nisabValue <= 0.0,
        // The two prices and the unit, with the basis's own price accented — so the tile row
        // says which of the two figures the threshold above was actually derived from.
        stats = listOf(
            ZakatHeroStat(
                value = formatCurrency(state.goldPricePerGram, state.currency),
                label = stringResource(R.string.gold),
                accented = state.nisabType == NisabType.GOLD,
            ),
            ZakatHeroStat(
                value = formatCurrency(state.silverPricePerGram, state.currency),
                label = stringResource(R.string.silver),
                accented = state.nisabType == NisabType.SILVER,
            ),
            ZakatHeroStat(
                value = state.currency,
                label = stringResource(R.string.zakat_currency),
            ),
        ),
    )
}

/** One editable per-gram price, laid out as a settings row. */
@Composable
private fun PriceRow(
    label: String,
    price: Double,
    currency: String,
    onPriceChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        NimazAmountField(
            value = price,
            onValueChange = onPriceChange,
            currencySymbol = currencySymbolOf(currency),
        )
    }
}

/**
 * One of the two nisab bases, as a selectable card.
 *
 * Two peers on the page background: elevation gives each option a card boundary, the accent
 * fill + border carries the selection. FILLED (not ELEVATED) because only Material's `Card`
 * renders a border — `ElevatedCard` has no border slot, so an ELEVATED card would drop
 * `activeBorder` silently. Elevation is passed explicitly to keep the lift.
 */
@Composable
private fun NisabOptionCard(
    label: String,
    subtitle: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    NimazCard(
        onClick = onClick,
        modifier = modifier,
        style = NimazCardStyle.FILLED,
        elevation = 1.dp,
        shape = RoundedCornerShape(14.dp),
        selected = isSelected,
        colors = NimazCardDefaults.selectable(
            container = surface,
            // Composited to an OPAQUE colour on purpose. A translucent container on a
            // shadow-casting surface makes the RenderNode non-opaque, so Android fills the
            // shadow's interior behind it — which showed up as a pale box in the middle of
            // the selected card.
            activeContainer = accentColor.copy(alpha = 0.15f).compositeOver(surface),
            // Border alpha is safe — it is stroked on top, not behind the shadow.
            activeBorder = accentColor.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
