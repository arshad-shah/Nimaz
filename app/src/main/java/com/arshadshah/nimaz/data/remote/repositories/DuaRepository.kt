package com.arshadshah.nimaz.data.remote.repositories

import com.arshadshah.nimaz.data.remote.models.Chapter
import com.arshadshah.nimaz.data.remote.models.Dua
import com.arshadshah.nimaz.utils.api.ApiResponse
import com.arshadshah.nimaz.utils.api.NimazServicesImpl
import io.ktor.client.plugins.*
import java.io.IOException

object DuaRepository
{

	/**
	 * Get all the chapters of dua from the API
	 * */
	suspend fun getChapters() : ApiResponse<ArrayList<Chapter>>
	{
		return try
		{
			val response = NimazServicesImpl.getChapters()
			//reponse contains a list of chapters where each chapter has a list of dua
			//arraylist of chapters
			val chapters = ArrayList<Chapter>()
			for (chapterResponse in response)
			{
				//map the duas of each chapter to a list of Dua
				val duas = ArrayList<Dua>()
				for (duaResponse in chapterResponse.duas)
				{
					duas.add(
							Dua(
									duaResponse.id ,
									duaResponse.chapterId ,
									duaResponse.favourite ,
									duaResponse.arabicDua ,
									duaResponse.englishTranslation ,
									duaResponse.englishReference
							   )
							)
				}
				//add the chapter to the list
				chapters.add(
						Chapter(
								chapterResponse.id ,
								chapterResponse.arabicTitle ,
								chapterResponse.englishTitle ,
								duas
							   )
							)
			}
			ApiResponse.Success(chapters)
		} catch (e : ClientRequestException)
		{
			ApiResponse.Error(e.message , null)

		} catch (e : IOException)
		{
			ApiResponse.Error(e.message !! , null)
		}
	}
}