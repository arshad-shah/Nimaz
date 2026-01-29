package com.arshadshah.nimaz.presentation.screens.hadith

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.Hadith
import com.arshadshah.nimaz.domain.model.HadithBook
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.HadithArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.HadithEvent
import com.arshadshah.nimaz.presentation.viewmodel.HadithViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithCollectionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBook: (String) -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToSearch: () -> Unit,
    viewModel: HadithViewModel = hiltViewModel()
) {
    val state by viewModel.collectionState.collectAsState()
    val bookmarksState by viewModel.bookmarksState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = "Hadith",
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                    IconButton(onClick = onNavigateToBookmarks) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmarks"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Stats Row
                item {
                    StatsRow(
                        readToday = 0,
                        bookmarked = bookmarksState.bookmarks.size,
                        dayStreak = 0
                    )
                }

                // Hadith of the Day
                item {
                    val hadithOfTheDay = state.hadithOfTheDay
                    HadithOfTheDayCard(
                        hadith = hadithOfTheDay,
                        onBookmarkClick = {
                            hadithOfTheDay?.let { hadith ->
                                viewModel.onEvent(
                                    HadithEvent.ToggleBookmark(
                                        hadithId = hadith.id,
                                        bookId = hadith.bookId,
                                        hadithNumber = hadith.hadithNumberInBook
                                    )
                                )
                            }
                        },
                        onShareClick = {
                            val shareText = buildString {
                                if (hadithOfTheDay != null) {
                                    appendLine(hadithOfTheDay.textArabic)
                                    appendLine()
                                    appendLine(hadithOfTheDay.textEnglish)
                                    appendLine()
                                    appendLine(hadithOfTheDay.reference ?: "")
                                } else {
                                    appendLine("مَنْ كَانَ يُؤْمِنُ بِاللَّهِ وَالْيَوْمِ الْآخِرِ فَلْيَقُلْ خَيْرًا أَوْ لِيَصْمُتْ")
                                    appendLine()
                                    appendLine("\"Whoever believes in Allah and the Last Day, let him speak good or remain silent.\"")
                                    appendLine()
                                    appendLine("Sahih al-Bukhari 6018")
                                }
                            }
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Hadith"))
                        },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                // Books Section Header
                item {
                    SectionHeader(
                        title = "Kutub al-Sittah",
                        showSeeAll = true,
                        onSeeAllClick = {
                            Toast.makeText(context, "All books are shown below", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                // Books Grid
                item {
                    BooksGrid(
                        books = state.books,
                        onBookClick = onNavigateToBook,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsRow(
    readToday: Int,
    bookmarked: Int,
    dayStreak: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            value = "$readToday",
            label = "Read Today",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = "$bookmarked",
            label = "Bookmarked",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = "$dayStreak",
            label = "Day Streak",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun HadithOfTheDayCard(
    hadith: Hadith?,
    onBookmarkClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Fallback values when hadith is null
    val arabicText = hadith?.textArabic ?: "مَنْ كَانَ يُؤْمِنُ بِاللَّهِ وَالْيَوْمِ الْآخِرِ فَلْيَقُلْ خَيْرًا أَوْ لِيَصْمُتْ"
    val translationText = hadith?.textEnglish ?: "\"Whoever believes in Allah and the Last Day, let him speak good or remain silent.\""
    val source = hadith?.reference ?: "Sahih al-Bukhari 6018"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                // Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFEAB308).copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFACC15),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Hadith of the Day",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFACC15),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(15.dp))

                // Arabic text - using HadithArabicText for proper Amiri font rendering
                HadithArabicText(
                    text = arabicText,
                    size = ArabicTextSize.MEDIUM,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(15.dp))

                // English translation
                Text(
                    text = translationText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(15.dp))

                // Source row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = source,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HadithActionButton(
                            icon = Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            onClick = onBookmarkClick
                        )
                        HadithActionButton(
                            icon = Icons.Default.Share,
                            contentDescription = "Share",
                            onClick = onShareClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HadithActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    showSeeAll: Boolean,
    onSeeAllClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (showSeeAll) {
            Text(
                text = "See All",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onSeeAllClick)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BooksGrid(
    books: List<HadithBook>,
    onBookClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Use a Column with Rows to create a 2-column grid inside LazyColumn
    val rows = books.chunked(2)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { rowBooks ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowBooks.forEach { book ->
                    BookCard(
                        book = book,
                        onClick = { onBookClick(book.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining space if odd number
                if (rowBooks.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookCard(
    book: HadithBook,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    getBookColor(book.id)
    val bookGradient = getBookGradient(book.id)

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(15.dp)
        ) {
            // Book icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(bookGradient)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = book.nameEnglish,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            ArabicText(
                text = book.nameArabic,
                size = ArabicTextSize.SMALL,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${formatNumber(book.totalHadiths)} hadith",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

private fun formatNumber(number: Int): String {
    return String.format("%,d", number)
}

@Composable
private fun getBookColor(bookId: String): Color {
    return when (bookId.lowercase()) {
        "bukhari" -> Color(0xFF22C55E)
        "muslim" -> Color(0xFF3B82F6)
        "tirmidhi" -> Color(0xFFA855F7)
        "nasai" -> Color(0xFFF97316)
        "abudawud" -> Color(0xFFEC4899)
        "ibnmajah" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
private fun getBookGradient(bookId: String): List<Color> {
    return when (bookId.lowercase()) {
        "bukhari" -> listOf(Color(0xFF22C55E), Color(0xFF16A34A))
        "muslim" -> listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
        "tirmidhi" -> listOf(Color(0xFFA855F7), Color(0xFF9333EA))
        "nasai" -> listOf(Color(0xFFF97316), Color(0xFFEA580C))
        "abudawud" -> listOf(Color(0xFFEC4899), Color(0xFFDB2777))
        "ibnmajah" -> listOf(Color(0xFF14B8A6), Color(0xFF0D9488))
        else -> listOf(Color(0xFF14B8A6), Color(0xFF0D9488))
    }
}
