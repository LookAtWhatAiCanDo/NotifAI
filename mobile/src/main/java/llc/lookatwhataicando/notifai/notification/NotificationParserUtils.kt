package llc.lookatwhataicando.notifai.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.view.size
import com.smartfoo.android.core.FooString
import com.smartfoo.android.core.logging.FooLog
import com.smartfoo.android.core.view.FooViewUtils
import llc.lookatwhataicando.notifai.R

/**
 * NOTE: Probably >90% of this code is deprecated as of 2026.
 * It is kept around most for reference purposes.
 */
@Suppress("unused")
object NotificationParserUtils {
    private val TAG = FooLog.TAG(NotificationParserUtils::class)

    fun toVerboseString(value: Int?): String {
        return if (value == null) "null" else "$value(0x${Integer.toHexString(value)})"
    }

    fun getResourcesForApplication(
        context: Context,
        appPackageName: String
    ): Resources? {
        return try {
            context.packageManager.getResourcesForApplication(appPackageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun getPackageName(sbn: StatusBarNotification): String = sbn.packageName

    fun getNotification(sbn: StatusBarNotification): Notification = sbn.notification

    fun getExtras(notification: Notification): Bundle = notification.extras

    fun getExtras(sbn: StatusBarNotification) = getExtras(getNotification(sbn))

    fun getActions(notification: Notification): Array<Notification.Action?> = notification.actions

    fun getActions(sbn: StatusBarNotification) = getActions(getNotification(sbn))

    fun getCompactActions(extras: Bundle): IntArray? {
        return extras.getIntArray(Notification.EXTRA_COMPACT_ACTIONS)
    }

    fun getAndroidTitle(extras: Bundle): CharSequence? {
        return extras.getCharSequence(Notification.EXTRA_TITLE)
    }

    fun getAndroidText(extras: Bundle): CharSequence? {
        return extras.getCharSequence(Notification.EXTRA_TEXT)
    }

    /**
     * @param sbn
     * @return "As of N, this field may be null" :(
     */
    fun getBigContentRemoteViews(sbn: StatusBarNotification): RemoteViews? {
        val notification = getNotification(sbn)
        return notification.bigContentView
    }

    /**
     * @param sbn
     * @return "As of N, this field may be null" :(
     */
    fun getContentRemoteViews(sbn: StatusBarNotification): RemoteViews? {
        val notification = getNotification(sbn)
        return notification.contentView
    }

    fun createPackageContext(context: Context, remoteView: RemoteViews?): Context? {
        if (remoteView == null) {
            return null
        }
        val packageName: String? = remoteView.getPackage()
        return try {
            context.createPackageContext(packageName, Context.CONTEXT_RESTRICTED)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun inflateRemoteView(context: Context, remoteViews: RemoteViews?): View? {
        val remoteContext = createPackageContext(context, remoteViews) ?: return null
        return remoteViews?.apply(remoteContext, RelativeLayout(remoteContext))
    }

    /*
    public static View mockRemoteView(Context context, RemoteViews remoteView)
    {
        Context otherAppContext = createPackageContext(context, remoteView);
        if (otherAppContext == null)
        {
            return null;
        }
        LayoutInflater layoutInflater = LayoutInflater.from(otherAppContext);
        return layoutInflater.inflate(remoteView.getLayoutId(), null, true);
    }
    */

    fun logResourceInfo(context: Context, resId: Int) {
        val resources = context.resources
        val resourceName = resources.getResourceName(resId)
        FooLog.e(TAG, "logResourceInfo: resourceName == " + FooString.quote(resourceName))
        val resourcePackageName = resources.getResourcePackageName(resId)
        FooLog.e(
            TAG,
            "logResourceInfo: resourcePackageName == " + FooString.quote(resourcePackageName)
        )
        val resourceEntryName = resources.getResourceEntryName(resId)
        FooLog.e(TAG, "logResourceInfo: resourceEntryName == " + FooString.quote(resourceEntryName))
        val resourceTypeName = resources.getResourceTypeName(resId)
        FooLog.e(TAG, "logResourceInfo: resourceTypeName == " + FooString.quote(resourceTypeName))
    }

    fun getPackageIdOfChildWithName(parent: View, childName: String): Int {
        //FooLog.e(TAG, "getPackageIdOfChildWithName(parent=" + parent + ", childName=" + FooString.quote(childName) + ')');
        return getIdentifier(parent.context, ResourceType.id, childName)
    }

    fun getAndroidIdOfChildWithName(parent: View, childName: String): Int {
        //FooLog.e(TAG, "getAndroidIdOfChildWithName(parent=" + parent + ", childName=" + FooString.quote(childName) + ')');
        val resources = parent.resources
        return getIdentifier(resources, "android", ResourceType.id, childName)
    }

    private fun getIdOfChildWithName(
        resources: Resources,
        packageName: String,
        childName: String
    ): Int {
        return getIdentifier(resources, childName, ResourceType.id, packageName)
    }

    fun getIdentifier(context: Context, resourceType: ResourceType, name: String): Int {
        val resources = context.resources
        val packageName = context.packageName
        return getIdentifier(resources, packageName, resourceType, name)
    }

    fun getIdentifier(
        resources: Resources,
        packageName: String,
        resourceType: ResourceType,
        name: String
    ): Int {
        return resources.getIdentifier(name, resourceType.name, packageName)
    }

    fun getImageResource(imageView: ImageView): Int {
        try {
            val field = imageView.javaClass.getDeclaredField("mResource")
            field.isAccessible = true
            return field.get(imageView) as Int
        } catch (e: NoSuchFieldException) {
            FooLog.e(TAG, "getImageResource", e)
        } catch (e: IllegalAccessException) {
            FooLog.e(TAG, "getImageResource", e)
        }

        return 0
    }

    fun getRemoteViewValueById(
        remoteViews: RemoteViews?,
        viewId: Int,
        valueType: ActionValueType
    ): Any? {
        if (remoteViews == null) {
            return null
        }

        try {
            var cls: Class<*>? = remoteViews.javaClass
            while (cls != RemoteViews::class.java) {
                cls = cls!!.getSuperclass()
            }

            val field = cls.getDeclaredField("mActions")
            field.isAccessible = true

            val actions: ArrayList<Parcelable>? = field.get(remoteViews) as ArrayList<Parcelable>?

            for (i in actions!!.indices) {
                val parcelable: Parcelable = actions.get(i)

                val parcel: Parcel = Parcel.obtain()

                try {
                    parcelable.writeToParcel(parcel, 0)

                    parcel.setDataPosition(0)

                    val actionTag: Int = parcel.readInt()
                    FooLog.v(TAG, "getRemoteViewValueById: actionTag=" + toVerboseString(actionTag))
                    when (valueType) {
                        ActionValueType.PENDING_INTENT -> when (actionTag) {
                            TagTypes.PendingIntent -> {}
                            else -> continue
                        }
                        ActionValueType.TEXT, ActionValueType.VISIBILITY, ActionValueType.ENABLED, ActionValueType.IMAGE_RESOURCE_ID -> when (actionTag) {
                            TagTypes.ReflectionAction -> {}
                            else -> continue
                        }
                        ActionValueType.INTENT -> when (actionTag) {
                            TagTypes.SetOnClickFillInIntent, TagTypes.SetRemoteViewsAdapterIntent -> {}
                            else -> continue
                        }
                        ActionValueType.BITMAP_RESOURCE_ID -> when (actionTag) {
                            TagTypes.BitmapReflectionAction -> {}
                            else -> continue
                        }
                        else -> continue
                    }

                    val actionViewId: Int = parcel.readInt()
                    FooLog.v(
                        TAG,
                        "getRemoteViewValueById: actionViewId=" + toVerboseString(actionViewId)
                    )
                    if (actionViewId != viewId) {
                        continue
                    }

                    var value: Any? = null

                    when (actionTag) {
                        TagTypes.PendingIntent -> {
                            if (parcel.readInt() != 0) {
                                value = PendingIntent.readPendingIntentOrNullFromParcel(parcel)
                            }
                        }

                        TagTypes.ReflectionAction -> {
                            val actionMethodName: String? = parcel.readString()
                            FooLog.v(
                                TAG,
                                "getRemoteViewValueById: actionMethodName=" + FooString.quote(
                                    actionMethodName
                                )
                            )
                            when (valueType) {
                                ActionValueType.TEXT -> if ("setText" != actionMethodName) {
                                    continue
                                }
                                ActionValueType.VISIBILITY -> if ("setVisibility" != actionMethodName) {
                                    continue
                                }
                                ActionValueType.IMAGE_RESOURCE_ID -> if ("setImageResource" != actionMethodName) {
                                    continue
                                }
                                ActionValueType.ENABLED -> if ("setEnabled" != actionMethodName) {
                                    continue
                                }
                                else -> continue
                            }

                            val actionType: Int = parcel.readInt()
                            // per:
                            // https://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/RemoteViews.java#L1101
                            when (actionType) {
                                ActionTypes.BOOLEAN -> value = parcel.readInt() != 0
                                ActionTypes.INT -> value = parcel.readInt()
                                ActionTypes.CHAR_SEQUENCE -> value =
                                    TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel)
                                        .toString()
                                        .trim { it <= ' ' }
                            }
                        }
                        TagTypes.SetOnClickFillInIntent, TagTypes.SetRemoteViewsAdapterIntent -> {
                            if (parcel.readInt() != 0) {
                                value = Intent.CREATOR.createFromParcel(parcel)
                            }
                        }
                        TagTypes.BitmapReflectionAction -> {
                            val actionMethodName: String? = parcel.readString()
                            FooLog.v(
                                TAG,
                                "getRemoteViewValueById: actionMethodName=" + FooString.quote(
                                    actionMethodName
                                )
                            )
                            when (valueType) {
                                ActionValueType.BITMAP_RESOURCE_ID -> if ("setImageBitmap" != actionMethodName) {
                                    continue
                                }
                                else -> continue
                            }

                            value = parcel.readInt()
                        }
                        else -> continue
                    }

                    val parcelDataAvail: Int = parcel.dataAvail()
                    if (parcelDataAvail > 0) {
                        FooLog.w(TAG, "getRemoteViewValueById: parcel.dataAvail()=$parcelDataAvail")
                    }

                    return value
                } finally {
                    parcel.recycle()
                }
            }
        } catch (e: IllegalAccessException) {
            FooLog.e(TAG, "getRemoteViewValueById", e)
        } catch (e: NoSuchFieldException) {
            FooLog.e(TAG, "getRemoteViewValueById", e)
        }

        return null
    }

    fun walkActions(remoteViews: RemoteViews?, actionInfos: ActionInfos?) {
        if (remoteViews == null) {
            return
        }

        try {
            var cls: Class<*>? = remoteViews.javaClass
            while (cls != RemoteViews::class.java) {
                cls = cls!!.getSuperclass()
            }

            val field = cls.getDeclaredField("mActions")
            field.isAccessible = true

            val actions: ArrayList<Parcelable>? = field.get(remoteViews) as ArrayList<Parcelable>?

            for (i in actions!!.indices) {
                val parcelable: Parcelable = actions.get(i)

                val parcel: Parcel = Parcel.obtain()

                try {
                    parcelable.writeToParcel(parcel, 0)

                    parcel.setDataPosition(0)

                    val actionTag: Int = parcel.readInt()

                    //FooLog.v(TAG, "walkActions: actionTag=" + toVerboseString(actionTag));
                    val actionViewId: Int = parcel.readInt()

                    //FooLog.v(TAG, "walkActions: actionViewId=" + toVerboseString(actionViewId));
                    val actionValueType: ActionValueType?
                    var value: Any? = null

                    when (actionTag) {
                        TagTypes.PendingIntent -> {
                            actionValueType = ActionValueType.PENDING_INTENT
                            if (parcel.readInt() != 0) {
                                value = PendingIntent.readPendingIntentOrNullFromParcel(parcel)
                            }
                        }

                        TagTypes.ReflectionAction -> {
                            val actionMethodName: String? = parcel.readString()
                            //FooLog.e(TAG, "walkActions: actionMethodName=" + FooString.quote(actionMethodName));
                            actionValueType = when (actionMethodName) {
                                "setText" -> ActionValueType.TEXT
                                "setVisibility" -> ActionValueType.VISIBILITY
                                "setImageResource" -> ActionValueType.IMAGE_RESOURCE_ID

                                "setEnabled" -> ActionValueType.ENABLED
                                else -> ActionValueType.UNKNOWN
                            }

                            val actionType: Int = parcel.readInt()
                            // per:
                            // https://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/RemoteViews.java#L1101
                            when (actionType) {
                                ActionTypes.BOOLEAN -> value = parcel.readInt() != 0
                                ActionTypes.INT -> value = parcel.readInt()
                                ActionTypes.CHAR_SEQUENCE -> value =
                                    TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel)

                                ActionTypes.INTENT -> if (parcel.readInt() != 0) {
                                    value = Intent.CREATOR.createFromParcel(parcel)
                                }

                                ActionTypes.ICON -> if (parcel.readInt() != 0) {
                                    value = Icon.CREATOR.createFromParcel(parcel)
                                }
                            }
                        }

                        TagTypes.BitmapReflectionAction -> {
                            parcel.readString()

                            //FooLog.v(TAG, "walkActions: actionMethodName=" + FooString.quote(actionMethodName));
                            actionValueType = ActionValueType.BITMAP_RESOURCE_ID

                            value = parcel.readInt()
                        }

                        else -> continue
                    }

                    val parcelDataAvail: Int = parcel.dataAvail()
                    if (parcelDataAvail > 0) {
                        FooLog.w(TAG, "walkActions: parcel.dataAvail()=$parcelDataAvail")
                    }

                    val actionInfo = ActionInfo(actionViewId, actionValueType, value)

                    //FooLog.i(TAG, "walkActions: actionInfo=" + actionInfo);
                    actionInfos?.add(actionInfo)
                } finally {
                    parcel.recycle()
                }
            }
        } catch (e: IllegalAccessException) {
            FooLog.e(TAG, "walkActions", e)
        } catch (e: NoSuchFieldException) {
            FooLog.e(TAG, "walkActions", e)
        }
    }

    fun walkView(view: View?, viewWrappers: ViewWrappers?, visibleOnly: Boolean) {
        walkView(0, view, viewWrappers, visibleOnly, null)
    }

    fun walkView(
        view: View?,
        viewWrappers: ViewWrappers?,
        visibleOnly: Boolean,
        callbacks: WalkViewCallbacks?
    ) {
        walkView(0, view, viewWrappers, visibleOnly, callbacks)
    }

    private fun walkView(
        depth: Int,
        view: View?,
        viewWrappers: ViewWrappers?,
        visibleOnly: Boolean,
        callbacks: WalkViewCallbacks?
    ) {
        if (view == null) {
            return
        }

        if (visibleOnly && view.visibility != View.VISIBLE) {
            return
        }

        val indent = StringBuilder()
        for (i in 0..<depth) {
            indent.append(' ')
        }
        FooLog.v(TAG, "walkView: " + indent + "view=" + view)

        viewWrappers?.add(view)

        if (callbacks != null) {
            if (view is TextView && view !is Button) {
                callbacks.onTextView(view)
            }
        }

        if (view is ViewGroup) {
            val childCount = view.size
            for (i in 0..<childCount) {
                val childView = view.getChildAt(i)
                walkView(depth + 1, childView, viewWrappers, visibleOnly, callbacks)
            }
        }
    }

    fun unknownIfNullOrEmpty(context: Context, value: CharSequence?): String {
        return unknownIfNullOrEmpty(context, value?.toString())
    }

    fun unknownIfNullOrEmpty(context: Context, value: String): String {
        var value = value
        if (FooString.isNullOrEmpty(value)) {
            value = context.getString(R.string.unknown)
        }
        return value
    }

    @Suppress("unused", "EnumEntryName")
    enum class ResourceType {
        drawable,
        id,
        string,
    }

    /*
    public static BitmapDrawable getImageBitmap(@NonNull ImageView imageView)
    {
        //noinspection TryWithIdenticalCatches
        try
        {
            Field field = imageView.getClass().getDeclaredField("mRecycleableBitmapDrawable");
            field.setAccessible(true);
            return (BitmapDrawable) field.get(imageView);
        }
        catch (NoSuchFieldException e)
        {
            FooLog.e(TAG, "getImageBitmap", e);
        }
        catch (IllegalAccessException e)
        {
            FooLog.e(TAG, "getImageBitmap", e);
        }
        return null;
    }
    */

    /*
    public static BitmapDrawable getImageBitmap(@NonNull RemoteViews remoteViews, BitmapDrawable )
    {
        //noinspection TryWithIdenticalCatches
        try
        {
            Field field = imageView.getClass().getDeclaredField("mRecycleableBitmapDrawable");
            field.setAccessible(true);
            return (BitmapDrawable) field.get(imageView);
        }
        catch (NoSuchFieldException e)
        {
            FooLog.e(TAG, "getImageBitmap", e);
        }
        catch (IllegalAccessException e)
        {
            FooLog.e(TAG, "getImageBitmap", e);
        }
        return null;
    }
    */

    /*
    public static View findViewByName(@NonNull View parent, @NonNull String childName)
    {
        FooLog.v(TAG, "findViewByName(parent=" + parent + ", childName=" + FooString.quote(childName) + ')');
        int id = getIdOfChildWithName(parent, childName);
        if (id == 0)
        {
            return null;
        }
        return parent.findViewById(id);
    }
    */

    interface TagTypes {
        companion object {
            /**
             * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/RemoteViews.java#L733
             */
            const val PendingIntent: Int = 1

            /**
             * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/RemoteViews.java#L1057
             */
            const val ReflectionAction: Int = 2

            /**
             * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/RemoteViews.java#L437
             */
            const val SetOnClickFillInIntent: Int = 9

            /**
             * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/RemoteViews.java#L655
             */
            const val SetRemoteViewsAdapterIntent: Int = 10

            /**
             * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/RemoteViews.java#L1050
             */
            const val BitmapReflectionAction: Int = 12
        }
    }

    /**
     * From:
     * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/RemoteViews.java#L1074
     */
    @Suppress("unused")
    interface ActionTypes {
        companion object {
            const val BOOLEAN: Int = 1
            const val BYTE: Int = 2
            const val SHORT: Int = 3
            const val INT: Int = 4
            const val LONG: Int = 5
            const val FLOAT: Int = 6
            const val DOUBLE: Int = 7
            const val CHAR: Int = 8
            const val STRING: Int = 9
            const val CHAR_SEQUENCE: Int = 10
            const val URI: Int = 11
            const val BITMAP: Int = 12
            const val BUNDLE: Int = 13
            const val INTENT: Int = 14
            const val COLOR_STATE_LIST: Int = 15
            const val ICON: Int = 16
        }
    }

    enum class ActionValueType {
        UNKNOWN,
        TEXT,
        VISIBILITY,
        ENABLED,
        IMAGE_RESOURCE_ID,
        BITMAP_RESOURCE_ID,
        PENDING_INTENT,
        INTENT,
        //ICON,
    }

    /*
    public static Object getViewValueById(View parent, int viewId, int valueType)
    {
        Object value = null;
        View view = parent.findViewById(viewId);
        if (view != null)
        {
            switch (valueType)
            {
                case ValueTypes.TEXT:
                    if (view instanceof TextView)
                    {
                        value = ((TextView) view).getText();
                    }
                    break;
            }
        }
        return value;
    }
    */

    class ActionInfo(
        val mViewId: Int,
        val mValueType: ActionValueType,
        val mValue: Any?) :
        Comparable<ActionInfo> {

        override fun toString(): String {
            val sb = StringBuilder()

            sb.append("{ mViewId=0x").append(Integer.toHexString(mViewId))
                .append(", mValueType=").append(mValueType)
                .append(", mValue=")

            when (mValueType) {
                ActionValueType.BITMAP_RESOURCE_ID, ActionValueType.IMAGE_RESOURCE_ID -> sb.append("0x")
                    .append(
                        Integer.toHexString((mValue as Int?)!!)
                    )
                ActionValueType.VISIBILITY -> {
                    if (mValue is Int) {
                        sb.append(FooViewUtils.viewVisibilityToString(mValue))
                    } else {
                        sb.append(FooString.quote(mValue))
                    }
                }
                else -> sb.append(FooString.quote(mValue))
            }
            sb.append(" }")
            return sb.toString()
        }

        override fun compareTo(other: ActionInfo): Int {
            return mViewId.compareTo(other.mViewId)
        }
    }

    class ActionInfos {
        private val mViewIdToActionValueTypeToActionInfo: MutableMap<Int?, MutableMap<ActionValueType?, ActionInfo?>?> =
            LinkedHashMap()

        fun add(actionInfo: ActionInfo) {
            val viewId = actionInfo.mViewId

            var actionInfos = mViewIdToActionValueTypeToActionInfo.get(viewId)
            if (actionInfos == null) {
                actionInfos = LinkedHashMap<ActionValueType?, ActionInfo?>()
                mViewIdToActionValueTypeToActionInfo.put(viewId, actionInfos)
            }

            actionInfos.put(actionInfo.mValueType, actionInfo)
        }

        fun get(viewId: Int): MutableMap<ActionValueType?, ActionInfo?>? {
            return mViewIdToActionValueTypeToActionInfo.get(viewId)
        }
    }

    interface WalkViewCallbacks {
        fun onTextView(textView: TextView)
    }

    class ViewWrapper
        (val mView: View) {
        val mViewEntryName: String?

        init {
            val viewId = mView.id

            val res = mView.resources
            /*
            switch (mId & 0xff000000)
            {
                / *
                case 0x7f000000:
                    mPackageName = "app";
                    break;
                case 0x01000000:
                    mPackageName = "android";
                    break;
                    * /
                default:
                    mPackageName = res.getResourcePackageName(mId);
                    break;
            }
            mTypeName = res.getResourceTypeName(mId);
            */
            mViewEntryName = res.getResourceEntryName(viewId)
        }

        val viewId: Int
            get() = mView.id

        override fun toString(): String {
            return mView.toString()
        }
    }

    class ViewWrappers {
        private val mViewEntryNameToViewInfo: MutableMap<String?, ViewWrapper?>
        private val mViewIdToViewInfo: MutableMap<Int?, ViewWrapper?>

        init {
            mViewEntryNameToViewInfo = LinkedHashMap<String?, ViewWrapper?>()
            mViewIdToViewInfo = LinkedHashMap<Int?, ViewWrapper?>()
        }

        fun add(view: View) {
            val viewWrapper = ViewWrapper(view)
            mViewEntryNameToViewInfo.put(viewWrapper.mViewEntryName, viewWrapper)
            mViewIdToViewInfo.put(viewWrapper.viewId, viewWrapper)
        }

        fun get(name: String): ViewWrapper? {
            return mViewEntryNameToViewInfo.get(name)
        }

        fun get(id: Int): ViewWrapper? {
            return mViewIdToViewInfo.get(id)
        }

        override fun toString(): String {
            val sb = StringBuilder()
            sb.append("{")
            val iterator = mViewEntryNameToViewInfo.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                sb.append(entry.key).append(':').append(entry.value)
                if (iterator.hasNext()) {
                    sb.append(", ")
                }
            }
            sb.append(" }")
            return sb.toString()
        }
    }
}