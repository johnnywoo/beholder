package beholder.backend.config

import com.google.gson.Gson
import java.nio.file.Files
import java.nio.file.Path
import beholder.backend.fromJsonOrNull
import java.io.IOException
import java.nio.file.Paths

class Configuration(val packageName: String) {
    val GSON = Gson();

    private val userConfigurations: MutableList<UserConfiguration> = arrayListOf();

    {
        // something like /home/user/.beholder/users
        val usersPath = getUsersPath()
        if (usersPath == null) {
            throw Exception("E.T. phone home")
        }

        if (Files.exists(usersPath)) {
            userConfigurations.addAll(loadUserConfigurations(usersPath))
        } else {
            Files.createDirectories(usersPath)
        }
    }

    private fun loadUserConfigurations(path: Path): List<UserConfiguration> {
        val configurations: MutableList<UserConfiguration> = arrayListOf()
        Files.newDirectoryStream(path, "*.json")?.use {
            it.forEach {
                val userConfiguration = GSON.fromJsonOrNull(it.text, javaClass<beholder.backend.config.UserConfiguration>())
                if (userConfiguration != null) {
                    // TODO generate apiKey
                    userConfiguration.apiKey = userConfiguration.userName
                    configurations.add(userConfiguration)
                }
            }
        }
        return configurations
    }

    private fun getUsersPath(): Path? = Paths.get(System.getProperty("user.home") ?: ".", "." + packageName, "users")

    fun getUserConfigurationByApiKey(apiKey: String): UserConfiguration? = userConfigurations.firstOrNull { it.apiKey == apiKey }
}

val Path.text : String?
    get() = try { Files.newBufferedReader(this)?.use { it.readText() } } catch (e: IOException) { null }
