package cl.jacevedo.droidcontroller

import android.util.Log

/**
 * Logging wrapper that stays silent in release builds.
 *
 * The app ships without code shrinking, so android.util.Log calls are not
 * stripped automatically. These helpers gate on [BuildConfig.DEBUG] at runtime
 * so BLE payloads, GATT characteristic UUIDs and permission state never reach
 * release logcat.
 */
object DebugLog {
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) Log.e(tag, message, throwable)
    }
}
