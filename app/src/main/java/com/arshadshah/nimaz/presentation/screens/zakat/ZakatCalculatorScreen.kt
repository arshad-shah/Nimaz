package com.arshadshah.nimaz.presentation.screens.zakat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.core.share.ContentShareManager
import com.arshadshah.nimaz.core.share.Shareables
import com.arshadshah.nimaz.domain.calendar.HijriDateCalculator
import com.arshadshah.nimaz.core.common.currencySymbolOf
import com.arshadshah.nimaz.core.common.formatCurrency
import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButtonStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconWell
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconWellSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcons
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.molecules.NimazAccordion
import com.arshadshah.nimaz.presentation.components.molecules.NimazAmountField
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorDefaults
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorState
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import com.arshadshah.nimaz.presentation.components.molecules.ZakatHeroStat
import com.arshadshah.nimaz.presentation.components.molecules.ZakatHeroStatus
import com.arshadshah.nimaz.presentation.components.molecules.ZakatSummaryHero
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.currentWindowSizeClass
import com.arshadshah.nimaz.presentation.theme.isCompact
import com.arshadshah.nimaz.presentation.viewmodel.tools.ZakatCalculatorUiState
import com.arshadshah.nimaz.presentation.viewmodel.tools.ZakatEvent
import com.arshadshah.nimaz.presentation.viewmodel.tools.ZakatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatCalculatorScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ZakatViewModel = hiltViewModel()
) {
    val state by viewModel.calculatorState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    NimazScreenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.zakat_calculator),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { viewModel.onEvent(ZakatEvent.ClearAll) }) {
                        NimazIcon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.cd_reset)
                        )
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        NimazIcon(
                            imageVector = Icons.Default.History,
                            contentDescription = stringResource(R.string.cd_history)
                        )
                    }
                    // The nisab basis, the metal prices and the currency used to be an
                    // accordion in the middle of this form. They are persisted preferences,
                    // not figures typed per calculation, so they live on their own screen —
                    // reachable from here the same way the Quran reader reaches its settings.
                    IconButton(onClick = onNavigateToSettings) {
                        NimazIcon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = stringResource(R.string.zakat_settings)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        val windowSizeClass = currentWindowSizeClass()

        if (windowSizeClass.isCompact) {
            ZakatCompactContent(
                state = state,
                viewModel = viewModel,
                onNavigateToSettings = onNavigateToSettings,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            ZakatTabletContent(
                state = state,
                viewModel = viewModel,
                onNavigateToSettings = onNavigateToSettings,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

@Composable
private fun ZakatCompactContent(
    state: ZakatCalculatorUiState,
    viewModel: ZakatViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val onShareCalculation = rememberZakatShareAction(state)

    // Collapse past a threshold, but **re-expand only at a true top**, and the asymmetry is the
    // whole point rather than a nicety.
    //
    // The hero sits above the list and the list takes the height the hero leaves, so the trigger
    // is reading a scroll position that the trigger's own outcome changes. With one symmetric
    // threshold that is a loop: scrolling past it collapses the hero, the taller viewport lets the
    // list clamp its offset back under the threshold, the hero expands, the shorter viewport pushes
    // the offset back over it. Whether that settles expanded, settles collapsed or flickers depends
    // on the exact content height, which is why it presented as a hero that simply never
    // collapsed.
    //
    // Re-expanding only at offset 0 breaks it, because clamping can lower the offset but never
    // raise it. Where collapsing frees enough room for the whole form, the clamp lands on 0, the
    // hero expands, and nothing moves it again — expanded is the correct answer there anyway.
    var collapsed by remember { mutableStateOf(false) }
    // Written from an effect, never during composition. A `collapsed = …` in the composition body
    // compiles and even mostly works, and it is the shape that turns a layout that feeds back into
    // its own trigger from a settling loop into a non-settling one.
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                collapsed = when {
                    index == 0 && offset == 0 -> false
                    index > 0 || offset > HeroCollapseThresholdPx -> true
                    else -> collapsed
                }
            }
    }

    Column(modifier = modifier) {
        // Above the LazyColumn, not inside it. The total is what the whole task is about, and a
        // hero that scrolls away loses it exactly when the numbers being typed are changing it.
        ZakatResultSummaryCard(
            zakatDue = state.calculation?.zakatDue ?: 0.0,
            nisabValue = state.calculation?.nisabValue ?: 0.0,
            netWealth = state.calculation?.netWorth ?: 0.0,
            isAboveNisab = state.calculation?.isAboveNisab ?: false,
            nisabType = state.nisabType,
            currency = state.currency,
            collapsed = collapsed,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Each accordion header carries its running subtotal, so the shape of the
            // calculation is legible before anything is opened.
            item {
                NimazAccordion(
                    title = stringResource(R.string.assets),
                    subtitle = stringResource(R.string.zakat_section_assets_subtitle),
                    initiallyExpanded = true,
                    trailing = {
                        SubtotalLabel(
                            amount = state.assetsTotal(),
                            currency = state.currency,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                ) {
                    AssetInputCards(state = state, viewModel = viewModel)
                }
            }
            item {
                NimazAccordion(
                    title = stringResource(R.string.zakat_section_deducted),
                    subtitle = stringResource(R.string.zakat_section_deducted_subtitle),
                    trailing = {
                        SubtotalLabel(
                            amount = state.liabilities.total,
                            currency = state.currency,
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                ) {
                    LiabilityInputCards(state = state, viewModel = viewModel)
                }
            }
            // The basis is *reported* here and *set* in settings. It stays on the form
            // because the threshold is what decides whether anything is owed at all — a
            // reader who cannot see it cannot tell why the total says zero — but nothing
            // about it is editable in the middle of typing thirteen figures.
            item {
                NisabBasisRow(
                    nisabType = state.nisabType,
                    nisabValue = state.calculation?.nisabValue ?: state.nisabValue,
                    currency = state.currency,
                    onClick = onNavigateToSettings,
                )
            }

            // INLINE, and above the result rather than in place of the form: every figure the
            // user typed is still on screen and still valid, and losing an afternoon of asset
            // entries to report a failed sum would be far worse than the failure.
            state.error?.let { error ->
                item {
                    NimazErrorState(
                        title = stringResource(error.message),
                        kind = error.kind,
                        variant = NimazErrorVariant.INLINE,
                        primaryAction = NimazErrorDefaults.retry(
                            onRetry = { viewModel.onEvent(ZakatEvent.Recalculate) },
                            label = stringResource(R.string.try_again),
                        ),
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }

            state.calculation?.let { calculation ->
                item {
                    BreakdownCard(
                        totalAssets = calculation.totalAssets,
                        totalLiabilities = calculation.totalLiabilities,
                        netWorth = calculation.netWorth,
                        nisabValue = calculation.nisabValue,
                        isAboveNisab = calculation.isAboveNisab,
                        zakatDue = calculation.zakatDue,
                        currency = state.currency,
                        expanded = state.showBreakdown,
                        onToggleExpanded = { viewModel.onEvent(ZakatEvent.ToggleBreakdown) },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Save and share live here because the screen had nowhere good for them, and the
        // keyboard covering them is fine: they are pressed *after* typing, not during. The
        // total deliberately is not repeated here — showing €1,284.50 twice means the eye
        // never settles, and a bottom bar is exactly where the keyboard would hide it.
        ZakatActionBar(
            enabled = state.calculation != null,
            onSave = { viewModel.onEvent(ZakatEvent.SaveCalculation) },
            onShare = onShareCalculation,
        )
    }
}

/**
 * How far the list must scroll before the hero collapses, in pixels.
 *
 * A raw pixel threshold rather than a `Dp` because `firstVisibleItemScrollOffset` is in pixels;
 * converting per frame to compare against a `Dp` would be work for no accuracy, and the exact
 * distance is a feel decision, not a layout measurement.
 */
private const val HeroCollapseThresholdPx = 120

/**
 * The assets subtotal, with gold and silver valued at the prices currently in the form.
 *
 * `assets.total` counts only the cash-like rows — gold and silver are stored as *grams*, so
 * without the two multiplications the header would report a figure that disagrees with the
 * breakdown by the whole value of someone's jewellery.
 */
private fun ZakatCalculatorUiState.assetsTotal(): Double =
    assets.total +
            (assets.goldGrams * goldPricePerGram) +
            (assets.silverGrams * silverPricePerGram)

/**
 * The share action, built once for both size classes.
 *
 * A no-op until there is a calculation: sharing a card of zeroes would be worse than the button
 * doing nothing, and the bar disables itself in that state anyway.
 */
@Composable
private fun rememberZakatShareAction(state: ZakatCalculatorUiState): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Resolved in composition, not inside the click: `context.getString` from a callback does
    // not re-resolve across a configuration change, so a card shared after switching language
    // would carry the previous locale's basis name.
    val basis = stringResource(
        when (state.nisabType) {
            NisabType.GOLD -> R.string.gold
            NisabType.SILVER -> R.string.silver
        }
    )
    return {
        val calculation = state.calculation
        if (calculation != null) {
            scope.launch {
                ContentShareManager.shareBranded(
                    context,
                    Shareables.zakat(
                        context = context,
                        due = formatCurrency(calculation.zakatDue, state.currency),
                        assets = formatCurrency(calculation.totalAssets, state.currency),
                        deducted = formatCurrency(calculation.totalLiabilities, state.currency),
                        net = formatCurrency(calculation.netWorth, state.currency),
                        nisab = formatCurrency(calculation.nisabValue, state.currency),
                        // The Hijri year the calculation belongs to — zakat is owed on a lunar
                        // year, so the Gregorian one would label it with the wrong period.
                        yearLabel = HijriDateCalculator.today().year.toString(),
                        basis = basis,
                        aboveNisab = calculation.isAboveNisab,
                    ),
                )
            }
        }
    }
}

/** A running subtotal in an accordion header. */
@Composable
private fun SubtotalLabel(amount: Double, currency: String, color: Color) {
    Text(
        text = formatCurrency(amount, currency),
        style = MaterialTheme.typography.bodyMedium,
        color = color,
    )
}

/**
 * Save and share, pinned below the form.
 *
 * Disabled until there is something to act on: saving or sharing a calculation that does not
 * exist yet would either write an empty row or share a card of zeroes.
 */
@Composable
private fun ZakatActionBar(
    enabled: Boolean,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    NimazCard(
        modifier = Modifier.fillMaxWidth(),
        style = NimazCardStyle.FILLED,
        tone = NimazTone.NEUTRAL,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NimazButton(
                text = stringResource(R.string.zakat_save_this_year),
                onClick = onSave,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            NimazIconButton(
                icon = Icons.Default.IosShare,
                onClick = onShare,
                enabled = enabled,
                contentDescription = stringResource(R.string.zakat_share),
                style = NimazIconButtonStyle.FILLED_TONAL,
            )
        }
    }
}

@Composable
private fun ZakatTabletContent(
    state: ZakatCalculatorUiState,
    viewModel: ZakatViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onShareCalculation = rememberZakatShareAction(state)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Result card spans full width
        ZakatResultSummaryCard(
            zakatDue = state.calculation?.zakatDue ?: 0.0,
            nisabValue = state.calculation?.nisabValue ?: 0.0,
            netWealth = state.calculation?.netWorth ?: 0.0,
            isAboveNisab = state.calculation?.isAboveNisab ?: false,
            nisabType = state.nisabType,
            currency = state.currency
        )

        // Reported, not editable — the basis is set on the zakat settings screen.
        NisabBasisRow(
            nisabType = state.nisabType,
            nisabValue = state.calculation?.nisabValue ?: state.nisabValue,
            currency = state.currency,
            onClick = onNavigateToSettings,
        )

        // Two columns: Assets left, Liabilities right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Assets column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NimazSectionHeader(
                    title = stringResource(R.string.assets),
                    trailingContent = {
                        SubtotalLabel(
                            amount = state.assetsTotal(),
                            currency = state.currency,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                )
                AssetInputCards(state = state, viewModel = viewModel)
            }

            // Liabilities column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NimazSectionHeader(
                    title = stringResource(R.string.liabilities),
                    trailingContent = {
                        SubtotalLabel(
                            amount = state.liabilities.total,
                            currency = state.currency,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                )
                LiabilityInputCards(state = state, viewModel = viewModel)
            }
        }

        // Breakdown spans full width
        state.calculation?.let { calculation ->
            BreakdownCard(
                totalAssets = calculation.totalAssets,
                totalLiabilities = calculation.totalLiabilities,
                netWorth = calculation.netWorth,
                nisabValue = calculation.nisabValue,
                isAboveNisab = calculation.isAboveNisab,
                zakatDue = calculation.zakatDue,
                currency = state.currency,
                expanded = state.showBreakdown,
                onToggleExpanded = { viewModel.onEvent(ZakatEvent.ToggleBreakdown) },
            )
        }

        // The tablet layout does not collapse its hero — there is no scroll pressure on a wide
        // screen where the whole form fits — but save and share live in the same bar, so there is
        // one place to look for them whatever the size class.
        ZakatActionBar(
            enabled = state.calculation != null,
            onSave = { viewModel.onEvent(ZakatEvent.SaveCalculation) },
            onShare = onShareCalculation,
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * The nisab basis and the threshold it prices out to, as a row into the settings screen.
 *
 * A `NimazMenuItem`, not a card wrapped in `Modifier.clickable` — a wrapping clickable paints a
 * sharp-cornered ripple that ignores the card radius.
 */
@Composable
private fun NisabBasisRow(
    nisabType: NisabType,
    nisabValue: Double,
    currency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val basis = stringResource(
        when (nisabType) {
            NisabType.GOLD -> R.string.gold
            NisabType.SILVER -> R.string.silver
        }
    )
    NimazMenuItem(
        modifier = modifier,
        title = stringResource(R.string.zakat_section_nisab),
        // "Gold · €5,687.10" — the basis and what it comes to, which together are the whole
        // of what this row has to say.
        subtitle = stringResource(
            R.string.settings_value_with_qualifier,
            basis,
            formatCurrency(nisabValue, currency),
        ),
        onClick = onClick,
    )
}

@Composable
private fun AssetInputCards(
    state: ZakatCalculatorUiState,
    viewModel: ZakatViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InputCard(
            icon = Icons.Default.Wallet,
            iconTint = NimazColors.ZakatColors.Cash,
            label = stringResource(R.string.cash_on_hand),
            hint = stringResource(R.string.hint_physical_cash),
            value = state.assets.cashOnHand,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateCash(it)) },
            currency = state.currency
        )
        InputCard(
            icon = Icons.Default.AccountBalance,
            iconTint = NimazColors.ZakatColors.Cash,
            label = stringResource(R.string.bank_balance),
            hint = stringResource(R.string.hint_bank_accounts),
            value = state.assets.bankBalance,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateBankBalance(it)) },
            currency = state.currency
        )
        InputCard(
            icon = Icons.Default.Savings,
            iconTint = NimazColors.ZakatColors.Gold,
            label = stringResource(R.string.gold),
            hint = stringResource(R.string.hint_weight_in_grams),
            value = state.assets.goldGrams,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateGold(it)) },
            currency = state.currency,
            unitSuffix = stringResource(R.string.zakat_unit_grams)
        )
        InputCard(
            icon = Icons.Default.Savings,
            iconTint = NimazColors.ZakatColors.Silver,
            label = stringResource(R.string.silver),
            hint = stringResource(R.string.hint_weight_in_grams),
            value = state.assets.silverGrams,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateSilver(it)) },
            currency = state.currency,
            unitSuffix = stringResource(R.string.zakat_unit_grams)
        )
        InputCard(
            icon = Icons.AutoMirrored.Filled.ShowChart,
            iconTint = NimazColors.ZakatColors.Investment,
            label = stringResource(R.string.investments),
            hint = stringResource(R.string.hint_stocks_bonds),
            value = state.assets.investments,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateInvestments(it)) },
            currency = state.currency
        )
        InputCard(
            icon = Icons.Default.Business,
            iconTint = MaterialTheme.colorScheme.primary,
            label = stringResource(R.string.business_inventory),
            hint = stringResource(R.string.hint_goods_for_trade),
            value = state.assets.businessInventory,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateBusinessInventory(it)) },
            currency = state.currency
        )
        InputCard(
            icon = Icons.Default.Receipt,
            iconTint = MaterialTheme.colorScheme.primary,
            label = stringResource(R.string.receivables),
            hint = stringResource(R.string.hint_money_owed_to_you),
            value = state.assets.receivables,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateReceivables(it)) },
            currency = state.currency
        )
        InputCard(
            icon = Icons.Default.Home,
            iconTint = MaterialTheme.colorScheme.primary,
            label = stringResource(R.string.rental_income),
            hint = stringResource(R.string.hint_income_from_properties),
            value = state.assets.rentalIncome,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateRentalIncome(it)) },
            currency = state.currency
        )
        InputCard(
            icon = Icons.Default.MoreHoriz,
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
            label = stringResource(R.string.other_assets),
            hint = stringResource(R.string.hint_other_zakatable_assets),
            value = state.assets.otherAssets,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateOtherAssets(it)) },
            currency = state.currency
        )
    }
}

