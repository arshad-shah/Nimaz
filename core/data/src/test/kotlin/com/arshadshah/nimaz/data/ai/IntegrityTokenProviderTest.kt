package com.arshadshah.nimaz.data.ai

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The attestation token that decides whether Ask with Proof answers at all.
 *
 * Everything this class does fails *silently and expensively*. The Worker fails **closed** on a
 * missing token — anyone could omit one — so an empty token is not "verification could not run",
 * it is `ATTESTATION_FAILED`, and the user loses the answer they asked for. What is pinned:
 *
 *  - **the provider is prepared once and reused.** The standard API exists because the classic
 *    one throttles per app-instance after a handful of calls: a few asks worked and then every
 *    token fetch failed. Re-preparing on every question would put the throttling straight back;
 *  - **a stale provider is re-prepared once and retried.** Play services updates or evicts its
 *    state, and without the retry every question after that fails until the process restarts;
 *  - **an unconfigured build falls back by build type.** A debug build sends `"debug-skip"`,
 *    which the Worker honours only with `SKIP_ATTESTATION=true`; a release build sends `""`,
 *    which it rejects. Sending `"debug-skip"` from a release build would be an attestation
 *    bypass shipped to users.
 */
@RunWith(RobolectricTestRunner::class)
class IntegrityTokenProviderTest {

    private lateinit var context: Context
    private lateinit var manager: StandardIntegrityManager
    private lateinit var provider: StandardIntegrityTokenProvider

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        manager = mockk(relaxed = true)
        provider = mockk(relaxed = true)
        mockkStatic(IntegrityManagerFactory::class)
        every { IntegrityManagerFactory.createStandard(any()) } returns manager
        every { manager.prepareIntegrityToken(any<PrepareIntegrityTokenRequest>()) } returns
            immediate(provider)
        every { provider.request(any<StandardIntegrityTokenRequest>()) } returns
            immediate(token("a-real-token"))
    }

    @After
    fun tearDown() = unmockkAll()

    // ── the configured path ───────────────────────────────────────────────────

    @Test
    fun `a configured build asks Play services for a token`() = runTest {
        assertThat(providerFor(cloudProjectNumber = 12345L).getToken())
            .isEqualTo("a-real-token")
    }

    @Test
    fun `the heavy warm-up runs once however many questions are asked`() = runTest {
        val subject = providerFor(cloudProjectNumber = 12345L)

        repeat(5) { assertThat(subject.getToken()).isEqualTo("a-real-token") }

        // The classic API throttled after a handful of calls; preparing per question would put
        // that back with extra steps.
        verify(exactly = 1) { manager.prepareIntegrityToken(any<PrepareIntegrityTokenRequest>()) }
        verify(exactly = 5) { provider.request(any<StandardIntegrityTokenRequest>()) }
    }

    @Test
    fun `a provider that has gone stale is re-prepared once and the question retried`() =
        runTest {
            val fresh = mockk<StandardIntegrityTokenProvider>(relaxed = true) {
                every { request(any<StandardIntegrityTokenRequest>()) } returns
                    immediate(token("token-after-retry"))
            }
            every { provider.request(any<StandardIntegrityTokenRequest>()) } returns
                failing(IllegalStateException("provider evicted"))
            every { manager.prepareIntegrityToken(any<PrepareIntegrityTokenRequest>()) } returnsMany
                listOf(immediate(provider), immediate(fresh))

            assertThat(providerFor(12345L).getToken()).isEqualTo("token-after-retry")

            verify(exactly = 2) {
                manager.prepareIntegrityToken(any<PrepareIntegrityTokenRequest>())
            }
        }

    @Test
    fun `a release build that still cannot get a token sends nothing, and is rejected`() =
        runTest {
            every { provider.request(any<StandardIntegrityTokenRequest>()) } returns
                failing(IllegalStateException("offline"))

            // Empty, not "debug-skip": the Worker fails closed on an absent token, and that is
            // the intended outcome for a release build that cannot attest.
            assertThat(providerFor(12345L, isDebugBuild = false).getToken()).isEmpty()
        }

    @Test
    fun `a debug build that cannot get a token sends the skip token`() = runTest {
        every { provider.request(any<StandardIntegrityTokenRequest>()) } returns
            failing(IllegalStateException("no play services"))

        assertThat(providerFor(12345L, isDebugBuild = true).getToken()).isEqualTo("debug-skip")
    }

    @Test
    fun `a warm-up that cannot even start falls back rather than throwing`() = runTest {
        every { manager.prepareIntegrityToken(any<PrepareIntegrityTokenRequest>()) } returns
            failing(IllegalStateException("play services missing"))

        assertThat(providerFor(12345L, isDebugBuild = true).getToken()).isEqualTo("debug-skip")
        assertThat(providerFor(12345L, isDebugBuild = false).getToken()).isEmpty()
    }

    // ── the unconfigured build ────────────────────────────────────────────────

    @Test
    fun `a build with no project number never calls Play services at all`() = runTest {
        assertThat(providerFor(cloudProjectNumber = 0L, isDebugBuild = true).getToken())
            .isEqualTo("debug-skip")

        verify(exactly = 0) { IntegrityManagerFactory.createStandard(any()) }
    }

    @Test
    fun `a release build with no project number sends an empty token`() = runTest {
        // Sending "debug-skip" here would be an attestation bypass shipped to users.
        assertThat(providerFor(cloudProjectNumber = -1L, isDebugBuild = false).getToken())
            .isEmpty()
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun providerFor(cloudProjectNumber: Long, isDebugBuild: Boolean = false) =
        IntegrityTokenProvider(context, cloudProjectNumber, isDebugBuild)

    private fun token(value: String) = mockk<StandardIntegrityToken>(relaxed = true) {
        every { token() } returns value
    }

    /**
     * A `Task` whose listeners fire inline — the real one posts to the main looper, which under
     * Robolectric is the thread the test is suspended on.
     */
    private fun <T> immediate(result: T): Task<T> {
        val task = mockk<Task<T>>(relaxed = true)
        every { task.addOnSuccessListener(any<OnSuccessListener<T>>()) } answers {
            firstArg<OnSuccessListener<T>>().onSuccess(result)
            task
        }
        every { task.addOnFailureListener(any<OnFailureListener>()) } returns task
        return task
    }

    private fun <T> failing(error: Exception): Task<T> {
        val task = mockk<Task<T>>(relaxed = true)
        every { task.addOnSuccessListener(any<OnSuccessListener<T>>()) } returns task
        every { task.addOnFailureListener(any<OnFailureListener>()) } answers {
            firstArg<OnFailureListener>().onFailure(error)
            task
        }
        return task
    }
}
