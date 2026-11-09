package expo.modules.dynamicappicon

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log

/**
 * Trampoline activity for notification taps.
 *
 * Libraries such as Notifee open the app via an explicit activity class. While a non-default icon
 * is active, `MainActivity` is disabled and the enabled launcher entry is an `activity-alias`, so
 * neither can be used as an explicit class. This activity is a real class that is never disabled;
 * it forwards to the currently enabled launcher entry, preserving extras and data.
 *
 * Usage (Notifee): `pressAction: { id: 'default', launchActivity: 'expo.modules.dynamicappicon.NotificationLauncherActivity' }`
 */
class NotificationLauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                intent?.extras?.let { launchIntent.putExtras(it) }
                intent?.data?.let { launchIntent.data = it }
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(launchIntent)
            } else {
                Log.w(TAG, "No launch intent found for $packageName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error forwarding notification open", e)
        }
        finish()
    }

    companion object {
        private const val TAG = "ExpoDynamicAppIcon"
    }
}
