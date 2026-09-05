package com.abhishek.zerodroid.core.lifecycle

/**
 * What a screen should do with its hardware session in response to a lifecycle event.
 */
enum class HardwareSessionAction {
    /** Nothing to do. */
    NONE,

    /** Release the hardware because the app went to the background. */
    PAUSE,

    /** Re-acquire the hardware because the app returned to the foreground. */
    RESUME,

    /** Release the hardware because the screen is leaving composition. */
    STOP
}

/**
 * Pure decision logic for pausing and resuming a hardware session (radio scan,
 * sensor listener, microphone, ranging session) across app lifecycle changes.
 *
 * It has no Android dependencies so it can be unit tested. [HardwareLifecycleEffect]
 * feeds it lifecycle events and executes the returned action.
 *
 * @param resumeOnForeground when false, a session paused by backgrounding is not
 * automatically restarted; the user must start it again. Use this for one-shot
 * scans and sessions that need peer coordination (UWB, network sweep).
 */
class HardwareSessionPolicy(private val resumeOnForeground: Boolean = true) {

    /** True while a session is suspended waiting for the app to return to the foreground. */
    var isPausedByLifecycle: Boolean = false
        private set

    /**
     * Called when the app is no longer visible (ON_STOP).
     * @param isActive whether the feature currently holds hardware.
     */
    fun onBackground(isActive: Boolean): HardwareSessionAction {
        if (!isActive) {
            isPausedByLifecycle = false
            return HardwareSessionAction.NONE
        }
        isPausedByLifecycle = resumeOnForeground
        return HardwareSessionAction.PAUSE
    }

    /** Called when the app becomes visible again (ON_START). */
    fun onForeground(): HardwareSessionAction {
        if (!isPausedByLifecycle) return HardwareSessionAction.NONE
        isPausedByLifecycle = false
        return HardwareSessionAction.RESUME
    }

    /** Called when the screen leaves composition. Always releases hardware. */
    fun onDispose(): HardwareSessionAction {
        isPausedByLifecycle = false
        return HardwareSessionAction.STOP
    }

    /**
     * Called when the user manually stops the session while it was paused by the
     * lifecycle, so it must not silently restart on foreground.
     */
    fun onUserStopped() {
        isPausedByLifecycle = false
    }
}
