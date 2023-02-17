package com.arshadshah.nimaz.data.remote.viewModel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.data.remote.models.Juz
import com.arshadshah.nimaz.data.remote.models.Surah
import com.arshadshah.nimaz.data.remote.repositories.QuranRepository
import com.arshadshah.nimaz.utils.LocalDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class QuranViewModel(context : Context) : ViewModel()
{

	sealed class SurahState
	{

		object Loading : SurahState()
		data class Success(val data : ArrayList<Surah>?) : SurahState()
		data class Error(val errorMessage : String) : SurahState()
	}

	sealed class JuzState
	{

		object Loading : JuzState()
		data class Success(val data : ArrayList<Juz>) : JuzState()
		data class Error(val errorMessage : String) : JuzState()
	}

	sealed class AyaSurahState
	{

		object Loading : AyaSurahState()
		data class Success(val data : ArrayList<Aya>) : AyaSurahState()
		data class Error(val errorMessage : String) : AyaSurahState()
	}

	sealed class AyaJuzState
	{

		object Loading : AyaJuzState()
		data class Success(val data : ArrayList<Aya>) : AyaJuzState()
		data class Error(val errorMessage : String) : AyaJuzState()
	}

	//aya features state
	sealed class AyaState
	{

		object Loading : AyaState()
		data class Success(val data : String) : AyaState()
		data class Error(val errorMessage : String) : AyaState()
	}

	private var _noteOfAya = MutableLiveData<String>()
	val noteOfAya : LiveData<String> = _noteOfAya

	private var _ayaState = MutableStateFlow(AyaState.Loading as AyaState)
	val ayaState = _ayaState.asStateFlow()

	private var _ayaJuzstate = MutableStateFlow(AyaJuzState.Loading as AyaJuzState)
	val ayaJuzstate = _ayaJuzstate.asStateFlow()

	private var _ayaSurahstate = MutableStateFlow(AyaSurahState.Loading as AyaSurahState)
	val ayaSurahState = _ayaSurahstate.asStateFlow()


	private var _surahState = MutableStateFlow(SurahState.Loading as SurahState)
	val surahState = _surahState.asStateFlow()


	private var _juzState = MutableStateFlow(JuzState.Loading as JuzState)
	val juzState = _juzState.asStateFlow()

	init
	{
		getSurahList(context)
		getJuzList(context)
	}

	fun getSurahList(context : Context)
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				val surahAvailable = dataStore.countSurah()
				if (surahAvailable > 0)
				{
					val surahList = dataStore.getAllSurah().toMutableList() as ArrayList<Surah>
					_surahState.value = SurahState.Success(surahList)
				} else
				{
					val response = QuranRepository.getSurahs()
					if (response.data != null)
					{
						dataStore.saveAllSurah(response.data)
						_surahState.value = SurahState.Success(response.data)
					} else
					{
						_surahState.value = SurahState.Error(response.message !!)
					}
				}
			} catch (e : Exception)
			{
				_surahState.value = SurahState.Error(e.message ?: "Unknown error")
			}
		}
	}

	fun getJuzList(context : Context)
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				val juzAvailable = dataStore.countJuz()
				if (juzAvailable > 0)
				{
					val juzList = dataStore.getAllJuz().toMutableList() as ArrayList<Juz>
					_juzState.value = JuzState.Success(juzList)
				} else
				{
					val response = QuranRepository.getJuzs()
					if (response.data != null)
					{
						dataStore.saveAllJuz(response.data)
						_juzState.value = JuzState.Success(response.data)
					} else
					{
						_juzState.value = JuzState.Error(response.message !!)
					}
				}
			} catch (e : Exception)
			{
				_juzState.value = JuzState.Error(e.message ?: "Unknown error")
			}
		}
	}

	fun getAllAyaForSurah(surahNumber : Int , language : String)
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				val surahTotalAya = (dataStore.getSurahById(surahNumber).numberOfAyahs)
				val languageConverted = language.uppercase(Locale.ROOT)
				val ayaInDatabase = dataStore.countSurahAyat(surahNumber , languageConverted)
				//check if the ayat are teh same as the surah total ayat
				val areAyatSame = ayaInDatabase == surahTotalAya

				if (areAyatSame)
				{
					val surahAyatList =
						dataStore.getAyasOfSurah(surahNumber , languageConverted) as ArrayList<Aya>
					val newList =
						addBismillahToFirstAya(surahAyatList , languageConverted , surahNumber)
					_ayaSurahstate.value = AyaSurahState.Success(newList)
				} else
				{
					val response = QuranRepository.getAyaForSurah(surahNumber , language)
					if (response.data != null)
					{
						dataStore.insertAyats(response.data)
						val newList =
							addBismillahToFirstAya(response.data , languageConverted , surahNumber)
						_ayaSurahstate.value = AyaSurahState.Success(newList)
					} else
					{
						_ayaSurahstate.value = AyaSurahState.Error(response.message !!)
					}
				}
			} catch (e : Exception)
			{
				_ayaSurahstate.value = AyaSurahState.Error(e.message ?: "Unknown error")
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
		//create a map of the aya of bismillah
		val aya = Aya(
				0 ,
				ayaNumberOfBismillah ,
				ayaArabicOfBismillah ,
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
				languageConverted
					 )
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
						dataStore.getAyasOfJuz(juzNumber , languageConverted) as ArrayList<Aya>
					val newList = addBismillahInJuz(juzNumber , languageConverted , listOfJuzAyat)
					_ayaJuzstate.value = AyaJuzState.Success(newList)
				} else
				{
					val response = QuranRepository.getAyaForJuz(juzNumber , language)
					if (response.data != null)
					{
						dataStore.insertAyats(response.data)
						val newList =
							addBismillahInJuz(juzNumber , languageConverted , response.data)
						_ayaJuzstate.value = AyaJuzState.Success(newList)
					} else
					{
						_ayaJuzstate.value = AyaJuzState.Error(response.message !!)
					}
				}

			} catch (e : Exception)
			{
				_ayaJuzstate.value = AyaJuzState.Error(e.message ?: "Unknown error")
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
		val ayaNumberOfBismillah = "0"

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
		val ayaOfBismillahMap = Aya(
				0 ,
				ayaNumberOfBismillah.toInt() ,
				ayaArabicOfBismillah ,
				ayaOfBismillah ,
				0 ,
				1 ,
				false ,
				false ,
				"" ,
				"" ,
				false ,
				"" ,
				0 ,
				juzNumber ,
				languageConverted
								   )


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
						listOfJuzAyat.add(index , ayaOfBismillahMap)
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
				_ayaState.value = AyaState.Loading
				val dataStore = LocalDataStore.getDataStore()
				dataStore.addAudioToAya(surahNumber , ayaNumberInSurah , audio)
				_ayaState.value = AyaState.Success("")
			} catch (e : Exception)
			{
				_ayaState.value = AyaState.Error(e.message ?: "Unknown error")
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
				_ayaState.value = AyaState.Loading
				val dataStore = LocalDataStore.getDataStore()
				dataStore.bookmarkAya(ayaNumber , surahNumber , ayaNumberInSurah , bookmark)
				_ayaState.value = AyaState.Success("")
			} catch (e : Exception)
			{
				_ayaState.value = AyaState.Error(e.message ?: "Unknown error")
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
				_ayaState.value = AyaState.Loading
				val dataStore = LocalDataStore.getDataStore()
				dataStore.favoriteAya(ayaNumber , surahNumber , ayaNumberInSurah , favorite)
				_ayaState.value = AyaState.Success("")
			} catch (e : Exception)
			{
				_ayaState.value = AyaState.Error(e.message ?: "Unknown error")
			}
		}
	}

	//add a note to an aya
	fun addNoteToAya(id : Int , surahNumber : Int , ayaNumberInSurah : Int , note : String)
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				_ayaState.value = AyaState.Loading
				val dataStore = LocalDataStore.getDataStore()
				dataStore.addNoteToAya(id , surahNumber , ayaNumberInSurah , note)
				_ayaState.value = AyaState.Success("")
			} catch (e : Exception)
			{
				_ayaState.value = AyaState.Error(e.message ?: "Unknown error")
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
				_noteOfAya.value = note
			} catch (e : Exception)
			{
				_noteOfAya.value = "Error getting note"
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
				_ayaState.value = AyaState.Loading
				val dataStore = LocalDataStore.getDataStore()
				val notes = dataStore.getAyasWithNotes()
				_notes.value = notes
				_ayaState.value = AyaState.Success("")
			} catch (e : Exception)
			{
				_ayaState.value = AyaState.Error(e.message ?: "Unknown error")
			}
		}
	}


	//get all favorites
	fun getAllFavorites()
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				_ayaState.value = AyaState.Loading
				val dataStore = LocalDataStore.getDataStore()
				val favorites = dataStore.getFavoritedAyas()
				_favorites.value = favorites
				_ayaState.value = AyaState.Success("")
			} catch (e : Exception)
			{
				_ayaState.value = AyaState.Error(e.message ?: "Unknown error")
			}
		}
	}

	//get all bookmarks
	fun getAllBookmarks()
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				_ayaState.value = AyaState.Loading
				val dataStore = LocalDataStore.getDataStore()
				val bookmarks = dataStore.getBookmarkedAyas()
				_bookmarks.value = bookmarks
				_ayaState.value = AyaState.Success("")
			} catch (e : Exception)
			{
				_ayaState.value = AyaState.Error(e.message ?: "Unknown error")
			}
		}
	}
}