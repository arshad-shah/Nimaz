package com.arshadshah.nimaz.presentation.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.arshadshah.nimaz.feature.onboarding.R
import com.arshadshah.nimaz.presentation.theme.NimazColors

/** Decorative only: localized copy and all interactions remain native Compose. */
internal enum class OnboardingIllustration(@DrawableRes val resource: Int) {
    WELCOME(R.drawable.onboarding_welcome),
    PRAYER(R.drawable.onboarding_prayer),
    LEARNING(R.drawable.onboarding_learning),
    QURAN(R.drawable.onboarding_quran),
    PROGRESS(R.drawable.onboarding_progress),
    PERMISSIONS(R.drawable.onboarding_permissions),
}

@Composable
internal fun IllustratedOnboardingBackground(
    illustration: OnboardingIllustration,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Image(
            painter = painterResource(illustration.resource),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to NimazColors.OnboardingBgTop.copy(alpha = 0.75f),
                    0.3f to NimazColors.OnboardingBgTop.copy(alpha = 0.15f),
                    0.65f to NimazColors.OnboardingBgTop.copy(alpha = 0.05f),
                    1f to NimazColors.OnboardingBgTop.copy(alpha = 0.95f),
                )
            )
        )
    }
}
