package com.arshadshah.nimaz.ui.screens.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.ui.components.zakat.AssetsSection
import com.arshadshah.nimaz.ui.components.zakat.CalculationResultCard
import com.arshadshah.nimaz.ui.components.zakat.LiabilitiesSection
import com.arshadshah.nimaz.ui.components.zakat.NisabCard
import com.arshadshah.nimaz.ui.components.zakat.ZakatPeriodCard
import com.arshadshah.nimaz.viewModel.ZakatViewModel


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
                    OutlinedIconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
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
                    contentPadding = PaddingValues(8.dp),
                ) {

                    // Add the new Zakat Period card
                    item {
                        ZakatPeriodCard(
                            startDate = zakatState.zakatStartDate,
                            endDate = zakatState.zakatEndDate
                        )
                    }
                    // Show current metal prices
                    item {
                        NisabCard(
                            zakatState,
                            onNisabTypeChange = {
                                viewModel.setNisabType(it)
                            }
                        )
                    }

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

@Composable
private fun LoadingOverlay() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )

                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
