package beholder.frontend

import js.dom.html.*
import beholder.frontend.sugar.WebSocket

fun main(args: Array<String>) {
    println("Our location: " + window.location.href) // this now goes to the console
    val webSocketLocation = "ws://" + window.location.host + "/ws" // host includes port, unlike hostname
    println("Websocket location: " + webSocketLocation)

    val webSocket = WebSocket(webSocketLocation)
    webSocket.onopen = {
        println("open")
        webSocket.onmessage = {
            println("message")
            println(it.data)
        }
        webSocket.onclose = {
            println("close")
        }

        webSocket.send("{\"action\": \"echo\", \"data\": \"hello world\"}")
    }
}
