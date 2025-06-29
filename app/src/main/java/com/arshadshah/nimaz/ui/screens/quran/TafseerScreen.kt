package com.arshadshah.nimaz.ui.screens.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.ui.components.common.MixedScriptText
import com.arshadshah.nimaz.ui.components.common.PageErrorState
import com.arshadshah.nimaz.ui.components.common.PageLoading
import com.arshadshah.nimaz.ui.theme.almajeed
import com.arshadshah.nimaz.ui.theme.amiri
import com.arshadshah.nimaz.ui.theme.englishQuranTranslation
import com.arshadshah.nimaz.ui.theme.hidayat
import com.arshadshah.nimaz.ui.theme.quranFont
import com.arshadshah.nimaz.ui.theme.urduFont
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.utils.StringUtils.cleanTextFromBackslash
import com.arshadshah.nimaz.viewModel.TafsirViewModel
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafseerScreen(
    onNavigateBack: () -> Unit,
    ayaNumber: Int,
    surahNumber: Int,
    viewModel: TafsirViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(key1 = surahNumber, key2 = ayaNumber) {
        viewModel.handleEvent(
            TafsirViewModel.TafsirEvent.LoadTafsir(
                surahNumber = surahNumber,
                ayaNumber = ayaNumber,
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (uiState) {
                        is TafsirViewModel.TafsirUiState.Success -> {
                            val state = uiState as TafsirViewModel.TafsirUiState.Success
                            Column {
                                Text(
                                    text = state.surah.englishName.ifEmpty { "Surah $surahNumber" },
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Verse $ayaNumber",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        else -> {
                            Text(text = "Loading...")
                        }
                    }
                },
                navigationIcon = {
                    OutlinedIconButton(onClick = {
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is TafsirViewModel.TafsirUiState.Loading -> {
                PageLoading()
            }

            is TafsirViewModel.TafsirUiState.Error -> {
                PageErrorState(
                    message = (uiState as TafsirViewModel.TafsirUiState.Error).message,
                )
            }

            is TafsirViewModel.TafsirUiState.Success -> {
                val state = uiState as TafsirViewModel.TafsirUiState.Success
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Verse Section
                    item {
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Arabic Text
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    SelectionContainer {
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                            Text(
                                                text = state.aya?.ayaArabic?.cleanTextFromBackslash()
                                                    ?: "",
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontSize = state.settings.arabicFontSize.sp,
                                                fontFamily = when (state.settings.arabicFont) {
                                                    "Default" -> utmaniQuranFont
                                                    "Quranme" -> quranFont
                                                    "Hidayat" -> hidayat
                                                    "Amiri" -> amiri
                                                    "IndoPak" -> almajeed
                                                    else -> utmaniQuranFont
                                                },
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp)
                                            )
                                        }
                                    }
                                }

                                // Translation
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        when (state.settings.translationLanguage) {
                                            "Urdu" -> {
                                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                                    Text(
                                                        text = "${state.aya?.translationUrdu?.cleanTextFromBackslash()} ۔",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontSize = state.settings.translationFontSize.sp,
                                                        fontFamily = urduFont,
                                                        textAlign = TextAlign.Center,
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }

                                            else -> {
                                                Text(
                                                    text = state.aya?.translationEnglish?.cleanTextFromBackslash()
                                                        ?: "",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontFamily = englishQuranTranslation,
                                                    fontSize = state.settings.translationFontSize.sp,
                                                    textAlign = TextAlign.Center,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Tafseer Section with Mixed Script Support
                    item {
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Header Section
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier
                                                        .padding(8.dp)
                                                        .size(24.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = "Tafsir Ibn Kathir",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Verse ${state.aya?.ayaNumberInSurah}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                                        alpha = 0.8f
                                                    )
                                                )
                                            }
                                        }

                                        Surface(
                                            color = MaterialTheme.colorScheme.secondary,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = state.tafsir.language.replaceFirstChar {
                                                    if (it.isLowerCase()) it.titlecase(Locale.ROOT)
                                                    else it.toString()
                                                },
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSecondary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(
                                                    horizontal = 12.dp,
                                                    vertical = 8.dp
                                                )
                                            )
                                        }
                                    }
                                }

                                // Content Section with Mixed Script Support
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Mixed script tafsir content
                                        SelectionContainer {
                                            MixedScriptText(
                                                text = state.tafsir.content,
                                                arabicFontFamily = when (state.settings.arabicFont) {
                                                    "Default" -> utmaniQuranFont
                                                    "Quranme" -> quranFont
                                                    "Hidayat" -> hidayat
                                                    "Amiri" -> amiri
                                                    "IndoPak" -> almajeed
                                                    else -> utmaniQuranFont
                                                },
                                                englishFontFamily = englishQuranTranslation,
                                                urduFontFamily = urduFont,
                                                arabicFontSize = state.settings.arabicFontSize.sp,
                                                englishFontSize = if (state.settings.translationFontSize > 0f) {
                                                    state.settings.translationFontSize.sp
                                                } else {
                                                    18.sp
                                                },
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    lineHeight = 28.sp
                                                ),
                                                textAlign = TextAlign.Justify,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        // Divider
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp),
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        ) {}

                                        // Source citation
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "📚 Tafsir Ibn Kathir",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(
                                                        horizontal = 12.dp,
                                                        vertical = 8.dp
                                                    )
                                                )
                                            }

                                            Text(
                                                text = "Surah $surahNumber:$ayaNumber",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.7f
                                                ),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}