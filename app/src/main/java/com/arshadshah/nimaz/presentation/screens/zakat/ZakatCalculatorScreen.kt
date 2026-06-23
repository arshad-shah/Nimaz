package com.arshadshah.nimaz.presentation.screens.zakat

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.currentWindowSizeClass
import com.arshadshah.nimaz.presentation.theme.isCompact
import com.arshadshah.nimaz.presentation.viewmodel.ZakatEvent
import com.arshadshah.nimaz.presentation.viewmodel.ZakatViewModel
import com.arshadshah.nimaz.core.util.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatCalculatorScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: ZakatViewModel = hiltViewModel()
) {
    val state by viewModel.calculatorState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
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
    state: com.arshadshah.nimaz.presentation.viewmodel.ZakatCalculatorUiState,
    viewModel: ZakatViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ZakatResultSummaryCard(
                zakatDue = state.calculation?.zakatDue ?: 0.0,
                nisabValue = state.calculation?.nisabValue ?: 0.0,
                isAboveNisab = state.calculation?.isAboveNisab ?: false,
                nisabType = state.nisabType,
                currency = state.currency
            )
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            NisabSelector(
                selectedType = state.nisabType,
                goldPrice = state.goldPricePerGram,
                silverPrice = state.silverPricePerGram,
                onTypeChange = { viewModel.onEvent(ZakatEvent.SetNisabType(it)) }
            )
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            NimazSectionHeader(
                title = stringResource(R.string.assets),
                trailingContent = {
                    Text(
                        text = formatCurrency(
                            state.assets.total +
                                    (state.assets.goldGrams * state.goldPricePerGram) +
                                    (state.assets.silverGrams * state.silverPricePerGram),
                            state.currency
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
        item { AssetInputCards(state = state, viewModel = viewModel) }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            NimazSectionHeader(
                title = stringResource(R.string.liabilities),
                trailingContent = {
                    Text(
                        text = formatCurrency(state.liabilities.total, state.currency),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            )
        }
        item { LiabilityInputCards(state = state, viewModel = viewModel) }
        state.calculation?.let { calculation ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BreakdownCard(
                    totalAssets = calculation.totalAssets,
                    totalLiabilities = calculation.totalLiabilities,
                    netWorth = calculation.netWorth,
                    nisabValue = calculation.nisabValue,
                    isAboveNisab = calculation.isAboveNisab,
                    zakatDue = calculation.zakatDue,
                    currency = state.currency,
                    onSaveClick = { viewModel.onEvent(ZakatEvent.SaveCalculation) }
                )
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun ZakatTabletContent(
    state: com.arshadshah.nimaz.presentation.viewmodel.ZakatCalculatorUiState,
    viewModel: ZakatViewModel,
    modifier: Modifier = Modifier
) {
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
            isAboveNisab = state.calculation?.isAboveNisab ?: false,
            nisabType = state.nisabType,
            currency = state.currency
        )

        // Nisab selector spans full width
        NisabSelector(
            selectedType = state.nisabType,
            goldPrice = state.goldPricePerGram,
            silverPrice = state.silverPricePerGram,
            onTypeChange = { viewModel.onEvent(ZakatEvent.SetNisabType(it)) }
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
                        Text(
                            text = formatCurrency(
                                state.assets.total +
                                        (state.assets.goldGrams * state.goldPricePerGram) +
                                        (state.assets.silverGrams * state.silverPricePerGram),
                                state.currency
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
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
                        Text(
                            text = formatCurrency(state.liabilities.total, state.currency),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
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
                onSaveClick = { viewModel.onEvent(ZakatEvent.SaveCalculation) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AssetInputCards(
    state: com.arshadshah.nimaz.presentation.viewmodel.ZakatCalculatorUiState,
    viewModel: ZakatViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InputCard(
            icon = Icons.Default.Wallet,
            iconTint = NimazColors.ZakatColors.Cash,
            iconBackground = NimazColors.ZakatColors.Cash.copy(alpha = 0.2f),
            label = stringResource(R.string.cash_on_hand),
            hint = stringResource(R.string.hint_physical_cash),
            value = state.assets.cashOnHand,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateCash(it)) }
        )
        InputCard(
            icon = Icons.Default.AccountBalance,
            iconTint = NimazColors.ZakatColors.Cash,
            iconBackground = NimazColors.ZakatColors.Cash.copy(alpha = 0.2f),
            label = stringResource(R.string.bank_balance),
            hint = stringResource(R.string.hint_bank_accounts),
            value = state.assets.bankBalance,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateBankBalance(it)) }
        )
        InputCard(
            icon = Icons.Default.Savings,
            iconTint = NimazColors.ZakatColors.Gold,
            iconBackground = NimazColors.ZakatColors.Gold.copy(alpha = 0.2f),
            label = stringResource(R.string.gold),
            hint = stringResource(R.string.hint_weight_in_grams),
            value = state.assets.goldGrams,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateGold(it)) },
            suffix = "g"
        )
        InputCard(
            icon = Icons.Default.Savings,
            iconTint = NimazColors.ZakatColors.Silver,
            iconBackground = NimazColors.ZakatColors.Silver.copy(alpha = 0.2f),
            label = stringResource(R.string.silver),
            hint = stringResource(R.string.hint_weight_in_grams),
            value = state.assets.silverGrams,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateSilver(it)) },
            suffix = "g"
        )
        InputCard(
            icon = Icons.AutoMirrored.Filled.ShowChart,
            iconTint = NimazColors.ZakatColors.Investment,
            iconBackground = NimazColors.ZakatColors.Investment.copy(alpha = 0.2f),
            label = stringResource(R.string.investments),
            hint = stringResource(R.string.hint_stocks_bonds),
            value = state.assets.investments,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateInvestments(it)) }
        )
        InputCard(
            icon = Icons.Default.Business,
            iconTint = MaterialTheme.colorScheme.primary,
            iconBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            label = stringResource(R.string.business_inventory),
            hint = stringResource(R.string.hint_goods_for_trade),
            value = state.assets.businessInventory,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateBusinessInventory(it)) }
        )
        InputCard(
            icon = Icons.Default.Receipt,
            iconTint = MaterialTheme.colorScheme.primary,
            iconBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            label = stringResource(R.string.receivables),
            hint = stringResource(R.string.hint_money_owed_to_you),
            value = state.assets.receivables,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateReceivables(it)) }
        )
        InputCard(
            icon = Icons.Default.Home,
            iconTint = MaterialTheme.colorScheme.primary,
            iconBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            label = stringResource(R.string.rental_income),
            hint = stringResource(R.string.hint_income_from_properties),
            value = state.assets.rentalIncome,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateRentalIncome(it)) }
        )
        InputCard(
            icon = Icons.Default.MoreHoriz,
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
            iconBackground = MaterialTheme.colorScheme.surfaceVariant,
            label = stringResource(R.string.other_assets),
            hint = stringResource(R.string.hint_other_zakatable_assets),
            value = state.assets.otherAssets,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateOtherAssets(it)) }
        )
    }
}

