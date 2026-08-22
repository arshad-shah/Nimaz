package com.arshadshah.nimaz.presentation.screens.quran

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.core.navigation.taggedComposable
import com.arshadshah.nimaz.domain.model.TopicTree
import com.arshadshah.nimaz.presentation.screens.adaptive.AdaptiveKhatamScreen
import com.arshadshah.nimaz.presentation.screens.adaptive.AdaptiveQuranScreen
import com.arshadshah.nimaz.presentation.screens.bookmarks.SavedScreen
import com.arshadshah.nimaz.presentation.screens.khatam.KhatamDetailScreen
import com.arshadshah.nimaz.presentation.screens.khatam.KhatamFormScreen

/**
 * The 21 Quran destinations — the reader, the mushaf, tafseer, topics, khatam and bookmarks.
 *
 * Split out of `NavGraph.kt` in PR 12 of #551. That file registered all 94 destinations and
 * imported 69 screen composables, which meant every screen in the app was reachable from one
 * place — and no feature could move into its own module while that was true, because `:app`
 * would have had to import from all eleven feature modules at once.
 *
 * The bodies are unchanged; only their location is. `:app` keeps the `NavHost` and calls this.
 *
 * It takes a `NavController` rather than the `onNavigate` lambda #563 sketches because 11 of the
 * 158 `navigate` calls in these blocks pass a `NavOptionsBuilder` — `popUpTo`, `launchSingleTop` —
 * which `(Route) -> Unit` cannot express, and flattening them would change back-stack behaviour
 * silently. A graph function *is* navigation wiring, so holding the controller is what it is for;
 * the rule that matters is that a **screen** must not, which `NavControllerConfinementTest`
 * enforces.
 */
