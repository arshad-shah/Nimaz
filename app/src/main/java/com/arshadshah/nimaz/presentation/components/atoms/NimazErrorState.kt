package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * How a [NimazErrorState] occupies space — the mirror of [NimazLoadingVariant], so
 * a screen can swap loading → error without changing its layout.
 *
 * - [FULLSCREEN] — fills the available space and centres. For a screen body that
 *   has nothing to show because the load failed.
 * - [SECTION] — a bordered panel filling the width, for one section that failed
 *   while the rest of the screen is fine.
 * - [INLINE] — a single compact row. For a field, a list row, or a control whose
 *   last action failed; claims almost no vertical space.
 */
enum class NimazErrorVariant {
    FULLSCREEN,
    SECTION,
    INLINE
}

/**
 * What *kind* of failure this is. The kind picks the glyph and the [NimazTone], so
 * the same failure always looks the same wherever it surfaces — a dropped network
 * is never red-alarming in one screen and quietly grey in another.
 *
 * Copy stays with the caller: only the caller knows whether "offline" means
 * "prayer times can't refresh" or "this reciter can't be downloaded".
 */
enum class NimazErrorKind(
    internal val icon: ImageVector,
    internal val tone: NimazTone,
) {
    /** Anything without a better home — a thrown exception, an unknown failure. */
    GENERIC(Icons.Outlined.ErrorOutline, NimazTone.ERROR),

    /** No connection, or a request that never reached the network. */
    OFFLINE(Icons.Outlined.CloudOff, NimazTone.WARNING),

    /** The request reached a server and the server failed. */
    SERVER(Icons.Outlined.Storage, NimazTone.ERROR),

    /** The thing asked for isn't there — a bad id, a deleted bookmark. */
    NOT_FOUND(Icons.Outlined.SearchOff, NimazTone.MUTED),

    /** A permission is missing — notifications, storage, exact alarms. */
    PERMISSION(Icons.Outlined.Lock, NimazTone.WARNING),

    /** Location is unavailable or denied — prayer times and qibla depend on it. */
    LOCATION(Icons.Outlined.LocationOff, NimazTone.WARNING)
}

/**
 * A button offered by a [NimazErrorState]. The first one is rendered filled, the
 * second as a low-emphasis text button.
 *
 * @param loading shows a spinner in place of the label — for a retry that is
 *   already in flight, so a second tap can't queue another one.
 */
data class NimazErrorAction(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val loading: Boolean = false,
)

/** Presets and copy defaults for [NimazErrorState]. */
object NimazErrorDefaults {

    /** The near-universal primary action: try the failed thing again. */
    fun retry(
        onRetry: () -> Unit,
        label: String = "Try again",
        loading: Boolean = false,
    ): NimazErrorAction = NimazErrorAction(
        label = label,
        onClick = onRetry,
        icon = Icons.Outlined.Refresh,
        loading = loading,
    )
}

/**
 * The app's failure state — one component for "this didn't work", at three scales.
 *
 * It exists because a failed load was being hand-rolled per screen: a bare red
 * `Text`, a `Column` with an icon and a `TextButton`, sometimes nothing at all. A
 * shared component means every failure in Nimaz says three things in the same
 * order — **what happened**, **why it might have**, and **what to do next** — and
 * is announced to a screen reader as a polite live region rather than silently
 * replacing the content.
 *
 * The [FULLSCREEN][NimazErrorVariant.FULLSCREEN] and
 * [SECTION][NimazErrorVariant.SECTION] variants are anchored by a *fractured
 * shamsa* — the app's manuscript medallion (same [scallopPath] geometry as
 * [ShamsaMedallion]) drawn as a slowly turning, broken ring. It carries the
 * failure without a cartoon or a stock illustration, and it is drawn from the
 * [NimazTone], so it is correct in both themes for free.
 *
 * @param title the headline. One short sentence in plain language, describing what
 *   failed from the reader's side ("Prayer times couldn't refresh"), not the
 *   exception's side ("IOException").
 * @param message the supporting line — the likely cause or the consequence.
 * @param kind picks the glyph and tone; see [NimazErrorKind].
 * @param icon overrides the kind's glyph. Reach for a [NimazErrorKind] first.
 * @param tone overrides the kind's tone — for the rare case where the same kind
 *   should read louder or quieter in one place.
 * @param details technical detail (an exception message, an HTTP status). Hidden
 *   behind a "Show details" toggle so it never shouts at a reader who can't use
 *   it, but stays reachable for a bug report. Ignored by [NimazErrorVariant.INLINE].
 * @param primaryAction the recovery action, usually [NimazErrorDefaults.retry].
 * @param secondaryAction an escape hatch — "Go back", "Open settings".
 * @param animated set false to freeze the medallion — for tests, screenshots, or
 *   a caller honouring a reduce-motion preference.
 */
