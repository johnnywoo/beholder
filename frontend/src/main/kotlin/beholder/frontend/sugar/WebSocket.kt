package beholder.frontend.sugar

import js.dom.html.*
import js.*

native public class WebSocket(val url: String) {
    var onopen: (WebSocketEvent) -> Unit = noImpl
    var onmessage: (WebSocketMessageEvent) -> Unit = noImpl
    var onclose: (WebSocketEvent) -> Unit = noImpl

    fun send(message: String) = noImpl
    fun close() = noImpl


    open public class WebSocketEvent

    public class WebSocketMessageEvent : WebSocketEvent() {
        val data: String = noImpl
    }
}