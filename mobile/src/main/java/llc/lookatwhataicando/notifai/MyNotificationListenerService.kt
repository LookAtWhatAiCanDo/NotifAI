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
import com.smartfoo.android.core.notification.ActiveNotificationsSnapshot

class MyNotificationListenerService : NotificationListenerService() {
    companion object {
        private val TAG = FooLog.TAG(MyNotificationListenerService::class)

        @Suppress("SimplifyBooleanWithConstants", "KotlinConstantConditions", "RedundantSuppression", "UNREACHABLE_CODE")
        private val LOG_NOTIFICATION = true && BuildConfig.DEBUG

        fun isNotificationListenerEnabled(context: Context) =
            FooNotification.isNotificationListenerEnabled(context, MyNotificationListenerService::class.java)

        /**
         * Do NOT call startForegroundService to start this service manually!
         * NotificationListenerService startup is managed entirely by the system.
         * If there is a need to ensure it is bound, call `MyNotificationListenerService.requestNotificationListenerRebind(context)`.
         */
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
        if (LOG_NOTIFICATION) {
            Log.v(TAG, "#NOTIFICATION +initializeActiveNotifications()")
        }
        val activeNotificationsSnapshot = getActiveNotificationsSnapshot()
        initializeActiveNotifications(activeNotificationsSnapshot)
        if (LOG_NOTIFICATION) {
            Log.v(TAG, "#NOTIFICATION -initializeActiveNotifications()")
        }
    }

    private fun initializeActiveNotifications(activeNotificationsSnapshot: ActiveNotificationsSnapshot) {
        if (LOG_NOTIFICATION) {
            Log.d(TAG, "#NOTIFICATION initializeActiveNotifications(activeNotificationsSnapshot(${activeNotificationsSnapshot.activeNotifications?.size}))")
        }
        for (activeNotification in activeNotificationsSnapshot.activeNotificationsRanked.orEmpty()) {
            //Log.v(TAG, "#NOTIFICATION initializeActiveNotifications: activeNotification=${toString(activeNotification)}")
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

    @Suppress("KotlinConstantConditions")
    private fun toString(rankingMap: RankingMap?): String {
        val level = 0
        if (rankingMap == null) {
            return "null"
        }
        val sb = StringBuilder()
        if (level > 0) {
            sb.append("RankingMap(")
            var first = true
            val ranking = Ranking()
            for (key in rankingMap.orderedKeys) {
                if (first) {
                    first = false
                } else {
                    sb.append(", ")
                }
                sb.append(FooString.quote(key)).append("=")
                if (rankingMap.getRanking(key, ranking)) {
                    when (level) {
                        1 -> sb.append("…")
                        2 -> sb.append(ranking.toString().substringAfterLast('$'))
                    }
                } else {
                    sb.append("null")
                }
            }
            sb.append(")")
        } else {
            sb.append("…")
        }
        return sb.toString()
    }

    private fun toString(sbn: StatusBarNotification?): String {
        val notification = sbn?.notification ?: return "null"
        val extras = notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
        val infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()

        val sb = StringBuilder()
        sb.append("StatusBarNotification(")
        sb.append("pkg=").append(FooString.quote(sbn.packageName))
        sb.append(", user=").append(sbn.user)
        sb.append(", id=").append(sbn.id)
        sb.append(", tag=").append(FooString.quote(sbn.tag))
        sb.append(", key=").append(FooString.quote(sbn.key))
        sb.append(", notification=").append(notification)
        sb.append(", title=").append(FooString.quote(title))
        sb.append(", text=").append(FooString.quote(text))
        sb.append(", bigText=").append(FooString.quote(bigText))
        sb.append(", subText=").append(FooString.quote(subText)) // subtitle
        sb.append(", summaryText=").append(FooString.quote(summaryText))
        sb.append(", infoText=").append(FooString.quote(infoText))
        sb.append(")")
        return sb.toString()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        if (LOG_NOTIFICATION) {
            Log.d(TAG, "#NOTIFICATION onNotificationPosted(sbn=${toString(sbn)}, rankingMap=${toString(rankingMap)})")
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
            Log.d(TAG, "#NOTIFICATION onNotificationRemoved(sbn=${toString(sbn)}, rankingMap=${toString(rankingMap)}, reason=${FooString.quote(FooNotification.notificationCancelReasonToString(reason))})")
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
