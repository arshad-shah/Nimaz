package com.arshadshah.nimaz.behavior

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.support.BaseAppTest
import com.arshadshah.nimaz.support.Selectors
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * First-run experience: with onboarding NOT yet completed, the app must open on the
 * onboarding screen, and completing it (here via "Skip", which fires
 * `OnboardingEvent.CompleteOnboarding`) must navigate to Home *and* persist completion
 * so it never shows again.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest : BaseAppTest() {

    override suspend fun seedState() {
        settings.setOnboardingCompleted(false)
    }

    @Test
    fun freshLaunch_showsOnboarding_thenSkipCompletesToHome() {
        launchApp()
        assertScreen(ScreenTags.Onboarding)

        tapText(Selectors.str(R.string.onboarding_skip))

        assertScreen(ScreenTags.Home)
        assertThat(runBlocking { settings.onboardingCompleted.first() }).isTrue()
    }
}
