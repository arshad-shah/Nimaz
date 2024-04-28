package com.arshadshah.nimaz.ui.components.compass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R

@Composable
fun DialComponent(
    directionToTurn: String,
    pointingToQibla: Boolean,
    imageToDisplay: Painter,
    rotateAnim: Animatable<Float, AnimationVector1D>,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {

        Text(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            text = directionToTurn,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Icon(
            painter = painterResource(id = R.drawable.circle_close_icon),
            contentDescription = "dot",
            modifier = Modifier
                .fillMaxWidth()
                .size(28.dp),
            tint = if (pointingToQibla) MaterialTheme.colorScheme.inversePrimary else Color.Red
        )
        //the dial
        Image(
            painter = imageToDisplay,
            contentDescription = "Compass",
            modifier = Modifier
                .clip(CircleShape)
                .rotate(rotateAnim.value)
                .fillMaxWidth()
                .padding(12.dp),
            alignment = Alignment.Center
        )
    }
}

@Preview
@Composable
fun DialPreview() {
    DialComponent("Turn Left", false, painterResource(id = R.drawable.qibla2), remember {
        Animatable(0f)
    })
}