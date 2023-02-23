package com.arshadshah.nimaz.data.remote.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.data.remote.models.Juz
import com.arshadshah.nimaz.data.remote.models.Surah
import com.arshadshah.nimaz.data.remote.repositories.QuranRepository
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class QuranViewModel(context : Context) : ViewModel()
{
	//repository
	private val quranRepository = QuranRepository

	val sharedPreferences = PrivateSharedPreferences(context)


	//general state for error and loading
	private val _errorState = MutableStateFlow("")
	val errorState = _errorState.asStateFlow()
	private val _loadingState = MutableStateFlow(false)
	val loadingState = _loadingState.asStateFlow()


	//surah list state
	private var _surahListState = MutableStateFlow(ArrayList<Surah>())
	val surahListState = _surahListState.asStateFlow()

	//juz list state
	private var _juzListState = MutableStateFlow(ArrayList<Juz>())
	val juzListState = _juzListState.asStateFlow()

	private val _ayaListState = MutableStateFlow(ArrayList<Aya>())
	val ayaListState = _ayaListState.asStateFlow()

	//state for quran menu features like page display, font size, font type, etc
	private val _arabic_Font_size = MutableStateFlow(24.0f)
	val arabic_Font_size = _arabic_Font_size.asStateFlow()

	private val _arabic_Font = MutableStateFlow("Default")
	val arabic_Font = _arabic_Font.asStateFlow()

	private val _translation_Font_size = MutableStateFlow(14.0f)
	val translation_Font_size = _translation_Font_size.asStateFlow()

	private val _translation = MutableStateFlow("English")
	val translation = _translation.asStateFlow()

	private val _display_Mode = MutableStateFlow("List")
	val display_Mode = _display_Mode.asStateFlow()


	init
	{
		getSurahList()
		getJuzList()
	}


	//events for quran menu features like page display, font size, font type, etc
	sealed class QuranMenuEvents
	{
		//change arabic font
		data class Change_Arabic_Font(val font : String) : QuranMenuEvents()
		//change translation font
		data class Change_Translation(val lang : String) : QuranMenuEvents()
		data class Change_Arabic_Font_Size(val size : Float) : QuranMenuEvents()
		//change translation font size
		data class Change_Translation_Font_Size(val size : Float) : QuranMenuEvents()

		//change display mode
		data class Change_Display_Mode(val mode : String) : QuranMenuEvents()

		//initialize quran using settings
		object Initialize_Quran : QuranMenuEvents()
	}

	fun handleQuranMenuEvents(event : QuranMenuEvents)
	{
		when (event)
		{
			is QuranMenuEvents.Change_Arabic_Font ->
			{
				_arabic_Font.value = event.font
			}
			is QuranMenuEvents.Change_Translation ->
			{
				_translation.value = event.lang
			}
			is QuranMenuEvents.Change_Arabic_Font_Size ->
			{
				_arabic_Font_size.value = event.size
			}
			is QuranMenuEvents.Change_Translation_Font_Size ->
			{
				_translation_Font_size.value = event.size
			}
			is QuranMenuEvents.Change_Display_Mode ->
			{
				_display_Mode.value = event.mode
			}
			is QuranMenuEvents.Initialize_Quran ->
			{
				_arabic_Font.value = sharedPreferences.getData(AppConstants.FONT_STYLE , "Default")
				_translation.value = sharedPreferences.getData(AppConstants.TRANSLATION_LANGUAGE , "English")
				_arabic_Font_size.value = sharedPreferences.getDataFloat(AppConstants.ARABIC_FONT_SIZE)
				_translation_Font_size.value = sharedPreferences.getDataFloat(AppConstants.TRANSLATION_FONT_SIZE)
				_display_Mode.value = sharedPreferences.getData(AppConstants.PAGE_TYPE , "List")
			}
		}
	}

	fun getSurahList()
	{
		viewModelScope.launch(Dispatchers.IO) {
			_loadingState.value = true
			_errorState.value = ""
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				val surahAvailable = dataStore.countSurah()
				if (surahAvailable > 0)
				{
					val surahList = dataStore.getAllSurah().toMutableList() as ArrayList<Surah>
					_surahListState.value = surahList
					_loadingState.value = false
					_errorState.value = ""
				} else
				{
					val response = quranRepository.getSurahs()
					if (response.data != null)
					{
						dataStore.saveAllSurah(response.data)
						_surahListState.value = response.data
						_loadingState.value = false
						_errorState.value = ""
					} else
					{
						_surahListState.value = ArrayList()
						_loadingState.value = false
						_errorState.value = response.message!!
					}
				}
			} catch (e : Exception)
			{
				_surahListState.value = ArrayList()
				_loadingState.value = false
				_errorState.value = e.message!!
			}
		}
	}

	fun getJuzList()
	{
		viewModelScope.launch(Dispatchers.IO) {
			_loadingState.value = true
			_errorState.value = ""
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				val juzAvailable = dataStore.countJuz()
				if (juzAvailable > 0)
				{
					val juzList = dataStore.getAllJuz().toMutableList() as ArrayList<Juz>
					_juzListState.value = juzList
					_loadingState.value = false
					_errorState.value = ""
				} else
				{
					val response = quranRepository.getJuzs()
					if (response.data != null)
					{
						dataStore.saveAllJuz(response.data)
						_juzListState.value = response.data
						_loadingState.value = false
						_errorState.value = ""
					} else
					{
						_juzListState.value = ArrayList()
						_loadingState.value = false
						_errorState.value = response.message!!
					}
				}
			} catch (e : Exception)
			{
				_juzListState.value = ArrayList()
				_loadingState.value = false
				_errorState.value = e.message!!
			}
		}
	}

	fun getAllAyaForSurah(surahNumber : Int , language : String)
	{
		viewModelScope.launch(Dispatchers.IO) {
			_loadingState.value = true
			_errorState.value = ""
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				val surahTotalAya = (dataStore.getSurahById(surahNumber).numberOfAyahs)
				val languageConverted = language.uppercase(Locale.ROOT)
				val ayaInDatabase = dataStore.countSurahAyat(surahNumber)
				//check if the ayat are teh same as the surah total ayat
				val areAyatSame = ayaInDatabase == surahTotalAya

				if (areAyatSame)
				{
					val surahAyatList =
						dataStore.getAyasOfSurah(surahNumber) as ArrayList<Aya>
					val newList =
						addBismillahToFirstAya(surahAyatList , languageConverted , surahNumber)
					_ayaListState.value = newList
					_loadingState.value = false
					_errorState.value = ""
				} else
				{
					val response = quranRepository.getAyaForSurah(surahNumber , language)
					if (response.data != null)
					{
						dataStore.insertAyats(response.data)
						val newList =
							addBismillahToFirstAya(response.data , languageConverted , surahNumber)
						_ayaListState.value = newList
						_loadingState.value = false
						_errorState.value = ""
					} else
					{
						_ayaListState.value = ArrayList()
						_loadingState.value = false
						_errorState.value = response.message!!
					}
				}
			} catch (e : Exception)
			{
				_ayaListState.value = ArrayList()
				_loadingState.value = false
				_errorState.value = e.message!!
			}
		}
	}

	fun addBismillahToFirstAya(
		surahAyatList : ArrayList<Aya> ,
		languageConverted : String ,
		surahNumber : Int ,
							  ) : ArrayList<Aya>
	{
		//an empty number
		val ayaNumberOfBismillah = 0
		val ayaOfBismillah = when (languageConverted)
		{
			"ENGLISH" -> "In the name of Allah, the Entirely Merciful, the Especially Merciful"
			"URDU" -> "اللہ کے نام سے جو رحمان و رحیم ہے"

			else ->
			{
				"In the name of Allah, the Entirely Merciful, the Especially Merciful"
			}
		}
		val ayaArabicOfBismillah = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"
		val aya : Aya
		if (languageConverted == "ENGLISH")
		{
			aya = Aya(
					0 ,
					ayaNumberOfBismillah ,
					ayaArabicOfBismillah ,
					ayaOfBismillah ,
					"" ,
					surahNumber ,
					1 ,
					false ,
					false ,
					"" ,
					"" ,
					false ,
					"" ,
					0 ,
					0 ,
					 )
		} else
		{
			aya = Aya(
					0 ,
					ayaNumberOfBismillah ,
					ayaArabicOfBismillah ,
					"" ,
					ayaOfBismillah ,
					surahNumber ,
					1 ,
					false ,
					false ,
					"" ,
					"" ,
					false ,
					"" ,
					0 ,
					0 ,
					 )
		}
		//first check if an object like this is already in the list
		//check all the attributes of the object bisimillah with the attributes of the object in the list at index 0
		if (surahAyatList[0].ayaArabic != ayaArabicOfBismillah && surahAyatList[0].suraNumber != 1)
		{
			if (surahNumber != 9)
			{
				surahAyatList.add(0 , aya)
			}
		}

		return surahAyatList
	}

	fun getAllAyaForJuz(juzNumber : Int , language : String)
	{
		viewModelScope.launch(Dispatchers.IO) {
			_loadingState.value = true
			_errorState.value = ""
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				val languageConverted = language.uppercase(Locale.ROOT)
				val areAyatAvailable = dataStore.countJuzAyat(juzNumber , languageConverted)
				val juzStartAya = dataStore.getJuzById(juzNumber).juzStartAyaInQuran
				val juzEndAya =
					if (juzNumber != 30) (dataStore.getJuzById(juzNumber + 1).juzStartAyaInQuran) else 6236
				val juzTotalAyat = juzEndAya - juzStartAya

				if (juzTotalAyat == areAyatAvailable)
				{
					val listOfJuzAyat =
						dataStore.getAyasOfJuz(juzNumber) as ArrayList<Aya>
					val newList = addBismillahInJuz(juzNumber , languageConverted , listOfJuzAyat)
					_ayaListState.value = newList
					_loadingState.value = false
					_errorState.value = ""

				} else
				{
					val response = quranRepository.getAyaForJuz(juzNumber , language)
					if (response.data != null)
					{
						dataStore.insertAyats(response.data)
						val newList =
							addBismillahInJuz(juzNumber , languageConverted , response.data)
						_ayaListState.value = newList
						_loadingState.value = false
						_errorState.value = ""
					} else
					{
						_ayaListState.value = ArrayList()
						_loadingState.value = false
						_errorState.value = response.message!!
					}
				}

			} catch (e : Exception)
			{
				_ayaListState.value = ArrayList()
				_loadingState.value = false
				_errorState.value = e.message!!
			}
		}
	}

	//function to add biismillah to the start of every surah
	fun addBismillahInJuz(
		juzNumber : Int ,
		languageConverted : String ,
		listOfJuzAyat : ArrayList<Aya> ,
						 ) : ArrayList<Aya>
	{

		//add the following object to index 0 of ayaForSurah without losing value of index 0 in ayaForSurah
		val ayaNumberOfBismillah = 0

		val ayaOfBismillah = when (languageConverted)
		{
			"ENGLISH" -> "In the name of Allah, the Entirely Merciful, the Especially Merciful"
			"URDU" -> "اللہ کے نام سے جو رحمان و رحیم ہے"

			else ->
			{
				"In the name of Allah, the Entirely Merciful, the Especially Merciful"
			}
		}
		val ayaArabicOfBismillah = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"

		//create a map of the aya of bismillah
		val aya : Aya
		if (languageConverted == "ENGLISH")
		{
			aya = Aya(
					0 ,
					ayaNumberOfBismillah ,
					ayaArabicOfBismillah ,
					ayaOfBismillah ,
					"" ,
					listOfJuzAyat[0].suraNumber ,
					1 ,
					false ,
					false ,
					"" ,
					"" ,
					false ,
					"" ,
					0 ,
					0 ,
					 )
		} else
		{
			aya = Aya(
					0 ,
					ayaNumberOfBismillah ,
					ayaArabicOfBismillah ,
					"" ,
					ayaOfBismillah ,
					listOfJuzAyat[0].suraNumber ,
					1 ,
					false ,
					false ,
					"" ,
					"" ,
					false ,
					"" ,
					0 ,
					0 ,
					 )
		}
		//find all the objects in arraylist ayaForJuz where ayaForJuz[i]!!.ayaNumber = 1
		//add object bismillah before it for every occurance of ayaForJuz[i]!!.ayaNumber = 1
		var index = 0
		while (index < listOfJuzAyat.size)
		{
			if (listOfJuzAyat[index].ayaNumber == 1 && listOfJuzAyat[index].suraNumber != 1)
			{
				//add bismillah before ayaForJuz[i]
				if (listOfJuzAyat[index].ayaNumber == 1)
				{
					if (juzNumber + 1 != 10 && index != 36)
					{
						//add the map of bismillah to ayaList at the current index
						listOfJuzAyat.add(index , aya)
						//skip the next iteration
						index ++
					}
				}
			}
			index ++
		}

		return listOfJuzAyat
	}


	//events to bookmark an aya, favorite an aya, add a note to an aya
	sealed class AyaEvent
	{

		data class BookmarkAya(
			val ayaNumber : Int ,
			val surahNumber : Int ,
			val ayaNumberInSurah : Int ,
			val bookmark : Boolean ,
							  ) : AyaEvent()

		data class FavoriteAya(
			val ayaNumber : Int ,
			val surahNumber : Int ,
			val ayaNumberInSurah : Int ,
			val favorite : Boolean ,
							  ) : AyaEvent()

		data class AddNoteToAya(
			val ayaNumber : Int ,
			val surahNumber : Int ,
			val ayaNumberInSurah : Int ,
			val note : String ,
							   ) : AyaEvent()

		data class getNoteForAya(
			val ayaNumber : Int ,
			val surahNumber : Int ,
			val ayaNumberInSurah : Int ,
								) : AyaEvent()

		//get all bookmarks
		object getBookmarks : AyaEvent()

		//get all favorites
		object getFavorites : AyaEvent()

		//get all notes
		object getNotes : AyaEvent()

		//addAudioToAya
		data class addAudioToAya(
			val surahNumber : Int ,
			val ayaNumberInSurah : Int ,
			val audio : String ,
								) : AyaEvent()
	}

	//events handler
	fun handleAyaEvent(ayaEvent : AyaEvent)
	{
		when (ayaEvent)
		{
			is AyaEvent.BookmarkAya ->
			{
				bookmarkAya(
						ayaEvent.ayaNumber ,
						ayaEvent.surahNumber ,
						ayaEvent.ayaNumberInSurah ,
						ayaEvent.bookmark
						   )
			}

			is AyaEvent.FavoriteAya ->
			{
				favoriteAya(
						ayaEvent.ayaNumber ,
						ayaEvent.surahNumber ,
						ayaEvent.ayaNumberInSurah ,
						ayaEvent.favorite
						   )
			}

			is AyaEvent.AddNoteToAya ->
			{
				addNoteToAya(
						ayaEvent.ayaNumber ,
						ayaEvent.surahNumber ,
						ayaEvent.ayaNumberInSurah ,
						ayaEvent.note
							)
			}

			is AyaEvent.getNoteForAya ->
			{
				getNoteForAya(ayaEvent.ayaNumber , ayaEvent.surahNumber , ayaEvent.ayaNumberInSurah)
			}

			is AyaEvent.getBookmarks ->
			{
				getAllBookmarks()
			}

			is AyaEvent.getFavorites ->
			{
				getAllFavorites()
			}

			is AyaEvent.getNotes ->
			{
				getAllNotes()
			}

			is AyaEvent.addAudioToAya ->
			{
				addAudioToAya(
						ayaEvent.surahNumber ,
						ayaEvent.ayaNumberInSurah ,
						ayaEvent.audio
							 )
			}

			else ->
			{
			}
		}
	}

	//add audio to aya
	fun addAudioToAya(
		surahNumber : Int ,
		ayaNumberInSurah : Int ,
		audio : String ,
					 )
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				dataStore.addAudioToAya(surahNumber , ayaNumberInSurah , audio)
			} catch (e : Exception)
			{
				Log.d("addAudioToAya" , e.message ?: "Unknown error")
			}
		}
	}

	//bookmark an aya
	fun bookmarkAya(
		ayaNumber : Int ,
		surahNumber : Int ,
		ayaNumberInSurah : Int ,
		bookmark : Boolean ,
				   )
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				dataStore.bookmarkAya(ayaNumber , surahNumber , ayaNumberInSurah , bookmark)
			} catch (e : Exception)
			{
				Log.d("bookmarkAya" , e.message ?: "Unknown error")
			}
		}
	}

	//favorite an aya
	fun favoriteAya(
		ayaNumber : Int ,
		surahNumber : Int ,
		ayaNumberInSurah : Int ,
		favorite : Boolean ,
				   )
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				dataStore.favoriteAya(ayaNumber , surahNumber , ayaNumberInSurah , favorite)
			} catch (e : Exception)
			{
				Log.d("favoriteAya" , e.message ?: "Unknown error")
			}
		}
	}

	//add a note to an aya
	fun addNoteToAya(id : Int , surahNumber : Int , ayaNumberInSurah : Int , note : String)
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				dataStore.addNoteToAya(id , surahNumber , ayaNumberInSurah , note)
			} catch (e : Exception)
			{
				Log.d("addNoteToAya" , e.message ?: "Unknown error")
			}
		}
	}

	//get a note for an aya
	fun getNoteForAya(ayaNumber : Int , surahNumber : Int , ayaNumberInSurah : Int)
	{
		viewModelScope.launch(Dispatchers.Main) {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				val note = dataStore.getNoteOfAya(ayaNumber , surahNumber , ayaNumberInSurah)
			} catch (e : Exception)
			{
				Log.d("getNoteForAya" , e.message ?: "Unknown error")
			}
		}
	}

	//state for bookmarking, favoriting, adding a note
	private val _bookmarks = MutableLiveData<List<Aya>>()
	val bookmarks : LiveData<List<Aya>> = _bookmarks

	private val _favorites = MutableLiveData<List<Aya>>()
	val favorites : LiveData<List<Aya>> = _favorites

	private val _notes = MutableLiveData<List<Aya>>()
	val notes : LiveData<List<Aya>> = _notes

	fun getAllNotes()
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				val notes = dataStore.getAyasWithNotes()
				_notes.value = notes
			} catch (e : Exception)
			{
				Log.d("getAllNotes" , e.message ?: "Unknown error")
			}
		}
	}


	//get all favorites
	fun getAllFavorites()
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				val favorites = dataStore.getFavoritedAyas()
				_favorites.value = favorites
			} catch (e : Exception)
			{
				Log.d("getAllFavorites" , e.message ?: "Unknown error")
			}
		}
	}

	//get all bookmarks
	fun getAllBookmarks()
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				val bookmarks = dataStore.getBookmarkedAyas()
				_bookmarks.value = bookmarks
			} catch (e : Exception)
			{
				Log.d("getAllBookmarks" , e.message ?: "Unknown error")
			}
		}
	}
}