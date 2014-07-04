package beholder.frontend.sugar

// Standard Kotlin library does not seem to be browser-friendly,
// or at least I could not find that part. So in this package we
// define helpful browser-related classes that work with JS values
// by using BrowserNative class.

// Then we patch how BrowserNative is compiled so it has access to
// actual JS values and global scope (see monkey-patches.js).

val window = Window()

class BrowserNative(val nativeValue: Any? = null) {
    fun thisIsBrowserNativeClassPleaseHackItMisterMonkey() = true

    fun get(name: String) = this // stub
    fun call(vararg args: Any?, context: BrowserNative? = null) = this // stub

    override fun toString() = "" // stub
}
