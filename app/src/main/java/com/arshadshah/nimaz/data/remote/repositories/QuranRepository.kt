package com.arshadshah.nimaz.data.remote.repositories

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
			//create an array list of surah from the response
			val ayas = ArrayList<Aya>()
			for (ayaResponse in response)
			{
				//	var ayaNumber: Int,
				//	var ayaArabic: String,
				//	var ayaTranslation: String,
				//	var suraNumber: Int,
				//	var ayaNumberInSurah: Int,
				//	var bookmark: Boolean,
				//	var favorite: Boolean,
				//	var note: String,
				//	var audioFileLocation: String,
				//	var sajda: Boolean,
				//	var sajdaType: String,
				//	var ruku: Int,
				//	var juzNumber: Int,
				//	val ayaType : String , //surah or juz
				//	val numberOfType : Int , //surah number or juz number
				//	val TranslationLanguage : String ,
				val aya = Aya(
						id = 0,
						ayaResponse.number ,
						ayaResponse.arabic ,
						ayaResponse.translation ,
						ayaResponse.surahNumber ,
						ayaResponse.ayaNumberInSurah ,
						false ,
						false ,
						ayaResponse.note ,
						ayaResponse.audioFileLocation ,
						ayaResponse.sajda ,
						ayaResponse.sajdaType ,
						ayaResponse.ruku ,
						ayaResponse.juzNumber ,
						"Surah" ,
						surahNumber ,
						languageConverted ,
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
			//create an array list of surah from the response
			val ayas = ArrayList<Aya>()
			for (ayaResponse in response)
			{
				val aya = Aya(
						id = 0,
						ayaResponse.number ,
						ayaResponse.arabic ,
						ayaResponse.translation ,
						ayaResponse.surahNumber ,
						ayaResponse.ayaNumberInSurah ,
						false ,
						false ,
						ayaResponse.note ,
						ayaResponse.audioFileLocation ,
						ayaResponse.sajda ,
						ayaResponse.sajdaType ,
						ayaResponse.ruku ,
						ayaResponse.juzNumber ,
						"Juz" ,
						juzNumber ,
						languageConverted ,
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