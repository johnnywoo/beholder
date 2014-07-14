package beholder.frontend

import js.dom.html.*
import beholder.frontend.sugar.WebSocket
import js.native
import js.JSON
import beholder.backend.api.EchoMessage
import beholder.backend.api.LoginMessage

native val beholderApiKey: String = js.noImpl

fun main(args: Array<String>) {
    val webSocketLocation = "ws://" + window.location.host + "/ws" // host includes port, unlike hostname

    val apiClient = ApiClient(webSocketLocation)
    apiClient.connect()

    apiClient.send(EchoMessage("this shall not pass"))
    apiClient.send(LoginMessage(beholderApiKey))
    apiClient.send(EchoMessage("hello bloody world"))
}