@Composable
private fun LiabilityInputCards(
    state: com.arshadshah.nimaz.presentation.viewmodel.ZakatCalculatorUiState,
    viewModel: ZakatViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InputCard(
            icon = Icons.Default.CreditCard,
            iconTint = MaterialTheme.colorScheme.error,
            iconBackground = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
            label = stringResource(R.string.debts_owed),
            hint = stringResource(R.string.hint_personal_debts),
            value = state.liabilities.debts,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateDebts(it)) }
        )
        InputCard(
            icon = Icons.Default.AccountBalance,
            iconTint = MaterialTheme.colorScheme.error,
            iconBackground = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
            label = stringResource(R.string.loans),
            hint = stringResource(R.string.hint_bank_personal_loans),
            value = state.liabilities.loans,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateLoans(it)) }
        )
        InputCard(
            icon = Icons.Default.Receipt,
            iconTint = MaterialTheme.colorScheme.error,
            iconBackground = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
            label = stringResource(R.string.bills_due),
            hint = stringResource(R.string.hint_outstanding_bills),
            value = state.liabilities.billsDue,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateBillsDue(it)) }
        )
        InputCard(
            icon = Icons.Default.MoreHoriz,
            iconTint = MaterialTheme.colorScheme.error,
            iconBackground = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
            label = stringResource(R.string.other_liabilities),
            hint = stringResource(R.string.hint_other_liabilities),
            value = state.liabilities.otherLiabilities,
            onValueChange = { viewModel.onEvent(ZakatEvent.UpdateOtherLiabilities(it)) }
        )
    }
}

// --- Result Summary Card (gold gradient) ---

