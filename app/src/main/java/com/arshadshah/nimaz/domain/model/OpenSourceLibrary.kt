package com.arshadshah.nimaz.domain.model

/**
 * A dependency's licence record, in the shape the About screens need.
 *
 * [id] is derived from the Maven coordinate rather than the source object's hash, so a
 * version bump does not change which library a detail route points at.
 */
data class OpenSourceLibrary(
    val id: Int,
    val name: String,
    val coordinate: String,
    val version: String?,
    val author: String?,
    val website: String?,
    val licenses: List<LibraryLicense>,
) {
    /**
     * The licence family the list groups, filters and colours by.
     *
     * A dependency may declare several licences (dual-licensed, or a licence plus an
     * exception). The first is the one the list speaks for — showing a library twice, once
     * per licence, would make the section counts disagree with the library count.
     */
    val family: LicenseFamily get() = licenses.firstOrNull()?.family ?: LicenseFamily.OTHER

    /**
     * The Maven group, or null for anything not published as `group:artifact`.
     *
     * Shown as the detail screen's "coordinate" row so a reader can tell two artifacts with
     * the same short name apart.
     */
    val group: String? get() = coordinate.substringBeforeLast(':', "").takeIf { it.isNotEmpty() }
}

/** One licence attached to an [OpenSourceLibrary]; a dependency may carry several. */
data class LibraryLicense(
    val name: String,
    val url: String?,
    val content: String?,
) {
    /** Which [LicenseFamily] this licence's name places it in. */
    val family: LicenseFamily get() = LicenseFamily.of(name)
}
