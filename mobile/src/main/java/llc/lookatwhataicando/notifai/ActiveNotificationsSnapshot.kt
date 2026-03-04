package llc.lookatwhataicando.notifai

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.NotificationListenerService.Ranking
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import android.util.Log
import llc.lookatwhataicando.notifai.util.MyLogUtils
import llc.lookatwhataicando.notifai.util.MyNotificationUtils
import llc.lookatwhataicando.notifai.util.MyStringUtils
import kotlin.text.isNullOrEmpty
import kotlin.text.substring

/**
 * Holds an immutable snapshot of active notifications and their ranking.
 *
 * Call [snapshot] to populate from a NotificationListenerService, or [reset] to clear.
 */
class ActiveNotificationsSnapshot(
    val context: Context,
) {
    /** Snapshot of the [NotificationListenerService] used at the last [snapshot] call; null after [reset]. */
    var notificationListenerService: MyNotificationListenerService? = null
        private set

    /** Snapshot of active notifications at the last [snapshot] call; null after [reset]. */
    var activeNotifications: List<StatusBarNotification>? = null
        private set

    /** Snapshot of the system RankingMap at the last [snapshot] call; null after [reset]. */
    var currentRanking: RankingMap? = null
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
                        Log.e(TAG, "activeNotificationsRanked: notification=${toString(sbn, showAllExtras = false)}")
                    }
                    Log.e(TAG, "activeNotificationsRanked:")
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
            template?.endsWith("\$MediaStyle") == true ||
                    template?.contains("MediaStyle") == true
        return hasMediaSession || isTransport || isMediaStyle
    }

    private fun bucketOfWithRank(
        sbn: StatusBarNotification,
        r: Ranking,
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
        val nc = MyNotificationUtils.getNotificationChannel(context, n)
        if (isMediaNotificationCompat(n)) return UiBucket.MEDIA
        val importance = nc?.importance ?: NotificationManager.IMPORTANCE_UNSPECIFIED
        // Heuristic: treat importance below as silent when no ranking is available
        val silent = importance <= NotificationManager.IMPORTANCE_DEFAULT
        return if (silent) UiBucket.SILENT else UiBucket.ALERTING
    }

    private fun shadeSort(
        context: Context,
        actives: List<StatusBarNotification>?,
        rankingMap: RankingMap?,
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
                    val r = Ranking()
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

    companion object {
        private val TAG = MyLogUtils.TAG(ActiveNotificationsSnapshot::class)

        fun toString(ranking: Ranking): String = "{key=${ranking.key}, rank=${ranking.rank}}"

        fun toString(
            sbn: StatusBarNotification,
            showAllExtras: Boolean = false,
        ): String {
            val notification = sbn.notification
            val extras = notification.extras
            val title = extras?.getCharSequence(Notification.EXTRA_TITLE)
            var text = extras?.getCharSequence(Notification.EXTRA_TEXT)
            if (text != null) {
                text =
                    if (text.length > 33) {
                        "(${text.length})${MyStringUtils.quote(text.substring(0, 32)).replaceAfterLast("\"", "…\"")}"
                    } else {
                        MyStringUtils.quote(text)
                    }
            }
            val subText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)

            val sb = StringBuilder("{ ")
            if (title != null || text != null || subText != null) {
                sb.append("extras={ ")
            }
            if (title != null) {
                sb.append("${Notification.EXTRA_TITLE}=${MyStringUtils.quote(title)}")
            }
            if (text != null) {
                sb.append(", ${Notification.EXTRA_TEXT}=$text")
            }
            if (subText != null) {
                sb.append(", ${Notification.EXTRA_SUB_TEXT}=${MyStringUtils.quote(subText)}")
            }
            if (title != null || text != null || subText != null) {
                sb.append(" }, ")
            }
            sb.append(
                "id=${sbn.id}, key=${MyStringUtils.quote(sbn.key)}, packageName=${
                    MyStringUtils.quote(sbn.packageName)
                }, notification={ $notification",
            )
            if (showAllExtras) {
                sb.append(", extras=")
                if (extras != null) {
                    extras.remove(Notification.EXTRA_TITLE)
                    extras.remove(Notification.EXTRA_TEXT)
                    extras.remove(Notification.EXTRA_SUB_TEXT)
                }
                sb.append(MyStringUtils.toString(extras))
            }
            sb.append(" } }")
            return sb.toString()
        }
    }
}
