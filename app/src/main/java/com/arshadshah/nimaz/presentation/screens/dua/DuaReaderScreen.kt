package com.arshadshah.nimaz.presentation.screens.dua

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.Dua
import com.arshadshah.nimaz.domain.model.TasbihCategory
import com.arshadshah.nimaz.domain.model.TasbihPreset
import com.arshadshah.nimaz.presentation.components.atoms.DuaArabicText
import com.arshadshah.nimaz.presentation.components.atoms.NimazLabelChip
import com.arshadshah.nimaz.presentation.components.atoms.NimazPillActionButton
import com.arshadshah.nimaz.presentation.components.molecules.NimazReaderBottomBar
import com.arshadshah.nimaz.presentation.components.templates.ReaderPagerScaffold
import com.arshadshah.nimaz.presentation.theme.AdaptiveSpacing
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.DuaEvent
import com.arshadshah.nimaz.presentation.viewmodel.DuaReaderUiState
import com.arshadshah.nimaz.presentation.viewmodel.DuaViewModel
import com.arshadshah.nimaz.presentation.viewmodel.TasbihEvent
import com.arshadshah.nimaz.presentation.viewmodel.TasbihViewModel

@Composable
fun DuaReaderScreen(
    duaId: String,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: DuaViewModel = hiltViewModel(),
    tasbihViewModel: TasbihViewModel = hiltViewModel()
) {
    val state by viewModel.readerState.collectAsState()
    val duas = state.duas

    LaunchedEffect(duaId) {
        viewModel.onEvent(DuaEvent.LoadDua(duaId))
    }

    ReaderPagerScaffold(
        items = duas,
        targetIndex = state.initialIndex,
        isLoading = state.isLoading,
        title = { current -> current?.titleEnglish ?: stringResource(R.string.dua_reader_loading) },
        onNavigateBack = onNavigateBack,
        onSettingsClick = onNavigateToSettings,
        settingsContentDescription = stringResource(R.string.dua_settings),
        emptyText = stringResource(R.string.dua_reader_not_found),
        itemKey = { it.id },
        pageContent = { dua -> DuaPage(dua = dua, state = state) },
        bottomBar = { dua, currentPage, pageCount, onPrev, onNext ->
            DuaReaderBottomBar(
                dua = dua,
                viewModel = viewModel,
                tasbihViewModel = tasbihViewModel,
                currentPage = currentPage,
                pageCount = pageCount,
                onPrev = onPrev,
                onNext = onNext
            )
        }
    )
}

/**
 * A single dua, laid out as a vertically-centred flowing reading column. Short
 * duas sit centred in the viewport (so the screen never looks empty); long ones
 * grow past the viewport and scroll, thanks to [heightIn] + [verticalScroll].
 */
@Composable
private fun DuaPage(
    dua: Dua,
    state: DuaReaderUiState
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val minColumnHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = AdaptiveSpacing.maxReadableWidth())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .heightIn(min = minColumnHeight),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            dua.occasion?.let { occasion ->
                NimazLabelChip(text = occasion.displayName(), highlighted = true)
                Spacer(modifier = Modifier.height(22.dp))
            }

            if (state.showArabic) {
                DuaArabicText(
                    text = dua.textArabic,
                    customFontSize = state.arabicFontSize,
                    fontFamily = state.arabicFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (state.showTransliteration && !dua.textTransliteration.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = dua.textTransliteration,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = state.fontSize.sp,
                        lineHeight = (state.fontSize * 1.6f).sp
                    ),
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }

            if (state.showTranslation) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = dua.textEnglish,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = state.fontSize.sp,
                        lineHeight = (state.fontSize * 1.6f).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            val repeatCount = dua.repeatCount ?: 0
            if (!dua.reference.isNullOrEmpty() || repeatCount > 0) {
                Spacer(modifier = Modifier.height(22.dp))
                DuaMetaChips(reference = dua.reference, repeatCount = repeatCount)
            }

            if (!dua.benefits.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(22.dp))
                VirtueCard(text = dua.benefits)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DuaMetaChips(
    reference: String?,
    repeatCount: Int
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!reference.isNullOrEmpty()) {
            NimazLabelChip(text = reference, icon = Icons.Default.Book)
        }
        if (repeatCount > 0) {
            val label = if (repeatCount > 1) {
                stringResource(R.string.dua_reader_recite_times, repeatCount)
            } else {
                stringResource(R.string.dua_reader_recite_once, repeatCount)
            }
            NimazLabelChip(text = label, icon = Icons.Default.Refresh)
        }
    }
}

