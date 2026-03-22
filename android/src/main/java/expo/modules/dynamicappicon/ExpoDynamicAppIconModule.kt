package expo.modules.dynamicappicon

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class ExpoDynamicAppIconModule : Module() {

    override fun definition() = ModuleDefinition {
        Name("ExpoDynamicAppIcon")

        // [C1] Keep as synchronous Function on Android — the icon change is deferred to
        // onPause/onDestroy by design. The JS async wrapper resolves this immediately.
        // On iOS, AsyncFunction is used because the icon change happens inline.
        Function("setAppIcon") { name: String?, isInBackground: Boolean? ->
            try {
                val ctx = context
                val pkgName = ctx.packageName
                val pkgManager = currentActivity.packageManager

                synchronized(IconState) {
                    IconState.packageName = pkgName
                    IconState.pm = pkgManager
                    IconState.isInBackground = isInBackground ?: true
                    IconState.shouldChangeIcon = true

                    val targetIcon = if (name == null) {
                        "$pkgName.MainActivity"
                    } else {
                        "$pkgName.MainActivity$name"
                    }

                    // Skip if already set to the same icon
                    val currentEnabled = getEnabledLauncherActivity(pkgManager, pkgName)
                    if (currentEnabled == targetIcon) {
                        IconState.shouldChangeIcon = false
                        return@Function if (name == null) "DEFAULT" else name
                    }

                    IconState.icon = targetIcon
                }

                return@Function if (name == null) "DEFAULT" else name
            } catch (e: Exception) {
                Log.e("ExpoDynamicAppIcon", "Error in setAppIcon", e)
                return@Function false
            }
        }

        Function("getAppIcon") {
            try {
                val pkgName = context.packageName
                val pm = currentActivity.packageManager
                val enabledActivity = getEnabledLauncherActivity(pm, pkgName)

                if (enabledActivity.isEmpty() || enabledActivity == "$pkgName.MainActivity") {
                    return@Function "DEFAULT"
                }

                val parts = enabledActivity.split(".MainActivity")
                return@Function if (parts.size > 1 && parts.last().isNotEmpty()) {
                    parts.last()
                } else {
                    "DEFAULT"
                }
            } catch (e: Exception) {
                Log.e("ExpoDynamicAppIcon", "Error in getAppIcon", e)
                return@Function "DEFAULT"
            }
        }
    }

    /**
     * Query the PackageManager for the currently enabled launcher activity.
     * This reflects actual system state, not module-internal state.
     */
    private fun getEnabledLauncherActivity(pm: PackageManager, packageName: String): String {
        return try {
            val packageInfo = pm.getPackageInfo(
                packageName,
                PackageManager.GET_ACTIVITIES or PackageManager.MATCH_DISABLED_COMPONENTS
            )
            // [I3] Prefer explicitly enabled components over default-enabled ones
            val mainActivityPrefix = "$packageName.MainActivity"
            val explicitlyEnabled = packageInfo.activities?.firstOrNull { activityInfo ->
                val componentName = ComponentName(packageName, activityInfo.name)
                val state = pm.getComponentEnabledSetting(componentName)
                state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED &&
                    activityInfo.name.startsWith(mainActivityPrefix)
            }
            if (explicitlyEnabled != null) {
                return explicitlyEnabled.name
            }
            // Fall back to default-enabled activities
            packageInfo.activities?.firstOrNull { activityInfo ->
                val componentName = ComponentName(packageName, activityInfo.name)
                val state = pm.getComponentEnabledSetting(componentName)
                state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT &&
                    activityInfo.enabled &&
                    activityInfo.name.startsWith(mainActivityPrefix)
            }?.name ?: "$packageName.MainActivity"
        } catch (e: Exception) {
            "$packageName.MainActivity"
        }
    }

    private val context: Context
        get() = requireNotNull(appContext.reactContext) { "React Application Context is null" }

    private val currentActivity
        get() = requireNotNull(appContext.activityProvider?.currentActivity)
}
