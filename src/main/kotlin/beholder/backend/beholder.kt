package beholder.backend

import beholder.backend.api.EchoMessage
import beholder.backend.api.LoginMessage
import beholder.backend.config.Configuration
import beholder.backend.config.UserConfiguration
import beholder.backend.http.Server
import com.google.gson.Gson
import beholder.backend.http.Connection

val GSON = Gson()

val configuration = Configuration("beholder")

fun main(args: Array<String>) {
    if (args.size == 0) {
        System.err.println("Usage: beholder.sh (daemon|user)")
        System.exit(1)
    }

    val command = args[0]
    when (command) {
        "daemon" -> daemon()
        "user" -> createUser(getArg(args, 1), getArg(args, 2))
        else -> {
            System.err.println("Usage: beholder.sh (daemon|user)")
            System.exit(1)
        }
    }
}

fun getArg(args: Array<String>, index: Int): String? {
    if (args.size > index) {
        return args[index]
    }
    return null
}

fun daemon() {
    val server = Server(configuration.port, "beholder.backend")

    server.onAction("login", javaClass<LoginMessage>(), {
        connection, data ->
            if (!connection.isAuthorized && data is LoginMessage) {
                val userConfiguration = configuration.getUserConfigurationByApiKey(data.apiKey)
                if (userConfiguration != null) {
                    connection.user = userConfiguration
                }
            }
    })

    server.onAction("echo", javaClass<EchoMessage>(), restictedAction {
        connection, data ->
            connection.send(EchoMessage((data as EchoMessage).data))
    })

    server.start()
}

fun createUser(userName: String?, password: String?) {
    if (userName == null || password == null) {
        System.err.println("Usage: beholder.sh user <username> <password>")
        System.exit(1)
        return
    }
    val userConfiguration = UserConfiguration()
    userConfiguration.userName = userName
    userConfiguration.password = password
    configuration.saveUserConfiguration(userConfiguration)
}

fun restictedAction(block: (Connection, Any) -> Unit): (Connection, Any) -> Unit
    = { connection, data -> if (connection.isAuthorized) block(connection, data) }
