package com.arshadshah.nimaz.utils

class ErrorDetector
{

	/**
	 * An arabic text error detector.
	 * it detects and highlights errors in arabic text.
	 */
	fun errorDetector(correct : String , incorrect : String ) : String
	{
		val correctWords = correct.split(" ")
		val incorrectWords = incorrect.split(" ").toMutableList()
		var errorEncountered = false
		var i = 0
		while (i < correctWords.size)
		{
			if (i >= incorrectWords.size)
			{
				break
			}
			if (correctWords[i] != incorrectWords[i])
			{
				if (!errorEncountered)
				{
					errorEncountered = true
				}
				incorrectWords[i] = "\u001B[31m" + incorrectWords[i] + "\u001B[0m"
			}
			i++
		}
		return incorrectWords.joinToString(" ")
	}
}