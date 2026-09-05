package com.abhishek.zerodroid.core.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Ties a hardware session (radio scan, sensor listener, microphone, camera-free
 * ranging session, etc.) to the host lifecycle so it is released when the app
 * goes to the background and re-acquired when it returns.
 *
 * Behaviour:
 *  - ON_STOP while [isActive] is true: calls [onPause].
 *  - ON_START after such a pause: calls [onResume] (unless [resumeOnForeground] is false).
 *  - Leaving composition: calls [onPause] unconditionally, matching the previous
 *    `DisposableEffect { onDispose { stop() } }` pattern used by every screen.
 *
 * Wardriving intentionally does not use this; it runs a foreground service so
 * that collection continues in the background.
 *
 * @param isActive whether the feature currently holds hardware. Read from the
 * ViewModel state so a session the user stopped manually is not resumed.
 * @param onPause releases the hardware. Must be idempotent.
 * @param onResume re-acquires the hardware after a lifecycle pause.
 * @param resumeOnForeground false for one-shot or peer-coordinated sessions.
 * @param key restart the effect when this changes (e.g. a device address).
 */
@Composable
fun HardwareLifecycleEffect(
    isActive: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit = {},
    resumeOnForeground: Boolean = true,
    key: Any? = Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentIsActive by rememberUpdatedState(isActive)
    val currentOnPause by rememberUpdatedState(onPause)
    val currentOnResume by rememberUpdatedState(onResume)
    val policy = remember(key, resumeOnForeground) { HardwareSessionPolicy(resumeOnForeground) }

    DisposableEffect(lifecycleOwner, key) {
        val observer = LifecycleEventObserver { _, event ->
            val action = when (event) {
                Lifecycle.Event.ON_STOP -> policy.onBackground(currentIsActive)
                Lifecycle.Event.ON_START -> policy.onForeground()
                else -> HardwareSessionAction.NONE
            }
            action.execute(currentOnPause, currentOnResume)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            policy.onDispose().execute(currentOnPause, currentOnResume)
        }
    }
}

private fun HardwareSessionAction.execute(onPause: () -> Unit, onResume: () -> Unit) {
    when (this) {
        HardwareSessionAction.PAUSE, HardwareSessionAction.STOP -> onPause()
        HardwareSessionAction.RESUME -> onResume()
        HardwareSessionAction.NONE -> Unit
    }
}
