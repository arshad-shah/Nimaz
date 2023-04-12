package com.arshadshah.nimaz.data.remote.repositories

import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.data.remote.models.Juz
import com.arshadshah.nimaz.data.remote.models.Surah
import com.arshadshah.nimaz.utils.api.ApiResponse
import com.arshadshah.nimaz.utils.api.NimazServicesImpl
import io.ktor.client.plugins.*

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
							  ) : ApiResponse<ArrayList<Aya>>
	{
		return try
		{
			val responses =
				NimazServicesImpl.getAyaForSurah(surahNumber)
			//get the english response
			val responseEnglish = responses["english"] !!
			val responseUrdu = responses["urdu"] !!
			//create an array list of surah from the response
			val ayas = ArrayList<Aya>()
			for (ayaResponse in responseEnglish)
			{
				//get the urdu response index for the english response
				val indexOfAyaInUrdu =
					responseUrdu.indexOfFirst { it.ayaNumberInQuran == ayaResponse.ayaNumberInQuran }
				val aya = Aya(
						ayaResponse.ayaNumberInQuran ,
						ayaResponse.number ,
						ayaResponse.arabic ,
						ayaResponse.translation ,
						responseUrdu[indexOfAyaInUrdu].translation ,
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
							) : ApiResponse<ArrayList<Aya>>
	{
		return try
		{
			val responses = NimazServicesImpl.getAyaForJuz(juzNumber)
			//create an array list of surah from the response
			val ayas = ArrayList<Aya>()
			val responseEnglish = responses["english"] !!
			val responseUrdu = responses["urdu"] !!
			for (ayaResponse in responseEnglish)
			{
				//get the urdu response index for the english response
				val indexOfAyaInUrdu =
					responseUrdu.indexOfFirst { it.ayaNumberInQuran == ayaResponse.ayaNumberInQuran }
				val aya = Aya(
						ayaResponse.ayaNumberInQuran ,
						ayaResponse.number ,
						ayaResponse.arabic ,
						ayaResponse.translation ,
						responseUrdu[indexOfAyaInUrdu].translation ,
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