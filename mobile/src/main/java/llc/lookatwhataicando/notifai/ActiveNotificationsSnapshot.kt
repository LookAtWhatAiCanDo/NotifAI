package llc.lookatwhataicando.notifai

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.smartfoo.android.core.FooString
import com.smartfoo.android.core.logging.FooLog
import com.smartfoo.android.core.notification.FooNotification

/**
 * Holds an immutable snapshot of active notifications and their ranking.
 *
 * Call [snapshot] to populate from a NotificationListenerService, or [reset] to clear.
 */
class ActiveNotificationsSnapshot(
    val context: Context,
) {
    companion object {
        private val TAG = FooLog.TAG(ActiveNotificationsSnapshot::class)
    }

    /** Snapshot of the [android.service.notification.NotificationListenerService] used at the last [snapshot] call; null after [reset]. */
    var notificationListenerService: MyNotificationListenerService? = null
        private set

    /** Snapshot of active notifications at the last [snapshot] call; null after [reset]. */
    var activeNotifications: List<StatusBarNotification>? = null
        private set

    /** Snapshot of the system RankingMap at the last [snapshot] call; null after [reset]. */
    var currentRanking: NotificationListenerService.RankingMap? = null
        private set

    private var _activeNotificationsRanked: List<StatusBarNotification>? = null

    /** Ranked view (top → bottom), cached after the first computation per snapshot. */
    val activeNotificationsRanked: List<StatusBarNotification>?
        get() {
            if (_activeNotificationsRanked == null) {
                _activeNotificationsRanked = shadeSort(context, activeNotifications, currentRanking)
                @Suppress("ConstantConditionIf")
                if (false) {
                    for (sbn in _activeNotificationsRanked!!) {
                        FooLog.e(TAG, "activeNotificationsRanked: notification=${FooNotification.toString(sbn, showAllExtras = false)}")
                    }
                    FooLog.e(TAG, "activeNotificationsRanked:")
                }
            }
            return _activeNotificationsRanked
        }

    /** Clears all state back to defaults (empty notifications, null ranking map). */
    fun reset() {
        notificationListenerService = null
        activeNotifications = null
        currentRanking = null
        _activeNotificationsRanked = null
    }

    // @WorkerThread

    /**
     * Replaces current state with a fresh snapshot from [service].
     * If [service] is null, behaves like [reset].
     */
    fun snapshot(service: MyNotificationListenerService?): ActiveNotificationsSnapshot {
        reset()
        notificationListenerService = service
        if (service != null) {
            activeNotifications = service.activeNotifications?.toList()
            currentRanking = service.currentRanking
        }
        return this
    }

    /**
     * Top to bottom order of appearance in the Notification Shade.
     * Analogous to ...?
     */
    private enum class UiBucket {
        MEDIA,
        CONVERSATION,
        ALERTING,
        SILENT,
    }

    private fun isMediaNotificationCompat(n: Notification): Boolean {
        val extras = n.extras
        val hasMediaSession = extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true
        val isTransport = n.category == Notification.CATEGORY_TRANSPORT
        val template = extras?.getString(Notification.EXTRA_TEMPLATE)
        // Accept framework or compat styles; the literal string contains '$'
        val isMediaStyle =
            template?.endsWith($$"$MediaStyle") == true ||
                    template?.contains("MediaStyle") == true
        return hasMediaSession || isTransport || isMediaStyle
    }

    private fun bucketOfWithRank(
        sbn: StatusBarNotification,
        r: NotificationListenerService.Ranking,
    ): UiBucket {
        if (isMediaNotificationCompat(sbn.notification)) return UiBucket.MEDIA
        if (r.isConversation) return UiBucket.CONVERSATION
        val isSilent = r.isAmbient || r.importance <= NotificationManager.IMPORTANCE_LOW
        return if (isSilent) UiBucket.SILENT else UiBucket.ALERTING
    }

    /** Fallback when RankingMap is null: heuristic using flags/category/priority. */
    private fun bucketOfNoRank(
        context: Context,
        sbn: StatusBarNotification,
    ): UiBucket {
        val n = sbn.notification
        val nc = FooNotification.getNotificationChannel(context, n)
        if (isMediaNotificationCompat(n)) return UiBucket.MEDIA
        val importance = nc?.importance ?: NotificationManager.IMPORTANCE_UNSPECIFIED
        // Heuristic: treat importance below as silent when no ranking is available
        val silent = importance <= NotificationManager.IMPORTANCE_DEFAULT
        return if (silent) UiBucket.SILENT else UiBucket.ALERTING
    }

    private fun shadeSort(
        context: Context,
        actives: List<StatusBarNotification>?,
        rankingMap: NotificationListenerService.RankingMap?,
    ): List<StatusBarNotification> {
        val list = actives ?: return emptyList()
        if (list.isEmpty()) return emptyList()

        // Collapse groups: prefer GROUP_SUMMARY when present
        val summariesByGroup =
            list
                .filter { it.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0 }
                .associateBy { it.notification.group }

        val collapsed =
            buildList {
                val seen = HashSet<String>()
                list
                    .filter { it.notification.group.isNullOrEmpty() }
                    .forEach { if (seen.add(it.key)) add(it) }
                list.groupBy { it.notification.group }.forEach { (g, members) ->
                    if (g.isNullOrEmpty()) return@forEach
                    val summary = summariesByGroup[g]
                    if (summary != null) {
                        if (seen.add(summary.key)) add(summary)
                    } else {
                        members.forEach { if (seen.add(it.key)) add(it) }
                    }
                }
            }

        // System order index (may be empty if rankingMap == null)
        val sysOrder: Map<String, Int> =
            rankingMap?.orderedKeys?.withIndex()?.associate { it.value to it.index } ?: emptyMap()

        data class K(
            val bucket: UiBucket,
            val sys: Int,
            val tiebreak: Long,
        )
        val keys = HashMap<String, K>(collapsed.size * 2)

        for (sbn in collapsed) {
            val n = sbn.notification
            val sortKey = n.sortKey
            val tiebreak = if (!sortKey.isNullOrEmpty()) sortKey.hashCode().toLong() else -sbn.postTime

            val (bucket, sysIdx) =
                if (rankingMap != null) {
                    val r = NotificationListenerService.Ranking()
                    val has = rankingMap.getRanking(sbn.key, r)
                    val b = if (has) bucketOfWithRank(sbn, r) else bucketOfNoRank(context, sbn)
                    val idx = sysOrder[sbn.key] ?: Int.MAX_VALUE
                    b to idx
                } else {
                    bucketOfNoRank(context, sbn) to Int.MAX_VALUE
                }

            keys[sbn.key] = K(bucket, sysIdx, tiebreak)
        }

        return collapsed.sortedWith(
            compareBy<StatusBarNotification> { keys[it.key]!!.bucket }
                .thenBy { keys[it.key]!!.sys }
                .thenBy { keys[it.key]!!.tiebreak },
        )
    }
}