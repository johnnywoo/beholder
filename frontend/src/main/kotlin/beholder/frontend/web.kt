package beholder.frontend

fun main(args: Array<String>) {

    // this should be working according to examples
    println("println baby!") // this actually does not print anything, but accumulates the output, which you can then get in JS by calling Kotlin.System.output()

    // this works, but in the IDE it's red
    val doc = js.dom.html.document
    doc.write("BLAH")
    val pre = doc.createElement("pre")
    pre.appendChild(doc.createTextNode("monospaced"))
    doc.getElementsByTagName("body").item(0).appendChild(pre)

    // this works and is green in the IDE
    val kdoc = kotlin.browser.document
    kdoc.getElementsByTagName("body")?.item(0)?.appendChild(kdoc.createTextNode("kotlin.browser.document also works")!!)
}
