package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.AppLanguage
import com.arshadshah.nimaz.presentation.viewmodel.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val generalState by viewModel.generalState.collectAsState()

    Scaffold(
        containerColor = NimazColors.Neutral950,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Language",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(40.dp)
                            .background(
                                color = NimazColors.Neutral900,
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NimazColors.Neutral300
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NimazColors.Neutral950
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Section Title
            item {
                Text(
                    text = "APP LANGUAGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = NimazColors.Neutral500,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 5.dp, bottom = 12.dp)
                )
            }

            // Language Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = NimazColors.Neutral900
                    )
                ) {
                    Column {
                        AppLanguage.entries.forEachIndexed { index, language ->
                            LanguageItem(
                                language = language,
                                isSelected = generalState.language == language,
                                onClick = { viewModel.onEvent(SettingsEvent.SetLanguage(language)) }
                            )

                            // Add divider between items (not after last item)
                            if (index < AppLanguage.entries.size - 1) {
                                HorizontalDivider(
                                    color = NimazColors.Neutral800,
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(start = 67.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Info Text
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Changing the language will restart the app and apply the new language to all screens.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NimazColors.Neutral500,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 5.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun LanguageItem(
    language: AppLanguage,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) {
                    NimazColors.Primary.copy(alpha = 0.1f)
                } else {
                    Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Flag
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = NimazColors.Neutral800,
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = language.flag,
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.width(15.dp))

        // Language Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = language.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
            Text(
                text = language.nativeName,
                style = MaterialTheme.typography.bodySmall,
                color = NimazColors.Neutral500,
                fontSize = 13.sp
            )
        }

        // Check Mark
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = NimazColors.Primary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
