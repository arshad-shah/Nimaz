package com.arshadshah.nimaz.data.remote.repositories

import android.util.Log
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.data.remote.models.Juz
import com.arshadshah.nimaz.data.remote.models.Surah
import com.arshadshah.nimaz.utils.network.ApiResponse
import com.arshadshah.nimaz.utils.network.NimazServicesImpl
import io.ktor.client.plugins.*
import java.util.*

object QuranRepository
{

	suspend fun getSurahs() : ApiResponse<ArrayList<Surah>>
	{
		return try
		{
			val response = NimazServicesImpl.getSurahs()
			Log.d("QuranRepository" , "getSurahs: $response")
			//create an array list of surah from the response
			val surahs = ArrayList<Surah>()
			for (surahResponse in response)
			{
				val surah = Surah(
						surahResponse.number ,
						surahResponse.numberOfAyahs ,
						surahResponse.startAya ,
						surahResponse.name ,
						surahResponse.englishName ,
						surahResponse.englishNameTranslation ,
						surahResponse.revelationType ,
						surahResponse.revelationOrder ,
						surahResponse.rukus
								 )
				surahs.add(surah)
			}
			ApiResponse.Success(surahs)
		} catch (e : ClientRequestException)
		{
			ApiResponse.Error(e.message , null)

		} catch (e : Exception)
		{
			throw Exception(e.message)
		}
	}

	suspend fun getJuzs() : ApiResponse<ArrayList<Juz>>
	{
		return try
		{
			val response = NimazServicesImpl.getJuzs()
			Log.d("QuranRepository" , "getJuzs: $response")
			//create an array list of surah from the response
			val Juzs = ArrayList<Juz>()
			for (juzResponse in response)
			{
				val juz = Juz(
						juzResponse.number ,
						juzResponse.name ,
						juzResponse.tname ,
						juzResponse.juzStartAyaInQuran ,
							 )
				Juzs.add(juz)
			}
			ApiResponse.Success(Juzs)
		} catch (e : ClientRequestException)
		{
			ApiResponse.Error(e.message , null)

		} catch (e : Exception)
		{
			throw Exception(e.message)
		}
	}


	suspend fun getAyaForSurah(
		surahNumber : Int ,
		language : String ,
							  ) : ApiResponse<ArrayList<Aya>>
	{
		return try
		{
			//capitalize the language
			val languageConverted = language.uppercase(Locale.ROOT)
			val response =
				NimazServicesImpl.getAyaForSurah(surahNumber , languageConverted)
			Log.d("QuranRepository" , "getAyaForSurah: $response")
			//create an array list of surah from the response
			val ayas = ArrayList<Aya>()
			for (ayaResponse in response)
			{
				val aya = Aya(
						ayaResponse.number ,
						ayaResponse.arabic ,
						ayaResponse.translation ,
						"Surah" ,
						surahNumber ,
							 )
				ayas.add(aya)
			}
			ApiResponse.Success(ayas)
		} catch (e : ClientRequestException)
		{
			ApiResponse.Error(e.message , null)

		} catch (e : Exception)
		{
			throw Exception(e.message)
		}
	}

	suspend fun getAyaForJuz(
		juzNumber : Int ,
		language : String ,
							) : ApiResponse<ArrayList<Aya>>
	{
		return try
		{
			//capitalize the language
			val languageConverted = language.uppercase(Locale.ROOT)
			val response = NimazServicesImpl.getAyaForJuz(juzNumber , languageConverted)
			Log.d("QuranRepository" , "getAyaForJuz: $response")
			//create an array list of surah from the response
			val ayas = ArrayList<Aya>()
			for (ayaResponse in response)
			{
				val aya = Aya(
						ayaResponse.number ,
						ayaResponse.arabic ,
						ayaResponse.translation ,
						"Juz" ,
						juzNumber ,
							 )
				ayas.add(aya)
			}
			ApiResponse.Success(ayas)
		} catch (e : ClientRequestException)
		{
			ApiResponse.Error(e.message , null)

		} catch (e : Exception)
		{
			throw Exception(e.message)
		}
	}
}