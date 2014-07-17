package beholder.frontend

import js.dom.html.*
import js.native
import beholder.backend.api.EchoMessage
import js.reflection.hack.jsClass
import beholder.backend.api.LoginMessage
import beholder.backend.api.ComplexMessage

native val beholderApiKey: String = js.noImpl

fun main(args: Array<String>) {
    val webSocketLocation = "ws://" + window.location.host + "/ws" // host includes port, unlike hostname

//    js.debug.console.log(jsClass<EchoMessage>())
    js.debug.console.log("constructor args", jsClass<ComplexMessage>().getConstructorArgNames())
    for (entry in jsClass<ComplexMessage>().getProperties().entrySet()) {
        js.debug.console.log(entry.getKey(), entry.getValue().typedef)
    }

    val apiClient = ApiClient(webSocketLocation)
    apiClient.connect()

    apiClient.send(EchoMessage("this shall not pass"))
    apiClient.send(LoginMessage(beholderApiKey))
    apiClient.send(EchoMessage("hello bloody world"))
}
