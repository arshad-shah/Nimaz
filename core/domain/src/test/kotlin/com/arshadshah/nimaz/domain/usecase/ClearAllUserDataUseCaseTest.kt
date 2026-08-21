package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.repository.UserDataRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * "Delete all my data" used to be eleven `userDatabase.xxxDao()` calls written out inside
 * `SettingsViewModel` — a Room database injected into the presentation layer, and a list a
 * future table could silently fall off the end of.
 *
 * The operation now belongs to one repository method behind one use case. What this test
 * pins is the contract the ViewModel depends on: invoking the use case clears user data,
 * and does so exactly once.
 */
class ClearAllUserDataUseCaseTest {

    private class RecordingUserDataRepository : UserDataRepository {
        var clearCount = 0
            private set

        override suspend fun clearAllUserData() {
            clearCount++
        }
    }

    @Test
    fun `invoking the use case clears user data`() = runTest {
        val repository = RecordingUserDataRepository()

        ClearAllUserDataUseCase(repository)()

        assertThat(repository.clearCount).isEqualTo(1)
    }

    @Test
    fun `each invocation clears again`() = runTest {
        val repository = RecordingUserDataRepository()
        val clearAll = ClearAllUserDataUseCase(repository)

        clearAll()
        clearAll()

        assertThat(repository.clearCount).isEqualTo(2)
    }
}
