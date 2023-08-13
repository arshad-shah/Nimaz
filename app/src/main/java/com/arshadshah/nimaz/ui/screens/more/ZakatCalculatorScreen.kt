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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.arshadshah.nimaz.R
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
			Category(
					"Money" ,
					money.value ,
					"Your cash assets include all monies in your bank account(s) and at home or on your person. Any interest you have received at the bank is haram and must not be included." ,
					onMoneyChange
					)
		}
		item {
			//gold and silver
			Category(
					"Gold & Silver" ,
					gold.value ,
					"Most scholars are of the opinion that zakat should be paid on all gold and silver jewellery, whether it is worn or not, and even if it is owned by a man (who isn’t permitted to wear gold jewellery)." ,
					onGoldChange
					)
		}
		item {
			//property other than home
			Category(
					"Property" ,
					properties.value ,
					"Any property other than your home must be considered for zakat. If you are in the business of buying and then selling properties when they appreciate in value, then zakat is due on the current resale value of these properties. However, If you are in the business of letting properties (rather than buying and selling them), then zakat is due on savings made from this rental income only." ,
					onPropertiesChange
					)
		}
		item {
			//investments
			Category(
					"Investments" ,
					investments.value ,
					"Zakat is due on any investments you have made, including shares, stocks, pensions, ISAs, unit trusts, investment bonds, and any other type of investment." ,
					onInvestmentsChange
					)
		}
		item {
			//business
			Category(
					"Business" ,
					business.value ,
					"Add the total value of: cash in tills and at bank + stock for sale (current sale value) + raw materials (value at cost)." ,
					onBusinessChange
					)
		}
		item {
			//debt that you owe
			Category(
					"Payables" ,
					payables.value ,
					"If you have any debts that you owe, then you can deduct the total amount from your zakatable assets." ,
					onPayablesChange
					)
		}
		item {
			//debtor
			Category(
					"Debtor" ,
					others.value ,
					"If you have loaned money to someone and you are not sure if they will pay you back, then you can deduct the total amount from your zakatable assets." ,
					onOthersChange
					)
		}
		item {
			//agriculture
			Category(
					"Agriculture" ,
					agriculture.value ,
					"If you have any crops or produce that you have grown yourself, then you must pay zakat on the current market value of these items." ,
					onAgricultureChange
					)
		}
		item {
			//cattle
			Category(
					"Cattle" ,
					cattle.value ,
					"If you have any cattle, then you must pay zakat on the current market value of these animals." ,
					onCattleChange
					)
		}
		item {
			//precious stones
			Category(
					"Precious Stones" ,
					preciousStones.value ,
					"If you have any precious stones, then you must pay zakat on the current market value of these items." ,
					onPreciousStonesChange
					)
		}
		item {
			//nisaab
			Text(
					"Zakat is due on any wealth that is above the nisaab threshold. The nisaab threshold is the minimum amount of wealth a Muslim must have before zakat becomes due. The nisaab threshold is the equivalent of 87.48 grams of gold or 612.36 grams of silver." ,
					modifier = Modifier.padding(16.dp) ,
					style = MaterialTheme.typography.bodyMedium
				)
		}
		//result
		item {
			Text(
					result.value ,
					modifier = Modifier.padding(16.dp) ,
					style = MaterialTheme.typography.titleLarge
				)
		}
		item {
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
fun Category(
	label : String ,
	value : String ,
	explaination : String ,
	onValueChange : (String) -> Unit ,
			)
{
	val isPopupVisible = remember { mutableStateOf(false) }
	Row(
			modifier = Modifier
				.padding(8.dp)
				.fillMaxWidth() ,
			horizontalArrangement = Arrangement.SpaceBetween ,
			verticalAlignment = Alignment.CenterVertically
	   ) {
		Row(
				modifier = Modifier ,
				horizontalArrangement = Arrangement.SpaceBetween ,
				verticalAlignment = Alignment.CenterVertically
		   ) {
			Text(text = label , style = MaterialTheme.typography.titleMedium)
			// a info icon that shows a popup with the description of the category
			IconButton(
					onClick = {
						//show a popup with the description of the category
						isPopupVisible.value = true
					} ,
					modifier = Modifier
						.size(24.dp)
						.padding(4.dp)
					  ) {
				Icon(
						painter = painterResource(id = R.drawable.info_icon) ,
						contentDescription = "Info" ,
					)
			}
			if (isPopupVisible.value)
			{
				Popup(
						offset = IntOffset(- 50 , 50) ,
						onDismissRequest = {
							isPopupVisible.value = false
						} ,
						content = {
							Surface(
									modifier = Modifier
										.padding(16.dp) ,
									color = MaterialTheme.colorScheme.surface
								   ) {
								Text(
										text = explaination ,
										style = MaterialTheme.typography.bodyMedium
									)
							}
						}
					 )
			}
		}

		//a text field that returns the value as the current users local currency
		// current users local currency
		val currencySymbol = Currency.getInstance(Locale.getDefault()).symbol
		OutlinedTextField(
				shape = MaterialTheme.shapes.extraLarge ,
				value = value ,
				onValueChange = onValueChange ,
				modifier = Modifier
					//reduce size of the text field so that it accommodates the 100000,
					.width(150.dp) ,
				leadingIcon = {
					Text(text = currencySymbol , style = MaterialTheme.typography.titleLarge)
				} ,
				//only allow numbers
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number) ,

				)
	}
}

@Preview(showBackground = true , backgroundColor = 0xFFFFFFFF)
@Composable
fun Preview()
{
	ZakatCalculatorScreen(
			paddingValues = PaddingValues(16.dp)
						 )
}