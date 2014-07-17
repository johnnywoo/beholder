package js.regexp

import js.native
import java.util.ArrayList

native class RegExp(val regexp: String, val flags: String = "") {
    fun test(string: String): Boolean = js.noImpl
    fun exec(string: String): Array<String>? = js.noImpl
}

fun String.matchesRegexp(regexp: String)
    = RegExp(regexp).test(this)
fun String.matchesRegexp(regexp: RegExp)
    = regexp.test(this)

[native("match")] fun String.matchRegexp(regexp: RegExp): Array<String>?
    = js.noImpl

fun String.matchAllRegexp(regexp: String): List<Array<String>>
    = this.matchAllRegexp(RegExp(regexp, "g"))
fun String.matchAllRegexp(regexp: RegExp): List<Array<String>> {
    val list = ArrayList<Array<String>>()
    do {
        val match = regexp.exec(this)
        if (match != null) {
            list.add(match)
        }
    } while (match != null)
    return list
}

fun String.replaceAllRegexp(regexp: String, replacement: String)
    = this.replaceRegexp(RegExp(regexp, "g"), replacement)
fun String.replaceRegexp(regexp: String, replacement: String)
    = this.replaceRegexp(RegExp(regexp), replacement)
[native("replace")] fun String.replaceRegexp(regexp: RegExp, replacement: String): String
    = js.noImpl

fun String.splitRegexp(regexp: String)
    = this.splitRegexp(RegExp(regexp))
[native("split")] fun String.splitRegexp(regexp: RegExp): Array<String?>
    = js.noImpl

