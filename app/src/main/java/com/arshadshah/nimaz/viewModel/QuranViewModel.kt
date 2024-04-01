package com.arshadshah.nimaz.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.FULL_QURAN_DOWNLOADED
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.data.local.models.LocalJuz
import com.arshadshah.nimaz.data.local.models.LocalSurah
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.QuranUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class QuranViewModel(private val sharedPreferences: PrivateSharedPreferences) : ViewModel() {

    //general state for error and loading
    private val _errorState = MutableStateFlow("")
    val errorState = _errorState.asStateFlow()
    private val _loadingState = MutableStateFlow(false)
    val loadingState = _loadingState.asStateFlow()


    //surah list state
    private var _surahListState = MutableStateFlow(ArrayList<LocalSurah>(114))
    val surahListState = _surahListState.asStateFlow()

    //juz list state
    private var _juzListState = MutableStateFlow(ArrayList<LocalJuz>(30))
    val juzListState = _juzListState.asStateFlow()

    private val _ayaListState = MutableStateFlow(ArrayList<LocalAya>(100))
    val ayaListState = _ayaListState.asStateFlow()

    private val _currentAyatScreenNumber = MutableStateFlow(1)
    val currentAyatScreenNumber = _currentAyatScreenNumber.asStateFlow()

    //state for quran menu features like page display, font size, font type, etc
    private val _arabic_Font_size = MutableStateFlow(26.0f)
    val arabic_Font_size = _arabic_Font_size.asStateFlow()

    private val _arabic_Font = MutableStateFlow("Default")
    val arabic_Font = _arabic_Font.asStateFlow()

    private val _translation_Font_size = MutableStateFlow(16.0f)
    val translation_Font_size = _translation_Font_size.asStateFlow()

    private val _translation = MutableStateFlow("English")
    val translation = _translation.asStateFlow()

    private val _display_Mode = MutableStateFlow("List")
    val display_Mode = _display_Mode.asStateFlow()

    private val _surahState = MutableStateFlow(
        LocalSurah(0, 0, 0, "", "", "", "", 0, 0)
    )
    val surahState = _surahState.asStateFlow()

    //_scrollToAya
    private val _scrollToAya = MutableStateFlow<LocalAya?>(null)
    val scrollToAya = _scrollToAya.asStateFlow()

    init {
        getSurahList()
        getJuzList()
    }


    //events for quran menu features like page display, font size, font type, etc
    sealed class QuranMenuEvents {

        //change arabic font
        data class Change_Arabic_Font(val font: String) : QuranMenuEvents()

        //change translation font
        data class Change_Translation(val lang: String) : QuranMenuEvents()
        data class Change_Arabic_Font_Size(val size: Float) : QuranMenuEvents()

        //change translation font size
        data class Change_Translation_Font_Size(val size: Float) : QuranMenuEvents()

        //change display mode
        data class Change_Display_Mode(val mode: String) : QuranMenuEvents()

        //initialize quran using settings
        object Initialize_Quran : QuranMenuEvents()

        //scroll to aya
        data class Scroll_To_Aya(val aya: LocalAya?) : QuranMenuEvents()

        //reset quran data
        object Reset_Quran_Data : QuranMenuEvents()
    }

    fun handleQuranMenuEvents(event: QuranMenuEvents) {
        when (event) {
            is QuranMenuEvents.Change_Arabic_Font -> {
                _arabic_Font.value = event.font
            }

            is QuranMenuEvents.Change_Translation -> {
                _translation.value = event.lang
            }

            is QuranMenuEvents.Change_Arabic_Font_Size -> {
                _arabic_Font_size.value = event.size
            }

            is QuranMenuEvents.Change_Translation_Font_Size -> {
                _translation_Font_size.value = event.size
            }

            is QuranMenuEvents.Change_Display_Mode -> {
                _display_Mode.value = event.mode
            }

            is QuranMenuEvents.Initialize_Quran -> {
                _arabic_Font.value = sharedPreferences.getData(AppConstants.FONT_STYLE, "Default")
                _translation.value =
                    sharedPreferences.getData(AppConstants.TRANSLATION_LANGUAGE, "English")

                //if the font size is not set, set it to default 26 and 16

                _arabic_Font_size.value =
                    if (sharedPreferences.getDataFloat(AppConstants.ARABIC_FONT_SIZE) == 0.0f) {
                        //save the default font size and also return it
                        sharedPreferences.saveDataFloat(AppConstants.ARABIC_FONT_SIZE, 26.0f)
                        26.0f
                    } else {
                        sharedPreferences.getDataFloat(AppConstants.ARABIC_FONT_SIZE)
                    }
                _translation_Font_size.value =
                    if (sharedPreferences.getDataFloat(AppConstants.TRANSLATION_FONT_SIZE) == 0.0f) {
                        //save the default font size and also return it
                        sharedPreferences.saveDataFloat(AppConstants.TRANSLATION_FONT_SIZE, 16.0f)
                        16.0f
                    } else {
                        sharedPreferences.getDataFloat(AppConstants.TRANSLATION_FONT_SIZE)
                    }

                _display_Mode.value = sharedPreferences.getData(AppConstants.PAGE_TYPE, "List")
            }

            is QuranMenuEvents.Scroll_To_Aya -> {
                _scrollToAya.value = event.aya
            }

            is QuranMenuEvents.Reset_Quran_Data -> {
                //reset quran data
                _arabic_Font.value = "Default"
                sharedPreferences.saveData(AppConstants.FONT_STYLE, "Default")
                _translation.value = "English"
                sharedPreferences.saveData(AppConstants.TRANSLATION_LANGUAGE, "English")
                _arabic_Font_size.value = 26.0f
                sharedPreferences.saveDataFloat(AppConstants.ARABIC_FONT_SIZE, 26.0f)
                _translation_Font_size.value = 16.0f
                sharedPreferences.saveDataFloat(AppConstants.TRANSLATION_FONT_SIZE, 16.0f)
                _display_Mode.value = "List"
                sharedPreferences.saveData(AppConstants.PAGE_TYPE, "List")
                sharedPreferences.saveDataBoolean(FULL_QURAN_DOWNLOADED, false)
                deleteAllAyat()
            }
        }
    }

    //deleteAllAyat
    private fun deleteAllAyat() {
        viewModelScope.launch(Dispatchers.IO) {
            _errorState.value = ""
            try {
                val dataStore = LocalDataStore.getDataStore()
                dataStore.deleteAllAyat()
            } catch (e: Exception) {
                _errorState.value = e.message.toString()
            }
        }
    }

    private fun getSurahList() {
        viewModelScope.launch(Dispatchers.IO) {
            _loadingState.value = true
            _errorState.value = ""
            try {
                val dataStore = LocalDataStore.getDataStore()
                val surahList = dataStore.getAllSurah().toMutableList() as ArrayList<LocalSurah>
                _surahListState.value = surahList
                _loadingState.value = false
                _errorState.value = ""
            } catch (e: Exception) {
                _surahListState.value = ArrayList()
                _loadingState.value = false
                _errorState.value = e.message!!
            }
        }
    }

    private fun getJuzList() {
        viewModelScope.launch(Dispatchers.IO) {
            _loadingState.value = true
            _errorState.value = ""
            try {
                val dataStore = LocalDataStore.getDataStore()
                val juzList = dataStore.getAllJuz().toMutableList() as ArrayList<LocalJuz>
                _juzListState.value = juzList
                _loadingState.value = false
                _errorState.value = ""
            } catch (e: Exception) {
                _juzListState.value = ArrayList()
                _loadingState.value = false
                _errorState.value = e.message!!
            }
        }
    }

    fun getAllAyaForSurah(surahNumber: Int, language: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _loadingState.value = true
                _errorState.value = ""
                _ayaListState.value = ArrayList()
                val dataStore = LocalDataStore.getDataStore()
                val languageConverted = language.uppercase(Locale.ROOT)
                val ayatList = dataStore.getAyasOfSurah(surahNumber)
                val newList =
                    addBismillahToFirstAya(
                        ayatList as ArrayList<LocalAya>,
                        languageConverted,
                        surahNumber
                    )
                _ayaListState.value = newList
                _loadingState.value = false
                _errorState.value = ""
            } catch (e: Exception) {
                _ayaListState.value = ArrayList()
                _loadingState.value = false
                _errorState.value = e.message!!
            }
        }
    }

    private fun addBismillahToFirstAya(
        surahAyatList: ArrayList<LocalAya>,
        languageConverted: String,
        surahNumber: Int,
    ): ArrayList<LocalAya> {
        //an empty number
        val ayaNumberOfBismillah = 0
        val ayaOfBismillah = when (languageConverted) {
            "ENGLISH" -> "In the name of Allah, the Entirely Merciful, the Especially Merciful"
            "URDU" -> "اللہ کے نام سے جو رحمان و رحیم ہے"

            else -> {
                "In the name of Allah, the Entirely Merciful, the Especially Merciful"
            }
        }
        val ayaArabicOfBismillah = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"
        val aya: LocalAya
        if (languageConverted == "ENGLISH") {
            aya = LocalAya(
                0,
                ayaArabicOfBismillah,
                ayaOfBismillah,
                "",
                surahNumber,
                0,
                false,
                false,
                "",
                "",
                false,
                "",
                0,
                0,
            )
        } else {
            aya = LocalAya(
                0,
                ayaArabicOfBismillah,
                ayaOfBismillah,
                ayaOfBismillah,
                surahNumber,
                1,
                false,
                false,
                "",
                "",
                false,
                "",
                0,
                0,
            )
        }
        //first check if an object like this is already in the list
        //check all the attributes of the object bisimillah with the attributes of the object in the list at index 0
        if (surahAyatList[0].ayaArabic != ayaArabicOfBismillah && surahAyatList[0].suraNumber != 1) {
            if (surahNumber != 9) {
                surahAyatList.add(0, aya)
            }
        }

        return QuranUtils.processAyaEnd(surahAyatList)
    }

    fun getAllAyaForJuz(juzNumber: Int, language: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loadingState.value = true
            _errorState.value = ""
            _ayaListState.value = ArrayList()
            try {
                val dataStore = LocalDataStore.getDataStore()
                val languageConverted = language.uppercase(Locale.ROOT)
                val listOfJuzAyat =
                    dataStore.getAyasOfJuz(juzNumber) as ArrayList<LocalAya>
                val newList = addBismillahInJuz(juzNumber, languageConverted, listOfJuzAyat)
                _ayaListState.value = newList
                _loadingState.value = false
                _errorState.value = ""

            } catch (e: Exception) {
                _ayaListState.value = ArrayList()
                _loadingState.value = false
                _errorState.value = e.message!!
            }
        }
    }

    //function to add biismillah to the start of every surah
    private fun addBismillahInJuz(
        juzNumber: Int,
        languageConverted: String,
        listOfJuzAyat: ArrayList<LocalAya>,
    ): ArrayList<LocalAya> {

        val ayaOfBismillah = when (languageConverted) {
            "ENGLISH" -> "In the name of Allah, the Entirely Merciful, the Especially Merciful"
            "URDU" -> "اللہ کے نام سے جو رحمان و رحیم ہے"

            else -> {
                "In the name of Allah, the Entirely Merciful, the Especially Merciful"
            }
        }
        val ayaArabicOfBismillah = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"

        //create a map of the aya of bismillah
        var aya: LocalAya
        //find all the objects in arraylist ayaForJuz where ayaForJuz[i]!!.ayaNumber = 1
        //add object bismillah before it for every occurance of ayaForJuz[i]!!.ayaNumber = 1
        var index = 0
        while (index < listOfJuzAyat.size) {
            if (listOfJuzAyat[index].ayaNumberInSurah == 1 && listOfJuzAyat[index].suraNumber != 1) {
                //add bismillah before ayaForJuz[i]
                if (listOfJuzAyat[index].ayaNumberInSurah == 1) {
                    if (juzNumber + 1 != 10 && index != 36) {
                        if (languageConverted == "ENGLISH") {
                            aya = LocalAya(
                                0,
                                ayaArabicOfBismillah,
                                ayaOfBismillah,
                                "",
                                listOfJuzAyat[index].suraNumber,
                                0,
                                false,
                                false,
                                "",
                                "",
                                false,
                                "",
                                0,
                                0,
                            )
                        } else {
                            aya = LocalAya(
                                0,
                                ayaArabicOfBismillah,
                                "",
                                ayaOfBismillah,
                                listOfJuzAyat[index].suraNumber,
                                1,
                                false,
                                false,
                                "",
                                "",
                                false,
                                "",
                                0,
                                0,
                            )
                        }
                        //add the map of bismillah to ayaList at the current index
                        listOfJuzAyat.add(index, aya)
                        //skip the next iteration
                        index++
                    }
                }
            }
            index++
        }

        return QuranUtils.processAyaEnd(listOfJuzAyat)
    }

    //events to bookmark an aya, favorite an aya, add a note to an aya
    sealed class AyaEvent {

        data class BookmarkAya(
            val ayaNumber: Int,
            val surahNumber: Int,
            val ayaNumberInSurah: Int,
            val bookmark: Boolean,
        ) : AyaEvent()

        data class FavoriteAya(
            val ayaNumber: Int,
            val surahNumber: Int,
            val ayaNumberInSurah: Int,
            val favorite: Boolean,
        ) : AyaEvent()

        data class AddNoteToAya(
            val ayaNumber: Int,
            val surahNumber: Int,
            val ayaNumberInSurah: Int,
            val note: String,
        ) : AyaEvent()

        data class getNoteForAya(
            val ayaNumber: Int,
            val surahNumber: Int,
            val ayaNumberInSurah: Int,
        ) : AyaEvent()

        //get all bookmarks
        object getBookmarks : AyaEvent()

        //get all favorites
        object getFavorites : AyaEvent()

        //get all notes
        object getNotes : AyaEvent()

        //addAudioToAya
        class addAudioToAya(
            val surahNumber: Int,
            val ayaNumberInSurah: Int,
            val audio: String,
        ) : AyaEvent()

        //delete a note from an aya
        class deleteNoteFromAya(
            val ayaNumber: Int,
            val surahNumber: Int,
            val ayaNumberInSurah: Int,
        ) : AyaEvent()

        //delete a bookmark from an aya
        class deleteBookmarkFromAya(
            val ayaNumber: Int,
            val surahNumber: Int,
            val ayaNumberInSurah: Int,
        ) : AyaEvent()

        //delete a favorite from an aya
        class deleteFavoriteFromAya(
            val ayaNumber: Int,
            val surahNumber: Int,
            val ayaNumberInSurah: Int,
        ) : AyaEvent()

        class getSurahById(val id: Int) : AyaEvent()

    }

    //events handler
    fun handleAyaEvent(ayaEvent: AyaEvent) {
        when (ayaEvent) {
            is AyaEvent.BookmarkAya -> {
                bookmarkAya(
                    ayaEvent.ayaNumber,
                    ayaEvent.surahNumber,
                    ayaEvent.ayaNumberInSurah,
                    ayaEvent.bookmark
                )
            }

            is AyaEvent.FavoriteAya -> {
                favoriteAya(
                    ayaEvent.ayaNumber,
                    ayaEvent.surahNumber,
                    ayaEvent.ayaNumberInSurah,
                    ayaEvent.favorite
                )
            }

            is AyaEvent.AddNoteToAya -> {
                addNoteToAya(
                    ayaEvent.ayaNumber,
                    ayaEvent.surahNumber,
                    ayaEvent.ayaNumberInSurah,
                    ayaEvent.note
                )
            }

            is AyaEvent.getNoteForAya -> {
                getNoteForAya(ayaEvent.ayaNumber, ayaEvent.surahNumber, ayaEvent.ayaNumberInSurah)
            }

            is AyaEvent.getBookmarks -> {
                getAllBookmarks()
            }

            is AyaEvent.getFavorites -> {
                getAllFavorites()
            }

            is AyaEvent.getNotes -> {
                getAllNotes()
            }

            is AyaEvent.addAudioToAya -> {
                addAudioToAya(
                    ayaEvent.surahNumber,
                    ayaEvent.ayaNumberInSurah,
                    ayaEvent.audio
                )
            }

            is AyaEvent.deleteNoteFromAya -> {
                deleteNoteFromAya(
                    ayaEvent.ayaNumber,
                    ayaEvent.surahNumber,
                    ayaEvent.ayaNumberInSurah
                )
            }

            is AyaEvent.deleteBookmarkFromAya -> {
                deleteBookmarkFromAya(
                    ayaEvent.ayaNumber,
                    ayaEvent.surahNumber,
                    ayaEvent.ayaNumberInSurah
                )
            }

            is AyaEvent.deleteFavoriteFromAya -> {
                deleteFavoriteFromAya(
                    ayaEvent.ayaNumber,
                    ayaEvent.surahNumber,
                    ayaEvent.ayaNumberInSurah
                )
            }

            is AyaEvent.getSurahById -> {
                getSurahById(ayaEvent.id)
            }
        }
    }

    //add audio to aya
    private fun addAudioToAya(
        surahNumber: Int,
        ayaNumberInSurah: Int,
        audio: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataStore = LocalDataStore.getDataStore()
                dataStore.addAudioToAya(surahNumber, ayaNumberInSurah, audio)
            } catch (e: Exception) {
                Log.d("addAudioToAya", e.message ?: "Unknown error")
            }
        }
    }

    //bookmark an aya
    private fun bookmarkAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
        bookmark: Boolean,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataStore = LocalDataStore.getDataStore()
                dataStore.bookmarkAya(ayaNumber, surahNumber, ayaNumberInSurah, bookmark)
            } catch (e: Exception) {
                Log.d("bookmarkAya", e.message ?: "Unknown error")
            }
        }
    }

    //favorite an aya
    private fun favoriteAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
        favorite: Boolean,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataStore = LocalDataStore.getDataStore()
                dataStore.favoriteAya(ayaNumber, surahNumber, ayaNumberInSurah, favorite)
            } catch (e: Exception) {
                Log.d("favoriteAya", e.message ?: "Unknown error")
            }
        }
    }

    //add a note to an aya
    private fun addNoteToAya(id: Int, surahNumber: Int, ayaNumberInSurah: Int, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataStore = LocalDataStore.getDataStore()
                dataStore.addNoteToAya(id, surahNumber, ayaNumberInSurah, note)
            } catch (e: Exception) {
                Log.d("addNoteToAya", e.message ?: "Unknown error")
            }
        }
    }

    //get a note for an aya
    private fun getNoteForAya(ayaNumber: Int, surahNumber: Int, ayaNumberInSurah: Int) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val dataStore = LocalDataStore.getDataStore()
                val note = dataStore.getNoteOfAya(ayaNumber, surahNumber, ayaNumberInSurah)
            } catch (e: Exception) {
                Log.d("getNoteForAya", e.message ?: "Unknown error")
            }
        }
    }

    //state for bookmarking, favoriting, adding a note
    private val _bookmarks = MutableStateFlow(listOf<LocalAya>())
    val bookmarks = _bookmarks.asStateFlow()

    private val _favorites = MutableStateFlow(listOf<LocalAya>())
    val favorites = _favorites.asStateFlow()

    private val _notes = MutableStateFlow(listOf<LocalAya>())
    val notes = _notes.asStateFlow()

    private fun deleteNoteFromAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataStore = LocalDataStore.getDataStore()
                dataStore.deleteNoteFromAya(ayaNumber, surahNumber, ayaNumberInSurah)
                val notes = dataStore.getAyasWithNotes()
                _notes.value = notes
            } catch (e: Exception) {
                Log.d("deleteNoteFromAya", e.message ?: "Unknown error")
            }
        }
    }

    private fun deleteBookmarkFromAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataStore = LocalDataStore.getDataStore()
                dataStore.deleteBookmarkFromAya(ayaNumber, surahNumber, ayaNumberInSurah)
                val bookmarks = dataStore.getBookmarkedAyas()
                _bookmarks.value = bookmarks
            } catch (e: Exception) {
                Log.d("deleteBookmarkFromAya", e.message ?: "Unknown error")
            }
        }
    }

    private fun deleteFavoriteFromAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataStore = LocalDataStore.getDataStore()
                dataStore.deleteFavoriteFromAya(ayaNumber, surahNumber, ayaNumberInSurah)
                val favorites = dataStore.getFavoritedAyas()
                _favorites.value = favorites
            } catch (e: Exception) {
                Log.d("deleteFavoriteFromAya", e.message ?: "Unknown error")
            }
        }
    }

    private fun getAllNotes() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataStore = LocalDataStore.getDataStore()
                val notes = dataStore.getAyasWithNotes()
                _notes.value = notes
            } catch (e: Exception) {
                Log.d("getAllNotes", e.message ?: "Unknown error")
            }
        }
    }

    private fun getAllFavorites() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataStore = LocalDataStore.getDataStore()
                val favorites = dataStore.getFavoritedAyas()
                _favorites.value = favorites
            } catch (e: Exception) {
                Log.d("getAllFavorites", e.message ?: "Unknown error")
            }
        }
    }

    private fun getAllBookmarks() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataStore = LocalDataStore.getDataStore()
                val bookmarks = dataStore.getBookmarkedAyas()
                _bookmarks.value = bookmarks
            } catch (e: Exception) {
                Log.d("getAllBookmarks", e.message ?: "Unknown error")
            }
        }
    }

    fun getSurahById(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataStore = LocalDataStore.getDataStore()
                val surah = dataStore.getSurahById(id)
                _surahState.value = surah
            } catch (e: Exception) {
                Log.d("getSurahById", e.message ?: "Unknown error")
            }
        }
    }
}