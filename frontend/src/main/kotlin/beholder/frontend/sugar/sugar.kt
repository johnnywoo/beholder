package beholder.frontend.sugar

import org.w3c.dom.Document

native val window: Window = noImpl

native class Window {
    native val location: Location = js.noImpl
    native val document: Document = js.noImpl

    native fun alert(message: String) = js.noImpl
}

native class Location {
    native val href: String = js.noImpl
}
