package com.arshadshah.nimaz.presentation.viewmodel.location

import java.util.Locale
import kotlin.math.abs

/**
 * Geographic grouping for the curated city list shown on the Location screen.
 * `order` controls the display order of region groups in "All" mode.
 */
enum class CityRegion(val label: String, val order: Int) {
    MIDDLE_EAST("Middle East", 0),
    ASIA("Asia", 1),
    EUROPE("Europe", 2),
    AMERICAS("Americas", 3),
    AFRICA("Africa", 4),
}

/**
 * Curated set of notable cities (Muslim-majority + major diaspora), each tagged with a
 * [CityRegion] and its country-flag emoji. Flags are the one sanctioned emoji use in the app
 * (Location screen only) — see docs/ARCHITECTURE.md §7 / §9.
 */
val defaultPopularCities: List<SearchLocation> = listOf(
    // Middle East
    SearchLocation("Mecca", "Saudi Arabia", 21.4225, 39.8262, CityRegion.MIDDLE_EAST, "🇸🇦"),
    SearchLocation("Madinah", "Saudi Arabia", 24.4686, 39.6142, CityRegion.MIDDLE_EAST, "🇸🇦"),
    SearchLocation("Riyadh", "Saudi Arabia", 24.7136, 46.6753, CityRegion.MIDDLE_EAST, "🇸🇦"),
    SearchLocation("Jeddah", "Saudi Arabia", 21.4858, 39.1925, CityRegion.MIDDLE_EAST, "🇸🇦"),
    SearchLocation("Dubai", "United Arab Emirates", 25.2048, 55.2708, CityRegion.MIDDLE_EAST, "🇦🇪"),
    SearchLocation(
        "Abu Dhabi",
        "United Arab Emirates",
        24.4539,
        54.3773,
        CityRegion.MIDDLE_EAST,
        "🇦🇪"
    ),
    SearchLocation("Doha", "Qatar", 25.2854, 51.5310, CityRegion.MIDDLE_EAST, "🇶🇦"),
    SearchLocation("Kuwait City", "Kuwait", 29.3759, 47.9774, CityRegion.MIDDLE_EAST, "🇰🇼"),
    SearchLocation("Manama", "Bahrain", 26.2285, 50.5860, CityRegion.MIDDLE_EAST, "🇧🇭"),
    SearchLocation("Muscat", "Oman", 23.5880, 58.3829, CityRegion.MIDDLE_EAST, "🇴🇲"),
    SearchLocation("Jerusalem", "Palestine", 31.7683, 35.2137, CityRegion.MIDDLE_EAST, "🇵🇸"),
    SearchLocation("Amman", "Jordan", 31.9454, 35.9284, CityRegion.MIDDLE_EAST, "🇯🇴"),
    SearchLocation("Baghdad", "Iraq", 33.3152, 44.3661, CityRegion.MIDDLE_EAST, "🇮🇶"),
    SearchLocation("Tehran", "Iran", 35.6892, 51.3890, CityRegion.MIDDLE_EAST, "🇮🇷"),
    // Asia
    SearchLocation("Istanbul", "Türkiye", 41.0082, 28.9784, CityRegion.ASIA, "🇹🇷"),
    SearchLocation("Karachi", "Pakistan", 24.8607, 67.0011, CityRegion.ASIA, "🇵🇰"),
    SearchLocation("Lahore", "Pakistan", 31.5204, 74.3587, CityRegion.ASIA, "🇵🇰"),
    SearchLocation("Islamabad", "Pakistan", 33.6844, 73.0479, CityRegion.ASIA, "🇵🇰"),
    SearchLocation("Dhaka", "Bangladesh", 23.8103, 90.4125, CityRegion.ASIA, "🇧🇩"),
    SearchLocation("Delhi", "India", 28.6139, 77.2090, CityRegion.ASIA, "🇮🇳"),
    SearchLocation("Hyderabad", "India", 17.3850, 78.4867, CityRegion.ASIA, "🇮🇳"),
    SearchLocation("Jakarta", "Indonesia", -6.2088, 106.8456, CityRegion.ASIA, "🇮🇩"),
    SearchLocation("Kuala Lumpur", "Malaysia", 3.1390, 101.6869, CityRegion.ASIA, "🇲🇾"),
    SearchLocation("Singapore", "Singapore", 1.3521, 103.8198, CityRegion.ASIA, "🇸🇬"),
    // Europe
    SearchLocation("London", "United Kingdom", 51.5074, -0.1278, CityRegion.EUROPE, "🇬🇧"),
    SearchLocation("Birmingham", "United Kingdom", 52.4862, -1.8904, CityRegion.EUROPE, "🇬🇧"),
    SearchLocation("Paris", "France", 48.8566, 2.3522, CityRegion.EUROPE, "🇫🇷"),
    SearchLocation("Berlin", "Germany", 52.5200, 13.4050, CityRegion.EUROPE, "🇩🇪"),
    SearchLocation("Amsterdam", "Netherlands", 52.3676, 4.9041, CityRegion.EUROPE, "🇳🇱"),
    SearchLocation("Brussels", "Belgium", 50.8503, 4.3517, CityRegion.EUROPE, "🇧🇪"),
    SearchLocation("Sarajevo", "Bosnia and Herzegovina", 43.8563, 18.4131, CityRegion.EUROPE, "🇧🇦"),
    // Americas
    SearchLocation("New York", "United States", 40.7128, -74.0060, CityRegion.AMERICAS, "🇺🇸"),
    SearchLocation("Chicago", "United States", 41.8781, -87.6298, CityRegion.AMERICAS, "🇺🇸"),
    SearchLocation("Dearborn", "United States", 42.3223, -83.1763, CityRegion.AMERICAS, "🇺🇸"),
    SearchLocation("Toronto", "Canada", 43.6532, -79.3832, CityRegion.AMERICAS, "🇨🇦"),
    // Africa
    SearchLocation("Cairo", "Egypt", 30.0444, 31.2357, CityRegion.AFRICA, "🇪🇬"),
    SearchLocation("Casablanca", "Morocco", 33.5731, -7.5898, CityRegion.AFRICA, "🇲🇦"),
    SearchLocation("Lagos", "Nigeria", 6.5244, 3.3792, CityRegion.AFRICA, "🇳🇬"),
    SearchLocation("Kano", "Nigeria", 12.0022, 8.5920, CityRegion.AFRICA, "🇳🇬"),
    SearchLocation("Khartoum", "Sudan", 15.5007, 32.5599, CityRegion.AFRICA, "🇸🇩"),
    SearchLocation("Nairobi", "Kenya", -1.2921, 36.8219, CityRegion.AFRICA, "🇰🇪"),
)

/** Groups curated cities by region, preserving [CityRegion.order]; skips regions with no cities. */
fun groupCitiesByRegion(cities: List<SearchLocation>): List<Pair<CityRegion, List<SearchLocation>>> =
    cities.filter { it.region != null }
        .groupBy { it.region!! }
        .toList()
        .sortedBy { it.first.order }

/** Returns all cities when [region] is null, otherwise only that region's cities. */
fun citiesForRegion(cities: List<SearchLocation>, region: CityRegion?): List<SearchLocation> =
    if (region == null) cities else cities.filter { it.region == region }

/** Formats a coordinate pair with sign-derived hemispheres, e.g. "21.4225° N, 39.8262° E". */
fun formatCoordinates(latitude: Double, longitude: Double): String {
    val ns = if (latitude >= 0) "N" else "S"
    val ew = if (longitude >= 0) "E" else "W"
    return String.format(Locale.US, "%.4f° %s, %.4f° %s", abs(latitude), ns, abs(longitude), ew)
}
