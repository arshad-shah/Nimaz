package com.arshadshah.nimaz.data.remote.viewModel

import android.content.Context
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
import kotlin.collections.ArrayList

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
				val languageConverted = language.uppercase(Locale.ROOT)
				val areAyatAvailable = dataStore.countSurahAyat(surahNumber, languageConverted)
				if(areAyatAvailable > 0){
					val surahAyatList = dataStore.getAyasOfSurah(surahNumber, languageConverted)
					_ayaSurahstate.value = AyaSurahState.Success(surahAyatList as ArrayList<Aya>)
				}else{
					val response = QuranRepository.getAyaForSurah(surahNumber , language)
					if (response.data != null)
					{
						dataStore.insertAyats(response.data)
						_ayaSurahstate.value = AyaSurahState.Success(response.data)
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

	fun getAllAyaForJuz(juzNumber : Int , language : String)
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				val languageConverted = language.uppercase(Locale.ROOT)
				val areAyatAvailable = dataStore.countJuzAyat(juzNumber, languageConverted)
				if(areAyatAvailable > 0){
					val listOfJuzAyat = dataStore.getAyasOfJuz(juzNumber, languageConverted)
					_ayaJuzstate.value = AyaJuzState.Success(listOfJuzAyat as ArrayList<Aya>)
				}
				val response = QuranRepository.getAyaForJuz(juzNumber , language)
				if (response.data != null)
				{
					dataStore.insertAyats(response.data)
					_ayaJuzstate.value = AyaJuzState.Success(response.data)
				} else
				{
					_ayaJuzstate.value = AyaJuzState.Error(response.message !!)
				}

			} catch (e : Exception)
			{
				_ayaJuzstate.value = AyaJuzState.Error(e.message ?: "Unknown error")
			}
		}
	}
}