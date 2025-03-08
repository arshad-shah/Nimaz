package com.arshadshah.nimaz.ui.components.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.min

/**
 * Optimized data structure for cloud properties
 * Uses primitives instead of a class to reduce memory overhead
 */
private class CloudParams(val params: FloatArray) {
    // Index constants for accessing the float array
    companion object {
        const val X_IDX = 0
        const val Y_IDX = 1
        const val SCALE_IDX = 2
        const val OPACITY_IDX = 3
        const val SIZE = 4 // Number of elements per cloud
    }
}

/**
 * Memory-optimized static clouds that distributes clouds across container
 */
@Composable
fun StaticClouds(numClouds: Int, modifier: Modifier = Modifier) {
    // Use a single Float array for all cloud data instead of multiple objects
    // Structure: [x1, y1, scale1, opacity1, x2, y2, scale2, opacity2, ...]
    val cloudParams = remember {
        val params = FloatArray(numClouds * CloudParams.SIZE)
        val random = kotlin.random.Random(0) // Fixed seed for deterministic output

        for (i in 0 until numClouds) {
            val baseIdx = i * CloudParams.SIZE
            // X position (0.0-1.0)
            params[baseIdx + CloudParams.X_IDX] = (i.toFloat() / numClouds) +
                    (random.nextFloat() * 0.1f - 0.05f)
            // Y position (0.0-1.0)
            params[baseIdx + CloudParams.Y_IDX] = 0.1f + (random.nextFloat() * 0.5f)
            // Scale (0.5-1.3)
            params[baseIdx + CloudParams.SCALE_IDX] = 0.5f + random.nextFloat() * 0.8f
            // Opacity (0.6-1.0)
            params[baseIdx + CloudParams.OPACITY_IDX] = 0.6f + random.nextFloat() * 0.4f
        }

        CloudParams(params)
    }

    // White color cached once
    val white = remember { Color.White }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Use a single, reusable Path object for all clouds
        val cloudPath = Path()
        val highlightPath = Path()

        // Draw all clouds using the primitive array
        val params = cloudParams.params
        for (i in 0 until numClouds) {
            val baseIdx = i * CloudParams.SIZE
            val x = params[baseIdx + CloudParams.X_IDX] * width
            val y = params[baseIdx + CloudParams.Y_IDX] * height
            val scale = params[baseIdx + CloudParams.SCALE_IDX]
            val opacity = params[baseIdx + CloudParams.OPACITY_IDX]

            // Reuse the same path object for all clouds
            drawOptimizedCloud(
                center = Offset(x, y),
                scale = scale,
                color = white.copy(alpha = opacity),
                cloudPath = cloudPath,
                highlightPath = highlightPath
            )
        }
    }
}

/**
 * Memory-optimized implementation with multiple layers but less object creation
 */
@Composable
fun StaticCloudsWithDepth(modifier: Modifier = Modifier) {
    // Using a single array for all cloud parameters to minimize allocations
    val allCloudParams = remember {
        // Fixed number of clouds for each layer to further reduce memory usage
        val totalClouds = 15 // 5 background + 7 midground + 3 foreground
        val params = FloatArray(totalClouds * CloudParams.SIZE)
        val random = kotlin.random.Random(0) // Fixed seed for deterministic output

        // Background clouds (first 5)
        for (i in 0 until 5) {
            val baseIdx = i * CloudParams.SIZE
            params[baseIdx + CloudParams.X_IDX] = (i.toFloat() / 5) +
                    (random.nextFloat() * 0.1f - 0.05f)
            params[baseIdx + CloudParams.Y_IDX] = 0.05f + (random.nextFloat() * 0.3f)
            params[baseIdx + CloudParams.SCALE_IDX] = 0.4f + random.nextFloat() * 0.4f
            params[baseIdx + CloudParams.OPACITY_IDX] = 0.3f + random.nextFloat() * 0.3f
        }

        // Midground clouds (next 7)
        for (i in 0 until 7) {
            val baseIdx = (i + 5) * CloudParams.SIZE
            params[baseIdx + CloudParams.X_IDX] = (i.toFloat() / 7) +
                    (random.nextFloat() * 0.15f - 0.075f)
            params[baseIdx + CloudParams.Y_IDX] = 0.2f + (random.nextFloat() * 0.4f)
            params[baseIdx + CloudParams.SCALE_IDX] = 0.6f + random.nextFloat() * 0.5f
            params[baseIdx + CloudParams.OPACITY_IDX] = 0.5f + random.nextFloat() * 0.3f
        }

        // Foreground clouds (last 3)
        for (i in 0 until 3) {
            val baseIdx = (i + 12) * CloudParams.SIZE
            params[baseIdx + CloudParams.X_IDX] = (i.toFloat() / 3) +
                    (random.nextFloat() * 0.2f - 0.1f)
            params[baseIdx + CloudParams.Y_IDX] = 0.4f + (random.nextFloat() * 0.3f)
            params[baseIdx + CloudParams.SCALE_IDX] = 0.9f + random.nextFloat() * 0.6f
            params[baseIdx + CloudParams.OPACITY_IDX] = 0.7f + random.nextFloat() * 0.3f
        }

        CloudParams(params)
    }

    // White color cached once
    val white = remember { Color.White }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Use a single, reusable Path object for all clouds
        val cloudPath = Path()
        val highlightPath = Path()

        // Draw all clouds using the shared primitive array
        val params = allCloudParams.params
        val totalClouds = 15

        for (i in 0 until totalClouds) {
            val baseIdx = i * CloudParams.SIZE
            val x = params[baseIdx + CloudParams.X_IDX] * width
            val y = params[baseIdx + CloudParams.Y_IDX] * height
            val scale = params[baseIdx + CloudParams.SCALE_IDX]
            val opacity = params[baseIdx + CloudParams.OPACITY_IDX]

            drawOptimizedCloud(
                center = Offset(x, y),
                scale = scale,
                color = white.copy(alpha = opacity),
                cloudPath = cloudPath,
                highlightPath = highlightPath
            )
        }
    }
}

