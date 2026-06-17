package com.arshadshah.nimaz

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Empty Hilt-enabled host activity for instrumented Compose tests that need to
 * drive [com.arshadshah.nimaz.core.navigation.NavGraph] directly (e.g. the
 * navigation crawl). Lives in the debug source set so it ships only in the
 * debug/androidTest APK, never in release.
 */
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()
