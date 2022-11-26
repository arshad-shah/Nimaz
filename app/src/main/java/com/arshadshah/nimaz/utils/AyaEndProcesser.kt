package com.arshadshah.nimaz.utils

import java.text.NumberFormat
import java.util.*

fun AyaEndProcesser(arabic : String , ayaNumber : Int) : String
{
	val unicodeAyaEndEnd = "\uFD3E"
	val unicodeAyaEndStart = "\uFD3F"
	val arabiclocal = Locale.forLanguageTag("ar")
	val nf : NumberFormat = NumberFormat.getInstance(arabiclocal)
	val endOfAyaWithNumber = nf.format(ayaNumber)
	//remove the comma separator from the number
	var unicodeWithNumber = ""
	//if the endOfAyaWithNumber has ٬ then remove it
	if (endOfAyaWithNumber.contains("٬"))
	{
		val endOfAyaWithNumberNoComma = endOfAyaWithNumber.replace("٬" , "")
		unicodeWithNumber = "$unicodeAyaEndStart$endOfAyaWithNumberNoComma$unicodeAyaEndEnd"
	} else
	{
		unicodeWithNumber = "$unicodeAyaEndStart$endOfAyaWithNumber$unicodeAyaEndEnd"
	}

	return "$arabic $unicodeWithNumber"
}