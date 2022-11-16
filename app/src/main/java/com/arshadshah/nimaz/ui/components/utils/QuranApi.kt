package com.arshadshah.nimaz.ui.components.utils

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

class QuranApi(context: Context) {
    val sharedPreferences = PrivateSharedPreferences(context)

    private val ipAddress: String = "178.62.35.217"
    private val port: String = "8080"
    private val protocol: String = "http"


    /**
     * request prayer times from the API
     * */
    fun getAllSurahs(context: Context) {
        //call the server to get the prayer time
        val requestMethod = Request.Method.GET
        val url = "$protocol://$ipAddress:$port/quran/getallsurahs"

        val queue = Volley.newRequestQueue(context)
        val stringRequest = StringRequest(
            requestMethod, url,
            { response ->
                val correctUnicode =
                    String(response.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
                Log.d("QuranApi", "Response is: $correctUnicode")
                sharedPreferences.saveData("surahList", correctUnicode)
            },
            { error ->
                Log.d("QuranApi", "That didn't work! $error")
            }
        )
        queue.add(stringRequest)
    }

    /**
     * Get all juz from the API
     * */
    fun getAllJuz(context: Context) {
        //call the server to get the prayer time
        val requestMethod = Request.Method.GET
        val url = "$protocol://$ipAddress:$port/quran/getalljuz"

        val queue = Volley.newRequestQueue(context)
        val stringRequest = StringRequest(
            requestMethod, url,
            { response ->
                val correctUnicode =
                    String(response.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
                Log.d("QuranApi", "Response is: $correctUnicode")
                sharedPreferences.saveData("juzList", correctUnicode)
            },
            { error ->
                Log.d("QuranApi", "That didn't work! $error")
            }
        )
        queue.add(stringRequest)
    }

    /**
     * Get all ayat for a surah
     * */
    fun getAyatForSurah(context: Context, surahNumber: Int, isEnglish: Boolean) {
        //call the server to get the prayer time
        val requestMethod = Request.Method.GET
        val url =
            "$protocol://$ipAddress:$port/quran/getsurah?surahNumber=$surahNumber&isEnglish=$isEnglish"

        val queue = Volley.newRequestQueue(context)
        val stringRequest = StringRequest(
            requestMethod, url,
            { response ->
                val correctUnicode =
                    String(response.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
                Log.d("QuranApi", "Response is: $correctUnicode")
                sharedPreferences.saveData("surahAyatList", correctUnicode)
            },
            { error ->
                Log.d("QuranApi", "That didn't work! $error")
            }
        )
        queue.add(stringRequest)
    }

    /**
     * Get all ayat for a juz
     * */
    fun getAyatForJuz(context: Context, juzNumber: Int, isEnglish: Boolean) {
        //call the server to get the prayer time
        val requestMethod = Request.Method.GET
        val url =
            "$protocol://$ipAddress:$port/quran/getjuz?juzNumber=$juzNumber&isEnglish=$isEnglish"

        val queue = Volley.newRequestQueue(context)
        val stringRequest = StringRequest(
            requestMethod, url,
            { response ->
                val correctUnicode =
                    String(response.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
                Log.d("QuranApi", "Response is: $correctUnicode")
                sharedPreferences.saveData("juzAyatList", correctUnicode)
            },
            { error ->
                Log.d("QuranApi", "That didn't work! $error")
            }
        )
        queue.add(stringRequest)
    }

}