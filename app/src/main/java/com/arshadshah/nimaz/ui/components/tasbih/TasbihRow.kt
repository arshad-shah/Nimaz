package com.arshadshah.nimaz.ui.components.tasbih

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.models.LocalTasbih
import com.arshadshah.nimaz.ui.theme.englishQuranTranslation
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.viewModel.TasbihViewModel
import java.time.LocalDate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
@Composable
fun TasbihRow(
    arabicName: String,
    englishName: String,
    translationName: String,
    onNavigateToTasbihScreen: ((String, String, String, String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val viewModel = viewModel<TasbihViewModel>(
        key = AppConstants.TASBIH_VIEWMODEL_KEY,
        initializer = { TasbihViewModel(context) },
        viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity
    )

    val tasbih = remember { viewModel.tasbihCreated }.collectAsState()
    val navigateToTasbihScreen = remember { mutableStateOf(false) }
    val showTasbihDialog = remember { mutableStateOf(false) }

    LaunchedEffect(navigateToTasbihScreen.value) {
        if (navigateToTasbihScreen.value) {
            viewModel.getTasbih(tasbih.value.id)
            onNavigateToTasbihScreen?.invoke(
                tasbih.value.id.toString(),
                tasbih.value.arabicName,
                tasbih.value.englishName,
                tasbih.value.translationName
            )
            navigateToTasbihScreen.value = false
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = onNavigateToTasbihScreen != null) {
                if (onNavigateToTasbihScreen != null) {
                    showTasbihDialog.value = true
                }
            },
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Arabic Text Section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = arabicName,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = utmaniQuranFont,
                            fontWeight = FontWeight.SemiBold
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // English and Translation Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = englishName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = translationName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = englishQuranTranslation
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (onNavigateToTasbihScreen != null) {
                    IconButton(
                        onClick = { showTasbihDialog.value = true },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = "Start Tasbih",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }

    val goal = remember { mutableStateOf("") }

    TasbihGoalDialog(
        state = goal,
        onConfirm = {
            viewModel.createTasbih(
                LocalTasbih(
                    arabicName = arabicName,
                    englishName = englishName,
                    translationName = translationName,
                    goal = it.toInt(),
                    count = 0,
                    date = LocalDate.now()
                )
            )
            navigateToTasbihScreen.value = true
        },
        isOpen = showTasbihDialog
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