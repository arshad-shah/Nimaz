package com.arshadshah.nimaz.data.remote.repositories

import com.arshadshah.nimaz.data.remote.models.Chapter
import com.arshadshah.nimaz.data.remote.models.Dua
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.utils.api.ApiResponse
import com.arshadshah.nimaz.utils.api.NimazServicesImpl
import io.ktor.client.plugins.*
import java.io.IOException

object DuaRepository
{

	//get chaptesr by categories
	suspend fun getChaptersByCategories() : ApiResponse<ArrayList<Map<String , ArrayList<Chapter>>>>
	{
		return try
		{
			val dataStore = LocalDataStore.getDataStore()
			val chaptersCount = dataStore.countChapters()
			val categories = ArrayList<Map<String , ArrayList<Chapter>>>()
			if (chaptersCount == 0)
			{
				val response = NimazServicesImpl.getChapters()
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
										  duaResponse.englishReference ,
										  getCategoriesName(duaResponse.chapterId) ,
									)
								)
					}
					//add the chapter to the list
					val chapter = Chapter(
							 chapterResponse.id ,
							 getCategoriesName(chapterResponse.id) ,
							 chapterResponse.arabicTitle ,
							 chapterResponse.englishTitle ,
										 )
					chapters.add(chapter)
					//save the duas
					dataStore.saveAllDuas(duas)
					//save the chapters
					dataStore.saveAllChapters(chapters)

					//add the chapter to the categories
					val category = HashMap<String , ArrayList<Chapter>>()
					category[chapter.category] = chapters
					categories.add(category)

				}
			} else
			{
				//get the chapters from the database
				val chapters = dataStore.getAllChapters()
				for (chapter in chapters)
				{
					//add the chapter to the categories
					val category = HashMap<String , ArrayList<Chapter>>()
					category[chapter.category] = chapters as ArrayList<Chapter>
					categories.add(category)
				}
			}
			//result
			ApiResponse.Success(categories)
		} catch (e : ClientRequestException)
		{
			ApiResponse.Error(e.message , null)

		} catch (e : IOException)
		{
			ApiResponse.Error(e.message !! , null)
		}
	}

	suspend fun getChaptersByCategory(category : String) : ArrayList<Chapter>
	{
		val dataStore = LocalDataStore.getDataStore()
		return dataStore.getAllChapters().filter { it.category == category } as ArrayList<Chapter>
	}

	//get duas of a chapter by chapter id
	suspend fun getDuasOfChapter(chapterId : Int) : ArrayList<Dua>
	{
		val dataStore = LocalDataStore.getDataStore()
		return dataStore.getDuasOfChapter(chapterId) as ArrayList<Dua>
	}

	//function to return the categories name based on the chapter id
	private fun getCategoriesName(chapterId : Int) : String
	{
		return when (chapterId)
		{
			1 , 27 , 28 , 29 , 30 , 31 -> "Morning and Evening"
			8 , 9 , 10 , 11 , 12 , 13 , 14 , 15 , 16 , 17 , 18 , 19 , 20 , 21 , 22 , 23 , 24 , 25 , 32 , 33 , 42 -> "Prayer"
			26 , 43 , 44 , 46 , 88 , 107 , 129 , 130 , 131 -> "Praising Allah"
			115 , 116 , 117 , 118 , 119 , 120 , 121 , 127 -> "Hajj and Umrah"
			95 , 96 , 97 , 98 , 99 , 100 , 101 , 102 , 103 , 104 , 105 -> "Travel"
			34 , 35 , 36 , 37 , 38 , 39 , 40 , 41 , 53 , 92 , 94 , 106 , 122 , 123 , 126 -> "Joy & Distress"
			61 , 67 , 76 , 110 , 111 -> "Nature"
			47 , 75 , 77 , 78 , 80 , 82 , 83 , 84 , 85 , 86 , 87 , 89 , 90 , 91 , 93 , 108 , 109 , 112 , 113 , 114 -> "Good Etiquette"
			2 , 3 , 4 , 5 , 6 , 7 , 10 , 11 , 45 , 79 , 81 , 125 , 128 , 132 -> "Home & Family"
			68 , 69 , 70 , 71 , 72 , 73 , 74 -> "Food & Drinks"
			48 , 49 , 50 , 51 , 52 , 54 , 55 , 56 , 57 , 58 , 59 , 60 , 124 -> "Sickness & Death"
			else -> ""
		}
	}

	suspend fun getAllChapters() : ArrayList<Chapter>
	{
		val dataStore = LocalDataStore.getDataStore()
		return dataStore.getAllChapters() as java.util.ArrayList<Chapter>
	}
}