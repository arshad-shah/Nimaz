package com.arshadshah.nimaz.ui.components.common.placeholder.material


import androidx.annotation.FloatRange
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import com.google.android.material.math.MathUtils
import kotlin.math.max


/**
 * A class which provides a brush to paint placeholder based on progress.
 */
@Stable
interface PlaceholderHighlight {

    /**
     * The optional [AnimationSpec] to use when running the animation for this highlight.
     */
    val animationSpec: InfiniteRepeatableSpec<Float>?

    /**
     * Return a [Brush] to draw for the given [progress] and [size].
     *
     * @param progress the current animated progress in the range of 0f..1f.
     * @param size The size of the current layout to draw in.
     */
    fun brush(
        @FloatRange(from = 0.0, to = 1.0) progress: Float,
        size: Size,
    ): Brush

    /**
     * Return the desired alpha value used for drawing the [Brush] returned from [brush].
     *
     * @param progress the current animated progress in the range of 0f..1f.
     */
    @FloatRange(from = 0.0, to = 1.0)
    fun alpha(progress: Float): Float

    companion object
}

/**
 * Creates a [Fade] brush with the given initial and target colors.
 *
 * @sample com.google.accompanist.sample.placeholder.DocSample_Foundation_PlaceholderFade
 *
 * @param highlightColor the color of the highlight which is faded in/out.
 * @param animationSpec the [AnimationSpec] to configure the animation.
 */

fun PlaceholderHighlight.Companion.fade(
    highlightColor: Color,
    animationSpec: InfiniteRepeatableSpec<Float> = PlaceholderDefaults.fadeAnimationSpec,
): PlaceholderHighlight = Fade(
    highlightColor = highlightColor,
    animationSpec = animationSpec,
)

/**
 * Creates a [PlaceholderHighlight] which 'shimmers', using the given [highlightColor].
 *
 * The highlight starts at the top-start, and then grows to the bottom-end during the animation.
 * During that time it is also faded in, from 0f..progressForMaxAlpha, and then faded out from
 * progressForMaxAlpha..1f.
 *
 * @sample com.google.accompanist.sample.placeholder.DocSample_Foundation_PlaceholderShimmer
 *
 * @param highlightColor the color of the highlight 'shimmer'.
 * @param animationSpec the [AnimationSpec] to configure the animation.
 * @param progressForMaxAlpha The progress where the shimmer should be at it's peak opacity.
 * Defaults to 0.6f.
 */
fun PlaceholderHighlight.Companion.shimmer(
    highlightColor: Color,
    animationSpec: InfiniteRepeatableSpec<Float> = PlaceholderDefaults.shimmerAnimationSpec,
    @FloatRange(from = 0.0, to = 1.0) progressForMaxAlpha: Float = 0.6f,
): PlaceholderHighlight = Shimmer(
    highlightColor = highlightColor,
    animationSpec = animationSpec,
    progressForMaxAlpha = progressForMaxAlpha,
)

private data class Fade(
    private val highlightColor: Color,
    override val animationSpec: InfiniteRepeatableSpec<Float>,
) : PlaceholderHighlight {

    private val brush = SolidColor(highlightColor)

    override fun brush(progress: Float, size: Size): Brush = brush
    override fun alpha(progress: Float): Float = progress
}

private data class Shimmer(
    private val highlightColor: Color,
    override val animationSpec: InfiniteRepeatableSpec<Float>,
    private val progressForMaxAlpha: Float = 0.6f,
) : PlaceholderHighlight {

    override fun brush(
        progress: Float,
        size: Size,
    ): Brush = Brush.radialGradient(
        colors = listOf(
            highlightColor.copy(alpha = 0f),
            highlightColor,
            highlightColor.copy(alpha = 0f),
        ),
        center = Offset(x = 0f, y = 0f),
        radius = (max(size.width, size.height) * progress * 2).coerceAtLeast(0.01f),
    )

    override fun alpha(progress: Float): Float = when {
        // From 0f...ProgressForOpaqueAlpha we animate from 0..1
        progress <= progressForMaxAlpha -> {
            MathUtils.lerp(
                0f,
                1f,
                progress / progressForMaxAlpha
            )
        }
        // From ProgressForOpaqueAlpha..1f we animate from 1..0
        else -> {
            MathUtils.lerp(
                1f,
                0f,
                (progress - progressForMaxAlpha) / (1f - progressForMaxAlpha)
            )
        }
    }
}

/**
 * Creates a [PlaceholderHighlight] which fades in an appropriate color, using the
 * given [animationSpec].
 *
 * @sample com.google.accompanist.sample.placeholder.DocSample_Material_PlaceholderFade
 *
 * @param animationSpec the [AnimationSpec] to configure the animation.
 */
@Composable
fun PlaceholderHighlight.Companion.fade(
    animationSpec: InfiniteRepeatableSpec<Float> = PlaceholderDefaults.fadeAnimationSpec,
): PlaceholderHighlight = PlaceholderHighlight.fade(
    highlightColor = PlaceholderDefaults.fadeHighlightColor(),
    animationSpec = animationSpec,
)

/**
 * Creates a [PlaceholderHighlight] which 'shimmers', using a default color.
 *
 * The highlight starts at the top-start, and then grows to the bottom-end during the animation.
 * During that time it is also faded in, from 0f..progressForMaxAlpha, and then faded out from
 * progressForMaxAlpha..1f.
 *
 * @sample com.google.accompanist.sample.placeholder.DocSample_Material_PlaceholderShimmer
 *
 * @param animationSpec the [AnimationSpec] to configure the animation.
 * @param progressForMaxAlpha The progress where the shimmer should be at it's peak opacity.
 * Defaults to 0.6f.
 */
@Composable
fun PlaceholderHighlight.Companion.shimmer(
    animationSpec: InfiniteRepeatableSpec<Float> = PlaceholderDefaults.shimmerAnimationSpec,
    @FloatRange(from = 0.0, to = 1.0) progressForMaxAlpha: Float = 0.6f,
): PlaceholderHighlight = PlaceholderHighlight.shimmer(
    highlightColor = PlaceholderDefaults.shimmerHighlightColor(),
    animationSpec = animationSpec,
    progressForMaxAlpha = progressForMaxAlpha,
)