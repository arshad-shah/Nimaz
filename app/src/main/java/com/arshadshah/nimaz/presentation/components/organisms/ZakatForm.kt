package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.domain.model.ZakatAssets
import com.arshadshah.nimaz.domain.model.ZakatConstants
import com.arshadshah.nimaz.domain.model.ZakatLiabilities
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.theme.NimazColors

/**
 * Complete Zakat calculator form with assets and liabilities.
 */
@Composable
fun ZakatCalculatorForm(
    assets: ZakatAssets,
    liabilities: ZakatLiabilities,
    nisabType: NisabType,
    nisabValue: Double,
    goldPrice: Double,
    silverPrice: Double,
    currency: String,
    modifier: Modifier = Modifier,
    onAssetsChange: (ZakatAssets) -> Unit = {},
    onLiabilitiesChange: (ZakatLiabilities) -> Unit = {},
    onNisabTypeChange: (NisabType) -> Unit = {},
    onCalculate: () -> Unit = {}
) {
    var assetsExpanded by remember { mutableStateOf(true) }
    var liabilitiesExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Nisab info card
        item {
            NisabInfoCard(
                nisabType = nisabType,
                nisabValue = nisabValue,
                goldPrice = goldPrice,
                silverPrice = silverPrice,
                currency = currency,
                onNisabTypeChange = onNisabTypeChange
            )
        }

        // Assets section
        item {
            AssetsSection(
                assets = assets,
                currency = currency,
                expanded = assetsExpanded,
                onExpandChange = { assetsExpanded = it },
                onAssetsChange = onAssetsChange
            )
        }

        // Liabilities section
        item {
            LiabilitiesSection(
                liabilities = liabilities,
                currency = currency,
                expanded = liabilitiesExpanded,
                onExpandChange = { liabilitiesExpanded = it },
                onLiabilitiesChange = onLiabilitiesChange
            )
        }

        // Calculate button
        item {
            NimazButton(
                text = "Calculate Zakat",
                onClick = onCalculate,
                modifier = Modifier.fillMaxWidth(),
                size = NimazButtonSize.LARGE,
                leadingIcon = Icons.Default.Calculate
            )
        }
    }
}

/**
 * Nisab information card.
 */
