package com.arshadshah.nimaz.ui.screens.more

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.*

@Composable
fun ZakatCalculatorScreen(paddingValues : PaddingValues)
{
	val money = remember { mutableStateOf("") }
	val gold = remember { mutableStateOf("") }
	val silver = remember { mutableStateOf("") }
	val investments = remember { mutableStateOf("") }
	val properties = remember { mutableStateOf("") }
	val business = remember { mutableStateOf("") }
	val agriculture = remember { mutableStateOf("") }
	val cattle = remember { mutableStateOf("") }
	val preciousStones = remember { mutableStateOf("") }
	val payables = remember { mutableStateOf("") }
	val others = remember { mutableStateOf("") }

	//onvalue change
	val onMoneyChange : (String) -> Unit = { money.value = it }
	val onGoldChange : (String) -> Unit = { gold.value = it }
	val onSilverChange : (String) -> Unit = { silver.value = it }
	val onInvestmentsChange : (String) -> Unit = { investments.value = it }
	val onPropertiesChange : (String) -> Unit = { properties.value = it }
	val onBusinessChange : (String) -> Unit = { business.value = it }
	val onAgricultureChange : (String) -> Unit = { agriculture.value = it }
	val onCattleChange : (String) -> Unit = { cattle.value = it }
	val onPreciousStonesChange : (String) -> Unit = { preciousStones.value = it }
	val onPayablesChange : (String) -> Unit = { payables.value = it }
	val onOthersChange : (String) -> Unit = { others.value = it }

	val result = remember { mutableStateOf("") }

	//on calculate get all the values and calculate the result
	val onCalculate : () -> Unit = {
		val moneyValue = money.value.toDoubleOrNull() ?: 0.0
		val goldValue = gold.value.toDoubleOrNull() ?: 0.0
		val silverValue = silver.value.toDoubleOrNull() ?: 0.0
		val investmentsValue = investments.value.toDoubleOrNull() ?: 0.0
		val propertiesValue = properties.value.toDoubleOrNull() ?: 0.0
		val businessValue = business.value.toDoubleOrNull() ?: 0.0
		val agricultureValue = agriculture.value.toDoubleOrNull() ?: 0.0
		val cattleValue = cattle.value.toDoubleOrNull() ?: 0.0
		val preciousStonesValue = preciousStones.value.toDoubleOrNull() ?: 0.0
		val payablesValue = payables.value.toDoubleOrNull() ?: 0.0
		val othersValue = others.value.toDoubleOrNull() ?: 0.0

		val total =
			moneyValue + goldValue + silverValue + investmentsValue + propertiesValue + businessValue + agricultureValue + cattleValue + preciousStonesValue + payablesValue + othersValue

		result.value = "Your Zakat is ${total * 0.025}"
	}

	LazyColumn(
			contentPadding = paddingValues ,
			  ) {
		item {
			Category("Money" , money.value , onMoneyChange)
			Divider(
					modifier = Modifier.fillMaxWidth() ,
					color = MaterialTheme.colorScheme.outline
				   )
			Category("Gold" , gold.value , onGoldChange)
			Divider(
					modifier = Modifier.fillMaxWidth() ,
					color = MaterialTheme.colorScheme.outline
				   )
			Category("Silver" , silver.value , onSilverChange)
			Divider(
					modifier = Modifier.fillMaxWidth() ,
					color = MaterialTheme.colorScheme.outline
				   )
			Category("Investments" , investments.value , onInvestmentsChange)
			Divider(
					modifier = Modifier.fillMaxWidth() ,
					color = MaterialTheme.colorScheme.outline
				   )
			Category("Properties" , properties.value , onPropertiesChange)
			Divider(
					modifier = Modifier.fillMaxWidth() ,
					color = MaterialTheme.colorScheme.outline
				   )
			Category("Business" , business.value , onBusinessChange)
			Divider(
					modifier = Modifier.fillMaxWidth() ,
					color = MaterialTheme.colorScheme.outline
				   )
			Category("Agriculture" , agriculture.value , onAgricultureChange)
			Divider(
					modifier = Modifier.fillMaxWidth() ,
					color = MaterialTheme.colorScheme.outline
				   )
			Category("Cattle" , cattle.value , onCattleChange)
			Divider(
					modifier = Modifier.fillMaxWidth() ,
					color = MaterialTheme.colorScheme.outline
				   )
			Category("Precious Stones" , preciousStones.value , onPreciousStonesChange)
			Divider(
					modifier = Modifier.fillMaxWidth() ,
					color = MaterialTheme.colorScheme.outline
				   )
			Category("Payables" , payables.value , onPayablesChange)
			Divider(
					modifier = Modifier.fillMaxWidth() ,
					color = MaterialTheme.colorScheme.outline
				   )
			Category("Others" , others.value , onOthersChange)
			Divider(
					modifier = Modifier.fillMaxWidth() ,
					color = MaterialTheme.colorScheme.outline
				   )
			//Calculate Button
			//Result
			Button(
					modifier = Modifier
						.padding(16.dp)
						.fillMaxWidth() ,
					onClick = {
						onCalculate()
					}) {
				Text(
						"Calculate" ,
						modifier = Modifier.padding(16.dp) ,
						style = MaterialTheme.typography.titleLarge
					)
			}
		}
	}

}

//one Category with a label and a outlined text field it returns its value as the current users local currency
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Category(label : String , value : String , onValueChange : (String) -> Unit)
{
	Row(
			modifier = Modifier
				.padding(8.dp)
				.fillMaxWidth() ,
			horizontalArrangement = Arrangement.SpaceBetween ,
			verticalAlignment = Alignment.CenterVertically
	   ) {
		Text(text = label , style = MaterialTheme.typography.titleMedium)
		//a text field that returns the value as the current users local currency
		// current users local currency
		val currencySymbol = Currency.getInstance(Locale.getDefault()).symbol
		OutlinedTextField(
				shape = MaterialTheme.shapes.extraLarge ,
				value = value ,
				onValueChange = onValueChange ,
				modifier = Modifier
					//reduce size of the text field so that it accommodates the 100000,
					.width(100.dp) ,
				trailingIcon = {
					Text(text = currencySymbol , style = MaterialTheme.typography.titleLarge)
				} ,
				//only allow numbers
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number) ,

				)
	}
}