package com.arshadshah.nimaz.presentation.screens.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.OpenSourceLibrary
import com.arshadshah.nimaz.presentation.components.atoms.NimazErrorAction
import com.arshadshah.nimaz.presentation.components.atoms.NimazErrorState
import com.arshadshah.nimaz.presentation.components.atoms.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.about.LicensesEvent
import com.arshadshah.nimaz.presentation.viewmodel.about.LicensesViewModel

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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Library info card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = library.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                library.version?.let { version ->
                    Text(
                        text = stringResource(R.string.license_detail_version, version),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                library.author?.let { author ->
                    Text(
                        text = stringResource(R.string.license_detail_author, author),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                library.website?.let { website ->
                    Text(
                        text = website,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // License content
        library.licenses.forEach { license ->
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = license.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val licenseContent = license.content
                    if (!licenseContent.isNullOrBlank()) {
                        Text(
                            text = licenseContent,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
