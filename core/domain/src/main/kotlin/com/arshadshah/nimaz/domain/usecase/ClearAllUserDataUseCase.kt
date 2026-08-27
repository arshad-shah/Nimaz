package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.repository.UserDataRepository
import javax.inject.Inject

/**
 * Deletes everything the person made, leaving shipped content untouched.
 * See [UserDataRepository] for what that boundary covers.
 */
class ClearAllUserDataUseCase @Inject constructor(
    private val repository: UserDataRepository
) {
    suspend operator fun invoke() = repository.clearAllUserData()
}
