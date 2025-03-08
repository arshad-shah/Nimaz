package com.arshadshah.nimaz.ui.components.zakat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.services.CurrencyInfo
import com.arshadshah.nimaz.ui.components.common.HeaderWithIcon

@Composable
fun LiabilitiesSection(
    moneyYouOwe: String,
    otherOutgoingsDue: String,
    currencyInfo: CurrencyInfo,
    onMoneyYouOweChange: (String) -> Unit,
    onOtherOutgoingsDueChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            HeaderWithIcon(
                icon = Icons.AutoMirrored.Filled.TrendingDown,
                title = "Liabilities",
                contentDescription = "Liabilities calculation section",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )

            CurrencyInputField(
                value = moneyYouOwe,
                onValueChange = onMoneyYouOweChange,
                label = "Money You Owe",
                currencyInfo = currencyInfo
            )

            CurrencyInputField(
                value = otherOutgoingsDue,
                onValueChange = onOtherOutgoingsDueChange,
                label = "Other Outgoings Due",
                currencyInfo = currencyInfo
            )
        }
    }
}
