package beholder.backend.config

import java.nio.file.Files
import java.nio.file.Path
import beholder.backend.fromJsonOrNull
import java.nio.file.Paths
import beholder.backend.getFileContents
import beholder.backend.putFileContents
import beholder.backend.log
import beholder.backend.makeRandomString
import beholder.backend.gson

class Configuration(val configDir: String) {
    val app = loadConfig("app.json", javaClass<AppConfiguration>())

    private val userConfigurations: List<UserConfiguration> = loadUserConfigurations()

    private fun loadConfig<T : Any>(file: String, klass: Class<T>): T {
        val config = gson.fromJsonOrNull(getFileContents(Paths.get(configDir, file)), klass)
        if (config != null) {
            return config
        }

        val emptyConfig = gson.fromJsonOrNull("{}", klass)
        if (emptyConfig == null) {
            throw Exception("Cannot create empty config for " + klass.getName())
        }
        return emptyConfig
    }

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
                    val userConfiguration = gson.fromJsonOrNull(getFileContents(it), javaClass<UserConfiguration>())
                    if (userConfiguration != null) {
                        val fileName = it.getFileName()?.toString()
                        if (fileName != null) {
                            userConfiguration.userName = fileName.substring(0, fileName.length - ".json".length)
                            userConfiguration.apiKey   = makeRandomString(32)
                            configurations.add(userConfiguration)
                        }
                    } else {
                        log("Corrupted user config: " + it.toString())
                    }
                }
            }
        }
        return configurations
    }

    private fun getUsersPath(): Path?
        = Paths.get(configDir, "users")

    fun getUserConfiguration(userName: String)
        = userConfigurations.firstOrNull { it.userName == userName }

    fun getUserConfigurationByApiKey(apiKey: String): UserConfiguration?
        = userConfigurations.firstOrNull { it.apiKey == apiKey }

    fun saveUserConfiguration(userConf: UserConfiguration) {
        val text = gson.toJson(userConf)
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

        val userPath = usersPath.resolve(userConf.userName + ".json")
        if (userPath == null) {
            return
        }
        putFileContents(userPath, text)
    }
}