@Composable
private fun NisabInfoCard(
    nisabType: NisabType,
    nisabValue: Double,
    goldPrice: Double,
    silverPrice: Double,
    currency: String,
    onNisabTypeChange: (NisabType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Nisab Threshold",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nisab type selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NisabTypeButton(
                    type = NisabType.GOLD,
                    isSelected = nisabType == NisabType.GOLD,
                    price = goldPrice,
                    currency = currency,
                    onClick = { onNisabTypeChange(NisabType.GOLD) },
                    modifier = Modifier.weight(1f)
                )
                NisabTypeButton(
                    type = NisabType.SILVER,
                    isSelected = nisabType == NisabType.SILVER,
                    price = silverPrice,
                    currency = currency,
                    onClick = { onNisabTypeChange(NisabType.SILVER) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Current Nisab: $currency ${String.format("%.2f", nisabValue)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Zakat is obligatory if your zakatable wealth exceeds this amount for one lunar year.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NisabTypeButton(
    type: NisabType,
    isSelected: Boolean,
    price: Double,
    currency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        if (type == NisabType.GOLD) NimazColors.ZakatColors.Gold else NimazColors.ZakatColors.Silver
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor.copy(alpha = if (isSelected) 0.2f else 1f),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (type == NisabType.GOLD) "Gold" else "Silver",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) {
                    if (type == NisabType.GOLD) NimazColors.ZakatColors.Gold else Color.Gray
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = type.displayName(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$currency ${String.format("%.2f", price)}/g",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Assets input section.
 */
@Composable
private fun AssetsSection(
    assets: ZakatAssets,
    currency: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onAssetsChange: (ZakatAssets) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(NimazColors.ZakatColors.Cash.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = NimazColors.ZakatColors.Cash,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Assets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Total: $currency ${String.format("%.2f", assets.total)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NimazColors.ZakatColors.Cash
                        )
                    }
                }

                IconButton(onClick = { onExpandChange(!expanded) }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider()

                    AssetInputField(
                        label = "Cash on Hand",
                        value = assets.cashOnHand,
                        currency = currency,
                        icon = Icons.Default.AttachMoney,
                        onValueChange = { onAssetsChange(assets.copy(cashOnHand = it)) }
                    )

                    AssetInputField(
                        label = "Bank Balance",
                        value = assets.bankBalance,
                        currency = currency,
                        icon = Icons.Default.AccountBalance,
                        onValueChange = { onAssetsChange(assets.copy(bankBalance = it)) }
                    )

                    AssetInputField(
                        label = "Gold (grams)",
                        value = assets.goldGrams,
                        currency = currency,
                        icon = Icons.Default.MonetizationOn,
                        iconColor = NimazColors.ZakatColors.Gold,
                        onValueChange = { onAssetsChange(assets.copy(goldGrams = it)) }
                    )

                    AssetInputField(
                        label = "Silver (grams)",
                        value = assets.silverGrams,
                        currency = currency,
                        icon = Icons.Default.MonetizationOn,
                        iconColor = NimazColors.ZakatColors.Silver,
                        onValueChange = { onAssetsChange(assets.copy(silverGrams = it)) }
                    )

                    AssetInputField(
                        label = "Investments",
                        value = assets.investments,
                        currency = currency,
                        icon = Icons.Default.TrendingUp,
                        onValueChange = { onAssetsChange(assets.copy(investments = it)) }
                    )

                    AssetInputField(
                        label = "Business Inventory",
                        value = assets.businessInventory,
                        currency = currency,
                        icon = Icons.Default.Business,
                        onValueChange = { onAssetsChange(assets.copy(businessInventory = it)) }
                    )

                    AssetInputField(
                        label = "Money Owed to You",
                        value = assets.receivables,
                        currency = currency,
                        icon = Icons.Default.AttachMoney,
                        onValueChange = { onAssetsChange(assets.copy(receivables = it)) }
                    )

                    AssetInputField(
                        label = "Other Assets",
                        value = assets.otherAssets,
                        currency = currency,
                        icon = Icons.Default.AttachMoney,
                        onValueChange = { onAssetsChange(assets.copy(otherAssets = it)) }
                    )
                }
            }
        }
    }
}

/**
 * Liabilities input section.
 */
@Composable
private fun LiabilitiesSection(
    liabilities: ZakatLiabilities,
    currency: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onLiabilitiesChange: (ZakatLiabilities) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Liabilities",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Total: $currency ${String.format("%.2f", liabilities.total)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                IconButton(onClick = { onExpandChange(!expanded) }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider()

                    AssetInputField(
                        label = "Personal Debts",
                        value = liabilities.debts,
                        currency = currency,
                        icon = Icons.Default.AttachMoney,
                        onValueChange = { onLiabilitiesChange(liabilities.copy(debts = it)) }
                    )

                    AssetInputField(
                        label = "Loans",
                        value = liabilities.loans,
                        currency = currency,
                        icon = Icons.Default.AccountBalance,
                        onValueChange = { onLiabilitiesChange(liabilities.copy(loans = it)) }
                    )

                    AssetInputField(
                        label = "Bills Due",
                        value = liabilities.billsDue,
                        currency = currency,
                        icon = Icons.Default.CreditCard,
                        onValueChange = { onLiabilitiesChange(liabilities.copy(billsDue = it)) }
                    )

                    AssetInputField(
                        label = "Other Liabilities",
                        value = liabilities.otherLiabilities,
                        currency = currency,
                        icon = Icons.Default.Home,
                        onValueChange = { onLiabilitiesChange(liabilities.copy(otherLiabilities = it)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AssetInputField(
    label: String,
    value: Double,
    currency: String,
    icon: ImageVector,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    iconColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        OutlinedTextField(
            value = if (value == 0.0) "" else value.toString(),
            onValueChange = { newValue ->
                onValueChange(newValue.toDoubleOrNull() ?: 0.0)
            },
            label = { Text(label) },
            prefix = { Text("$currency ") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

/**
 * Zakat calculation result card.
 */
@Composable
fun ZakatResultCard(
    totalAssets: Double,
    totalLiabilities: Double,
    netWorth: Double = totalAssets - totalLiabilities,
    nisabValue: Double,
    isAboveNisab: Boolean = netWorth >= nisabValue,
    zakatDue: Double,
    currency: String,
    modifier: Modifier = Modifier,
    onSaveClick: () -> Unit = {}
) {
    val zakatableAmount = netWorth

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isAboveNisab) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Calculate,
                contentDescription = null,
                tint = if (isAboveNisab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Zakat Due",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "$currency ${String.format("%.2f", zakatDue)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (isAboveNisab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            if (!isAboveNisab) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your wealth is below Nisab. No Zakat is due.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            // Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Assets",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currency ${String.format("%.2f", totalAssets)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Liabilities",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "- $currency ${String.format("%.2f", totalLiabilities)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Zakatable Wealth",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currency ${String.format("%.2f", zakatableAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Zakat Rate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(ZakatConstants.ZAKAT_RATE * 100)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            NimazButton(
                text = "Save Calculation",
                onClick = onSaveClick,
                variant = NimazButtonVariant.TONAL,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
