package llc.lookatwhataicando.notifai

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

/**
 * Coordinates a graceful, single-shot application shutdown.
 * This ensures the foreground service winds down, shared application resources are released,
 * and finally the process exits so no background components linger.
 */
object AppShutdownManager {
    private const val TAG = "AppShutdownManager"
    private val quitting = AtomicBoolean(false)
    private val shutdownPrepared = AtomicBoolean(false)

    private fun prepareForShutdown(context: Context) {
        if (!shutdownPrepared.compareAndSet(false, true)) {
            Log.d(TAG, "prepareForShutdown: already prepared")
            return
        }
        Log.i(TAG, "prepareForShutdown: closing tasks and unbinding listener")
        val activityManager = context.getSystemService(ActivityManager::class.java)
        activityManager?.appTasks?.forEach {
            runCatching { it.finishAndRemoveTask() }
        }
        MyNotificationListenerService.requestNotificationListenerUnbind(context)
    }

    /**
     * @return true if MyNotificationListenerService.isNotificationListenerEnabled(context) is true, otherwise false
     */
    fun onForegroundNotificationServiceStarted(context: Context): Boolean {
        shutdownPrepared.set(false)
        quitting.set(false)
        return MyNotificationListenerService.isNotificationListenerEnabled(context)
    }

    fun requestQuit(context: Context) {
        Log.i(TAG, "requestQuit(context)")
        if (!quitting.compareAndSet(false, true)) {
            Log.d(TAG, "requestQuit: shutdown already in progress")
        } else {
            Log.i(TAG, "requestQuit: initiating app shutdown")
        }
        prepareForShutdown(context)
        MyForegroundNotificationService.appShutdown(context)
    }

    fun markQuitRequested(context: Context) {
        Log.v(TAG, "markQuitRequested()")
        if (quitting.compareAndSet(false, true)) {
            Log.i(TAG, "markQuitRequested: quit triggered by service command")
        }
        prepareForShutdown(context)
    }

    fun onForegroundNotificationServiceDestroyed(app: MyApp) {
        if (!quitting.get()) {
            Log.d(TAG, "onForegroundNotificationServiceDestroyed: service stopped outside quit flow; ignore")
            return
        }
        Log.i(TAG, "onForegroundNotificationServiceDestroyed: finalizing application scope")
        app.shutdown()
        Log.w(TAG, "onForegroundNotificationServiceDestroyed: terminating process")
        //android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(0)
    }
}
