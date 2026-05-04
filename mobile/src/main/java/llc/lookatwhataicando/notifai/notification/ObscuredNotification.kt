package llc.lookatwhataicando.notifai.notification

/**
 * Describes a notification whose content is obscured from [android.service.notification.NotificationListenerService]
 * — the NLS payload contains no title, text, or extras — but whose content IS visible in the
 * notification shade and readable via [MyAccessibilityService].
 *
 * ## Sub-classes
 *
 * ### Stale-obscured (confirmed, common)
 * During live delivery the app posts a GROUP_SUMMARY immediately followed by a content-bearing
 * child notification (e.g. `CHAT_CHIME`, `MessagingStyle`) within milliseconds. NLS reads the
 * child normally; the 300 ms cancellation window in `schedulePendingLookup` prevents the
 * accessibility path from firing. The GROUP_SUMMARY becomes obscured only during stale
 * catch-up (`initializeActiveNotifications`) — the child has already been dismissed, only the
 * empty GROUP_SUMMARY remains.
 *
 * Confirmed: `com.google.android.apps.dynamite` (Google Chat).
 *
 * ### Always-obscured (theoretical, unconfirmed)
 * Some apps may never post a content-bearing sibling — not even for live delivery. Content is
 * only ever available in the shade. The accessibility path would fire for both live and stale
 * notifications. If this category exists, it is theorized to be limited to system-signed /
 * AOSP / Google apps. See `notification/parsers/README.md` for detection guidance.
 *
 * Currently uncharacterized: `com.google.android.googlequicksearchbox` (Google).
 */
data class ObscuredNotification(
    val packageName: String,
    val appLabel: String,
    val notificationFlags: Int,
    val postedAtMs: Long,
    val resolutionOutcome: ResolutionOutcome,
)

enum class ResolutionOutcome {
    /** Accessibility found and read the shade row for this notification. */
    FOUND,
    /** Accessibility was available but no matching row was found in the shade. */
    NOT_FOUND,
    /** MyAccessibilityService.instance was null — permission not granted. */
    ACCESSIBILITY_UNAVAILABLE,
}
