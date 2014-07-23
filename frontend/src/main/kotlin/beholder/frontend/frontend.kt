package beholder.frontend

import js.dom.html.*
import js.native
import beholder.backend.api.EchoMessage
import beholder.backend.api.LoginMessage

native val beholderApiKey: String = js.noImpl

fun main(args: Array<String>) {
    val webSocketLocation = "ws://" + window.location.host + "/ws" // host includes port, unlike hostname

    val apiClient = object : ApiClient(webSocketLocation) {
        override fun onReceive(json: String) {
            document.getElementsByTagName("body").item(0)
                .appendChild(document.createElement("div"))
                    .appendChild(document.createTextNode(json))
        }
    }

    apiClient.connect()
    apiClient.send(LoginMessage(beholderApiKey))
}
