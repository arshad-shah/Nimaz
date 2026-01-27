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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.ZakatResultCard
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.ZakatEvent
import com.arshadshah.nimaz.presentation.viewmodel.ZakatViewModel

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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Nisab Type Selector
            item {
                NisabSelector(
                    selectedType = state.nisabType,
                    goldPrice = state.goldPricePerGram,
                    silverPrice = state.silverPricePerGram,
                    onTypeChange = { viewModel.onEvent(ZakatEvent.SetNisabType(it)) }
                )
            }

            // Assets Section
            item {
                AssetsSection(
                    cashOnHand = state.assets.cashOnHand,
                    bankBalance = state.assets.bankBalance,
                    goldGrams = state.assets.goldGrams,
                    silverGrams = state.assets.silverGrams,
                    investments = state.assets.investments,
                    businessInventory = state.assets.businessInventory,
                    receivables = state.assets.receivables,
                    otherAssets = state.assets.otherAssets,
                    onCashChange = { viewModel.onEvent(ZakatEvent.UpdateCash(it)) },
                    onBankChange = { viewModel.onEvent(ZakatEvent.UpdateBankBalance(it)) },
                    onGoldChange = { viewModel.onEvent(ZakatEvent.UpdateGold(it)) },
                    onSilverChange = { viewModel.onEvent(ZakatEvent.UpdateSilver(it)) },
                    onInvestmentsChange = { viewModel.onEvent(ZakatEvent.UpdateInvestments(it)) },
                    onInventoryChange = { viewModel.onEvent(ZakatEvent.UpdateBusinessInventory(it)) },
                    onReceivablesChange = { viewModel.onEvent(ZakatEvent.UpdateReceivables(it)) },
                    onOtherChange = { viewModel.onEvent(ZakatEvent.UpdateOtherAssets(it)) }
                )
            }

            // Liabilities Section
            item {
                LiabilitiesSection(
                    debts = state.liabilities.debts,
                    loans = state.liabilities.loans,
                    billsDue = state.liabilities.billsDue,
                    otherLiabilities = state.liabilities.otherLiabilities,
                    onDebtsChange = { viewModel.onEvent(ZakatEvent.UpdateDebts(it)) },
                    onLoansChange = { viewModel.onEvent(ZakatEvent.UpdateLoans(it)) },
                    onBillsChange = { viewModel.onEvent(ZakatEvent.UpdateBillsDue(it)) },
                    onOtherChange = { viewModel.onEvent(ZakatEvent.UpdateOtherLiabilities(it)) }
                )
            }

            // Result Card
            state.calculation?.let { calculation ->
                item {
                    ZakatResultCard(
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
        }
    }
}

@Composable
private fun NisabSelector(
    selectedType: NisabType,
    goldPrice: Double,
    silverPrice: Double,
    onTypeChange: (NisabType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Nisab Threshold",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Choose your calculation basis",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = selectedType == NisabType.GOLD,
                    onClick = { onTypeChange(NisabType.GOLD) },
                    label = {
                        Column {
                            Text("Gold", fontWeight = FontWeight.Bold)
                            Text(
                                text = "87.48g @ \$${goldPrice.toInt()}/g",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = selectedType == NisabType.SILVER,
                    onClick = { onTypeChange(NisabType.SILVER) },
                    label = {
                        Column {
                            Text("Silver", fontWeight = FontWeight.Bold)
                            Text(
                                text = "612.36g @ \$${silverPrice}/g",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AssetsSection(
    cashOnHand: Double,
    bankBalance: Double,
    goldGrams: Double,
    silverGrams: Double,
    investments: Double,
    businessInventory: Double,
    receivables: Double,
    otherAssets: Double,
    onCashChange: (Double) -> Unit,
    onBankChange: (Double) -> Unit,
    onGoldChange: (Double) -> Unit,
    onSilverChange: (Double) -> Unit,
    onInvestmentsChange: (Double) -> Unit,
    onInventoryChange: (Double) -> Unit,
    onReceivablesChange: (Double) -> Unit,
    onOtherChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = NimazColors.Primary.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Assets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NimazColors.Primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            AmountField("Cash on Hand", cashOnHand, onCashChange)
            AmountField("Bank Balance", bankBalance, onBankChange)
            AmountField("Gold (grams)", goldGrams, onGoldChange, suffix = "g")
            AmountField("Silver (grams)", silverGrams, onSilverChange, suffix = "g")
            AmountField("Investments", investments, onInvestmentsChange)
            AmountField("Business Inventory", businessInventory, onInventoryChange)
            AmountField("Receivables", receivables, onReceivablesChange)
            AmountField("Other Assets", otherAssets, onOtherChange)
        }
    }
}

@Composable
private fun LiabilitiesSection(
    debts: Double,
    loans: Double,
    billsDue: Double,
    otherLiabilities: Double,
    onDebtsChange: (Double) -> Unit,
    onLoansChange: (Double) -> Unit,
    onBillsChange: (Double) -> Unit,
    onOtherChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = NimazColors.StatusColors.Missed.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Liabilities",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NimazColors.StatusColors.Missed
            )

            Spacer(modifier = Modifier.height(16.dp))

            AmountField("Debts Owed", debts, onDebtsChange)
            AmountField("Loans", loans, onLoansChange)
            AmountField("Bills Due", billsDue, onBillsChange)
            AmountField("Other Liabilities", otherLiabilities, onOtherChange)
        }
    }
}

@Composable
private fun AmountField(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    suffix: String = "$",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = if (value == 0.0) "" else value.toString(),
        onValueChange = { text ->
            val newValue = text.toDoubleOrNull() ?: 0.0
            onValueChange(newValue)
        },
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        prefix = if (suffix == "$") { { Text("$") } } else null,
        suffix = if (suffix != "$") { { Text(suffix) } } else null
    )
}
