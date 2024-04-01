package com.arshadshah.nimaz.ui.components.tasbih

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.models.LocalTasbih
import com.arshadshah.nimaz.ui.theme.englishQuranTranslation
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.viewModel.TasbihViewModel
import java.time.LocalDate


@Composable
fun TasbihRow(
    arabicName: String,
    englishName: String,
    translationName: String,
    onNavigateToTasbihScreen: ((String, String, String, String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val viewModel = viewModel(
        key = AppConstants.TASBIH_VIEWMODEL_KEY,
        initializer = { TasbihViewModel(context) },
        viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity
    )
    val tasbih = remember {
        viewModel.tasbihCreated
    }.collectAsState()

    val navigateToTasbihScreen = remember {
        mutableStateOf(false)
    }

    LaunchedEffect(key1 = navigateToTasbihScreen.value) {
        if (navigateToTasbihScreen.value) {
            viewModel.handleEvent(TasbihViewModel.TasbihEvent.GetTasbih(tasbih.value.id))
            //navigate to tasbih screen
            onNavigateToTasbihScreen?.invoke(
                tasbih.value.id.toString(),
                tasbih.value.arabicName,
                tasbih.value.englishName,
                tasbih.value.translationName
            )
            navigateToTasbihScreen.value = false
        }
    }
    val showTasbihDialog = remember {
        mutableStateOf(false)
    }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable(
                    //disable it if onNavigateToTasbihScreen has no implementation
                    enabled = onNavigateToTasbihScreen != null,
                ) {
                    if (onNavigateToTasbihScreen != null) {
                        showTasbihDialog.value = true
                    }
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    .weight(0.80f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center,
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = arabicName,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 28.sp,
                        fontFamily = utmaniQuranFont,
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth(),
                    text = englishName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth(),
                    text = translationName,
                    fontFamily = englishQuranTranslation,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

    val goal = remember {
        mutableStateOf("")
    }

    TasbihGoalDialog(
        state = goal,
        onConfirm = {
            viewModel.handleEvent(
                TasbihViewModel.TasbihEvent.SetTasbih(
                    LocalTasbih(
                        arabicName = arabicName,
                        englishName = englishName,
                        translationName = translationName,
                        goal = it.toInt(),
                        count = 0,
                        date = LocalDate.now(),
                    )
                )
            )
            navigateToTasbihScreen.value = true
        },
        isOpen = showTasbihDialog,
    )
}

@Preview
@Composable
fun TasbihRowPreview() {
    TasbihRow(
        englishName = "Tasbih",
        arabicName = "تسبيح",
        translationName = "Praise",
        onNavigateToTasbihScreen = null
    )
}