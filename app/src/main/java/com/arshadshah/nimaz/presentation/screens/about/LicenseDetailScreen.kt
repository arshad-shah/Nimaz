package com.arshadshah.nimaz.presentation.screens.about

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.LibraryLicense
import com.arshadshah.nimaz.domain.model.OpenSourceLibrary
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcons
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.molecules.NimazBanner
import com.arshadshah.nimaz.presentation.components.molecules.NimazBannerVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorAction
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorState
import com.arshadshah.nimaz.presentation.components.molecules.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.about.LicensesEvent
import com.arshadshah.nimaz.presentation.viewmodel.about.LicensesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseDetailScreen(
    libraryId: Int,
    onNavigateBack: () -> Unit,
    viewModel: LicensesViewModel = hiltViewModel(),
) {
    val state by viewModel.detailState.collectAsStateWithLifecycle()

    LaunchedEffect(libraryId) { viewModel.onEvent(LicensesEvent.LoadLibrary(libraryId)) }

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = state.library?.name ?: stringResource(R.string.license_detail_title),
                subtitle = state.library?.family?.label(),
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        // Bound to locals so the null checks smart-cast — `state` is a delegated
        // property, so its fields do not.
        val error = state.error
        val library = state.library
        when {
            state.isLoading -> NimazLoadingState(modifier = Modifier.padding(paddingValues))

            error != null -> NimazErrorState(
                title = stringResource(error.message),
                message = stringResource(R.string.license_detail_not_found_body),
                kind = error.kind,
                details = error.details,
                secondaryAction = NimazErrorAction(
                    label = stringResource(R.string.close),
                    onClick = onNavigateBack,
                ),
                modifier = Modifier.padding(paddingValues),
            )

            library != null -> LibraryDetailContent(
                library = library,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun LibraryDetailContent(
    library: OpenSourceLibrary,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") { LibraryHeaderCard(library) }

        // The summary is a string resource, so whether there is one can only be asked inside a
        // composable — the item is always emitted and renders nothing for an unknown family.
        item(key = "plain") {
            PlainTermsCard(library)
        }

        library.licenses.forEach { license ->
            item(key = "licence-${license.name}") { LicenseTextCard(license) }
        }

        item(key = "note") {
            NimazBanner(
                title = stringResource(R.string.license_detail_governs_note),
                variant = NimazBannerVariant.INFO,
            )
        }
        item(key = "tail") { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun LibraryHeaderCard(library: OpenSourceLibrary) {
    val context = LocalContext.current
    NimazCard(
        modifier = Modifier.fillMaxWidth(),
        style = NimazCardStyle.ELEVATED,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LibraryMonogram(library)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = library.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        NimazBadge(
                            text = library.family.label(),
                            tone = library.family.tone,
                            size = NimazBadgeSize.SMALL,
                        )
                        library.version?.let { version ->
                            NimazBadge(
                                text = version,
                                tone = NimazTone.MUTED,
                                size = NimazBadgeSize.SMALL,
                            )
                        }
                    }
                }
            }

            library.group?.let { group ->
                MetaRow(label = stringResource(R.string.license_detail_coordinate), value = group)
            }
            library.author?.let { author ->
                MetaRow(
                    label = stringResource(R.string.license_detail_published_by),
                    value = author,
                )
            }
            library.website?.let { website ->
                MetaRow(
                    label = stringResource(R.string.license_detail_home_page),
                    value = website.substringAfter("://"),
                    emphasised = true,
                )
                NimazButton(
                    text = stringResource(R.string.license_detail_open_home_page),
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, website.toUri()))
                    },
                    // FILLED, not TONAL: Material's tonal button takes `secondaryContainer`,
                    // which in this theme is the brand gold — it read as a warning next to a
                    // teal-toned screen rather than as the primary action.
                    variant = NimazButtonVariant.FILLED,
                    size = NimazButtonSize.MEDIUM,
                    leadingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                    fullWidth = true,
                )
            }
        }
    }
}

