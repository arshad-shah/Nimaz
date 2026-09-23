package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.theme.rememberNimazHaptics
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The A–Z strip down the edge of a long alphabetical list: where you are, and a way to get
 * anywhere else in one gesture.
 *
 * It **follows the list**: [current] — the section on screen — is filled, and its neighbours
 * swell a little, so the rail answers "where am I" without the reader looking for a heading. And
 * it **scrubs**: press and slide, and every letter passed is handed to [onSelect] (the list jumps)
 * with a light tick, while a bubble beside the finger shows the letter and [bubbleLabel]. The
 * bubble's tip sits on the vertical centre of the letter under the finger and it grows out of
 * that tip, so it reads as coming *from* the letter rather than floating near it.
 *
 * Letters not in [available] are drawn greyed and never selected — scrubbing over one lands on
 * the nearest letter that has entries, so a jump never opens onto nothing.
 *
 * Each letter is also its own accessibility node ("Jump to M"), because a drag is not something
 * TalkBack can perform.
 */
@Composable
fun NimazIndexRail(
    letters: List<Char>,
    available: Set<Char>,
    current: Char?,
    onSelect: (Char) -> Unit,
    modifier: Modifier = Modifier,
    bubbleLabel: @Composable (Char) -> String? = { null },
) {
    val haptics = rememberNimazHaptics()
    val density = LocalDensity.current
    val select by rememberUpdatedState(onSelect)
    var railHeight by remember { mutableIntStateOf(0) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubbed by remember { mutableStateOf<Char?>(null) }

    // Where the bubble is, kept after the finger lifts so it fades out in place rather than
    // jumping to the top of the rail on its way out.
    var bubbleLetter by remember { mutableStateOf<Char?>(null) }
    val highlighted = if (scrubbing) scrubbed ?: current else current
    val highlightIndex = letters.indexOf(highlighted)

    fun letterAt(y: Float): Char? {
        if (railHeight <= 0 || letters.isEmpty()) return null
        val i = (y / railHeight * letters.size).toInt().coerceIn(0, letters.lastIndex)
        // The nearest letter with entries, looking both ways, so a greyed letter is passed over.
        return (0..letters.size).asSequence()
            .flatMap { d -> sequenceOf(i + d, i - d) }
            .firstOrNull { it in letters.indices && letters[it] in available }
            ?.let { letters[it] }
    }

    Box(modifier = modifier.width(RailWidth)) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                // A shaped background, not a clip: the highlighted letter swells past the rail's
                // left edge while scrubbing, and a clip sliced the side of its circle off.
                .background(
                    color = if (scrubbing) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    else Color.Transparent,
                    shape = RoundedCornerShape(RailWidth / 2),
                )
                .onSizeChanged { railHeight = it.height }
                .pointerInput(letters, available) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        scrubbing = true
                        fun scrubTo(y: Float) {
                            val letter = letterAt(y) ?: return
                            if (letter != scrubbed) {
                                scrubbed = letter
                                bubbleLetter = letter
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                                select(letter)
                            }
                        }
                        scrubTo(down.position.y)
                        down.consume()
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            scrubTo(change.position.y)
                            change.consume()
                        }
                        scrubbing = false
                        scrubbed = null
                    }
                },
        ) {
            letters.forEachIndexed { i, letter ->
                val distance = if (highlightIndex < 0) Int.MAX_VALUE else abs(i - highlightIndex)
                RailLetter(
                    letter = letter,
                    enabled = letter in available,
                    distance = distance,
                    scrubbing = scrubbing,
                    onSelect = { select(letter) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            }
        }

        // The bubble. Its tip is the right-hand corner of a square turned 45°, placed so that
        // corner sits on the centre line of the highlighted letter's slot, just left of the rail.
        val slotCentre = if (letters.isEmpty()) 0f
        else (letters.indexOf(bubbleLetter).coerceAtLeast(0) + 0.5f) * railHeight / letters.size
        val diagonalPx = with(density) { BubbleDiagonal.toPx() }
        val gapPx = with(density) { BubbleGap.toPx() }
        AnimatedVisibility(
            visible = scrubbing && scrubbed != null,
            enter = fadeIn() + scaleIn(initialScale = 0.3f, transformOrigin = TipOrigin),
            exit = fadeOut() + scaleOut(targetScale = 0.3f, transformOrigin = TipOrigin),
            // Measured unbounded and placed outside the rail: the rail is 28dp wide, and inheriting
            // its constraints crushed the bubble into a sliver. Zero-sized in layout, so the rail
            // keeps its own width and the bubble takes no touches from the list beneath it.
            modifier = Modifier.layout { measurable, _ ->
                val bubble = measurable.measure(Constraints())
                layout(0, 0) {
                    bubble.place(
                        x = -(diagonalPx + gapPx).roundToInt(),
                        y = (slotCentre - diagonalPx / 2).roundToInt(),
                    )
                }
            },
        ) {
            bubbleLetter?.let { ScrubBubble(it, bubbleLabel(it)) }
        }
    }
}

