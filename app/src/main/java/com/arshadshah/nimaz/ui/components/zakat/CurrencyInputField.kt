package com.arshadshah.nimaz.ui.components.zakat

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.services.CurrencyInfo
import com.arshadshah.nimaz.ui.components.common.NimazTextField
import com.arshadshah.nimaz.ui.components.common.NimazTextFieldType

@Composable
fun CurrencyInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    currencyInfo: CurrencyInfo,
    modifier: Modifier = Modifier
) {
    NimazTextField(
        value = value,
        onValueChange = onValueChange,
        type = NimazTextFieldType.CURRENCY,
        label = label,
        placeholder = "0.00",
        leadingText = currencyInfo.symbol,
        requestFocus = true,
        modifier = modifier
    )
}


@Preview
@Composable
fun CurrencyInputFieldPreview() {
    CurrencyInputField(
        value = "1500",
        onValueChange = {},
        label = "Cash at Home & Bank Accounts",
        currencyInfo = CurrencyInfo(
            code = "USD",
            symbol = "$",
            exchangeRate = 1.0
        ),
        modifier = Modifier.padding(16.dp)
    )
}