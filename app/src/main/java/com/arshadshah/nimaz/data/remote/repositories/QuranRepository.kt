package com.arshadshah.nimaz.data.remote.repositories

import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.data.remote.models.Juz
import com.arshadshah.nimaz.data.remote.models.Surah
import com.arshadshah.nimaz.utils.api.ApiResponse
import com.arshadshah.nimaz.utils.api.NimazServicesImpl

object QuranRepository
{

	suspend fun getSurahs() : ApiResponse<ArrayList<Surah>>
	{
		return try
		{
			val response = NimazServicesImpl.getSurahs()
			//create an array list of surah from the response
			val surahs = ArrayList<Surah>()
			response.data?.getAllSura?.map { it ->
				val surah = Surah(
						 it !!.id ,
						 it.ayaAmount ,
						 it.start ,
						 it.name ,
						 it.ename ,
						 it.tname ,
						 it.type ,
						 it.order ,
						 it.rukus
								 )
				surahs.add(surah)
			}
			ApiResponse.Success(surahs)
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
			response.data?.getAllJuz?.map { it ->
				val juz = Juz(
						 it !!.id ,
						 it.name ,
						 it.tname ,
						 it.juzStartAyaInQuran ,
							 )
				Juzs.add(juz)
			}
			ApiResponse.Success(Juzs)
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
			//create an array list of surah from the response
			val ayas = ArrayList<Aya>()
			responses.data?.getListAyaForSura?.map { it ->
				val aya = Aya(
						 it !!.id ,
						 // TODO: Possible issue here
						 it.ayaNumberInSura ,
						 it.arabic ,
						 it.english ,
						 it.urdu ,
						 it.suraNumber ,
						 it.ayaNumberInSura ,
						 bookmark = false ,
						 favorite = false ,
						 note = it.note ,
						 audioFileLocation = it.audioFileLocation ,
						 sajda = it.sajda ,
						 sajdaType = it.sajdaType ,
						 ruku = it.ruku ,
						 juzNumber = it.juzNumber ,
							 )
				ayas.add(aya)
			}
			ApiResponse.Success(ayas)
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
			responses.data?.getListAyaForJuz?.map { it ->
				val aya = Aya(
						 it !!.id ,
						 it.ayaNumberInSura ,
						 it.arabic ,
						 it.english ,
						 it.urdu ,
						 it.suraNumber ,
						 it.ayaNumberInSura ,
						 bookmark = false ,
						 favorite = false ,
						 note = it.note ,
						 audioFileLocation = it.audioFileLocation ,
						 sajda = it.sajda ,
						 sajdaType = it.sajdaType ,
						 ruku = it.ruku ,
						 juzNumber = it.juzNumber ,
							 )
				ayas.add(aya)
			}
			ApiResponse.Success(ayas)
		} catch (e : Exception)
		{
			throw Exception(e.message)
		}
	}
}