@Composable
private fun RailLetter(
    letter: Char,
    enabled: Boolean,
    distance: Int,
    scrubbing: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier,
) {
    // A small fisheye around the highlighted letter, stronger while the finger is on the rail.
    val scale by animateFloatAsState(
        targetValue = when (distance) {
            0 -> if (scrubbing) 1.4f else 1.2f
            1 -> if (scrubbing) 1.25f else 1.12f
            2 -> if (scrubbing) 1.12f else 1.05f
            else -> 1f
        },
        label = "rail_letter_scale",
    )
    val shift by animateFloatAsState(
        targetValue = when (distance) {
            0 -> if (scrubbing) -6f else 0f
            1 -> if (scrubbing) -5f else -2f
            2 -> if (scrubbing) -2f else -1f
            else -> 0f
        },
        label = "rail_letter_shift",
    )
    val description = stringResource(R.string.cd_index_rail_jump, letter.toString())
    Box(
        modifier = modifier.semantics {
            contentDescription = description
            role = Role.Button
            selected = distance == 0
            if (enabled) onClick { onSelect(); true }
        },
        contentAlignment = Alignment.Center,
    ) {
        val current = distance == 0
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = shift.dp.toPx()
                    transformOrigin = TransformOrigin(1f, 0.5f)
                }
                .size(18.dp)
                .clip(CircleShape)
                .background(
                    if (current) MaterialTheme.colorScheme.primary
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = letter.toString(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                // Centred on the glyph, not the font's line box, or the letter sits low in its
                // circle once the circle is filled.
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeight = 10.sp,
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both,
                    ),
                ),
                color = when {
                    current -> MaterialTheme.colorScheme.onPrimary
                    enabled -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outlineVariant
                },
            )
        }
    }
}

/** A teardrop with its tip at the right-middle — a square turned 45°, one corner left sharp. */
@Composable
private fun ScrubBubble(letter: Char, label: String?) {
    Box(Modifier.size(BubbleDiagonal), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(BubbleSide)
                .rotate(45f)
                .clip(
                    RoundedCornerShape(
                        topStartPercent = 50,
                        topEndPercent = 8,
                        bottomEndPercent = 50,
                        bottomStartPercent = 50,
                    )
                )
                .background(MaterialTheme.colorScheme.primary),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = letter.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                )
            }
        }
    }
}

private val RailWidth = 28.dp
/**
 * Big enough that "106 entries" fits across the round body at the label's height — at 60dp the
 * three-digit counts ran off both edges.
 */
private val BubbleSide = 72.dp

/** The turned square's diagonal: the box the bubble occupies, tip at its right-middle. */
private val BubbleDiagonal = BubbleSide * sqrt(2f)
/** Clears the highlighted letter at full swell (1.4× and 6dp in) so the tip meets it, not covers it. */
private val BubbleGap = 10.dp
private val TipOrigin = TransformOrigin(1f, 0.5f)
