
package com.buzbuz.smartautoclicker.core.common.permissions.model

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

sealed class Permission(internal val isOptional: Boolean) {

    internal fun hasBeenRequestedBefore(context: Context): Boolean =
        context.getPermissionSharedPrefs().getBoolean(javaClass.simpleName, false)

    /** Tells if the [Permission] is granted. */
    fun checkIfGranted(context: Context): Boolean {
        if (!isRequiredForAndroidSdkVersion()) return true
        return isGranted(context)
    }

    /** Start the request ui flow for the permission, if required. */
    internal fun startRequestFlowIfNeeded(context: Context): Boolean {
        if (!isRequiredForAndroidSdkVersion()) return true

        val result = onStartRequestFlow(context)

        context.getPermissionSharedPrefs()
            .edit()
            .putBoolean(javaClass.simpleName, true)
            .apply()

        return result
    }

    protected abstract fun isGranted(context: Context): Boolean
    protected abstract fun onStartRequestFlow(context: Context): Boolean

    /** The permission isn't using the standard Android system, such as overlay, accessibility service ... */
    sealed class Special(isOptional: Boolean) : Permission(isOptional)

    /** The permission requires the standard Android permission dialog display. */
    sealed class Dangerous(isOptional: Boolean) : Permission(isOptional) {

        /** Launcher for requesting the permission. */
        private var permissionLauncher: ActivityResultLauncher<String>? = null
        /** The Android permission string value. */
        protected abstract val permissionString: String

        protected open val fallbackSettingsIntent: Intent? = null

        internal fun initResultLauncher(fragment: Fragment, onResult: (isGranted: Boolean) -> Unit) {
            permissionLauncher = fragment
                .registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    onResult(granted)
                }
        }

        override fun onStartRequestFlow(context: Context): Boolean {
            if (!hasBeenRequestedBefore(context)) {
                return permissionLauncher?.let { launcher ->
                    try {
                        launcher.launch(permissionString)
                        true
                    } catch (isEx: IllegalStateException) {
                        Log.e("PermissionDangerous", "Can't start permission request", isEx)
                        false
                    }

                } ?: false
            }

            fallbackSettingsIntent?.let { intent ->
                try {
                    context.startActivity(intent)
                    return true
                } catch (ex: Exception) {
                    Log.e("PermissionDangerous", "Can't start permission settings", ex)
                }
            }

            return false
        }
    }

    /** Declares a permission that should be requested only for a certain Android SDK. */
    internal interface ForApiRange {
        /** The starting API lvl, included in range. */
        val fromApiLvl: Int
            get() = ANDROID_API_LVL_MIN
        /** The ending API lvl, excluded in range. */
        val toApiLvl: Int
            get() = ANDROID_API_LVL_MAX
    }

    protected companion object {
        const val TAG = "Permission"

        private const val ANDROID_API_LVL_MIN = 0
        private const val ANDROID_API_LVL_MAX = Int.MAX_VALUE
    }

    /** Tells if the [Permission] should be requested given the current Android SDK version*/
    @SuppressLint("ObsoleteSdkInt")
    private fun isRequiredForAndroidSdkVersion(): Boolean =
        if (this is ForApiRange) Build.VERSION.SDK_INT in fromApiLvl..<toApiLvl
        else true
}

/** Get the shared preferences file for the permissions. */
private fun Context.getPermissionSharedPrefs() =
    getSharedPreferences("permissions", Context.MODE_PRIVATE)