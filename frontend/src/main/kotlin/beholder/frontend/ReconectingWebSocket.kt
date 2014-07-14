package beholder.frontend

import js.dom.html.window

import beholder.frontend.sugar.WebSocket
import beholder.frontend.sugar.WebSocket.WebSocketMessageEvent
import java.util.ArrayList

open class ReconnectingWebSocket(val url: String, val logger: (String)->Unit = {}, val timeoutAfterSeconds: Int = 2, val reconnectAfterSeconds: Int = 30) {
    open fun onConnect() {}
    open fun onReceive(json: String) {}
    open fun onDisconnect() {}

    fun connect() {
        connectWebSocket()
    }

    fun send(text: String) {
        if (lastConnectionIsOpen) {
            logger("WebSocket is sending " + text)
            webSocket?.send(text)
        } else {
            // if the socket is not connected yet, sending into it will result in
            // InvalidStateError: An attempt was made to use an object that is not, or is no longer, usable
            // so we accumulate these messages and will send them when the connection will be established
            unsentMessages.add(text)
            logger("WebSocket to send later " + text)
        }
    }


    //
    // PRIVATE
    //

    private var webSocket: WebSocket? = null
    private var lastConnectionIsOpen = false
    private val unsentMessages = ArrayList<String>()

    private fun connectWebSocket() {
        disconnectWebSocket()

        webSocket = WebSocket(url)
        webSocket?.onopen    = { onOpen() }
        webSocket?.onmessage = { onMessage(it) }
        webSocket?.onclose   = { onClose() }

        // handling connection timeouts
        // onOpen clears our timeout, so if that didn't happen, it means the connection took too long
        resetTimeout({
            logger("websocket connection timeout")
            disconnectWebSocket()
        }, timeoutAfterSeconds * 1000)
    }

    private fun disconnectWebSocket() {
        if (webSocket != null) {
            webSocket?.close()
            webSocket = null
        }
    }

    private fun onOpen() {
        if (timeout != null) {
            window.clearTimeout(timeout!!) // connection timeout didn't happen
        }

        lastConnectionIsOpen = true;

        logger("WebSocket connected to " + url)

        onConnect()

        if (!unsentMessages.isEmpty()) {
            val unsent = unsentMessages.copyToArray()
            unsentMessages.clear()
            for (message in unsent) {
                send(message)
            }
        }
    }

    private fun onMessage(event: WebSocketMessageEvent) {
        onReceive(event.data)
    }

    private fun onClose() {
        val seconds = if (lastConnectionIsOpen) 0 else reconnectAfterSeconds

        lastConnectionIsOpen = false

        logger("WebSocket disconnected, reconnecting" + (if (seconds == 0) "" else " in " + seconds + " sec"))
        onDisconnect()
        resetTimeout({ connectWebSocket() }, seconds * 1000)
    }


    //
    // MISC
    //

    private var timeout: Long? = null
    private fun resetTimeout(callback: ()->Unit, milliseconds: Number) {
        if (timeout != null) {
            window.clearTimeout(timeout!!)
        }
        timeout = window.setTimeout(callback, milliseconds)
    }
}
