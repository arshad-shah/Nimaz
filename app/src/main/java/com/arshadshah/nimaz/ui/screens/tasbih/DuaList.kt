package com.arshadshah.nimaz.ui.screens.tasbih

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.models.LocalDua
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.viewModel.DuaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuaList(
    chapterId: String,
    navController: NavHostController,
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Supplications")
                },
                navigationIcon = {
                    OutlinedIconButton(
                        modifier = Modifier
                            .testTag("backButton")
                            .padding(start = 8.dp),
                        onClick = {
                            navController.popBackStack()
                        }) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(id = R.drawable.back_icon),
                            contentDescription = "Back"
                        )
                    }
                },
            )
        },
    ) {
        Card(
            modifier = Modifier
                .padding(it)
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
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                // Arabic Text
                Text(
                    text = dua.arabic_dua.cleanText(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = utmaniQuranFont,
                        fontSize = 28.sp,
                        lineHeight = 46.sp,
                        textAlign = TextAlign.Start
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    softWrap = true,  // Enable text wrapping
                    overflow = TextOverflow.Visible  // Show all content
                )
            }

            // Translation
            Text(
                text = dua.english_translation.cleanText(),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontStyle = FontStyle.Italic,
                    lineHeight = 28.sp,
                    letterSpacing = 0.3.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
                softWrap = true,
                overflow = TextOverflow.Visible
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
                            append(dua.english_reference.formatReference())
                        }
                    },
                    style = MaterialTheme.typography.labelLarge.copy(
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}


fun String.cleanText(): String {
    return this
        .replace(Regex("<[^>]*>"), "")  // Remove HTML tags
        .replace("\\r\\n", "\n")        // Convert literal "\r\n" to actual line break
        .replace("\\n", "\n")           // Convert literal "\n" to actual line break
        .replace("\\r", "\n")           // Convert literal "\r" to line break
        .trim()
}

/**
 * Formats a reference string by cleaning up quotes, backslashes and whitespace.
 */
private fun String.formatReference(): String = this
    .replace("\"", "")         // Remove quotes
    .replace("\\", "")         // Remove backslashes
    .replace(Regex("\\s+"), " ") // Normalize whitespace to single spaces
    .trim()                    // Remove leading/trailing whitespace