@Composable
private fun LiabilityInputCards(
    state: ZakatCalculatorUiState,
    viewModel: ZakatViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InputCard(
            icon = Icons.Default.CreditCard,
            iconTint = MaterialTheme.colorScheme.error,
            label = stringResource(R.string.debts_owed),
            hint = stringResource(R.string.hint_personal_debts),
            value = state.liabilities.debts,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateDebts(it)) },
            currency = state.currency
        )
        InputCard(
            icon = Icons.Default.AccountBalance,
            iconTint = MaterialTheme.colorScheme.error,
            label = stringResource(R.string.loans),
            hint = stringResource(R.string.hint_bank_personal_loans),
            value = state.liabilities.loans,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateLoans(it)) },
            currency = state.currency
        )
        InputCard(
            icon = Icons.Default.Receipt,
            iconTint = MaterialTheme.colorScheme.error,
            label = stringResource(R.string.bills_due),
            hint = stringResource(R.string.hint_outstanding_bills),
            value = state.liabilities.billsDue,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateBillsDue(it)) },
            currency = state.currency
        )
        InputCard(
            icon = Icons.Default.MoreHoriz,
            iconTint = MaterialTheme.colorScheme.error,
            label = stringResource(R.string.other_liabilities),
            hint = stringResource(R.string.hint_other_liabilities),
            value = state.liabilities.otherLiabilities,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateOtherLiabilities(it)) },
            currency = state.currency
        )
    }
}

