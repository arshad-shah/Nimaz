package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer

@Composable
fun PlayerForAyat(
    isPlaying: MutableState<Boolean>,
    isPaused: MutableState<Boolean>,
    isStopped: MutableState<Boolean>,
    isDownloaded: MutableState<Boolean>,
    hasAudio: MutableState<Boolean>,
    onPlayClicked: () -> Unit,
    onPauseClicked: () -> Unit,
    onStopClicked: () -> Unit,
    isLoading: Boolean,
) {


    if (isDownloaded.value || hasAudio.value) {
        //a row to show th play button and the audio player
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 4.dp)
        ) {
            if (isPaused.value || isStopped.value || !isPlaying.value) {
                //play and pause button
                IconButton(
                    onClick = { onPlayClicked() },
                    enabled = !isLoading,
                    modifier = Modifier
                        .align(Alignment.CenterVertically).placeholder(isLoading, highlight = PlaceholderHighlight.shimmer())
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.play_icon),
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(horizontal = 4.dp)
                            .placeholder(
                                visible = isLoading,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(4.dp),
                                highlight = PlaceholderHighlight.shimmer(
                                    highlightColor = Color.White,
                                )
                            )
                    )
                }
            }

            if (isPlaying.value && !isStopped.value) {
                //play and puase button
                IconButton(
                    onClick = { onPauseClicked() },
                    enabled = true,
                    modifier = Modifier
                        .align(Alignment.CenterVertically).placeholder(isLoading, highlight = PlaceholderHighlight.shimmer())
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.pause_icon),
                        contentDescription = "Pause",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(horizontal = 4.dp)
                    )
                }
            }

            if (isPlaying.value || isPaused.value) {
                //stop button
                IconButton(
                    onClick = { onStopClicked() },
                    enabled = true,
                    modifier = Modifier
                        .align(Alignment.CenterVertically).placeholder(isLoading, highlight = PlaceholderHighlight.shimmer())
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.stop_icon),
                        contentDescription = "Stop",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }

}