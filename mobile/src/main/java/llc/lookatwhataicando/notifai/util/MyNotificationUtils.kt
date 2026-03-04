package llc.lookatwhataicando.notifai.util

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.content.ContextCompat

object MyNotificationUtils {
    private val TAG = MyLogUtils.TAG(MyNotificationUtils::class)

    fun notificationCancelReasonToString(reason: Int): String =
        when (reason) {
            NotificationListenerService.REASON_CLICK -> "REASON_CLICK"
            NotificationListenerService.REASON_CANCEL -> "REASON_CANCEL"
            NotificationListenerService.REASON_CANCEL_ALL -> "REASON_CANCEL_ALL"
            NotificationListenerService.REASON_ERROR -> "REASON_ERROR"
            NotificationListenerService.REASON_PACKAGE_CHANGED -> "REASON_PACKAGE_CHANGED"
            NotificationListenerService.REASON_USER_STOPPED -> "REASON_USER_STOPPED"
            NotificationListenerService.REASON_PACKAGE_BANNED -> "REASON_PACKAGE_BANNED"
            NotificationListenerService.REASON_APP_CANCEL -> "REASON_APP_CANCEL"
            NotificationListenerService.REASON_APP_CANCEL_ALL -> "REASON_APP_CANCEL_ALL"
            NotificationListenerService.REASON_LISTENER_CANCEL -> "REASON_LISTENER_CANCEL"
            NotificationListenerService.REASON_LISTENER_CANCEL_ALL -> "REASON_LISTENER_CANCEL_ALL"
            NotificationListenerService.REASON_GROUP_SUMMARY_CANCELED -> "REASON_GROUP_SUMMARY_CANCELED"
            NotificationListenerService.REASON_GROUP_OPTIMIZATION -> "REASON_GROUP_OPTIMIZATION"
            NotificationListenerService.REASON_PACKAGE_SUSPENDED -> "REASON_PACKAGE_SUSPENDED"
            NotificationListenerService.REASON_PROFILE_TURNED_OFF -> "REASON_PROFILE_TURNED_OFF"
            NotificationListenerService.REASON_UNAUTOBUNDLED -> "REASON_UNAUTOBUNDLED"
            NotificationListenerService.REASON_CHANNEL_BANNED -> "REASON_CHANNEL_BANNED"
            NotificationListenerService.REASON_SNOOZED -> "REASON_SNOOZED"
            NotificationListenerService.REASON_TIMEOUT -> "REASON_TIMEOUT"
            NotificationListenerService.REASON_CHANNEL_REMOVED -> "REASON_CHANNEL_REMOVED"
            NotificationListenerService.REASON_CLEAR_DATA -> "REASON_CLEAR_DATA"
            NotificationListenerService.REASON_ASSISTANT_CANCEL -> "REASON_ASSISTANT_CANCEL"
            NotificationListenerService.REASON_LOCKDOWN -> "REASON_LOCKDOWN"
            else -> "UNKNOWN"
        }.let { "$it($reason)" }

    @JvmStatic
    fun intentAppNotificationSettings(context: Context) =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    @JvmStatic
    fun isPostNotificationsPermissionGranted(context: Context) =
        ContextCompat
            .checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    /**
     * Launch the POST_NOTIFICATIONS runtime permission dialog.
     * No-op below API 33 (permission not required).
     */
    @JvmStatic
    fun requestPostNotifications(launcher: ManagedActivityResultLauncher<String, Boolean>) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /**
     * Non-hidden duplicate of [android.app.Notification.FLAG_NO_DISMISS]
     */
    @Suppress("KDocUnresolvedReference")
    const val FLAG_NO_DISMISS = 0x00002000

    @JvmStatic
    fun hasFlags(notification: Notification?, flags: Int, ) =
        notification != null && (notification.flags and flags) != 0

    /**
     * Similar to [androidx.core.app.NotificationCompat.getOngoing]
     */
    @JvmStatic
    fun getNoDismiss(notification: Notification?): Boolean = hasFlags(notification, FLAG_NO_DISMISS)

    @JvmStatic
    fun findCallingAppNotification(
        context: Context,
        notificationId: Int,
    ): Notification? {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        if (notificationManager != null) {
            val activeNotifications = notificationManager.activeNotifications
            if (activeNotifications != null) {
                for (statusBarNotification in activeNotifications) {
                    if (statusBarNotification.id == notificationId) {
                        return statusBarNotification.notification
                    }
                }
            }
        }
        return null
    }

    /**
     * NOTE: Since Android 14 (API34) [androidx.core.app.NotificationCompat.Builder.setOngoing]
     * notifications **CAN** be dismissed by the user...
     *
     * ...unless...
     *
     * [https://www.reddit.com/r/tasker/comments/1fv9ez4/how_to_enable_nondismissible_persistent/](https://www.reddit.com/r/tasker/comments/1fv9ez4/how_to_enable_nondismissible_persistent/)
     *
     * (There are lots of goodies in this article that might be of some help in the future.)
     *
     * To enable:
     *
     * `adb shell appops set --uid ${packageName} SYSTEM_EXEMPT_FROM_DISMISSIBLE_NOTIFICATIONS allow`
     *
     * This will add a `android.app.Notification.FLAG_NO_DISMISS` to the notification that can be seen with:
     *
     * `adb shell dumpsys notification --noredact | grep ${packageName}`
     *
     * To disable:
     *
     * `adb shell appops set --uid ${packageName} SYSTEM_EXEMPT_FROM_DISMISSIBLE_NOTIFICATIONS default`
     */
    @JvmStatic
    fun isCallingAppNotificationNoDismiss(context: Context, notificationId: Int, )
        = getNoDismiss(findCallingAppNotification(context, notificationId))

