package com.arshadshah.nimaz.presentation.screens.content

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import com.arshadshah.nimaz.core.navigation.NamesTab
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.core.navigation.taggedComposable
import com.arshadshah.nimaz.domain.model.DuaOccasion
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.arshadshah.nimaz.presentation.screens.adaptive.AdaptiveDuaScreen
import com.arshadshah.nimaz.presentation.screens.adaptive.AdaptiveHadithScreen
import com.arshadshah.nimaz.presentation.screens.adaptive.AdaptiveNamesScreen
import com.arshadshah.nimaz.presentation.screens.asma.AsmaUlHusnaDetailScreen
import com.arshadshah.nimaz.presentation.screens.asmaunnabi.AsmaUnNabiDetailScreen
import com.arshadshah.nimaz.presentation.screens.dua.DuaCategoryScreen
import com.arshadshah.nimaz.presentation.screens.dua.DuaOccasionScreen
import com.arshadshah.nimaz.presentation.screens.dua.DuaReaderScreen
import com.arshadshah.nimaz.presentation.screens.dua.DuaSettingsScreen
import com.arshadshah.nimaz.presentation.screens.dua.DuasCollectionScreen
import com.arshadshah.nimaz.presentation.screens.hadith.HadithChaptersScreen
import com.arshadshah.nimaz.presentation.screens.hadith.HadithReaderScreen
import com.arshadshah.nimaz.presentation.screens.hadith.HadithSettingsScreen
import com.arshadshah.nimaz.presentation.screens.names.FavouritesScreen
import com.arshadshah.nimaz.presentation.screens.prophets.ProphetDetailScreen
import com.arshadshah.nimaz.presentation.screens.qaida.QaidaHomeScreen
import com.arshadshah.nimaz.presentation.screens.qaida.QaidaLettersScreen
import com.arshadshah.nimaz.presentation.screens.qaida.QaidaReaderScreen

