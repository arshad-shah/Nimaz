package com.arshadshah.nimaz.ui.components.tasbih

import android.util.Log

//function to clean \n and \t and \r from the string if it exists
fun cleanString(string: String): String {

    Log.d("cleanString", "cleanString: $string")
    //clean any html tags
    val cleanStringFromHtml = string.replace(Regex("<[^>]*>"), "")
    //regex for \r\n
    val cleanAnyMarkers = cleanStringFromHtml.replace("\r\n(", "(")
    Log.d("cleanString Cleaned", "cleanString: $cleanAnyMarkers")
    //clean any \n, \t, \r
    return cleanAnyMarkers
}