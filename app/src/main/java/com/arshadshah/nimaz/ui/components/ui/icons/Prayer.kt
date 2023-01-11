package com.arshadshah.nimaz.ui.components.ui.icons

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialPath
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

val Icons.Prayer: ImageVector
    get() {
        if (_prayer != null) {
            return _prayer!!
        }
        _prayer = ImageVector.Builder(
            name = "Prayer",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 448.0F,
            viewportHeight = 512.0F,
        ).materialPath {
              moveTo(352.0F, 64.0F)
              curveToRelative(0.0F, -35.3F, -28.7F, -64.0F, -64.0F, -64.0F)
              reflectiveCurveToRelative(-64.0F, 28.7F, -64.0F, 64.0F)
              reflectiveCurveToRelative(28.7F, 64.0F, 64.0F, 64.0F)
              reflectiveCurveToRelative(64.0F, -28.7F, 64.0F, -64.0F)

              moveTo(232.7F, 264.0F)
              lineToRelative(22.9F, 31.5F)
              curveToRelative(6.5F, 8.9F, 16.3F, 14.7F, 27.2F, 16.1F)
              reflectiveCurveToRelative(21.9F, -1.7F, 30.4F, -8.7F)
              lineToRelative(88.0F, -72.0F)
              curveToRelative(17.1F, -14.0F, 19.6F, -39.2F, 5.6F, -56.3F)
              reflectiveCurveToRelative(-39.2F, -19.6F, -56.3F, -5.6F)
              lineToRelative(-55.2F, 45.2F)
              lineToRelative(-26.2F, -36.0F)
              curveTo(253.6F, 156.7F, 228.6F, 144.0F, 202.0F, 144.0F)
              curveToRelative(-30.9F, 0.0F, -59.2F, 17.1F, -73.6F, 44.4F)
              lineTo(79.8F, 280.9F)
              curveToRelative(-20.2F, 38.5F, -9.4F, 85.9F, 25.6F, 111.8F)
              lineTo(158.6F, 432.0F)
              horizontalLineTo(72.0F)
              curveToRelative(-22.1F, 0.0F, -40.0F, 17.9F, -40.0F, 40.0F)
              reflectiveCurveToRelative(17.9F, 40.0F, 40.0F, 40.0F)
              horizontalLineTo(280.0F)
              curveToRelative(17.3F, 0.0F, 32.6F, -11.1F, 38.0F, -27.5F)
              reflectiveCurveToRelative(-0.3F, -34.4F, -14.2F, -44.7F)
              lineTo(187.7F, 354.0F)
              lineToRelative(45.0F, -90.0F)
              close()
        }.build()
        return _prayer!!
    }
private var _prayer: ImageVector? = null

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun IconPrayerPreview() {
    Image(imageVector = Icons.Prayer, contentDescription = null)
}