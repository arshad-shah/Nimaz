package com.arshadshah.nimaz.presentation.components.molecules

import android.content.res.Configuration
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * A single "voice" option — a reciter (Quran audio) or a muezzin (adhan) — shown as
 * a podcast-style card: a monogram avatar (an equalizer while playing), the name,
 * a style + origin chip pair, and a round preview button that morphs across its
 * idle / playing / downloading / needs-download states.
 *
 * Shared by `SelectReciterScreen` and the muezzin section of `NotificationSettingsScreen`
 * so both pickers read identically. Reciters simply pass `isDownloaded = true`.
 *
 * @param name full voice name (also drives the monogram via [voiceInitials])
 * @param primaryTag the accent chip — recitation style ("Murattal") or origin
 * @param secondaryTag optional muted chip — origin / location (omit for a single tag)
 * @param onClick select this voice
 * @param onPreviewClick play / stop the preview (or trigger download-then-play)
 */
@Composable
fun VoiceOptionCard(
    name: String,
    primaryTag: String,
    isSelected: Boolean,
    secondaryTag: String? = null,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPreviewClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDownloading: Boolean = false,
    isDownloaded: Boolean = true,
    monogram: String = voiceInitials(name),
    previewContentDescription: String? = null,
) {
    NimazCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        style = NimazCardStyle.FILLED,
        shape = RoundedCornerShape(16.dp),
        selected = isSelected,
        colors = NimazCardDefaults.selectable(
            container = MaterialTheme.colorScheme.surface,
            border = MaterialTheme.colorScheme.outlineVariant,
            activeBorder = MaterialTheme.colorScheme.primary,
            activeBorderWidth = 1.5.dp,
        ),
        elevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VoiceAvatar(monogram = monogram, isSelected = isSelected, isPlaying = isPlaying)

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.padding(top = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    NimazBadge(
                        text = primaryTag,
                        size = NimazBadgeSize.SMALL,
                        shape = NimazBadgeShape.ROUNDED,
                        tone = NimazTone.ACCENT,
                        emphasis = NimazBadgeEmphasis.SOFT,
                    )
                    if (secondaryTag != null) {
                        NimazBadge(
                            text = secondaryTag,
                            size = NimazBadgeSize.SMALL,
                            shape = NimazBadgeShape.ROUNDED,
                            tone = NimazTone.NEUTRAL,
                            emphasis = NimazBadgeEmphasis.SOFT,
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            PreviewButton(
                isPlaying = isPlaying,
                isDownloading = isDownloading,
                isDownloaded = isDownloaded,
                onClick = onPreviewClick,
                contentDescription = previewContentDescription,
            )
        }
    }
}

/** Circular monogram avatar; shows an animated equalizer while [isPlaying]. */
@Composable
private fun VoiceAvatar(monogram: String, isSelected: Boolean, isPlaying: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                )
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            if (isPlaying) {
                EqualizerBars(color = MaterialTheme.colorScheme.onPrimaryContainer)
            } else {
                Text(
                    text = monogram,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    NimazIcon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        variant = NimazIconVariant.ON_ACCENT,
                        iconSize = 9.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun EqualizerBars(color: Color) {
    val transition = rememberInfiniteTransition(label = "equalizer")
    val delays = listOf(0, 180, 360, 120)
    Row(
        modifier = Modifier.height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        delays.forEach { delay ->
            val fraction by transition.animateFloat(
                initialValue = 0.30f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 520, delayMillis = delay),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "bar",
            )
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(18.dp * fraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
        }
    }
}

/** Round preview control: play / pause / spinner / download. */
@Composable
private fun PreviewButton(
    isPlaying: Boolean,
    isDownloading: Boolean,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    contentDescription: String?,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isDownloading -> CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )

                isPlaying -> NimazIcon(
                    imageVector = Icons.Filled.Pause,
                    contentDescription = contentDescription,
                    variant = NimazIconVariant.ON_ACCENT,
                    iconSize = 18.dp,
                )

                isDownloaded -> NimazIcon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = contentDescription,
                    variant = NimazIconVariant.ON_ACCENT,
                    iconSize = 18.dp,
                )

                else -> NimazIcon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = contentDescription,
                    variant = NimazIconVariant.ON_ACCENT,
                    iconSize = 18.dp,
                )
            }
        }
    }
}

/** First letters of up to two leading words, e.g. "Mishary Rashid …" → "MR". */
fun voiceInitials(name: String): String {
    val words = name
        .split(' ', '-', '—', '(', ')', '–')
        .map { it.trim() }
        .filter { it.isNotEmpty() && it.first().isLetter() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(2).uppercase()
        else -> "${words[0].first()}${words[1].first()}".uppercase()
    }
}

// ==================== PREVIEWS ====================

@Composable
private fun VoiceOptionCardShowcase() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        VoiceOptionCard(
            name = "Mishary Rashid Alafasy",
            primaryTag = "Murattal",
            secondaryTag = "Kuwait",
            isSelected = true,
            isPlaying = false,
            onClick = {}, onPreviewClick = {},
        )
        VoiceOptionCard(
            name = "Abdul Basit Abdul Samad",
            primaryTag = "Mujawwad",
            secondaryTag = "Egypt",
            isSelected = false,
            isPlaying = true,
            onClick = {}, onPreviewClick = {},
        )
        VoiceOptionCard(
            name = "Makkah (Masjid al-Haram)",
            primaryTag = "Makkah",
            isSelected = false,
            isPlaying = false,
            isDownloaded = false,
            onClick = {}, onPreviewClick = {},
        )
        VoiceOptionCard(
            name = "Madinah (Al-Masjid an-Nabawi)",
            primaryTag = "Madinah",
            isSelected = false,
            isPlaying = false,
            isDownloading = true,
            onClick = {}, onPreviewClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Voice Option Card — Light")
@Composable
private fun VoiceOptionCardLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { VoiceOptionCardShowcase() }
}

@Preview(
    showBackground = true, name = "Voice Option Card — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Composable
private fun VoiceOptionCardDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { VoiceOptionCardShowcase() }
}
