package beholder.frontend

import beholder.backend.api.Message
import js.JSON
import js.debug.console

class ApiClient(url: String) : ReconnectingWebSocket(url, { console.log(it) }) {
    override fun onReceive(json: String) {
        console.log("WebSocket received " + json)
    }

    fun send(message: Message) {
        send(JSON.stringify(message))
    }
}

