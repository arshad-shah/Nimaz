package com.arshadshah.nimaz.presentation.screens.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arshadshah.nimaz.core.navigation.ScreenTags
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreMenuScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onShareApp: () -> Unit,
    onRateApp: () -> Unit,
    onNavigateToHadith: () -> Unit,
    onNavigateToFasting: () -> Unit,
    onNavigateToZakat: () -> Unit,
    onNavigateToDuas: () -> Unit,
    onNavigateToTafseer: () -> Unit,
    onNavigateToPrayerTracker: () -> Unit,
    onNavigateToPrayerTimes: () -> Unit,
    onNavigateToMonthlyPrayerTimes: () -> Unit,
    onNavigateToKhatam: () -> Unit,
    onNavigateToAsmaUlHusna: () -> Unit,
    onNavigateToAsmaUnNabi: () -> Unit,
    onNavigateToProphets: () -> Unit,
    onNavigateToQaida: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazTopAppBar(
                title = stringResource(R.string.more),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(ScreenTags.MoreList)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Daily Practice Section
            item {
                NimazSectionHeader(title = stringResource(R.string.daily_practice))
            }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = stringResource(R.string.prayer_tracker),
                        subtitle = stringResource(R.string.prayer_tracker_subtitle),
                        icon = Icons.Default.Schedule,
                        onClick = onNavigateToPrayerTracker
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.fasting),
                        subtitle = stringResource(R.string.fasting_subtitle),
                        icon = Icons.Default.Fastfood,
                        onClick = onNavigateToFasting
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.khatam_quran),
                        subtitle = stringResource(R.string.khatam_quran_subtitle),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        onClick = onNavigateToKhatam
                    )
                }
            }

            // Learning Section
            item {
                NimazSectionHeader(title = stringResource(R.string.learning))
            }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = stringResource(R.string.qaida),
                        subtitle = stringResource(R.string.qaida_subtitle),
                        icon = Icons.Default.Abc,
                        onClick = onNavigateToQaida
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.allahs_99_names),
                        subtitle = stringResource(R.string.allahs_99_names_subtitle),
                        icon = Icons.Default.AutoAwesome,
                        onClick = onNavigateToAsmaUlHusna
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.prophets_99_names),
                        subtitle = stringResource(R.string.prophets_99_names_subtitle),
                        icon = Icons.Default.Person,
                        onClick = onNavigateToAsmaUnNabi
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.prophets_of_islam),
                        subtitle = stringResource(R.string.prophets_of_islam_subtitle),
                        icon = Icons.Default.Groups,
                        onClick = onNavigateToProphets
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.hadith),
                        subtitle = stringResource(R.string.hadith_subtitle),
                        icon = Icons.Default.FormatQuote,
                        onClick = onNavigateToHadith
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.duas),
                        subtitle = stringResource(R.string.duas_subtitle),
                        icon = ImageVector.vectorResource(R.drawable.ic_dua),
                        onClick = onNavigateToDuas
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.tafseer),
                        subtitle = stringResource(R.string.tafseer_subtitle),
                        icon = Icons.AutoMirrored.Filled.Article,
                        onClick = onNavigateToTafseer
                    )
                }
            }

            // Tools Section
            item {
                NimazSectionHeader(title = stringResource(R.string.tools))
            }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = stringResource(R.string.calendar),
                        subtitle = stringResource(R.string.calendar_subtitle),
                        icon = Icons.Default.CalendarMonth,
                        onClick = onNavigateToCalendar
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.prayer_times),
                        subtitle = stringResource(R.string.prayer_times_menu_subtitle),
                        icon = Icons.Default.Mosque,
                        onClick = onNavigateToPrayerTimes
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.monthly_prayer_times),
                        subtitle = stringResource(R.string.monthly_prayer_times_subtitle),
                        icon = Icons.Default.CalendarViewMonth,
                        onClick = onNavigateToMonthlyPrayerTimes
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.zakat),
                        subtitle = stringResource(R.string.zakat_subtitle),
                        icon = Icons.Default.Calculate,
                        onClick = onNavigateToZakat
                    )
                }
            }

            // Support Section
            item {
                NimazSectionHeader(title = stringResource(R.string.support))
            }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = stringResource(R.string.about_nimaz),
                        subtitle = stringResource(R.string.about_nimaz_subtitle),
                        icon = Icons.Default.Info,
                        onClick = onNavigateToAbout
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.help_support),
                        subtitle = stringResource(R.string.help_support_subtitle),
                        icon = Icons.AutoMirrored.Filled.Help,
                        onClick = onNavigateToHelp
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.share_app),
                        subtitle = stringResource(R.string.share_app_subtitle),
                        icon = Icons.Default.Share,
                        onClick = onShareApp
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.rate_us),
                        subtitle = stringResource(R.string.rate_us_subtitle),
                        icon = Icons.Default.Star,
                        onClick = onRateApp
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
