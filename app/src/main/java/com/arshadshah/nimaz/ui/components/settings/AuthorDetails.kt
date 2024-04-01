package com.arshadshah.nimaz.ui.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.theme.NimazTheme

@Composable
fun AuthorDetails() {

    //Designed and Created By: Arshad Shah
    //occupation: Associate Software Engineer
    //company: HMHco (Houghton Mifflin Harcourt)
    //Description: Loves to code, create, and learn new things.
    //Nimaz is a project that I created to learn more about Android development and to help others learn about Islam.
    //it is created as part of my final year project for my BSc in Computer Science.
    //it is a free and open source project that I hope will be useful to many people. I hope you enjoy it.
    ElevatedCard(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth(),
        content = {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            ) {
                Text(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    text = "Designed and Developed By",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    text = "Arshad Shah",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    text = "Associate Software Engineer",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                AuthorLinks()
            }
        },
    )
}

@Preview
@Composable
fun AuthorDetailsPreview() {
    NimazTheme {
        AuthorDetails()
    }
}