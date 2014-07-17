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

//
// LOG
//

fun Any.log(message: String)
    = Logger.getLogger(this.javaClass.getName()).log(Level.INFO, message)

fun Any.logWarning(message: String, cause: Throwable?)
    = Logger.getLogger(this.javaClass.getName()).log(Level.WARNING, message, cause)


//
// MISC
//

fun Gson.fromJsonOrNull<T : Any>(json: String?, classOfT: Class<T>): T?
    = try { this.fromJson(json, classOfT) } catch (e: JsonSyntaxException) { null }

fun <T : Any> T.having(block: (T) -> Boolean): T?
    = if (block(this)) this else null


//
// ARRAY
//

fun <T : Any> getItemOrNull(args: Array<T>, index: Int): T?
    = if (args.size > index) args[index] else null


//
// STRING
//

fun String.addUriPathComponent(component: String)
    = this + (if (this.endsWith("/")) "" else "/") + component

private val random = SecureRandom()
fun makeRandomString(length: Int)
    = BigInteger(5 * length, SecureRandom()).toString(32) // TODO this does not actually guarantee correct length


//
// FILES
//

fun getFileContents(path: Path?, charset: Charset = defaultCharset): String? {
    if (path == null) {
        return null
    }
    try {
        return Files.newBufferedReader(path, charset).use { it.readText() }
    } catch (e: IOException) {
        return null
    }
}

fun putFileContents(path: Path, content: String, charset: Charset = defaultCharset) {
    Files.newBufferedWriter(path, charset).use { it.write(content) }
}
