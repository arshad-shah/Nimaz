package com.arshadshah.nimaz.ui.screens.more

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalAtm
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.services.CurrencyInfo
import com.arshadshah.nimaz.services.NisabType
import com.arshadshah.nimaz.viewModel.ZakatState
import com.arshadshah.nimaz.viewModel.ZakatViewModel
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatCalculator(navController: NavHostController) {
    val viewModel: ZakatViewModel = hiltViewModel()
    val zakatState by viewModel.zakatState.collectAsState()

    val locale = LocalConfiguration.current.locales[0]
    LaunchedEffect(locale) {
        viewModel.updateCurrency(locale)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Zakat Calculator") },
                navigationIcon = {
                    OutlinedIconButton(
                        modifier = Modifier
                            .testTag("backButton")
                            .padding(start = 8.dp),
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(id = R.drawable.back_icon),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Add refresh button
                    IconButton(onClick = { viewModel.refreshNisabThresholds() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh prices"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (zakatState.isLoading) {
                LoadingOverlay()
            } else {

                // Show error snackbar if there's an error
                zakatState.error?.let { error ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        ErrorSnackbar(
                            message = error,
                            onDismiss = { /* Add error dismissal handling */ }
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { ZakatInfoCard() }

                    // Add the new Zakat Period card
                    item {
                        ZakatPeriodCard(
                            startDate = zakatState.zakatStartDate,
                            endDate = zakatState.zakatEndDate
                        )
                    }

                    // Add Nisab Type Selector
                    item { NisabTypeSelector(zakatState, viewModel::setNisabType) }

                    // Show current metal prices
                    item { MetalPricesCard(zakatState) }

                    item {
                        AssetsSection(
                            goldAndSilverAmount = zakatState.goldAndSilverAmount,
                            cashAmount = zakatState.cashAmount,
                            otherSavings = zakatState.otherSavings,
                            investmentsAmount = zakatState.investmentsAmount,
                            moneyOwedToYou = zakatState.moneyOwedToYou,
                            stockValue = zakatState.stockValue,
                            currencyInfo = zakatState.currencyInfo,
                            onGoldAndSilverChange = viewModel::updateGoldAndSilver,
                            onCashAmountChange = viewModel::updateCashAmount,
                            onOtherSavingsChange = viewModel::updateOtherSavings,
                            onInvestmentsChange = viewModel::updateInvestments,
                            onMoneyOwedToYouChange = viewModel::updateMoneyOwedToYou,
                            onStockValueChange = viewModel::updateStockValue
                        )
                    }

                    item {
                        LiabilitiesSection(
                            moneyYouOwe = zakatState.moneyYouOwe,
                            otherOutgoingsDue = zakatState.otherOutgoingsDue,
                            currencyInfo = zakatState.currencyInfo,
                            onMoneyYouOweChange = viewModel::updateMoneyYouOwe,
                            onOtherOutgoingsDueChange = viewModel::updateOtherOutgoingsDue
                        )
                    }

                    item {
                        CalculationResultCard(
                            netAssets = zakatState.netAssets,
                            totalZakat = zakatState.totalZakat,
                            nisabThreshold = zakatState.currentNisabThreshold,
                            isEligible = zakatState.isEligible,
                            formatCurrency = viewModel::formatCurrency
                        )
                    }
                }
            }

        }
    }
}

@Composable
private fun ZakatPeriodCard(
    startDate: String,
    endDate: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Calendar Icon
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = "Zakat Period Icon",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Zakat Period",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Date Range Display
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // From Date
                    // From Date Section
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "From",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = startDate,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )

                    // To Date Section
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "To",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = endDate,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Informational text
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Zakat is calculated for assets held over one complete Islamic lunar year (Hijri)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


@Composable
private fun AssetsSection(
    goldAndSilverAmount: String,
    cashAmount: String,
    otherSavings: String,
    investmentsAmount: String,
    moneyOwedToYou: String,
    stockValue: String,
    currencyInfo: CurrencyInfo,
    onGoldAndSilverChange: (String) -> Unit,
    onCashAmountChange: (String) -> Unit,
    onOtherSavingsChange: (String) -> Unit,
    onInvestmentsChange: (String) -> Unit,
    onMoneyOwedToYouChange: (String) -> Unit,
    onStockValueChange: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Assets",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            CurrencyTextField(
                value = goldAndSilverAmount,
                onValueChange = onGoldAndSilverChange,
                label = "Gold and Silver Value",
                currencyInfo = currencyInfo
            )

            CurrencyTextField(
                value = cashAmount,
                onValueChange = onCashAmountChange,
                label = "Cash at Home & Bank Accounts",
                currencyInfo = currencyInfo
            )

            CurrencyTextField(
                value = otherSavings,
                onValueChange = onOtherSavingsChange,
                label = "Other Savings",
                currencyInfo = currencyInfo
            )

            CurrencyTextField(
                value = investmentsAmount,
                onValueChange = onInvestmentsChange,
                label = "Investment & Share Values",
                currencyInfo = currencyInfo
            )

            CurrencyTextField(
                value = moneyOwedToYou,
                onValueChange = onMoneyOwedToYouChange,
                label = "Money Owed to You",
                currencyInfo = currencyInfo
            )

            CurrencyTextField(
                value = stockValue,
                onValueChange = onStockValueChange,
                label = "Stock Value",
                currencyInfo = currencyInfo
            )
        }
    }
}

