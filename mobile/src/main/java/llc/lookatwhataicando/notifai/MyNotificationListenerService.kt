package llc.lookatwhataicando.notifai

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.content.Context
import android.os.UserHandle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.smartfoo.android.core.logging.FooLog
import com.smartfoo.android.core.notification.FooNotification
import com.smartfoo.android.core.FooString
import com.smartfoo.android.core.notification.ActiveNotificationsSnapshot

class MyNotificationListenerService : NotificationListenerService() {
    companion object {
        private val TAG = FooLog.TAG(MyNotificationListenerService::class)

        @Suppress("SimplifyBooleanWithConstants", "KotlinConstantConditions", "RedundantSuppression", "UNREACHABLE_CODE")
        private val LOG_NOTIFICATION = true && BuildConfig.DEBUG

        fun isNotificationListenerEnabled(context: Context) =
            FooNotification.isNotificationListenerEnabled(context, MyNotificationListenerService::class.java)

        fun requestNotificationListenerRebind(context: Context) {
            FooNotification.requestNotificationListenerRebind(context, MyNotificationListenerService::class.java)
        }

        fun requestNotificationListenerUnbind(context: Context) {
            FooNotification.requestNotificationListenerUnbind(context, MyNotificationListenerService::class.java)
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

    private val activeNotificationsSnapshot = ActiveNotificationsSnapshot(this)

    private fun getActiveNotificationsSnapshot() = activeNotificationsSnapshot.snapshot(this)

    fun initializeActiveNotifications() {
        val activeNotificationsSnapshot = getActiveNotificationsSnapshot()
        initializeActiveNotifications(activeNotificationsSnapshot)
    }

    private fun initializeActiveNotifications(activeNotificationsSnapshot: ActiveNotificationsSnapshot) {
        if (LOG_NOTIFICATION) {
            Log.d(TAG, "#NOTIFICATION initializeActiveNotifications(activeNotificationsSnapshot(${activeNotificationsSnapshot.activeNotifications?.size}))")
        }
        for (activeNotification in activeNotificationsSnapshot.activeNotificationsRanked.orEmpty()) {
            Log.v(TAG, "#NOTIFICATION initializeActiveNotifications: activeNotification=$activeNotification")
            onNotificationPosted(activeNotification, activeNotificationsSnapshot.currentRanking)
        }
    }

    override fun onListenerConnected() {
        if (LOG_NOTIFICATION) {
            Log.d(TAG, "#NOTIFICATION onListenerConnected()")
        }
        super.onListenerConnected()
        initializeActiveNotifications()
    }

    override fun onListenerDisconnected() {
        if (LOG_NOTIFICATION) {
            Log.d(TAG, "#NOTIFICATION onListenerDisconnected()")
        }
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        if (LOG_NOTIFICATION) {
            Log.d(TAG, "#NOTIFICATION onNotificationPosted(sbn=$sbn, rankingMap=$rankingMap)")
        }
        super.onNotificationPosted(sbn, rankingMap)
        // TODO: Handle incoming notification...
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification?,
        rankingMap: RankingMap?,
        reason: Int
    ) {
        if (LOG_NOTIFICATION) {
            Log.d(TAG, "#NOTIFICATION onNotificationRemoved(sbn=$sbn, rankingMap=$rankingMap, reason=${FooString.quote(FooNotification.notificationCancelReasonToString(reason))})")
        }
        super.onNotificationRemoved(sbn, rankingMap, reason)
        // TODO: Handle removed notification...
    }

    override fun onNotificationRankingUpdate(rankingMap: RankingMap?) {
        if (LOG_NOTIFICATION) {
            Log.v(TAG, "#NOTIFICATION onNotificationRankingUpdate(...)")
        }
        super.onNotificationRankingUpdate(rankingMap)
    }

    override fun onListenerHintsChanged(hints: Int) {
        if (LOG_NOTIFICATION) {
            Log.v(TAG, "#NOTIFICATION onListenerHintsChanged(...)")
        }
        super.onListenerHintsChanged(hints)
    }

    override fun onSilentStatusBarIconsVisibilityChanged(hideSilentStatusIcons: Boolean) {
        if (LOG_NOTIFICATION) {
            Log.v(TAG, "#NOTIFICATION onSilentStatusBarIconsVisibilityChanged(...)")
        }
        super.onSilentStatusBarIconsVisibilityChanged(hideSilentStatusIcons)
    }

    override fun onNotificationChannelModified(
        pkg: String?,
        user: UserHandle?,
        channel: NotificationChannel?,
        modificationType: Int,
    ) {
        if (LOG_NOTIFICATION) {
            Log.v(TAG, "#NOTIFICATION onNotificationChannelModified(...)")
        }
        super.onNotificationChannelModified(pkg, user, channel, modificationType)
    }

    override fun onNotificationChannelGroupModified(
        pkg: String?,
        user: UserHandle?,
        group: NotificationChannelGroup?,
        modificationType: Int,
    ) {
        if (LOG_NOTIFICATION) {
            Log.v(TAG, "#NOTIFICATION onNotificationChannelGroupModified(...)")
        }
        super.onNotificationChannelGroupModified(pkg, user, group, modificationType)
    }

    override fun onInterruptionFilterChanged(interruptionFilter: Int) {
        if (LOG_NOTIFICATION) {
            Log.v(TAG, "#NOTIFICATION onInterruptionFilterChanged(...)")
        }
        super.onInterruptionFilterChanged(interruptionFilter)
    }
}
