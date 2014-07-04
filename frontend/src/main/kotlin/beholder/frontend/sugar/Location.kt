package beholder.frontend.sugar

class Location {
    val href = BrowserNative()["window"]["location"]["href"].toString()
}
