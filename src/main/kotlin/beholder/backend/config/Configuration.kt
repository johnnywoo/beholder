package beholder.backend.config

import com.google.gson.Gson
import java.nio.file.Files
import java.nio.file.Path
import beholder.backend.fromJsonOrNull
import java.nio.file.Paths
import beholder.backend.getFileContents
import beholder.backend.putFileContents
import beholder.backend.log

class Configuration(val packageName: String) {
    val port = 3822

    private val GSON = Gson()
    private val userConfigurations: List<UserConfiguration> = loadUserConfigurations()

    private fun loadUserConfigurations(): List<UserConfiguration> {
        // something like /home/user/.beholder/users
        val usersPath = getUsersPath()
        if (usersPath == null) {
            throw Exception("E.T. phone home")
        }

        val configurations: MutableList<UserConfiguration> = arrayListOf()
        if (Files.exists(usersPath)) {
            Files.newDirectoryStream(usersPath, "*.json")?.use {
                it.forEach {
                    val userConfiguration = GSON.fromJsonOrNull(getFileContents(it), javaClass<UserConfiguration>())
                    if (userConfiguration != null) {
                        val fileName = it.getFileName()?.toString()
                        if (fileName != null) {
                            userConfiguration.userName = fileName.substring(0, fileName.length - ".json".length)
                            // TODO generate apiKey
                            userConfiguration.apiKey = userConfiguration.userName
                            configurations.add(userConfiguration)
                        }
                    }
                }
            }
        }
        return configurations
    }

    private fun getUsersPath(): Path?
        = Paths.get(System.getProperty("user.home") ?: ".", "." + packageName, "users")

    fun getUserConfiguration(userName: String)
        = userConfigurations.firstOrNull { it.userName == userName }

    fun getUserConfigurationByApiKey(apiKey: String): UserConfiguration?
        = userConfigurations.firstOrNull { it.apiKey == apiKey }

    fun saveUserConfiguration(configuration: UserConfiguration) {
        val text = GSON.toJson(configuration)
        if (text == null) {
            return
        }
        val usersPath = getUsersPath()
        if (usersPath == null) {
            return
        }
        if (!Files.exists(usersPath)) {
            Files.createDirectories(usersPath)
            log(usersPath.toString() + " created")
        }

        val userPath = usersPath.resolve(configuration.userName + ".json")
        if (userPath == null) {
            return
        }
        putFileContents(userPath, text)
    }
}
