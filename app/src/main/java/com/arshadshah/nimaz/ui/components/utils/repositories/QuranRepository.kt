package com.arshadshah.nimaz.ui.components.utils.repositories

import android.util.Log
import com.arshadshah.nimaz.ui.components.utils.network.ApiResponse
import com.arshadshah.nimaz.ui.components.utils.network.NimazServicesImpl
import com.arshadshah.nimaz.ui.models.Aya
import com.arshadshah.nimaz.ui.models.Juz
import com.arshadshah.nimaz.ui.models.Surah
import io.ktor.client.features.*

object QuranRepository {
    suspend fun getSurahs(): ApiResponse<ArrayList<Surah>> {
        return try {
            val response = NimazServicesImpl.getSurahs()
            Log.d("QuranRepository", "getSurahs: $response")
            //create an array list of surah from the response
            val surahs = ArrayList<Surah>()
            for (surahResponse in response) {
                val surah = Surah(
                    surahResponse.number,
                    surahResponse.numberOfAyahs,
                    surahResponse.startAya,
                    surahResponse.name,
                    surahResponse.englishName,
                    surahResponse.englishNameTranslation,
                    surahResponse.revelationType,
                    surahResponse.revelationOrder,
                    surahResponse.rukus
                )
                surahs.add(surah)
            }
            ApiResponse.Success(surahs)
        } catch (e: ClientRequestException) {
            ApiResponse.Error(e.message, null)

        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }

    suspend fun getJuzs(): ApiResponse<ArrayList<Juz>> {
        return try {
            val response = NimazServicesImpl.getJuzs()
            Log.d("QuranRepository", "getJuzs: $response")
            //create an array list of surah from the response
            val Juzs = ArrayList<Juz>()
            for (juzResponse in response) {
                val juz = Juz(
                    juzResponse.number,
                    juzResponse.name,
                    juzResponse.tname,
                    juzResponse.juzStartAyaInQuran,
                )
                Juzs.add(juz)
            }
            ApiResponse.Success(Juzs)
        } catch (e: ClientRequestException) {
            ApiResponse.Error(e.message, null)

        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }


    suspend fun getAyaForSurah(surahNumber: Int, isEnglish: Boolean): ApiResponse<ArrayList<Aya>> {
        return try {
            val response = NimazServicesImpl.getAyaForSurah(surahNumber, isEnglish)
            Log.d("QuranRepository", "getAyaForSurah: $response")
            //create an array list of surah from the response
            val ayas = ArrayList<Aya>()
            for (ayaResponse in response) {
                val aya = Aya(
                    ayaResponse.number,
                    ayaResponse.arabic,
                    ayaResponse.translation,
                )
                ayas.add(aya)
            }
            ApiResponse.Success(ayas)
        } catch (e: ClientRequestException) {
            ApiResponse.Error(e.message, null)

        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }

    suspend fun getAyaForJuz(juzNumber: Int, isEnglish: Boolean): ApiResponse<ArrayList<Aya>> {
        return try {
            val response = NimazServicesImpl.getAyaForJuz(juzNumber, isEnglish)
            Log.d("QuranRepository", "getAyaForJuz: $response")
            //create an array list of surah from the response
            val ayas = ArrayList<Aya>()
            for (ayaResponse in response) {
                val aya = Aya(
                    ayaResponse.number,
                    ayaResponse.arabic,
                    ayaResponse.translation,
                )
                ayas.add(aya)
            }
            ApiResponse.Success(ayas)
        } catch (e: ClientRequestException) {
            ApiResponse.Error(e.message, null)

        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }
}