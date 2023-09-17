package com.arshadshah.nimaz.utils.api

import com.apollographql.apollo3.api.ApolloResponse
import com.arshadshah.nimaz.GetAllAyaForJuzQuery
import com.arshadshah.nimaz.GetAllAyaForSuraQuery
import com.arshadshah.nimaz.GetAllCategoriesQuery
import com.arshadshah.nimaz.GetAllChaptersQuery
import com.arshadshah.nimaz.GetAllJuzQuery
import com.arshadshah.nimaz.GetAllSurahQuery
import com.arshadshah.nimaz.GetChapterByIdQuery
import com.arshadshah.nimaz.GetChaptersByCategoryQuery
import com.arshadshah.nimaz.GetPrayerTimesForMonthQuery
import com.arshadshah.nimaz.type.Parameters


interface NimazService
{
	suspend fun getPrayerTimesMonthlyCustom(
		parameters : Parameters ,
										   ) : ApolloResponse<GetPrayerTimesForMonthQuery.Data>

	suspend fun getSurahs() : ApolloResponse<GetAllSurahQuery.Data>

	suspend fun getJuzs() : ApolloResponse<GetAllJuzQuery.Data>

	suspend fun getAyaForSurah(
		surahNumber : Int ,
							  ) :  ApolloResponse<GetAllAyaForSuraQuery.Data>

	suspend fun getAyaForJuz(
		juzNumber : Int ,
							) : ApolloResponse<GetAllAyaForJuzQuery.Data>

	//get duas chapters
	suspend fun getChapters() : ApolloResponse<GetAllChaptersQuery.Data>

	suspend fun getCategories() : ApolloResponse<GetAllCategoriesQuery.Data>
	suspend fun getChaptersByCategory(id: Int) : ApolloResponse<GetChaptersByCategoryQuery.Data>

	//get duas for chapter
	suspend fun getDuasForChapter(
		chapterId : Int ,
								 ) : ApolloResponse<GetChapterByIdQuery.Data>

}