package com.arshadshah.nimaz.ui.screens.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.ui.components.common.BackButton
import com.arshadshah.nimaz.ui.components.common.BannerLarge
import com.arshadshah.nimaz.ui.components.common.PageErrorState
import com.arshadshah.nimaz.ui.components.common.PageLoading
import com.arshadshah.nimaz.ui.components.zakat.AssetsSection
import com.arshadshah.nimaz.ui.components.zakat.CalculationResultCard
import com.arshadshah.nimaz.ui.components.zakat.CurrencySelectionDialog
import com.arshadshah.nimaz.ui.components.zakat.LiabilitiesSection
import com.arshadshah.nimaz.ui.components.zakat.NisabCard
import com.arshadshah.nimaz.ui.components.zakat.ZakatPeriodCard
import com.arshadshah.nimaz.viewModel.ZakatViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatCalculator(navController: NavHostController) {
    val viewModel: ZakatViewModel = hiltViewModel()
    val zakatState by viewModel.zakatState.collectAsState()
    var showCurrencyDialog by remember { mutableStateOf(false) }

    val locale = LocalConfiguration.current.locales[0]
    LaunchedEffect(locale) {
        viewModel.updateCurrency(locale)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Zakat Calculator") },
                navigationIcon = {
                    BackButton(onBackClick = {
                        navController.popBackStack()
                    })
                },
                actions = {
                    // Currency selection button
                    IconButton(onClick = { showCurrencyDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = "Select currency"
                        )
                    }
                    // Refresh button
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
                PageLoading()
            } else {

                // Show error snackbar if there's an error
                zakatState.error?.let { error ->
                    PageErrorState(message = error)
                    val isOpen = remember { mutableStateOf(true) }
                    BannerLarge(
                        message = error,
                        title = "Error",
                        isOpen = isOpen,
                        onDismiss = { isOpen.value = false }
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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

    // Currency Selection Dialog
    if (showCurrencyDialog) {
        CurrencySelectionDialog(
            currentCurrency = zakatState.currencyInfo,
            onDismiss = { showCurrencyDialog = false },
            onCurrencySelected = { code, symbol ->
                viewModel.updateCurrencyManually(code, symbol)
            },
            onUseLocale = {
                viewModel.resetCurrencyToLocale()
                showCurrencyDialog = false
            }
        )
    }
}

