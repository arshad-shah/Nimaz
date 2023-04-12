package com.arshadshah.nimaz.ui.components.icons

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialPath
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

val Icons.Dot : ImageVector
	get()
	{
		if (_dot != null)
		{
			return _dot !!
		}
		_dot = ImageVector.Builder(
				name = "Dot" ,
				defaultWidth = 48.0.dp ,
				defaultHeight = 48.0.dp ,
				viewportWidth = 48.0F ,
				viewportHeight = 48.0F ,
								  ).materialPath {
			moveTo(24.0F , 48.0F)
			curveTo(10.745F , 48.0F , 0.0F , 37.255F , 0.0F , 24.0F)
			curveTo(0.0F , 10.745F , 10.745F , 0.0F , 24.0F , 0.0F)
			curveTo(37.255F , 0.0F , 48.0F , 10.745F , 48.0F , 24.0F)
			curveTo(48.0F , 37.255F , 37.255F , 48.0F , 24.0F , 48.0F)
			close()
		}.build()
		return _dot !!
	}
private var _dot : ImageVector? = null

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun IconDotPreview()
{
	Image(imageVector = Icons.Dot , contentDescription = null)
}