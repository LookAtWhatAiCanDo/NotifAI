package llc.lookatwhataicando.notifai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.smartfoo.android.core.logging.FooLog
import com.smartfoo.android.core.notification.FooNotification
import java.util.concurrent.atomic.AtomicBoolean

class MyForegroundNotificationService : Service() {
    companion object {
        private val TAG = FooLog.TAG(MyNotificationListenerService::class)

        private const val NOTIFICATION_ID = 42

        private const val REQUEST_SHOW = 100
        private const val REQUEST_PIN = 101
        private const val REQUEST_QUIT = 102
        private const val REQUEST_ENABLE = 103
        private const val REQUEST_DEBUG_SPEECH = 104

        private const val ACTION_APP_SHUTDOWN = "llc.lookatwhataicando.notifai.action.APP_SHUTDOWN"
        private const val ACTION_DEBUG_SPEECH_NOTIFICATION = "llc.lookatwhataicando.notifai.action.DEBUG_SPEECH_NOTIFICATION"

        private const val NOTIFICATION_CHANNEL_ID = "notifai_status"
        private const val NOTIFICATION_GROUP_KEY = "notifai_status_group"

        private fun intent(context: Context, action: String? = null): Intent =
            Intent(context, MyForegroundNotificationService::class.java)
                .setAction(action)


        fun startForegroundService(context: Context, action: String? = null) {
            context.startForegroundService(intent(context, action))
        }

        // Q: Is this reentrant [OK to call multiple times], or does it need to gate?
        fun start(context: Context) = startForegroundService(context)

        private fun intentAppShutdown(context: Context) =
            intent(context, ACTION_APP_SHUTDOWN)

        fun appShutdown(context: Context) {
            startForegroundService(context, ACTION_APP_SHUTDOWN)
        }

        fun isOngoingNotificationNoDismiss(context: Context) =
            FooNotification.isCallingAppNotificationNoDismiss(context, NOTIFICATION_ID)
    }

    private val app by lazy { application as MyApp }
    private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        FooLog.v(TAG, "+onCreate()")
        super.onCreate()
        startForeground()
    }

    private fun startForeground() {
        /**
         * From [https://developer.android.com/about/versions/14/changes/fgs-types-required#include-fgs-type-runtime](https://developer.android.com/about/versions/14/changes/fgs-types-required#include-fgs-type-runtime):
         * > **If the foreground service type is not specified in the call,
         * > the type defaults to the values defined in the manifest.**
         * > If you didn't specify the service type in the manifest, the system throws
         * > [MissingForegroundServiceTypeException](https://developer.android.com/reference/android/app/MissingForegroundServiceTypeException).
         *
         * ie: [android.app.Service.startForeground]`(int id, Notification notification)` defaults `foregroundServiceType` to [ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST]
         *
         * NOTE: If multiple types are needed/defined in AndroidManifest then all of those
         * foreground services types need to be passed as flags to `android.app.Service.startForeground(...)`
         * All foreground service type requirements must be met before `startForeground(...)` will succeed.
         * > In cases where a foreground service is started with multiple types, then the
         * > foreground service must adhere to the
         * > [platform enforcement requirements](https://developer.android.com/guide/components/foreground-services#runtime-permissions)
         * > of all types.
         */
        val foregroundServiceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
        startForeground(NOTIFICATION_ID, buildOngoingNotification(), foregroundServiceType)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        FooLog.v(TAG, "onStartCommand(intent=$intent}, flags=$flags, startId=$startId)")
        when (intent?.action) {
            ACTION_APP_SHUTDOWN -> {
                AppShutdownManager.markQuitRequested(this)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    private var contentTitle: String? = null

    private fun buildOngoingNotification(
        contentTitle: String? = null,
        contentText: String? = null,
        pinEnable: Boolean = false,
        promptEnable: Boolean = false,
    ): Notification {
        val contentTitle = contentTitle ?: this.contentTitle
        ?: getString(R.string.notification_initializing)

        this.contentTitle = contentTitle

        ensureOngoingNotificationChannel()

        val pendingIntentShow =
            PendingIntent.getActivity(
                this,
                REQUEST_SHOW,
                MainActivity.intentShow(this),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val builder =
            NotificationCompat
                .Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle(contentTitle)
                //.setSubText("TODO")
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(pendingIntentShow)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setGroup(NOTIFICATION_GROUP_KEY)
                .setWhen(Long.MAX_VALUE)
                .setShowWhen(false)
        contentText?.let {
            builder.setContentText(it)
        }

        if (pinEnable) {
            val pendingIntentPin =
                PendingIntent.getActivity(
                    this,
                    REQUEST_PIN,
                    MainActivity.intentPin(this),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            builder
                //.setContentTitle("...")
                .addAction(R.drawable.ic_warning, getString(R.string.notification_action_persistent), pendingIntentPin)
        }

        // BUG: "Quit" showed up as disabled once! Why?!?!?
        val pendingIntentAppShutdown =
            PendingIntent.getService(
                this,
                REQUEST_QUIT,
                intentAppShutdown(this),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        builder
            //.setContentText("...")
            .addAction(0, getString(R.string.notification_action_quit), pendingIntentAppShutdown)

        if (promptEnable) {
            val intentEnable = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            val pendingIntentEnable =
                PendingIntent.getActivity(
                    this,
                    REQUEST_ENABLE,
                    intentEnable,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            builder
                //.setContentText(getString(R.string.pipeline_notification_permission))
                .addAction(0, getString(R.string.notification_action_enable), pendingIntentEnable)
        }

        if (BuildConfig.DEBUG) {
            val debugIntent =
                PendingIntent.getService(
                    this,
                    REQUEST_DEBUG_SPEECH,
                    intent(this, ACTION_DEBUG_SPEECH_NOTIFICATION),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            builder.addAction(0, getString(R.string.notification_action_debug_speak), debugIntent)
        }

        return builder.build()
    }

    private val notificationChannelInitialized = AtomicBoolean(false)

    private fun ensureOngoingNotificationChannel() {
        if (notificationChannelInitialized.compareAndSet(false, true)) {
            val channel =
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            channel.description = getString(R.string.notification_channel_description)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
