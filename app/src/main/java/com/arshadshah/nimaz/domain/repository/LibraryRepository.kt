package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.OpenSourceLibrary

/** The open-source dependencies the app ships, and the licences they carry. */
interface LibraryRepository {

    /** Every bundled dependency, ordered by name. */
    suspend fun getLibraries(): List<OpenSourceLibrary>

    /** One dependency by its [OpenSourceLibrary.id], or null when nothing matches. */
    suspend fun getLibrary(id: Int): OpenSourceLibrary?
}
