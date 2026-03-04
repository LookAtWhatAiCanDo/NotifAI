package llc.lookatwhataicando.notifai.util

object MyReflectionUtils {
    @JvmStatic
    fun getClass(o: Any?): Class<*>? = o as? Class<*> ?: o?.javaClass

    @JvmStatic
    fun getClassName(o: Any?): String = getClassName(getClass(o))

    @JvmStatic
    fun getClassName(c: Class<*>?): String =
        getClassName(c?.getName(), true)

    @JvmStatic
    fun getClassName(
        className: String?,
        shortClassName: Boolean,
    ): String {
        val className = className ?: "null"
        return if (shortClassName) className.substring(className.lastIndexOf('.') + 1) else className
    }

    @JvmStatic
    fun getShortClassName(className: String?): String = getClassName(className, true)

    @JvmStatic
    fun getShortClassName(o: Any?): String = getShortClassName(o?.javaClass)

    @JvmStatic
    fun getShortClassName(c: Class<*>?): String = getShortClassName(c?.getName())
}