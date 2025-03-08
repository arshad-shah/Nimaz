package com.arshadshah.nimaz.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.theme.NimazTheme


@Composable
fun PageErrorState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .width(IntrinsicSize.Min),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(80.dp)
                        .padding(8.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxSize()
                    )
                }

                Text(
                    text = "Error",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ErrorStatePreview() {
    NimazTheme {
        PageErrorState(
            "An error occurred while fetching data from the server please try again"
        )
    }
}