# Notification Parsing — Ground Truth

## Two delivery paths

### Path 1 — NotificationListenerService (NLS), `MyNotificationListenerService`

The primary path. The system delivers every `StatusBarNotification` to `onNotificationPosted`.
`defaultOnNotificationPosted` extracts title, text, ticker, and `MessagingStyle` messages from
the notification extras and speaks them via TTS.

This covers the vast majority of apps and all well-behaved notifications.

### Path 2 — AccessibilityService, `MyAccessibilityService`

A fallback for notifications whose content is **not present in the NLS payload** but **is
visible in the notification shade**. The accessibility tree exposes the rendered shade rows,
including content that the app only populates in the visual layout.

---

## "Obscured" notifications (`ObscuredNotification`)

Some apps post a `GROUP_SUMMARY` notification whose NLS payload contains no title, text, or
extras — only metadata. The actual message content is only accessible by reading the expanded
notification row in the shade via the accessibility tree.

There are two meaningfully different sub-classes of obscured notification:

### Stale-obscured (confirmed, common)

For live delivery the GROUP_SUMMARY is **immediately followed** by a content-bearing child
notification (`CHAT_CHIME`, `MessagingStyle`, etc.) within milliseconds. NLS sees both and
reads the child normally — no accessibility involvement.

The GROUP_SUMMARY becomes obscured only when it is **stale**: the app has restarted,
`initializeActiveNotifications` iterates `getActiveNotifications()`, and the content-bearing
child has already been dismissed. Only the empty GROUP_SUMMARY remains. The 300 ms
`PENDING_LOOKUP_DELAY_MS` cancellation window elapses with no sibling, so the accessibility
path fires.

**Confirmed stale-obscured packages:**
- `com.google.android.apps.dynamite` (Google Chat) — live GROUP_SUMMARY is immediately
  followed by a `CHAT_CHIME` child; only the stale GROUP_SUMMARY reaches accessibility.

**Live flow (no accessibility involvement):**
1. GROUP_SUMMARY arrives → `ParsedIgnored` → `schedulePendingLookup(300 ms)`
2. Content-bearing child arrives → `cancelPendingLookup` fires before 300 ms elapses
3. Child spoken via normal NLS path.

**Stale/catch-up flow (accessibility required):**
1. GROUP_SUMMARY → `ParsedIgnored` → `schedulePendingLookup(300 ms)`
2. 300 ms elapses with no cancel → `MyAccessibilityService.findRowForAppLabel()`
3. Shade opens → rows scanned (expanding collapsed rows, scrolling for off-screen) →
   content read from accessibility tree → spoken via TTS.

**This is the primary scenario requiring the Accessibility permission.**

### Always-obscured (theoretical, unconfirmed)

The hypothesis: some apps **never** post a content-bearing sibling — not even for live
delivery. The GROUP_SUMMARY is the only notification they ever post for a given message.
Content is only ever visible in the shade. These packages would require the accessibility
path for **both** live and stale delivery; the `PENDING_LOOKUP_DELAY_MS` cancellation window
would never fire regardless of app state.

**Theory:** if this class of app exists, it is almost certainly limited to **system-signed /
AOSP / Google apps** — third-party apps generally carry readable content in their NLS payload
or in a sibling notification. System apps have additional delivery channels (e.g. FCM data
messages delivered directly to the app process) and may use the notification solely as a
visual shade badge.

**Currently uncharacterized** (appeared in production logs, live vs. stale behavior not yet
confirmed):
- `com.google.android.googlequicksearchbox` (Google)

**Detection signal:** `ObscuredNotificationLogger` will consistently log `FOUND` outcomes for
a given `packageName` even when notifications arrive in real-time (not only at app startup).
A package that triggers the accessibility path on live notifications — not just catch-up — is
a candidate for the always-obscured category.

---

## Open questions / assumptions to validate

### Does any known app always require accessibility, even for live delivery?

For **Google Chat** (`com.google.android.apps.dynamite`): confirmed stale-obscured only.
Live delivery always includes a content-bearing sibling. The 300 ms cancellation window
reliably prevents the accessibility path from firing for live notifications.

**Unconfirmed:** whether any package consistently reaches accessibility even for freshly
arriving live notifications. The leading theory is that if such a class exists, it is limited
to system-signed / AOSP / Google apps that have alternative delivery channels and use the
notification only for the visual badge.

**Action needed:** monitor `ObscuredNotificationLogger` output for any `packageName` that
consistently fires the accessibility lookup with `FOUND` outcomes during live notification
delivery (i.e. not only at app startup). Such a package is a candidate for always-obscured
classification and may need a bespoke `NotificationParser` or a dedicated accessibility search
strategy.

---

## Adding a new parser

1. Create `NotificationParserXxx.kt` extending `NotificationParser`.
2. Override `packageName` and `onNotificationPosted`.
3. Register in `MyNotificationListenerService.addNotificationParsers()`.

If the app only posts `GROUP_SUMMARY` with no content extras, the accessibility path handles
it automatically — no custom parser required unless bespoke field extraction is needed.

---

## Constants

| Constant | Location | Value | Purpose |
|---|---|---|---|
| `PENDING_LOOKUP_DELAY_MS` | `MyNotificationListenerService` | 300 ms | Window for content-bearing child to cancel the accessibility lookup |
| `ShadeDelays.FAST.shadeSettle` | `ShadeDelays` | 600 ms | Shade animation settle time before scanning (production) |
| `ShadeDelays.SLOW.shadeSettle` | `ShadeDelays` | 3000 ms | Shade animation settle time before scanning (debug/observation) |
| `MAX_SCROLL_ATTEMPTS` | `ShadeRowSearchQueue`, `DebugShadeScan` | 10 | Max `ACTION_SCROLL_FORWARD` calls before giving up on off-screen rows |
