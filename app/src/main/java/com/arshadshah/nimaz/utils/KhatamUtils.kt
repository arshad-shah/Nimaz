package com.arshadshah.nimaz.utils

import androidx.compose.ui.graphics.Color

object KhatamUtils {
    fun calculateDaysElapsed(startDate: String): Int {
        return try {
            val start = java.time.LocalDate.parse(startDate)
            val now = java.time.LocalDate.now()
            java.time.temporal.ChronoUnit.DAYS.between(start, now).toInt() + 1
        } catch (e: Exception) {
            1
        }
    }

    fun calculateDaysRemaining(targetDate: String): Int {
        return try {
            val target = java.time.LocalDate.parse(targetDate)
            val now = java.time.LocalDate.now()
            java.time.temporal.ChronoUnit.DAYS.between(now, target).toInt()
        } catch (e: Exception) {
            0
        }
    }

    fun formatDate(dateString: String): String {
        return try {
            val date = java.time.LocalDate.parse(dateString)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")
            date.format(formatter)
        } catch (e: Exception) {
            dateString
        }
    }

    fun calculateProgress(totalAyasRead: Int): Float {
        return (totalAyasRead.toFloat() / 6236f).coerceIn(0f, 1f)
    }

    fun getProgressColor(progress: Float): Color {
        return when {
            progress < 0.3f -> Color(0xFF4CAF50) // Green
            progress < 0.7f -> Color(0xFF2196F3) // Blue
            else -> Color(0xFF9C27B0) // Purple
        }
    }
}