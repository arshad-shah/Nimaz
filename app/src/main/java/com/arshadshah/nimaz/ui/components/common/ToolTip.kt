package com.arshadshah.nimaz.ui.components.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.compose.Balloon
import com.skydoves.balloon.compose.rememberBalloonBuilder

/**
 * A tooltip that shows a popup with the given text when the user clicks on the icon
 * @param icon the icon to show for the tooltip
 * @param tipText the text to show in the popup
 */
@Composable
fun ToolTip(icon : Painter , tipText : String)
{

	val colorOfToolTip = MaterialTheme.colorScheme.surface.hashCode()


	// create and remember a builder of Balloon.
	val builder = rememberBalloonBuilder {
		setArrowSize(10)
		setArrowPosition(0.5f)
		setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
		setWidthRatio(0.7f)
		setHeight(BalloonSizeSpec.WRAP)
		setPadding(8)
		setMarginHorizontal(12)
		setCornerRadius(16F)
		setBackgroundColor(colorOfToolTip)
		setBalloonAnimation(BalloonAnimation.ELASTIC)
	}

	Balloon(
			 builder = builder ,
			 balloonContent = {
				 Text(
						  text = tipText ,
						  style = MaterialTheme.typography.bodySmall
					 )
			 }
		   ) { balloonWindow ->
		Icon(
				 modifier = Modifier
					 .size(24.dp)
					 .clip(CircleShape)
					 .clickable {
						 balloonWindow.showAlignBottom()
					 } ,
				 painter = icon ,
				 contentDescription = "Info" ,
				 tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
			)
	}
}