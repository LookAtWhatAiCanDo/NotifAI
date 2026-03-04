package llc.lookatwhataicando.notifai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED) return
        (context.applicationContext as? MyApp)?.onBootCompleted()
    }
}
