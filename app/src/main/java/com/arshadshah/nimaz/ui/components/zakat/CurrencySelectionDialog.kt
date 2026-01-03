package com.arshadshah.nimaz.ui.components.zakat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.services.CurrencyInfo
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.NimazTextField
import com.arshadshah.nimaz.ui.components.common.NimazTextFieldType

data class CurrencyOption(
    val code: String,
    val symbol: String,
    val name: String,
    val flag: String = ""
)

object CurrencyList {
    val currencies = listOf(
        CurrencyOption("USD", "$", "US Dollar", "🇺🇸"),
        CurrencyOption("EUR", "€", "Euro", "🇪🇺"),
        CurrencyOption("GBP", "£", "British Pound", "🇬🇧"),
        CurrencyOption("JPY", "¥", "Japanese Yen", "🇯🇵"),
        CurrencyOption("CNY", "¥", "Chinese Yuan", "🇨🇳"),
        CurrencyOption("AUD", "A$", "Australian Dollar", "🇦🇺"),
        CurrencyOption("CAD", "C$", "Canadian Dollar", "🇨🇦"),
        CurrencyOption("CHF", "Fr", "Swiss Franc", "🇨🇭"),
        CurrencyOption("INR", "₹", "Indian Rupee", "🇮🇳"),
        CurrencyOption("SAR", "﷼", "Saudi Riyal", "🇸🇦"),
        CurrencyOption("AED", "د.إ", "UAE Dirham", "🇦🇪"),
        CurrencyOption("PKR", "₨", "Pakistani Rupee", "🇵🇰"),
        CurrencyOption("BDT", "৳", "Bangladeshi Taka", "🇧🇩"),
        CurrencyOption("TRY", "₺", "Turkish Lira", "🇹🇷"),
        CurrencyOption("MYR", "RM", "Malaysian Ringgit", "🇲🇾"),
        CurrencyOption("IDR", "Rp", "Indonesian Rupiah", "🇮🇩"),
        CurrencyOption("SGD", "S$", "Singapore Dollar", "🇸🇬"),
        CurrencyOption("KWD", "د.ك", "Kuwaiti Dinar", "🇰🇼"),
        CurrencyOption("QAR", "ر.ق", "Qatari Riyal", "🇶🇦"),
        CurrencyOption("OMR", "ر.ع.", "Omani Rial", "🇴🇲"),
        CurrencyOption("BHD", "د.ب", "Bahraini Dinar", "🇧🇭"),
        CurrencyOption("EGP", "£", "Egyptian Pound", "🇪🇬"),
        CurrencyOption("ZAR", "R", "South African Rand", "🇿🇦"),
        CurrencyOption("NGN", "₦", "Nigerian Naira", "🇳🇬"),
        CurrencyOption("KRW", "₩", "South Korean Won", "🇰🇷"),
        CurrencyOption("THB", "฿", "Thai Baht", "🇹🇭"),
        CurrencyOption("SEK", "kr", "Swedish Krona", "🇸🇪"),
        CurrencyOption("NOK", "kr", "Norwegian Krone", "🇳🇴"),
        CurrencyOption("DKK", "kr", "Danish Krone", "🇩🇰"),
        CurrencyOption("PLN", "zł", "Polish Złoty", "🇵🇱"),
        CurrencyOption("RUB", "₽", "Russian Ruble", "🇷🇺"),
        CurrencyOption("BRL", "R$", "Brazilian Real", "🇧🇷"),
        CurrencyOption("MXN", "$", "Mexican Peso", "🇲🇽"),
        CurrencyOption("NZD", "NZ$", "New Zealand Dollar", "🇳🇿"),
        CurrencyOption("HKD", "HK$", "Hong Kong Dollar", "🇭🇰"),
    )
}

@Composable
fun CurrencySelectionDialog(
    currentCurrency: CurrencyInfo,
    onDismiss: () -> Unit,
    onCurrencySelected: (code: String, symbol: String) -> Unit,
    onUseLocale: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredCurrencies = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            CurrencyList.currencies
        } else {
            CurrencyList.currencies.filter {
                it.code.contains(searchQuery, ignoreCase = true) ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.symbol.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialogNimaz(
        title = "Select Currency",
        contentDescription = "Choose your currency",
        contentToShow = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search Field
                NimazTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    type = NimazTextFieldType.SEARCH,
                    placeholder = "Search currency...",
                    leadingIconVector = Icons.Rounded.Search,
                    onSearchClick = { /* Search is immediate via filter */ }
                )

                // Current Currency Display
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Current",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${currentCurrency.code} (${currentCurrency.symbol})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Currency List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredCurrencies) { currency ->
                        CurrencyItem(
                            currency = currency,
                            isSelected = currency.code == currentCurrency.code,
                            onClick = {
                                onCurrencySelected(currency.code, currency.symbol)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        },
        contentHeight = 550.dp,
        cardContent = true,
        onDismissRequest = onDismiss,
        onConfirm = onUseLocale,
        confirmButtonText = "Use Auto",
        onDismiss = onDismiss,
        dismissButtonText = "Cancel"
    )
}

@Composable
private fun CurrencyItem(
    currency: CurrencyOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Flag
                if (currency.flag.isNotEmpty()) {
                    Text(
                        text = currency.flag,
                        style = MaterialTheme.typography.headlineSmall
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = currency.symbol,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currency.code,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = currency.symbol,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = currency.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

