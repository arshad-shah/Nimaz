package com.arshadshah.nimaz.presentation.screens.zakat

import com.arshadshah.nimaz.domain.model.ZakatDefaults
import com.arshadshah.nimaz.presentation.components.molecules.NimazListPicker
import com.arshadshah.nimaz.presentation.components.molecules.NimazPickerItem
import com.arshadshah.nimaz.presentation.components.molecules.NimazAccordion
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import java.util.Currency
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.share.ContentShareManager
import com.arshadshah.nimaz.core.share.Shareables
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.core.util.formatCurrency
import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.presentation.components.atoms.NimazAmountInput
import com.arshadshah.nimaz.presentation.components.atoms.amountToInput
import com.arshadshah.nimaz.presentation.components.atoms.parseAmountInput
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButtonStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconWell
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconWellShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconWellSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazErrorDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazErrorState
import com.arshadshah.nimaz.presentation.components.atoms.NimazErrorVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.molecules.ZakatHeroStat
import com.arshadshah.nimaz.presentation.components.molecules.ZakatHeroStatus
import com.arshadshah.nimaz.presentation.components.molecules.ZakatSummaryHero
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazShapes
import com.arshadshah.nimaz.presentation.theme.currentWindowSizeClass
import com.arshadshah.nimaz.presentation.theme.isCompact
import com.arshadshah.nimaz.presentation.viewmodel.tools.ZakatEvent
import com.arshadshah.nimaz.presentation.viewmodel.tools.ZakatViewModel
import com.arshadshah.nimaz.presentation.viewmodel.tools.ZakatCalculatorUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatCalculatorScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
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
                }
            )
        }
    ) { paddingValues ->
        val windowSizeClass = currentWindowSizeClass()

        if (windowSizeClass.isCompact) {
            ZakatCompactContent(
                state = state,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            ZakatTabletContent(
                state = state,
                viewModel = viewModel,
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
    modifier: Modifier = Modifier
) {
    var showCurrencyPicker by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val onShareCalculation = rememberZakatShareAction(state)

    // Threshold on the first item's own scroll offset, not an accumulated delta. A delta drifts
    // over a long form and can strand the hero half-collapsed after a fling; this is derived from
    // where the list actually is, so it cannot disagree with the scroll position.
    val collapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > HeroCollapseThresholdPx
        }
    }
    val collapseProgress by animateFloatAsState(
        targetValue = if (collapsed) 1f else 0f,
        label = "zakat_hero_collapse",
    )

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
            collapseProgress = collapseProgress,
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
            item {
                NimazAccordion(
                    title = stringResource(R.string.zakat_section_nisab),
                    subtitle = stringResource(R.string.zakat_section_nisab_subtitle),
                    trailing = {
                        SubtotalLabel(
                            amount = state.calculation?.nisabValue ?: 0.0,
                            currency = state.currency,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NisabSelector(
                            selectedType = state.nisabType,
                            goldPrice = state.goldPricePerGram,
                            silverPrice = state.silverPricePerGram,
                            currency = state.currency,
                            onTypeChange = { viewModel.onEvent(ZakatEvent.SetNisabType(it)) },
                            onGoldPriceChange = { viewModel.onEvent(ZakatEvent.UpdateGoldPrice(it)) },
                            onSilverPriceChange = { viewModel.onEvent(ZakatEvent.UpdateSilverPrice(it)) }
                        )
                        // ZakatEvent.SetCurrency existed with a handler and no producer: every
                        // figure on this screen was formatted with state.currency, and nothing
                        // could change it, so anyone outside the default read someone else's
                        // symbol on their own zakat.
                        NimazMenuItem(
                            title = stringResource(R.string.zakat_currency),
                            subtitle = currencyLabel(state.currency),
                            onClick = { showCurrencyPicker = true },
                        )
                    }
                }
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

    if (showCurrencyPicker) {
        NimazListPicker(
            title = stringResource(R.string.zakat_currency),
            items = ZakatDefaults.CURRENCIES.map { code ->
                NimazPickerItem(value = code, title = code, description = currencyLabel(code))
            },
            selected = state.currency,
            onSelected = { viewModel.onEvent(ZakatEvent.SetCurrency(it)) },
            onDismiss = { showCurrencyPicker = false },
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
    modifier: Modifier = Modifier
) {
    var showCurrencyPicker by rememberSaveable { mutableStateOf(false) }
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

        // Nisab selector spans full width
        NisabSelector(
            selectedType = state.nisabType,
            goldPrice = state.goldPricePerGram,
            silverPrice = state.silverPricePerGram,
            currency = state.currency,
            onTypeChange = { viewModel.onEvent(ZakatEvent.SetNisabType(it)) },
            onGoldPriceChange = { viewModel.onEvent(ZakatEvent.UpdateGoldPrice(it)) },
            onSilverPriceChange = { viewModel.onEvent(ZakatEvent.UpdateSilverPrice(it)) }
        )
        NimazMenuItem(
            title = stringResource(R.string.zakat_currency),
            subtitle = currencyLabel(state.currency),
            onClick = { showCurrencyPicker = true },
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

    if (showCurrencyPicker) {
        NimazListPicker(
            title = stringResource(R.string.zakat_currency),
            items = ZakatDefaults.CURRENCIES.map { code ->
                NimazPickerItem(value = code, title = code, description = currencyLabel(code))
            },
            selected = state.currency,
            onSelected = { viewModel.onEvent(ZakatEvent.SetCurrency(it)) },
            onDismiss = { showCurrencyPicker = false },
        )
    }
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
    collapseProgress: Float = 0f,
) {
    ZakatSummaryHero(
        modifier = modifier,
        collapseProgress = collapseProgress,
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

// --- Nisab Selector ---

@Composable
private fun NisabSelector(
    selectedType: NisabType,
    goldPrice: Double,
    silverPrice: Double,
    currency: String,
    onTypeChange: (NisabType) -> Unit,
    onGoldPriceChange: (Double) -> Unit,
    onSilverPriceChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NisabOptionCard(
                label = stringResource(R.string.gold),
                // Formatted, not truncated: goldPrice.toInt() rendered the silver
                // price of 0.80 as "0", and the hardcoded "$" ignored the currency.
                subtitle = stringResource(
                    R.string.zakat_nisab_gold_subtitle,
                    formatCurrency(goldPrice, currency)
                ),
                isSelected = selectedType == NisabType.GOLD,
                accentColor = NimazColors.ZakatColors.Gold,
                onClick = { onTypeChange(NisabType.GOLD) },
                modifier = Modifier.weight(1f)
            )

            NisabOptionCard(
                label = stringResource(R.string.silver),
                subtitle = stringResource(
                    R.string.zakat_nisab_silver_subtitle,
                    formatCurrency(silverPrice, currency)
                ),
                isSelected = selectedType == NisabType.SILVER,
                accentColor = NimazColors.ZakatColors.Silver,
                onClick = { onTypeChange(NisabType.SILVER) },
                modifier = Modifier.weight(1f)
            )
        }

        MetalPricesEditor(
            goldPrice = goldPrice,
            silverPrice = silverPrice,
            currency = currency,
            onGoldPriceChange = onGoldPriceChange,
            onSilverPriceChange = onSilverPriceChange
        )
    }
}

/**
 * The metal prices the nisab threshold is derived from, editable.
 *
 * These used to be constants no user could reach, so every zakat figure was wrong by
 * however stale they were — and because the gold price sets the nisab threshold as
 * well as the metal valuation, a stale price could change whether zakat was owed at
 * all. The hint says they are estimates so a default is never mistaken for a rate.
 */
@Composable
private fun MetalPricesEditor(
    goldPrice: Double,
    silverPrice: Double,
    currency: String,
    onGoldPriceChange: (Double) -> Unit,
    onSilverPriceChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    NimazCard(
        modifier = modifier.fillMaxWidth(),
        style = NimazCardStyle.OUTLINED,
        shape = NimazShapes.small
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.zakat_gold_price_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                AmountField(
                    value = goldPrice,
                    onValueChange = onGoldPriceChange,
                    currencySymbol = currencySymbolOf(currency),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.zakat_silver_price_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                AmountField(
                    value = silverPrice,
                    onValueChange = onSilverPriceChange,
                    currencySymbol = currencySymbolOf(currency),
                )
            }

            Text(
                text = stringResource(R.string.zakat_metal_prices_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NisabOptionCard(
    label: String,
    subtitle: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surface = MaterialTheme.colorScheme.surface
    NimazCard(
        onClick = onClick,
        modifier = modifier,
        // Two peers on the page background: elevation gives each option a card
        // boundary, the accent fill + border carries the selection.
        // FILLED (not ELEVATED) because only Material's `Card` renders a border —
        // `ElevatedCard` has no border slot, so an ELEVATED card drops
        // `activeBorder` silently. Elevation is passed explicitly to keep the lift.
        style = NimazCardStyle.FILLED,
        elevation = 1.dp,
        shape = RoundedCornerShape(14.dp),
        selected = isSelected,
        colors = NimazCardDefaults.selectable(
            container = surface,
            // Composited to an OPAQUE colour on purpose. A translucent container on
            // a shadow-casting surface makes the RenderNode non-opaque, so Android
            // fills the shadow's interior behind it — that leaked through as a pale
            // box in the middle of the selected gold/silver card.
            activeContainer = accentColor.copy(alpha = 0.15f).compositeOver(surface),
            // Border alpha is safe — it is stroked on top, not behind the shadow.
            activeBorder = accentColor.copy(alpha = 0.5f),
        )
    ) {
        Column(
            modifier = Modifier.padding(15.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
                accent = iconTint,
                size = NimazIconWellSize.MEDIUM,
                shape = NimazIconWellShape.ROUNDED
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

            AmountField(
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

/**
 * A money or weight field bound to a `Double` on the ViewModel.
 *
 * The text is **local state**, and that is the whole fix. The field this replaced parsed every
 * keystroke straight to a `Double` and re-rendered the result, so "10." became "10" before the
 * next digit landed and a decimal amount was literally unenterable. Here the string is what the
 * person typed and the `Double` is derived from it.
 *
 * The sync back the other way is guarded: an incoming [value] only overwrites the text when it
 * disagrees with what the text already parses to. Without that guard, the ViewModel echoing back
 * the user's own keystroke would erase the trailing point as fast as it was typed — while
 * "Clear all" and a restored calculation, which genuinely differ, still win.
 */
@Composable
private fun AmountField(
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    currencySymbol: String? = null,
    unitSuffix: String? = null,
    placeholder: String = "0.00",
) {
    var text by rememberSaveable { mutableStateOf(amountToInput(value)) }
    LaunchedEffect(value) {
        if (parseAmountInput(text) != value) text = amountToInput(value)
    }
    NimazAmountInput(
        value = text,
        onValueChange = { next ->
            text = next
            onValueChange(parseAmountInput(next))
        },
        modifier = modifier,
        currencySymbol = currencySymbol,
        unitSuffix = unitSuffix,
        placeholder = placeholder,
    )
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
                    imageVector = if (expanded) Icons.Filled.ExpandLess
                    else Icons.Filled.ExpandMore,
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

/**
 * "US Dollar ($)" in English, "US-Dollar ($)" in German — resolved by `java.util.Currency`
 * from the ISO code, so the picker carries no translated strings of its own.
 */
/**
 * The bare symbol for an ISO code — `€` for EUR — falling back to the code itself.
 *
 * The same resolution `formatCurrency` performs, so the symbol inside a field and the symbol
 * beside the total cannot disagree. A code with no symbol on this device renders as the code,
 * which is still true and still readable.
 */
internal fun currencySymbolOf(code: String): String = runCatching {
    Currency.getInstance(code).getSymbol(Locale.getDefault())
}.getOrDefault(code)

private fun currencyLabel(code: String): String = runCatching {
    val currency = Currency.getInstance(code)
    val name = currency.getDisplayName(Locale.getDefault())
    val symbol = currency.getSymbol(Locale.getDefault())
    if (symbol == code) name else "$name ($symbol)"
}.getOrDefault(code)
