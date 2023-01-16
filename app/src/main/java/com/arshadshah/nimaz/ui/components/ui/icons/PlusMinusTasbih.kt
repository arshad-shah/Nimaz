package com.arshadshah.nimaz.ui.components.ui.icons

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

val Icons.PlusMinusTasbih : ImageVector
	get()
	{
		if (_plusMinusTasbih != null)
		{
			return _plusMinusTasbih !!
		}
		_plusMinusTasbih = ImageVector.Builder(
				name = "PlusMinusTasbih" , defaultWidth = 48.0.dp , defaultHeight = 48.0.dp ,
				viewportWidth = 24.0f , viewportHeight = 42.0f
											  ).apply {
			path(
					fill = SolidColor(Color(0x00000000)) ,
					stroke = SolidColor(Color(0xFF000000)) ,
					strokeLineWidth = 2.0f ,
					strokeLineCap = StrokeCap.Round ,
					strokeLineJoin =
					StrokeJoin.Companion.Round ,
					strokeLineMiter = 4.0f ,
					pathFillType = PathFillType.NonZero
				) {
				moveTo(12.0f , 8.0f)
				lineTo(12.0f , 16.0f)
			}
			path(
					fill = SolidColor(Color(0x00000000)) ,
					stroke = SolidColor(Color(0xFF000000)) ,
					strokeLineWidth = 2.0f ,
					strokeLineCap = StrokeCap.Round ,
					strokeLineJoin =
					StrokeJoin.Companion.Round ,
					strokeLineMiter = 4.0f ,
					pathFillType = PathFillType.NonZero
				) {
				moveTo(8.0f , 12.0f)
				lineTo(16.0f , 12.0f)
			}


			path(
					fill = SolidColor(Color(0x00000000)) ,
					stroke = SolidColor(Color(0xFF000000)) ,
					strokeLineWidth = 2.0f ,
					strokeLineCap = StrokeCap.Round ,
					strokeLineJoin =
					StrokeJoin.Companion.Round ,
					strokeLineMiter = 4.0f ,
					pathFillType = PathFillType.NonZero
				) {
				moveTo(8.0f , 32.0f)
				lineTo(16.0f , 32.0f)
			}
		}
			.build()
		return _plusMinusTasbih !!
	}

private var _plusMinusTasbih : ImageVector? = null

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun IconQiblaCompassMainPreview()
{
	Image(imageVector = Icons.PlusMinusTasbih , contentDescription = null)
}
