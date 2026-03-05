package llc.lookatwhataicando.notifai

import android.app.Application
import android.util.Log
import com.smartfoo.android.core.logging.FooLog
import java.util.concurrent.atomic.AtomicBoolean

class MyApp : Application() {
    companion object {
        private val TAG = FooLog.TAG(MyApp::class)
    }

    private val isShutdown = AtomicBoolean(false)

    override fun onCreate() {
        FooLog.v(TAG, "+onCreate()")
        super.onCreate()
        //...
        FooLog.v(TAG, "-onCreate()")
    }

    fun onBootCompleted() {
        FooLog.v(TAG, "+onBootCompleted()")
        //systemEvents.onBootCompleted(Intent.ACTION_BOOT_COMPLETED)
        MyForegroundNotificationService.start(this)
        //HourlyDigestWorker.schedule(this)
        FooLog.v(TAG, "-onBootCompleted()")
    }

    fun shutdown() {
        FooLog.v(TAG, "+shutdown()")
        if (!isShutdown.compareAndSet(false, true)) {
            FooLog.d(TAG, "shutdown: already executed")
        } else {
            FooLog.v(TAG, "...")
//        if (this::mediaSource.isInitialized) {
//            try {
//                mediaSource.stop("NotifAI.shutdown")
//            } catch (t: Throwable) {
//                FooLog.w(TAG, "shutdown: mediaSource.stop failed", t)
//            }
//        }
//        if (this::audioProfiles.isInitialized) {
//            runCatching { audioProfiles.shutdown() }
//                .onFailure { FooLog.w(TAG, "shutdown: audioProfiles.shutdown failed", it) }
//        }
//        if (this::appScope.isInitialized) {
//            appScope.cancel()
//        }
//        if (this::db.isInitialized) {
//            try {
//                db.close()
//            } catch (t: Throwable) {
//                FooLog.w(TAG, "shutdown: db.close failed", t)
//            }
//        }
        }
        FooLog.v(TAG, "-shutdown()")
    }
}
