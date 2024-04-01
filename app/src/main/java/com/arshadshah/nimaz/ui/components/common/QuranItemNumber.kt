package com.arshadshah.nimaz.ui.components.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R

@Composable
fun QuranItemNumber(number: Int) {
    Box(contentAlignment = Alignment.Center) {
        // Draw the star image
        Icon(
            painter = painterResource(id = R.drawable.number_back_icon),
            tint = MaterialTheme.colorScheme.secondary,
            contentDescription = null, // Provide a proper content description
            modifier = Modifier
                .size(48.dp)
        )
        Text(
            modifier = Modifier.align(Alignment.Center).padding(4.dp),
            text = number.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}