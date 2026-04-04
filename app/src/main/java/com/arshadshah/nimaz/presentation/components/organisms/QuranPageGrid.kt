package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.local.database.dao.PageAyahRange

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
internal fun LazyListScope.pageGridItems(
    onNavigateToPage: (Int) -> Unit,
    khatamReadAyahIds: Set<Int> = emptySet(),
    isKhatamActive: Boolean = false,
    pageAyahRanges: List<PageAyahRange> = emptyList(),
    selectedPageNumber: Int? = null,
    surahStartPageMap: Map<Int, List<String>> = emptyMap()
) {
    val columns = 5

    // Pre-compute page progress map
    val pageProgressMap = if (isKhatamActive && pageAyahRanges.isNotEmpty()) {
        pageAyahRanges.associate { range ->
            val readCount = khatamReadAyahIds.count { it in range.minAyahId..range.maxAyahId }
            range.page to (readCount to range.ayahCount)
        }
    } else {
        emptyMap()
    }

    // Jump-to-page input as first item
    item(key = "page_jump_input") {
        var jumpToPage by remember { mutableStateOf("") }
        OutlinedTextField(
            value = jumpToPage,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                    jumpToPage = newValue
                }
            },
            label = { Text(stringResource(R.string.quran_home_jump_to_page)) },
            placeholder = { Text(stringResource(R.string.quran_home_enter_page_number)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(
                onGo = {
                    jumpToPage.toIntOrNull()?.let { page ->
                        if (page in 1..604) {
                            onNavigateToPage(page)
                        }
                    }
                }
            ),
            trailingIcon = {
                IconButton(
                    onClick = {
                        jumpToPage.toIntOrNull()?.let { page ->
                            if (page in 1..604) {
                                onNavigateToPage(page)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.quran_home_go_to_page),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Add items for each Juz section
    (1..30).forEach { juz ->
        val startPage = getJuzStartPage(juz)
        val endPage = getJuzEndPage(juz)

        // Juz header
        item(key = "page_juz_header_$juz") {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.quran_home_juz_pages_format, juz, startPage, endPage),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        // Pages in this Juz, chunked into rows
        val pagesInJuz = (startPage..endPage).toList()
        val chunkedPages = pagesInJuz.chunked(columns)
        chunkedPages.forEach { row ->
            // Show per-page surah start indicators above the row
            row.forEach { pageNumber ->
                val surahs = surahStartPageMap[pageNumber]
                if (!surahs.isNullOrEmpty()) {
                    item(key = "surah_start_page_$pageNumber") {
                        FlowRow(
                            modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            surahs.forEach { surahName ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "\u25B8 $surahName \u00B7 p.$pageNumber",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 3.dp, horizontal = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item(key = "page_row_${row.first()}") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { pageNumber ->
                        val (readCount, totalCount) = pageProgressMap[pageNumber] ?: (0 to 0)
                        val progress = if (totalCount > 0) readCount.toFloat() / totalCount else 0f
                        val isComplete = isKhatamActive && totalCount > 0 && readCount == totalCount
                        val isSelected = selectedPageNumber == pageNumber

                        Card(
                            onClick = { onNavigateToPage(pageNumber) },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .border(
                                    width = if (isSelected) 2.dp else 1.5.dp,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    isComplete -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                }
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                // Progress ring when khatam is active and partially read
                                if (isKhatamActive && progress > 0f && !isComplete) {
                                    CircularProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.size(48.dp),
                                        color = MaterialTheme.colorScheme.tertiary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        strokeWidth = 3.dp
                                    )
                                }
                                Text(
                                    text = pageNumber.toString(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isComplete || isSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    // Fill remaining space if row is incomplete
                    repeat(columns - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
