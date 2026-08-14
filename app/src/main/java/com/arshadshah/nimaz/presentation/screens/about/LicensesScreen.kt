package com.arshadshah.nimaz.presentation.screens.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.LicenseFamily
import com.arshadshah.nimaz.domain.model.OpenSourceLibrary
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazFilterChip
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionTitle
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.molecules.NimazBanner
import com.arshadshah.nimaz.presentation.components.molecules.NimazBannerVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorDefaults
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorState
import com.arshadshah.nimaz.presentation.components.molecules.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.molecules.NimazStatCard
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazSearchBar
import com.arshadshah.nimaz.presentation.viewmodel.about.LicenseGrouping
import com.arshadshah.nimaz.presentation.viewmodel.about.LicenseSection
import com.arshadshah.nimaz.presentation.viewmodel.about.LicensesEvent
import com.arshadshah.nimaz.presentation.viewmodel.about.LicensesListUiState
import com.arshadshah.nimaz.presentation.viewmodel.about.LicensesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: LicensesViewModel = hiltViewModel(),
) {
    val state by viewModel.listState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onEvent(LicensesEvent.LoadLibraries) }

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.licenses_title),
                subtitle = if (state.libraries.isEmpty()) null else stringResource(
                    R.string.licenses_subtitle_format,
                    state.totalCount,
                    state.licenceCount,
                ),
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        // Bound to a local so the null check smart-casts: `state` is a delegated
        // property, so `state.error` does not, and `!!` at every use is not the answer.
        val error = state.error
        when {
            state.isLoading && state.libraries.isEmpty() ->
                NimazLoadingState(modifier = Modifier.padding(paddingValues))

            error != null -> NimazErrorState(
                title = stringResource(R.string.licenses_load_failed),
                message = stringResource(R.string.licenses_load_failed_body),
                kind = error.kind,
                details = error.details,
                primaryAction = NimazErrorDefaults.retry(
                    onRetry = { viewModel.onEvent(LicensesEvent.Retry) },
                    label = stringResource(R.string.try_again),
                ),
                modifier = Modifier.padding(paddingValues),
            )

            else -> LicensesContent(
                state = state,
                onQueryChange = { viewModel.onEvent(LicensesEvent.Search(it)) },
                onSelectFamily = { viewModel.onEvent(LicensesEvent.SelectFamily(it)) },
                onToggleGrouping = { viewModel.onEvent(LicensesEvent.ToggleGrouping) },
                onLibraryClick = onNavigateToDetail,
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
}

@Composable
private fun LicensesContent(
    state: LicensesListUiState,
    onQueryChange: (String) -> Unit,
    onSelectFamily: (LicenseFamily?) -> Unit,
    onToggleGrouping: () -> Unit,
    onLibraryClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        NimazSearchBar(
            query = state.query,
            onQueryChange = onQueryChange,
            placeholder = stringResource(R.string.licenses_search_placeholder),
            onClear = { onQueryChange("") },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "credit") { CreditCard(state) }

            if (state.familyCounts.size > 1) {
                item(key = "filters") {
                    FamilyFilters(state = state, onSelectFamily = onSelectFamily)
                }
            }

            item(key = "controls") {
                NimazSectionHeader(
                    title = stringResource(
                        when (state.grouping) {
                            LicenseGrouping.BY_LICENCE -> R.string.licenses_grouped_by_licence
                            LicenseGrouping.ALPHABETICAL -> R.string.licenses_grouped_alphabetically
                        }
                    ),
                    // The count and the toggle both have to appear, and the atom's own
                    // `trailingText` / `showSeeAll` are mutually exclusive — its `when` picks
                    // one. The slot takes both, and makes the toggle a real button rather
                    // than the atom's clickable label.
                    trailingContent = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.licenses_visible_count_format,
                                    state.visibleCount,
                                    state.totalCount,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            NimazButton(
                                text = stringResource(
                                    when (state.grouping) {
                                        LicenseGrouping.BY_LICENCE ->
                                            R.string.licenses_action_sort_alphabetically

                                        LicenseGrouping.ALPHABETICAL ->
                                            R.string.licenses_action_group_by_licence
                                    }
                                ),
                                onClick = onToggleGrouping,
                                variant = NimazButtonVariant.TEXT,
                                size = NimazButtonSize.SMALL,
                            )
                        }
                    },
                )
            }

            if (state.isEmptyResult) {
                item(key = "empty") {
                    NimazEmptyState(
                        title = stringResource(R.string.licenses_no_matches_title),
                        message = stringResource(R.string.licenses_no_matches_body),
                        icon = Icons.Default.SearchOff,
                    )
                }
            }

            state.sections.forEach { section ->
                item(key = "section-${section.key}") { SectionHeading(section) }
                items(section.libraries, key = { it.id }) { library ->
                    LibraryRow(
                        library = library,
                        query = state.query,
                        onClick = { onLibraryClick(library.id) },
                    )
                }
            }

            item(key = "note") {
                NimazBanner(
                    message = stringResource(R.string.licenses_generated_note),
                    variant = NimazBannerVariant.INFO,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            item(key = "tail") { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

/**
 * The one piece of colour on the list, and the reason the screen exists: the app is built on
 * work other people gave away.
 *
 * Both figures are counted from the parsed list — there is no third "typefaces" stat, because
 * AboutLibraries only knows Maven dependencies and a hand-kept font count would drift.
 */
@Composable
private fun CreditCard(state: LicensesListUiState) {
    NimazCard(
        modifier = Modifier.fillMaxWidth(),
        style = NimazCardStyle.FILLED,
        tone = NimazTone.ACCENT,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.licenses_credit_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.licenses_credit_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NimazStatCard(
                    value = state.totalCount.toString(),
                    label = stringResource(R.string.licenses_stat_libraries),
                    modifier = Modifier.weight(1f),
                )
                NimazStatCard(
                    value = state.licenceCount.toString(),
                    label = stringResource(R.string.licenses_stat_licences),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Licence-family filter chips.
 *
 * Counts come from the whole list rather than the filtered one, so a chip's number does not
 * change as you narrow — a filter whose own label moves when you use it is unreadable. Hidden
 * entirely when there is only one family, which is the common case for this app.
 */
@Composable
private fun FamilyFilters(
    state: LicensesListUiState,
    onSelectFamily: (LicenseFamily?) -> Unit,
) {
    Column {
        NimazSectionTitle(text = stringResource(R.string.licenses_filter_section))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item(key = "all") {
                NimazFilterChip(
                    label = stringResource(
                        R.string.licenses_chip_format,
                        stringResource(R.string.licenses_filter_all),
                        state.totalCount,
                    ),
                    selected = state.selectedFamily == null,
                    onClick = { onSelectFamily(null) },
                    showSelectedIcon = false,
                    shape = RoundedCornerShape(16.dp),
                )
            }
            items(state.familyCounts, key = { it.family.name }) { entry ->
                NimazFilterChip(
                    selected = state.selectedFamily == entry.family,
                    onClick = {
                        onSelectFamily(entry.family.takeIf { it != state.selectedFamily })
                    },
                    label = stringResource(
                        R.string.licenses_chip_format,
                        entry.family.label(),
                        entry.count,
                    ),
                    showSelectedIcon = false,
                    shape = RoundedCornerShape(16.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionHeading(section: LicenseSection) {
    val title = section.family?.label() ?: section.letter.orEmpty()
    NimazSectionTitle(text = title, trailingContent = {
        Text(
            text = section.libraries.size.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    })
}

@Composable
private fun LibraryRow(
    library: OpenSourceLibrary,
    query: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NimazCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        style = NimazCardStyle.OUTLINED,
        onClick = onClick,
        tone = NimazTone.NEUTRAL,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // AboutLibraries' display name is the human one — "Compose UI", "Core Ktx" —
                // and a dozen of them collide. The Maven group is what tells them apart.
                library.group?.let { group ->
                    Text(
                        text = highlighted(group, query),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = highlighted(library.name, query),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                library.author?.let { author ->
                    Text(
                        text = highlighted(author, query),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                NimazBadge(
                    text = library.family.label(),
                    tone = library.family.tone,
                    size = NimazBadgeSize.SMALL,
                )
            }
            // Right-aligned, as in the prototype: versions are the one field a reader scans
            // down a column, and they only line up if they share an edge.
            library.version?.let { version ->
                NimazBadge(
                    text = version,
                    tone = NimazTone.MUTED,
                    size = NimazBadgeSize.SMALL,
                )
            }
            NimazIcon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                variant = NimazIconVariant.MUTED,
                size = NimazIconSize.SMALL,
            )
        }
    }
}
