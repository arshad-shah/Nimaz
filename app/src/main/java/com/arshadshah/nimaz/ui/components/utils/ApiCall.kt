package com.arshadshah.nimaz.ui.components.utils

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

fun apiCall(
    context: Context,
    url: String,
    requestMethod: String,
    mapOfBody: Map<String, String>? = null,
    keyForPref: String,
    loggingKey: String
) {
    val sharedPreferences = PrivateSharedPreferences(context)
    val requestQueue = Volley.newRequestQueue(context)

    var requestMethodValue = 0

    when (requestMethod) {
        "POST" -> {
            requestMethodValue = Request.Method.POST
        }
        "GET" -> {
            requestMethodValue = Request.Method.GET
        }
        "PUT" -> {
            requestMethodValue = Request.Method.PUT
        }
        "DELETE" -> {
            requestMethodValue = Request.Method.DELETE
        }
    }

    //content type for the request body is application form url encoded
    val contentType = "application/json"
    //if the Map is not null then we are sending a body
    //create a body for the request
    val bodyOfRequest = if (mapOfBody != null) {
        val body = StringBuilder()
        for ((key, value) in mapOfBody) {
            body.append("$key=$value&")
        }
        body.toString().trimIndent().trim()
    } else {
        ""
    }

    val stringRequest = object : StringRequest(requestMethodValue, url,
        { response ->
            sharedPreferences.saveData(keyForPref, response)
            Log.d(loggingKey, response)
        },
        { error ->
            Log.d(loggingKey, error.toString())
        }) {
        override fun getBody(): ByteArray {
            return bodyOfRequest.toByteArray()
        }

        override fun getBodyContentType(): String {
            return contentType
        }
    }
    requestQueue.add(stringRequest)
}