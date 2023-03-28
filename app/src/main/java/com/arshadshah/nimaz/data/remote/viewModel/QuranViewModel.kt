package com.arshadshah.nimaz.data.remote.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.FULL_QURAN_DOWNLOADED
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.data.remote.models.Juz
import com.arshadshah.nimaz.data.remote.models.Surah
import com.arshadshah.nimaz.data.remote.repositories.QuranRepository
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class QuranViewModel(context : Context) : ViewModel()
{

	//repository
	private val quranRepository = QuranRepository

	val sharedPreferences = PrivateSharedPreferences(context)

//	val spaceFilesRepository = SpacesFileRepository(context)
//	//for each surah, download all the audio files
//	for (aya in response.data)
//	{
//		//get the audio file
//		spaceFilesRepository.downloadAyaFile(
//				aya.suraNumber ,
//				aya.ayaNumberInSurah
//											){ file: File? , error: Exception? , progress: Int , isCompleted: Boolean  ->
//			//if file is downloaded, save the location in database
//			if (isCompleted)
//			{
//				CoroutineScope(Dispatchers.IO).launch {
//					dataStore.addAudioToAya(
//							aya.suraNumber ,
//							aya.ayaNumberInSurah ,
//							file?.absolutePath.toString()
//										   )
//				}
//			}
//		}
//	}


	//general state for error and loading
	private val _errorState = MutableStateFlow("")
	val errorState = _errorState.asStateFlow()
	private val _loadingState = MutableStateFlow(false)
	val loadingState = _loadingState.asStateFlow()


	//surah list state
	private var _surahListState = MutableStateFlow(ArrayList<Surah>(114))
	val surahListState = _surahListState.asStateFlow()

	//juz list state
	private var _juzListState = MutableStateFlow(ArrayList<Juz>(30))
	val juzListState = _juzListState.asStateFlow()

	private val _ayaListState = MutableStateFlow(ArrayList<Aya>(100))
	val ayaListState = _ayaListState.asStateFlow()

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

	//random aya state a map of aya, surah, juz data
	private val _randomAyaState = MutableStateFlow(Aya(0 , 0 , "" , "" , "" , 0 , 0 , false , false , "" , "" , false , "" , 0 , 0))
	val randomAyaState = _randomAyaState.asStateFlow()
	private val _randomAyaSurahState = MutableStateFlow(Surah(0 , 0 , 0 , "" , "" , "" , "" , 0 , 0))
	val randomAyaSurahState = _randomAyaSurahState.asStateFlow()
	private val _randomAyaJuzState = MutableStateFlow(Juz(0 , "" , "" , 0))
	val randomAyaJuzState = _randomAyaJuzState.asStateFlow()
	//download button state
	private val _downloadButtonState = MutableStateFlow(!sharedPreferences.getDataBoolean(FULL_QURAN_DOWNLOADED , false))
	val downloadButtonState = _downloadButtonState.asStateFlow()

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

		object Download_Quran : QuranMenuEvents()

		//to check progress of download
		object Check_Download_Progress : QuranMenuEvents()

		//cancel download
		object Cancel_Download : QuranMenuEvents()

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
				_translation.value =
					sharedPreferences.getData(AppConstants.TRANSLATION_LANGUAGE , "English")

				//if the font size is not set, set it to default 26 and 16

				_arabic_Font_size.value = if (sharedPreferences.getDataFloat(AppConstants.ARABIC_FONT_SIZE) == 0.0f)
				{
					//save the default font size and also return it
					sharedPreferences.saveDataFloat(AppConstants.ARABIC_FONT_SIZE , 26.0f)
					26.0f
				}
				else
				{
					sharedPreferences.getDataFloat(AppConstants.ARABIC_FONT_SIZE)
				}
				_translation_Font_size.value = if (sharedPreferences.getDataFloat(AppConstants.TRANSLATION_FONT_SIZE) == 0.0f)
				{
					//save the default font size and also return it
					sharedPreferences.saveDataFloat(AppConstants.TRANSLATION_FONT_SIZE , 16.0f)
					16.0f
				}
				else
				{
					sharedPreferences.getDataFloat(AppConstants.TRANSLATION_FONT_SIZE)
				}

				_display_Mode.value = sharedPreferences.getData(AppConstants.PAGE_TYPE , "List")

				//downloadButtonState
				_downloadButtonState.value = !sharedPreferences.getDataBoolean(FULL_QURAN_DOWNLOADED , false)
			}
			is QuranMenuEvents.Download_Quran ->
			{
				downloadQuran()
			}
			is QuranMenuEvents.Check_Download_Progress ->
			{
				checkDownloadProgress()
			}
			is QuranMenuEvents.Cancel_Download ->
			{
				cancelDownload()
			}
		}
	}

	//progress of download
	private val _downloadProgress = MutableStateFlow(0)
	val downloadProgress = _downloadProgress.asStateFlow()

	private fun checkDownloadProgress()
	{
		viewModelScope.launch(Dispatchers.IO) {
			_errorState.value = ""
			try
			{
				//check if quran is downloaded by checking if database has 6236 ayats
				val dataStore = LocalDataStore.getDataStore()
				val ayats = dataStore.countAllAyat()
				_downloadProgress.value = (ayats * 100) / 6236
			} catch (e : Exception)
			{
				_errorState.value = e.message.toString()
			}
		}
	}

	//download coroutine
	private var downloadJob : Job? = null

	//cancel download
	private fun cancelDownload()
	{
		//cancel download coroutine
		downloadJob?.cancel()
	}

	private fun downloadQuran()
	{
		//cancel previous download
		cancelDownload()
		//start new download
		downloadJob = viewModelScope.launch(Dispatchers.IO) {
			_errorState.value = ""
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				//find out what is the last surah downloaded
				val ayat = dataStore.getAllAyat()
				//array of the surah numbers from 1 to 114
				val surahs = IntArray(114) { it + 1 }
				//check which surahs are already downloaded
				val downloadedSurahs = ayat.map { it.suraNumber }.distinct()
				//remove downloaded surahs from array
				val surahsToDownload = surahs.filter { !downloadedSurahs.contains(it) }
				//if all surahs are downloaded, return
				if (surahsToDownload.isEmpty())
				{
					_loadingState.value = false
					return@launch
				}
				//for 114 surahs, get all ayats
				for (i in surahsToDownload)
				{
					val response = quranRepository.getAyaForSurah(i)
					if (response.data != null)
					{
						dataStore.insertAyats(response.data)
					} else
					{
						_loadingState.value = false
						_errorState.value = response.message.toString()
					}
				}


				//check if all ayats are downloaded
				val ayats = dataStore.countAllAyat()
				if (ayats == 6236)
				{
					//downloadButtonState
					_downloadButtonState.value = false
					//save that quran is downloaded
					sharedPreferences.saveDataBoolean(FULL_QURAN_DOWNLOADED , true)
				}
			} catch (e : Exception)
			{
				_errorState.value = e.message.toString()
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
						_errorState.value = response.message !!
					}
				}
			} catch (e : Exception)
			{
				_surahListState.value = ArrayList()
				_loadingState.value = false
				_errorState.value = e.message !!
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
						_errorState.value = response.message !!
					}
				}
			} catch (e : Exception)
			{
				_juzListState.value = ArrayList()
				_loadingState.value = false
				_errorState.value = e.message !!
			}
		}
	}

	fun getAllAyaForSurah(surahNumber : Int , language : String)
	{
		viewModelScope.launch(Dispatchers.IO) {
			_loadingState.value = true
			_errorState.value = ""
			_ayaListState.value = ArrayList()
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				val surahTotalAya = dataStore.getSurahById(surahNumber).numberOfAyahs
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
					val response = quranRepository.getAyaForSurah(surahNumber)
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
						_errorState.value = response.message !!
					}
				}
			} catch (e : Exception)
			{
				_ayaListState.value = ArrayList()
				_loadingState.value = false
				_errorState.value = e.message !!
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
			_ayaListState.value = ArrayList()
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				val languageConverted = language.uppercase(Locale.ROOT)
				val areAyatAvailable = dataStore.countJuzAyat(juzNumber)
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
					val response = quranRepository.getAyaForJuz(juzNumber)
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
						_errorState.value = response.message !!
					}
				}

			} catch (e : Exception)
			{
				_ayaListState.value = ArrayList()
				_loadingState.value = false
				_errorState.value = e.message !!
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

		//delete a note from an aya
		data class deleteNoteFromAya(
			val ayaNumber : Int ,
			val surahNumber : Int ,
			val ayaNumberInSurah : Int ,
									) : AyaEvent()

		//delete a bookmark from an aya
		data class deleteBookmarkFromAya(
			val ayaNumber : Int ,
			val surahNumber : Int ,
			val ayaNumberInSurah : Int ,
										) : AyaEvent()

		//delete a favorite from an aya
		data class deleteFavoriteFromAya(
			val ayaNumber : Int ,
			val surahNumber : Int ,
			val ayaNumberInSurah : Int ,
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
			is AyaEvent.deleteNoteFromAya ->
			{
				deleteNoteFromAya(
						ayaEvent.ayaNumber ,
						ayaEvent.surahNumber ,
						ayaEvent.ayaNumberInSurah
								 )
			}
			is AyaEvent.deleteBookmarkFromAya ->
			{
				deleteBookmarkFromAya(
						ayaEvent.ayaNumber ,
						ayaEvent.surahNumber ,
						ayaEvent.ayaNumberInSurah
									 )
			}
			is AyaEvent.deleteFavoriteFromAya ->
			{
				deleteFavoriteFromAya(
						ayaEvent.ayaNumber ,
						ayaEvent.surahNumber ,
						ayaEvent.ayaNumberInSurah
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
	private val _bookmarks = MutableStateFlow(listOf<Aya>())
	val bookmarks = _bookmarks.asStateFlow()

	private val _favorites = MutableStateFlow(listOf<Aya>())
	val favorites = _favorites.asStateFlow()

	private val _notes = MutableStateFlow(listOf<Aya>())
	val notes = _notes.asStateFlow()

	fun deleteNoteFromAya(
		ayaNumber : Int ,
		surahNumber : Int ,
		ayaNumberInSurah : Int ,
						 )
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				dataStore.deleteNoteFromAya(ayaNumber , surahNumber , ayaNumberInSurah)
				val notes = dataStore.getAyasWithNotes()
				_notes.value = notes
			} catch (e : Exception)
			{
				Log.d("deleteNoteFromAya" , e.message ?: "Unknown error")
			}
		}
	}

	fun deleteBookmarkFromAya(
		ayaNumber : Int ,
		surahNumber : Int ,
		ayaNumberInSurah : Int ,
							 )
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				dataStore.deleteBookmarkFromAya(ayaNumber , surahNumber , ayaNumberInSurah)
				val bookmarks = dataStore.getBookmarkedAyas()
				_bookmarks.value = bookmarks
			} catch (e : Exception)
			{
				Log.d("deleteBookmarkFromAya" , e.message ?: "Unknown error")
			}
		}
	}

	fun deleteFavoriteFromAya(
		ayaNumber : Int ,
		surahNumber : Int ,
		ayaNumberInSurah : Int ,
							 )
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				dataStore.deleteFavoriteFromAya(ayaNumber , surahNumber , ayaNumberInSurah)
				val favorites = dataStore.getFavoritedAyas()
				_favorites.value = favorites
			} catch (e : Exception)
			{
				Log.d("deleteFavoriteFromAya" , e.message ?: "Unknown error")
			}
		}
	}

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
	//get random aya from database
	fun getRandomAya()
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				//check if there are any ayas in the database
				val ayas = dataStore.countAllAyat()
				if (ayas > 0)
				{
					//get a random aya
					val randomAya = dataStore.getRandomAya()
					if (randomAya.ayaNumberInSurah == 0 || randomAya.ayaNumberInSurah == 1)
					{
						getRandomAya()
					}
					val surahOfTheAya = dataStore.getSurahById(randomAya.suraNumber)
					val juzOfTheAya = dataStore.getJuzById(randomAya.juzNumber)
					_randomAyaState.value = randomAya
					_randomAyaSurahState.value = surahOfTheAya
					_randomAyaJuzState.value = juzOfTheAya
					sharedPreferences.saveDataInt(
							AppConstants.RANDOM_AYAT_NUMBER_IN_SURAH_LAST_FETCHED , randomAya.ayaNumberInSurah)

				} else
				{
					val ayat = QuranRepository.getAyaForSurah(1)
					//add the ayat to the database
					dataStore.insertAyats(ayat.data!!)
					//get a random aya
					val randomAya = dataStore.getRandomAya()
					if (randomAya.ayaNumberInSurah == 0 || randomAya.ayaNumberInSurah == 1)
					{
						getRandomAya()
					}
					val surahOfTheAya = dataStore.getSurahById(randomAya.suraNumber)
					val juzOfTheAya = dataStore.getJuzById(randomAya.juzNumber)
					_randomAyaState.value = randomAya
					_randomAyaSurahState.value = surahOfTheAya
					_randomAyaJuzState.value = juzOfTheAya
					sharedPreferences.saveDataInt(
							AppConstants.RANDOM_AYAT_NUMBER_IN_SURAH_LAST_FETCHED , randomAya.ayaNumberInSurah)
				}
			} catch (e : Exception)
			{
				Log.d("getRandomAya" , e.message ?: "Unknown error")
			}
		}
	}

	//getAyatByAyaNumberInSurah
	fun getAyatByAyaNumberInSurah(ayaNumberInSurah : Int)
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				val ayat = dataStore.getAyatByAyaNumberInSurah(ayaNumberInSurah)
				val surahOfTheAya = dataStore.getSurahById(ayat.suraNumber)
				val juzOfTheAya = dataStore.getJuzById(ayat.juzNumber)
				_randomAyaSurahState.value = surahOfTheAya
				_randomAyaJuzState.value = juzOfTheAya
				_randomAyaState.value = ayat
				sharedPreferences.saveDataInt(
						AppConstants.RANDOM_AYAT_NUMBER_IN_SURAH_LAST_FETCHED , ayaNumberInSurah)
			} catch (e : Exception)
			{
				Log.d("getAyatByAyaNumberInSurah" , e.message ?: "Unknown error")
			}
		}
	}
}