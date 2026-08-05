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
    val version: String?,
    val author: String?,
    val website: String?,
    val licenses: List<LibraryLicense>,
)

/** One licence attached to an [OpenSourceLibrary]; a dependency may carry several. */
data class LibraryLicense(
    val name: String,
    val url: String?,
    val content: String?,
)
