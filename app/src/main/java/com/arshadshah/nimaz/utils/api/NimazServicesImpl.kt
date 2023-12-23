package com.arshadshah.nimaz.utils.api

import android.util.Log
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
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.type.Parameters

object NimazServicesImpl : NimazService {

    private val apolloClient by lazy {
        ApolloClientUtil.getApolloClient()
    }

    override suspend fun getPrayerTimesMonthlyCustom(
        parameters: Parameters,
    ): ApolloResponse<GetPrayerTimesForMonthQuery.Data> {
        //create a post request with stuff in body and return the response
        val response: ApolloResponse<GetPrayerTimesForMonthQuery.Data> = apolloClient.query(
            GetPrayerTimesForMonthQuery(parameters)
        )
            .execute()

        Log.d(AppConstants.NIMAZ_SERVICES_IMPL_TAG, "getPrayerTimes: $response")

        return response
    }

    override suspend fun getSurahs(): ApolloResponse<GetAllSurahQuery.Data> {
        //create a get request and return the response
        val response: ApolloResponse<GetAllSurahQuery.Data> =
            apolloClient.query(GetAllSurahQuery()).execute()

        Log.d(AppConstants.NIMAZ_SERVICES_IMPL_TAG, "getSurahs: $response")

        return response
    }

    override suspend fun getJuzs(): ApolloResponse<GetAllJuzQuery.Data> {
        //create a get request and return the response
        val response: ApolloResponse<GetAllJuzQuery.Data> =
            apolloClient.query(GetAllJuzQuery()).execute()

        Log.d(AppConstants.NIMAZ_SERVICES_IMPL_TAG, "getJuzs: $response")

        return response
    }


    override suspend fun getAyaForSurah(
        surahNumber: Int,
    ): ApolloResponse<GetAllAyaForSuraQuery.Data> {
        val response: ApolloResponse<GetAllAyaForSuraQuery.Data> = apolloClient.query(
            GetAllAyaForSuraQuery(surahNumber)
        ).execute()
        Log.d(AppConstants.NIMAZ_SERVICES_IMPL_TAG, "getAyaForSurah: $response")

        return response
    }

    override suspend fun getAyaForJuz(
        juzNumber: Int,
    ): ApolloResponse<GetAllAyaForJuzQuery.Data> {
        val response: ApolloResponse<GetAllAyaForJuzQuery.Data> = apolloClient.query(
            GetAllAyaForJuzQuery(juzNumber)
        ).execute()

        Log.d(AppConstants.NIMAZ_SERVICES_IMPL_TAG, "getAyaForJuz: $response")

        return response
    }

    override suspend fun getCategories(): ApolloResponse<GetAllCategoriesQuery.Data> {
        //create a get request and return the response
        val response: ApolloResponse<GetAllCategoriesQuery.Data> =
            apolloClient.query(GetAllCategoriesQuery()).execute()

        Log.d(AppConstants.NIMAZ_SERVICES_IMPL_TAG, "getCategories: $response")

        return response
    }

    override suspend fun getChaptersByCategory(id: Int): ApolloResponse<GetChaptersByCategoryQuery.Data> {
        //create a get request and return the response
        val response: ApolloResponse<GetChaptersByCategoryQuery.Data> =
            apolloClient.query(GetChaptersByCategoryQuery(id)).execute()

        Log.d(AppConstants.NIMAZ_SERVICES_IMPL_TAG, "getChaptersByCategory: $response")

        return response
    }


    //get all the chapters for duas from api
    override suspend fun getChapters(): ApolloResponse<GetAllChaptersQuery.Data> {
        //create a get request and return the response
        val response: ApolloResponse<GetAllChaptersQuery.Data> =
            apolloClient.query(GetAllChaptersQuery()).execute()

        Log.d(AppConstants.NIMAZ_SERVICES_IMPL_TAG, "getChapters: $response")

        return response
    }

    override suspend fun getDuasForChapter(
        chapterId: Int,
    ): ApolloResponse<GetChapterByIdQuery.Data> {
        //create a get request and return the response
        val response: ApolloResponse<GetChapterByIdQuery.Data> = apolloClient.query(
            GetChapterByIdQuery(chapterId)
        ).execute()

        Log.d(AppConstants.NIMAZ_SERVICES_IMPL_TAG, "getDuasForChapter: $response")

        return response
    }
}