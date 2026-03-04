package llc.lookatwhataicando.notifai.startup

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import com.smartfoo.android.core.notification.FooNotification

/**
 * Reactive Flow that emits whenever the system's enabled_notification_listeners
 * setting changes, via a ContentObserver on Settings.Secure.
 *
 * Why this instead of ON_RESUME polling:
 *   A user can revoke Notification Listener access via Settings while the
 *   app is in the foreground (e.g. via quick settings or another app).
 *   ON_RESUME wouldn't catch that until the user backgrounds and returns.
 *   The ContentObserver fires immediately.
 *
 * The Flow is collected in the ViewModel's viewModelScope so it lives as
 * long as the ViewModel — surviving configuration changes and brief
 * backgrounding.
 */
object NotificationListenerEnabledMonitor {
    fun observe(context: Context): Flow<Boolean> = callbackFlow {

        fun isEnabled() = NotificationManagerCompat
            .getEnabledListenerPackages(context)
            .contains(context.packageName)

        // Emit current state immediately so the ViewModel has a value on init
        trySend(isEnabled())

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(isEnabled())
            }
        }

        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(FooNotification.ENABLED_NOTIFICATION_LISTENERS),
            false,
            observer
        )

        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
        }

    }.distinctUntilChanged() // suppress no-op emissions (observer fires on all Secure changes)
}