/**
 * Ultra-optimized cloud drawing function that reuses path objects
 * and minimizes calculations
 */
fun DrawScope.drawOptimizedCloud(
    center: Offset,
    scale: Float,
    color: Color,
    cloudPath: Path,
    highlightPath: Path
) {
    // Clear paths for reuse
    cloudPath.reset()
    highlightPath.reset()

    // Limit scale to avoid excessive drawing work for very small scales
    val effectiveScale = min(scale, 2.0f)

    // Cloud dimensions
    val width = 110f * effectiveScale
    val height = 50f * effectiveScale

    // Pre-compute offsets to minimize calculations during path construction
    val leftEdge = center.x - width * 0.45f
    val rightEdge = center.x + width * 0.48f
    val bottomEdge = center.y + height * 0.1f

    // Create cloud path
    cloudPath.apply {
        // Start at bottom left of the cloud
        moveTo(leftEdge, bottomEdge)

        // Bottom curve (simplified number of points)
        quadraticTo(
            center.x - width * 0.2f, center.y + height * 0.35f,
            center.x, center.y + height * 0.2f
        )

        // Bottom right curve
        quadraticTo(
            center.x + width * 0.25f, center.y + height * 0.35f,
            rightEdge, center.y + height * 0.05f
        )

        // Right side curve up
        quadraticTo(
            center.x + width * 0.5f, center.y - height * 0.15f,
            center.x + width * 0.35f, center.y - height * 0.3f
        )

        // Top right bump
        quadraticTo(
            center.x + width * 0.3f, center.y - height * 0.5f,
            center.x + width * 0.15f, center.y - height * 0.45f
        )

        // Middle top bump
        quadraticTo(
            center.x, center.y - height * 0.55f,
            center.x - width * 0.15f, center.y - height * 0.45f
        )

        // Left top bump
        quadraticTo(
            center.x - width * 0.3f, center.y - height * 0.5f,
            center.x - width * 0.4f, center.y - height * 0.3f
        )

        // Left side curve down to starting point
        quadraticTo(
            center.x - width * 0.5f, center.y - height * 0.1f,
            leftEdge, bottomEdge
        )

        close()
    }

    // Pre-compute alpha values to reduce allocations
    val shadowAlpha = color.alpha * 0.3f
    val shadowColor = color.copy(alpha = shadowAlpha)

    // Draw shadow with minimal transformations
    translate(left = -1f * effectiveScale, top = 1f * effectiveScale) {
        drawPath(
            path = cloudPath,
            color = shadowColor,
            style = Fill
        )
    }

    // Main cloud
    drawPath(
        path = cloudPath,
        color = color,
        style = Fill
    )

    // Only draw highlight for larger clouds to save performance
    if (effectiveScale > 0.7f) {
        // Create highlight path
        highlightPath.apply {
            moveTo(center.x - width * 0.2f, center.y - height * 0.3f)
            quadraticTo(
                center.x, center.y - height * 0.4f,
                center.x + width * 0.2f, center.y - height * 0.3f
            )
        }

        drawPath(
            path = highlightPath,
            color = color.copy(alpha = color.alpha * 0.5f),
            style = Stroke(width = 3f * effectiveScale)
        )
    }
}