package llc.lookatwhataicando.notifai.util

import android.content.Intent
import android.os.Bundle
import java.util.Locale

object MyStringUtils {
    @JvmField
    val LINEFEED: String? = System.lineSeparator()

    /**
     * Identical to [repr], but grammatically intended for Strings.
     *
     * @param value value
     * @return "null", or '\"' + value.toString + '\"', or value.toString()
     */
    @JvmStatic
    fun quote(value: Any?): String = repr(value, false)

    /**
     * Identical to [quote], but grammatically intended for Objects.
     *
     * @param value    value
     * @param typeOnly typeOnly
     * @return "null", or '\"' + value.toString + '\"', or value.toString(), or getShortClassName(value)
     */
    @JvmOverloads
    @JvmStatic
    fun repr(
        value: Any?,
        typeOnly: Boolean = false,
    ): String {
        if (value == null) {
            return "null"
        }

        if (typeOnly) {
            return MyReflectionUtils.getShortClassName(value)
        }

        if (value is String) {
            return "\"$value\""
        }

        if (value is CharSequence) {
            return "\"$value\""
        }

        return value.toString()
    }

    @JvmStatic
    fun <T> toString(
        items: Iterable<T?>?,
        multiline: Boolean,
    ): String {
        val sb = StringBuilder()

        if (items == null) {
            sb.append("null")
        } else {
            sb.append('[')
            val it = items.iterator()
            while (it.hasNext()) {
                val item = it.next()
                sb.append(quote(item))
                if (it.hasNext()) {
                    sb.append(", ")
                }
                if (multiline) {
                    sb.append(LINEFEED)
                }
            }
            sb.append(']')
        }
        return sb.toString()
    }

    @JvmStatic
    fun toString(items: Array<Any?>?): String {
        val sb = StringBuilder()

        if (items == null) {
            sb.append("null")
        } else {
            sb.append('[')
            for (i in items.indices) {
                val item = items[i]
                if (i != 0) {
                    sb.append(", ")
                }
                sb.append(quote(item))
            }
            sb.append(']')
        }
        return sb.toString()
    }


    /**
     * More useful than [android.content.Intent.toString] that only prints "(has extras)" if there are extras.
     */
    @JvmStatic
    fun toString(intent: Intent?): String {
        if (intent == null) return "null"
        return StringBuilder()
            .append(intent)
            .append(", extras=")
            .append(toString(intent.extras))
            .toString()
    }

    /**
     * May be unnecessary; [android.os.Bundle]`.toString` output seems almost acceptable nowadays.
     */
    @JvmStatic
    fun toString(bundle: Bundle?): String {
        if (bundle == null) return "null"

        val sb = StringBuilder()

        val keys = bundle.keySet()
        val it = keys.iterator()

        sb.append('{')
        while (it.hasNext()) {
            val key = it.next()

            @Suppress("KDocUnresolvedReference")
            var value =
                try {
                    /**
                     * [android.os.BaseBundle.get] calls hidden method [android.os.BaseBundle.getValue].
                     * `android.os.BaseBundle#getValue(java.lang.String)` says:
                     * "Deprecated: Use `getValue(String, Class, Class[])`. This method should only be used in other deprecated APIs."
                     * That first sentence does not help this method that dynamically enumerates the Bundle entries without awareness/concern of any types.
                     * That second sentence tells me they probably won't be getting rid of android.os.BaseBundle#get(java.lang.String) any time soon.
                     * So marking deprecated `android.os.BaseBundle#get(java.lang.String)` as safe to call for awhile... until it isn't.
                     */
                    @Suppress("DEPRECATION")
                    bundle.get(key)
                } catch (e: RuntimeException) {
                    // Known issue if a Bundle (Parcelable) incorrectly implements writeToParcel
                    "[Error retrieving \"$key\" value: ${e.message}]"
                }

            sb.append(quote(key)).append('=')

            if (key.lowercase(Locale.getDefault()).contains("password")) {
                value = "*REDACTED*"
            }

            when (value) {
                is Bundle -> {
                    sb.append(toString(value))
                }

                is Intent -> {
                    sb.append(toString(value))
                }

                else -> {
                    sb.append(quote(value))
                }
            }

            if (it.hasNext()) {
                sb.append(", ")
            }
        }
        sb.append('}')

        return sb.toString()
    }
}