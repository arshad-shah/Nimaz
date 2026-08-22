package com.arshadshah.nimaz.presentation.screens.adaptive

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.presentation.screens.settings.AppearanceSettingsScreen
import com.arshadshah.nimaz.presentation.screens.settings.LanguageScreen
import com.arshadshah.nimaz.presentation.screens.settings.LocationScreen
import com.arshadshah.nimaz.presentation.screens.settings.NotificationSettingsScreen
import com.arshadshah.nimaz.presentation.screens.settings.PrayerSettingsScreen
import com.arshadshah.nimaz.presentation.screens.settings.QuranSettingsScreen
import com.arshadshah.nimaz.presentation.screens.settings.SettingsScreen
import com.arshadshah.nimaz.presentation.screens.settings.SyncScreen
import com.arshadshah.nimaz.presentation.screens.settings.WidgetsScreen
import com.arshadshah.nimaz.presentation.screens.settings.ZakatSettingsScreen
import com.arshadshah.nimaz.presentation.theme.currentWindowSizeClass
import com.arshadshah.nimaz.presentation.theme.isCompact
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveSettingsScreen(
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    onRestartApp: () -> Unit,
) {
    val windowSizeClass = currentWindowSizeClass()

    if (windowSizeClass.isCompact) {
        SettingsScreen(
            onNavigateBack = { onBack() },
            onNavigateToPrayerSettings = { onNavigate(Route.SettingsPrayerCalculation) },
            onNavigateToNotifications = { onNavigate(Route.SettingsNotifications) },
            onNavigateToQuranSettings = { onNavigate(Route.SettingsQuran) },
            onNavigateToAppearance = { onNavigate(Route.SettingsAppearance) },
            onNavigateToLocation = { onNavigate(Route.SettingsLocation) },
            onNavigateToLanguage = { onNavigate(Route.SettingsLanguage) },
            onNavigateToWidgets = { onNavigate(Route.SettingsWidgets) },
            onNavigateToSync = { onNavigate(Route.SettingsSync) },
            onNavigateToSearchSettings = { onNavigate(Route.SearchSettings) },
            onNavigateToZakatSettings = { onNavigate(Route.SettingsZakat) },
            onRestartApp = onRestartApp,
        )
    } else {
        val navigator = rememberListDetailPaneScaffoldNavigator<SettingsDetailArgs>()
        val scope = rememberCoroutineScope()

        NavigableListDetailPaneScaffold(
            navigator = navigator,
            listPane = {
                AnimatedPane {
                    SettingsScreen(
                        onNavigateBack = { onBack() },
                        onNavigateToPrayerSettings = {
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    SettingsDetailArgs(SettingsDetailPane.PRAYER)
                                )
                            }
                        },
                        onNavigateToNotifications = {
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    SettingsDetailArgs(SettingsDetailPane.NOTIFICATIONS)
                                )
                            }
                        },
                        onNavigateToQuranSettings = {
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    SettingsDetailArgs(SettingsDetailPane.QURAN)
                                )
                            }
                        },
                        onNavigateToAppearance = {
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    SettingsDetailArgs(SettingsDetailPane.APPEARANCE)
                                )
                            }
                        },
                        onNavigateToLocation = {
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    SettingsDetailArgs(SettingsDetailPane.LOCATION)
                                )
                            }
                        },
                        onNavigateToLanguage = {
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    SettingsDetailArgs(SettingsDetailPane.LANGUAGE)
                                )
                            }
                        },
                        onNavigateToWidgets = {
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    SettingsDetailArgs(SettingsDetailPane.WIDGETS)
                                )
                            }
                        },
                        onNavigateToSync = {
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    SettingsDetailArgs(SettingsDetailPane.SYNC)
                                )
                            }
                        },
                        onNavigateToSearchSettings = {
                            onNavigate(Route.SearchSettings)
                        },
                        onNavigateToZakatSettings = {
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    SettingsDetailArgs(SettingsDetailPane.ZAKAT)
                                )
                            }
                        },
                        onRestartApp = onRestartApp,
                    )
                }
            },
            detailPane = {
                AnimatedPane {
                    val args = navigator.currentDestination?.contentKey
                    if (args != null) {
                        when (args.pane) {
                            SettingsDetailPane.PRAYER -> PrayerSettingsScreen(
                                onNavigateBack = { scope.launch { navigator.navigateBack() } },
                                onNavigateToNotifications = {
                                    scope.launch {
                                        navigator.navigateTo(
                                            ListDetailPaneScaffoldRole.Detail,
                                            SettingsDetailArgs(SettingsDetailPane.NOTIFICATIONS)
                                        )
                                    }
                                }
                            )

                            SettingsDetailPane.NOTIFICATIONS -> NotificationSettingsScreen(
                                onNavigateBack = { scope.launch { navigator.navigateBack() } }
                            )

                            SettingsDetailPane.QURAN -> QuranSettingsScreen(
                                onNavigateBack = { scope.launch { navigator.navigateBack() } },
                                onNavigateToSelectReciter = {
                                    onNavigate(Route.SelectReciter)
                                },
                                onNavigateToSelectTranslation = {
                                    onNavigate(Route.SelectTranslation)
                                }
                            )

                            SettingsDetailPane.APPEARANCE -> AppearanceSettingsScreen(
                                onNavigateBack = { scope.launch { navigator.navigateBack() } }
                            )

                            SettingsDetailPane.LOCATION -> LocationScreen(
                                onNavigateBack = { scope.launch { navigator.navigateBack() } }
                            )

                            SettingsDetailPane.LANGUAGE -> LanguageScreen(
                                onNavigateBack = { scope.launch { navigator.navigateBack() } }
                            )

                            SettingsDetailPane.WIDGETS -> WidgetsScreen(
                                onNavigateBack = { scope.launch { navigator.navigateBack() } }
                            )

                            SettingsDetailPane.SYNC -> SyncScreen(
                                onNavigateBack = { scope.launch { navigator.navigateBack() } }
                            )

                            SettingsDetailPane.ZAKAT -> ZakatSettingsScreen(
                                onNavigateBack = { scope.launch { navigator.navigateBack() } }
                            )
                        }
                    }
                }
            }
        )
    }
}

enum class SettingsDetailPane {
    PRAYER, NOTIFICATIONS, QURAN, APPEARANCE, LOCATION, LANGUAGE, WIDGETS, SYNC, ZAKAT
}

@kotlinx.parcelize.Parcelize
data class SettingsDetailArgs(
    val pane: SettingsDetailPane,
) : android.os.Parcelable
