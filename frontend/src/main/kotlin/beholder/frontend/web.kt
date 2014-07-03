package beholder.frontend

import js.dom.html.document

fun main(args: Array<String>) {
    document.getElementById("email").setAttribute("value", "hello@kotlinlang.org")
}