@Composable
private fun VirtueCard(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.dua_reader_virtue),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * The consolidated bottom bar: prev/next chevrons flanking the action pill
 * (Favourite · Add-to-Tasbih · Share) in a single row, with the page indicator
 * directly beneath. Reuses the shared [NimazActionPill] so it matches the Quran
 * reader exactly. Chevrons disable at the ends of the collection; the indicator
 * falls back to a "current / total" counter when there are too many duas for dots.
 */
@Composable
private fun DuaReaderBottomBar(
    dua: Dua,
    viewModel: DuaViewModel,
    tasbihViewModel: TasbihViewModel,
    currentPage: Int,
    pageCount: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val favoriteFlow = remember(dua.id) { viewModel.isDuaFavorite(dua.id) }
    val isFavorite by favoriteFlow.collectAsState(initial = false)

    val addedToTasbihMsg = stringResource(R.string.dua_reader_added_tasbih)
    val shareLabel = stringResource(R.string.dua_reader_share)
    val sourceLabel = if (!dua.reference.isNullOrEmpty()) {
        stringResource(R.string.dua_reader_source_label, dua.reference)
    } else {
        ""
    }
    NimazReaderBottomBar(
        currentPage = currentPage,
        pageCount = pageCount,
        onPrev = onPrev,
        onNext = onNext,
        prevContentDescription = stringResource(R.string.dua_reader_prev),
        nextContentDescription = stringResource(R.string.dua_reader_next)
    ) {
        NimazPillActionButton(
            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = stringResource(R.string.dua_reader_favorite),
            onClick = { viewModel.onEvent(DuaEvent.ToggleFavorite(dua.id, dua.categoryId)) },
            active = isFavorite,
            activeColor = NimazColors.ReaderActionColors.FavoriteActive
        )
        NimazPillActionButton(
            icon = Icons.Default.Add,
            contentDescription = stringResource(R.string.dua_reader_add_tasbih),
            onClick = {
                tasbihViewModel.onEvent(TasbihEvent.CreateCustomPreset(dua.toTasbihPreset()))
                Toast.makeText(context, addedToTasbihMsg, Toast.LENGTH_SHORT).show()
            }
        )
        NimazPillActionButton(
            icon = Icons.Default.Share,
            contentDescription = shareLabel,
            onClick = {
                val textToShare = buildString {
                    appendLine(dua.titleEnglish)
                    appendLine()
                    appendLine(dua.textArabic)
                    appendLine()
                    if (!dua.textTransliteration.isNullOrEmpty()) {
                        appendLine(dua.textTransliteration)
                        appendLine()
                    }
                    appendLine(dua.textEnglish)
                    if (sourceLabel.isNotEmpty()) {
                        appendLine()
                        appendLine(sourceLabel)
                    }
                }
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, textToShare)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, shareLabel))
            }
        )
    }
}

private fun Dua.toTasbihPreset(): TasbihPreset {
    val now = System.currentTimeMillis()
    val presetName = titleEnglish.trim().let {
        if (it.length > 40) it.take(40).trimEnd() + "…" else it
    }
    return TasbihPreset(
        id = 0,
        name = presetName.ifBlank { titleArabic.trim() },
        arabicText = textArabic.ifBlank { null },
        transliteration = textTransliteration?.ifBlank { null },
        translation = textEnglish.ifBlank { null },
        targetCount = repeatCount?.takeIf { it > 0 } ?: 33,
        category = TasbihCategory.CUSTOM,
        reference = reference?.ifBlank { null },
        isDefault = false,
        displayOrder = 0,
        createdAt = now,
        updatedAt = now
    )
}
