package com.arshadshah.nimaz.utils

object StringUtils {

    fun String.cleanTextFromBackslash(): String {
        return this
            .replace("\\\"", "\"")  // Handle escaped quotes first
            .replace("\\\\", "\\")  // Then handle double backslashes
            .replace("\\n", "\n")   // Handle newlines
            .replace("\\t", "\t")   // Handle tabs
            .replace("\\", "")      // Finally remove any remaining single backslashes
    }
}