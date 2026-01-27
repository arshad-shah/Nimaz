package com.arshadshah.nimaz.presentation.screens.dua

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.Dua
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.DuaEvent
import com.arshadshah.nimaz.presentation.viewmodel.DuaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuaReaderScreen(
    duaId: String,
    onNavigateBack: () -> Unit,
    viewModel: DuaViewModel = hiltViewModel()
) {
    val state by viewModel.readerState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(duaId) {
        viewModel.onEvent(DuaEvent.LoadDua(duaId))
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = state.dua?.titleEnglish ?: "Loading...",
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        onClick = {
                            state.dua?.let {
                                viewModel.onEvent(DuaEvent.ToggleFavorite(it.id, it.categoryId))
                            }
                        }
                    ) {
                        val isFavorite = state.isFavorite
                        val tint by animateColorAsState(
                            targetValue = if (isFavorite) NimazColors.Secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "favorite_tint"
                        )
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = tint
                        )
                    }
                    IconButton(onClick = { /* Share */ }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            state.dua?.let { dua ->
                val repeatCount = dua.repeatCount ?: 0
                if (repeatCount > 0) {
                    FloatingActionButton(
                        onClick = {
                            viewModel.onEvent(DuaEvent.IncrementProgress(dua.id, repeatCount))
                        },
                        containerColor = NimazColors.Secondary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increment Count",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            state.dua?.let { dua ->
                val repeatCount = dua.repeatCount ?: 0
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Counter Card (if applicable)
                    if (repeatCount > 0) {
                        item {
                            CounterCard(
                                currentCount = state.progress?.completedCount ?: 0,
                                targetCount = repeatCount,
                                onIncrement = {
                                    viewModel.onEvent(DuaEvent.IncrementProgress(dua.id, repeatCount))
                                }
                            )
                        }
                    }

                    // Arabic Text
                    item {
                        DuaTextCard(
                            label = "Arabic",
                            text = dua.textArabic,
                            isArabic = true,
                            fontSize = state.arabicFontSize,
                            showContent = state.showArabic,
                            onToggle = { viewModel.onEvent(DuaEvent.ToggleArabic) }
                        )
                    }

                    // Transliteration
                    if (!dua.textTransliteration.isNullOrEmpty()) {
                        item {
                            DuaTextCard(
                                label = "Transliteration",
                                text = dua.textTransliteration,
                                isArabic = false,
                                fontSize = state.fontSize,
                                showContent = state.showTransliteration,
                                onToggle = { viewModel.onEvent(DuaEvent.ToggleTransliteration) }
                            )
                        }
                    }

                    // Translation
                    item {
                        DuaTextCard(
                            label = "Translation",
                            text = dua.textEnglish,
                            isArabic = false,
                            fontSize = state.fontSize,
                            showContent = state.showTranslation,
                            onToggle = { viewModel.onEvent(DuaEvent.ToggleTranslation) }
                        )
                    }

                    // Reference/Source
                    if (!dua.reference.isNullOrEmpty()) {
                        item {
                            ReferenceCard(source = dua.reference)
                        }
                    }

                    // Benefits
                    if (!dua.benefits.isNullOrEmpty()) {
                        item {
                            BenefitsCard(benefits = dua.benefits)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CounterCard(
    currentCount: Int,
    targetCount: Int,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = currentCount.toFloat() / targetCount.toFloat()
    val isComplete = currentCount >= targetCount

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isComplete) {
                NimazColors.StatusColors.Prayed.copy(alpha = 0.1f)
            } else {
                NimazColors.TasbihColors.Counter.copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Daily Target",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentCount.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isComplete) NimazColors.StatusColors.Prayed else NimazColors.TasbihColors.Counter
                )
                Text(
                    text = " / $targetCount",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isComplete) NimazColors.StatusColors.Prayed else NimazColors.TasbihColors.Counter
                        )
                )
            }

            if (isComplete) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Target Complete!",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = NimazColors.StatusColors.Prayed
                )
            }
        }
    }
}

@Composable
private fun DuaTextCard(
    label: String,
    text: String,
    isArabic: Boolean,
    fontSize: Float,
    showContent: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    onClick = onToggle,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = if (showContent) "Hide" else "Show",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (showContent) {
                Spacer(modifier = Modifier.height(12.dp))

                if (isArabic) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * 2f).sp
                        ),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * 1.6f).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ReferenceCard(
    source: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Source",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = source,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BenefitsCard(
    benefits: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = NimazColors.StatusColors.Prayed.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Benefits & Virtues",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = NimazColors.StatusColors.Prayed
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = benefits,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
