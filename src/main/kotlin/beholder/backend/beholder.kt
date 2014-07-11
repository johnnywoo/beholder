package beholder.backend

import beholder.backend.api.EchoMessage
import beholder.backend.api.LoginMessage
import beholder.backend.config.Configuration
import beholder.backend.config.UserConfiguration
import beholder.backend.http.Server
import com.google.gson.Gson
import beholder.backend.http.Connection

val gson = Gson()

val configuration = Configuration("beholder")

fun main(args: Array<String>) {
    when (getItemOrNull(args, 0)) {
        "daemon" -> {
            daemon()
        }

        "user" -> {
            val userName = getItemOrNull(args, 1)
            val password = getItemOrNull(args, 2)
            if (userName == null || password == null) {
                System.err.println("Usage: beholder.sh user <username> <password>")
                System.exit(1)
                return
            }
            createUser(userName, password)
        }

        else -> {
            System.err.println("Usage:")
            System.err.println("beholder.sh daemon")
            System.err.println("beholder.sh user <username> <password>")
            System.exit(1)
        }
    }
}

fun daemon() {
    val server = Server(configuration, "beholder.backend")

    server.onAction("login", javaClass<LoginMessage>(), {
        connection, data ->
            if (!connection.isAuthorized() && data is LoginMessage) {
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

fun createUser(userName: String, password: String) {
    val user = UserConfiguration()
    user.userName = userName
    user.password = password
    configuration.saveUserConfiguration(user)
}

fun restictedAction(block: (Connection, Any) -> Unit): (Connection, Any) -> Unit
    = { connection, data -> if (connection.isAuthorized()) block(connection, data) }
