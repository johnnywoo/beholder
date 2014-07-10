package beholder.backend.configuration

import beholder.backend.user.UserConfiguration
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.Path
import com.google.gson.Gson
import beholder.backend.fromJsonOrNull

val GSON = Gson()

fun getUserConfiguration(apiKey: String): UserConfiguration? {
    // TODO refactor, cache user configurations
    val userHome = System.getProperty("user.home")
    if (userHome == null) {
        return null
    }

    val usersPath = Paths.get(userHome, ".beholder", "users")
    if (usersPath == null) {
        return null
    }

    if (!Files.exists(usersPath)) {
        Files.createDirectories(usersPath)
        return null
    }

    return Files.newDirectoryStream(usersPath, "*.json")?.firstOrNull { it.userConfiguration?.apiKey == apiKey }?.userConfiguration
}

val Path.userConfiguration : UserConfiguration?
    get() = GSON.fromJsonOrNull(Files.newBufferedReader(this)?.use { it.readText() }, javaClass<UserConfiguration>())