// --- Result Summary Hero ---

@Composable
private fun ZakatResultSummaryCard(
    zakatDue: Double,
    nisabValue: Double,
    netWealth: Double,
    isAboveNisab: Boolean,
    nisabType: NisabType,
    currency: String,
    modifier: Modifier = Modifier,
    collapsed: Boolean = false,
) {
    ZakatSummaryHero(
        modifier = modifier,
        collapsed = collapsed,
        label = stringResource(R.string.zakat_due),
        amount = formatCurrency(zakatDue, currency),
        // Below nisab nothing is owed, so the rate line would be misleading —
        // say why the figure is zero instead of restating how it is derived.
        subtitle = if (isAboveNisab) {
            stringResource(R.string.zakat_rate_subtitle)
        } else {
            stringResource(R.string.zakat_below_nisab_subtitle)
        },
        // A full-strength $0.00 overstates a number that is not owed.
        muteAmount = !isAboveNisab,
        status = ZakatHeroStatus(
            text = if (isAboveNisab) {
                stringResource(R.string.zakat_status_above_nisab)
            } else {
                stringResource(R.string.zakat_status_below_nisab)
            },
            met = isAboveNisab,
        ),
        stats = listOf(
            ZakatHeroStat(
                value = formatCurrency(netWealth, currency),
                label = stringResource(R.string.zakat_stat_net),
            ),
            ZakatHeroStat(
                value = formatCurrency(nisabValue, currency),
                // NisabType.displayName() is hardcoded English ("Gold (87.48g)")
                // and far too long for a tile caption — use the localised noun.
                label = stringResource(
                    R.string.zakat_stat_nisab_format,
                    stringResource(
                        when (nisabType) {
                            NisabType.GOLD -> R.string.gold
                            NisabType.SILVER -> R.string.silver
                        }
                    )
                ),
                accented = true,
            ),
            ZakatHeroStat(
                value = stringResource(R.string.zakat_stat_rate_value),
                label = stringResource(R.string.zakat_stat_rate),
            ),
        ),
    )
}