@Composable
private fun ZakatResultSummaryCard(
    zakatDue: Double,
    nisabValue: Double,
    isAboveNisab: Boolean,
    nisabType: NisabType,
    currency: String,
    modifier: Modifier = Modifier
) {
    val goldGradient = Brush.linearGradient(
        colors = listOf(
            NimazColors.ZakatColors.Gold,
            NimazColors.ZakatColors.GoldAccent
        )
    )

    NimazCard(
        modifier = modifier.fillMaxWidth(),
        style = NimazCardStyle.FILLED,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(goldGradient)
                .padding(25.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.zakat_due),
                    style = MaterialTheme.typography.bodySmall,
                    color = NimazColors.Neutral900.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = formatCurrency(zakatDue, currency),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 36.sp
                    ),
                    color = NimazColors.Neutral900
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.zakat_rate_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = NimazColors.Neutral900.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(15.dp))

                HorizontalDivider(
                    color = Color.Black.copy(alpha = 0.15f),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(15.dp))

                Text(
                    text = if (isAboveNisab) {
                        "Your wealth exceeds the ${nisabType.displayName()} nisab threshold of ${
                            formatCurrency(
                                nisabValue,
                                currency
                            )
                        }"
                    } else {
                        "Nisab threshold (${nisabType.displayName()}): ${
                            formatCurrency(
                                nisabValue,
                                currency
                            )
                        }"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = NimazColors.Neutral900.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// --- Nisab Selector ---

@Composable
private fun NisabSelector(
    selectedType: NisabType,
    goldPrice: Double,
    silverPrice: Double,
    onTypeChange: (NisabType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        NisabOptionCard(
            label = stringResource(R.string.gold),
            subtitle = "87.48g @ \$${goldPrice.toInt()}/g",
            isSelected = selectedType == NisabType.GOLD,
            accentColor = NimazColors.ZakatColors.Gold,
            onClick = { onTypeChange(NisabType.GOLD) },
            modifier = Modifier.weight(1f)
        )

        NisabOptionCard(
            label = stringResource(R.string.silver),
            subtitle = "612.36g @ \$${silverPrice}/g",
            isSelected = selectedType == NisabType.SILVER,
            accentColor = NimazColors.ZakatColors.Silver,
            onClick = { onTypeChange(NisabType.SILVER) },
            modifier = Modifier.weight(1f)
        )
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
    NimazCard(
        onClick = onClick,
        modifier = modifier,
        style = NimazCardStyle.FILLED,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                accentColor.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.5.dp, accentColor.copy(alpha = 0.5f))
        } else null
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
    iconBackground: Color,
    label: String,
    hint: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    suffix: String = "$",
    modifier: Modifier = Modifier
) {
    NimazCard(
        modifier = modifier.fillMaxWidth(),
        style = NimazCardStyle.FILLED,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconBackground,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    NimazIcon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        size = NimazIconSize.MEDIUM
                    )
                }
            }

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

            // Compact input field
            CompactAmountField(
                value = value,
                onValueChange = onValueChange,
                suffix = suffix
            )
        }
    }
}

@Composable
private fun CompactAmountField(
    value: Double,
    onValueChange: (Double) -> Unit,
    suffix: String = "$",
    modifier: Modifier = Modifier
) {
    val displayText = if (value == 0.0) "" else {
        if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.width(100.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (suffix == "$") {
                Text(
                    text = "$",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(2.dp))
            }

            BasicTextField(
                value = displayText,
                onValueChange = { text ->
                    val newValue = text.toDoubleOrNull() ?: 0.0
                    onValueChange(newValue)
                },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterEnd) {
                        if (displayText.isEmpty()) {
                            Text(
                                text = "0",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    textAlign = TextAlign.End
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (suffix != "$") {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = suffix,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// --- Breakdown Card ---

@Composable
private fun BreakdownCard(
    totalAssets: Double,
    totalLiabilities: Double,
    netWorth: Double,
    nisabValue: Double,
    isAboveNisab: Boolean,
    zakatDue: Double,
    currency: String,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.calculation_breakdown),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        NimazCard(
            modifier = Modifier.fillMaxWidth(),
            style = NimazCardStyle.FILLED,
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
                    value = if (isAboveNisab) "Yes" else "No",
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

        // Save button
        NimazCard(
            onClick = onSaveClick,
            modifier = Modifier.fillMaxWidth(),
            style = NimazCardStyle.FILLED,
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = stringResource(R.string.save_calculation),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )
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

