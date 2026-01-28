package com.arshadshah.nimaz.data.audio

/**
 * Represents available adhan sounds with both regular and Fajr variants.
 * Fajr adhan includes the additional phrase "الصلاة خير من النوم" (Prayer is better than sleep).
 */
enum class AdhanSound(
    val displayName: String,
    val origin: String,
    val fileName: String,
    val fajrFileName: String,
    val downloadUrl: String,
    val fajrDownloadUrl: String
) {
    MISHARY(
        displayName = "Mishary Rashid Alafasy",
        origin = "Kuwait",
        fileName = "adhan_mishary.mp3",
        fajrFileName = "adhan_mishary_fajr.mp3",
        // Source: Internet Archive - Public domain
        downloadUrl = "https://archive.org/download/AdhanMisharyRashid/Adhan%20Mishary%20Rashid.mp3",
        fajrDownloadUrl = "https://archive.org/download/AdhanFajrAndDuaBySyeikhMisharyRashidAlAfasy/Adhan%20Fajr%20and%20dua%20by%20Syeikh%20Mishary%20Rashid%20Al%20Afasy.mp3"
    ),
    ABDUL_BASIT(
        displayName = "Abdul Basit Abdul Samad",
        origin = "Egypt",
        fileName = "adhan_abdul_basit.mp3",
        fajrFileName = "adhan_abdul_basit_fajr.mp3",
        // Source: Assabile.com (regular), Internet Archive (fajr - uses same as regular since specific fajr not available)
        downloadUrl = "https://media.assabile.com/assabile/adhan_3435370/1a014366658c.mp3",
        fajrDownloadUrl = "https://archive.org/download/Adhan/Abdul-Basit.mp3"
    ),
    MAKKAH(
        displayName = "Makkah (Masjid al-Haram)",
        origin = "Saudi Arabia",
        fileName = "adhan_makkah.mp3",
        fajrFileName = "adhan_makkah_fajr.mp3",
        // Source: Internet Archive
        downloadUrl = "https://archive.org/download/AdzanDariMasjidAlHaramMakkah/Makkah%20Isha%20Azan.mp3",
        fajrDownloadUrl = "https://archive.org/download/AdzanDariMasjidAlHaramMakkah/Adzan%20Subuh%20%28Fajr%29%20Dari%20Masjid%20Al-Haram%2C%20Makkah.mp3"
    ),
    SIMPLE_BEEP(
        displayName = "Simple Beep",
        origin = "System sound",
        fileName = "adhan_beep.mp3",
        fajrFileName = "adhan_beep.mp3", // Same for Fajr
        // Generated programmatically - no download needed
        downloadUrl = "",
        fajrDownloadUrl = ""
    );

    /**
     * Returns the appropriate file name based on whether it's Fajr prayer.
     */
    fun getFileName(isFajr: Boolean): String {
        return if (isFajr) fajrFileName else fileName
    }

    /**
     * Returns the appropriate download URL based on whether it's Fajr prayer.
     */
    fun getDownloadUrl(isFajr: Boolean): String {
        return if (isFajr) fajrDownloadUrl else downloadUrl
    }

    companion object {
        fun fromName(name: String): AdhanSound {
            return entries.find { it.name == name } ?: MISHARY
        }
    }
}
