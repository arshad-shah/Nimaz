package com.arshadshah.nimaz.presentation.update

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.StateFlow

/**
 * The in-app-update seam, as a screen sees it.
 *
 * `InAppUpdateManager` — the Play Core implementation — **stays in `:app` permanently**, because
 * it holds an `Activity` and `docs/ARCHITECTURE.md` §2 lists it with `BootReceiver` and
 * `core/init` as composition-root concerns. But two screens read it, and they do not both live
 * there any more: `AboutScreen` moved to `:feature:about` in PR 14 of #551, and `HomeScreen`
 * follows in a later milestone.
 *
 * So the *port* moved and the implementation did not — the same split `WidgetRefresher` and
 * `CompassSensors` already use, applied to a UI-side seam rather than a domain one. A feature
 * module gets [UpdateState] and [AppUpdateController]; `:app` supplies the object behind them,
 * with its Play Core dependency, at the composition root.
 *
 * Null when no update mechanism is available — a debug build, a test, or a `@Preview`. Every
 * call site already handled that, since the CompositionLocal this replaces defaulted to null.
 */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpdateAvailable : UpdateState

    /**
     * The user requested an update and we're fetching the AppUpdateInfo / bringing
     * up the Play confirmation dialog. This can take several seconds on slow
     * connections, so it's surfaced as a distinct loading state to give immediate
     * feedback that the tap was registered.
     */
    data object Starting : UpdateState
    data object Downloading : UpdateState
    data class Downloaded(val completeUpdate: () -> Unit) : UpdateState
    data object NoUpdateAvailable : UpdateState
    data class Error(val message: String) : UpdateState
}

/**
 * What a screen may do about an update: watch [updateState], ask whether there is one, and start
 * it.
 *
 * Deliberately narrower than `InAppUpdateManager` — **three of its seven members**. The other four
 * (`setUpdateFlowLauncher`, `onUpdateFlowResult`, `checkForStalledUpdate`, `cleanup`) are the Play
 * Core lifecycle hooks `MainActivity` drives, and no screen has ever called one. Completing a
 * downloaded update is not a method either: it arrives as the lambda inside
 * [UpdateState.Downloaded], so a screen cannot complete an update that has not finished
 * downloading.
 */
interface AppUpdateController {
    val updateState: StateFlow<UpdateState>

    /** Asks Play whether an update exists. Result arrives on [updateState]. */
    fun checkForUpdate()

    fun startUpdate()
}

/** Provided by `MainActivity`. Null wherever no update mechanism exists. */
val LocalAppUpdateController = staticCompositionLocalOf<AppUpdateController?> { null }
