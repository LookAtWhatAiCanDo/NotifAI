package llc.lookatwhataicando.notifai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.smartfoo.android.core.logging.FooLog
import com.smartfoo.android.core.notification.FooNotification
import com.smartfoo.android.core.FooString
import llc.lookatwhataicando.notifai.ActiveNotificationsSnapshot

class MyNotificationListenerService : NotificationListenerService() {
    companion object {
        private val TAG = FooLog.TAG(MyNotificationListenerService::class)

        @Suppress("SimplifyBooleanWithConstants", "KotlinConstantConditions", "RedundantSuppression", "UNREACHABLE_CODE")
        private val LOG_NOTIFICATION = true && BuildConfig.DEBUG

        fun isNotificationListenerEnabled(context: Context) =
            FooNotification.isNotificationListenerEnabled(context, MyNotificationListenerService::class)

        /**
         * Do NOT call startForegroundService to start this service manually!
         * NotificationListenerService startup is managed entirely by the system.
         * If there is a need to ensure it is bound, call `MyNotificationListenerService.requestNotificationListenerRebind(context)`.
         */
        fun requestNotificationListenerRebind(context: Context) {
            FooNotification.requestNotificationListenerRebind(context, MyNotificationListenerService::class)
        }

        fun requestNotificationListenerUnbind(context: Context) {
            FooNotification.requestNotificationListenerUnbind(context, MyNotificationListenerService::class)
        }
    }

    override fun onCreate() {
        FooLog.d(TAG, "+onCreate()")
        super.onCreate()
        FooLog.d(TAG, "-onCreate()")
    }

    override fun onDestroy() {
        FooLog.d(TAG, "+onDestroy()")
        super.onDestroy()
        FooLog.d(TAG, "-onDestroy()")
    }

    private val activeNotificationsSnapshot = ActiveNotificationsSnapshot(this)

    private fun getActiveNotificationsSnapshot() = activeNotificationsSnapshot.snapshot(this)

    fun initializeActiveNotifications() {
        if (LOG_NOTIFICATION) {
            FooLog.v(TAG, "#NOTIFICATION +initializeActiveNotifications()")
        }
        val activeNotificationsSnapshot = getActiveNotificationsSnapshot()
        initializeActiveNotifications(activeNotificationsSnapshot)
        if (LOG_NOTIFICATION) {
            FooLog.v(TAG, "#NOTIFICATION -initializeActiveNotifications()")
        }
    }

    private fun initializeActiveNotifications(activeNotificationsSnapshot: ActiveNotificationsSnapshot) {
        if (LOG_NOTIFICATION) {
            FooLog.d(TAG, "#NOTIFICATION initializeActiveNotifications(activeNotificationsSnapshot(${activeNotificationsSnapshot.activeNotifications?.size}))")
        }
        for (activeNotification in activeNotificationsSnapshot.activeNotificationsRanked.orEmpty()) {
            //Log.v(TAG, "#NOTIFICATION initializeActiveNotifications: activeNotification=${toString(activeNotification)}")
            onNotificationPosted(activeNotification, activeNotificationsSnapshot.currentRanking)
        }
    }

    override fun onListenerConnected() {
        if (LOG_NOTIFICATION) {
            FooLog.d(TAG, "#NOTIFICATION onListenerConnected()")
        }
        super.onListenerConnected()
        initializeActiveNotifications()
    }

    override fun onListenerDisconnected() {
        if (LOG_NOTIFICATION) {
            FooLog.d(TAG, "#NOTIFICATION onListenerDisconnected()")
        }
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        if (LOG_NOTIFICATION) {
            FooLog.d(TAG, "#NOTIFICATION onNotificationPosted(sbn=${FooNotification.toString(sbn)}, rankingMap=${FooNotification.toString(rankingMap)})")
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
            FooLog.d(TAG, "#NOTIFICATION onNotificationRemoved(sbn=${FooNotification.toString(sbn)}, rankingMap=${FooNotification.toString(rankingMap)}, reason=${FooString.quote(FooNotification.notificationCancelReasonToString(reason))})")
        }
        super.onNotificationRemoved(sbn, rankingMap, reason)
        // TODO: Handle removed notification...
    }

    override fun onNotificationRankingUpdate(rankingMap: RankingMap?) {
        if (LOG_NOTIFICATION) {
            FooLog.v(TAG, "#NOTIFICATION onNotificationRankingUpdate(rankingMap=${FooNotification.toString(rankingMap)})")
        }
        super.onNotificationRankingUpdate(rankingMap)
    }

    override fun onListenerHintsChanged(hints: Int) {
        if (LOG_NOTIFICATION) {
            FooLog.v(TAG, "#NOTIFICATION onListenerHintsChanged(hints=${FooNotification.notificationHintsToString(hints)})")
        }
        super.onListenerHintsChanged(hints)
    }

    override fun onSilentStatusBarIconsVisibilityChanged(hideSilentStatusIcons: Boolean) {
        if (LOG_NOTIFICATION) {
            FooLog.v(TAG, "#NOTIFICATION onSilentStatusBarIconsVisibilityChanged(hideSilentStatusIcons=$hideSilentStatusIcons)")
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
            FooLog.v(TAG, "#NOTIFICATION onNotificationChannelModified(...)")
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
            FooLog.v(TAG, "#NOTIFICATION onNotificationChannelGroupModified(...)")
        }
        super.onNotificationChannelGroupModified(pkg, user, group, modificationType)
    }

    override fun onInterruptionFilterChanged(interruptionFilter: Int) {
        if (LOG_NOTIFICATION) {
            FooLog.v(TAG, "#NOTIFICATION onInterruptionFilterChanged(interruptionFilter=${FooNotification.notificationInterruptionFilterToString(interruptionFilter)})")
        }
        super.onInterruptionFilterChanged(interruptionFilter)
    }
}
