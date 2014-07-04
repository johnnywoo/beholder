package beholder.frontend.sugar

class Window {
    val location = Location()
    val document = kotlin.browser.document

    fun alert(message: String) {
        BrowserNative()["alert"].call(message, context = BrowserNative()) // alert only works in 'window' context
    }
}
