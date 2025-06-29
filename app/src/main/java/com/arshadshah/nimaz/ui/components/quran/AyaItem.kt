package com.arshadshah.nimaz.ui.components.quran


import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.ui.components.quran.aya.components.AyatContent
import com.arshadshah.nimaz.ui.components.quran.aya.components.AyatFeatures
import com.arshadshah.nimaz.ui.components.quran.aya.components.SpecialAyat
import com.arshadshah.nimaz.ui.components.quran.aya.components.TafseerSection
import com.arshadshah.nimaz.utils.DisplaySettings
import com.arshadshah.nimaz.viewModel.AudioState
import com.arshadshah.nimaz.viewModel.AyatViewModel

@Composable
fun AyaItem(
    aya: LocalAya,
    displaySettings: DisplaySettings,
    audioState: AudioState,
    onTafseerClick: (Int, Int) -> Unit,
    onEvent: (AyatViewModel.AyatEvent) -> Unit,
    loading: Boolean = false
) {
    // Only create states for values that need to trigger UI updates
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    if (aya.ayaNumberInSurah != 0) {
        ElevatedCard(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .scale(scale),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Features Section
                AyatFeatures(
                    aya = aya,
                    audioState = audioState,
                    onEvent = onEvent,
                    loading = loading
                )

                // Content Section
                AyatContent(
                    aya = aya,
                    displaySettings = displaySettings,
                    loading = loading
                )

                // Tafseer Section
                if (aya.ayaNumberInSurah != 0) {
                    TafseerSection(
                        aya = aya,
                        onOpenTafsir = onTafseerClick,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    } else {
        // Special Ayat
        SpecialAyat(
            aya = aya,
            displaySettings = displaySettings,
            loading = loading
        )
    }
}