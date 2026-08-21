package com.arshadshah.nimaz.domain.usecase.licenses

import com.arshadshah.nimaz.domain.model.OpenSourceLibrary
import com.arshadshah.nimaz.domain.repository.LibraryRepository

data class LicensesUseCases(
    val getLibraries: GetLibrariesUseCase,
    val getLibrary: GetLibraryUseCase,
)

class GetLibrariesUseCase(private val repo: LibraryRepository) {
    suspend operator fun invoke(): List<OpenSourceLibrary> = repo.getLibraries()
}

class GetLibraryUseCase(private val repo: LibraryRepository) {
    suspend operator fun invoke(id: Int): OpenSourceLibrary? = repo.getLibrary(id)
}
