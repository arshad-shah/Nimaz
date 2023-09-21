package com.arshadshah.nimaz.data.remote.repositories

import com.arshadshah.nimaz.data.remote.models.Category
import com.arshadshah.nimaz.data.remote.models.Chapter
import com.arshadshah.nimaz.data.remote.models.Dua
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.utils.api.ApiResponse
import com.arshadshah.nimaz.utils.api.NimazServicesImpl
import java.io.IOException

object DuaRepository
{

	//get chaptesr by categories
	suspend fun getCategories() : ApiResponse<ArrayList<Category>>
	{
		return try
		{
			val dataStore = LocalDataStore.getDataStore()
			val categoryCount = dataStore.countCategories()
			val categories = ArrayList<Category>()
			if (categoryCount == 0)
			{
				//get the categories from the api
				val response = NimazServicesImpl.getCategories()
				response.data?.getAllCategories?.map { category ->
					val category = Category(
							 category !!.id ,
							 category.name ,
										   )
					categories.add(category)
				}
				//save the categories to the database
				dataStore.saveAllCategories(categories)
				ApiResponse.Success(dataStore.getAllCategories())
			}
			//result
			ApiResponse.Success(dataStore.getAllCategories() as ArrayList<Category>)
		} catch (e : IOException)
		{
			ApiResponse.Error(e.message !! , null)
		}
	}

	suspend fun getChaptersByCategory(id : Int) : ApiResponse<ArrayList<Chapter>>
	{
		return try
		{
			val dataStore = LocalDataStore.getDataStore()
			val chapterCount = dataStore.countChapters()
			val chapters = ArrayList<Chapter>()
			val duas = ArrayList<Dua>()
			if (chapterCount == 0)
			{
				//get the categories from the api
				val response = NimazServicesImpl.getChaptersByCategory(id)
				response.data?.getChaptersByCategory?.map { chapter ->
					val category = Chapter(
							 chapter !!.id ,
							 chapter.category !!.name ,
							 chapter.arabicTitle.toString() ,
							 chapter.englishTitle.toString()
										  )
					chapters.add(category)

					chapter.duas?.map { dua ->
						val dua = Dua(
								 dua !!.id ,
								 chapter.id ,
								 dua.favourite ,
								 dua.arabicDua ,
								 dua.englishTranslation ,
								 dua.englishReference ,
								 chapter.category.name
									 )
						duas.add(dua)
					}
				}
				//save the categories to the database
				dataStore.saveAllChapters(chapters)
				dataStore.saveAllDuas(duas)
				ApiResponse.Success(dataStore.getAllChapters() as ArrayList<Chapter>)
			}
			//result
			ApiResponse.Success(dataStore.getAllChapters() as ArrayList<Chapter>)
		} catch (e : IOException)
		{
			ApiResponse.Error(e.message !! , null)
		}
	}

	//get duas of a chapter by chapter id
	suspend fun getDuasOfChapter(chapterId : Int) : ApiResponse<ArrayList<Dua>>
	{
		return try
		{
			val dataStore = LocalDataStore.getDataStore()
			val duas = ArrayList<Dua>()
			val duaCount = dataStore.countDuas()
			if (duaCount == 0)
			{
				//get the duas from the api
				val response = NimazServicesImpl.getDuasForChapter(chapterId)
				response.data?.getChapterById?.map { dua ->
					val dua = Dua(
							 dua !!.id ,
							 chapterId ,
							 dua.favourite ,
							 dua.arabicDua ,
							 dua.englishTranslation ,
							 dua.englishReference ,
							 response.data !!.getCategoryNameForAChapter !!
								 )
					duas.add(dua)
				}
				//save the duas to the database
				dataStore.saveAllDuas(duas)
				//get the duas from the database
				duas.addAll(dataStore.getDuasOfChapter(chapterId))
			} else
			{
				//get the duas from the database
				duas.addAll(dataStore.getDuasOfChapter(chapterId))
			}
			//result
			return ApiResponse.Success(duas)
		} catch (e : IOException)
		{
			ApiResponse.Error(e.message !! , null)
		}

	}
}