    /**
     * Needs to be reasonably longer than the app startup time.
     *
     * NOTE1 that the app startup time can be a few seconds when debugging.
     *
     * NOTE2 that this will time out if paused too long at a debug breakpoint while launching.
     */
    @Suppress("ClassName")
    object NOTIFICATION_LISTENER_SERVICE_CONNECTED_TIMEOUT_MILLIS {
        const val NORMAL: Int = 1500
        const val SLOW: Int = 6000

        fun getRecommendedTimeout(slow: Boolean): Int = if (slow) SLOW else NORMAL
    }

    /**
     * Per hidden field [android.provider.Settings.Secure] `ENABLED_NOTIFICATION_LISTENERS`
     */
    const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"

    /**
     * Similar to API27 [android.app.NotificationManager.isNotificationListenerAccessGranted],
     * but not limited to "The listener service must belong to the calling app."
     *
     * Similar to calling [androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages]`.contains(context.packageName)`,
     * but not limited to only package names.
     */
    @JvmStatic
    fun isNotificationListenerEnabled(
        context: Context,
        notificationListenerServiceClass: Class<out NotificationListenerService>,
    ): Boolean {
        val notificationListenerServiceLookingFor =
            ComponentName(context, notificationListenerServiceClass)
        Log.d(TAG, "isNotificationListenerEnabled: notificationListenerServiceLookingFor=$notificationListenerServiceLookingFor")

        val notificationListenersString =
            Settings.Secure.getString(context.contentResolver, ENABLED_NOTIFICATION_LISTENERS)
        if (notificationListenersString != null) {
            val notificationListeners = notificationListenersString.split(':').dropLastWhile { it.isEmpty() }.toTypedArray()
            for (i in notificationListeners.indices) {
                val notificationListener = ComponentName.unflattenFromString(notificationListeners[i])
                Log.d(TAG, "isNotificationListenerEnabled: notificationListeners[$i]=$notificationListener")
                if (notificationListenerServiceLookingFor == notificationListener) {
                    Log.i(TAG, "isNotificationListenerEnabled: found match; return true")
                    return true
                }
            }
        }

        Log.w(TAG, "isNotificationListenerEnabled: found NO match; return false")
        return false
    }


    @JvmStatic
    val intentNotificationListenerSettings
        get() = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    /**
     * Deep-link to the system Notification Listener settings panel.
     * This is the only way to grant/revoke listener access — there is no
     * runtime dialog for it.
     */
    @JvmStatic
    fun startActivityNotificationListenerSettings(context: Context) =
        context.startActivity(intentNotificationListenerSettings
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    /**
     * Deep-link to the per-app notification settings page.
     * Useful as a secondary action when POST_NOTIFICATIONS is granted
     * but the user has manually disabled notification channels.
     */
    @JvmStatic
    fun startActivityAppNotificationSettings(context: Context) =
        context.startActivity(intentAppNotificationSettings(context)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    @JvmStatic
    fun requestNotificationListenerUnbind(
        context: Context,
        notificationListenerServiceClass: Class<out NotificationListenerService>,
    ) {
        runCatching {
            val componentName = ComponentName(context, notificationListenerServiceClass)
            Log.v(TAG, "requestNotificationListenerUnbind: +NotificationListenerService.requestUnbind($componentName)")
            NotificationListenerService.requestUnbind(componentName)
            Log.v(TAG, "requestNotificationListenerUnbind: -NotificationListenerService.requestUnbind($componentName)")
        }.onFailure { throwable ->
            Log.w(TAG, "requestNotificationListenerUnbind: failed", throwable)
        }
    }

    @JvmStatic
    fun requestNotificationListenerRebind(
        context: Context,
        notificationListenerServiceClass: Class<out NotificationListenerService>,
    ) {
        runCatching {
            val componentName = ComponentName(context, notificationListenerServiceClass)
            Log.v(TAG, "requestNotificationListenerRebind: +NotificationListenerService.requestRebind($componentName)")
            NotificationListenerService.requestRebind(componentName)
            Log.v(TAG, "requestNotificationListenerRebind: -NotificationListenerService.requestRebind($componentName)")
        }.onFailure { throwable ->
            Log.w(TAG, "requestNotificationListenerRebind: failed", throwable)
        }
    }

    @JvmStatic
    fun getNotificationChannel(
        context: Context,
        notification: Notification,
    ): NotificationChannel? {
        val channelId = notification.channelId ?: return null
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.getNotificationChannel(channelId)
    }
}