@Composable
fun NimazErrorState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    kind: NimazErrorKind = NimazErrorKind.GENERIC,
    variant: NimazErrorVariant = NimazErrorVariant.FULLSCREEN,
    icon: ImageVector? = null,
    tone: NimazTone? = null,
    details: String? = null,
    primaryAction: NimazErrorAction? = null,
    secondaryAction: NimazErrorAction? = null,
    animated: Boolean = true,
) {
    val resolvedTone = tone ?: kind.tone
    val glyph = icon ?: kind.icon
    val accent = NimazBadgeDefaults
        .colors(tone = resolvedTone, emphasis = NimazBadgeEmphasis.OUTLINED)
        .contentColor
    val wash = NimazBadgeDefaults
        .colors(tone = resolvedTone, emphasis = NimazBadgeEmphasis.SOFT)
        .containerColor

    // Announced politely: the screen reader finishes its current utterance, then
    // reads the failure. `assertive` would cut the user off mid-word.
    val announce = Modifier.semantics { liveRegion = LiveRegionMode.Polite }

    when (variant) {
        NimazErrorVariant.INLINE -> InlineError(
            title = title,
            message = message,
            glyph = glyph,
            tone = resolvedTone,
            primaryAction = primaryAction,
            modifier = modifier.then(announce),
        )

        NimazErrorVariant.SECTION -> NimazCard(
            modifier = modifier
                .fillMaxWidth()
                .then(announce),
            style = NimazCardStyle.OUTLINED,
            tone = resolvedTone,
        ) {
            ErrorBody(
                title = title,
                message = message,
                details = details,
                glyph = glyph,
                accent = accent,
                wash = wash,
                glyphSize = SECTION_GLYPH,
                titleStyle = MaterialTheme.typography.titleMedium,
                primaryAction = primaryAction,
                secondaryAction = secondaryAction,
                animated = animated,
                modifier = Modifier.padding(vertical = 24.dp, horizontal = 20.dp),
            )
        }

        NimazErrorVariant.FULLSCREEN -> Box(
            modifier = modifier
                .fillMaxSize()
                .then(announce),
            contentAlignment = Alignment.Center,
        ) {
            ErrorBody(
                title = title,
                message = message,
                details = details,
                glyph = glyph,
                accent = accent,
                wash = wash,
                glyphSize = FULLSCREEN_GLYPH,
                titleStyle = MaterialTheme.typography.headlineSmall,
                primaryAction = primaryAction,
                secondaryAction = secondaryAction,
                animated = animated,
                modifier = Modifier.padding(32.dp),
            )
        }
    }
}

// ==================== INTERNALS ====================

/**
 * The shared centred stack — medallion, title, message, details toggle, actions.
 * [FULLSCREEN][NimazErrorVariant.FULLSCREEN] and [SECTION][NimazErrorVariant.SECTION]
 * differ only in scale and framing, so they share one column rather than two that
 * can drift apart.
 */
