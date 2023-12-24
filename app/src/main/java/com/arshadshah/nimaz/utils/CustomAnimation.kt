package com.arshadshah.nimaz.utils

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.Alignment

object CustomAnimation {

    fun fadeIn(duration: Int): EnterTransition =
        expandVertically(
            expandFrom = Alignment.Top,
            animationSpec = tween(durationMillis = duration)
        )

    fun fadeOut(duration: Int): ExitTransition =
        shrinkVertically(
            shrinkTowards = Alignment.Top,
            animationSpec = tween(durationMillis = duration)
        )

    fun expandHorizontally(duration: Int): EnterTransition =
        androidx.compose.animation.expandHorizontally(
            expandFrom = Alignment.CenterHorizontally,
            animationSpec = tween(durationMillis = duration)
        )

    fun shrinkHorizontally(duration: Int): ExitTransition =
        androidx.compose.animation.shrinkHorizontally(
            shrinkTowards = Alignment.CenterHorizontally,
            animationSpec = tween(durationMillis = duration)
        )
}