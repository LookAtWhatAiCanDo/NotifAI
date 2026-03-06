package llc.lookatwhataicando.notifai.notification.parsers

import android.app.Notification
import android.app.Person
import android.service.notification.StatusBarNotification
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import com.smartfoo.android.core.FooString
import com.smartfoo.android.core.logging.FooLog
import com.smartfoo.android.core.notification.FooNotification
import com.smartfoo.android.core.platform.FooPlatformUtils
import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder
import llc.lookatwhataicando.notifai.notification.NotificationParserUtils

class NotificationParserAndroidMessages(parserCallbacks: NotificationParserCallbacks) :
    NotificationParser("#MESSAGES", parserCallbacks) {
    companion object {
        private val TAG = FooLog.TAG(NotificationParserAndroidMessages::class)
    }

    override val packageName: String
        get() = "com.google.android.apps.messaging"

    override fun onNotificationPosted(sbn: StatusBarNotification): NotificationParseResult {
        /**
         */

        val packageName = NotificationParserUtils.getPackageName(sbn)
        FooLog.v(TAG, "onNotificationPosted: packageName=${FooString.quote(packageName)}")

        val packageAppSpokenName = if (!FooString.isNullOrEmpty(packageAppSpokenName)) packageAppSpokenName else FooPlatformUtils.getApplicationName(context, packageName)
        FooLog.v(TAG, "onNotificationPosted: packageAppSpokenName=${FooString.quote(packageAppSpokenName)}")

        val notification = NotificationParserUtils.getNotification(sbn)
        FooLog.v(TAG, "onNotificationPosted: notification=$notification")

        val extras = NotificationParserUtils.getExtras(notification)
        FooLog.v(TAG, "onNotificationPosted: extras=${FooPlatformUtils.toString(extras)}")

        val actions = notification.actions
        FooLog.v(TAG, "onNotificationPosted: actions(${actions?.size})=${FooString.toString(actions, ({ item ->
            when (item) {
                is Notification.Action -> FooNotification.toString(item)
                else -> FooString.quote(item)
            }
        }))}")

        val builder = FooTextToSpeechBuilder(packageAppSpokenName!!)

        val pkgContext = context.createPackageContext(sbn.packageName, 0)

        val views = listOfNotNull(
            notification.contentView,
            notification.bigContentView,
            notification.headsUpContentView
        )
        for (rv in views) {
            try {
                val root = rv.apply(pkgContext, FrameLayout(pkgContext))
                //collectTextViews(root, out)
                FooLog.v(TAG, "onNotificationPosted: root=$root")
            } catch (_: Throwable) {
                // ignore and continue
                FooLog.v(TAG, "onNotificationPosted: failed to apply")
            }
        }

//        val conversationTitle = extras.getCharSequence("android.conversationTitle")
//            ?: return NotificationParseResult.Unparsable
//        builder.appendSilenceParagraphBreak()
//        builder.appendSpeech(conversationTitle)

//        val isGroupConversation = extras.getBoolean("android.isGroupConversation")

//        val messagingUser = extras.getParcelable("android.messagingUser", Person::class.java)
//        val meName = messagingUser?.name

//        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
//        val messages = style?.messages
//        messages?.let { messages ->
//            FooLog.v(TAG, "onNotificationPosted: messages(${messages.size})")
//            for (i in messages.indices) {
//                val message = messages[i]
//                FooLog.v(TAG, "onNotificationPosted:   message[$i]: ${FooNotification.toString(message)}")
//                val person = message.person ?: return NotificationParseResult.Unparsable
//                val name = person.name ?: return NotificationParseResult.Unparsable
//                val text = message.text ?: return NotificationParseResult.Unparsable
//                builder.appendSilenceParagraphBreak()
//                builder.appendSpeech("$name says")
//                builder.appendSilenceParagraphBreak()
//                builder.appendSpeech(text)
//            }
//            FooLog.v(TAG, "onNotificationPosted:")
//        }
//
//        textToSpeech.speak(builder)

        // TODO: Cut down on the plethora of redundant "Amazon Alexa"s that are spoken
        return defaultOnNotificationPosted(context, sbn, textToSpeech)
    }
}