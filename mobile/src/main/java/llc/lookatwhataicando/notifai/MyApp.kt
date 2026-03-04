package llc.lookatwhataicando.notifai

import android.app.Application
import android.util.Log
import llc.lookatwhataicando.notifai.util.MyLogUtils
import java.util.concurrent.atomic.AtomicBoolean

class MyApp : Application() {
    companion object {
        private val TAG = MyLogUtils.TAG(MyApp::class)
    }

    private val isShutdown = AtomicBoolean(false)

    override fun onCreate() {
        Log.v(TAG, "+onCreate()")
        super.onCreate()
        //...
        Log.v(TAG, "-onCreate()")
    }

    fun onBootCompleted() {
        Log.v(TAG, "+onBootCompleted()")
        //systemEvents.onBootCompleted(Intent.ACTION_BOOT_COMPLETED)
        MyForegroundNotificationService.start(this)
        //HourlyDigestWorker.schedule(this)
        Log.v(TAG, "-onBootCompleted()")
    }

    fun shutdown() {
        Log.v(TAG, "+shutdown()")
        if (!isShutdown.compareAndSet(false, true)) {
            Log.d(TAG, "shutdown: already executed")
        } else {
            Log.v(TAG, "...")
//        if (this::mediaSource.isInitialized) {
//            try {
//                mediaSource.stop("NotifAI.shutdown")
//            } catch (t: Throwable) {
//                Log.w(TAG, "shutdown: mediaSource.stop failed", t)
//            }
//        }
//        if (this::audioProfiles.isInitialized) {
//            runCatching { audioProfiles.shutdown() }
//                .onFailure { Log.w(TAG, "shutdown: audioProfiles.shutdown failed", it) }
//        }
//        if (this::appScope.isInitialized) {
//            appScope.cancel()
//        }
//        if (this::db.isInitialized) {
//            try {
//                db.close()
//            } catch (t: Throwable) {
//                Log.w(TAG, "shutdown: db.close failed", t)
//            }
//        }
        }
        Log.v(TAG, "-shutdown()")
    }
}