// --- Input Card ---

@Composable
private fun InputCard(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    hint: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    currency: String,
    /** A weight unit that follows the number. Null means this row is money in [currency]. */
    unitSuffix: String? = null,
    modifier: Modifier = Modifier
) {
    NimazCard(
        modifier = modifier.fillMaxWidth(),
        // Each input row is a card on the page background → elevated.
        tone = NimazTone.NEUTRAL,
        style = NimazCardStyle.ELEVATED,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NimazIconWell(
                icon = icon,
                color = iconTint,
                size = NimazIconWellSize.STANDARD,
            )

            // Label and hint
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            NimazAmountField(
                value = value,
                onValueChange = onValueChange,
                // Money leads with its symbol, a weight follows with its unit. The field this
                // replaced chose between them by comparing its suffix against the string "$",
                // which meant every non-dollar currency rendered a dollar sign.
                currencySymbol = if (unitSuffix == null) currencySymbolOf(currency) else null,
                unitSuffix = unitSuffix,
                placeholder = if (unitSuffix == null) "0.00" else "0",
            )
        }
    }
}

// --- Breakdown Card ---

/**
 * The line-by-line working behind the zakat figure, collapsible so the summary and the save
 * action stay reachable without scrolling past eight rows of arithmetic.
 *
 * [expanded] is `showBreakdown` from the ViewModel rather than local `remember` state: the
 * calculator is a long screen whose state survives rotation and process death through the
 * ViewModel, and a breakdown that silently re-opened on the way back would undo the choice.
 * Saving used to live at the bottom of this card, deliberately outside the disclosure so it could
 * not be hidden. It has moved to the screen's action bar, which is a strictly better home for the
 * same reason: it is reachable without finding the breakdown at all.
 */
