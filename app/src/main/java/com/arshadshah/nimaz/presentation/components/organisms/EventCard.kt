package com.arshadshah.nimaz.presentation.components.organisms

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconContainerShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconType
import com.arshadshah.nimaz.presentation.components.atoms.NimazPatternBackground
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.atoms.QaidaCelebrationBurst
import com.arshadshah.nimaz.presentation.components.atoms.QuranOrnamentalDivider
import com.arshadshah.nimaz.presentation.theme.NimazPalette
import com.arshadshah.nimaz.presentation.theme.NimazPatternStyle
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/** Background/emphasis treatment for an [EventCard]. */
sealed interface EventOrnament {
    data object None : EventOrnament
    data class Pattern(val style: NimazPatternStyle) : EventOrnament
    data class Burst(val play: Boolean) : EventOrnament
    data object Divider : EventOrnament
}

/** A labelled call-to-action on an [EventCard]. */
data class EventAction(val label: String, val onClick: () -> Unit)

/**
 * White-surface occasion card (Jumu'ah, Eid, Ramadan, …) in the house style:
 * accented icon well, English + Arabic headline, an optional proof chip, and up
 * to two CTAs. Accent lives only in the well/chip/border/divider; body copy stays
 * neutral (contrast rule). Reuses existing atoms — no hand-rolled wells or dividers.
 */
@Composable
fun EventCard(
    accent: Color,
    icon: ImageVector,
    eyebrow: String,
    arabic: String?,
    headline: String? = null,
    body: String,
    modifier: Modifier = Modifier,
    containerAccent: Color = accent,
    transliteration: String? = null,
    proof: Pair<String, String>? = null,
    trailing: (@Composable () -> Unit)? = null,
    highlight: (@Composable () -> Unit)? = null,
    ornament: EventOrnament = EventOrnament.None,
    primaryAction: EventAction? = null,
    secondaryAction: EventAction? = null,
    onDismiss: (() -> Unit)? = null,
    fillHeight: Boolean = false,
) {
    NimazCard(
        tone = NimazTone.NEUTRAL,
        style = NimazCardStyle.ELEVATED,
        modifier = modifier.fillMaxWidth(),
    ) {
        EventCardOrnamentScope(ornament) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (fillHeight) Modifier.fillMaxSize() else Modifier)
                    .padding(15.dp)
            ) {
                // Header: well + eyebrow/arabic + trailing + dismiss
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        NimazIcon(
                            imageVector = icon,
                            contentDescription = null,
                            type = NimazIconType.CONTAINED,
                            containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
                            tint = accent,
                            containerColor = containerAccent.copy(alpha = 0.12f),
                            containerSize = 38.dp,
                            iconSize = 20.dp,
                        )
                        Column {
                            Text(
                                text = eyebrow,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (arabic != null) {
                                ArabicText(
                                    text = arabic,
                                    size = ArabicTextSize.SMALL,
                                    color = accent
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        trailing?.invoke()
                        if (onDismiss != null) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (arabic != null) {
                    QuranOrnamentalDivider(
                        color = accent.copy(alpha = 0.5f),
                        horizontalPadding = 8.dp,
                        verticalPadding = 10.dp,
                    )
                } else {
                    Spacer(Modifier.height(12.dp))
                }

                if (headline != null) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (transliteration != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = transliteration,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = accent
                    )
                }

                highlight?.let {
                    Spacer(Modifier.height(12.dp))
                    it()
                }

                if (proof != null) {
                    Spacer(Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(11.dp))
                            .background(accent.copy(alpha = 0.08f))
                            .padding(horizontal = 13.dp, vertical = 11.dp)
                    ) {
                        Text(
                            text = proof.first,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = proof.second,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (primaryAction != null || secondaryAction != null) {
                    Spacer(Modifier.height(11.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    )
                    Spacer(Modifier.height(11.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        primaryAction?.let {
                            NimazButton(
                                text = it.label,
                                onClick = it.onClick,
                                variant = NimazButtonVariant.TONAL,
                                size = NimazButtonSize.SMALL,
                                accent = accent,
                            )
                        }
                        secondaryAction?.let {
                            NimazButton(
                                text = it.label,
                                onClick = it.onClick,
                                variant = NimazButtonVariant.TEXT,
                                size = NimazButtonSize.SMALL,
                                accent = accent,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Wraps [content] in the chosen background ornament, respecting user pattern prefs. */
@Composable
private fun EventCardOrnamentScope(
    ornament: EventOrnament,
    content: @Composable () -> Unit,
) {
    when (ornament) {
        is EventOrnament.Pattern ->
            NimazPatternBackground(
                style = ornament.style,
                surface = MaterialTheme.colorScheme.surface,
                alphaScale = 0.6f,
            ) { content() }

        is EventOrnament.Burst ->
            Box {
                QaidaCelebrationBurst(play = ornament.play)
                content()
            }

        EventOrnament.None, EventOrnament.Divider -> content()
    }
}

@Preview(showBackground = true, widthDp = 400, name = "EventCard — full (light)")
@Composable
private fun EventCard_Full_Light_Preview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        EventCard(
            accent = NimazPalette.GoldDark,
            containerAccent = NimazPalette.Gold500,
            icon = Icons.Filled.Celebration,
            eyebrow = "Eid al-Fitr",
            arabic = "عيد مبارك",
            headline = "Eid Mubarak",
            body = "Thirty days behind you. May every one of them be accepted.",
            transliteration = "taqabbal Allāhu minnā wa minkum",
            proof = "Al-Baqarah 2:185" to "…that you may complete the count and glorify God.",
            ornament = EventOrnament.Burst(play = false),
            primaryAction = EventAction("Eid prayer time") {},
            secondaryAction = EventAction("Later") {},
            onDismiss = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(
    showBackground = true, widthDp = 400, name = "EventCard — full (dark)",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun EventCard_Full_Dark_Preview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        EventCard(
            accent = NimazPalette.GoldDark,
            containerAccent = NimazPalette.Gold500,
            icon = Icons.Filled.Celebration,
            eyebrow = "Eid al-Fitr",
            arabic = "عيد مبارك",
            headline = "Eid Mubarak",
            body = "Thirty days behind you. May every one of them be accepted.",
            transliteration = "taqabbal Allāhu minnā wa minkum",
            proof = "Al-Baqarah 2:185" to "…that you may complete the count and glorify God.",
            ornament = EventOrnament.Burst(play = false),
            primaryAction = EventAction("Eid prayer time") {},
            secondaryAction = EventAction("Later") {},
            onDismiss = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "EventCard — minimal (light)")
@Composable
private fun EventCard_Minimal_Light_Preview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        EventCard(
            accent = NimazPalette.Teal700,
            icon = Icons.Filled.Celebration,
            eyebrow = "Occasion",
            arabic = null,
            headline = "A blessed day",
            body = "A short, warm line with no Arabic, proof, or actions.",
            modifier = Modifier.padding(16.dp),
        )
    }
}