@Composable
private fun LiabilitiesSection(
    moneyYouOwe: String,
    otherOutgoingsDue: String,
    currencyInfo: CurrencyInfo,
    onMoneyYouOweChange: (String) -> Unit,
    onOtherOutgoingsDueChange: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "Liabilities",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            CurrencyTextField(
                value = moneyYouOwe,
                onValueChange = onMoneyYouOweChange,
                label = "Money You Owe",
                currencyInfo = currencyInfo
            )

            CurrencyTextField(
                value = otherOutgoingsDue,
                onValueChange = onOtherOutgoingsDueChange,
                label = "Other Outgoings Due",
                currencyInfo = currencyInfo
            )
        }
    }
}

@Composable
private fun CalculationResultCard(
    netAssets: Double,
    totalZakat: Double,
    nisabThreshold: Double,
    isEligible: Boolean,
    formatCurrency: (Double) -> String
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Calculate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Calculation Results",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Eligibility Status
            Surface(
                color = if (isEligible)
                    MaterialTheme.colorScheme.tertiaryContainer
                else
                    MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isEligible) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                        contentDescription = null,
                        tint = if (isEligible)
                            MaterialTheme.colorScheme.onTertiaryContainer
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (isEligible)
                            "Your wealth has reached the Nisab threshold"
                        else
                            "Your wealth has not yet reached the Nisab threshold",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isEligible)
                            MaterialTheme.colorScheme.onTertiaryContainer
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Main Results
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Total Zakat Section
                    if (isEligible) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Total Zakat Due",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = formatCurrency(totalZakat),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Detailed Breakdown
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailRow(
                            icon = Icons.Rounded.AccountBalance,
                            label = "Net Assets",
                            value = formatCurrency(netAssets)
                        )
                        DetailRow(
                            icon = Icons.AutoMirrored.Rounded.TrendingUp,
                            label = "Nisab Threshold",
                            value = formatCurrency(nisabThreshold)
                        )
                    }
                }
            }

            // Additional Info
            if (isEligible) {
                Text(
                    text = "Zakat is calculated at 2.5% of your net eligible assets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ZakatInfoCard() {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.settings_icon),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Zakat Information",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Text(
                text = "Zakat is calculated at 2.5% of your net eligible assets when they reach the Nisab threshold and are held for one lunar year.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun CurrencyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    currencyInfo: CurrencyInfo
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        leadingIcon = {
            Text(
                text = currencyInfo.symbol,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    )
}

@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun NisabTypeSelector(
    state: ZakatState,
    onNisabTypeChange: (NisabType) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.CompareArrows,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Select Nisab Type",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Nisab Type Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NisabType.entries.forEach { type ->
                    NisabTypeChip(
                        type = type,
                        selected = state.selectedNisabType == type,
                        onSelect = onNisabTypeChange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Info Text
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Choose the metal standard for calculating your Nisab threshold",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NisabTypeChip(
    type: NisabType,
    selected: Boolean,
    onSelect: (NisabType) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = when {
            selected -> when (type) {
                NisabType.GOLD -> MaterialTheme.colorScheme.primaryContainer
                NisabType.SILVER -> MaterialTheme.colorScheme.tertiaryContainer
            }

            else -> MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = 1.dp,
            color = when {
                selected -> when (type) {
                    NisabType.GOLD -> MaterialTheme.colorScheme.primary
                    NisabType.SILVER -> MaterialTheme.colorScheme.tertiary
                }

                else -> MaterialTheme.colorScheme.outline
            }
        ),
        onClick = { onSelect(type) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = when (type) {
                    NisabType.GOLD -> Icons.Rounded.LocalAtm
                    NisabType.SILVER -> Icons.Rounded.Savings
                },
                contentDescription = null,
                tint = when {
                    selected -> when (type) {
                        NisabType.GOLD -> MaterialTheme.colorScheme.onPrimaryContainer
                        NisabType.SILVER -> MaterialTheme.colorScheme.onTertiaryContainer
                    }

                    else -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = type.name.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = when {
                    selected -> when (type) {
                        NisabType.GOLD -> MaterialTheme.colorScheme.onPrimaryContainer
                        NisabType.SILVER -> MaterialTheme.colorScheme.onTertiaryContainer
                    }

                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun MetalPricesCard(state: ZakatState) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Nisab Thresholds",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MetalPriceItem(
                    modifier = Modifier.weight(1f),
                    title = "Gold Nisab",
                    value = state.goldNisabThreshold,
                    currencyInfo = state.currencyInfo,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    icon = Icons.Rounded.LocalAtm
                )

                MetalPriceItem(
                    modifier = Modifier.weight(1f),
                    title = "Silver Nisab",
                    value = state.silverNisabThreshold,
                    currencyInfo = state.currencyInfo,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                    icon = Icons.Rounded.Savings
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Update,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Live market rates",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "All amounts in ${state.currencyInfo.code}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetalPriceItem(
    modifier: Modifier = Modifier,
    title: String,
    value: Double,
    currencyInfo: CurrencyInfo,
    containerColor: Color,
    icon: ImageVector
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            fontSize = MaterialTheme.typography.bodySmall.fontSize,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    ) {
                        append(currencyInfo.symbol)
                    }
                    withStyle(
                        style = SpanStyle(
                            fontSize = MaterialTheme.typography.titleLarge.fontSize,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        append(String.format("%.2f", value))
                    }
                }
            )
        }
    }
}

@Composable
private fun ErrorSnackbar(
    message: String,
    onDismiss: () -> Unit
) {
    Snackbar(
        modifier = Modifier.padding(16.dp),
        action = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    ) {
        Text(message)
    }
}