@Composable
private fun BreakdownCard(
    totalAssets: Double,
    totalLiabilities: Double,
    netWorth: Double,
    nisabValue: Double,
    isAboveNisab: Boolean,
    zakatDue: Double,
    currency: String,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        NimazCard(
            onClick = onToggleExpanded,
            modifier = Modifier.fillMaxWidth(),
            style = NimazCardStyle.FILLED,
            tone = NimazTone.NEUTRAL,
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.calculation_breakdown),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                NimazIcon(
                    imageVector = if (expanded) NimazIcons.Collapse
                    else NimazIcons.Expand,
                    contentDescription = stringResource(
                        if (expanded) R.string.cd_collapse else R.string.cd_expand
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (expanded) {
            NimazCard(
                modifier = Modifier.fillMaxWidth(),
                // A section card on the page background → elevated.
                tone = NimazTone.NEUTRAL,
                style = NimazCardStyle.ELEVATED,
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BreakdownRow(
                        label = stringResource(R.string.total_assets),
                        value = formatCurrency(totalAssets, currency),
                        valueColor = MaterialTheme.colorScheme.primary
                    )
                    BreakdownRow(
                        label = stringResource(R.string.total_liabilities),
                        value = "- ${formatCurrency(totalLiabilities, currency)}",
                        valueColor = MaterialTheme.colorScheme.error
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp
                    )

                    BreakdownRow(
                        label = stringResource(R.string.net_zakatable_wealth),
                        value = formatCurrency(netWorth, currency),
                        valueColor = MaterialTheme.colorScheme.onSurface,
                        isBold = true
                    )

                    BreakdownRow(
                        label = stringResource(R.string.nisab_threshold),
                        value = formatCurrency(nisabValue, currency),
                        valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    BreakdownRow(
                        label = stringResource(R.string.meets_nisab),
                        value = stringResource(if (isAboveNisab) R.string.yes else R.string.no),
                        valueColor = if (isAboveNisab) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp
                    )

                    BreakdownRow(
                        label = stringResource(R.string.zakat_due_2_5_percent),
                        value = formatCurrency(zakatDue, currency),
                        valueColor = NimazColors.ZakatColors.Gold,
                        isBold = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

    }
}

@Composable
private fun BreakdownRow(
    label: String,
    value: String,
    valueColor: Color,
    isBold: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}

