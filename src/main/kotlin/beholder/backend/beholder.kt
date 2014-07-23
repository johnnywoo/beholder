package beholder.backend

import beholder.backend.api.EchoMessage
import beholder.backend.api.LoginMessage
import beholder.backend.config.Configuration
import beholder.backend.config.UserConfiguration
import beholder.backend.http.Server
import beholder.backend.http.Connection
import com.google.gson.GsonBuilder
import beholder.backend.files.FileMonitor
import beholder.backend.api.FileFoundMessage
import beholder.backend.api.FileLostMessage
import beholder.backend.api.LineScannedMessage

val gson = GsonBuilder().setPrettyPrinting()?.create()!!

fun main(args: Array<String>) {
    try {
        val conf = Configuration((System.getProperty("user.home") ?: ".") + "/.beholder")

        when (getItemOrNull(args, 0)) {
            "daemon" -> {
                daemon(conf)
            }

            "user" -> {
                val userName = getItemOrNull(args, 1)
                val password = getItemOrNull(args, 2)
                if (userName == null || password == null) {
                    System.err.println("Usage: beholder.sh user <username> <password>")
                    System.exit(1)
                    return
                }
                createUser(conf, userName, password)
            }

            else -> {
                System.err.println("Usage:")
                System.err.println("beholder.sh daemon")
                System.err.println("beholder.sh user <username> <password>")
                System.exit(1)
            }
        }
    } catch (e: BadStartException) {
        System.err.println(e.getMessage())
        System.exit(1)
    }
}

fun daemon(conf: Configuration) {
    //
    // FILE MONITOR SETUP
    //

    val fileMonitor = FileMonitor(conf)
    fileMonitor.onFileFound = {
        fileMonitor.log("Log file found: $it")
        Connection.sendToAll(FileFoundMessage(it))
    }
    fileMonitor.onFileLost = {
        fileMonitor.log("Log file lost: $it")
        Connection.sendToAll(FileLostMessage(it))
    }
    fileMonitor.onLineScanned = {
        file, text ->
            fileMonitor.log("Updated: $file with $text")
            Connection.sendToAll(LineScannedMessage(file, text))
    }


    //
    // HTTP SERVER SETUP
    //

    val server = Server(conf, "beholder.backend")
    server.onAction("login", javaClass<LoginMessage>(), {
        connection, data ->
            if (!connection.isAuthorized() && data is LoginMessage) {
                val userConfiguration = conf.getUserConfigurationByApiKey(data.apiKey)
                if (userConfiguration != null) {
                    connection.user = userConfiguration
                }
            }
    })

    server.onAction("echo", javaClass<EchoMessage>(), restictedAction {
        connection, data ->
            connection.send(EchoMessage((data as EchoMessage).data))
    })


    //
    // START
    //
    fileMonitor.startInBackground()
    server.start()
}

fun createUser(conf: Configuration, userName: String, password: String) {
    val user = UserConfiguration()
    user.userName = userName
    user.password = password
    conf.saveUserConfiguration(user)
}

fun restictedAction(block: (Connection, Any) -> Unit): (Connection, Any) -> Unit
    = { connection, data -> if (connection.isAuthorized()) block(connection, data) }
