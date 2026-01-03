package com.arshadshah.nimaz.ui.components.zakat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.services.CurrencyInfo
import com.arshadshah.nimaz.ui.components.common.HeaderWithIcon


@Composable
fun AssetsSection(
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
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeaderWithIcon(
                icon = Icons.Default.Inventory,
                title = "Assets",
                contentDescription = "Assets calculation section",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )

            CurrencyInputField(
                value = goldAndSilverAmount,
                onValueChange = onGoldAndSilverChange,
                label = "Gold and Silver Value",
                currencyInfo = currencyInfo
            )

            CurrencyInputField(
                value = cashAmount,
                onValueChange = onCashAmountChange,
                label = "Cash at Home & Bank Accounts",
                currencyInfo = currencyInfo
            )

            CurrencyInputField(
                value = otherSavings,
                onValueChange = onOtherSavingsChange,
                label = "Other Savings",
                currencyInfo = currencyInfo
            )

            CurrencyInputField(
                value = investmentsAmount,
                onValueChange = onInvestmentsChange,
                label = "Investment & Share Values",
                currencyInfo = currencyInfo
            )

            CurrencyInputField(
                value = moneyOwedToYou,
                onValueChange = onMoneyOwedToYouChange,
                label = "Money Owed to You",
                currencyInfo = currencyInfo
            )

            CurrencyInputField(
                value = stockValue,
                onValueChange = onStockValueChange,
                label = "Stock Value",
                currencyInfo = currencyInfo
            )
        }
    }
}

@Preview
@Composable
fun AssetsSectionPreview() {
    AssetsSection(
        goldAndSilverAmount = "",
        cashAmount = "",
        otherSavings = "",
        investmentsAmount = "",
        moneyOwedToYou = "",
        stockValue = "",
        currencyInfo = CurrencyInfo(
            code = "USD",
            symbol = "$",
            exchangeRate = 1.0
        ),
        onGoldAndSilverChange = {},
        onCashAmountChange = {},
        onOtherSavingsChange = {},
        onInvestmentsChange = {},
        onMoneyOwedToYouChange = {},
        onStockValueChange = {}
    )
}