@Composable
private fun ErrorBody(
    title: String,
    message: String?,
    details: String?,
    glyph: ImageVector,
    accent: Color,
    wash: Color,
    glyphSize: Dp,
    titleStyle: TextStyle,
    primaryAction: NimazErrorAction?,
    secondaryAction: NimazErrorAction?,
    animated: Boolean,
    modifier: Modifier = Modifier,
) {
    var detailsShown by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FracturedShamsa(
            icon = glyph,
            accent = accent,
            wash = wash,
            size = glyphSize,
            animated = animated,
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = title,
            style = titleStyle,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = TEXT_MAX_WIDTH),
        )

        if (message != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = TEXT_MAX_WIDTH),
            )
        }

        if (primaryAction != null || secondaryAction != null) {
            Spacer(Modifier.height(24.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                primaryAction?.let {
                    NimazButton(
                        text = it.label,
                        onClick = it.onClick,
                        variant = NimazButtonVariant.FILLED,
                        type = NimazButtonType.PILL,
                        leadingIcon = it.icon,
                        loading = it.loading,
                    )
                }
                secondaryAction?.let {
                    NimazButton(
                        text = it.label,
                        onClick = it.onClick,
                        variant = NimazButtonVariant.TEXT,
                        type = NimazButtonType.PILL,
                        leadingIcon = it.icon,
                        loading = it.loading,
                    )
                }
            }
        }

        if (details != null) {
            Spacer(Modifier.height(8.dp))
            NimazButton(
                text = if (detailsShown) "Hide details" else "Show details",
                onClick = { detailsShown = !detailsShown },
                variant = NimazButtonVariant.TEXT,
                size = NimazButtonSize.SMALL,
            )
            AnimatedVisibility(
                visible = detailsShown,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                NimazCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = TEXT_MAX_WIDTH),
                    tone = NimazTone.MUTED,
                ) {
                    Text(
                        text = details,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

/**
 * The one-row failure: a small tinted well, the message, and an optional text
 * action. Deliberately not centred — it lives in a form or a list, where centred
 * text would break the reading column.
 */
@Composable
private fun InlineError(
    title: String,
    message: String?,
    glyph: ImageVector,
    tone: NimazTone,
    primaryAction: NimazErrorAction?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NimazIconWell(
            icon = glyph,
            tone = tone,
            size = NimazIconWellSize.SMALL,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        primaryAction?.let {
            NimazButton(
                text = it.label,
                onClick = it.onClick,
                variant = NimazButtonVariant.TEXT,
                size = NimazButtonSize.SMALL,
                loading = it.loading,
            )
        }
    }
}

/**
 * The **fractured shamsa** — the medallion that anchors a full-screen or section
 * failure.
 *
 * Four concentric layers, all built from the shared ornament geometry so it is the
 * same mark the Quran surah header uses, not a second visual language:
 *
 * 1. a soft wash disc that breathes, giving the mark a centre of gravity;
 * 2. a **broken** outer ring — a dashed stroke, turning slowly clockwise. The gaps
 *    are the whole idea: this is the app's ornament, come apart;
 * 3. a 12-lobe scalloped rim turning the other way, with four diamond florets at
 *    the cardinal points, so the two rotations read as one mechanism out of step;
 * 4. a filled inner disc holding the kind's glyph.
 *
 * Rotations are deliberately slow (36s and 48s). A failure screen is read, not
 * watched — anything faster reads as a spinner and implies work is happening.
 */
@Composable
private fun FracturedShamsa(
    icon: ImageVector,
    accent: Color,
    wash: Color,
    size: Dp,
    animated: Boolean,
    modifier: Modifier = Modifier,
) {
    val spin = spinDegrees(animated, SPIN_DURATION_MS, "shamsa-spin")
    val counterSpin = spinDegrees(animated, COUNTER_SPIN_DURATION_MS, "shamsa-counter-spin")
    val breath = breathe(animated, from = 0.94f, to = 1.04f, durationMillis = BREATH_MS)

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val centre = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f

            // 1 — breathing wash.
            drawCircle(
                color = wash,
                radius = radius * 0.86f * breath,
                center = centre,
            )
            drawCircle(
                color = accent.copy(alpha = HALO_ALPHA),
                radius = radius * breath,
                center = centre,
            )

            // 2 — the broken ring.
            rotate(degrees = spin, pivot = centre) {
                drawCircle(
                    color = accent.copy(alpha = BROKEN_RING_ALPHA),
                    radius = radius * 0.93f,
                    center = centre,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(10.dp.toPx(), 7.dp.toPx()),
                        ),
                    ),
                )
            }

            // 3 — scalloped rim turning the other way, with cardinal florets.
            rotate(degrees = -counterSpin, pivot = centre) {
                val rim = radius * 0.72f
                drawPath(
                    scallopPath(centre, rim, lobes = 12, anchor = 0.86f, control = 1.14f),
                    color = accent.copy(alpha = SCALLOP_ALPHA),
                    style = Stroke(1.2.dp.toPx(), join = StrokeJoin.Round),
                )
                listOf(-90f, 0f, 90f, 180f).forEach { degrees ->
                    val radians = Math.toRadians(degrees.toDouble())
                    val at = Offset(
                        centre.x + radius * 0.90f * kotlin.math.cos(radians).toFloat(),
                        centre.y + radius * 0.90f * kotlin.math.sin(radians).toFloat(),
                    )
                    drawPath(
                        diamondPath(at, radius * FLORET_RADIUS_FRACTION),
                        color = accent.copy(alpha = FLORET_ALPHA),
                    )
                }
            }

            // 4 — the disc the glyph sits on.
            drawCircle(color = wash, radius = radius * 0.48f, center = centre)
            drawCircle(
                color = accent.copy(alpha = INNER_RING_ALPHA),
                radius = radius * 0.48f,
                center = centre,
                style = Stroke(1.dp.toPx()),
            )
        }

        NimazIcon(
            imageVector = icon,
            contentDescription = null,
            iconSize = size * GLYPH_FRACTION,
            tint = accent,
            modifier = Modifier.scale(breath),
        )
    }
}

