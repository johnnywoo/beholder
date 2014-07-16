package beholder.frontend

import js.dom.html.*
import js.native
import beholder.backend.api.EchoMessage
import beholder.backend.api.LoginMessage
import beholder.backend.api.Message
import js.reflection.hack.jsClass

native val beholderApiKey: String = js.noImpl

fun main(args: Array<String>) {
    val webSocketLocation = "ws://" + window.location.host + "/ws" // host includes port, unlike hostname

    js.debug.console.log(jsClass<Message>().name)
    js.debug.console.log(jsClass<Message>().rawConstructorJs)

    val apiClient = ApiClient(webSocketLocation)
    apiClient.connect()

    apiClient.send(EchoMessage("this shall not pass"))
    apiClient.send(LoginMessage(beholderApiKey))
    apiClient.send(EchoMessage("hello bloody world"))
}