fun NavGraphBuilder.quranGraph(navController: NavController) {
    taggedComposable<Route.Quran>(ScreenTags.Quran) {
        AdaptiveQuranScreen(
            onNavigate = { navController.navigate(it) },
            onBack = { navController.popBackStack() },
            onNavigateToSearch = { navController.navigate(Route.GlobalSearch) },
            onNavigateToTopics = { navController.navigate(Route.QuranTopics) },
            onNavigateToBrowse = { navController.navigate(Route.QuranBrowse()) },
            onNavigateToSaved = { navController.navigate(Route.QuranSaved) },
            onNavigateToSettings = { navController.navigate(Route.SettingsQuran) },
            onNavigateToKhatam = { navController.navigate(Route.KhatamList) },
        )
    }

    taggedComposable<Route.QuranBrowse>(ScreenTags.QuranBrowse) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.QuranBrowse>()
        QuranBrowseScreen(
            initialInfoForSurah = args.infoForSurah,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToSurah = { surah ->
                navController.navigate(Route.QuranReader(surah))
            },
            onNavigateToJuz = { juz -> navController.navigate(Route.QuranJuz(juz)) },
            onNavigateToPage = { page -> navController.navigate(Route.QuranPage(page)) },
            onOpenBackground = { surah ->
                navController.navigate(Route.SurahBackground(surah))
            },
            onOpenPassages = { surah ->
                navController.navigate(Route.SurahPassages(surah))
            },
            onOpenSubjects = { surah ->
                navController.navigate(Route.SurahSubjects(surah))
            },
        )
    }

    // Quran screens
    taggedComposable<Route.QuranReader>(ScreenTags.QuranReader) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.QuranReader>()
        QuranReaderScreen(
            surahNumber = args.surahNumber,
            initialAyahNumber = args.ayahNumber,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToQuranSettings = { navController.navigate(Route.SettingsQuran) },
            onNavigateToTafseer = { surah, ayah ->
                navController.navigate(Route.Tafseer(surah, ayah))
            },
            onNavigateToPassages = { surah, ayah ->
                navController.navigate(Route.SurahPassages(surah, ayah))
            },
            onNavigateToSubjects = { surah ->
                navController.navigate(
                    if (surah != null) Route.SurahSubjects(surah) else Route.QuranTopics
                )
            },
            onNavigateToReciters = { navController.navigate(Route.SelectReciter) },
            onNavigateToNextSurah = { nextSurah ->
                navController.navigate(Route.QuranReader(nextSurah)) {
                    popUpTo<Route.QuranReader> { inclusive = true }
                }
            }
        )
    }

    taggedComposable<Route.TafseerChapters>(ScreenTags.TafseerChapters) {
        TafseerChaptersScreen(
            onNavigateBack = { navController.popBackStack() },
            onOpenTafseer = { surah, ayah ->
                navController.navigate(
                    Route.Tafseer(
                        surahNumber = surah,
                        ayahNumber = ayah
                    )
                )
            }
        )
    }

    taggedComposable<Route.Tafseer>(ScreenTags.Tafseer) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.Tafseer>()
        TafseerScreen(
            surahNumber = args.surahNumber,
            ayahNumber = args.ayahNumber,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToTopic = { topicId ->
                navController.navigate(Route.QuranTopicDetail(topicId))
            }
        )
    }

    taggedComposable<Route.SurahBackground>(ScreenTags.SurahBackground) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.SurahBackground>()
        SurahBackgroundScreen(
            surahNumber = args.surahNumber,
            onNavigateBack = { navController.popBackStack() },
            onOpenAyah = { surah, ayah ->
                navController.navigate(Route.QuranReader(surah, ayah))
            },
            onOpenTopic = { topicId ->
                navController.navigate(Route.QuranTopicDetail(topicId))
            }
        )
    }

    taggedComposable<Route.SurahPassages>(ScreenTags.SurahPassages) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.SurahPassages>()
        SurahPassagesScreen(
            surahNumber = args.surahNumber,
            currentAyah = args.currentAyah,
            onNavigateBack = { navController.popBackStack() },
            onOpenAyah = { surah, ayah ->
                navController.navigate(Route.QuranReader(surah, ayah))
            }
        )
    }

    taggedComposable<Route.SurahSubjects>(ScreenTags.SurahSubjects) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.SurahSubjects>()
        SurahSubjectsScreen(
            surahNumber = args.surahNumber,
            onNavigateBack = { navController.popBackStack() },
            onOpenTopic = { topicId, tree ->
                navController.navigate(
                    Route.QuranTopicDetail(topicId, tree.wire, args.surahNumber)
                )
            },
            onBrowseAllSubjects = { navController.navigate(Route.QuranTopics) }
        )
    }

    taggedComposable<Route.QuranTopics>(ScreenTags.QuranTopics) {
        QuranTopicsScreen(
            onNavigateBack = { navController.popBackStack() },
            onOpenTopic = { topicId, tree ->
                navController.navigate(Route.QuranTopicDetail(topicId, tree.wire))
            }
        )
    }

    taggedComposable<Route.QuranTopicDetail>(ScreenTags.QuranTopicDetail) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.QuranTopicDetail>()
        QuranTopicDetailScreen(
            topicId = args.topicId,
            tree = TopicTree.fromWire(args.tree),
            fromSurah = args.fromSurah,
            onNavigateBack = { navController.popBackStack() },
            onOpenAyah = { surah, ayah ->
                navController.navigate(Route.QuranReader(surah, ayah))
            },
            // The surah travels with every lateral move — a subtopic, a related
            // subject, a cross-link in the prose. Dropping it one hop in would put the
            // reader back where this whole change started, one screen later.
            onOpenTopic = { topicId, tree ->
                navController.navigate(
                    Route.QuranTopicDetail(topicId, tree.wire, args.fromSurah)
                )
            }
        )
    }

    taggedComposable<Route.QuranPage>(ScreenTags.QuranPage) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.QuranPage>()
        QuranReaderScreen(
            pageNumber = args.pageNumber,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToQuranSettings = { navController.navigate(Route.SettingsQuran) },
            onNavigateToTafseer = { surah, ayah ->
                navController.navigate(Route.Tafseer(surah, ayah))
            },
            onNavigateToPassages = { surah, ayah ->
                navController.navigate(Route.SurahPassages(surah, ayah))
            },
            onNavigateToSubjects = { surah ->
                navController.navigate(
                    if (surah != null) Route.SurahSubjects(surah) else Route.QuranTopics
                )
            },
            onNavigateToReciters = { navController.navigate(Route.SelectReciter) }
        )
    }

    taggedComposable<Route.QuranJuz>(ScreenTags.QuranJuz) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.QuranJuz>()
        QuranReaderScreen(
            juzNumber = args.juzNumber,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToQuranSettings = { navController.navigate(Route.SettingsQuran) },
            onNavigateToTafseer = { surah, ayah ->
                navController.navigate(Route.Tafseer(surah, ayah))
            },
            onNavigateToPassages = { surah, ayah ->
                navController.navigate(Route.SurahPassages(surah, ayah))
            },
            onNavigateToSubjects = { surah ->
                navController.navigate(
                    if (surah != null) Route.SurahSubjects(surah) else Route.QuranTopics
                )
            },
            onNavigateToReciters = { navController.navigate(Route.SelectReciter) }
        )
    }

    taggedComposable<Route.QuranSaved>(ScreenTags.QuranSaved) {
        SavedScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToQuranAyah = { surah, ayah ->
                navController.navigate(Route.QuranReader(surah, ayah))
            },
            onNavigateToHadith = { bookId, hadithNumber ->
                navController.navigate(Route.HadithByNumber(bookId, hadithNumber))
            },
            onNavigateToDua = { duaId ->
                navController.navigate(Route.DuaReader(duaId))
            },
        )
    }

    taggedComposable<Route.HadithBookmarks>(ScreenTags.HadithBookmarks) {
        SavedScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToQuranAyah = { surah, ayah ->
                navController.navigate(Route.QuranReader(surah, ayah))
            },
            onNavigateToHadith = { bookId, hadithNumber ->
                navController.navigate(Route.HadithByNumber(bookId, hadithNumber))
            },
            onNavigateToDua = { duaId ->
                navController.navigate(Route.DuaReader(duaId))
            },
        )
    }

    taggedComposable<Route.SelectReciter>(ScreenTags.SelectReciter) {
        SelectReciterScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    taggedComposable<Route.SelectTranslation>(ScreenTags.SelectTranslation) {
        SelectTranslationScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    // Khatam screens
    taggedComposable<Route.KhatamList>(ScreenTags.KhatamList) {
        AdaptiveKhatamScreen(
            onNavigate = { navController.navigate(it) },
            onNavigateBack = { navController.popBackStack() },
            onNavigateToCreate = { navController.navigate(Route.KhatamCreate) },
        )
    }

    taggedComposable<Route.KhatamDetail>(ScreenTags.KhatamDetail) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.KhatamDetail>()
        KhatamDetailScreen(
            khatamId = args.khatamId,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToRead = { surahNumber, ayahNumber ->
                navController.navigate(Route.QuranReader(surahNumber, ayahNumber))
            },
            onNavigateToEdit = { khatamId ->
                navController.navigate(Route.KhatamEdit(khatamId))
            }
        )
    }

    // Create and edit share one screen; a null id means "create".
    taggedComposable<Route.KhatamCreate>(ScreenTags.KhatamCreate) {
        KhatamFormScreen(
            khatamId = null,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    taggedComposable<Route.KhatamEdit>(ScreenTags.KhatamEdit) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.KhatamEdit>()
        KhatamFormScreen(
            khatamId = args.khatamId,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    // Bookmarks
    taggedComposable<Route.AllBookmarks>(ScreenTags.AllBookmarks) {
        SavedScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToQuranAyah = { surah, ayah ->
                navController.navigate(Route.QuranReader(surah, ayah))
            },
            onNavigateToHadith = { bookId, hadithNumber ->
                navController.navigate(Route.HadithByNumber(bookId, hadithNumber))
            },
            onNavigateToDua = { duaId ->
                navController.navigate(Route.DuaReader(duaId))
            },
        )
    }
}
