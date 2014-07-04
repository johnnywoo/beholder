package beholder.frontend

import js.dom.html.*
import beholder.frontend.sugar.alert

fun main(args: Array<String>) {
    println("Our location: " + window.location.href) // this now goes to the console
    window.alert("IT'S ALIVE!!!")

    // this works, but in the IDE it's red
//    val doc = js.dom.html.document
//    doc.write("BLAH")
//    val pre = doc.createElement("pre")
//    pre.appendChild(doc.createTextNode("monospaced"))
//    doc.getElementsByTagName("body").item(0).appendChild(pre)

    // this works and is green in the IDE
//    val kdoc = kotlin.browser.document
//    kdoc.getElementsByTagName("body")?.item(0)?.appendChild(kdoc.createTextNode("kotlin.browser.document also works")!!)
}
