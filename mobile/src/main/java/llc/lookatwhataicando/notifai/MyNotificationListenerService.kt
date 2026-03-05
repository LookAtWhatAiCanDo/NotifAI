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
import com.smartfoo.android.core.logging.FooLog
import com.smartfoo.android.core.notification.FooNotification
import com.smartfoo.android.core.FooString
import llc.lookatwhataicando.notifai.ActiveNotificationsSnapshot
import llc.lookatwhataicando.notifai.notification.NotificationParserManager

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
        /*
        * Delay initialization to avoid system_server process binder transaction issues during the onListenerConnected callback:
        * ```
        * 2026-03-04 13:01:03.519  1469-1469  BoundServiceSession  system_server  E  Bad key 0 received in binderTransactionCompleted! Closing all transactions on CR{fe8bec1 1469->llc.lookatwhataicando.notifai/.MyNotificationListenerService flags=0x805000101}. Current keys: {onListenerConnected=0}; Counts: [0] (Fix with AI)
        *                                                                           android.util.Log$TerribleFailure: Bad key 0 received in binderTransactionCompleted! Closing all transactions on CR{fe8bec1 1469->llc.lookatwhataicando.notifai/.MyNotificationListenerService flags=0x805000101}. Current keys: {onListenerConnected=0}; Counts: [0]
        *                                                                             at android.util.Log.wtf(Log.java:339)
        *                                                                             at android.util.Slog.wtfStack(Slog.java:246)
        *                                                                             at com.android.server.am.BoundServiceSession.handleInvalidToken(BoundServiceSession.java:128)
        *                                                                             at com.android.server.am.BoundServiceSession.binderTransactionCompleted(BoundServiceSession.java:183)
        *                                                                             at com.android.server.notification.NotificationManagerService$NotificationListeners$1.$r8$lambda$QiDnMMKg1JpZvr40fh3mCflc3tA(NotificationManagerService.java:13958)
        *                                                                             at com.android.server.notification.NotificationManagerService$NotificationListeners$1$$ExternalSyntheticLambda0.run(R8$$SyntheticClass:0)
        *                                                                             at android.os.Handler.handleCallback(Handler.java:1070)
        *                                                                             at android.os.Handler.dispatchMessage(Handler.java:125)
        *                                                                             at android.os.Looper.dispatchMessage(Looper.java:333)
        *                                                                             at android.os.Looper.loopOnce(Looper.java:263)
        *                                                                             at android.os.Looper.loop(Looper.java:367)
        *                                                                             at com.android.server.SystemServer.run(SystemServer.java:1081)
        *                                                                             at com.android.server.SystemServer.main(SystemServer.java:711)
        *                                                                             at java.lang.reflect.Method.invoke(Native Method)
        *                                                                             at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:566)
        *                                                                             at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:907)
        * ```
        * The app itself and phone still seems to be running fine.
        * Since adding this delay I have not seen this error since 2026/03/04.
        */
        Handler(Looper.getMainLooper()).post {
            initializeActiveNotifications()
        }
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
