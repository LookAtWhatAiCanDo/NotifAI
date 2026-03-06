package llc.lookatwhataicando.notifai.notification.parsers

import android.app.Notification
import android.app.Person
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.smartfoo.android.core.FooString
import com.smartfoo.android.core.logging.FooLog
import com.smartfoo.android.core.notification.FooNotification
import com.smartfoo.android.core.platform.FooPlatformUtils
import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder
import llc.lookatwhataicando.notifai.notification.NotificationParserUtils

class NotificationParserDiscord(parserCallbacks: NotificationParserCallbacks) :
    NotificationParser("#DISCORD", parserCallbacks) {
    companion object {
        private val TAG = FooLog.TAG(NotificationParserDiscord::class)
    }

    override val packageName: String
        get() = "com.discord"

    override fun onNotificationPosted(sbn: StatusBarNotification): NotificationParseResult {
        /**
         * 2026-03-05 15:37:34.787 28322-28322 NotificationParser      llc.lookatwhataicando.notifai        V  defaultOnNotificationPosted: packageName="com.discord"
         * 2026-03-05 15:37:34.808 28322-28322 NotificationParser      llc.lookatwhataicando.notifai        V  defaultOnNotificationPosted: packageAppSpokenName="Discord"
         * 2026-03-05 15:37:34.809 28322-28322 NotificationParser      llc.lookatwhataicando.notifai        V  defaultOnNotificationPosted: notification=Notification(channel=messages shortcut=1238004201456406590 contentView=null vibrate=null sound=null defaults=0 flags=ONLY_ALERT_ONCE|AUTO_CANCEL color=0xff5865f2 category=msg groupKey=GROUP_MESSAGE_CREATE actions=3 vis=PRIVATE locusId=LocusId[19_chars])
         * 2026-03-05 15:37:34.817 28322-28322 NotificationParser      llc.lookatwhataicando.notifai        V  defaultOnNotificationPosted: extras={"android.title"="DistroAV #business: Trouffman", "android.hiddenConversationTitle"="DistroAV #business", "android.reduced.images"=true, "android.conversationTitle"="DistroAV #business", "android.subText"=null, "android.template"="android.app.Notification$MessagingStyle", "android.showChronometer"=false, "android.text"="Same process when I go to "sensitive" country like Mainland china & USA these days", "android.progress"=0, "androidx.core.app.extra.COMPAT_TEMPLATE"="androidx.core.app.NotificationCompat$MessagingStyle", "android.progressMax"=0, "android.selfDisplayName"="pvhaus", "android.conversationUnreadMessageCount"=0, "android.appInfo"=ApplicationInfo{82575f com.discord}, "android.messages"=[Landroid.os.Parcelable;@9855ac, "android.showWhen"=true, "android.largeIcon"=Icon(typ=BITMAP size=128x128), "android.messagingStyleUser"={"key"="me", "uri"=null, "icon"=null, "name"="pvhaus", "isBot"=false, "isImportant"=false}, "android.messagingUser"=android.app.Person@7c97b33c, "android.infoText"=null, "android.progressIndeterminate"=false, "android.remoteInputHistory"=null, "android.shortCriticalText"=null, "latestMessageId"=1479209639156519166, "android.isGroupConversation"=true}
         * 2026-03-05 15:37:34.817 28322-28322 NotificationParser      llc.lookatwhataicando.notifai        V  defaultOnNotificationPosted: tickerText=null
         * 2026-03-05 15:37:34.823 28322-28322 NotificationParser      llc.lookatwhataicando.notifai        V  defaultOnNotificationPosted: headUpContentView=null
         * 2026-03-05 15:37:34.823 28322-28322 NotificationParser      llc.lookatwhataicando.notifai        V  defaultOnNotificationPosted: actions=[Landroid.app.Notification$Action;@9ff1275
         * 2026-03-05 15:37:34.823 28322-28322 NotificationParser      llc.lookatwhataicando.notifai        V  defaultOnNotificationPosted: category=msg
         * 2026-03-05 15:37:34.824 28322-28322 NotificationParser      llc.lookatwhataicando.notifai        V  defaultOnNotificationPosted: title="DistroAV #business: Trouffman"
         * 2026-03-05 15:37:34.825 28322-28322 NotificationParser      llc.lookatwhataicando.notifai        V  defaultOnNotificationPosted: text="Same process when I go to "sensitive" country like Mainland china & USA these days"
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

        val conversationTitle = extras.getCharSequence("android.conversationTitle")
            ?: return NotificationParseResult.Unparsable

        builder.appendSilenceParagraphBreak()
        builder.appendSpeech(conversationTitle)

        val isGroupConversation = extras.getBoolean("android.isGroupConversation")

        val messagingUser = extras.getParcelable("android.messagingUser", Person::class.java)
        val meName = messagingUser?.name

        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        val messages = style?.messages
        messages?.let { messages ->
            FooLog.v(TAG, "onNotificationPosted: messages(${messages.size})")
            for (i in messages.indices) {
                val message = messages[i]
                FooLog.v(TAG, "onNotificationPosted:   message[$i]: ${FooNotification.toString(message)}")
                val person = message.person ?: return NotificationParseResult.Unparsable
                val name = person.name ?: return NotificationParseResult.Unparsable
                val text = message.text ?: return NotificationParseResult.Unparsable
                builder.appendSilenceParagraphBreak()
                builder.appendSpeech("$name says")
                builder.appendSilenceParagraphBreak()
                builder.appendSpeech(text)
            }
            FooLog.v(TAG, "onNotificationPosted:")
        }

        textToSpeech.speak(builder)

        return NotificationParseResult.ParsedHandled
    }
}