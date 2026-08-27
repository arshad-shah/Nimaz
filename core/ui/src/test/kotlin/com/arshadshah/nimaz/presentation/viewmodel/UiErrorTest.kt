package com.arshadshah.nimaz.presentation.viewmodel

import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorKind
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The contract a failing UiState carries. Two properties matter enough to pin: the
 * copy is a resource id, so it is translated, and the exception text is kept apart
 * from it, so it is never what the user reads.
 *
 * The app ships `"Failed to search locations: ${e.message}"` today, in English,
 * exception-shaped, in a build that otherwise translates everything.
 */
class UiErrorTest {

    @Test
    fun `defaults to a generic failure with no technical detail`() {
        val error = UiError(R.string.error_generic)

        assertThat(error.kind).isEqualTo(NimazErrorKind.GENERIC)
        assertThat(error.details).isNull()
    }

    @Test
    fun `an offline failure is a warning, not an alarm`() {
        // A dropped connection is expected and recoverable; rendering it in error-red
        // overstates it. The kind decides that once, not each call site.
        assertThat(NimazErrorKind.OFFLINE.tone).isEqualTo(NimazTone.WARNING)
        assertThat(NimazErrorKind.GENERIC.tone).isEqualTo(NimazTone.ERROR)
        assertThat(NimazErrorKind.NOT_FOUND.tone).isEqualTo(NimazTone.MUTED)
    }

    @Test
    fun `technical detail is carried separately from the readable message`() {
        val error = UiError(
            message = R.string.error_generic,
            kind = NimazErrorKind.SERVER,
            details = "java.net.SocketTimeoutException: timeout",
        )

        assertThat(error.message).isEqualTo(R.string.error_generic)
        assertThat(error.details).isEqualTo("java.net.SocketTimeoutException: timeout")
    }
}
