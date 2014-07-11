package beholder.backend

import java.util.logging.Logger
import java.util.logging.Level
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.nio.file.Path
import java.nio.charset.Charset
import java.nio.file.Files
import java.io.IOException
import java.security.SecureRandom
import java.math.BigInteger

fun Any.log(message: String)
    = Logger.getLogger(this.javaClass.getName()).log(Level.INFO, message)
fun Any.logWarning(message: String, cause: Throwable?)
    = Logger.getLogger(this.javaClass.getName()).log(Level.WARNING, message, cause)

fun Gson.fromJsonOrNull<T : Any>(json: String?, classOfT: Class<T>): T?
    = try { this.fromJson(json, classOfT) } catch (e: JsonSyntaxException) { null }

fun String.addUriPathComponent(component: String)
    = this + (if (this.endsWith("/")) "" else "/") + component

private val random = SecureRandom()
fun makeRandomString(length: Int)
    = BigInteger(5 * length, SecureRandom()).toString(32) // TODO this does not actually guarantee correct length

fun getFileContents(path: Path, charset: Charset = defaultCharset): String? {
    try {
        return Files.newBufferedReader(path, charset).use { it.readText() }
    } catch (e: IOException) {
        return null
    }
}

fun putFileContents(path: Path, content: String, charset: Charset = defaultCharset) {
    Files.newBufferedWriter(path, charset).use { it.write(content) }
}
