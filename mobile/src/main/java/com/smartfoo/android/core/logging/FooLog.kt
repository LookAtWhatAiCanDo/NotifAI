package com.smartfoo.android.core.logging

import com.smartfoo.android.core.FooReflection
import kotlin.reflect.KClass

object FooLog {
    private val TAG = TAG(FooLog::class)

    @Suppress("FunctionName")
    @JvmStatic
    fun TAG(o: Any): String = TAG(o.javaClass)

    @Suppress("FunctionName")
    @JvmStatic
    fun TAG(c: KClass<*>): String = TAG(c.java)

    @Suppress("FunctionName")
    @JvmStatic
    fun TAG(c: Class<*>): String = TAG(FooReflection.getShortClassName(c))

    /**
     * Per https://developer.android.com/reference/android/util/Log.html#isLoggable(java.lang.String,%20int)
     */
    const val LOG_TAG_LENGTH_LIMIT: Int = 23

    /**
     * Limits the tag length to [.LOG_TAG_LENGTH_LIMIT]
     *
     * @param value tag
     * @return the tag limited to [.LOG_TAG_LENGTH_LIMIT]
     */
    @Suppress("FunctionName")
    @JvmStatic
    fun TAG(value: String): String {
        var tag = value
        val length = tag.length
        if (length > LOG_TAG_LENGTH_LIMIT) {
            // Turn "ReallyLongClassName" to "ReallyLo…lassName";
            val half = LOG_TAG_LENGTH_LIMIT / 2
            tag = tag.substring(0, half) + '…' + tag.substring(length - half)
        }
        return tag
    }
}