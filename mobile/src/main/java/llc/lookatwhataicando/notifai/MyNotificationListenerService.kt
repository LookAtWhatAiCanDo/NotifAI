package llc.lookatwhataicando.notifai

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import llc.lookatwhataicando.notifai.util.MyNotificationUtils

class MyNotificationListenerService : NotificationListenerService() {
    companion object {
        private const val TAG = "MyNotificationListenerService"

        fun isNotificationListenerEnabled(context: Context) =
            MyNotificationUtils.isNotificationListenerEnabled(context, MyNotificationListenerService::class.java)

        fun requestNotificationListenerRebind(context: Context) {
            MyNotificationUtils.requestNotificationListenerRebind(context, MyNotificationListenerService::class.java)
        }

        fun requestNotificationListenerUnbind(context: Context) {
            MyNotificationUtils.requestNotificationListenerUnbind(context, MyNotificationListenerService::class.java)
        }
    }

    override fun onCreate() {
        Log.d(TAG, "+onCreate()")
        super.onCreate()
        Log.d(TAG, "-onCreate()")
    }

    override fun onDestroy() {
        Log.d(TAG, "+onDestroy()")
        super.onDestroy()
        Log.d(TAG, "-onDestroy()")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        Log.d(TAG, "onNotificationPosted(sbn=$sbn)")
        // Handle incoming notifications
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        Log.d(TAG, "onNotificationRemoved(sbn=$sbn)")
        // Handle removed notifications
    }
}
