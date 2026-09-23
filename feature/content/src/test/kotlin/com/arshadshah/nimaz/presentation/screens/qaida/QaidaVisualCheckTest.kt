package com.arshadshah.nimaz.presentation.screens.qaida

import com.arshadshah.nimaz.data.qaida.QaidaLearningSettings
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.arshadshah.nimaz.presentation.components.atoms.NimazPatternBackground
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.Gravity
import android.view.WindowManager
import android.graphics.Paint
import android.graphics.Color
import android.view.inspector.WindowInspector
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode
import com.arshadshah.nimaz.presentation.viewmodel.content.*
import com.arshadshah.nimaz.data.audio.QaidaAudioState
import com.arshadshah.nimaz.domain.model.*
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
/** Native previews of every Qaida destination and important state, in both app themes. */
class QaidaVisualCheckTest(private val screen: String, private val theme: ThemeMode) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun screens(): List<Array<Any>> = listOf(
            "journey", "chapters", "review", "review-empty", "audio", "audio-empty",
            "clear-audio", "reset", "settings", "intro", "focus", "repeat", "practise", "self-check",
            "all-cards", "due-review", "reward", "letters", "letter-detail", "loading",
            "downloading", "audio-unavailable", "audio-error", "playback-error", "lesson-empty"
        ).flatMap { name -> listOf(ThemeMode.LIGHT, ThemeMode.DARK).map { arrayOf<Any>(name, it) } }
    }

    @get:Rule val rule = createComponentComposeRule()
    private var root: View? = null
    private val lesson = qaidaLessonContent(4, "Short vowels", listOf(
        qaidaCell(41, lessonId = 4, textArabic = "بَ", transliteration = "ba"),
        qaidaCell(42, lessonId = 4, textArabic = "بِ", transliteration = "bi"),
        qaidaCell(43, lessonId = 4, textArabic = "بُ", transliteration = "bu")), instructionEnglish = null).let {
            it.copy(lesson = it.lesson.copy(description = "Learn the short vowel sounds (Harakaat).", titleArabic = "الحَرَكَات"))
        }
    private val preferences = MutableStateFlow(QaidaLearningSettings())
    private val vm: QaidaReaderViewModel = mockk(relaxed = true) {
        every { settings } returns preferences
        every { letters } returns MutableStateFlow(previewLetters())
        every { courseProgress } returns MutableStateFlow(qaidaCourse(listOf("The Letters", "Joined Letters", "Disjoined Letters", "Harakat (Short Vowels)", "Tanween", "Harakat & Tanween Drills", "Standing Harakat", "Madd & Leen Letters", "Madd, Leen & Tanween Drills", "Sukoon (Jazm)", "Sukoon Drills", "Shadda (Tashdeed)", "Shadda Drills", "Shadda with Sukoon", "Words with Two Shaddas", "Madd, Shadda & Sukoon Together", "Comprehensive Revision").mapIndexed { index, title ->
            qaidaLessonState(index + 1, title, if (index < 3) LessonStatus.COMPLETED else if (index == 3) LessonStatus.IN_PROGRESS else LessonStatus.LOCKED,
                if (index < 3) 2 else 0, if (index < 3) 20 else 0, 20)
        },3,6,4))
        every { dueLessons } returns MutableStateFlow(emptySet())
        every { todayCount } returns MutableStateFlow(3)
        every { cacheBytes } returns MutableStateFlow(0L)
        every { lessonContent } returns MutableStateFlow(lesson)
        every { selectedLessonId } returns MutableStateFlow<Int?>(4)
        every { sessionHeard } returns MutableStateFlow(emptySet())
        every { completedCellIds } returns MutableStateFlow(emptySet())
        every { download } returns MutableStateFlow(QaidaDownloadState(ready = true))
        every { audioState } returns MutableStateFlow(QaidaAudioState())
        every { resumeCell(any()) } returns null
        every { dueCells(any()) } returns emptySet()
    }
    @Composable
    private fun PreviewTheme(content: @Composable () -> Unit) {
        NimazTheme(themeMode = theme) { NimazPatternBackground(Modifier.fillMaxSize()) { content() } }
    }
    private fun capture() {
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        rule.runOnIdle {
            val view = root!!.rootView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            // Sheets and confirmation dialogs have their own Android window.
            WindowInspector.getGlobalWindowViews().filter { it !== view && it.isShown }.forEach { overlay ->
                val location = IntArray(2)
                overlay.getLocationOnScreen(location)
                val params = overlay.layoutParams as? WindowManager.LayoutParams
                if (params != null && params.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0) {
                    canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(),
                        Paint().apply { color = Color.BLACK; alpha = (params.dimAmount * 255).toInt() })
                }
                // Robolectric does not position separate windows using WindowManager gravity.
                val x = if (params != null && params.gravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.CENTER_HORIZONTAL)
                    (view.width - overlay.width) / 2f else location[0].toFloat()
                val y = if (params != null && params.gravity and Gravity.VERTICAL_GRAVITY_MASK == Gravity.CENTER_VERTICAL)
                    (view.height - overlay.height) / 2f else location[1].toFloat()
                canvas.save()
                canvas.translate(x, y)
                overlay.draw(canvas)
                canvas.restore()
            }
            val directory = File(System.getenv("QAIDA_PREVIEW_DIR") ?: "build/qaida-previews")
            directory.mkdirs()
            val suffix = if (theme == ThemeMode.DARK) "-dark" else ""
            File(directory, "$screen$suffix.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            check(bitmap.width > 0 && bitmap.height > 0)
        }
    }
    private fun tap(text: String) = rule.onNodeWithText(text).performScrollTo().performClick()
    private fun home() {
        rule.setContent { root = LocalView.current; PreviewTheme { QaidaHomeScreen({}, {}, {}, vm) } }
    }
    private fun reader() {
        rule.setContent { root = LocalView.current; PreviewTheme { QaidaReaderScreen(4, {}, vm) } }
    }
    @Test fun render() {
        when (screen) {
            "journey", "chapters", "review", "review-empty", "audio", "audio-empty", "clear-audio", "reset", "settings" -> {
                if (screen == "review") every { vm.dueLessons } returns MutableStateFlow(setOf(1, 4))
                if (screen == "audio" || screen == "clear-audio") every { vm.cacheBytes } returns MutableStateFlow(8L * 1024 * 1024)
                home()
                when (screen) {
                    "chapters" -> { tap("Letters & sounds"); rule.onNodeWithText("1 · The Letters").performScrollTo() }
                    "review", "review-empty" -> rule.onNodeWithText("Review").performClick()
                    "audio", "audio-empty", "clear-audio" -> {
                        rule.onNodeWithText("Audio").performClick()
                        if (screen == "clear-audio") tap("Remove downloaded audio")
                    }
                    "settings", "reset" -> {
                        rule.onNodeWithContentDescription("Qaida settings").performClick()
                        if (screen == "reset") tap("Reset journey")
                    }
                }
            }
            "letters", "letter-detail" -> {
                rule.setContent { root = LocalView.current; PreviewTheme { QaidaLettersScreen({}, vm) } }
                if (screen == "letter-detail") rule.onNodeWithText("ب").performClick()
            }
            else -> {
                when (screen) {
                    "loading" -> every { vm.lessonContent } returns MutableStateFlow<QaidaLessonContent?>(null)
                    "lesson-empty" -> every { vm.lessonContent } returns MutableStateFlow<QaidaLessonContent?>(lesson.copy(lines = emptyList()))
                    "downloading" -> every { vm.download } returns MutableStateFlow(QaidaDownloadState(loading = true, completed = 1, total = 3))
                    "audio-unavailable" -> every { vm.download } returns MutableStateFlow(QaidaDownloadState())
                    "audio-error" -> every { vm.download } returns MutableStateFlow(QaidaDownloadState(failed = true))
                    "playback-error" -> every { vm.audioState } returns MutableStateFlow(QaidaAudioState(error = "Preview playback failure"))
                    "due-review" -> every { vm.dueCells(4) } returns setOf(42)
                }
                reader()
                when (screen) {
                    "intro", "loading", "lesson-empty" -> Unit
                    "downloading", "audio-unavailable", "audio-error", "playback-error" -> {
                        rule.onNodeWithText(when (screen) {
                            "downloading" -> "Saving audio · 1 of 3"
                            "audio-unavailable" -> "Recordings are not available yet. You can still read and practise with your teacher."
                            "audio-error" -> "Couldn’t save this lesson’s audio. Check your connection and try again, or keep practising."
                            else -> "This recording couldn’t play. Try downloading the lesson again."
                        }).performScrollTo()
                    }
                    "due-review" -> tap("Review this lesson")
                    else -> {
                        tap("Begin practice")
                        when (screen) {
                            "repeat" -> tap("Repeat")
                            "practise", "self-check", "reward" -> {
                                tap("Practise")
                                if (screen == "self-check") tap("Show reminder")
                                if (screen == "reward") repeat(3) { tap("Show reminder"); tap("Feeling confident") }
                            }
                            "all-cards" -> tap("All")
                        }
                    }
                }
            }
        }
        capture()
    }

    private fun previewLetters(): List<QaidaLetter> = listOf(
        QaidaLetter(id = 1, letterArabic = "ا", nameArabic = "أَلِف", nameTransliteration = "alif", isolatedForm = "ا", initialForm = null, medialForm = null, finalForm = "ـا", isConnecting = false, makhrajArea = MakhrajArea.JAWF, makhrajDetail = "Jawf (the empty space of the mouth and throat); a carrier/elongation with no fixed contact point.", phoneticHint = "like the long 'a' in 'father' (when used for elongation)", audioKey = "letter_alif", audioPath = "", displayOrder = 1),
        QaidaLetter(id = 2, letterArabic = "ب", nameArabic = "بَاء", nameTransliteration = "baa", isolatedForm = "ب", initialForm = "بـ", medialForm = "ـبـ", finalForm = "ـب", isConnecting = true, makhrajArea = MakhrajArea.SHAFATAIN, makhrajDetail = "The inner part of both lips pressed together.", phoneticHint = "like 'b' in 'book'", audioKey = "letter_ba", audioPath = "", displayOrder = 2),
        QaidaLetter(id = 3, letterArabic = "ت", nameArabic = "تَاء", nameTransliteration = "taa", isolatedForm = "ت", initialForm = "تـ", medialForm = "ـتـ", finalForm = "ـت", isConnecting = true, makhrajArea = MakhrajArea.LISAN, makhrajDetail = "Tip of the tongue against the roots (gums) of the upper front teeth.", phoneticHint = "like 't' in 'table'", audioKey = "letter_ta", audioPath = "", displayOrder = 3),
        QaidaLetter(id = 4, letterArabic = "ث", nameArabic = "ثَاء", nameTransliteration = "thaa", isolatedForm = "ث", initialForm = "ثـ", medialForm = "ـثـ", finalForm = "ـث", isConnecting = true, makhrajArea = MakhrajArea.LISAN, makhrajDetail = "Tip of the tongue lightly touching the edges of the upper front teeth.", phoneticHint = "like 'th' in 'think'", audioKey = "letter_tha", audioPath = "", displayOrder = 4),
        QaidaLetter(id = 5, letterArabic = "ج", nameArabic = "جِيم", nameTransliteration = "jeem", isolatedForm = "ج", initialForm = "جـ", medialForm = "ـجـ", finalForm = "ـج", isConnecting = true, makhrajArea = MakhrajArea.LISAN, makhrajDetail = "Middle of the tongue against the roof of the mouth (hard palate).", phoneticHint = "like 'j' in 'jam'", audioKey = "letter_jim", audioPath = "", displayOrder = 5),
        QaidaLetter(id = 6, letterArabic = "ح", nameArabic = "حَاء", nameTransliteration = "Haa", isolatedForm = "ح", initialForm = "حـ", medialForm = "ـحـ", finalForm = "ـح", isConnecting = true, makhrajArea = MakhrajArea.HALQ, makhrajDetail = "Middle of the throat (heavy, whispered).", phoneticHint = "a deep, breathy 'h' from the middle of the throat (no English equivalent)", audioKey = "letter_hha", audioPath = "", displayOrder = 6),
        QaidaLetter(id = 7, letterArabic = "خ", nameArabic = "خَاء", nameTransliteration = "khaa", isolatedForm = "خ", initialForm = "خـ", medialForm = "ـخـ", finalForm = "ـخ", isConnecting = true, makhrajArea = MakhrajArea.HALQ, makhrajDetail = "Nearest (upper) part of the throat.", phoneticHint = "like 'ch' in the Scottish 'loch'", audioKey = "letter_kha", audioPath = "", displayOrder = 7),
        QaidaLetter(id = 8, letterArabic = "د", nameArabic = "دَال", nameTransliteration = "daal", isolatedForm = "د", initialForm = null, medialForm = null, finalForm = "ـد", isConnecting = false, makhrajArea = MakhrajArea.LISAN, makhrajDetail = "Tip of the tongue against the roots of the upper front teeth.", phoneticHint = "like 'd' in 'door'", audioKey = "letter_dal", audioPath = "", displayOrder = 8),
        QaidaLetter(id = 9, letterArabic = "ذ", nameArabic = "ذَال", nameTransliteration = "dhaal", isolatedForm = "ذ", initialForm = null, medialForm = null, finalForm = "ـذ", isConnecting = false, makhrajArea = MakhrajArea.LISAN, makhrajDetail = "Tip of the tongue lightly touching the edges of the upper front teeth.", phoneticHint = "like 'th' in 'this'", audioKey = "letter_dhal", audioPath = "", displayOrder = 9),
        QaidaLetter(id = 10, letterArabic = "ر", nameArabic = "رَاء", nameTransliteration = "raa", isolatedForm = "ر", initialForm = null, medialForm = null, finalForm = "ـر", isConnecting = false, makhrajArea = MakhrajArea.LISAN, makhrajDetail = "Tip of the tongue (slightly back) against the gum, with a light trill.", phoneticHint = "a lightly rolled 'r'", audioKey = "letter_ra", audioPath = "", displayOrder = 10),
        QaidaLetter(id = 11, letterArabic = "ز", nameArabic = "زَاي", nameTransliteration = "zaay", isolatedForm = "ز", initialForm = null, medialForm = null, finalForm = "ـز", isConnecting = false, makhrajArea = MakhrajArea.LISAN, makhrajDetail = "Tip of the tongue near the back of the lower front teeth.", phoneticHint = "like 'z' in 'zebra'", audioKey = "letter_za", audioPath = "", displayOrder = 11),
        QaidaLetter(id = 12, letterArabic = "س", nameArabic = "سِين", nameTransliteration = "seen", isolatedForm = "س", initialForm = "سـ", medialForm = "ـسـ", finalForm = "ـس", isConnecting = true, makhrajArea = MakhrajArea.LISAN, makhrajDetail = "Tip of the tongue near the back of the lower front teeth.", phoneticHint = "like 's' in 'sun'", audioKey = "letter_sin", audioPath = "", displayOrder = 12),
        QaidaLetter(id = 13, letterArabic = "ش", nameArabic = "شِين", nameTransliteration = "sheen", isolatedForm = "ش", initialForm = "شـ", medialForm = "ـشـ", finalForm = "ـش", isConnecting = true, makhrajArea = MakhrajArea.LISAN, makhrajDetail = "Middle of the tongue against the hard palate, with air spread.", phoneticHint = "like 'sh' in 'ship'", audioKey = "letter_shin", audioPath = "", displayOrder = 13),
        QaidaLetter(id = 14, letterArabic = "ص", nameArabic = "صَاد", nameTransliteration = "Saad", isolatedForm = "ص", initialForm = "صـ", medialForm = "ـصـ", finalForm = "ـص", isConnecting = true, makhrajArea = MakhrajArea.LISAN, makhrajDetail = "Tip of the tongue near the back of the lower front teeth (heavy/emphatic).", phoneticHint = "a heavy, emphatic 's'", audioKey = "letter_sad", audioPath = "", displayOrder = 14),
        QaidaLetter(id = 15, letterArabic = "ض", nameArabic = "ضَاد", nameTransliteration = "Daad", isolatedForm = "ض", initialForm = "ضـ", medialForm = "ـضـ", finalForm = "ـض", isConnecting = true, makhrajArea = MakhrajArea.LISAN, makhrajDetail = "One or both sides of the tongue against the upper molars (unique to Arabic).", phoneticHint = "a heavy, emphatic 'd'", audioKey = "letter_dad", audioPath = "", displayOrder = 15),
        QaidaLetter(id = 16, letterArabic = "ط", nameArabic = "طَاء", nameTransliteration = "Taa", isolatedForm = "ط", initialForm = "طـ", medialForm = "ـطـ", finalForm = "ـط", isConnecting = true, makhrajArea = MakhrajArea.LISAN, makhrajDetail = "Tip of the tongue against the roots of the upper front teeth (heavy/emphatic).", phoneticHint = "a heavy, emphatic 't'", audioKey = "letter_tta", audioPath = "", displayOrder = 16),
        QaidaLetter(id = 17, letterArabic = "ظ", nameArabic = "ظَاء", nameTransliteration = "Zaa", isolatedForm = "ظ", initialForm = "ظـ", medialForm = "ـظـ", finalForm = "ـظ", isConnecting = true, makhrajArea = MakhrajArea.LISAN, makhrajDetail = "Tip of the tongue lightly touching the edges of the upper front teeth (heavy/emphatic).", phoneticHint = "a heavy, emphatic 'th'/'dh'", audioKey = "letter_zha", audioPath = "", displayOrder = 17),
        QaidaLetter(id = 18, letterArabic = "ع", nameArabic = "عَيْن", nameTransliteration = "ayn", isolatedForm = "ع", initialForm = "عـ", medialForm = "ـعـ", finalForm = "ـع", isConnecting = true, makhrajArea = MakhrajArea.HALQ, makhrajDetail = "Middle of the throat.", phoneticHint = "a deep throat sound made by tightening the middle of the throat (no English equivalent)", audioKey = "letter_ain", audioPath = "", displayOrder = 18),
        QaidaLetter(id = 19, letterArabic = "غ", nameArabic = "غَيْن", nameTransliteration = "ghayn", isolatedForm = "غ", initialForm = "غـ", medialForm = "ـغـ", finalForm = "ـغ", isConnecting = true, makhrajArea = MakhrajArea.HALQ, makhrajDetail = "Nearest (upper) part of the throat.", phoneticHint = "like a gargled French 'r'", audioKey = "letter_ghain", audioPath = "", displayOrder = 19),
        QaidaLetter(id = 20, letterArabic = "ف", nameArabic = "فَاء", nameTransliteration = "faa", isolatedForm = "ف", initialForm = "فـ", medialForm = "ـفـ", finalForm = "ـف", isConnecting = true, makhrajArea = MakhrajArea.SHAFATAIN, makhrajDetail = "Inner edge of the upper front teeth against the inside of the lower lip.", phoneticHint = "like 'f' in 'fish'", audioKey = "letter_fa", audioPath = "", displayOrder = 20),
        QaidaLetter(id = 21, letterArabic = "ق", nameArabic = "قَاف", nameTransliteration = "qaaf", isolatedForm = "ق", initialForm = "قـ", medialForm = "ـقـ", finalForm = "ـق", isConnecting = true, makhrajArea = MakhrajArea.LISAN, makhrajDetail = "Deepest part of the tongue (its root) against the soft palate.", phoneticHint = "a deep 'k' produced from the back of the throat", audioKey = "letter_qaf", audioPath = "", displayOrder = 21),
        QaidaLetter(id = 22, letterArabic = "ك", nameArabic = "كَاف", nameTransliteration = "kaaf", isolatedForm = "ك", initialForm = "كـ", medialForm = "ـكـ", finalForm = "ـك", isConnecting = true, makhrajArea = MakhrajArea.LISAN, makhrajDetail = "Back of the tongue against the palate, just below the qaaf point.", phoneticHint = "like 'k' in 'kite'", audioKey = "letter_kaf", audioPath = "", displayOrder = 22),
        QaidaLetter(id = 23, letterArabic = "ل", nameArabic = "لَام", nameTransliteration = "laam", isolatedForm = "ل", initialForm = "لـ", medialForm = "ـلـ", finalForm = "ـل", isConnecting = true, makhrajArea = MakhrajArea.LISAN, makhrajDetail = "Tip and sides of the tongue against the gums of the upper front teeth.", phoneticHint = "like 'l' in 'lamp'", audioKey = "letter_lam", audioPath = "", displayOrder = 23),
        QaidaLetter(id = 24, letterArabic = "م", nameArabic = "مِيم", nameTransliteration = "meem", isolatedForm = "م", initialForm = "مـ", medialForm = "ـمـ", finalForm = "ـم", isConnecting = true, makhrajArea = MakhrajArea.SHAFATAIN, makhrajDetail = "Both lips pressed together (with nasal ghunnah from the khayshum).", phoneticHint = "like 'm' in 'moon'", audioKey = "letter_mim", audioPath = "", displayOrder = 24),
        QaidaLetter(id = 25, letterArabic = "ن", nameArabic = "نُون", nameTransliteration = "noon", isolatedForm = "ن", initialForm = "نـ", medialForm = "ـنـ", finalForm = "ـن", isConnecting = true, makhrajArea = MakhrajArea.LISAN, makhrajDetail = "Tip of the tongue against the gum of the upper front teeth (with nasal ghunnah from the khayshum).", phoneticHint = "like 'n' in 'noon'", audioKey = "letter_nun", audioPath = "", displayOrder = 25),
        QaidaLetter(id = 26, letterArabic = "ه", nameArabic = "هَاء", nameTransliteration = "haa", isolatedForm = "ه", initialForm = "هـ", medialForm = "ـهـ", finalForm = "ـه", isConnecting = true, makhrajArea = MakhrajArea.HALQ, makhrajDetail = "Deepest (lowest) part of the throat.", phoneticHint = "like 'h' in 'house'", audioKey = "letter_ha", audioPath = "", displayOrder = 26),
        QaidaLetter(id = 27, letterArabic = "و", nameArabic = "وَاو", nameTransliteration = "waw", isolatedForm = "و", initialForm = null, medialForm = null, finalForm = "ـو", isConnecting = false, makhrajArea = MakhrajArea.SHAFATAIN, makhrajDetail = "Rounding of both lips.", phoneticHint = "like 'w' in 'water' (or long 'oo' when used for elongation)", audioKey = "letter_waw", audioPath = "", displayOrder = 27),
        QaidaLetter(id = 28, letterArabic = "ي", nameArabic = "يَاء", nameTransliteration = "yaa", isolatedForm = "ي", initialForm = "يـ", medialForm = "ـيـ", finalForm = "ـي", isConnecting = true, makhrajArea = MakhrajArea.LISAN, makhrajDetail = "Middle of the tongue against the hard palate.", phoneticHint = "like 'y' in 'yes' (or long 'ee' when used for elongation)", audioKey = "letter_ya", audioPath = "", displayOrder = 28),
        QaidaLetter(id = 29, letterArabic = "ء", nameArabic = "هَمْزَة", nameTransliteration = "hamzah", isolatedForm = "ء", initialForm = null, medialForm = null, finalForm = null, isConnecting = false, makhrajArea = MakhrajArea.HALQ, makhrajDetail = "Deepest (lowest) part of the throat (a glottal stop).", phoneticHint = "a glottal catch, like the break in 'uh-oh'", audioKey = "letter_hamza", audioPath = "", displayOrder = 29),
    )
}