/**
 * The 21 Content destinations — dua, hadith, qaida, the name catalogues and the prophets.
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
fun NavGraphBuilder.contentGraph(navController: NavController) {
    // Qaida (children's Arabic reader) screens
    taggedComposable<Route.QaidaHome>(ScreenTags.QaidaHome) {
        QaidaHomeScreen(
            onNavigateBack = { navController.popBackStack() },
            onOpenLesson = { lessonId -> navController.navigate(Route.QaidaReader(lessonId)) },
            onOpenLetters = { navController.navigate(Route.QaidaLetters) },
        )
    }

    taggedComposable<Route.QaidaReader>(ScreenTags.QaidaReader) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.QaidaReader>()
        QaidaReaderScreen(
            lessonId = args.lessonId,
            onNavigateBack = { navController.popBackStack() },
        )
    }

    taggedComposable<Route.QaidaLetters>(ScreenTags.QaidaLetters) {
        QaidaLettersScreen(
            onNavigateBack = { navController.popBackStack() },
        )
    }

    // Hadith screens
    taggedComposable<Route.HadithHome>(ScreenTags.HadithHome) {
        AdaptiveHadithScreen(
            onNavigate = { navController.navigate(it) },
            onNavigateBack = { navController.popBackStack() },
            onNavigateToSearch = { navController.navigate(Route.HadithSearch) },
            onNavigateToBookmarks = { navController.navigate(Route.HadithBookmarks) },
            onNavigateToGrade = { grade ->
                navController.navigate(Route.HadithByGrade(grade.name))
            },
        )
    }

    taggedComposable<Route.HadithBook>(ScreenTags.HadithBook) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.HadithBook>()
        HadithChaptersScreen(
            bookId = args.bookId,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToChapter = { bookId, chapterId ->
                navController.navigate(Route.HadithChapter(bookId, chapterId))
            }
        )
    }

    taggedComposable<Route.HadithChapter>(ScreenTags.HadithChapter) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.HadithChapter>()
        HadithReaderScreen(
            bookId = args.bookId,
            chapterId = args.chapterId,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToSettings = { navController.navigate(Route.HadithSettings) }
        )
    }

    taggedComposable<Route.HadithReader>(ScreenTags.HadithReader) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.HadithReader>()
        HadithReaderScreen(
            bookId = "",
            chapterId = args.hadithId,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToSettings = { navController.navigate(Route.HadithSettings) }
        )
    }

    taggedComposable<Route.HadithByNumber>(ScreenTags.HadithByNumber) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.HadithByNumber>()
        HadithReaderScreen(
            bookId = args.bookId,
            chapterId = "",
            hadithNumber = args.hadithNumber,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToSettings = { navController.navigate(Route.HadithSettings) }
        )
    }

    taggedComposable<Route.HadithByGrade>(ScreenTags.HadithByGrade) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.HadithByGrade>()
        HadithReaderScreen(
            bookId = "",
            chapterId = "",
            grade = HadithGrade.entries.firstOrNull { it.name == args.grade },
            onNavigateBack = { navController.popBackStack() },
            onNavigateToSettings = { navController.navigate(Route.HadithSettings) }
        )
    }

    taggedComposable<Route.HadithSettings>(ScreenTags.HadithSettings) {
        HadithSettingsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    // Dua screens
    taggedComposable<Route.DuaHome>(ScreenTags.DuaHome) {
        AdaptiveDuaScreen(
            onNavigate = { navController.navigate(it) },
            onNavigateBack = { navController.popBackStack() },
            onNavigateToBookmarks = { navController.navigate(Route.AllBookmarks) },
            onNavigateToSearch = { navController.navigate(Route.DuaSearch) },
        )
    }

    taggedComposable<Route.DuaCategory>(ScreenTags.DuaCategory) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.DuaCategory>()
        DuaCategoryScreen(
            categoryId = args.categoryId,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToDua = { duaId ->
                navController.navigate(Route.DuaReader(duaId))
            },
            onNavigateToOccasion = { occasion ->
                navController.navigate(Route.DuaOccasion(occasion.name))
            }
        )
    }

    taggedComposable<Route.DuaOccasion>(ScreenTags.DuaOccasion) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.DuaOccasion>()
        DuaOccasionScreen(
            occasion = DuaOccasion.entries.firstOrNull { it.name == args.occasion }
                ?: DuaOccasion.GENERAL,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToDua = { duaId ->
                navController.navigate(Route.DuaReader(duaId))
            },
        )
    }

    taggedComposable<Route.DuaReader>(ScreenTags.DuaReader) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.DuaReader>()
        DuaReaderScreen(
            duaId = args.duaId,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToSettings = { navController.navigate(Route.DuaSettings) }
        )
    }

    taggedComposable<Route.DuaSettings>(ScreenTags.DuaSettings) {
        DuaSettingsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    taggedComposable<Route.DuaFavorites>(ScreenTags.DuaFavorites) {
        DuasCollectionScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToCategory = { categoryId ->
                navController.navigate(Route.DuaCategory(categoryId))
            },
            onNavigateToBookmarks = { navController.navigate(Route.AllBookmarks) },
            onNavigateToSearch = { navController.navigate(Route.DuaSearch) }
        )
    }

    // Names — three catalogues, one tabbed screen
    taggedComposable<Route.Names>(ScreenTags.Names) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.Names>()
        AdaptiveNamesScreen(
            onNavigate = { navController.navigate(it) },
            initialTab = NamesTab.fromOrdinal(args.tab),
            onNavigateBack = { navController.popBackStack() },
        )
    }

    taggedComposable<Route.Favourites>(ScreenTags.Favourites) {
        FavouritesScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToAsmaUlHusna = {
                navController.navigate(Route.AsmaUlHusnaDetail(it))
            },
            onNavigateToAsmaUnNabi = {
                navController.navigate(Route.AsmaUnNabiDetail(it))
            },
            onNavigateToProphet = { navController.navigate(Route.ProphetDetail(it)) },
        )
    }

    taggedComposable<Route.AsmaUlHusnaDetail>(ScreenTags.AsmaUlHusnaDetail) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.AsmaUlHusnaDetail>()
        AsmaUlHusnaDetailScreen(
            nameId = args.nameId,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    taggedComposable<Route.AsmaUnNabiDetail>(ScreenTags.AsmaUnNabiDetail) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.AsmaUnNabiDetail>()
        AsmaUnNabiDetailScreen(
            nameId = args.nameId,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    taggedComposable<Route.ProphetDetail>(ScreenTags.ProphetDetail) { backStackEntry ->
        val args = backStackEntry.toRoute<Route.ProphetDetail>()
        ProphetDetailScreen(
            prophetId = args.prophetId,
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
