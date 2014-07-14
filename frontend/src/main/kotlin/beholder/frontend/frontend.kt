package beholder.frontend

import js.dom.html.*
import beholder.frontend.sugar.WebSocket
import js.native
import js.JSON
import beholder.backend.api.EchoMessage

native val beholderApiKey: String = js.noImpl

fun main(args: Array<String>) {
    val webSocketLocation = "ws://" + window.location.host + "/ws" // host includes port, unlike hostname

    val webSocket = WebSocket(webSocketLocation)
    webSocket.onopen = {
        println("Websocket connected to $webSocketLocation")
        webSocket.onmessage = {
            println("message")
            println(it.data)
        }
        webSocket.onclose = {
            println("close")
        }

        webSocket.send("{\"action\": \"echo\", \"data\": \"hello world\"}")
        webSocket.send("{\"action\": \"login\", \"apiKey\": \"$beholderApiKey\"}")
        webSocket.send(JSON.stringify(EchoMessage("hello bloody world")))
    }
}
