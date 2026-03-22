package expo.modules.dynamicappicon

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import expo.modules.core.interfaces.ReactActivityLifecycleListener

object IconState {
    var packageName: String = ""
    var icon: String = ""
    var pm: PackageManager? = null
    var shouldChangeIcon: Boolean = false
    var isInBackground: Boolean = true
}

class ExpoDynamicAppIconReactActivityLifecycleListener : ReactActivityLifecycleListener {

    private var currentActivity: Activity? = null
    private var isBackground = false
    private val handler = Handler(Looper.getMainLooper())
    private val backgroundCheckRunnable = Runnable {
        if (isBackground) {
            onBackground()
        }
    }

    override fun onPause(activity: Activity) {
        currentActivity = activity
        isBackground = true
        // [I4] Remove any pending runnable before posting a new one to prevent duplicates
        handler.removeCallbacks(backgroundCheckRunnable)
        synchronized(IconState) {
            if (IconState.shouldChangeIcon) {
                val delay = if (IconState.isInBackground) 5000L else 0L
                handler.postDelayed(backgroundCheckRunnable, delay)
            }
        }
    }

    override fun onResume(activity: Activity) {
        currentActivity = activity
        isBackground = false
        handler.removeCallbacks(backgroundCheckRunnable)
    }

    override fun onDestroy(activity: Activity) {
        handler.removeCallbacks(backgroundCheckRunnable)
        // [C2] Copy state under lock, then do PM work outside the lock to avoid ANR
        val (shouldChange, icon, pm, packageName) = synchronized(IconState) {
            val result = IconChangeRequest(
                IconState.shouldChangeIcon,
                IconState.icon,
                IconState.pm,
                IconState.packageName
            )
            if (result.shouldChange) {
                IconState.shouldChangeIcon = false
            }
            result
        }
        if (shouldChange) {
            applyIconChange(activity, icon, pm, packageName)
        }
        if (currentActivity === activity) {
            currentActivity = null
        }
    }

    private fun onBackground() {
        currentActivity?.let { activity ->
            // [C2] Copy state under lock, then do PM work outside the lock to avoid ANR
            val (shouldChange, icon, pm, packageName) = synchronized(IconState) {
                val result = IconChangeRequest(
                    IconState.shouldChangeIcon,
                    IconState.icon,
                    IconState.pm,
                    IconState.packageName
                )
                if (result.shouldChange) {
                    IconState.shouldChangeIcon = false
                }
                result
            }
            if (shouldChange) {
                applyIconChange(activity, icon, pm, packageName)
            }
        }
    }

    private data class IconChangeRequest(
        val shouldChange: Boolean,
        val icon: String,
        val pm: PackageManager?,
        val packageName: String
    )

    private fun applyIconChange(activity: Activity, icon: String, pm: PackageManager?, packageName: String) {
        if (icon.isEmpty()) return
        val pmNonNull = pm ?: return

        val newComponent = ComponentName(packageName, icon)

        if (!doesComponentExist(pmNonNull, packageName, newComponent)) {
            Log.e("IconChange", "Component does not exist: $icon")
            return
        }

        try {
            // IMPORTANT: Enable the target icon FIRST to prevent "app not installed" state
            pmNonNull.setComponentEnabledSetting(
                newComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            Log.i("IconChange", "Enabled new icon: $icon")

            // Then disable all other launcher activities
            val packageInfo = pmNonNull.getPackageInfo(
                packageName,
                PackageManager.GET_ACTIVITIES or PackageManager.MATCH_DISABLED_COMPONENTS
            )

            // [S5] Use startsWith for precise matching instead of contains
            val mainActivityPrefix = "$packageName.MainActivity"
            packageInfo.activities?.forEach { activityInfo ->
                if (activityInfo.name.startsWith(mainActivityPrefix) && activityInfo.name != icon) {
                    try {
                        pmNonNull.setComponentEnabledSetting(
                            ComponentName(packageName, activityInfo.name),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    } catch (e: Exception) {
                        Log.w("IconChange", "Failed to disable component: ${activityInfo.name}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("IconChange", "Error during icon change", e)
            ensureAtLeastOneComponentEnabled(pmNonNull, packageName)
        }
    }

    private fun ensureAtLeastOneComponentEnabled(pm: PackageManager, packageName: String) {
        try {
            val packageInfo = pm.getPackageInfo(
                packageName,
                PackageManager.GET_ACTIVITIES or PackageManager.MATCH_DISABLED_COMPONENTS
            )

            val mainActivityPrefix = "$packageName.MainActivity"
            val hasEnabled = packageInfo.activities?.any { activityInfo ->
                activityInfo.name.startsWith(mainActivityPrefix) &&
                    pm.getComponentEnabledSetting(
                        ComponentName(packageName, activityInfo.name)
                    ) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } ?: false

            if (!hasEnabled) {
                val mainComponent = ComponentName(packageName, "$packageName.MainActivity")
                pm.setComponentEnabledSetting(
                    mainComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                Log.i("IconChange", "Fallback: re-enabled $packageName.MainActivity")
            }
        } catch (e: Exception) {
            Log.e("IconChange", "Error in ensureAtLeastOneComponentEnabled", e)
        }
    }

    private fun doesComponentExist(pm: PackageManager, packageName: String, componentName: ComponentName): Boolean {
        return try {
            val packageInfo = pm.getPackageInfo(
                packageName,
                PackageManager.GET_ACTIVITIES or PackageManager.MATCH_DISABLED_COMPONENTS
            )
            packageInfo.activities?.any { it.name == componentName.className } == true
        } catch (e: Exception) {
            false
        }
    }
}
