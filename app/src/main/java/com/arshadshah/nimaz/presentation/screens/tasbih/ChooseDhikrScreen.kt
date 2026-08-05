package com.arshadshah.nimaz.presentation.screens.tasbih

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.TasbihCategory
import com.arshadshah.nimaz.domain.model.TasbihPreset
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.molecules.NimazConfirmDialog
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.tracker.TasbihEvent
import com.arshadshah.nimaz.presentation.viewmodel.tracker.TasbihViewModel

private data class DhikrTab(
    val category: TasbihCategory? = null,
    val mine: Boolean = false,
    val favorites: Boolean = false,
)

@Composable
private fun tabLabel(tab: DhikrTab): String = when {
    tab.favorites -> "★"
    tab.mine -> stringResource(R.string.tasbih_category_mine)
    tab.category == TasbihCategory.AFTER_PRAYER -> stringResource(R.string.tasbih_category_after_prayer)
    tab.category == TasbihCategory.MORNING -> stringResource(R.string.tasbih_category_morning)
    tab.category == TasbihCategory.EVENING -> stringResource(R.string.tasbih_category_evening)
    else -> stringResource(R.string.all)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseDhikrScreen(
    onBack: () -> Unit,
    onNavigateToAddPreset: () -> Unit,
    viewModel: TasbihViewModel = hiltViewModel()
) {
    val presetsState by viewModel.presetsState.collectAsStateWithLifecycle()
    val counterState by viewModel.counterState.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var tabIndex by remember { mutableIntStateOf(0) }
    var presetToDelete by remember { mutableStateOf<TasbihPreset?>(null) }

    val tabs = remember {
        listOf(
            DhikrTab(),
            DhikrTab(favorites = true),
            DhikrTab(TasbihCategory.AFTER_PRAYER),
            DhikrTab(TasbihCategory.MORNING),
            DhikrTab(TasbihCategory.EVENING),
            DhikrTab(mine = true),
        )
    }

    val customIds = presetsState.customPresets.map { it.id }.toSet()
    val favorites = presetsState.favorites
    val all = presetsState.defaultPresets + presetsState.customPresets
    val tab = tabs[tabIndex]
    val filtered = all.filter { preset ->
        val byTab = when {
            tab.favorites -> preset.id in favorites
            tab.mine -> preset.id in customIds
            tab.category != null -> preset.category == tab.category
            else -> true
        }
        val q = query.trim()
        val byQuery = q.isEmpty() || listOfNotNull(
            preset.name, preset.translation, preset.transliteration, preset.arabicText
        ).any { it.contains(q, ignoreCase = true) }
        byTab && byQuery
    }

    presetToDelete?.let { preset ->
        NimazConfirmDialog(
            title = stringResource(R.string.tasbih_delete_preset_title),
            message = stringResource(R.string.tasbih_delete_preset_message),
            confirmText = stringResource(R.string.delete),
            cancelText = stringResource(R.string.cancel),
            titleIcon = Icons.Default.Delete,
            isDestructive = true,
            onConfirm = {
                viewModel.onEvent(TasbihEvent.DeleteCustomPreset(preset.id))
                presetToDelete = null
            },
            onDismiss = { presetToDelete = null }
        )
    }

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.tasbih_choose_dhikr),
                onBackClick = onBack,
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                placeholder = { Text(stringResource(R.string.tasbih_search_dhikr)) },
                leadingIcon = { NimazIcon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tabs.size) { i ->
                    NimazBadge(
                        text = tabLabel(tabs[i]),
                        tone = NimazTone.ACCENT,
                        size = NimazBadgeSize.LARGE,
                        selected = i == tabIndex,
                        onClick = { tabIndex = i })
                }
            }

            // Free count — count anything with your own target.
            FreeCountRow(
                selected = counterState.selectedPreset == null,
                onClick = {
                    viewModel.onEvent(TasbihEvent.ClearPreset)
                    onBack()
                }
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filtered, key = { it.id }) { preset ->
                    val isCustom = preset.id in customIds
                    SwipeableDhikrRow(
                        preset = preset,
                        selected = counterState.selectedPreset?.id == preset.id,
                        isFavorite = preset.id in favorites,
                        isCustom = isCustom,
                        onClick = {
                            viewModel.onEvent(TasbihEvent.SelectPreset(preset))
                            onBack()
                        },
                        onToggleFavorite = { viewModel.onEvent(TasbihEvent.ToggleFavorite(preset.id)) },
                        onRequestDelete = { presetToDelete = preset }
                    )
                }
            }

            NimazCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                style = NimazCardStyle.FILLED,
                onClick = { onNavigateToAddPreset() },
                shape = RoundedCornerShape(14.dp),
                tone = NimazTone.ACCENT
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NimazIcon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(R.string.new_tasbih),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableDhikrRow(
    preset: TasbihPreset,
    selected: Boolean,
    isFavorite: Boolean,
    isCustom: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    if (!isCustom) {
        DhikrRow(preset, selected, isFavorite, onClick, onToggleFavorite)
        return
    }
    // Custom presets: swipe end→start to delete (with confirmation).
    // Pass the confirmValueChange lambda into the state so we can intercept
    // attempts to dismiss (EndToStart) and trigger the confirmation dialog
    // without automatically completing the dismiss.
    val dismissState = rememberSwipeToDismissBoxState(
        initialValue = SwipeToDismissBoxValue.Settled,
        positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold
    )

    // Observe state changes and intercept an EndToStart target by showing the
    // confirmation dialog. We immediately reset the state so the item isn't
    // automatically dismissed; the dialog's confirm action will perform deletion.
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onRequestDelete()
            // Reset the state back to settled so the row remains visible.
            // reset() is a suspend function on the state.
            dismissState.reset()
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                NimazIcon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        DhikrRow(preset, selected, isFavorite, onClick, onToggleFavorite)
    }
}

@Composable
private fun FreeCountRow(selected: Boolean, onClick: () -> Unit) {
    NimazCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        style = if (selected) NimazCardStyle.OUTLINED else NimazCardStyle.FILLED,
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = NimazCardDefaults.selectable(
            activeBorder = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NimazIcon(
                Icons.Default.Calculate,
                contentDescription = null,
                tint = NimazColors.TasbihColors.Milestone
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.free_count_label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.tasbih_free_count_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DhikrRow(
    preset: TasbihPreset,
    selected: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    NimazCard(
        modifier = Modifier.fillMaxWidth(),
        style = if (selected) NimazCardStyle.OUTLINED else NimazCardStyle.FILLED,
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = NimazCardDefaults.selectable(
            container = MaterialTheme.colorScheme.surface,
            activeBorder = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            modifier = Modifier
                .padding(start = 12.dp, end = 6.dp)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!preset.arabicText.isNullOrEmpty()) {
                    ArabicText(
                        text = preset.arabicText,
                        size = ArabicTextSize.SMALL,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                NimazIcon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = stringResource(R.string.add_to_favorites),
                    tint = if (isFavorite) NimazColors.TasbihColors.Milestone
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    iconSize = 18.dp
                )
            }
            Box(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${preset.targetCount}×",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
