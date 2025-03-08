package com.arshadshah.nimaz.ui.components.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R

@Composable
fun NoResultFound(
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier
                .fillMaxWidth(0.4f),
            painter = painterResource(id = R.drawable.no_results_icon),
            contentDescription = "No Result found",
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .fillMaxWidth()
        )
        Text(
            text = subtitle,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

@Preview
@Composable
fun NoResultFoundPreview() {
    NoResultFound(
        title = "No result found",
        subtitle = "Please try again"
    )
}