/** A continuous 0→360 rotation, or a frozen 0 when [animated] is false. */
@Composable
private fun spinDegrees(animated: Boolean, durationMillis: Int, label: String): Float {
    if (!animated) return 0f
    val transition = rememberInfiniteTransition(label = label)
    val degrees by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = label,
    )
    return degrees
}

/** A slow ease in/out between [from] and [to], or a frozen [from]. */
@Composable
private fun breathe(animated: Boolean, from: Float, to: Float, durationMillis: Int): Float {
    if (!animated) return from
    val transition = rememberInfiniteTransition(label = "shamsa-breath")
    val value by transition.animateFloat(
        initialValue = from,
        targetValue = to,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shamsa-breath",
    )
    return value
}

private val FULLSCREEN_GLYPH = 132.dp
private val SECTION_GLYPH = 92.dp

/** Keeps the copy inside a comfortable reading measure instead of one long line. */
private val TEXT_MAX_WIDTH = 320.dp

private const val SPIN_DURATION_MS = 36_000
private const val COUNTER_SPIN_DURATION_MS = 48_000
private const val BREATH_MS = 3_200

private const val HALO_ALPHA = 0.05f
private const val BROKEN_RING_ALPHA = 0.34f
private const val SCALLOP_ALPHA = 0.26f
private const val FLORET_ALPHA = 0.40f
private const val INNER_RING_ALPHA = 0.35f
private const val FLORET_RADIUS_FRACTION = 0.045f
private const val GLYPH_FRACTION = 0.30f

// ==================== PREVIEWS ====================

@Composable
private fun NimazErrorStateFullscreenShowcase() {
    NimazErrorState(
        title = "Prayer times couldn't refresh",
        message = "Nimaz is showing yesterday's calculated times. Reconnect and try " +
                "again to sync with your current location.",
        kind = NimazErrorKind.OFFLINE,
        details = "java.net.UnknownHostException: Unable to resolve host \"api.aladhan.com\"",
        primaryAction = NimazErrorDefaults.retry(onRetry = {}),
        secondaryAction = NimazErrorAction(label = "Use offline times", onClick = {}),
    )
}

@Composable
private fun NimazErrorStateVariantShowcase() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        NimazErrorState(
            title = "Bookmarks didn't load",
            message = "Something went wrong reading your saved verses.",
            variant = NimazErrorVariant.SECTION,
            primaryAction = NimazErrorDefaults.retry(onRetry = {}),
        )
        NimazErrorState(
            title = "Reciter unavailable",
            message = "This recitation couldn't be downloaded.",
            kind = NimazErrorKind.SERVER,
            variant = NimazErrorVariant.SECTION,
            primaryAction = NimazErrorDefaults.retry(onRetry = {}, loading = true),
        )
        NimazErrorState(
            title = "Location is off",
            message = "Qibla needs your location to point correctly.",
            kind = NimazErrorKind.LOCATION,
            variant = NimazErrorVariant.INLINE,
            primaryAction = NimazErrorAction(label = "Enable", onClick = {}),
        )
        NimazErrorState(
            title = "No surah matches that search",
            kind = NimazErrorKind.NOT_FOUND,
            variant = NimazErrorVariant.INLINE,
        )
    }
}

@Composable
private fun NimazErrorStateKindShowcase() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NimazErrorKind.entries.forEach { kind ->
            NimazErrorState(
                title = kind.name,
                kind = kind,
                variant = NimazErrorVariant.INLINE,
                primaryAction = NimazErrorDefaults.retry(onRetry = {}),
            )
        }
    }
}

@Preview(showBackground = true, name = "Error — Fullscreen Light", heightDp = 700)
@Composable
private fun NimazErrorStateFullscreenLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        NimazErrorStateFullscreenShowcase()
    }
}

@Preview(
    showBackground = true, name = "Error — Fullscreen Dark", heightDp = 700,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazErrorStateFullscreenDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        NimazErrorStateFullscreenShowcase()
    }
}

@Preview(showBackground = true, name = "Error — Variants Light")
@Composable
private fun NimazErrorStateVariantsLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        NimazErrorStateVariantShowcase()
    }
}

@Preview(
    showBackground = true, name = "Error — Variants Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazErrorStateVariantsDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        NimazErrorStateVariantShowcase()
    }
}

@Preview(showBackground = true, name = "Error — Kinds Light")
@Composable
private fun NimazErrorStateKindsLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        NimazErrorStateKindShowcase()
    }
}

@Preview(
    showBackground = true, name = "Error — Kinds Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazErrorStateKindsDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        NimazErrorStateKindShowcase()
    }
}
