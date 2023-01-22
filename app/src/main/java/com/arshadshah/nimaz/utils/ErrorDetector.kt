package com.arshadshah.nimaz.utils

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log

class ErrorDetector
{

	/**
	 * An arabic text error detector.
	 * it detects and highlights errors in arabic text.
	 */
	fun errorDetector(correct: String, incorrect: String): SpannableString {
		//bismillah with and without diacratics
		val diacritics = "[\\u064b\\u064c\\u064d\\u064e\\u064f\\u0650\\u0651\\u0652\\u0640]"
		val correctPlain = correct.replace("ٱ".toRegex(),"ا").replace(diacritics.toRegex(), "")
		val incorrectPlain = incorrect.replace(diacritics.toRegex(), "")

		val correctWords = correct.split(" ").toMutableList()
		val incorrectWords = incorrect.split(" ").toMutableList()

		val spannableString = SpannableString(incorrect)
		var errorEncountered = false
		var i = 0
		while (i < correctWords.size) {
			if (i >= incorrectWords.size) {
				break
			}
			if (correctWords[i] != incorrectWords[i]) {
				if (!errorEncountered) {
					errorEncountered = true
				}
				// Find the starting and ending index of the error word in the incorrect text
				val startIndex = incorrect.indexOf(incorrectWords[i], i)
				val endIndex = startIndex + incorrectWords[i].length
				// Set the text color to red for the error word in the correct text
				spannableString.setSpan(ForegroundColorSpan(Color.RED) , startIndex , endIndex , Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
			}
			i++
		}

		return spannableString
	}

}