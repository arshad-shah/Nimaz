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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.ZakatEvent
import com.arshadshah.nimaz.presentation.viewmodel.ZakatViewModel
import java.text.NumberFormat
import java.util.Locale

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
                title = "Zakat Calculator",
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { viewModel.onEvent(ZakatEvent.ClearAll) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset"
                        )
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Result Card - gold gradient at top
            item {
                ZakatResultSummaryCard(
                    zakatDue = state.calculation?.zakatDue ?: 0.0,
                    nisabValue = state.calculation?.nisabValue ?: 0.0,
                    isAboveNisab = state.calculation?.isAboveNisab ?: false,
                    nisabType = state.nisabType,
                    currency = state.currency
                )
            }

            // Nisab Type Selector
            item {
                Spacer(modifier = Modifier.height(8.dp))
                NisabSelector(
                    selectedType = state.nisabType,
                    goldPrice = state.goldPricePerGram,
                    silverPrice = state.silverPricePerGram,
                    onTypeChange = { viewModel.onEvent(ZakatEvent.SetNisabType(it)) }
                )
            }

            // Assets Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "Assets",
                    total = state.assets.total +
                            (state.assets.goldGrams * state.goldPricePerGram) +
                            (state.assets.silverGrams * state.silverPricePerGram),
                    currency = state.currency
                )
            }

            item {
                InputCard(
                    icon = Icons.Default.Wallet,
                    iconTint = NimazColors.ZakatColors.Cash,
                    iconBackground = NimazColors.ZakatColors.Cash.copy(alpha = 0.2f),
                    label = "Cash on Hand",
                    hint = "Physical cash you possess",
                    value = state.assets.cashOnHand,
                    onValueChange = { viewModel.onEvent(ZakatEvent.UpdateCash(it)) }
                )
            }

            item {
                InputCard(
                    icon = Icons.Default.AccountBalance,
                    iconTint = NimazColors.ZakatColors.Cash,
                    iconBackground = NimazColors.ZakatColors.Cash.copy(alpha = 0.2f),
                    label = "Bank Balance",
                    hint = "All bank accounts combined",
                    value = state.assets.bankBalance,
                    onValueChange = { viewModel.onEvent(ZakatEvent.UpdateBankBalance(it)) }
                )
            }

            item {
                InputCard(
                    icon = Icons.Default.Savings,
                    iconTint = NimazColors.ZakatColors.Gold,
                    iconBackground = NimazColors.ZakatColors.Gold.copy(alpha = 0.2f),
                    label = "Gold",
                    hint = "Weight in grams",
                    value = state.assets.goldGrams,
                    onValueChange = { viewModel.onEvent(ZakatEvent.UpdateGold(it)) },
                    suffix = "g"
                )
            }

            item {
                InputCard(
                    icon = Icons.Default.Savings,
                    iconTint = NimazColors.ZakatColors.Silver,
                    iconBackground = NimazColors.ZakatColors.Silver.copy(alpha = 0.2f),
                    label = "Silver",
                    hint = "Weight in grams",
                    value = state.assets.silverGrams,
                    onValueChange = { viewModel.onEvent(ZakatEvent.UpdateSilver(it)) },
                    suffix = "g"
                )
            }

            item {
                InputCard(
                    icon = Icons.Default.ShowChart,
                    iconTint = NimazColors.ZakatColors.Investment,
                    iconBackground = NimazColors.ZakatColors.Investment.copy(alpha = 0.2f),
                    label = "Investments",
                    hint = "Stocks, bonds, mutual funds",
                    value = state.assets.investments,
                    onValueChange = { viewModel.onEvent(ZakatEvent.UpdateInvestments(it)) }
                )
            }

            item {
                InputCard(
                    icon = Icons.Default.Business,
                    iconTint = MaterialTheme.colorScheme.primary,
                    iconBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    label = "Business Inventory",
                    hint = "Goods held for trade",
                    value = state.assets.businessInventory,
                    onValueChange = { viewModel.onEvent(ZakatEvent.UpdateBusinessInventory(it)) }
                )
            }

            item {
                InputCard(
                    icon = Icons.Default.Receipt,
                    iconTint = MaterialTheme.colorScheme.primary,
                    iconBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    label = "Receivables",
                    hint = "Money owed to you",
                    value = state.assets.receivables,
                    onValueChange = { viewModel.onEvent(ZakatEvent.UpdateReceivables(it)) }
                )
            }

            item {
                InputCard(
                    icon = Icons.Default.Home,
                    iconTint = MaterialTheme.colorScheme.primary,
                    iconBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    label = "Rental Income",
                    hint = "Income from properties",
                    value = state.assets.rentalIncome,
                    onValueChange = { viewModel.onEvent(ZakatEvent.UpdateRentalIncome(it)) }
                )
            }

            item {
                InputCard(
                    icon = Icons.Default.MoreHoriz,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconBackground = MaterialTheme.colorScheme.surfaceVariant,
                    label = "Other Assets",
                    hint = "Any other zakatable assets",
                    value = state.assets.otherAssets,
                    onValueChange = { viewModel.onEvent(ZakatEvent.UpdateOtherAssets(it)) }
                )
            }

            // Liabilities Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "Liabilities",
                    total = state.liabilities.total,
                    currency = state.currency,
                    totalColor = MaterialTheme.colorScheme.error
                )
            }

            item {
                InputCard(
                    icon = Icons.Default.CreditCard,
                    iconTint = MaterialTheme.colorScheme.error,
                    iconBackground = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                    label = "Debts Owed",
                    hint = "Personal debts",
                    value = state.liabilities.debts,
                    onValueChange = { viewModel.onEvent(ZakatEvent.UpdateDebts(it)) }
                )
            }

            item {
                InputCard(
                    icon = Icons.Default.AccountBalance,
                    iconTint = MaterialTheme.colorScheme.error,
                    iconBackground = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                    label = "Loans",
                    hint = "Bank or personal loans",
                    value = state.liabilities.loans,
                    onValueChange = { viewModel.onEvent(ZakatEvent.UpdateLoans(it)) }
                )
            }

            item {
                InputCard(
                    icon = Icons.Default.Receipt,
                    iconTint = MaterialTheme.colorScheme.error,
                    iconBackground = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                    label = "Bills Due",
                    hint = "Outstanding bills",
                    value = state.liabilities.billsDue,
                    onValueChange = { viewModel.onEvent(ZakatEvent.UpdateBillsDue(it)) }
                )
            }

            item {
                InputCard(
                    icon = Icons.Default.MoreHoriz,
                    iconTint = MaterialTheme.colorScheme.error,
                    iconBackground = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                    label = "Other Liabilities",
                    hint = "Any other liabilities",
                    value = state.liabilities.otherLiabilities,
                    onValueChange = { viewModel.onEvent(ZakatEvent.UpdateOtherLiabilities(it)) }
                )
            }

            // Breakdown Section
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

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
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
            Color(0xFFCA8A04)
        )
    )

    Card(
        modifier = modifier.fillMaxWidth(),
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
                    text = "Zakat Due",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1C1917).copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = formatCurrency(zakatDue, currency),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 36.sp
                    ),
                    color = Color(0xFF1C1917)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "2.5% of eligible wealth",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1C1917).copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(15.dp))

                HorizontalDivider(
                    color = Color.Black.copy(alpha = 0.15f),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(15.dp))

                Text(
                    text = if (isAboveNisab) {
                        "Your wealth exceeds the ${nisabType.displayName()} nisab threshold of ${formatCurrency(nisabValue, currency)}"
                    } else {
                        "Nisab threshold (${nisabType.displayName()}): ${formatCurrency(nisabValue, currency)}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF1C1917).copy(alpha = 0.8f),
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
            label = "Gold",
            subtitle = "87.48g @ \$${goldPrice.toInt()}/g",
            isSelected = selectedType == NisabType.GOLD,
            accentColor = NimazColors.ZakatColors.Gold,
            onClick = { onTypeChange(NisabType.GOLD) },
            modifier = Modifier.weight(1f)
        )

        NisabOptionCard(
            label = "Silver",
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
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                accentColor.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
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

// --- Section Header ---

@Composable
private fun SectionHeader(
    title: String,
    total: Double,
    currency: String,
    totalColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = formatCurrency(total, currency),
            style = MaterialTheme.typography.bodyMedium,
            color = totalColor
        )
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
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
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
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
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
            text = "Calculation Breakdown",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BreakdownRow(
                    label = "Total Assets",
                    value = formatCurrency(totalAssets, currency),
                    valueColor = MaterialTheme.colorScheme.primary
                )
                BreakdownRow(
                    label = "Total Liabilities",
                    value = "- ${formatCurrency(totalLiabilities, currency)}",
                    valueColor = MaterialTheme.colorScheme.error
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp
                )

                BreakdownRow(
                    label = "Net Zakatable Wealth",
                    value = formatCurrency(netWorth, currency),
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    isBold = true
                )

                BreakdownRow(
                    label = "Nisab Threshold",
                    value = formatCurrency(nisabValue, currency),
                    valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                BreakdownRow(
                    label = "Meets Nisab",
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
                    label = "Zakat Due (2.5%)",
                    value = formatCurrency(zakatDue, currency),
                    valueColor = NimazColors.ZakatColors.Gold,
                    isBold = true
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Save button
        Card(
            onClick = onSaveClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Save Calculation",
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

// --- Utilities ---

private fun formatCurrency(amount: Double, currency: String): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    return formatter.format(amount)
}
