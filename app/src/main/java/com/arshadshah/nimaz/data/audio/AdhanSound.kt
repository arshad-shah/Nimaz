package com.arshadshah.nimaz.data.audio

enum class AdhanSound(
    val displayName: String,
    val origin: String,
    val fileName: String
) {
    MISHARY("Mishary Rashid Alafasy", "Kuwait", "adhan_mishary.mp3"),
    ABDUL_BASIT("Abdul Basit Abdul Samad", "Egypt", "adhan_abdul_basit.mp3"),
    MAKKAH("Makkah (Masjid al-Haram)", "Saudi Arabia", "adhan_makkah.mp3"),
    SIMPLE_BEEP("Simple Beep", "System sound", "adhan_beep.mp3");

    companion object {
        fun fromName(name: String): AdhanSound {
            return entries.find { it.name == name } ?: MISHARY
        }
    }
}