/**
 * The prototype's letter tile: the library's initial, tinted by its licence family.
 *
 * Built the same way as the developer avatar on `AboutScreen` — there is no text-monogram
 * atom, and a [NimazIconWell] only takes an [androidx.compose.ui.graphics.vector.ImageVector].
 * The colours still come from the design system: [NimazBadgeDefaults.colors] is the public
 * tone→colour resolver the icon well itself uses, so the tile cannot drift from a badge of
 * the same tone.
 */
@Composable
private fun LibraryMonogram(library: OpenSourceLibrary) {
    val colors = NimazBadgeDefaults.colors(
        tone = library.family.tone,
        emphasis = NimazBadgeEmphasis.SOFT,
    )
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.containerColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = library.name.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.contentColor,
        )
    }
}

@Composable
private fun MetaRow(label: String, value: String, emphasised: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        NimazDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.4f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = if (emphasised) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(0.6f),
            )
        }
    }
}

/**
 * The everyday-language gloss of the licence family, toned to match it.
 *
 * A paraphrase and nothing more — [R.string.license_detail_governs_note] at the foot of the
 * screen says so, and the full text sits directly below it.
 */
@Composable
private fun PlainTermsCard(library: OpenSourceLibrary) {
    val summary = library.family.plainSummary() ?: return
    NimazCard(
        modifier = Modifier.fillMaxWidth(),
        style = NimazCardStyle.FILLED,
        tone = library.family.tone,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.license_plain_title),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/**
 * One declared licence: its text, previewed and expandable, with a copy action in the header.
 *
 * Not a [NimazAccordion]. An accordion hides its body entirely, and the prototype deliberately
 * shows the opening of the licence under a fade — a reader can see what they are being offered
 * and decide, rather than tapping a chevron to find out. The title also carries the family
 * label rather than the declared name, which is what keeps it to one line.
 */
@Composable
private fun LicenseTextCard(license: LibraryLicense) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val clipboardScope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.license_text_copied)
    val content = license.content
    var expanded by rememberSaveable(license.name) { mutableStateOf(false) }

    NimazCard(
        modifier = Modifier.fillMaxWidth(),
        style = NimazCardStyle.ELEVATED,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NimazIcon(
                imageVector = Icons.Outlined.Description,
                contentDescription = null,
                variant = NimazIconVariant.MUTED,
                size = NimazIconSize.SMALL,
            )
            Text(
                text = stringResource(
                    R.string.license_detail_full_text_format,
                    license.family.label(),
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (!content.isNullOrBlank()) {
                NimazButton(
                    text = stringResource(R.string.action_copy),
                    onClick = {
                        clipboardScope.launch {
                            clipboard.setClipEntry(
                                ClipEntry(ClipData.newPlainText(license.name, content))
                            )
                            Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                        }
                    },
                    variant = NimazButtonVariant.OUTLINED,
                    size = NimazButtonSize.SMALL,
                    leadingIcon = Icons.Default.ContentCopy,
                )
            }
        }
        NimazDivider()

        if (content.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.license_detail_no_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(14.dp),
            )
            return@NimazCard
        }

        val bed = MaterialTheme.colorScheme.surfaceContainer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bed)
                .then(if (expanded) Modifier else Modifier.height(CollapsedTextHeight))
        ) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                // Licence text is dense, unbroken prose; the default leading makes a wall of it.
                lineHeight = MaterialTheme.typography.bodySmall.fontSize * 1.5,
                modifier = Modifier.padding(14.dp),
            )
            // The prototype's fade, so the clipped text reads as "continues" rather than
            // "ends mid-sentence". Painted from the bed colour, so it follows the theme.
            if (!expanded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, bed)))
                )
            }
        }

        NimazDivider()
        NimazButton(
            text = stringResource(
                if (expanded) R.string.license_detail_collapse
                else R.string.license_detail_show_full_text
            ),
            onClick = { expanded = !expanded },
            variant = NimazButtonVariant.TEXT,
            size = NimazButtonSize.MEDIUM,
            leadingIcon = if (expanded) NimazIcons.Collapse else NimazIcons.Expand,
            fullWidth = true,
        )
    }
}

/** How much of a licence stands above the fade before the reader asks for the rest. */
private val CollapsedTextHeight = 220.dp
