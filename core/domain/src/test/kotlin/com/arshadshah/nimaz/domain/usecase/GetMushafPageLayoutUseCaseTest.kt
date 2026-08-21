package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.MushafLine
import com.arshadshah.nimaz.domain.model.MushafLineType
import com.arshadshah.nimaz.domain.model.MushafPageLayout
import com.arshadshah.nimaz.domain.repository.QuranRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetMushafPageLayoutUseCaseTest {

    @Test
    fun `delegates to repository and returns its layout for the requested page`() = runTest {
        val repository = mockk<QuranRepository>()
        val expected = MushafPageLayout(
            page = 1,
            lines = listOf(MushafLine(page = 1, lineNumber = 1, type = MushafLineType.SURAH_HEADER, surahId = 1))
        )
        coEvery { repository.getMushafPageLayout(1) } returns expected

        val result = GetMushafPageLayoutUseCase(repository)(1)

        assertThat(result).isEqualTo(expected)
        coVerify(exactly = 1) { repository.getMushafPageLayout(1) }
    }
}
