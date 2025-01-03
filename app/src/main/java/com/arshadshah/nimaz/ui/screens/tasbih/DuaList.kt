package com.arshadshah.nimaz.ui.screens.tasbih

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.models.LocalDua
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.viewModel.DuaViewModel
import java.util.regex.Pattern

@Composable
fun DuaList(
    chapterId: String,
    paddingValues: PaddingValues
) {
    val context = LocalContext.current
    val viewModel = viewModel<DuaViewModel>(
        key = AppConstants.DUA_CHAPTERS_VIEWMODEL_KEY,
        viewModelStoreOwner = context as ComponentActivity
    )
    val duaState = viewModel.duas.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.getDuas(chapterId.toInt())
    }

    Card(
        modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        LazyColumn(
            modifier = Modifier.testTag(AppConstants.TEST_TAG_CHAPTER),
            state = listState,
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(duaState.value.size) { index ->
                DuaListItem(
                    dua = duaState.value[index],
                    isLastItem = index == duaState.value.size - 1
                )

                if (index < duaState.value.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuaListItem(
    dua: LocalDua,
    isLastItem: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            // Arabic Text
            Text(
                text = dua.arabic_dua,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = utmaniQuranFont,
                    fontSize = 28.sp,
                    lineHeight = 46.sp,
                    textAlign = TextAlign.Start
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            )

            // Translation
            Text(
                text = dua.english_translation,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontStyle = FontStyle.Italic,
                    lineHeight = 28.sp,
                    letterSpacing = 0.3.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Reference
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            append("Reference: ")
                        }
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
                            )
                        ) {
                            append(formatReference(dua.english_reference))
                        }
                    },
                    style = MaterialTheme.typography.labelLarge.copy(
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // Optional: Favorite Button
            IconButton(
                onClick = { /* Handle favorite toggle */ },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = if (dua.favourite == 1)
                        Icons.Filled.Favorite
                    else
                        Icons.Outlined.FavoriteBorder,
                    contentDescription = if (dua.favourite == 1)
                        "Remove from favorites"
                    else
                        "Add to favorites",
                    tint = if (dua.favourite == 1)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                )
            }
        }
    }
}


/**
 * Formats a reference string by removing unnecessary characters and whitespace.
 *
 * @param reference The reference string to format.
 * @return The formatted reference string.
 */
private fun formatReference(reference: String): String {
    // 1. Handle quotes more effectively.
    // Instead of replacing double quotes with empty strings, we should consider
    // if they are part of the content or just delimiters. If they are delimiters,
    // we can remove them. If they are part of the content, we should keep them.
    // For this example, we assume they are delimiters and remove them.
    // If the requirement changes, we can adjust this part.
    val withoutQuotes = reference.replace("\"", "")

    // 2. Remove backslashes.
    // Backslashes are often used for escaping characters. If they are not needed,
    // we can remove them.
    val withoutBackslashes = withoutQuotes.replace("\\", "")

    // 3. Normalize whitespace.
    // Replace multiple spaces with a single space.
    // Using a compiled Pattern for better performance if this function is called frequently.
    val whitespacePattern = Pattern.compile("\\s+")
    val normalizedWhitespace = whitespacePattern.matcher(withoutBackslashes).replaceAll(" ")

    // 4. Trim leading/trailing whitespace.
    // Remove any leading or trailing spaces.
    return normalizedWhitespace.trim()
}